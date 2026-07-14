package li.songe.gkd.a11y

import org.junit.Assert.assertEquals
import org.junit.Test

class A11yEventQueueTest {
    @Test
    fun `coalescing removes only contiguous matching prefix`() {
        val events = ArrayDeque(listOf("same-1", "same-2", "other", "same-3"))

        val consumed = events.removeMatchingPrefix { it.startsWith("same") }

        assertEquals(listOf("same-1", "same-2"), consumed)
        assertEquals(listOf("other", "same-3"), events.toList())
    }
}
