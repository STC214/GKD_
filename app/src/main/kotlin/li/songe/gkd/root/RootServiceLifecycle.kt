package li.songe.gkd.root

import android.os.SystemClock
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
    appScope.launch {
        storeFlow
            .map { it.enableApkRoot }
            .distinctUntilChanged()
            .collect { enabled ->
                if (enabled) {
                    RootServiceClient.connect(app)
                } else {
                    retryBudget.reset()
                    RootServiceClient.disconnect()
                }
            }
    }
    appScope.launch {
        RootServiceClient.state.collectLatest { state ->
            if (state is RootServiceState.Connected) {
                retryBudget.connected(SystemClock.elapsedRealtime())
                return@collectLatest
            }
            if (state !is RootServiceState.Failed ||
                !state.retryable ||
                !storeFlow.value.enableApkRoot
            ) {
                return@collectLatest
            }
            val attempt = retryBudget.consume(SystemClock.elapsedRealtime()) ?: return@collectLatest
            delay(AUTOMATIC_ROOT_RETRY_DELAY_MS * attempt)
            if (storeFlow.value.enableApkRoot && RootServiceClient.state.value == state) {
                RootServiceClient.connect(app)
            }
        }
    }
}

private const val AUTOMATIC_ROOT_RETRY_DELAY_MS = 750L
