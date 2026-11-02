package li.songe.gkd.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import li.songe.gkd.app
import li.songe.gkd.appScope
import li.songe.gkd.notif.exposeNotif
import li.songe.gkd.syncFixState
import li.songe.gkd.util.LogUtils
import li.songe.gkd.util.SnapshotExt
import li.songe.gkd.util.componentName
import li.songe.gkd.util.launchTry
import li.songe.gkd.util.runMainPost
import li.songe.gkd.util.shFolder
import li.songe.gkd.util.toast

class ExposeService : Service() {
    private var foregroundStarted = false

    override fun onBind(intent: Intent?): Binder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!foregroundStarted) {
            LogUtils.d("ExposeService rejected: foreground service is unavailable")
            toast("外部前台服务启动失败，调用未执行", forced = true)
            stopSelf(startId)
            return START_NOT_STICKY
        }
        appScope.launchTry {
            try {
                handleIntent(intent)
            } finally {
                runMainPost(1000) { stopSelf() }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    suspend fun handleIntent(intent: Intent?) {
        val expose = intent?.getIntExtra("expose", 0) ?: 0
        val data = intent?.getStringExtra("data")
        LogUtils.d("ExposeService::handleIntent", expose, data)
        when (expose) {
            -1 -> StatusService.autoStart()
            0 -> SnapshotExt.captureSnapshot()
            1 -> {
                toast("执行成功", forced = true)
                syncFixState()
            }

            4 -> {
                // Node-only probe: deliberately does not call screenshot/captureSnapshot.
                val root = li.songe.gkd.a11y.A11yRuleEngine.instance?.safeActiveWindow
                val queue = java.util.ArrayDeque<android.view.accessibility.AccessibilityNodeInfo>()
                if (root != null) queue.add(root)
                var count = 0
                var skipCandidates = 0
                while (queue.isNotEmpty() && count < 1000) {
                    val node = queue.removeFirst()
                    count++
                    if (node.text?.contains("跳过") == true || node.contentDescription?.contains("跳过") == true) skipCandidates++
                    for (i in 0 until minOf(node.childCount, 200)) {
                        node.getChild(i)?.let(queue::add)
                    }
                }
                val report = org.json.JSONObject()
                    .put("timestamp", System.currentTimeMillis())
                    .put("rootPackage", root?.packageName?.toString() ?: org.json.JSONObject.NULL)
                    .put("nodes", count).put("skipCandidates", skipCandidates)
                    .put("screenshotsUsed", false)
                li.songe.gkd.util.logFolder.resolve("root-node-probe.json").writeText(report.toString())
                LogUtils.d("Root node-only probe", report.toString())
            }

            5 -> li.songe.gkd.root.NodeScopeSetup.requestBatch(
                forwardedCandidates = intent?.getStringArrayListExtra("nodeScopeCandidates")?.toSet(),
            )
            6 -> li.songe.gkd.root.NodeScopeSetup.requestXSpaceBatch()

            else -> {
                toast("未知调用: expose=$expose data=$data", forced = true)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        // ExposeService is a bounded command bridge. It uses the manifest
        // shortService type and must not depend on the special-use AppOp.
        foregroundStarted = exposeNotif.notifyService(requireSpecialUse = false)
        if (!foregroundStarted) stopSelf()
    }

    companion object {
        fun initCommandFile() {
            val commandText = template
                .replace("{service}", ExposeService::class.componentName.flattenToShortString())
            shFolder.resolve("expose.sh").writeText(commandText)
        }

        fun exposeIntent(expose: Int, data: String? = null): Intent {
            return Intent(app, ExposeService::class.java).apply {
                putExtra("expose", expose)
                if (data != null) {
                    putExtra("data", data)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }
}

private const val template = $$"""set -euo pipefail
echo '> start expose.sh'
p=''
if [ -n "${1:-}" ]; then
  p+=" --ei expose $1"
fi
if [ -n "${2:-}" ]; then
  p+=" --es data $2"
fi
am start-foreground-service -n {service} $p
echo '> expose.sh end'
"""
