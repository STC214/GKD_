package li.songe.gkd.store

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import li.songe.gkd.appScope
import li.songe.gkd.a11y.ActivityScene
import li.songe.gkd.a11y.topActivityFlow
import li.songe.gkd.a11y.updateTopActivity
import li.songe.gkd.root.RootServiceClient
import li.songe.gkd.root.RootServiceState
import li.songe.gkd.root.canRecoverHiddenNodes
import li.songe.gkd.root.detectorFolderSeedPackages
import li.songe.gkd.util.AppListString
import li.songe.gkd.util.LogUtils

/** Separate persistent exclusions: manual white-list edits never erase folder protection. */
val detectorFolderPackagesFlow by lazy {
    createTextFlow("detector_folder_app_list",
        decode = { detectorFolderSeedPackages + (it?.let(AppListString::decode) ?: emptySet()) },
        encode = AppListString::encode)
}

val detectorFolderScopeReadyFlow = MutableStateFlow(false)

fun shouldRecoverHiddenNodes(appId: String): Boolean = canRecoverHiddenNodes(
    appId, detectorFolderScopeReadyFlow.value,
    blockMatchAppListFlow.value, actualBlockA11yAppList, detectorFolderPackagesFlow.value,
)

fun initDetectorFolderPolicy() {
    // Establish the built-in exclusions before the rule engine starts.
    blockMatchAppListFlow.update { it + detectorFolderPackagesFlow.value }
    appScope.launch {
        combine(blockMatchAppListFlow, detectorFolderPackagesFlow) { manual, automatic -> manual to automatic }
            .collect { (manual, automatic) ->
                if (!manual.containsAll(automatic)) blockMatchAppListFlow.update { it + automatic }
                val top = topActivityFlow.value
                if (top.appId in automatic) {
                    updateTopActivity(top.appId, top.activityId, scene = ActivityScene.RuleSummary)
                }
            }
    }
    appScope.launch(Dispatchers.IO) {
        RootServiceClient.state.collectLatest { state ->
            detectorFolderScopeReadyFlow.value = false
            if (state !is RootServiceState.Connected) return@collectLatest
            while (true) {
                val packages = RootServiceClient.getDetectorFolderPackages()
                detectorFolderScopeReadyFlow.value = packages != null
                if (packages != null) {
                    // Retain confirmed exclusions on moves/read failures; never silently widen hooks.
                    detectorFolderPackagesFlow.update { it + packages }
                    LogUtils.d("Detector folder whitelist", "members=${detectorFolderPackagesFlow.value.size}")
                }
                delay(15_000L)
            }
        }
    }
}
