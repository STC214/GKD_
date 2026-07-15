package li.songe.gkd.runtime.foreground

data class WindowContextToken(
    val generation: Long,
    val taskId: Int?,
    val windowId: Int?,
    val appId: String?,
    val displayId: Int,
    val rotation: Int,
)

/** Invalidates selector results whenever the observed window tree changes. */
class WindowGenerationState {
    private var generation = 0L
    private var displayContext: Pair<Int, Int>? = null

    @Synchronized
    fun advance(): Long {
        generation++
        return generation
    }

    @Synchronized
    fun capture(snapshot: ForegroundSnapshot): WindowContextToken {
        val newDisplayContext = snapshot.displayId to snapshot.rotation
        if (displayContext != null && displayContext != newDisplayContext) {
            generation++
        }
        displayContext = newDisplayContext
        return WindowContextToken(
            generation = generation,
            taskId = snapshot.task?.taskId,
            windowId = snapshot.window?.windowId,
            appId = snapshot.appId,
            displayId = snapshot.displayId,
            rotation = snapshot.rotation,
        )
    }

    @Synchronized
    fun isCurrent(token: WindowContextToken, snapshot: ForegroundSnapshot): Boolean {
        return token.generation == generation &&
                token.taskId == snapshot.task?.taskId &&
                token.windowId == snapshot.window?.windowId &&
                token.appId == snapshot.appId &&
                token.displayId == snapshot.displayId &&
                token.rotation == snapshot.rotation
    }
}
