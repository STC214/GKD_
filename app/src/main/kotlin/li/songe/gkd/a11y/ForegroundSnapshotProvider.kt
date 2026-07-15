package li.songe.gkd.a11y

import android.view.Display
import li.songe.gkd.app
import li.songe.gkd.runtime.foreground.ForegroundSnapshot
import li.songe.gkd.runtime.foreground.ForegroundWindow
import li.songe.gkd.runtime.foreground.ForegroundWindowKind
import li.songe.gkd.runtime.foreground.resolveForegroundSnapshot
import li.songe.gkd.root.RootServiceClient
import li.songe.gkd.shizuku.shizukuContextFlow
import li.songe.gkd.util.AndroidTarget

fun A11yCommonImpl.captureForegroundSnapshot(
    targetDisplayId: Int = Display.DEFAULT_DISPLAY,
    timestamp: Long = System.currentTimeMillis(),
): ForegroundSnapshot {
    val task = RootServiceClient.getForegroundTask(targetDisplayId)
        ?: shizukuContextFlow.value.getForegroundTask(targetDisplayId)
    val windows = runCatching { windowInfos }.getOrDefault(emptyList()).mapNotNull { window ->
        val displayId = if (AndroidTarget.TIRAMISU) {
            window.displayId
        } else {
            Display.DEFAULT_DISPLAY
        }
        if (displayId != targetDisplayId) return@mapNotNull null
        ForegroundWindow(
            windowId = window.id,
            displayId = displayId,
            type = window.type,
            kind = when (window.type) {
                android.view.accessibility.AccessibilityWindowInfo.TYPE_APPLICATION -> ForegroundWindowKind.Application
                android.view.accessibility.AccessibilityWindowInfo.TYPE_INPUT_METHOD -> ForegroundWindowKind.InputMethod
                android.view.accessibility.AccessibilityWindowInfo.TYPE_SYSTEM -> ForegroundWindowKind.System
                android.view.accessibility.AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY -> ForegroundWindowKind.AccessibilityOverlay
                else -> ForegroundWindowKind.Other
            },
            layer = window.layer,
            isFocused = window.isFocused,
            isActive = window.isActive,
            rootAppId = runCatching { window.root?.packageName?.toString() }.getOrNull(),
            isPictureInPicture = if (
                android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O
            ) {
                window.isInPictureInPictureMode
            } else {
                false
            },
        )
    }
    return resolveForegroundSnapshot(
        task = task,
        windows = windows,
        targetDisplayId = targetDisplayId,
        timestamp = timestamp,
        imeAppId = imeAppId,
        rotation = app.compatDisplay.rotation,
    )
}
