package li.songe.gkd.root

/** Internal exclusions: never eligible for node-recovery hooks or automatic actions. */
val detectorFolderSeedPackages = setOf(
    "com.zhenxi.hunter", "com.chunqiunativecheck",
    "com.eltavine.duckdetector", "me.garfieldhan.holmes",
    "io.github.vvb2060.keyattestation", "com.lingqing.detector",
    "luna.safe.luna", "com.reveny.nativecheck", "icu.nullptr.nativetest",
    "com.android.nativetest", "io.liankong.riskdetector", "com.byxiaorun.detector",
)

data class LauncherFolderEntry(val id: Long, val container: Long, val packageName: String?)

fun findDetectorFolderPackages(entries: List<LauncherFolderEntry>): Set<String> {
    val anchors = setOf("com.zhenxi.hunter", "com.chunqiunativecheck")
    val folders = entries.filter { it.packageName in anchors && it.container >= 0 }
        .mapTo(hashSetOf()) { it.container }
    // Never treat the desktop/dock (negative container) as a detector folder.
    return entries.filter { it.container in folders }
        .mapNotNullTo(hashSetOf()) { it.packageName }
}

fun canRecoverHiddenNodes(
    appId: String,
    scopeReady: Boolean,
    matchWhitelist: Set<String>,
    a11yWhitelist: Set<String>,
    detectorWhitelist: Set<String>,
): Boolean = scopeReady && appId.isNotBlank() &&
    appId !in matchWhitelist && appId !in a11yWhitelist && appId !in detectorWhitelist
