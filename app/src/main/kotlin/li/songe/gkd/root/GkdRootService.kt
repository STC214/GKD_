package li.songe.gkd.root

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.IBinder
import android.os.Process
import com.topjohnwu.superuser.ipc.RootService
import li.songe.gkd.shizuku.SafeActivityManager
import li.songe.gkd.shizuku.SafeActivityTaskManager
import li.songe.gkd.shizuku.SafeInputManager
import li.songe.gkd.shizuku.currentUserId
import li.songe.gkd.shizuku.selectForegroundTaskFromRunningTasks

class GkdRootService : RootService() {
    private val inputManager by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        SafeInputManager.newRootBinder()
    }
    private val activityTaskManager by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        SafeActivityTaskManager.newRootBinder()
    }
    private val activityManager by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        SafeActivityManager.newRootBinder()
    }

    private fun enforceTrustedCaller() {
        val callingUid = Binder.getCallingUid()
        val expectedPackage = packageName
        val expectedUid = packageManager.getApplicationInfo(expectedPackage, 0).uid
        val packages = packageManager.getPackagesForUid(callingUid)?.toList().orEmpty()
        val verification = verifyRootCaller(
            expectedUid = expectedUid,
            callingUid = callingUid,
            expectedPackage = expectedPackage,
            packagesForUid = packages,
            signaturesMatch = { callerPackage ->
                packageManager.checkSignatures(expectedPackage, callerPackage) ==
                    PackageManager.SIGNATURE_MATCH
            },
        )
        if (!verification.verified) {
            throw SecurityException(
                "Rejected root service caller uid=$callingUid reason=${verification.failure}",
            )
        }
    }

    private val binder = object : IRootService.Stub() {
        override fun getProtocolVersion(): Int {
            enforceTrustedCaller()
            return ROOT_SERVICE_PROTOCOL_VERSION
        }

        override fun getRemotePid(): Int {
            enforceTrustedCaller()
            return Process.myPid()
        }

        override fun getRemoteUid(): Int {
            enforceTrustedCaller()
            return Process.myUid()
        }

        override fun getServicePackageName(): String {
            enforceTrustedCaller()
            return packageName
        }

        override fun performInput(request: RootInputRequest): Int {
            enforceTrustedCaller()
            if (validateRootInputRequest(request) != null) {
                return ROOT_INPUT_RESULT_REJECTED
            }
            val manager = inputManager ?: return ROOT_INPUT_RESULT_UNAVAILABLE
            val completed = when (request.action) {
                ROOT_INPUT_ACTION_TAP -> manager.tap(
                    x = request.x1,
                    y = request.y1,
                    duration = request.durationMs,
                    displayId = request.displayId,
                )

                ROOT_INPUT_ACTION_SWIPE -> manager.swipe(
                    x1 = request.x1,
                    y1 = request.y1,
                    x2 = request.x2,
                    y2 = request.y2,
                    duration = request.durationMs,
                    displayId = request.displayId,
                )

                else -> false
            }
            return if (completed) ROOT_INPUT_RESULT_COMPLETED else ROOT_INPUT_RESULT_FAILED
        }

        override fun getForegroundTask(displayId: Int): RootForegroundTask? {
            enforceTrustedCaller()
            if (displayId < 0) return null
            return selectForegroundTaskFromRunningTasks(
                tasks = activityTaskManager?.getTasks(16)
                    ?: activityManager?.getTasks(16)
                    ?: emptyList(),
                targetDisplayId = displayId,
                fallbackUserId = currentUserId,
            )?.toRootForegroundTask()
        }
    }

    override fun onBind(intent: Intent): IBinder = binder
}
