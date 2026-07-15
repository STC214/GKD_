package li.songe.gkd.data

import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import li.songe.gkd.a11y.A11yRuleEngine
import li.songe.gkd.service.A11yService
import li.songe.gkd.service.GestureDispatchResult
import li.songe.gkd.service.TrackService
import li.songe.gkd.shizuku.casted
import li.songe.gkd.shizuku.shizukuContextFlow
import li.songe.gkd.util.ScreenUtils

@Serializable
data class GkdAction(
    val selector: String,
    val fastQuery: Boolean = false,
    val action: String? = null,
    override val position: RawSubscription.Position? = null,
    override val swipeArg: RawSubscription.SwipeArg? = null,
) : RawSubscription.LocationProps

@Serializable
data class ActionResult(
    val action: String,
    val result: Boolean,
    val shell: Boolean = false,
    val position: Pair<Float, Float>? = null,
    val state: ActionResultState = if (result) {
        ActionResultState.Accepted
    } else {
        ActionResultState.Rejected
    },
    val target: ActionTarget = ActionTarget.Unknown,
    val backend: ActionBackend = ActionBackend.Unknown,
    val displayId: Int? = null,
    val windowId: Int? = null,
    val rotation: Int? = null,
    val windowBounds: ActionBounds? = null,
    val visibleBounds: ActionBounds? = null,
    val retryCount: Int = 0,
)

@Serializable
enum class ActionTarget {
    Unknown,
    Node,
    ClickableParent,
    Coordinates,
    Global,
    None,
}

@Serializable
enum class ActionBackend {
    Unknown,
    Node,
    Root,
    Accessibility,
    Global,
    None,
}

@Serializable
enum class ActionResultState {
    /** API 或节点接受了动作，但没有提供完成回调。 */
    Accepted,

    /** 输入序列或无障碍手势已完整执行。 */
    Completed,

    /** 预留给后续界面状态复核；不能由“已接受/已完成”推断。 */
    Verified,

    Cancelled,
    Rejected,
    TimedOut,
    StaleContext,
}

private fun GestureDispatchResult.toActionResultState() = when (this) {
    GestureDispatchResult.Completed -> ActionResultState.Completed
    GestureDispatchResult.Cancelled -> ActionResultState.Cancelled
    GestureDispatchResult.Rejected -> ActionResultState.Rejected
    GestureDispatchResult.TimedOut -> ActionResultState.TimedOut
}

private fun ActionResultState.isSuccessful() = when (this) {
    ActionResultState.Accepted,
    ActionResultState.Completed,
    ActionResultState.Verified -> true

    ActionResultState.Cancelled,
    ActionResultState.Rejected,
    ActionResultState.TimedOut,
    ActionResultState.StaleContext -> false
}

private suspend fun dispatchGesture(
    gestureDescription: GestureDescription,
    duration: Long,
): ActionResultState = A11yService.instance?.dispatchGestureAwait(
    gestureDescription,
    timeoutMillis = maxOf(3_000L, duration + 2_000L),
)?.toActionResultState() ?: ActionResultState.Rejected

sealed class ActionPerformer(val action: String) {
    abstract suspend fun perform(
        node: AccessibilityNodeInfo,
        locationProps: RawSubscription.LocationProps,
        context: ActionExecutionContext = ActionExecutionContext.fromNode(node),
        guard: ActionExecutionGuard = ActionExecutionGuard { true },
    ): ActionResult

    data object ClickNode : ActionPerformer("clickNode") {
        override suspend fun perform(
            node: AccessibilityNodeInfo,
            locationProps: RawSubscription.LocationProps,
            context: ActionExecutionContext,
            guard: ActionExecutionGuard,
        ): ActionResult {
            TrackService.addA11yNodePosition(node)
            return ActionResult(
                action = action,
                result = node.performAction(AccessibilityNodeInfo.ACTION_CLICK),
                target = ActionTarget.Node,
                backend = ActionBackend.Node,
                displayId = context.displayId,
                windowId = context.windowId,
                rotation = context.rotation,
            )
        }
    }

