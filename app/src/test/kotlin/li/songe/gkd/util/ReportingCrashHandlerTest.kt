package li.songe.gkd.util

import org.junit.Assert.*
import org.junit.Test

class ReportingCrashHandlerTest {
    @Test fun `report before delegate and always terminate if delegate returns`() {
        val calls = mutableListOf<String>(); val error = IllegalStateException("fixture")
        ReportingCrashHandler({ _, e -> assertSame(error, e); calls.add("delegate") },
            { _, e -> assertSame(error, e); calls.add("report") }, { calls.add("terminate") })
            .uncaughtException(Thread.currentThread(), error)
        assertEquals(listOf("report", "delegate", "terminate"), calls)
    }
    @Test fun `failed report still delegates`() {
        var delegated = false; var terminated = false
        ReportingCrashHandler({ _, _ -> delegated = true }, { _, _ -> error("disk full") }, { terminated = true })
            .uncaughtException(Thread.currentThread(), IllegalStateException())
        assertTrue(delegated); assertTrue(terminated)
    }
    @Test fun `missing previous handler still terminates`() {
        var terminated = false
        ReportingCrashHandler(null, { _, _ -> }, { terminated = true }).uncaughtException(Thread.currentThread(), Exception())
        assertTrue(terminated)
    }
    @Test fun `throwing OEM delegate still terminates`() {
        var terminated = false
        runCatching { ReportingCrashHandler({ _, _ -> error("OEM") }, { _, _ -> }, { terminated = true })
            .uncaughtException(Thread.currentThread(), Exception()) }
        assertTrue(terminated)
    }
}
