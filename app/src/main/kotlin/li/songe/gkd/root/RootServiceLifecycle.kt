package li.songe.gkd.root

import android.os.SystemClock
import android.os.Process
import android.util.AtomicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import li.songe.gkd.util.LogUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import li.songe.gkd.app
import li.songe.gkd.appScope
import li.songe.gkd.store.storeFlow

fun initRootServiceLifecycle() {
    val retryBudget = RootRetryBudget()
    val permissionBudget = RootPermissionWaitBudget()
    val completed = app.filesDir.resolve("module_root_ready_v1")
    val initialSetup = shouldWaitForInitialModuleRoot(
        Process.myUid() / 100000 == 0,
        runCatching { app.filesDir.resolve("module_bootstrap_v1").readText().trim() }.getOrNull(),
        completed.exists(),
    )
    appScope.launch {
        storeFlow
            .map { it.enableApkRoot }
            .distinctUntilChanged()
            .collect { enabled ->
                if (enabled) {
                    RootServiceClient.connect(app)
                } else {
                    retryBudget.reset()
                    permissionBudget.reset()
                    RootServiceClient.disconnect()
                }
            }
    }
    appScope.launch {
        RootServiceClient.state.collectLatest { state ->
            if (state is RootServiceState.Connected) {
                retryBudget.connected(SystemClock.elapsedRealtime())
                if (initialSetup && !completed.exists()) withContext(Dispatchers.IO) {
                    runCatching {
                        val file = AtomicFile(completed)
                        val output = file.startWrite()
                        try { output.write("1".toByteArray()); file.finishWrite(output) }
                        catch (error: Throwable) { file.failWrite(output); throw error }
                    }.onFailure { LogUtils.d("Initial Root completion marker failed", it) }
                }
                return@collectLatest
            }
            if (state !is RootServiceState.Failed || !storeFlow.value.enableApkRoot) return@collectLatest
            if (state.waitingForRootGrant) {
                // Fresh module setup only: bounded recovery when the user allows
                // Root in the manager after the initial request has already ended.
                if (!initialSetup || completed.exists()) return@collectLatest
                val wait = permissionBudget.consumeDelay(SystemClock.elapsedRealtime()) ?: return@collectLatest
                delay(wait)
                if (permissionBudget.withinWindow(SystemClock.elapsedRealtime()) &&
                    storeFlow.value.enableApkRoot && RootServiceClient.state.value == state
                ) RootServiceClient.connect(app)
                return@collectLatest
            }
            if (!state.retryable) return@collectLatest
            val attempt = retryBudget.consume(SystemClock.elapsedRealtime()) ?: return@collectLatest
            delay(AUTOMATIC_ROOT_RETRY_DELAY_MS * attempt)
            if (storeFlow.value.enableApkRoot && RootServiceClient.state.value == state) {
                RootServiceClient.connect(app)
            }
        }
    }
}

private const val AUTOMATIC_ROOT_RETRY_DELAY_MS = 750L
