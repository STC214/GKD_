package li.songe.gkd.a11y

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.util.Log
import android.view.Display
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.getAndUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import li.songe.gkd.META
import li.songe.gkd.data.ActionPerformer
import li.songe.gkd.data.ActionResult
import li.songe.gkd.data.ActionResultState
import li.songe.gkd.data.AppRule
import li.songe.gkd.data.GkdAction
import li.songe.gkd.data.ResolvedRule
import li.songe.gkd.data.RpcError
import li.songe.gkd.data.RuleStatus
import li.songe.gkd.isActivityVisible
import li.songe.gkd.runtime.diagnostics.DecisionOutcome
import li.songe.gkd.runtime.diagnostics.DecisionReason
import li.songe.gkd.runtime.diagnostics.DecisionStage
import li.songe.gkd.runtime.diagnostics.decisionTraceBuffer
import li.songe.gkd.service.A11yService
import li.songe.gkd.service.EventService
import li.songe.gkd.service.topAppIdFlow
import li.songe.gkd.shizuku.shizukuContextFlow
import li.songe.gkd.shizuku.uiAutomationFlow
import li.songe.gkd.store.actualBlockA11yAppList
import li.songe.gkd.store.storeFlow
import li.songe.gkd.util.AndroidTarget
import li.songe.gkd.util.AutomatorModeOption
import li.songe.gkd.util.InterruptRuleMatchException
import li.songe.gkd.util.launchTry
import li.songe.gkd.util.runMainPost
import li.songe.gkd.util.showActionToast
import li.songe.gkd.util.systemUiAppId
import li.songe.selector.MatchOption
import li.songe.selector.Selector
import java.util.concurrent.Executors
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume


private val eventDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
private val queryDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
private val actionDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

internal fun <T> ArrayDeque<T>.removeMatchingPrefix(predicate: (T) -> Boolean): List<T> {
    val prefix = takeWhile(predicate)
    repeat(prefix.size) { removeFirst() }
    return prefix
}

private val latestServiceMode = atomic(0)
private val latestServiceTime = atomic(0L)

class A11yRuleEngine(val service: A11yCommonImpl) {
    private val a11yContext = A11yContext(this)
    private val effective get() = latestServiceMode.value == service.mode.value
    private val hasOthersService = when (service.mode) {
        AutomatorModeOption.A11yMode -> uiAutomationFlow.value != null
        AutomatorModeOption.AutomationMode -> A11yService.instance != null
    }

    fun onA11yConnected() {
        val serviceTime = System.currentTimeMillis()
        latestServiceMode.value = service.mode.value
        latestServiceTime.value = serviceTime
        if (storeFlow.value.enableBlockA11yAppList && !actualBlockA11yAppList.contains(topAppIdFlow.value)) {
            startQueryJob(byForced = true)
        }
        runMainPost(1000L) {// 共存 1000ms, 等待另一个服务稳定
            if (latestServiceTime.value == serviceTime) {
                when (service.mode) {
                    AutomatorModeOption.A11yMode -> uiAutomationFlow.value?.shutdown(true)
                    AutomatorModeOption.AutomationMode -> A11yService.instance?.shutdown(true)
                }
            }
        }
    }

    fun onScreenForcedActive() {
        // 关闭屏幕 -> Activity::onStop -> 点亮屏幕 -> Activity::onStart -> Activity::onResume
        val a = topActivityFlow.value
        synchronized(topActivityFlow) {
            updateTopActivity(
                a.appId,
                a.activityId,
                scene = ActivityScene.ScreenOn
            )
        }
        startQueryJob()
    }

    val safeActiveWindow: AccessibilityNodeInfo?
        get() = try {
            // 某些应用耗时 554ms
            // java.lang.SecurityException: Call from user 0 as user -2 without permission INTERACT_ACROSS_USERS or INTERACT_ACROSS_USERS_FULL not allowed.
            service.windowNodeInfo?.setGeneratedTime()
        } catch (_: Throwable) {
            null
        }.apply {
            a11yContext.rootCache.value = this
        }

