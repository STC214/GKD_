package li.songe.gkd.root

internal fun shouldWaitForInitialModuleRoot(owner: Boolean, marker: String?, completed: Boolean) =
    owner && marker == "1" && !completed

/** First module setup only. Never poll indefinitely after a missing/denied Root grant. */
internal class RootPermissionWaitBudget(
    private val maxAttempts: Int = 20,
    private val maxMillis: Long = 300_000L,
    private val intervalMillis: Long = 15_000L,
) {
    init { require(maxAttempts >= 0 && maxMillis > 0 && intervalMillis > 0) }
    private var start: Long? = null
    private var attempts = 0
    fun withinWindow(now: Long): Boolean {
        val first = start ?: return false
        return now >= first && now - first <= maxMillis
    }
    fun consumeDelay(now: Long): Long? {
        if (now < 0) return null
        val first = start ?: now.also { start = it }
        if (!withinWindow(now) || now - first >= maxMillis || attempts >= maxAttempts) return null
        attempts++
        return minOf(intervalMillis, maxMillis - (now - first))
    }
    fun reset() { start = null; attempts = 0 }
}
