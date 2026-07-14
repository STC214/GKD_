package li.songe.gkd.shizuku


import android.app.ActivityManager
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.RemoteException
import androidx.annotation.WorkerThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import li.songe.gkd.app
import li.songe.gkd.appScope
import li.songe.gkd.isActivityVisible
import li.songe.gkd.permission.shizukuGrantedState
import li.songe.gkd.permission.updatePermissionState
import li.songe.gkd.service.ExposeService
import li.songe.gkd.service.StatusService
import li.songe.gkd.service.currentAppBlocked
import li.songe.gkd.service.currentAppUseA11y
import li.songe.gkd.service.updateTopTaskAppId
import li.songe.gkd.store.storeFlow
import li.songe.gkd.util.AndroidTarget
import li.songe.gkd.util.LogUtils
import li.songe.gkd.util.MutexState
import li.songe.gkd.util.launchTry
import li.songe.gkd.util.toast
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import kotlin.system.exitProcess

inline fun <T> safeInvokeShizuku(
    block: () -> T
): T? = try {
    block()
} catch (_: ShizukuOffException) {
    null
} catch (e: RemoteException) {
    e.printStackTrace()
    null
} catch (e: IllegalStateException) {
    // https://github.com/RikkaApps/Shizuku-API/blob/a27f6e4151ba7b39965ca47edb2bf0aeed7102e5/api/src/main/java/rikka/shizuku/Shizuku.java#L430
    if (e.message == "binder haven't been received") {
        null
    } else {
        throw e
    }
}

class ShizukuOffException : IllegalStateException("Shizuku is off")

fun getShizukuService(name: String): ShizukuBinderWrapper? {
    return SystemServiceHelper.getSystemService(name)?.let(::ShizukuBinderWrapper)
}

// https://github.com/android-cs/16/blob/main/packages/Shell/AndroidManifest.xml
private fun checkRemotePermission(permission: String): Boolean {
    return Shizuku.checkRemotePermission(permission) == PackageManager.PERMISSION_GRANTED
}

private val isAdbRestricted: Boolean
    get() {
        if (!checkRemotePermission("android.permission.GRANT_RUNTIME_PERMISSIONS")) {
            return true
        }
        if (AndroidTarget.P && !checkRemotePermission("android.permission.MANAGE_APP_OPS_MODES")) {
            return true
        }
        return false
    }

class ShizukuContext(
    serviceWrapper: UserServiceWrapper?,
    val packageManager: SafePackageManager?,
    val userManager: SafeUserManager?,
    val activityManager: SafeActivityManager?,
    val activityTaskManager: SafeActivityTaskManager?,
    val appOpsService: SafeAppOpsService?,
    val inputManager: SafeInputManager?,
    val a11yManager: SafeAccessibilityManager?,
    val wmManager: SafeWindowManager?,
) {
    @Volatile
    private var destroyed = false

    @Volatile
    var serviceWrapper: UserServiceWrapper? = serviceWrapper
        private set

    val ok get() = this !== defaultShizukuContext
    fun destroy() {
        val oldServiceWrapper = synchronized(this) {
            destroyed = true
            serviceWrapper.also { serviceWrapper = null }
        }
        oldServiceWrapper?.destroy()
        if (activityTaskManager != null) {
            activityTaskManager.unregisterDefault()
        } else {
            activityManager?.unregisterDefault()
        }
    }

    val states: List<Pair<String, Any?>>
        get() = listOf(
            "IUserService" to serviceWrapper,
            "IActivityManager" to activityManager,
            "IActivityTaskManager" to activityTaskManager,
            "IAppOpsService" to appOpsService,
            "IInputManager" to inputManager,
            "IPackageManager" to packageManager,
            "IUserManager" to userManager,
            "IAccessibilityManager" to a11yManager,
            "IWindowManager" to wmManager,
        )

    fun installServiceWrapper(wrapper: UserServiceWrapper): Boolean {
        synchronized(this) {
            if (destroyed || serviceWrapper != null) return false
            serviceWrapper = wrapper
        }
        onServiceWrapperReady(wrapper)
        return true
    }

    private fun onServiceWrapperReady(wrapper: UserServiceWrapper) {
        // 某些情况下存在残留进程
        val size = wrapper.userService.killLegacyService()
        if (size > 0) {
            LogUtils.d("killLegacyService $size")
        }
    }

    fun grantSelf() {
        packageManager ?: return
        appOpsService ?: return
        if (isAdbRestricted) return
        appOpsService.allowAllSelfMode()
        packageManager.allowAllSelfPermission()
    }

    @WorkerThread
    fun tap(x: Float, y: Float, duration: Long = 0): Boolean {
        return serviceWrapper?.tap(x, y, duration) ?: inputManager?.tap(x, y, duration) ?: false
    }

    fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, duration: Long): Boolean {
        return serviceWrapper?.swipe(x1, y1, x2, y2, duration) ?: inputManager?.swipe(
            x1,
            y1,
            x2,
            y2,
            duration
        ) ?: false
    }

    fun getTasks(maxNum: Int = 1): List<ActivityManager.RunningTaskInfo> {
        return activityTaskManager?.getTasks(maxNum)
            ?: activityManager?.getTasks(maxNum)
            ?: emptyList()
    }

    fun topCpn(): ComponentName? = getTasks().firstOrNull()?.topActivity

    init {
        if (activityTaskManager != null) {
            activityTaskManager.registerDefault()
        } else {
            activityManager?.registerDefault()
        }
        grantSelf()
        serviceWrapper?.let(::onServiceWrapperReady)
    }
}