    data object ClickCenter : ActionPerformer("clickCenter") {
        override suspend fun perform(
            node: AccessibilityNodeInfo,
            locationProps: RawSubscription.LocationProps,
            context: ActionExecutionContext,
            guard: ActionExecutionGuard,
        ): ActionResult {
            val rect = node.casted.boundsInScreen
            val p = locationProps.position?.calc(rect)
            val x = p?.first ?: ((rect.right + rect.left) / 2f)
            val y = p?.second ?: ((rect.bottom + rect.top) / 2f)
            if (!ScreenUtils.inScreen(x, y) || !context.allowsPoint(x, y)) {
                return ActionResult(
                    action = action,
                    result = false,
                    position = x to y,
                    target = ActionTarget.Coordinates,
                    displayId = context.displayId,
                    windowId = context.windowId,
                    rotation = context.rotation,
                )
            }
            TrackService.addXyPosition(x, y)
            if (shizukuContextFlow.value.tap(x, y, displayId = context.displayId)) {
                return ActionResult(
                    action = action,
                    result = true,
                    shell = true,
                    position = x to y,
                    state = ActionResultState.Completed,
                    target = ActionTarget.Coordinates,
                    backend = ActionBackend.Root,
                    displayId = context.displayId,
                    windowId = context.windowId,
                    rotation = context.rotation,
                )
            }
            if (!guard.isCurrent()) return ActionExecutor.staleResult(this, context)
            if (!context.supportsAccessibilityGesture) {
                return ActionResult(
                    action = action,
                    result = false,
                    position = x to y,
                    target = ActionTarget.Coordinates,
                    backend = ActionBackend.Accessibility,
                    displayId = context.displayId,
                    windowId = context.windowId,
                    rotation = context.rotation,
                )
            }
            val path = Path().apply { moveTo(x, y) }
            val gestureDescription = GestureDescription.Builder()
                .addStroke(
                    GestureDescription.StrokeDescription(
                        path, 0, ViewConfiguration.getTapTimeout().toLong()
                    )
                )
                .build()
            val state = dispatchGesture(
                gestureDescription,
                ViewConfiguration.getTapTimeout().toLong(),
            )
            return ActionResult(
                action = action,
                result = state.isSuccessful(),
                position = x to y,
                state = state,
                target = ActionTarget.Coordinates,
                backend = ActionBackend.Accessibility,
                displayId = context.displayId,
                windowId = context.windowId,
                rotation = context.rotation,
            )
        }
    }

    data object Click : ActionPerformer("click") {
        override suspend fun perform(
            node: AccessibilityNodeInfo,
            locationProps: RawSubscription.LocationProps,
            context: ActionExecutionContext,
            guard: ActionExecutionGuard,
        ): ActionResult {
            return ActionExecutor.execute(this, node, locationProps, context, guard)
        }
    }

