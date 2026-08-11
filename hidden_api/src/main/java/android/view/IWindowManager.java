package android.view;

import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.IInterface;
import android.window.ScreenCapture;
import android.window.ScreenCaptureInternal;

import androidx.annotation.RequiresApi;

/**
 * @noinspection unused
 */
public interface IWindowManager extends IInterface {
    abstract class Stub extends Binder implements IWindowManager {
        public static IWindowManager asInterface(IBinder obj) {
            throw new RuntimeException();
        }
    }

    // https://diff.songe.li/i/IWindowManager.captureDisplay
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    void captureDisplay(int displayId, ScreenCapture.CaptureArgs captureArgs, ScreenCapture.ScreenCaptureListener listener);

    // android-16.0.0_r4
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    void captureDisplay(int displayId, ScreenCaptureInternal.CaptureArgs captureArgs, ScreenCaptureInternal.ScreenCaptureListener listener);
}
