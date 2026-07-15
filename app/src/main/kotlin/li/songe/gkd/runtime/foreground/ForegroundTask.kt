package li.songe.gkd.runtime.foreground

data class ForegroundTask(
    val taskId: Int,
    val userId: Int,
    val effectiveUid: Int,
    val displayId: Int,
    val isFocused: Boolean,
    val isVisible: Boolean,
    val isRunning: Boolean,
    val appId: String?,
    val activityId: String?,
)

fun selectForegroundTask(
    tasks: List<ForegroundTask>,
    targetDisplayId: Int,
): ForegroundTask? {
    val displayTasks = tasks.filter { it.displayId == targetDisplayId }
    return displayTasks.firstOrNull { it.isFocused && it.isVisible && it.isRunning }
        ?: displayTasks.firstOrNull { it.isFocused && it.isRunning }
        ?: displayTasks.firstOrNull { it.isFocused }
        ?: displayTasks.firstOrNull { it.isVisible && it.isRunning }
        ?: displayTasks.firstOrNull { it.isRunning }
        ?: displayTasks.firstOrNull()
}
