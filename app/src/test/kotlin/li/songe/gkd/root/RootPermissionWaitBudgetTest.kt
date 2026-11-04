package li.songe.gkd.root

import org.junit.Assert.*
import org.junit.Test

class RootPermissionWaitBudgetTest {
    @Test fun onlyFreshOwnerModuleSetupWaits() {
        assertTrue(shouldWaitForInitialModuleRoot(true, "1", false))
        assertFalse(shouldWaitForInitialModuleRoot(false, "1", false))
        assertFalse(shouldWaitForInitialModuleRoot(true, null, false))
        assertFalse(shouldWaitForInitialModuleRoot(true, "0", false))
        assertFalse(shouldWaitForInitialModuleRoot(true, "1", true))
    }
    @Test fun boundsAttemptCount() {
        val b = RootPermissionWaitBudget()
        repeat(20) { assertEquals(15_000L, b.consumeDelay(0)) }
        assertNull(b.consumeDelay(0))
    }
    @Test fun boundsElapsedWindowAndFinalDelay() {
        val b = RootPermissionWaitBudget()
        assertEquals(15_000L, b.consumeDelay(10))
        assertEquals(1L, b.consumeDelay(300_009))
        assertTrue(b.withinWindow(300_010))
        assertNull(b.consumeDelay(300_010))
        assertFalse(b.withinWindow(300_011))
    }
    @Test fun rejectsClockAnomalies() {
        val b = RootPermissionWaitBudget()
        assertNull(b.consumeDelay(-1)); assertFalse(b.withinWindow(0))
        b.consumeDelay(100); assertNull(b.consumeDelay(99)); assertFalse(b.withinWindow(99))
    }
    @Test fun explicitResetRenewsBudget() {
        val b = RootPermissionWaitBudget(maxAttempts = 1)
        b.consumeDelay(1); assertNull(b.consumeDelay(2)); b.reset()
        assertEquals(15_000L, b.consumeDelay(3))
    }
    @Test fun disabledRetryBudgetDoesNotRequestRoot() {
        assertNull(RootPermissionWaitBudget(maxAttempts = 0).consumeDelay(0))
    }
}
