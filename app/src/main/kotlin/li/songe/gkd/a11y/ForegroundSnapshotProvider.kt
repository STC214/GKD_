package li.songe.gkd.a11y

import android.view.Display
import li.songe.gkd.runtime.foreground.ForegroundSnapshot
import li.songe.gkd.runtime.foreground.ForegroundWindow
import li.songe.gkd.runtime.foreground.resolveForegroundSnapshot
import li.songe.gkd.shizuku.shizukuContextFlow
import li.songe.gkd.util.AndroidTarget

fun A11yCommonImpl.captureForegroundSnapshot(
    targetDisplayId: Int = Display.DEFAULT_DISPLAY,
    timestamp: Long = System.currentTimeMillis(),
): ForegroundSnapshot {
    val task = shizukuContextFlow.value.getForegroundTask(targetDisplayId)
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
            layer = window.layer,
            isFocused = window.isFocused,
            isActive = window.isActive,
            rootAppId = runCatching { window.root?.packageName?.toString() }.getOrNull(),
        )
    }
    return resolveForegroundSnapshot(task, windows, targetDisplayId, timestamp)
}
