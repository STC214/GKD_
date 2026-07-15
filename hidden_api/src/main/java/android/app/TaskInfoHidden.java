package android.app;

import android.content.res.Configuration;
import android.os.Build;

import androidx.annotation.RequiresApi;

import li.songe.remap.RemapType;

/**
 * @noinspection unused
 */
@RemapType(TaskInfo.class)
@RequiresApi(Build.VERSION_CODES.Q)
public class TaskInfoHidden {
    public Configuration configuration;
    public int userId;
    public int taskId;
    public int effectiveUid;
    public int displayId;
    public boolean isFocused;
    public boolean isVisible;
    public boolean isRunning;
}
