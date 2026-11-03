package li.songe.gkd.root

import android.os.Process
import io.github.libxposed.service.XposedService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import li.songe.gkd.util.logFolder
import li.songe.gkd.util.toast
import org.json.JSONObject

/** Explicit batch operation only. Never loops scope prompts or silently grants framework access. */
object NodeScopeSetup {
    internal val service = MutableStateFlow<XposedService?>(null)
    val status = MutableStateFlow("未批量配置；正常黑白名单切换不会申请新作用域")
    private val mutex = Mutex()

    suspend fun requestXSpaceBatch() {
        if (Process.myUid() / 100000 != 0) return
        try {
            val started = withContext(Dispatchers.IO) { RootServiceClient.requestXSpaceScopeSetup() }
            record(0, if (started) "已打开双开批量配置入口，结果以双开配置页为准" else
                "双开入口未启动，请检查 Root 连接和用户 999 的模块登记")
        } catch (error: Exception) { record(0, "双开配置失败：${error.message}") }
    }

    suspend fun requestBatch(forwardedCandidates: Set<String>? = null): Boolean {
        if (!mutex.tryLock()) return false
        try {
            val user = Process.myUid() / 100000
            val api = withTimeoutOrNull(8000) { service.filterNotNull().first() }
            if (api == null) { record(user, "模块未连接，请先在 LSPosed 启用 GKD"); return false }
            val candidates = if (forwardedCandidates != null && user == 999) nodeScopeCandidates(forwardedCandidates)
                else withContext(Dispatchers.IO) {
                    RootServiceClient.getNodeScopePackages(user)?.let(::nodeScopeCandidates).orEmpty()
                }
            if (candidates.isEmpty()) {
                record(user, "未取得可申请的应用清单，请检查 Root 连接；没有变更作用域")
                return false
            }
            val missing = candidates - api.scope.toSet()
            if (missing.isNotEmpty()) {
                val result = CompletableDeferred<Boolean>()
                record(user, "等待 LSPosed 批量授权 ${missing.size} 个应用")
                toast("请确认 LSPosed 的批量作用域申请", forced = true)
                api.requestScope(missing.sorted(), object : XposedService.OnScopeEventListener {
                    override fun onScopeRequestApproved(scope: List<String>) { result.complete(true) }
                    override fun onScopeRequestFailed(message: String) { record(user, "批量申请未完成：$message"); result.complete(false) }
                })
                if (withTimeoutOrNull(90_000) { result.await() } != true) {
                    record(user, "申请未完成或超时，可手动重试；不会自动重复弹窗")
                    return false
                }
            }
            val remaining = candidates - api.scope.toSet()
            record(user, "已配置 ${candidates.size - remaining.size}/${candidates.size} 个应用；名单控制生效条件以运行策略为准")
            return remaining.isEmpty()
        } catch (error: Exception) {
            record(Process.myUid() / 100000, "批量配置失败：${error.message}")
            return false
        } finally { mutex.unlock() }
    }

    private fun record(user: Int, message: String) {
        status.value = "用户 $user：$message"
        runCatching { logFolder.resolve("node-scope-setup.json").writeText(JSONObject()
            .put("userId", user).put("status", message).put("timestamp", System.currentTimeMillis()).toString()) }
    }
}
