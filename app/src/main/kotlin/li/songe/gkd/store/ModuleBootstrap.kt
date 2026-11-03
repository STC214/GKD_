package li.songe.gkd.store

import android.os.Process
import android.util.AtomicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import li.songe.gkd.app
import li.songe.gkd.appScope
import li.songe.gkd.root.NodeScopeSetup
import li.songe.gkd.root.RootServiceClient
import li.songe.gkd.root.RootServiceState
import li.songe.gkd.util.LogUtils
import li.songe.gkd.util.storeFolder

internal fun shouldBootstrapModule(owner: Boolean, marker: String?, hasStore: Boolean): Boolean =
    owner && marker == "1" && !hasStore

internal fun moduleBootstrapDefaults(): SettingsStore = if (shouldBootstrapModule(
    owner = Process.myUid() / 100000 == 0,
    marker = runCatching { app.filesDir.resolve("module_bootstrap_v1").readText().trim() }.getOrNull(),
    hasStore = storeFolder.resolve("store.json").exists(),
)) SettingsStore(enableAutomator = true, enableApkRoot = true, enableMatch = true,
    enableStatusService = true) else SettingsStore()

/** Only fresh module installs request setup. No polling of the unstable framework debug CLI. */
fun initModuleBootstrap() {
    if (Process.myUid() / 100000 != 0 ||
        runCatching { app.filesDir.resolve("module_bootstrap_v1").readText().trim() }.getOrNull() != "1") return
    val attempted = app.filesDir.resolve("module_scope_requested_v1")
    if (attempted.exists() || !storeFlow.value.enableApkRoot || !storeFlow.value.enableAutomator) return
    appScope.launch(Dispatchers.IO) {
        try {
            val ready = withTimeoutOrNull(60_000L) {
                combine(RootServiceClient.state, NodeScopeSetup.service) { root, framework ->
                    root is RootServiceState.Connected && framework != null
                }.first { it }
            } == true
            if (!ready) {
                LogUtils.d("Fresh module setup awaits initial Root/LSPosed approval; use batch setup after enabling the module")
                return@launch
            }
            if (!storeFlow.value.enableApkRoot || !storeFlow.value.enableAutomator) return@launch
            val marker = AtomicFile(attempted)
            val stream = marker.startWrite()
            try { stream.write("1".toByteArray()); marker.finishWrite(stream) }
            catch (error: Throwable) { marker.failWrite(stream); throw error }
            if (NodeScopeSetup.requestBatch()) NodeScopeSetup.requestXSpaceBatch()
        } catch (error: Exception) { LogUtils.d("Fresh module scope setup failed", error) }
    }
}
