package li.songe.gkd.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionVerificationStateMachineTest {
    private val machine = ActionVerificationStateMachine(
        timeoutMillis = 350L,
        intervalMillis = 50L,
    )

    @Test
    fun `target disappearance verifies action`() {
        val result = machine.observe(
            elapsedMillis = 50L,
            observation = current(targetAvailable = false),
        )

        assertEquals(
            ActionVerificationDecision.Verified(ActionVerificationSignal.TargetDisappeared),
            result,
        )
    }

    @Test
    fun `window change has priority over other signals`() {
        val result = machine.observe(
            elapsedMillis = 50L,
            observation = ActionVerificationObservation(
                targetAvailable = false,
                windowCurrent = false,
                generationCurrent = false,
            ),
        )

        assertEquals(
            ActionVerificationDecision.Verified(ActionVerificationSignal.WindowChanged),
            result,
        )
    }

    @Test
    fun `generation change verifies within same window`() {
        val result = machine.observe(
            elapsedMillis = 100L,
            observation = current(generationCurrent = false),
        )

        assertEquals(
            ActionVerificationDecision.Verified(ActionVerificationSignal.GenerationChanged),
            result,
        )
    }

    @Test
    fun `stable observation waits only to deadline`() {
        assertEquals(
            ActionVerificationDecision.Wait(20L),
            machine.observe(330L, current()),
        )
    }

    @Test
    fun `stable observation becomes inconclusive without claiming failure`() {
        assertTrue(machine.observe(350L, current()) is ActionVerificationDecision.Inconclusive)
    }

    @Test
    fun `successful click is observed`() {
        assertTrue(ActionResult(action = "click", result = true).shouldObserveAfterAction())
    }

    @Test
    fun `failed action is never observed`() {
        assertTrue(!ActionResult(action = "click", result = false).shouldObserveAfterAction())
    }

    @Test
    fun `none and swipe preserve compatibility without observation delay`() {
        assertTrue(!ActionResult(action = "none", result = true).shouldObserveAfterAction())
        assertTrue(!ActionResult(action = "swipe", result = true).shouldObserveAfterAction())
    }

    private fun current(
        targetAvailable: Boolean = true,
        generationCurrent: Boolean = true,
    ) = ActionVerificationObservation(
        targetAvailable = targetAvailable,
        windowCurrent = true,
        generationCurrent = generationCurrent,
    )
}
