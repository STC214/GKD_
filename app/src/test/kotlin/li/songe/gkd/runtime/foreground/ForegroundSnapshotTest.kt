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
        isPictureInPicture = false,
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
        kind: ForegroundWindowKind = ForegroundWindowKind.Application,
        pictureInPicture: Boolean = false,
    ) = ForegroundWindow(
        windowId = id,
        displayId = displayId,
        type = 1,
        kind = kind,
        layer = layer,
        isFocused = focused,
        isActive = active,
        rootAppId = appId,
        isPictureInPicture = pictureInPicture,
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
    fun `focused application with root not mounted is recoverable but cannot execute`() {
        val snapshot = resolveForegroundSnapshot(
            task,
            listOf(window(3, null, focused = true)),
            targetDisplayId = 0,
            timestamp = 123,
        )
        assertEquals(ForegroundConfidence.Probable, snapshot.confidence)
        assertTrue(snapshot.canRecoverMissingRoot)
        assertFalse(snapshot.canExecute)
    }

    @Test
    fun `unfocused or non application window cannot recover missing root`() {
        val unfocused = resolveForegroundSnapshot(
            task,
            listOf(window(3, null, active = true)),
            targetDisplayId = 0,
            timestamp = 123,
        )
        val systemWindow = resolveForegroundSnapshot(
            task,
            listOf(window(3, null, focused = true, kind = ForegroundWindowKind.System)),
            targetDisplayId = 0,
            timestamp = 123,
        )
        assertFalse(unfocused.canRecoverMissingRoot)
        assertFalse(systemWindow.canRecoverMissingRoot)
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

    @Test
    fun `input method overlay is classified and cannot execute host rules`() {
        val snapshot = resolveForegroundSnapshot(
            task,
            listOf(
                window(
                    id = 3,
                    appId = "host.app",
                    layer = 0,
                    focused = true,
                    active = true,
                ),
                window(
                    id = 4,
                    appId = "keyboard.app",
                    layer = 1,
                    kind = ForegroundWindowKind.InputMethod,
                )
            ),
            targetDisplayId = 0,
            timestamp = 123,
            imeAppId = "keyboard.app",
        )
        assertEquals(ForegroundSurface.InputMethod, snapshot.surface)
        assertEquals(ForegroundConfidence.Conflict, snapshot.confidence)
        assertFalse(snapshot.canExecute)
    }

    @Test
    fun `system ui and permission controller overlays cannot execute`() {
        listOf(
            "com.android.systemui" to ForegroundSurface.SystemUi,
            "com.android.permissioncontroller" to ForegroundSurface.PermissionController,
            "com.lbe.security.miui" to ForegroundSurface.PermissionController,
        ).forEach { (appId, expectedSurface) ->
            val snapshot = resolveForegroundSnapshot(
                task,
                listOf(window(4, appId, focused = true, kind = ForegroundWindowKind.System)),
                targetDisplayId = 0,
                timestamp = 123,
            )
            assertEquals(expectedSurface, snapshot.surface)
            assertFalse(snapshot.canExecute)
        }
    }

    @Test
    fun `permission controller task blocks when accessibility still focuses host window`() {
        val snapshot = resolveForegroundSnapshot(
            task.copy(
                appId = "com.android.permissioncontroller",
                activityId = "com.android.permissioncontroller.permission.ui.GrantPermissionsActivity",
            ),
            listOf(window(4, "host.app", focused = true, active = true)),
            targetDisplayId = 0,
            timestamp = 123,
        )
        assertEquals(ForegroundSurface.PermissionController, snapshot.surface)
        assertEquals(ForegroundConfidence.Conflict, snapshot.confidence)
        assertFalse(snapshot.canExecute)
    }

    @Test
    fun `picture in picture remains non executable even with matching package`() {
        val snapshot = resolveForegroundSnapshot(
            task.copy(isPictureInPicture = true),
            listOf(window(4, "host.app", focused = true)),
            targetDisplayId = 0,
            timestamp = 123,
        )
        assertEquals(ForegroundSurface.PictureInPicture, snapshot.surface)
        assertEquals(ForegroundConfidence.Confirmed, snapshot.confidence)
        assertFalse(snapshot.canExecute)
    }

    @Test
    fun `visible picture in picture window blocks focused host application`() {
        val snapshot = resolveForegroundSnapshot(
            task,
            listOf(
                window(3, "host.app", layer = 0, focused = true, active = true),
                window(4, "pip.app", layer = 2, pictureInPicture = true),
            ),
            targetDisplayId = 0,
            timestamp = 123,
        )
        assertEquals(ForegroundSurface.PictureInPicture, snapshot.surface)
        assertEquals(ForegroundConfidence.Conflict, snapshot.confidence)
        assertFalse(snapshot.canExecute)
    }

    @Test
    fun `accessibility and generic system overlays cannot execute`() {
        listOf(
            ForegroundWindowKind.AccessibilityOverlay to ForegroundSurface.AccessibilityOverlay,
            ForegroundWindowKind.System to ForegroundSurface.SystemOverlay,
        ).forEach { (kind, expectedSurface) ->
            val snapshot = resolveForegroundSnapshot(
                task,
                listOf(window(4, "host.app", focused = true, kind = kind)),
                targetDisplayId = 0,
                timestamp = 123,
            )
            assertEquals(expectedSurface, snapshot.surface)
            assertFalse(snapshot.canExecute)
        }
    }

    @Test
    fun `event activity is fallback only when task activity is unavailable`() {
        val authoritative = resolveForegroundSnapshot(
            task,
            listOf(window(4, "host.app", focused = true)),
            targetDisplayId = 0,
            timestamp = 123,
        )
        assertFalse(authoritative.canUseEventActivityFallback("host.app"))

        val missingTaskActivity = authoritative.copy(activityId = null)
        assertTrue(missingTaskActivity.canUseEventActivityFallback("host.app"))
        assertFalse(missingTaskActivity.canUseEventActivityFallback("other.app"))
    }
}
