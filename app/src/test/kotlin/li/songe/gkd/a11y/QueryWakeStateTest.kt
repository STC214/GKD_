package li.songe.gkd.a11y

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class QueryWakeStateTest {
    @Test
    fun `request during query leaves one pending wake`() {
        val state = QueryWakeState<String, String>()

        assertTrue(state.request(QueryWakeRequest(event = "first")))
        assertFalse(state.request(QueryWakeRequest(event = "second")))
        assertTrue(state.isRunning())
        assertTrue(state.hasPending())

        assertEquals("second", state.complete()?.event)
        assertTrue(state.isRunning())
        assertFalse(state.hasPending())
        assertNull(state.complete())
        assertFalse(state.isRunning())
    }

    @Test
    fun `event storm is conflated to latest event`() {
        val state = QueryWakeState<Int, String>()
        assertTrue(state.request(QueryWakeRequest(event = 0)))

        repeat(1_000) { event ->
            assertFalse(state.request(QueryWakeRequest(event = event + 1)))
        }

        assertEquals(1_000, state.complete()?.event)
        assertNull(state.complete())
    }

    @Test
    fun `real event supersedes forced and delayed pending wakes`() {
        val state = QueryWakeState<String, String>()
        assertTrue(state.request(QueryWakeRequest(forced = true)))
        assertFalse(state.request(QueryWakeRequest(delayRule = "delayed")))
        assertFalse(state.request(QueryWakeRequest(event = "latest")))

        val pending = state.complete()
        assertEquals("latest", pending?.event)
        assertFalse(pending?.forced ?: true)
        assertNull(pending?.delayRule)
        assertNull(state.complete())
    }

    @Test
    fun `normal wake supersedes forced pending wake`() {
        val state = QueryWakeState<String, String>()
        assertTrue(state.request(QueryWakeRequest(forced = true)))
        assertFalse(state.request(QueryWakeRequest(forced = true, correlationId = 1)))
        assertFalse(state.request(QueryWakeRequest(correlationId = 2)))

        val pending = state.complete()
        assertFalse(pending?.forced ?: true)
        assertEquals(2L, pending?.correlationId)
        assertNull(state.complete())
    }

    @Test
    fun `delayed wake is retained ahead of forced wake`() {
        val state = QueryWakeState<String, String>()
        assertTrue(state.request(QueryWakeRequest(event = "active")))
        assertFalse(state.request(QueryWakeRequest(delayRule = "rule")))
        assertFalse(state.request(QueryWakeRequest(forced = true)))

        assertEquals("rule", state.complete()?.delayRule)
        assertNull(state.complete())
    }

    @Test
    fun `concurrent requests start one query and keep bounded followup`() {
        val state = QueryWakeState<Int, String>()
        val executor = Executors.newFixedThreadPool(64)
        val ready = CountDownLatch(64)
        val start = CountDownLatch(1)
        val done = CountDownLatch(64)
        val immediateStarts = AtomicInteger(0)

        repeat(64) { event ->
            executor.execute {
                ready.countDown()
                start.await()
                if (state.request(QueryWakeRequest(event = event))) {
                    immediateStarts.incrementAndGet()
                }
                done.countDown()
            }
        }

        assertTrue(ready.await(5, TimeUnit.SECONDS))
        start.countDown()
        assertTrue(done.await(5, TimeUnit.SECONDS))
        executor.shutdownNow()

        assertEquals(1, immediateStarts.get())
        assertNotNull(state.complete())
        assertNull(state.complete())
        assertFalse(state.isRunning())
    }

    @Test
    fun `claimed handoff remains single runner while next query is launching`() {
        val state = QueryWakeState<String, String>()
        assertTrue(state.request(QueryWakeRequest(event = "active")))
        assertFalse(state.request(QueryWakeRequest(event = "handoff")))

        assertEquals("handoff", state.complete()?.event)
        assertFalse(state.request(QueryWakeRequest(event = "during-handoff")))
        assertEquals("during-handoff", state.complete()?.event)
        assertNull(state.complete())
    }

    @Test
    fun `same event storm retains only latest pair`() {
        val buffer = QueryEventBuffer<Int> { _, _ -> true }

        repeat(10_000) { buffer.addAll(listOf(it)) }

        assertEquals(2, buffer.retainedCount())
        val batch = buffer.drain()
        assertTrue(batch.hadEvents)
        assertEquals(listOf(9_998, 9_999), batch.events)
        assertEquals(0, buffer.retainedCount())
    }

    @Test
    fun `mixed events collapse to root query marker`() {
        val buffer = QueryEventBuffer<String> { first, second ->
            first.substringBefore(':') == second.substringBefore(':')
        }

        buffer.addAll(listOf("view:1", "view:2", "window:3", "window:4"))

        assertEquals(2, buffer.retainedCount())
        val batch = buffer.drain()
        assertTrue(batch.hadEvents)
        assertNull(batch.events)
    }

    @Test
    fun `empty event buffer is distinguishable from root query marker`() {
        val buffer = QueryEventBuffer<Int> { first, second -> first == second }

        val batch = buffer.drain()

        assertFalse(batch.hadEvents)
        assertNull(batch.events)
    }
}
