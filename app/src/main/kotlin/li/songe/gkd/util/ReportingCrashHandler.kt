package li.songe.gkd.util

/** Persist synchronously before delegating; a dead main looper cannot run a recovery coroutine. */
internal class ReportingCrashHandler(
    private val previous: Thread.UncaughtExceptionHandler?,
    private val report: (Thread, Throwable) -> Unit,
    private val terminate: () -> Unit,
) : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(thread: Thread, error: Throwable) {
        try {
            runCatching { report(thread, error) }
            previous?.uncaughtException(thread, error)
        } finally {
            // Android's handler normally exits; retain a fallback if an OEM handler returns/throws.
            terminate()
        }
    }
}
