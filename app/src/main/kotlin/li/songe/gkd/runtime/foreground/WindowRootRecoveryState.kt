package li.songe.gkd.runtime.foreground

data class WindowRootRecoveryContext(
    val taskId: Int?,
    val windowId: Int?,
    val appId: String?,
)

sealed interface WindowRootRecoveryResult {
    data object Inactive : WindowRootRecoveryResult

    data class Scheduled(
        val delayMillis: Long,
        val attempt: Int,
        val totalAttempts: Int,
    ) : WindowRootRecoveryResult

    data class Exhausted(val attempts: Int) : WindowRootRecoveryResult
}

/**
 * Finite recovery budget for the short interval after an explicit window transition.
 * A missing root never opens or extends the recovery interval by itself.
 */
class WindowRootRecoveryState(
    private val retryDelays: List<Long> = listOf(50L, 100L, 200L, 400L, 800L),
    private val transitionWindowMillis: Long = 3_000L,
    private val monotonicMillis: () -> Long = { System.nanoTime() / 1_000_000L },
) {
    init {
        require(retryDelays.isNotEmpty())
        require(retryDelays.all { it > 0L })
        require(transitionWindowMillis >= retryDelays.sum())
    }

    private var transitionActive = false
    private var transitionDeadline = 0L
    private var recoveryContext: WindowRootRecoveryContext? = null
    private var nextAttemptIndex = 0

    @Synchronized
    fun startTransition() {
        transitionActive = true
        transitionDeadline = monotonicMillis() + transitionWindowMillis
        recoveryContext = null
        nextAttemptIndex = 0
    }

    @Synchronized
    fun nextAttempt(context: WindowRootRecoveryContext): WindowRootRecoveryResult {
        if (!transitionActive) return WindowRootRecoveryResult.Inactive
        if (monotonicMillis() > transitionDeadline) {
            transitionActive = false
            return WindowRootRecoveryResult.Exhausted(nextAttemptIndex)
        }
        recoveryContext = context
        val delay = retryDelays.getOrNull(nextAttemptIndex) ?: run {
            transitionActive = false
            return WindowRootRecoveryResult.Exhausted(nextAttemptIndex)
        }
        nextAttemptIndex++
        return WindowRootRecoveryResult.Scheduled(
            delayMillis = delay,
            attempt = nextAttemptIndex,
            totalAttempts = retryDelays.size,
        )
    }

    @Synchronized
    fun complete() {
        transitionActive = false
        recoveryContext = null
        nextAttemptIndex = 0
    }
}
