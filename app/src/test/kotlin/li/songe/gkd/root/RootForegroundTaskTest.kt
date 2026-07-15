package li.songe.gkd.root

import li.songe.gkd.runtime.foreground.ForegroundTask
import org.junit.Assert.assertEquals
import org.junit.Test

class RootForegroundTaskTest {
    @Test
    fun preservesAllForegroundFieldsAcrossAidlModel() {
        val task = ForegroundTask(
            taskId = 42,
            userId = 10,
            effectiveUid = 10123,
            displayId = 2,
            isFocused = true,
            isVisible = true,
            isRunning = true,
            isPictureInPicture = false,
            appId = "example.app",
            activityId = "example.app.MainActivity",
        )

        assertEquals(task, task.toRootForegroundTask().toForegroundTask())
    }
}
