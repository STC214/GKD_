package li.songe.gkd.runtime.foreground

sealed interface ForegroundConfirmationResult {
    data class Accepted(val snapshot: ForegroundSnapshot) : ForegroundConfirmationResult
    data class Pending(
        val snapshot: ForegroundSnapshot,
        val retryAfterMillis: Long,
    ) : ForegroundConfirmationResult

    data class Rejected(val snapshot: ForegroundSnapshot) : ForegroundConfirmationResult
}

class ForegroundConfirmationState(
    private val confirmationMillis: Long = 150L,
    private val monotonicMillis: () -> Long = { System.nanoTime() / 1_000_000L },
) {
    init {
        require(confirmationMillis in 50L..300L)
    }

    private data class ContextKey(
        val taskId: Int?,
        val windowId: Int?,
        val taskAppId: String?,
        val windowAppId: String?,
        val confidence: ForegroundConfidence,
        val surface: ForegroundSurface,
    )

    private var pendingKey: ContextKey? = null
    private var pendingSince = 0L

    @Synchronized
    fun observe(snapshot: ForegroundSnapshot): ForegroundConfirmationResult {
        val now = monotonicMillis()
        if (snapshot.canExecute) {
            pendingKey = null
            return ForegroundConfirmationResult.Accepted(snapshot)
        }
        val key = ContextKey(
            taskId = snapshot.task?.taskId,
            windowId = snapshot.window?.windowId,
            taskAppId = snapshot.task?.appId,
            windowAppId = snapshot.window?.rootAppId,
            confidence = snapshot.confidence,
            surface = snapshot.surface,
        )
        if (pendingKey != key) {
            pendingKey = key
            pendingSince = now
            return ForegroundConfirmationResult.Pending(snapshot, confirmationMillis)
        }
        val elapsed = (now - pendingSince).coerceAtLeast(0L)
        if (elapsed < confirmationMillis) {
            return ForegroundConfirmationResult.Pending(snapshot, confirmationMillis - elapsed)
        }
        pendingKey = null
        return ForegroundConfirmationResult.Rejected(snapshot)
    }
}