    val safeActiveWindowAppId: String?
        get() = safeActiveWindow?.packageName?.toString()

    private val scope get() = service.scope

    private fun traceDecision(
        correlationId: Long?,
        stage: DecisionStage,
        outcome: DecisionOutcome,
        reason: DecisionReason,
        rule: ResolvedRule? = null,
        detail: String? = null,
        appId: String? = null,
        activityId: String? = null,
    ) {
        if (correlationId == null) return
        val topActivity = topActivityFlow.value
        decisionTraceBuffer.append(
            correlationId = correlationId,
            stage = stage,
            outcome = outcome,
            reason = reason,
            appId = appId ?: topActivity.appId.takeIf { it.isNotEmpty() },
            activityId = activityId ?: topActivity.activityId,
            ruleId = rule?.diagnosticId,
            detail = detail,
        )
    }

    private fun traceRuleStatus(correlationId: Long?, rule: ResolvedRule, status: RuleStatus) {
        if (correlationId == null) return
        val reason = when (status) {
            RuleStatus.StatusOk -> DecisionReason.RuleEligible
            RuleStatus.Status1 -> DecisionReason.ActionMaximumReached
            RuleStatus.Status2 -> DecisionReason.PrerequisiteUnsatisfied
            RuleStatus.Status3 -> DecisionReason.MatchDelayActive
            RuleStatus.Status4 -> DecisionReason.MatchTimeout
            RuleStatus.Status5 -> DecisionReason.CooldownActive
            RuleStatus.Status6 -> DecisionReason.ActionDelayActive
        }
        traceDecision(
            correlationId = correlationId,
            stage = DecisionStage.Rule,
            outcome = if (status.ok) DecisionOutcome.Observed else DecisionOutcome.Skipped,
            reason = reason,
            rule = rule,
        )
    }

    @Volatile
    private var latestStateEvent: A11yEvent? = null
    private var lastContentEventTime = 0L
    private var lastEventTime = 0L
    private val eventDeque = ArrayDeque<A11yEvent>()
    fun onA11yEvent(event: AccessibilityEvent?) {
        if (!effective) return
        if (!event.isUseful()) return
        // 拒绝副屏无障碍事件
        if (AndroidTarget.TIRAMISU && event.displayId != Display.DEFAULT_DISPLAY) return
        onA11yFeatEvent(event)
        if (event.eventType == CONTENT_CHANGED) {
            if (!isInteractive) return // 屏幕关闭后仍然有无障碍事件 type:2048, time:8094, app:com.miui.aod, cls:android.widget.TextView
            if (event.packageName == systemUiAppId && event.packageName != topActivityFlow.value.appId) return
        }
        // 过滤部分输入法事件
        if (event.packageName == imeAppId && topActivityFlow.value.appId != imeAppId) {
            if (event.recordCount == 0 && event.action == 0 && !event.isFullScreen) return
        }
        // 直接丢弃自身事件，自行更新 topActivity
        if ((event.eventType == CONTENT_CHANGED || !isActivityVisible) && event.packageName == META.appId) return

        val a11yEvent = event.toA11yEvent() ?: return
        if (a11yEvent.type == CONTENT_CHANGED) {
            // 防止 content 类型事件过快
            if (a11yEvent.time - lastContentEventTime < 100 && a11yEvent.time - appChangeTime > 5000 && a11yEvent.time - lastTriggerTime > 3000) {
                return
            }
            lastContentEventTime = a11yEvent.time
        }
        EventService.logEvent(event)
        if (META.debuggable) {
            Log.d(
                "onNewA11yEvent",
                "type:${event.eventType}, time:${event.eventTime - lastEventTime}, app:${event.packageName}, cls:${event.className}"
            )
        }
        if (event.eventTime < lastEventTime) {
            // 某些应用会发送负时间事件, 直接丢弃
            // type:32, time:-104, app:com.miui.home, cls:com.miui.home.launcher.Launcher
            return
        }
        lastEventTime = event.eventTime
        if (event.eventType == STATE_CHANGED) {
            latestStateEvent = a11yEvent
        }
        synchronized(eventDeque) { eventDeque.addLast(a11yEvent) }
        scope.launch(eventDispatcher) { consumeEvent(a11yEvent) }
    }

