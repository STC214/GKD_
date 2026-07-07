package li.songe.gkd.util

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// https://github.com/npes87184/ScreenShareTile/blob/master/app/src/main/java/com/npes87184/screenshottile/ScreenshotService.kt
class ScreenshotUtil(
    private val context: Context,
    private val screenshotIntent: Intent
) {

    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var mediaProjection: MediaProjection? = null

    private val mediaProjectionManager by lazy {
        context.getSystemService(
            Activity.MEDIA_PROJECTION_SERVICE
        ) as MediaProjectionManager
    }

    private val width: Int
        get() = ScreenUtils.getScreenWidth()
    private val height: Int
        get() = ScreenUtils.getScreenHeight()
    private val dpi: Int
        get() = ScreenUtils.getScreenDensityDpi()

    fun destroy() {
        releaseCapture()
        mediaProjection?.stop()
        mediaProjection = null
    }

    private fun releaseCapture(
        reader: ImageReader? = imageReader,
        display: VirtualDisplay? = virtualDisplay,
    ) {
        reader?.setOnImageAvailableListener(null, null)
        display?.release()
        reader?.close()
        if (reader === imageReader) {
            imageReader = null
        }
        if (display === virtualDisplay) {
            virtualDisplay = null
        }
    }

    suspend fun execute() = suspendCancellableCoroutine { cont ->
        releaseCapture()
        val reader = ImageReader.newInstance(
            width, height,
            PixelFormat.RGBA_8888, 2
        ).also { imageReader = it }
        if (mediaProjection == null) {
            mediaProjection = mediaProjectionManager.getMediaProjection(
                RESULT_OK,
                screenshotIntent
            )
        }
        val display = mediaProjection!!.createVirtualDisplay(
            "screenshot",
            width,
            height,
            dpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface,
            null,
            handler
        ).also { virtualDisplay = it }
        var resumed = false
        cont.invokeOnCancellation { releaseCapture(reader, display) }
        reader.setOnImageAvailableListener({ callbackReader ->
            if (resumed) return@setOnImageAvailableListener
            var image: Image? = null
            var bitmapWithStride: Bitmap? = null
            val bitmap: Bitmap?
            try {
                image = callbackReader.acquireLatestImage()
                if (image != null) {
                    val planes = image.planes
                    val buffer = planes[0].buffer
                    val pixelStride = planes[0].pixelStride
                    val rowStride = planes[0].rowStride
                    bitmapWithStride = createBitmap(rowStride / pixelStride, height)
                    bitmapWithStride.copyPixelsFromBuffer(buffer)
                    bitmap = Bitmap.createBitmap(bitmapWithStride, 0, 0, width, height)
                    if (!bitmap.isFullTransparent()) {
                        if (cont.isActive) {
                            cont.resume(bitmap)
                        }
                        resumed = true
                        releaseCapture(callbackReader, display)
                    } else {
                        bitmap.recycle()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (cont.isActive) {
                    cont.resumeWithException(e)
                }
                releaseCapture(callbackReader, display)
            } finally {
                bitmapWithStride?.recycle()
                image?.close()
            }
        }, handler)
    }
}
