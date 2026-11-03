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
    private var nodePolicyBroker: RootNodePolicyBroker? = null
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
        override fun publishNodePolicy(enabled: Boolean, scopeReady: Boolean, excluded: Array<String>, allowed: Array<String>, protectedPackages: Array<String>) {
            enforceTrustedCaller()
            if (excluded.size > 4096 || allowed.size > 4096 || protectedPackages.size > 4096) throw IllegalArgumentException("Too many policy entries")
            nodePolicyBroker?.publish(enabled, scopeReady, excluded.toSet() + nodeBridgeProtectedPackages,
                allowed.toSet() - nodeBridgeProtectedPackages, protectedPackages.toSet() + nodeBridgeProtectedPackages)
        }

        override fun requestXSpaceScopeSetup(): Boolean {
            enforceTrustedCaller()
            val candidates = getNodeScopePackages(999) ?: return false
            // A libsu root process is not an AMS-registered ApplicationThread. Use
            // Android's native activity shell entry instead of Context.startActivityAsUser.
            return runCatching {
                val command = ProcessBuilder("/system/bin/cmd", "activity", "start-activity",
                    "--user", "999", "-n", "$packageName/.root.NodeScopeSetupActivity",
                    "--esal", "nodeScopeCandidates", candidates.joinToString(","))
                    .redirectErrorStream(true).start()
                val output = command.inputStream.bufferedReader().use { it.readText() }
                val status = command.waitFor()
                if (status != 0 || output.contains("Error:")) {
                    android.util.Log.w("GKD-Scope", "XSpace setup exit=$status: $output")
                    false
                } else true
            }.onFailure { android.util.Log.w("GKD-Scope", "XSpace setup start failed", it) }.getOrDefault(false)
        }

        override fun getNodeScopePackages(userId: Int): Array<String>? {
            enforceTrustedCaller()
            require(userId == 0 || userId == 999)
            return runCatching {
                val command = ProcessBuilder("/system/bin/cmd", "package", "list", "packages", "-3", "--user", userId.toString())
                    .redirectErrorStream(true).start()
                val lines = command.inputStream.bufferedReader().readLines()
                val status = command.waitFor()
                if (status != 0) {
                    android.util.Log.w("GKD-Scope", "Inventory failed user=$userId exit=$status: ${lines.take(2)}")
                    null
                } else {
                    val names = lines.map { it.trim() }.filter { it.startsWith("package:") }
                        .mapTo(hashSetOf()) { it.removePrefix("package:") }
                    nodeScopeCandidates(names).toTypedArray()
                }
            }.onFailure { android.util.Log.w("GKD-Scope", "Inventory unavailable user=$userId", it) }.getOrNull()
        }

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

        override fun getDetectorFolderPackages(): Array<String>? {
            enforceTrustedCaller()
            return readDetectorFolderPackages()
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

    override fun onBind(intent: Intent): IBinder {
        if (nodePolicyBroker == null) {
            nodePolicyBroker = RootNodePolicyBroker(packageManager).also { it.start() }
        }
        return binder
    }

    override fun onDestroy() {
        nodePolicyBroker?.close()
        nodePolicyBroker = null
        super.onDestroy()
    }
}