    private val queryEvents = QueryEventBuffer<A11yEvent> { first, second ->
        first.sameAs(second)
    }
    private suspend fun consumeEvent(headEvent: A11yEvent) {
        val consumedEvents = synchronized(eventDeque) {
            if (eventDeque.firstOrNull() !== headEvent) return
            // Only coalesce the contiguous prefix. Filtering the whole deque and
            // then removing by count can accidentally discard interleaved events.
            eventDeque.removeMatchingPrefix { it.sameAs(headEvent) }
        }
        val latestEvent = consumedEvents.last()
        val decisionId = decisionTraceBuffer.newCorrelationId()
        traceDecision(
            correlationId = decisionId,
            stage = DecisionStage.Event,
            outcome = DecisionOutcome.Observed,
            reason = DecisionReason.EventReceived,
            appId = latestEvent.appId,
            activityId = latestEvent.name,
            detail = "type=${latestEvent.type} coalesced=${consumedEvents.size}",
        )
        val evAppId = latestEvent.appId
        val evActivityId = latestEvent.name
        val oldAppId = topActivityFlow.value.appId
        val rightAppId = if (oldAppId == evAppId) {
            evAppId
        } else {
            getTimeoutAppId() ?: run {
                traceDecision(
                    correlationId = decisionId,
                    stage = DecisionStage.Foreground,
                    outcome = DecisionOutcome.Skipped,
                    reason = DecisionReason.ForegroundUnconfirmed,
                    appId = evAppId,
                    activityId = evActivityId,
                )
                return
            }
        }
        if (rightAppId == evAppId) {
            if (latestEvent.type == STATE_CHANGED) {
                synchronized(topActivityFlow) {
                    // tv.danmaku.bili, com.miui.home, com.miui.home.launcher.Launcher
                    if (isActivity(evAppId, evActivityId)) {
                        updateTopActivity(evAppId, evActivityId)
                    }
                }
            }
        }
        if (rightAppId != topActivityFlow.value.appId) {
            synchronized(topActivityFlow) {
                // 从 锁屏，下拉通知栏 返回等情况, 应用不会发送事件, 但是系统组件会发送事件
                val topCpn = shizukuContextFlow.value.topCpn()
                if (topCpn?.packageName == rightAppId) {
                    updateTopActivity(topCpn.packageName, topCpn.className)
                } else {
                    updateTopActivity(rightAppId, null)
                }
            }
        }
        traceDecision(
            correlationId = decisionId,
            stage = DecisionStage.Foreground,
            outcome = DecisionOutcome.Observed,
            reason = DecisionReason.ForegroundConfirmed,
            appId = rightAppId,
            activityId = topActivityFlow.value.activityId,
        )
        val activityRule = activityRuleFlow.value
        if (evAppId != rightAppId) {
            traceDecision(
                correlationId = decisionId,
                stage = DecisionStage.Foreground,
                outcome = DecisionOutcome.Skipped,
                reason = DecisionReason.PackageActivityMismatch,
                appId = evAppId,
                activityId = evActivityId,
                detail = "foreground=$rightAppId",
            )
            return
        }
        if (!storeFlow.value.enableMatch) {
            traceDecision(
                correlationId = decisionId,
                stage = DecisionStage.Query,
                outcome = DecisionOutcome.Skipped,
                reason = DecisionReason.AutoMatchDisabled,
            )
            return
        }
        if (activityRule.currentRules.isEmpty()) {
            traceDecision(
                correlationId = decisionId,
                stage = DecisionStage.Rule,
                outcome = if (rightAppId == META.appId) DecisionOutcome.Observed else DecisionOutcome.Skipped,
                reason = DecisionReason.NoApplicableRules,
                appId = rightAppId,
                activityId = activityRule.topActivity.activityId,
            )
            return
        }
        if (activityRule.skipConsumeEvent) {
            traceDecision(
                correlationId = decisionId,
                stage = DecisionStage.Rule,
                outcome = DecisionOutcome.Skipped,
                reason = DecisionReason.MatchingPaused,
            )
            activityRule.currentRules.forEach { rule ->
                traceRuleStatus(decisionId, rule, rule.status)
            }
            return
        }
        queryEvents.addAll(consumedEvents)
        a11yContext.interruptKey++
        startQueryJob(byEvent = latestEvent, decisionId = decisionId)
    }

