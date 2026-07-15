package li.songe.gkd.a11y

import org.junit.Assert.assertEquals
import org.junit.Test

class NodeCachePolicyTest {
    @Test
    fun `default policy shortens dynamic node lifetime`() {
        assertEquals(500L, NodeCachePolicy.Default.expiryMillis(hasText = true))
        assertEquals(1_000L, NodeCachePolicy.Default.expiryMillis(hasText = false))
    }

    @Test
    fun `legacy policy preserves upstream lifetime as device fallback`() {
        assertEquals(1_000L, NodeCachePolicy.Legacy.expiryMillis(hasText = true))
        assertEquals(2_000L, NodeCachePolicy.Legacy.expiryMillis(hasText = false))
    }
}
