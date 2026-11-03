package li.songe.gkd.a11y

import org.junit.Assert.*
import org.junit.Test

class ServiceSessionTest {
    @Test fun `same mode replacement rejects old engine`() {
        val session = ServiceSession<Any>(); val old = Any(); val replacement = Any()
        val ticket = session.activate(old); session.activate(replacement)
        assertFalse(session.isCurrent(old, ticket)); assertSame(replacement, session.current())
    }
    @Test fun `late old shutdown does not clear replacement`() {
        val session = ServiceSession<Any>(); val old = Any(); val replacement = Any()
        session.activate(old); val ticket = session.activate(replacement); session.release(old)
        assertTrue(session.isCurrent(replacement, ticket))
    }
    @Test fun `two connects in same millisecond have different tickets`() {
        val session = ServiceSession<Any>(); val owner = Any()
        val first = session.activate(owner); val second = session.activate(owner)
        assertFalse(session.isCurrent(owner, first)); assertTrue(session.isCurrent(owner, second))
    }
    @Test fun `active shutdown removes service without fallback to old instance`() {
        val session = ServiceSession<Any>(); val owner = Any(); session.activate(owner); session.release(owner)
        assertNull(session.current())
    }
    @Test fun `rapid mode switch can reclaim an existing engine and invalidate old cleanup`() {
        val session = ServiceSession<Any>(); val automation = Any(); val native = Any()
        val oldAutoTicket = session.activate(automation)
        val nativeTicket = session.activate(native)
        val reclaimed = session.activate(automation)
        assertFalse(session.isCurrent(native, nativeTicket))
        assertFalse(session.isCurrent(automation, oldAutoTicket))
        assertTrue(session.isCurrent(automation, reclaimed))
        session.release(native); assertSame(automation, session.current())
    }
    @Test fun `future sender time cannot suppress valid events from another producer`() {
        assertFalse(validEventTimestamp(Long.MAX_VALUE, 1000))
        assertTrue(validEventTimestamp(900, 1000)); assertTrue(validEventTimestamp(800, 1001))
    }
    @Test fun `negative time rejected and zero or current accepted`() {
        assertFalse(validEventTimestamp(-1, 1000)); assertFalse(validEventTimestamp(1, -1))
        assertTrue(validEventTimestamp(0, 0)); assertTrue(validEventTimestamp(1000, 1000))
    }
}
