package li.songe.gkd.runtime.foreground

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForegroundSnapshotTest {
    private val task = ForegroundTask(
        taskId = 7,
        userId = 10,
        effectiveUid = 1010001,
        displayId = 0,
        isFocused = true,
        isVisible = true,
        isRunning = true,
        appId = "host.app",
        activityId = "host.app.MainActivity",
    )

    private fun window(
        id: Int,
        appId: String?,
        displayId: Int = 0,
        layer: Int = 1,
        focused: Boolean = false,
        active: Boolean = false,
    ) = ForegroundWindow(
        windowId = id,
        displayId = displayId,
        type = 1,
        layer = layer,
        isFocused = focused,
        isActive = active,
        rootAppId = appId,
    )

    @Test
    fun `focused window wins over active window`() {
        val selected = selectForegroundWindow(
            listOf(
                window(1, "active", active = true, layer = 9),
                window(2, "focused", focused = true, layer = 2),
            ),
            targetDisplayId = 0,
        )
        assertEquals(2, selected?.windowId)
    }

    @Test
    fun `highest focused layer wins`() {
        val selected = selectForegroundWindow(
            listOf(
                window(1, "low", focused = true, layer = 2),
                window(2, "high", focused = true, layer = 5),
            ),
            targetDisplayId = 0,
        )
        assertEquals(2, selected?.windowId)
    }

    @Test
    fun `window on another display is ignored`() {
        val selected = selectForegroundWindow(
            listOf(window(1, "other", displayId = 1, focused = true)),
            targetDisplayId = 0,
        )
        assertEquals(null, selected)
    }

    @Test
    fun `matching focused task and window is confirmed`() {
        val snapshot = resolveForegroundSnapshot(
            task,
            listOf(window(3, "host.app", focused = true)),
            targetDisplayId = 0,
            timestamp = 123,
        )
        assertEquals(ForegroundConfidence.Confirmed, snapshot.confidence)
        assertEquals("host.app", snapshot.appId)
        assertEquals(7, snapshot.task?.taskId)
        assertEquals(3, snapshot.window?.windowId)
        assertEquals(10, snapshot.userId)
        assertTrue(snapshot.canExecute)
    }

    @Test
    fun `task and focused window package mismatch is conflict`() {
        val snapshot = resolveForegroundSnapshot(
            task,
            listOf(window(3, "overlay.app", focused = true)),
            targetDisplayId = 0,
            timestamp = 123,
        )
        assertEquals(ForegroundConfidence.Conflict, snapshot.confidence)
        assertEquals("overlay.app", snapshot.appId)
        assertFalse(snapshot.canExecute)
    }

    @Test
    fun `active matching window is only probable`() {
        val snapshot = resolveForegroundSnapshot(
            task,
            listOf(window(3, "host.app", active = true)),
            targetDisplayId = 0,
            timestamp = 123,
        )
        assertEquals(ForegroundConfidence.Probable, snapshot.confidence)
        assertFalse(snapshot.canExecute)
    }

    @Test
    fun `matching focused window cannot confirm fallback non-focused task`() {
        val snapshot = resolveForegroundSnapshot(
            task.copy(isFocused = false),
            listOf(window(3, "host.app", focused = true)),
            targetDisplayId = 0,
            timestamp = 123,
        )
        assertEquals(ForegroundConfidence.Probable, snapshot.confidence)
        assertFalse(snapshot.canExecute)
    }

    @Test
    fun `matching focused window cannot confirm invisible or stopped task`() {
        listOf(
            task.copy(isVisible = false),
            task.copy(isRunning = false),
        ).forEach { invalidTask ->
            val snapshot = resolveForegroundSnapshot(
                invalidTask,
                listOf(window(3, "host.app", focused = true)),
                targetDisplayId = 0,
                timestamp = 123,
            )
            assertEquals(ForegroundConfidence.Probable, snapshot.confidence)
            assertFalse(snapshot.canExecute)
        }
    }

    @Test
    fun `task without window is probable`() {
        val snapshot = resolveForegroundSnapshot(
            task,
            emptyList(),
            targetDisplayId = 0,
            timestamp = 123,
        )
        assertEquals(ForegroundConfidence.Probable, snapshot.confidence)
        assertEquals("host.app", snapshot.appId)
    }

    @Test
    fun `missing task and window is unavailable`() {
        val snapshot = resolveForegroundSnapshot(
            null,
            emptyList(),
            targetDisplayId = 0,
            timestamp = 123,
        )
        assertEquals(ForegroundConfidence.Unavailable, snapshot.confidence)
        assertEquals(null, snapshot.appId)
    }
}