    private var lastGetAppIdTime = 0L
    private var lastAppId: String? = null
    private suspend fun getTimeoutAppId(): String? {
        if (lastAppId != null && System.currentTimeMillis() - lastGetAppIdTime <= 100) return lastAppId
        // 某些应用通过无障碍获取 safeActiveWindow 耗时长，导致多个事件连续堆积堵塞，无法检测到 appId 切换导致状态异常
        // https://github.com/gkd-kit/gkd/issues/622
        lastAppId = withTimeoutOrNull(100) {
            runInterruptible(Dispatchers.IO) { safeActiveWindowAppId }
        } ?: shizukuContextFlow.value.topCpn()?.packageName
        lastGetAppIdTime = System.currentTimeMillis()
        return lastAppId
    }

    // 某些场景耗时 5000 ms
    private suspend fun getTimeoutActiveWindow(): AccessibilityNodeInfo? {
        return suspendCancellableCoroutine { s ->
            val temp = atomic<Continuation<AccessibilityNodeInfo?>?>(s)
            scope.launch(Dispatchers.IO) {
                delay(500L)
                if (s.isActive) {
                    temp.getAndUpdate { null }?.resume(null)
                }
            }
            scope.launch(Dispatchers.IO) {
                val a = safeActiveWindow
                if (s.isActive) {
                    temp.getAndUpdate { null }?.resume(a)
                }
            }
        }
    }

    private val queryWakeState = QueryWakeState<A11yEvent, ResolvedRule>()

    private fun startQueryJob(
        byEvent: A11yEvent? = null,
        byForced: Boolean = false,
        byDelayRule: ResolvedRule? = null,
        decisionId: Long? = null,
    ) {
        val traceId = decisionId ?: decisionTraceBuffer.newCorrelationId()
        if (!effective) {
            traceDecision(traceId, DecisionStage.Query, DecisionOutcome.Skipped, DecisionReason.ServiceDisconnected)
            return
        }
        if (!storeFlow.value.enableMatch) {
            traceDecision(traceId, DecisionStage.Query, DecisionOutcome.Skipped, DecisionReason.AutoMatchDisabled)
            return
        }
        if (activityRuleFlow.value.currentRules.isEmpty()) {
            traceDecision(
                traceId,
                DecisionStage.Rule,
                if (topActivityFlow.value.appId == META.appId) DecisionOutcome.Observed else DecisionOutcome.Skipped,
                DecisionReason.NoApplicableRules,
            )
            return
        }
        // 无障碍从零启动时获取 safeActiveWindow 非常耗时
        if (byEvent == null && service.justStarted && !hasOthersService) {
            traceDecision(traceId, DecisionStage.Query, DecisionOutcome.Skipped, DecisionReason.ServiceStarting)
            return checkFutureStartJob()
        }
        val request = QueryWakeRequest(
            event = byEvent,
            forced = byForced,
            delayRule = byDelayRule,
            correlationId = traceId,
        )
        if (!queryWakeState.request(request)) {
            traceDecision(
                traceId,
                DecisionStage.Query,
                DecisionOutcome.Observed,
                DecisionReason.QueryDeferred,
                detail = "event=${byEvent != null} forced=$byForced delayRule=${byDelayRule != null}",
            )
            return
        }
        launchQueryJob(request)
    }

