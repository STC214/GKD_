package li.songe.gkd.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionExecutionContextTest {
    @Test
    fun `bounds use right and bottom as exclusive edges`() {
        val bounds = ActionBounds(10, 20, 100, 200)

        assertTrue(bounds.contains(10f, 20f))
        assertTrue(bounds.contains(99.9f, 199.9f))
        assertFalse(bounds.contains(100f, 100f))
        assertFalse(bounds.contains(50f, 200f))
    }

    @Test
    fun `intersection rejects disjoint and zero-area bounds`() {
        val bounds = ActionBounds(0, 0, 100, 100)

        assertEquals(ActionBounds(50, 40, 100, 100), bounds.intersect(ActionBounds(50, 40, 120, 130)))
        assertNull(bounds.intersect(ActionBounds(100, 0, 120, 20)))
        assertNull(bounds.intersect(ActionBounds(120, 120, 140, 140)))
    }

    @Test
    fun `point must be inside both window and visible region`() {
        val context = ActionExecutionContext(
            displayId = 0,
            rotation = 1,
            windowId = 7,
            windowBounds = ActionBounds(0, 0, 1000, 1000),
            visibleBounds = ActionBounds(100, 200, 900, 800),
        )

        assertTrue(context.allowsPoint(500f, 500f))
        assertFalse(context.allowsPoint(50f, 500f))
        assertFalse(context.allowsPoint(500f, 900f))
    }
}
