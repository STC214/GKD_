package li.songe.gkd.shizuku

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InputSequenceResultTest {
    @Test
    fun `all input steps must succeed`() {
        val sequence = InputSequenceResult()

        assertTrue(sequence.record(true))
        assertTrue(sequence.record(true))
        assertTrue(sequence.succeeded)
    }

    @Test
    fun `successful up cannot hide failed move`() {
        val sequence = InputSequenceResult()

        assertTrue(sequence.record(true)) // DOWN
        assertFalse(sequence.record(false)) // MOVE
        assertTrue(sequence.record(true)) // UP
        assertFalse(sequence.succeeded)
    }

    @Test
    fun `failed down is immediately visible`() {
        val sequence = InputSequenceResult()

        assertFalse(sequence.record(false))
        assertFalse(sequence.succeeded)
    }
}