    private fun launchQueryJob(request: QueryWakeRequest<A11yEvent, ResolvedRule>) {
        val traceId = request.correlationId
        traceDecision(traceId, DecisionStage.Query, DecisionOutcome.Observed, DecisionReason.QueryStarted)
        scope.launchTry(queryDispatcher) {
            val st = if (META.debuggable) System.currentTimeMillis() else 0L
            try {
                if (META.debuggable) {
                    Log.d(
                        "A11yRuleEngine",
                        "startQueryJob start byEvent=${request.event != null}, byForced=${request.forced}, byDelayRule=${request.delayRule != null}"
                    )
                }
                queryAction(traceId, request.event, request.forced, request.delayRule)
            } finally {
                val pendingRequest = queryWakeState.complete()
                checkFutureStartJob()
                if (META.debuggable) {
                    val et = System.currentTimeMillis() - st
                    Log.d("A11yRuleEngine", "startQueryJob end $et ms")
                }
                if (pendingRequest != null) {
                    launchQueryJob(pendingRequest)
                }
            }
        }
    }

    private fun checkFutureStartJob() {
        val t = System.currentTimeMillis()
        if (t - lastTriggerTime < 3000L || t - appChangeTime < 3000L) {
            scope.launch(actionDispatcher) {
                delay(300)
                startQueryJob()
            }
        } else if (activityRuleFlow.value.hasFeatureAction) {
            scope.launch(actionDispatcher) {
                delay(300)
                startQueryJob(byForced = true)
            }
        }
    }

    private fun fixAppId(rightAppId: String) {
        if (topActivityFlow.value.appId == rightAppId) return
        synchronized(topActivityFlow) {
            val topCpn = shizukuContextFlow.value.topCpn()
            if (topCpn?.packageName == rightAppId) {
                updateTopActivity(topCpn.packageName, topCpn.className)
            } else {
                updateTopActivity(rightAppId, null)
            }
        }
        scope.launch(actionDispatcher) {
            delay(300)
            startQueryJob()
        }
    }

