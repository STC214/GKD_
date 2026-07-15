package li.songe.gkd.root

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import li.songe.gkd.runtime.foreground.ForegroundTask

@Parcelize
data class RootForegroundTask(
    val taskId: Int,
    val userId: Int,
    val effectiveUid: Int,
    val displayId: Int,
    val isFocused: Boolean,
    val isVisible: Boolean,
    val isRunning: Boolean,
    val isPictureInPicture: Boolean,
    val appId: String?,
    val activityId: String?,
) : Parcelable {
    fun toForegroundTask() = ForegroundTask(
        taskId = taskId,
        userId = userId,
        effectiveUid = effectiveUid,
        displayId = displayId,
        isFocused = isFocused,
        isVisible = isVisible,
        isRunning = isRunning,
        isPictureInPicture = isPictureInPicture,
        appId = appId,
        activityId = activityId,
    )
}

fun ForegroundTask.toRootForegroundTask() = RootForegroundTask(
    taskId = taskId,
    userId = userId,
    effectiveUid = effectiveUid,
    displayId = displayId,
    isFocused = isFocused,
    isVisible = isVisible,
    isRunning = isRunning,
    isPictureInPicture = isPictureInPicture,
    appId = appId,
    activityId = activityId,
)
