package li.songe.gkd.runtime.foreground

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ForegroundTaskTest {
    private fun task(
        taskId: Int,
        displayId: Int = 0,
        focused: Boolean = false,
        visible: Boolean = false,
        running: Boolean = false,
    ) = ForegroundTask(
        taskId = taskId,
        userId = 0,
        effectiveUid = 10000 + taskId,
        displayId = displayId,
        isFocused = focused,
        isVisible = visible,
        isRunning = running,
        appId = "app.$taskId",
        activityId = "app.$taskId.MainActivity",
    )

    @Test
    fun `focused visible running task wins even when it is not first`() {
        val selected = selectForegroundTask(
            listOf(
                task(1, visible = true, running = true),
                task(2, focused = true, visible = true, running = true),
            ),
            targetDisplayId = 0,
        )
        assertEquals(2, selected?.taskId)
    }

    @Test
    fun `task on another display cannot win`() {
        val selected = selectForegroundTask(
            listOf(
                task(1, displayId = 1, focused = true, visible = true, running = true),
                task(2, displayId = 0, visible = true, running = true),
            ),
            targetDisplayId = 0,
        )
        assertEquals(2, selected?.taskId)
    }

    @Test
    fun `focused running task beats merely visible task`() {
        val selected = selectForegroundTask(
            listOf(
                task(1, visible = true, running = true),
                task(2, focused = true, running = true),
            ),
            targetDisplayId = 0,
        )
        assertEquals(2, selected?.taskId)
    }

    @Test
    fun `visible running task is safe fallback when focus is unavailable`() {
        val selected = selectForegroundTask(
            listOf(
                task(1, running = true),
                task(2, visible = true, running = true),
            ),
            targetDisplayId = 0,
        )
        assertEquals(2, selected?.taskId)
    }

    @Test
    fun `original task order is retained inside the same confidence tier`() {
        val selected = selectForegroundTask(
            listOf(
                task(1, visible = true, running = true),
                task(2, visible = true, running = true),
            ),
            targetDisplayId = 0,
        )
        assertEquals(1, selected?.taskId)
    }

    @Test
    fun `missing target display returns no task`() {
        val selected = selectForegroundTask(
            listOf(task(1, displayId = 1, focused = true, visible = true, running = true)),
            targetDisplayId = 0,
        )
        assertNull(selected)
    }
}
