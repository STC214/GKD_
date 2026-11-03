package li.songe.gkd.data

import li.songe.selector.Selector
import li.songe.selector.Transform
import li.songe.selector.QueryContext
import li.songe.selector.getCharSequenceAttr
import org.junit.Assert.*
import org.junit.Test

class KekeSplashSelectorTest {
    private data class Node(val attrs: Map<String, Any?>)
    private val transform = Transform<Node>(
        getName = { "android.widget.TextView" },
        getChildren = { emptySequence() },
        getParent = { null },
        getAttr = { target, name -> when (target) {
            is Node -> target.attrs[name]
            is QueryContext<*> -> (target.current as Node).attrs[name]
            is CharSequence -> getCharSequenceAttr(target, name)
            else -> null
        } },
    )
    private fun node(text: String = "4秒 点击跳过", id: String = "com.kkdyC1V260818.T150254:id/txt_status", visible: Boolean = true, clickable: Boolean = true) =
        Node(mapOf("text" to text, "id" to id, "width" to 518, "height" to 123, "visibleToUser" to visible, "clickable" to clickable))
    private fun matches(node: Node) = Selector.parse(RuleSelectorCompat.KEKE_SPLASH_SELECTOR).match(node, transform, li.songe.selector.MatchOption.default)

    @Test fun `recorded 518px button misses generic rule but matches precise fallback`() {
        val node = node()
        assertNull(Selector.parse(RuleSelectorCompat.GLOBAL_SPLASH_WIDTH_SELECTOR).match(node, transform, li.songe.selector.MatchOption.default))
        assertSame(node, matches(node))
    }
    @Test fun `rejects another package with same local id`() { assertNull(matches(node(id = "other.app:id/txt_status"))) }
    @Test fun `rejects loading status and video skip text`() {
        listOf("正在启动，请稍候…", "跳过片头", "点击跳过", "4秒 点击跳过广告详情").forEach { assertNull(matches(node(it))) }
    }
    @Test fun `rejects hidden or nonclickable status`() {
        assertNull(matches(node(visible = false)))
        assertNull(matches(node(clickable = false)))
    }
    @Test fun `accepts countdown range zero through two digits`() {
        listOf("0秒 点击跳过", "9秒 点击跳过", "10秒 点击跳过").forEach { assertNotNull(matches(node(it))) }
    }
    @Test fun `compat only extends exact global splash source once`() {
        val source = listOf(RuleSelectorCompat.GLOBAL_SPLASH_WIDTH_SELECTOR)
        val resolved = RuleSelectorCompat.resolveAnyMatchSources(null, source)
        assertEquals(source + RuleSelectorCompat.KEKE_SPLASH_SELECTOR, resolved)
        assertSame(resolved, RuleSelectorCompat.resolveAnyMatchSources(null, resolved))
        assertSame(source, RuleSelectorCompat.resolveAnyMatchSources("other.app", source))
        val fixed = listOf("[text=\"new upstream selector\"]")
        assertSame(fixed, RuleSelectorCompat.resolveAnyMatchSources(null, fixed))
    }
}
