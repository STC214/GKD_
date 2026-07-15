package li.songe.gkd.runtime.foreground

enum class ForegroundConfidence {
    Confirmed,
    Probable,
    Conflict,
    Unavailable,
}

data class ForegroundWindow(
    val windowId: Int,
    val displayId: Int,
    val type: Int,
    val layer: Int,
    val isFocused: Boolean,
    val isActive: Boolean,
    val rootAppId: String?,
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
) {
    val canExecute: Boolean get() = confidence == ForegroundConfidence.Confirmed
}

fun selectForegroundWindow(
    windows: List<ForegroundWindow>,
    targetDisplayId: Int,
): ForegroundWindow? {
    val displayWindows = windows.filter { it.displayId == targetDisplayId }
    return displayWindows.filter { it.isFocused }.maxByOrNull { it.layer }
        ?: displayWindows.filter { it.isActive }.maxByOrNull { it.layer }
}

fun resolveForegroundSnapshot(
    task: ForegroundTask?,
    windows: List<ForegroundWindow>,
    targetDisplayId: Int,
    timestamp: Long,
): ForegroundSnapshot {
    val window = selectForegroundWindow(windows, targetDisplayId)
    val taskAppId = task?.appId
    val windowAppId = window?.rootAppId
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
    )
}
