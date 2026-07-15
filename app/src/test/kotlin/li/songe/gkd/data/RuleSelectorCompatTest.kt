package li.songe.gkd.data

import li.songe.selector.Selector
import li.songe.selector.QueryContext
import li.songe.selector.Transform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class RuleSelectorCompatTest {
    @Test
    fun `adds mihoyo fallback after exact stale selector`() {
        val sources = listOf(RuleSelectorCompat.MIHOYO_SIGN_IN_STALE_SELECTOR)

        val resolved = RuleSelectorCompat.resolveAnyMatchSources(
            appId = RuleSelectorCompat.MIHOYO_APP_ID,
            sources = sources,
        )

        assertEquals(sources + RuleSelectorCompat.MIHOYO_SIGN_IN_FALLBACK_SELECTOR, resolved)
        Selector.parse(resolved.last())
    }

    @Test
    fun `adds star rail fallback after exact stale selector`() {
        val sources = listOf(RuleSelectorCompat.MIHOYO_STAR_RAIL_SIGN_IN_STALE_SELECTOR)

        val resolved = RuleSelectorCompat.resolveAnyMatchSources(
            appId = RuleSelectorCompat.MIHOYO_APP_ID,
            sources = sources,
        )

        assertEquals(
            sources + RuleSelectorCompat.MIHOYO_STAR_RAIL_SIGN_IN_FALLBACK_SELECTOR,
            resolved,
        )
        Selector.parse(resolved.last())
    }

    @Test
    fun `does not change another app using the same selector`() {
        val sources = listOf(RuleSelectorCompat.MIHOYO_SIGN_IN_STALE_SELECTOR)

        val resolved = RuleSelectorCompat.resolveAnyMatchSources(
            appId = "example.app",
            sources = sources,
        )

        assertSame(sources, resolved)
    }

    @Test
    fun `stops applying after upstream removes stale selector`() {
        val sources = listOf("[text=\"upstream fixed\"]")

        val resolved = RuleSelectorCompat.resolveAnyMatchSources(
            appId = RuleSelectorCompat.MIHOYO_APP_ID,
            sources = sources,
        )

        assertSame(sources, resolved)
    }

    @Test
    fun `does not duplicate an upstreamed fallback`() {
        val sources = listOf(
            RuleSelectorCompat.MIHOYO_SIGN_IN_STALE_SELECTOR,
            RuleSelectorCompat.MIHOYO_SIGN_IN_FALLBACK_SELECTOR,
        )

        val resolved = RuleSelectorCompat.resolveAnyMatchSources(
            appId = RuleSelectorCompat.MIHOYO_APP_ID,
            sources = sources,
        )

        assertSame(sources, resolved)
    }

    @Test
    fun `fallback selects unsigned daily sign-in reward`() {
        val fixture = signInFixture(
            title = "米游社原神每日签到",
            rewardImageIndex = 0,
        )

        val target = testTransform.querySelector(
            fixture.root,
            Selector.parse(RuleSelectorCompat.MIHOYO_SIGN_IN_FALLBACK_SELECTOR),
        )

        assertSame(fixture.reward, target)
    }

    @Test
    fun `fallback rejects already signed daily page`() {
        val fixture = signInFixture(
            title = "【绝区零】每日签到",
            rewardImageIndex = 2,
        )

        val target = testTransform.querySelector(
            fixture.root,
            Selector.parse(RuleSelectorCompat.MIHOYO_SIGN_IN_FALLBACK_SELECTOR),
        )

        assertNull(target)
    }

    @Test
    fun `fallback rejects another page in generic mihoyo web activity`() {
        val fixture = signInFixture(
            title = "参量质变仪提醒",
            rewardImageIndex = 0,
        )

        val target = testTransform.querySelector(
            fixture.root,
            Selector.parse(RuleSelectorCompat.MIHOYO_SIGN_IN_FALLBACK_SELECTOR),
        )

        assertNull(target)
    }

    @Test
    fun `star rail fallback selects first collapsed unsigned reward`() {
        val fixture = starRailFixture(title = "《崩坏：星穹铁道》签到福利")

        val target = testTransform.querySelector(
            fixture.root,
            Selector.parse(RuleSelectorCompat.MIHOYO_STAR_RAIL_SIGN_IN_FALLBACK_SELECTOR),
        )

        assertSame(fixture.reward, target)
    }

    @Test
    fun `star rail fallback rejects another childCount ten web page`() {
        val fixture = starRailFixture(
            title = "参量质变仪提醒",
            hasStarRailRewardPrefix = false,
        )

        val target = testTransform.querySelector(
            fixture.root,
            Selector.parse(RuleSelectorCompat.MIHOYO_STAR_RAIL_SIGN_IN_FALLBACK_SELECTOR),
        )

        assertNull(target)
    }

    @Test
    fun `star rail compatibility lengthens short action cooldown`() {
        val actionCd = RuleSelectorCompat.resolveActionCd(
            appId = RuleSelectorCompat.MIHOYO_APP_ID,
            sources = listOf(RuleSelectorCompat.MIHOYO_STAR_RAIL_SIGN_IN_STALE_SELECTOR),
            actionCd = 1_000L,
        )

        assertEquals(RuleSelectorCompat.MIHOYO_STAR_RAIL_ACTION_CD, actionCd)
    }

    @Test
    fun `star rail compatibility keeps longer upstream cooldown`() {
        val actionCd = RuleSelectorCompat.resolveActionCd(
            appId = RuleSelectorCompat.MIHOYO_APP_ID,
            sources = listOf(RuleSelectorCompat.MIHOYO_STAR_RAIL_SIGN_IN_STALE_SELECTOR),
            actionCd = 8_000L,
        )

        assertEquals(8_000L, actionCd)
    }

    private data class Fixture(
        val root: TestNode,
        val reward: TestNode,
    )

    private class TestNode(
        val name: String,
        val attrs: Map<String, Any?> = emptyMap(),
        val children: List<TestNode> = emptyList(),
    ) {
        var parent: TestNode? = null

        init {
            children.forEach { it.parent = this }
        }
    }

    private fun signInFixture(title: String, rewardImageIndex: Int): Fixture {
        val image = TestNode(
            name = "android.widget.Image",
            attrs = mapOf(
                "index" to rewardImageIndex,
                "text" to "reward-image-id",
            ),
        )
        val reward = TestNode(
            name = "android.view.View",
            attrs = mapOf(
                "childCount" to 3,
                "visibleToUser" to true,
            ),
            children = listOf(image),
        )
        val rewards = TestNode(
            name = "android.view.View",
            attrs = mapOf("childCount" to 11),
            children = listOf(reward),
        )
        val contentLevel3 = TestNode("android.view.View", children = listOf(rewards))
        val contentLevel2 = TestNode("android.view.View", children = listOf(contentLevel3))
        val titleNode = TestNode(
            name = "android.widget.TextView",
            attrs = mapOf(
                "text" to title,
                "visibleToUser" to true,
            ),
        )
        val contentLevel1 = TestNode(
            name = "android.view.View",
            children = listOf(titleNode, contentLevel2),
        )
        val webView = TestNode(
            name = "android.webkit.WebView",
            children = listOf(contentLevel1),
        )
        return Fixture(
            root = TestNode("root", children = listOf(webView)),
            reward = reward,
        )
    }

    private fun starRailFixture(
        title: String,
        hasStarRailRewardPrefix: Boolean = true,
    ): Fixture {
        val profile = TestNode(
            name = "android.view.View",
            attrs = mapOf("childCount" to if (hasStarRailRewardPrefix) 6 else 1),
        )
        val previousReward = TestNode(
            name = "android.view.View",
            attrs = mapOf("childCount" to 3),
        )
        val reward = TestNode(
            name = "android.widget.TextView",
            attrs = mapOf(
                "text" to "×5000第14天",
                "childCount" to 0,
                "visibleToUser" to true,
            ),
        )
        val rewards = TestNode(
            name = "android.view.View",
            attrs = mapOf("childCount" to 10),
            children = listOf(profile, previousReward, reward),
        )
        val contentLevel3 = TestNode("android.view.View", children = listOf(rewards))
        val contentLevel2 = TestNode("android.view.View", children = listOf(contentLevel3))
        val titleNode = TestNode(
            name = "android.widget.TextView",
            attrs = mapOf(
                "text" to title,
                "visibleToUser" to true,
            ),
        )
        val contentLevel1 = TestNode(
            name = "android.view.View",
            children = listOf(titleNode, contentLevel2),
        )
        val webView = TestNode(
            name = "android.webkit.WebView",
            children = listOf(contentLevel1),
        )
        return Fixture(
            root = TestNode("root", children = listOf(webView)),
            reward = reward,
        )
    }

    private val testTransform = Transform<TestNode>(
        getAttr = { target, name ->
            val node = when (target) {
                is TestNode -> target
                is QueryContext<*> -> target.current as? TestNode
                else -> null
            }
            when (name) {
                "parent" -> node?.parent
                else -> node?.attrs?.get(name)
            }
        },
        getName = { it.name },
        getChildren = { it.children.asSequence() },
        getParent = { it.parent },
    )
}