    private suspend fun queryAction(
        decisionId: Long?,
        byEvent: A11yEvent? = null,
        byForced: Boolean = false,
        delayRule: ResolvedRule? = null,
    ) {
        val tempStateEvent = latestStateEvent
        val newEvents = if (delayRule != null) {// 延迟规则不消耗事件
            null
        } else {
            val eventBatch = queryEvents.drain()
            if (byEvent != null && !eventBatch.hadEvents) {
                traceDecision(
                    decisionId,
                    DecisionStage.Event,
                    DecisionOutcome.Skipped,
                    DecisionReason.EventQueueEmpty,
                )
                return
            }
            // Mixed events deliberately fall back to a fresh root query.
            eventBatch.events?.toTypedArray()
        }
        val activityRule = synchronized(topActivityFlow) { activityRuleFlow.value }
        activityRule.currentRules.forEach { rule ->
            if (rule.status == RuleStatus.Status3 && rule.matchDelayJob.value == null) {
                rule.matchDelayJob.value = scope.launch(actionDispatcher) {
                    delay(rule.matchDelay)
                    rule.matchDelayJob.value = null
                    startQueryJob(byDelayRule = rule)
                }
            }
        }
        if (activityRule.skipMatch) {
            // 如果当前应用没有规则/暂停匹配, 则不去调用获取事件节点避免阻塞
            activityRule.currentRules.forEach { rule ->
                traceRuleStatus(decisionId, rule, rule.status)
            }
            return
        }
        var lastNode = if (newEvents == null || newEvents.size <= 1) {
            newEvents?.firstOrNull()?.safeSource
        } else {
            // 获取最后两个事件, 如果最后两个事件的节点不一致, 则丢弃
            // 相等则是同一个节点发出的连续事件, 常见于倒计时界面
            val lastNode = newEvents.last().safeSource
            if (lastNode == null || lastNode == newEvents[0].safeSource) {
                lastNode
            } else {
                null
            }
        }
        var lastNodeUsed = false
        if (!a11yContext.clearOldAppNodeCache()) {
            if (byEvent != null) { // 此为多数情况
                // 新事件到来时, 若缓存清理不及时会导致无法查询到节点
                a11yContext.clearNodeCache(lastNode)
            }
        }
        for (rule in activityRule.priorityRules) { // 规则数量有可能过多导致耗时过长
            if (!effective) {
                traceDecision(decisionId, DecisionStage.Query, DecisionOutcome.Skipped, DecisionReason.ServiceDisconnected, rule)
                return
            }
            if (checkOutDate(activityRule, tempStateEvent)) {
                traceDecision(decisionId, DecisionStage.Foreground, DecisionOutcome.Skipped, DecisionReason.StaleContext, rule)
                break
            }
            if (delayRule != null && delayRule !== rule) continue
            val status = rule.status
            if (status != RuleStatus.StatusOk) {
                traceRuleStatus(decisionId, rule, status)
                continue
            }
            if (byForced && !rule.checkForced()) {
                traceDecision(decisionId, DecisionStage.Rule, DecisionOutcome.Skipped, DecisionReason.ForcedRuleSkipped, rule)
                continue
            }
            traceRuleStatus(decisionId, rule, status)
            lastNode?.let { n ->
                val refreshOk = (!lastNodeUsed) || (try {
                    val e = n.refresh()
                    if (e) {
                        n.setGeneratedTime()
                    }
                    e
                } catch (_: Throwable) {
                    false
                })
                lastNodeUsed = true
                if (!refreshOk) {
                    lastNode = null
                }
            }
            val nodeVal = lastNode ?: getTimeoutActiveWindow()
            if (nodeVal == null) {
                traceDecision(decisionId, DecisionStage.Window, DecisionOutcome.Skipped, DecisionReason.WindowRootUnavailable, rule)
                continue
            }
            traceDecision(decisionId, DecisionStage.Window, DecisionOutcome.Observed, DecisionReason.WindowRootAvailable, rule)
            val rightAppId = nodeVal.packageName?.toString()
            if (rightAppId == null) {
                traceDecision(decisionId, DecisionStage.Window, DecisionOutcome.Skipped, DecisionReason.WindowRootUnavailable, rule, "root packageName is null")
                break
            }
            val matchApp = rule.matchActivity(rightAppId)
            if (topActivityFlow.value.appId != rightAppId || (!matchApp && rule is AppRule)) {
                traceDecision(
                    decisionId,
                    DecisionStage.Foreground,
                    DecisionOutcome.Skipped,
                    DecisionReason.PackageActivityMismatch,
                    rule,
                    "root=$rightAppId matchApp=$matchApp",
                )
                scope.launch(eventDispatcher) { fixAppId(rightAppId) }
                return
            }
            if (!matchApp) {
                traceDecision(decisionId, DecisionStage.Foreground, DecisionOutcome.Skipped, DecisionReason.PackageActivityMismatch, rule, "root=$rightAppId")
                continue
            }
            val target = try {
                a11yContext.queryRule(rule, nodeVal)
            } catch (e: InterruptRuleMatchException) {
                traceDecision(decisionId, DecisionStage.Query, DecisionOutcome.Skipped, DecisionReason.StaleContext, rule, "priority interrupt")
                throw e
            }
            if (target == null) {
                traceDecision(decisionId, DecisionStage.Selector, DecisionOutcome.Skipped, DecisionReason.SelectorMiss, rule)
                continue
            }
            traceDecision(decisionId, DecisionStage.Selector, DecisionOutcome.Observed, DecisionReason.SelectorMatched, rule)
            if (rule.checkDelay() && rule.actionDelayJob.value == null) {
                traceDecision(decisionId, DecisionStage.Rule, DecisionOutcome.Skipped, DecisionReason.ActionDelayActive, rule)
                rule.actionDelayJob.value = scope.launch(actionDispatcher) {
                    delay(rule.actionDelay)
                    rule.actionDelayJob.value = null
                    startQueryJob(byDelayRule = rule)
                }
                continue
            }
            val actionStatus = rule.status
            if (actionStatus != RuleStatus.StatusOk) {
                traceRuleStatus(decisionId, rule, actionStatus)
                break
            }
            if (checkOutDate(activityRule, tempStateEvent)) {
                traceDecision(decisionId, DecisionStage.Foreground, DecisionOutcome.Skipped, DecisionReason.StaleContext, rule)
                break
            }
            traceDecision(decisionId, DecisionStage.Action, DecisionOutcome.Submitted, DecisionReason.ActionSubmitted, rule)
            val actionResult = rule.performAction(target)
            if (actionResult.result) {
                traceDecision(
                    decisionId,
                    DecisionStage.Action,
                    DecisionOutcome.Succeeded,
                    DecisionReason.ActionSucceeded,
                    rule,
                    "action=${actionResult.action}, state=${actionResult.state}",
                )
                val topActivity = topActivityFlow.value
                rule.trigger()
                scope.launch(actionDispatcher) {
                    delay(300)
                    startQueryJob()
                }
                if (actionResult.action != ActionPerformer.None.action) {
                    showActionToast(rule)
                }
                addActionLog(rule, topActivity, target, actionResult)
            } else {
                traceDecision(
                    decisionId,
                    DecisionStage.Action,
                    DecisionOutcome.Failed,
                    when (actionResult.state) {
                        ActionResultState.Cancelled,
                        ActionResultState.TimedOut -> DecisionReason.ActionCancelled

                        else -> DecisionReason.ActionRejected
                    },
                    rule,
                    "action=${actionResult.action}, state=${actionResult.state}",
                )
            }
        }
    }

