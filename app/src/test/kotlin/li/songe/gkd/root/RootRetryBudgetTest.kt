package li.songe.gkd.root

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RootRetryBudgetTest {
    @Test fun boundsConsecutiveFailures() {
        val budget = RootRetryBudget()
        assertEquals(1, budget.consume(0)); assertEquals(2, budget.consume(10))
        assertNull(budget.consume(20))
    }
    @Test fun stableConnectionRenewsExhaustedBudget() {
        val budget = RootRetryBudget()
        budget.consume(0); budget.consume(10); budget.connected(100)
        assertEquals(1, budget.consume(30_100))
        assertEquals(2, budget.consume(30_101)); assertNull(budget.consume(30_102))
    }
    @Test fun shortConnectionsDoNotCreateInfiniteRetryLoop() {
        val budget = RootRetryBudget()
        budget.connected(0); assertEquals(1, budget.consume(100))
        budget.connected(200); assertEquals(2, budget.consume(300))
        budget.connected(400); assertNull(budget.consume(500))
    }
    @Test fun clockAnomaliesDoNotRenewAndManualResetDoes() {
        val budget = RootRetryBudget()
        budget.consume(0); budget.consume(1); budget.connected(100)
        assertNull(budget.consume(99))
        budget.connected(-1); assertNull(budget.consume(30_000))
        budget.reset(); assertEquals(1, budget.consume(30_001))
    }
}
