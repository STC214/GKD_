package li.songe.gkd.runtime.foreground

data class RootMismatchContext(
    val taskId: Int?,
    val windowId: Int?,
    val foregroundAppId: String?,
    val rootAppId: String,
)

/** Allows one delayed re-sample for each distinct foreground/root mismatch. */
class RootMismatchRetryState {
    private var attemptedContext: RootMismatchContext? = null

    @Synchronized
    fun request(context: RootMismatchContext): Boolean {
        if (attemptedContext == context) return false
        attemptedContext = context
        return true
    }

    @Synchronized
    fun clear() {
        attemptedContext = null
    }
}
