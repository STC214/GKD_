package li.songe.gkd.runtime.foreground

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RootMismatchRetryStateTest {
    private val context = RootMismatchContext(
        taskId = 1,
        windowId = 2,
        foregroundAppId = "host.app",
        rootAppId = "stale.app",
    )

    @Test
    fun `same mismatch receives only one retry`() {
        val state = RootMismatchRetryState()
        assertTrue(state.request(context))
        assertFalse(state.request(context))
        assertFalse(state.request(context))
    }

    @Test
    fun `changed context receives a new retry`() {
        val state = RootMismatchRetryState()
        assertTrue(state.request(context))
        assertTrue(state.request(context.copy(windowId = 3)))
    }

    @Test
    fun `successful match clears retry history`() {
        val state = RootMismatchRetryState()
        assertTrue(state.request(context))
        state.clear()
        assertTrue(state.request(context))
    }
}
