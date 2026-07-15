package li.songe.gkd.runtime.foreground

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WindowGenerationStateTest {
    private val snapshot = ForegroundSnapshot(
        task = null,
        window = null,
        appId = "host.app",
        activityId = "host.app.MainActivity",
        userId = 0,
        displayId = 0,
        timestamp = 1L,
        confidence = ForegroundConfidence.Confirmed,
        surface = ForegroundSurface.Application,
    )

    @Test
    fun `unchanged generation and context remain current`() {
        val state = WindowGenerationState()
        val token = state.capture(snapshot)
        assertTrue(state.isCurrent(token, snapshot))
    }

    @Test
    fun `window event invalidates an earlier selector token`() {
        val state = WindowGenerationState()
        val token = state.capture(snapshot)
        state.advance()
        assertFalse(state.isCurrent(token, snapshot))
        assertFalse(state.isGenerationCurrent(token))
        assertTrue(state.matchesWindowContext(token, snapshot))
    }

    @Test
    fun `context fields are checked even without an event`() {
        val state = WindowGenerationState()
        val token = state.capture(snapshot)
        assertFalse(state.isCurrent(token, snapshot.copy(displayId = 1)))
        assertFalse(state.isCurrent(token, snapshot.copy(appId = "other.app")))
        assertTrue(state.isGenerationCurrent(token))
        assertFalse(state.matchesWindowContext(token, snapshot.copy(appId = "other.app")))
    }

    @Test
    fun `rotation change invalidates and advances display context`() {
        val state = WindowGenerationState()
        val token = state.capture(snapshot)
        val rotated = snapshot.copy(rotation = 1)
        assertFalse(state.isCurrent(token, rotated))
        val rotatedToken = state.capture(rotated)
        assertTrue(state.isCurrent(rotatedToken, rotated))
        assertFalse(state.isCurrent(token, snapshot))
    }
}
