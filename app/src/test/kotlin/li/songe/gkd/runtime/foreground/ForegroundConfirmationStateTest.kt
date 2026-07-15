package li.songe.gkd.runtime.foreground

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ForegroundConfirmationStateTest {
    private val task = ForegroundTask(
        taskId = 1,
        userId = 0,
        effectiveUid = 10001,
        displayId = 0,
        isFocused = true,
        isVisible = true,
        isRunning = true,
        isPictureInPicture = false,
        appId = "host.app",
        activityId = "host.app.MainActivity",
    )

    private fun snapshot(
        timestamp: Long,
        windowAppId: String = "host.app",
        kind: ForegroundWindowKind = ForegroundWindowKind.Application,
    ) = resolveForegroundSnapshot(
        task = task,
        windows = listOf(
            ForegroundWindow(
                windowId = 2,
                displayId = 0,
                type = 1,
                kind = kind,
                layer = 1,
                isFocused = true,
                isActive = true,
                rootAppId = windowAppId,
            )
        ),
        targetDisplayId = 0,
        timestamp = timestamp,
        imeAppId = "keyboard.app",
    )

    @Test
    fun `confirmed application is accepted immediately`() {
        val state = ForegroundConfirmationState(150) { 100L }
        assertTrue(state.observe(snapshot(100)) is ForegroundConfirmationResult.Accepted)
    }

    @Test
    fun `first conflict schedules one bounded confirmation`() {
        val state = ForegroundConfirmationState(150) { 100L }
        val result = state.observe(snapshot(100, windowAppId = "overlay.app"))
        assertTrue(result is ForegroundConfirmationResult.Pending)
        assertEquals(150L, (result as ForegroundConfirmationResult.Pending).retryAfterMillis)
    }

    @Test
    fun `same conflict remains pending only until deadline then rejects`() {
        var now = 100L
        val state = ForegroundConfirmationState(150) { now }
        state.observe(snapshot(100, windowAppId = "overlay.app"))
        now = 200L
        val pending = state.observe(snapshot(200, windowAppId = "overlay.app"))
        assertEquals(50L, (pending as ForegroundConfirmationResult.Pending).retryAfterMillis)
        now = 250L
        assertTrue(
            state.observe(snapshot(250, windowAppId = "overlay.app")) is
                    ForegroundConfirmationResult.Rejected
        )
    }

    @Test
    fun `changed conflict restarts confirmation window`() {
        var now = 100L
        val state = ForegroundConfirmationState(150) { now }
        state.observe(snapshot(100, windowAppId = "overlay.one"))
        now = 200L
        val changed = state.observe(snapshot(200, windowAppId = "overlay.two"))
        assertEquals(150L, (changed as ForegroundConfirmationResult.Pending).retryAfterMillis)
    }

    @Test
    fun `confirmed application clears earlier pending conflict`() {
        var now = 100L
        val state = ForegroundConfirmationState(150) { now }
        state.observe(snapshot(100, windowAppId = "overlay.app"))
        now = 150L
        assertTrue(state.observe(snapshot(150)) is ForegroundConfirmationResult.Accepted)
        now = 160L
        val nextConflict = state.observe(snapshot(160, windowAppId = "overlay.app"))
        assertEquals(150L, (nextConflict as ForegroundConfirmationResult.Pending).retryAfterMillis)
    }

    @Test
    fun `wall clock rollback cannot extend confirmation`() {
        var now = 1_000L
        val state = ForegroundConfirmationState(150) { now }
        state.observe(snapshot(10_000, windowAppId = "overlay.app"))
        now = 1_150L
        assertTrue(
            state.observe(snapshot(1, windowAppId = "overlay.app")) is
                    ForegroundConfirmationResult.Rejected
        )
    }
}
