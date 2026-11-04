package li.songe.gkd.util

/** Fork distribution policy; subscription updates are independent of APK updates. */
object AppDistribution {
    const val repositoryUrl = "https://github.com/STC214/GKD_"
    const val releasesUrl = "$repositoryUrl/releases"
    const val upstreamRepositoryUrl = "https://github.com/gkd-kit/gkd"
    // An upstream APK does not contain this fork's global bridge integration.
    // Use the fork release page so the APK and Root module can be updated together.
    const val allowUpstreamApkUpdates = false

    fun sourceUrl(commitId: String, tagName: String?): String =
        "$repositoryUrl/" + if (!tagName.isNullOrBlank()) "tree/$tagName" else "commit/$commitId"
}
