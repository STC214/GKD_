package li.songe.gkd.root

import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import li.songe.gkd.appScope
import li.songe.gkd.store.*
import li.songe.gkd.util.LogUtils

/** The app owns the policy. Hook processes only receive a short-lived, deny-first lease. */
fun initRootNodeBridgePolicy() {
    var service: XposedService? = null
    XposedServiceHelper.registerListener(object : XposedServiceHelper.OnServiceListener {
        override fun onServiceBind(value: XposedService) { service = value; publish() }
        override fun onServiceDied(value: XposedService) { service = null }
        private fun publish() {
            runCatching { publishRootNodePolicy(service) }.onFailure { LogUtils.d("Node bridge policy sync failed", it) }
        }
    })
    appScope.launch {
        while (true) {
            runCatching { publishRootNodePolicy(service) }.onFailure { LogUtils.d("Node bridge policy sync failed", it) }
            delay(2000L)
        }
    }
}

private fun publishRootNodePolicy(service: XposedService?) {
    val prefs = service?.getRemotePreferences("root_node_bridge") ?: return
    val store = storeFlow.value
    val enabled = store.enableApkRoot && store.enableAutomator && store.enableMatch &&
        detectorFolderScopeReadyFlow.value && RootServiceClient.state.value is RootServiceState.Connected
    val excluded = blockMatchAppListFlow.value + actualBlockA11yAppList + detectorFolderPackagesFlow.value
    prefs.edit().putBoolean("enabled", enabled)
        .putLong("leaseUntil", if (enabled) System.currentTimeMillis() + 15_000L else 0L)
        .putStringSet("excluded", excluded)
        .putStringSet("allowed", setOf("com.tencent.zebra"))
        .apply()
}
