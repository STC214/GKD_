package li.songe.gkd.runtime.foreground

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WindowRootRecoveryStateTest {
    private val context = WindowRootRecoveryContext(1, 2, "host.app")

    @Test
    fun `missing root cannot start recovery by itself`() {
        val state = WindowRootRecoveryState(monotonicMillis = { 100L })
        assertTrue(state.nextAttempt(context) is WindowRootRecoveryResult.Inactive)
    }

    @Test
    fun `transition grants only the finite backoff sequence`() {
        var now = 100L
        val state = WindowRootRecoveryState(monotonicMillis = { now })
        state.startTransition()

        val delays = buildList {
            repeat(5) {
                val result = state.nextAttempt(context) as WindowRootRecoveryResult.Scheduled
                add(result.delayMillis)
            }
        }
        assertEquals(listOf(50L, 100L, 200L, 400L, 800L), delays)
        assertEquals(
            5,
            (state.nextAttempt(context) as WindowRootRecoveryResult.Exhausted).attempts,
        )
        now += 1L
        assertTrue(state.nextAttempt(context) is WindowRootRecoveryResult.Inactive)
    }

    @Test
    fun `deadline cannot be extended by repeated missing roots`() {
        var now = 100L
        val state = WindowRootRecoveryState(monotonicMillis = { now })
        state.startTransition()
        state.nextAttempt(context)
        now = 3_101L
        assertTrue(state.nextAttempt(context) is WindowRootRecoveryResult.Exhausted)
        assertTrue(state.nextAttempt(context) is WindowRootRecoveryResult.Inactive)
    }

    @Test
    fun `successful root closes current recovery window`() {
        val state = WindowRootRecoveryState(monotonicMillis = { 100L })
        state.startTransition()
        state.nextAttempt(context)
        state.complete()
        assertTrue(state.nextAttempt(context) is WindowRootRecoveryResult.Inactive)
    }

    @Test
    fun `new explicit transition resets exhausted budget`() {
        val state = WindowRootRecoveryState(
            retryDelays = listOf(50L),
            transitionWindowMillis = 50L,
            monotonicMillis = { 100L },
        )
        state.startTransition()
        state.nextAttempt(context)
        assertTrue(state.nextAttempt(context) is WindowRootRecoveryResult.Exhausted)
        state.startTransition()
        val retry = state.nextAttempt(context) as WindowRootRecoveryResult.Scheduled
        assertEquals(50L, retry.delayMillis)
    }

    @Test
    fun `context changes share one transition attempt budget`() {
        val state = WindowRootRecoveryState(
            retryDelays = listOf(50L, 100L),
            transitionWindowMillis = 150L,
            monotonicMillis = { 100L },
        )
        state.startTransition()
        val first = state.nextAttempt(context) as WindowRootRecoveryResult.Scheduled
        val second = state.nextAttempt(context.copy(windowId = 3)) as WindowRootRecoveryResult.Scheduled
        assertEquals(50L, first.delayMillis)
        assertEquals(100L, second.delayMillis)
        assertTrue(
            state.nextAttempt(context.copy(windowId = 4)) is WindowRootRecoveryResult.Exhausted
        )
    }
}
