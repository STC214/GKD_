package li.songe.gkd.a11y

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WindowGenerationPolicyTest {
    @Test
    fun `state changes advance structural generation`() {
        assertTrue(shouldAdvanceWindowGeneration(STATE_CHANGED))
    }

    @Test
    fun `content changes use branch invalidation instead of global generation`() {
        assertFalse(shouldAdvanceWindowGeneration(CONTENT_CHANGED))
    }
}