private val defaultShizukuContext by lazy {
    ShizukuContext(
        serviceWrapper = null,
        packageManager = null,
        userManager = null,
        activityManager = null,
        activityTaskManager = null,
        appOpsService = null,
        inputManager = null,
        a11yManager = null,
        wmManager = null,
    )
}

val currentUserId by lazy { android.os.Process.myUserHandle().hashCode() }

val shizukuContextFlow by lazy { MutableStateFlow(defaultShizukuContext) }

val shizukuUsedFlow by lazy {
    combine(
        shizukuGrantedState.stateFlow,
        storeFlow.map { it.enableShizuku },
    ) { a, b ->
        a && b
    }.stateIn(appScope, SharingStarted.Eagerly, false)
}

val updateBinderMutex = MutexState()
val rootBridgeReconnectMutex = MutexState()
val rootBridgeDiagnosticsFlow = MutableStateFlow(RootBridgeDiagnostics())

private const val AUTO_ROOT_RECONNECT_ATTEMPTS = 2

private fun updateRootBridgeDiagnostics(
    context: ShizukuContext = shizukuContextFlow.value,
    attempt: Int = 0,
    maxAttempts: Int = 0,
    connectionResult: UserServiceConnectionResult? = null,
    checking: Boolean = false,
    exhausted: Boolean = false,
) {
    if (checking) {
        rootBridgeDiagnosticsFlow.value = rootBridgeDiagnosticsFlow.value.copy(
            phase = RootBridgePhase.Checking,
            attempt = attempt,
            maxAttempts = maxAttempts,
            failure = null,
            error = null,
        )
        return
    }
    if (!context.ok) {
        rootBridgeDiagnosticsFlow.value = RootBridgeDiagnostics(
            checkedAt = System.currentTimeMillis(),
        )
        return
    }
    val wrapper = context.serviceWrapper
    val idResult = wrapper?.execCommandForResult("id")
    val shellCommandAvailable = idResult?.ok == true
    val remoteUid = idResult?.result?.let(::parseRemoteUid)
    val effectiveExhausted = exhausted || (wrapper != null && !shellCommandAvailable)
    val previous = rootBridgeDiagnosticsFlow.value
    rootBridgeDiagnosticsFlow.value = RootBridgeDiagnostics(
        phase = rootBridgePhase(
            contextAvailable = true,
            userServiceConnected = wrapper != null,
            shellCommandAvailable = shellCommandAvailable,
            remoteUid = remoteUid,
            exhausted = effectiveExhausted,
        ),
        checkedAt = System.currentTimeMillis(),
        attempt = attempt,
        maxAttempts = maxAttempts,
        userServiceConnected = wrapper != null,
        shellCommandAvailable = shellCommandAvailable,
        remoteUid = remoteUid,
        binderAvailable = context.states.drop(1).count { it.second != null },
        uiAutomationConnected = uiAutomationFlow.value != null,
        topActivity = runCatching { context.topCpn()?.flattenToShortString() }.getOrNull(),
        failure = connectionResult?.failure ?: previous.failure.takeIf { wrapper == null },
        error = connectionResult?.error
            ?: idResult?.error?.takeIf { it.isNotBlank() }
            ?: previous.error.takeIf { wrapper == null },
    )
}

fun refreshRootBridgeDiagnostics() {
    appScope.launchTry(Dispatchers.IO) {
        updateRootBridgeDiagnostics()
    }
}

