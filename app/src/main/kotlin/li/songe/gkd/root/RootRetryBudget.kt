package li.songe.gkd.root

/** Bound crash-loop retries, but renew the budget after a genuinely stable connection. */
internal class RootRetryBudget(
    private val maxRetries: Int = 2,
    private val stableMillis: Long = 30_000L,
) {
    private var attempts = 0
    private var connectedAt: Long? = null

    fun connected(nowElapsed: Long) { connectedAt = nowElapsed }

    fun consume(nowElapsed: Long): Int? {
        val since = connectedAt
        if (since != null && since >= 0 && nowElapsed >= since && nowElapsed - since >= stableMillis) {
            attempts = 0
        }
        connectedAt = null
        if (attempts >= maxRetries) return null
        return ++attempts
    }

    fun reset() { attempts = 0; connectedAt = null }
}
