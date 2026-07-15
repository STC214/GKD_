package li.songe.gkd.shizuku

import android.content.Context
import android.hardware.input.IInputManager
import android.view.InputEvent
import android.view.Display
import androidx.annotation.WorkerThread
import li.songe.gkd.util.AndroidTarget


class SafeInputManager(private val value: IInputManager) {
    companion object {
        fun newBinder() = getShizukuService(Context.INPUT_SERVICE)?.let {
            SafeInputManager(IInputManager.Stub.asInterface(it))
        }
    }

    private val command = InputShellCommand(this)

    fun compatInjectInputEvent(
        ev: InputEvent,
        mode: Int,
    ): Boolean = safeInvokeShizuku {
        if (AndroidTarget.TIRAMISU) {
            // https://github.com/android-cs/16/blob/main/core/java/android/hardware/input/InputManagerGlobal.java#L1707
            value.injectInputEventToTarget(ev, mode, android.os.Process.INVALID_UID)
        } else {
            value.injectInputEvent(ev, mode)
        }
    } == true

    @WorkerThread
    fun tap(
        x: Float,
        y: Float,
        duration: Long = 0,
        displayId: Int = Display.DEFAULT_DISPLAY,
    ): Boolean {
        return if (duration > 0) {
            command.runSwipe(x, y, x, y, duration, displayId)
        } else {
            command.runTap(x, y, displayId)
        }
    }

    @WorkerThread
    fun swipe(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        duration: Long,
        displayId: Int = Display.DEFAULT_DISPLAY,
    ): Boolean = command.runSwipe(x1, y1, x2, y2, duration, displayId)

    fun key(keyCode: Int) = command.runKeyEvent(keyCode)

}