fun retryRootUserService(manual: Boolean = true) = rootBridgeReconnectMutex.launchTry(
    appScope,
    Dispatchers.IO,
) {
    val context = shizukuContextFlow.value
    if (!shizukuUsedFlow.value || !context.ok) {
        updateRootBridgeDiagnostics(context)
        if (manual) toast("Shizuku 服务未连接")
        return@launchTry
    }
    if (context.serviceWrapper != null) {
        updateRootBridgeDiagnostics(context)
        if (manual) toast("Root 用户服务已连接")
        return@launchTry
    }
    val maxAttempts = AUTO_ROOT_RECONNECT_ATTEMPTS + 1
    repeat(AUTO_ROOT_RECONNECT_ATTEMPTS) { index ->
        if (!shizukuUsedFlow.value || shizukuContextFlow.value !== context) {
            updateRootBridgeDiagnostics(shizukuContextFlow.value)
            return@launchTry
        }
        val attempt = index + 2
        updateRootBridgeDiagnostics(
            context = context,
            attempt = attempt,
            maxAttempts = maxAttempts,
            checking = true,
        )
        if (index > 0) delay(1000)
        if (!shizukuUsedFlow.value || shizukuContextFlow.value !== context) {
            updateRootBridgeDiagnostics(shizukuContextFlow.value)
            return@launchTry
        }
        val result = buildServiceWrapper()
        val wrapper = result.wrapper
        if (!shizukuUsedFlow.value || shizukuContextFlow.value !== context) {
            wrapper?.destroy()
            updateRootBridgeDiagnostics(shizukuContextFlow.value)
            return@launchTry
        }
        if (wrapper != null) {
            if (context.installServiceWrapper(wrapper)) {
                updateRootBridgeDiagnostics(
                    context = context,
                    attempt = attempt,
                    maxAttempts = maxAttempts,
                    connectionResult = result,
                )
                LogUtils.d("Root UserService reconnect success, attempt=$attempt/$maxAttempts")
                if (manual) toast("Root 用户服务连接成功")
                return@launchTry
            }
            wrapper.destroy()
            updateRootBridgeDiagnostics(shizukuContextFlow.value)
            return@launchTry
        }
        val exhausted = index == AUTO_ROOT_RECONNECT_ATTEMPTS - 1
        updateRootBridgeDiagnostics(
            context = context,
            attempt = attempt,
            maxAttempts = maxAttempts,
            connectionResult = result,
            exhausted = exhausted,
        )
        LogUtils.d(
            "Root UserService reconnect failed, attempt=$attempt/$maxAttempts, failure=${result.failure}, error=${result.error}"
        )
    }
    if (manual) toast("Root 用户服务连接失败")
}

private fun updateShizukuBinder() = updateBinderMutex.launchTry(appScope, Dispatchers.IO) {
    if (shizukuUsedFlow.value) {
        if (!app.justStarted) {
            toast("正在连接 Shizuku 服务...")
        }
        val userServiceResult = buildServiceWrapper()
        val shizukuContext = ShizukuContext(
            serviceWrapper = userServiceResult.wrapper,
            packageManager = SafePackageManager.newBinder(),
            userManager = SafeUserManager.newBinder(),
            activityManager = SafeActivityManager.newBinder(),
            activityTaskManager = SafeActivityTaskManager.newBinder(),
            appOpsService = SafeAppOpsService.newBinder(),
            inputManager = SafeInputManager.newBinder(),
            a11yManager = SafeAccessibilityManager.newBinder(),
            wmManager = SafeWindowManager.newBinder(),
        )
        shizukuContextFlow.value = shizukuContext
        updateRootBridgeDiagnostics(
            context = shizukuContext,
            attempt = 1,
            maxAttempts = AUTO_ROOT_RECONNECT_ATTEMPTS + 1,
            connectionResult = userServiceResult,
        )
        shizukuContext.topCpn()?.let { cpn ->
            updateTopTaskAppId(cpn.packageName)
        }
        if (
            storeFlow.value.useAutomation &&
            !currentAppBlocked &&
            !currentAppUseA11y
        ) {
            AutomationService.tryConnect(true)
        }
        updatePermissionState()
        if (StatusService.needRestart) {
            //
            shizukuContext.activityManager?.startForegroundService(ExposeService.exposeIntent(expose = -1))
        }
        val delayMillis = if (app.justStarted) 1200L else 0L
        if (shizukuContext.serviceWrapper == null) {
            if (shizukuContext.packageManager != null) {
                toast("Shizuku 服务连接部分失败", delayMillis = delayMillis)
            } else {
                toast("Shizuku 服务连接失败", delayMillis = delayMillis)
            }
            retryRootUserService(manual = false)
        } else {
            toast("Shizuku 服务连接成功", delayMillis = delayMillis)
        }
    } else if (shizukuContextFlow.value.ok) {
        val willRelaunch = uiAutomationFlow.value != null && !shizukuGrantedState.updateAndGet()
        if (willRelaunch) {
            // 需要重启应用让系统释放 UiAutomation
            killRelaunchApp()
        } else {
            uiAutomationFlow.value?.shutdown(true)
            shizukuContextFlow.value.destroy()
            shizukuContextFlow.value = defaultShizukuContext
            updateRootBridgeDiagnostics(defaultShizukuContext)
            toast("Shizuku 服务已断开")
        }
    }
}

private suspend fun killRelaunchApp() {
    if (isActivityVisible) {
        toast("Shizuku 断开，重启应用以释放自动化服务", forced = true)
        delay(1500)
        app.startLaunchActivity()
    } else {
        toast("Shizuku 断开，结束应用以释放自动化服务", forced = true)
        delay(1500)
    }
    android.os.Process.killProcess(android.os.Process.myPid())
    exitProcess(0)
}

fun initShizuku() {
    Shizuku.addBinderReceivedListener {
        LogUtils.d("Shizuku.addBinderReceivedListener")
        appScope.launchTry(Dispatchers.IO) {
            shizukuGrantedState.updateAndGet()
        }
    }
    Shizuku.addBinderDeadListener {
        LogUtils.d("Shizuku.addBinderDeadListener")
        shizukuGrantedState.stateFlow.value = false
    }
    appScope.launchTry {
        shizukuUsedFlow.collect { updateShizukuBinder() }
    }
}
