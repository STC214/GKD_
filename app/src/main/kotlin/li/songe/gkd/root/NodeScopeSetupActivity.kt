package li.songe.gkd.root

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.widget.TextView
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Root-launched, non-exported one-shot setup; never starts a second matching engine. */
class NodeScopeSetupActivity : Activity() {
    private val scope = MainScope()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Process.myUid() / 100000 != 999) { finish(); return }
        val text = TextView(this).apply {
            this.text = "正在为双开用户批量配置 LSPosed；只需确认框架授权，完成后自动退出。"
            setPadding(48, 120, 48, 48)
            textSize = 18f
        }
        setContentView(text)
        scope.launch {
            NodeScopeSetup.requestBatch(forwardedCandidates =
                intent.getStringArrayListExtra("nodeScopeCandidates")?.toSet().orEmpty())
            text.text = NodeScopeSetup.status.value
            delay(1500)
            // Only this explicitly launched profile-host process is terminated, never owner GKD.
            Handler(Looper.getMainLooper()).postDelayed({ Process.killProcess(Process.myPid()) }, 500)
            finishAndRemoveTask()
        }
    }

    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}
