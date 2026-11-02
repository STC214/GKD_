package li.songe.gkd.root

internal val nodeBridgeProtectedPackages = detectorFolderSeedPackages + setOf(
    "android", "li.songe.gkd", "li.songe.gkd.debug", "com.android.systemui",
    "com.android.settings", "com.android.shell", "org.lsposed.manager", "org.frknkrc44.hma_oss",
)

/** Scope is a stable ceiling; normal whitelist changes only alter runtime eligibility. */
internal fun nodeScopeCandidates(installedUserApps: Set<String>): Set<String> =
    installedUserApps.filterTo(sortedSetOf()) { it.isNotBlank() && it !in nodeBridgeProtectedPackages }