    data object LongClickNode : ActionPerformer("longClickNode") {
        override suspend fun perform(
            node: AccessibilityNodeInfo,
            locationProps: RawSubscription.LocationProps,
            context: ActionExecutionContext,
            guard: ActionExecutionGuard,
        ): ActionResult {
            TrackService.addA11yNodePosition(node)
            return ActionResult(
                action = action,
                result = node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK).apply {
                    if (this) {
                        delay(LongClickCenter.LONG_DURATION)
                    }
                },
                target = ActionTarget.Node,
                backend = ActionBackend.Node,
                displayId = context.displayId,
                windowId = context.windowId,
                rotation = context.rotation,
            )
        }
    }

    data object LongClickCenter : ActionPerformer("longClickCenter") {
        const val LONG_DURATION = 500L
        override suspend fun perform(
            node: AccessibilityNodeInfo,
            locationProps: RawSubscription.LocationProps,
            context: ActionExecutionContext,
            guard: ActionExecutionGuard,
        ): ActionResult {
            val rect = node.casted.boundsInScreen
            val p = locationProps.position?.calc(rect)
            val x = p?.first ?: ((rect.right + rect.left) / 2f)
            val y = p?.second ?: ((rect.bottom + rect.top) / 2f)
            // 某些系统的 ViewConfiguration.getLongPressTimeout() 返回 300 , 这将导致触发普通的 click 事件
            if (!ScreenUtils.inScreen(x, y) || !context.allowsPoint(x, y)) {
                return ActionResult(
                    action = action,
                    result = false,
                    position = x to y,
                    target = ActionTarget.Coordinates,
                    displayId = context.displayId,
                    windowId = context.windowId,
                    rotation = context.rotation,
                )
            }
            TrackService.addXyPosition(x, y)
            if (shizukuContextFlow.value.tap(
                    x,
                    y,
                    LONG_DURATION,
                    context.displayId,
                )
            ) {
                return ActionResult(
                    action = action,
                    result = true,
                    shell = true,
                    position = x to y,
                    state = ActionResultState.Completed,
                    target = ActionTarget.Coordinates,
                    backend = ActionBackend.Root,
                    displayId = context.displayId,
                    windowId = context.windowId,
                    rotation = context.rotation,
                )
            }
            if (!guard.isCurrent()) return ActionExecutor.staleResult(this, context)
            if (!context.supportsAccessibilityGesture) {
                return ActionResult(
                    action = action,
                    result = false,
                    position = x to y,
                    target = ActionTarget.Coordinates,
                    backend = ActionBackend.Accessibility,
                    displayId = context.displayId,
                    windowId = context.windowId,
                    rotation = context.rotation,
                )
            }
            val path = Path().apply { moveTo(x, y) }
            val gestureDescription = GestureDescription.Builder()
                .addStroke(
                    GestureDescription.StrokeDescription(
                        path, 0, LONG_DURATION
                    )
                )
                .build()
            val state = dispatchGesture(gestureDescription, LONG_DURATION)
            return ActionResult(
                action = action,
                result = state.isSuccessful(),
                position = x to y,
                state = state,
                target = ActionTarget.Coordinates,
                backend = ActionBackend.Accessibility,
                displayId = context.displayId,
                windowId = context.windowId,
                rotation = context.rotation,
            )
        }
    }

    data object LongClick : ActionPerformer("longClick") {
        override suspend fun perform(
            node: AccessibilityNodeInfo,
            locationProps: RawSubscription.LocationProps,
            context: ActionExecutionContext,
            guard: ActionExecutionGuard,
        ): ActionResult {
            return ActionExecutor.execute(this, node, locationProps, context, guard)
        }
    }

    data object Back : ActionPerformer("back") {
        override suspend fun perform(
            node: AccessibilityNodeInfo,
            locationProps: RawSubscription.LocationProps,
            context: ActionExecutionContext,
            guard: ActionExecutionGuard,
        ): ActionResult {
            return ActionResult(
                action = action,
                result = A11yRuleEngine.performActionBack(),
                target = ActionTarget.Global,
                backend = ActionBackend.Global,
                displayId = context.displayId,
                windowId = context.windowId,
                rotation = context.rotation,
            )
        }
    }

    data object None : ActionPerformer("none") {
        override suspend fun perform(
            node: AccessibilityNodeInfo,
            locationProps: RawSubscription.LocationProps,
            context: ActionExecutionContext,
            guard: ActionExecutionGuard,
        ): ActionResult {
            return ActionResult(
                action = action,
                result = true,
                target = ActionTarget.None,
                backend = ActionBackend.None,
                displayId = context.displayId,
                windowId = context.windowId,
                rotation = context.rotation,
            )
        }
    }

    data object Swipe : ActionPerformer("swipe") {
        override suspend fun perform(
            node: AccessibilityNodeInfo,
            locationProps: RawSubscription.LocationProps,
            context: ActionExecutionContext,
            guard: ActionExecutionGuard,
        ): ActionResult {
            val rect = node.casted.boundsInScreen
            val swipeArg = locationProps.swipeArg ?: return ActionResult(
                action = action,
                result = false,
            )
            val startP = swipeArg.start.calc(rect)
            val endP = swipeArg.end?.calc(rect) ?: startP
            if (startP == null || endP == null) {
                return ActionResult(
                    action = action,
                    result = false,
                )
            }
            val startX = startP.first
            val startY = startP.second
            val endX = endP.first
            val endY = endP.second
            if (!(ScreenUtils.inScreen(startX, startY) && ScreenUtils.inScreen(endX, endY)) ||
                !context.allowsPoint(startX, startY) || !context.allowsPoint(endX, endY)
            ) {
                return ActionResult(
                    action = action,
                    result = false,
                    position = endX to endY,
                    target = ActionTarget.Coordinates,
                    displayId = context.displayId,
                    windowId = context.windowId,
                    rotation = context.rotation,
                )
            }
            TrackService.addSwipePosition(startX, startY, endX, endY, swipeArg.duration)
            return if (shizukuContextFlow.value.swipe(
                    startX,
                    startY,
                    endX,
                    endY,
                    swipeArg.duration,
                    context.displayId,
                )
            ) {
                ActionResult(
                    action = action,
                    result = true,
                    shell = true,
                    position = endX to endY,
                    state = ActionResultState.Completed,
                    target = ActionTarget.Coordinates,
                    backend = ActionBackend.Root,
                    displayId = context.displayId,
                    windowId = context.windowId,
                    rotation = context.rotation,
                )
            } else {
                if (!guard.isCurrent()) return ActionExecutor.staleResult(this, context)
                if (!context.supportsAccessibilityGesture) {
                    return ActionResult(
                        action = action,
                        result = false,
                        position = endX to endY,
                        target = ActionTarget.Coordinates,
                        backend = ActionBackend.Accessibility,
                        displayId = context.displayId,
                        windowId = context.windowId,
                        rotation = context.rotation,
                    )
                }
                val gestureDescription = GestureDescription.Builder()
                val path = Path()
                path.moveTo(startX, startY)
                path.lineTo(endX, endY)
                gestureDescription.addStroke(
                    GestureDescription.StrokeDescription(
                        path, 0, swipeArg.duration
                    )
                )
                val state = dispatchGesture(gestureDescription.build(), swipeArg.duration)
                ActionResult(
                    action = action,
                    result = state.isSuccessful(),
                    position = endX to endY,
                    state = state,
                    target = ActionTarget.Coordinates,
                    backend = ActionBackend.Accessibility,
                    displayId = context.displayId,
                    windowId = context.windowId,
                    rotation = context.rotation,
                )
            }
        }
    }

    companion object {
        private val allSubObjects by lazy {
            arrayOf(
                ClickNode,
                ClickCenter,
                Click,
                LongClickNode,
                LongClickCenter,
                LongClick,
                Back,
                None,
                Swipe,
            )
        }

        fun getAction(action: String?): ActionPerformer {
            return allSubObjects.find { it.action == action } ?: Click
        }
    }
}
