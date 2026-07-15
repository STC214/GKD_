package li.songe.gkd.data

/**
 * Fork-only compatibility for known third-party rule regressions.
 *
 * Compatibility is activated by the target app and the exact stale selector instead of a
 * subscription id or version. An upstream rule update therefore remains authoritative: once the
 * stale selector is removed, this fallback stops being injected automatically.
 */
internal object RuleSelectorCompat {
    internal const val MIHOYO_APP_ID = "com.mihoyo.hyperion"
    internal const val MIHOYO_STAR_RAIL_ACTION_CD = 5_000L

    internal const val MIHOYO_SIGN_IN_STALE_SELECTOR =
        "WebView[text*=\"签到\"] >4 View[childCount=11] > @View[childCount=3][visibleToUser=true] > Image[index=0][text!=null]"

    internal const val MIHOYO_SIGN_IN_FALLBACK_SELECTOR =
        "[text$=\"每日签到\"][visibleToUser=true] <<n WebView >4 View[childCount=11] > @View[childCount=3][visibleToUser=true] > Image[index=0][text!=null]"

    internal const val MIHOYO_STAR_RAIL_SIGN_IN_STALE_SELECTOR =
        "WebView[text*=\"签到\"] >4 View[childCount=10] > View + TextView[childCount=0][visibleToUser=true]"

    internal const val MIHOYO_STAR_RAIL_SIGN_IN_FALLBACK_SELECTOR =
        "WebView >4 View[childCount=10] > View[childCount=6] + View[childCount=3] + TextView[childCount=0][visibleToUser=true]"

    private val selectorFallbacks = listOf(
        MIHOYO_SIGN_IN_STALE_SELECTOR to MIHOYO_SIGN_IN_FALLBACK_SELECTOR,
        MIHOYO_STAR_RAIL_SIGN_IN_STALE_SELECTOR to MIHOYO_STAR_RAIL_SIGN_IN_FALLBACK_SELECTOR,
    )

    fun resolveAnyMatchSources(appId: String?, sources: List<String>): List<String> {
        if (appId != MIHOYO_APP_ID) {
            return sources
        }
        var resolved = sources
        selectorFallbacks.forEach { (stale, fallback) ->
            if (stale in sources && fallback !in resolved) {
                resolved = resolved + fallback
            }
        }
        return resolved
    }

    fun resolveActionCd(appId: String?, sources: List<String>, actionCd: Long): Long {
        return if (
            appId == MIHOYO_APP_ID &&
            MIHOYO_STAR_RAIL_SIGN_IN_STALE_SELECTOR in sources
        ) {
            maxOf(actionCd, MIHOYO_STAR_RAIL_ACTION_CD)
        } else {
            actionCd
        }
    }
}
