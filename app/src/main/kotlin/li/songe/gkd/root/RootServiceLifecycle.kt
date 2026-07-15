package li.songe.gkd.root

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import li.songe.gkd.app
import li.songe.gkd.appScope
import li.songe.gkd.store.storeFlow

fun initRootServiceLifecycle() {
    var automaticRetries = 0
    appScope.launch {
        storeFlow
            .map { it.enableApkRoot }
            .distinctUntilChanged()
            .collect { enabled ->
                if (enabled) {
                    RootServiceClient.connect(app)
                } else {
                    automaticRetries = 0
                    RootServiceClient.disconnect()
                }
            }
    }
    appScope.launch {
        RootServiceClient.state.collectLatest { state ->
            if (state !is RootServiceState.Failed ||
                !state.retryable ||
                !storeFlow.value.enableApkRoot ||
                automaticRetries >= MAX_AUTOMATIC_ROOT_RETRIES
            ) {
                return@collectLatest
            }
            automaticRetries += 1
            delay(AUTOMATIC_ROOT_RETRY_DELAY_MS * automaticRetries)
            if (storeFlow.value.enableApkRoot && RootServiceClient.state.value == state) {
                RootServiceClient.connect(app)
            }
        }
    }
}

private const val MAX_AUTOMATIC_ROOT_RETRIES = 2
private const val AUTOMATIC_ROOT_RETRY_DELAY_MS = 750L
