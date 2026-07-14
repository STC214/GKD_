package li.songe.gkd.shizuku

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionCallbackGateTest {
    @Test
    fun `late value after cancellation is disposed`() {
        val disposed = mutableListOf<String>()
        var delivered: String? = null
        val gate = ConnectionCallbackGate<String>(disposed::add)
        gate.arm { delivered = it }

        gate.cancel()

        assertFalse(gate.complete("late"))
        assertNull(delivered)
        assertEquals(listOf("late"), disposed)
    }

    @Test
    fun `only first completion owns callback`() {
        val disposed = mutableListOf<String>()
        var delivered: String? = null
        val gate = ConnectionCallbackGate<String>(disposed::add)
        gate.arm { delivered = it }

        assertTrue(gate.complete("first"))
        assertFalse(gate.complete("second"))
        assertEquals("first", delivered)
        assertEquals(listOf("second"), disposed)
    }

    @Test
    fun `failure after cancellation cannot resume callback`() {
        var calls = 0
        val gate = ConnectionCallbackGate<String> { }
        gate.arm { calls++ }

        gate.cancel()

        assertFalse(gate.fail())
        assertEquals(0, calls)
    }
}
