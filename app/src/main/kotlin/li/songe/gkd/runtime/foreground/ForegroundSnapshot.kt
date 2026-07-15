package li.songe.gkd.runtime.foreground

enum class ForegroundConfidence {
    Confirmed,
    Probable,
    Conflict,
    Unavailable,
}

enum class ForegroundWindowKind {
    Application,
    InputMethod,
    System,
    AccessibilityOverlay,
    Other,
}

enum class ForegroundSurface {
    Application,
    InputMethod,
    SystemUi,
    PermissionController,
    PictureInPicture,
    AccessibilityOverlay,
    SystemOverlay,
    Unknown,
}

data class ForegroundWindow(
    val windowId: Int,
    val displayId: Int,
    val type: Int,
    val kind: ForegroundWindowKind,
    val layer: Int,
    val isFocused: Boolean,
    val isActive: Boolean,
    val rootAppId: String?,
    val isPictureInPicture: Boolean = false,
)

data class ForegroundSnapshot(
    val task: ForegroundTask?,
    val window: ForegroundWindow?,
    val appId: String?,
    val activityId: String?,
    val userId: Int?,
    val displayId: Int,
    val timestamp: Long,
    val confidence: ForegroundConfidence,
    val surface: ForegroundSurface,
) {
    val canExecute: Boolean
        get() = confidence == ForegroundConfidence.Confirmed &&
                surface == ForegroundSurface.Application

    /** Task-stack Activity is authoritative; accessibility events are fallback only. */
    fun canUseEventActivityFallback(eventAppId: String): Boolean {
        return activityId == null && appId == eventAppId
    }
}

private fun isPermissionController(appId: String?): Boolean {
    if (appId == null) return false
    return appId.endsWith(".permissioncontroller") ||
            appId.endsWith(".packageinstaller") ||
            appId == "com.lbe.security.miui"
}

private fun resolveForegroundSurface(
    task: ForegroundTask?,
    window: ForegroundWindow?,
    imeAppId: String,
    systemUiAppId: String,
): ForegroundSurface {
    val taskAppId = task?.appId
    val windowAppId = window?.rootAppId
    return when {
        window?.kind == ForegroundWindowKind.InputMethod ||
                (imeAppId.isNotEmpty() && windowAppId == imeAppId) -> ForegroundSurface.InputMethod

        windowAppId == systemUiAppId || taskAppId == systemUiAppId -> ForegroundSurface.SystemUi
        isPermissionController(windowAppId) || isPermissionController(taskAppId) -> ForegroundSurface.PermissionController
        task?.isPictureInPicture == true || window?.isPictureInPicture == true -> ForegroundSurface.PictureInPicture
        window?.kind == ForegroundWindowKind.AccessibilityOverlay -> ForegroundSurface.AccessibilityOverlay
        window?.kind == ForegroundWindowKind.System -> ForegroundSurface.SystemOverlay
        window?.kind == ForegroundWindowKind.Application -> ForegroundSurface.Application
        else -> ForegroundSurface.Unknown
    }
}

fun selectForegroundWindow(
    windows: List<ForegroundWindow>,
    targetDisplayId: Int,
): ForegroundWindow? {
    val displayWindows = windows.filter { it.displayId == targetDisplayId }
    // Android 16 may keep the host application window focused while a visible IME
    // is reported as neither focused nor active. AccessibilityService.windows only
    // contains interactive windows, so prefer a present IME explicitly and block
    // host actions until it disappears.
    return displayWindows.filter { it.kind == ForegroundWindowKind.InputMethod }
        .maxByOrNull { it.layer }
        ?: displayWindows.filter { it.isPictureInPicture }.maxByOrNull { it.layer }
        ?: displayWindows.filter { it.isFocused }.maxByOrNull { it.layer }
        ?: displayWindows.filter { it.isActive }.maxByOrNull { it.layer }
}

fun resolveForegroundSnapshot(
    task: ForegroundTask?,
    windows: List<ForegroundWindow>,
    targetDisplayId: Int,
    timestamp: Long,
    imeAppId: String = "",
    systemUiAppId: String = "com.android.systemui",
): ForegroundSnapshot {
    val window = selectForegroundWindow(windows, targetDisplayId)
    val taskAppId = task?.appId
    val windowAppId = window?.rootAppId
    val surface = resolveForegroundSurface(task, window, imeAppId, systemUiAppId)
    val confidence = when {
        taskAppId != null && windowAppId != null && taskAppId != windowAppId -> {
            ForegroundConfidence.Conflict
        }

        taskAppId != null &&
                windowAppId == taskAppId &&
                task.isFocused &&
                task.isVisible &&
                task.isRunning &&
                window.isFocused -> {
            ForegroundConfidence.Confirmed
        }

        taskAppId != null || windowAppId != null -> ForegroundConfidence.Probable
        else -> ForegroundConfidence.Unavailable
    }
    return ForegroundSnapshot(
        task = task,
        window = window,
        appId = windowAppId ?: taskAppId,
        activityId = task?.activityId,
        userId = task?.userId,
        displayId = targetDisplayId,
        timestamp = timestamp,
        confidence = confidence,
        surface = surface,
    )
}