    private fun checkOutDate(
        activityRule: ActivityRule,
        stateEvent: A11yEvent?
    ): Boolean {
        if (stateEvent !== latestStateEvent) return true
        synchronized(topActivityFlow) {
            if (activityRule !== activityRuleFlow.value) return true
        }
        return false
    }

    companion object {
        val service: A11yCommonImpl?
            get() = uiAutomationFlow.value?.takeIf {
                it.mode.value == latestServiceMode.value
            } ?: A11yService.instance
        val instance: A11yRuleEngine? get() = service?.ruleEngine

        fun compatWindows(): List<AccessibilityWindowInfo> {
            return try {
                service?.windowInfos
            } catch (_: Throwable) {
                null
            } ?: emptyList()
        }

        fun onScreenForcedActive() {
            instance?.onScreenForcedActive()
        }

        fun onRuleSummaryChanged() {
            instance?.startQueryJob(byForced = true)
        }

        fun performActionBack(): Boolean {
            val r1 = shizukuContextFlow.value.inputManager?.key(KeyEvent.KEYCODE_BACK)
            if (r1 == true) return true
            return A11yService.instance?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK) == true
        }

        suspend fun screenshot(): Bitmap? = service?.screenshot()

        suspend fun execAction(gkdAction: GkdAction): ActionResult {
            val selector = Selector.parseOrNull(gkdAction.selector) ?: throw RpcError("非法选择器")
            runCatching { selector.checkType(typeInfo) }.exceptionOrNull()?.let {
                throw RpcError("选择器类型错误:${it.message}")
            }
            val s = instance ?: throw RpcError("服务未连接")
            val a = s.safeActiveWindow ?: throw RpcError("界面没有节点信息")
            val targetNode = A11yContext(s, interruptable = false).querySelfOrSelector(
                a, selector, MatchOption(fastQuery = gkdAction.fastQuery)
            ) ?: throw RpcError("没有查询到节点")
            return withContext(Dispatchers.IO) {
                ActionPerformer
                    .getAction(gkdAction.action ?: ActionPerformer.None.action)
                    .perform(targetNode, gkdAction)
            }
        }

    }
}
