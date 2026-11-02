package li.songe.gkd.root

import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import li.songe.gkd.appScope
import li.songe.gkd.store.*
import li.songe.gkd.util.LogUtils

private val nodeInventory = MutableStateFlow(emptySet<String>())

/** The app owns the policy. Hook processes only receive a short-lived, deny-first lease. */
fun initRootNodeBridgePolicy() {
    var service: XposedService? = null
    XposedServiceHelper.registerListener(object : XposedServiceHelper.OnServiceListener {
        override fun onServiceBind(value: XposedService) { service = value; NodeScopeSetup.service.value = value; publish() }
        override fun onServiceDied(value: XposedService) { service = null; NodeScopeSetup.service.value = null }
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
    if (android.os.Process.myUid() / 100000 == 0) appScope.launch(Dispatchers.IO) {
        while (true) {
            runCatching {
                if (RootServiceClient.state.value is RootServiceState.Connected) {
                    val owner = RootServiceClient.getNodeScopePackages(0)
                    nodeInventory.value = if (owner.isNullOrEmpty()) emptySet() else
                        owner + RootServiceClient.getNodeScopePackages(999).orEmpty()
                } else nodeInventory.value = emptySet()
            }.onFailure { nodeInventory.value = emptySet(); LogUtils.d("Root node inventory failed", it) }
            delay(if (nodeInventory.value.isEmpty()) 2000L else 15_000L)
        }
    }
}

private fun publishRootNodePolicy(service: XposedService?) {
    val store = storeFlow.value
    val enabled = service != null && android.os.Process.myUid() / 100000 == 0 &&
        store.enableApkRoot && store.enableAutomator && store.enableMatch &&
        detectorFolderScopeReadyFlow.value && RootServiceClient.state.value is RootServiceState.Connected
    val excluded = blockMatchAppListFlow.value + actualBlockA11yAppList + detectorFolderPackagesFlow.value + nodeBridgeProtectedPackages
    val allowed = nodeInventory.value - nodeBridgeProtectedPackages
    RootServiceClient.publishNodePolicy(enabled, excluded, allowed)
    val prefs = service?.getRemotePreferences("root_node_bridge") ?: return
    prefs.edit().putBoolean("enabled", enabled)
        .putLong("leaseUntil", if (enabled) System.currentTimeMillis() + 15_000L else 0L)
        .putStringSet("excluded", excluded)
        .putStringSet("allowed", allowed)
        .apply()
}
