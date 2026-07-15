package li.songe.gkd.data

import kotlinx.serialization.Serializable

@Serializable
enum class ActionVerificationState {
    NotRequested,
    Pending,
    Verified,
    Inconclusive,
}

@Serializable
enum class ActionVerificationSignal {
    TargetDisappeared,
    WindowChanged,
    GenerationChanged,
}

data class ActionVerificationObservation(
    val targetAvailable: Boolean,
    val windowCurrent: Boolean,
    val generationCurrent: Boolean,
)

sealed class ActionVerificationDecision {
    data class Wait(val delayMillis: Long) : ActionVerificationDecision()
    data class Verified(val signal: ActionVerificationSignal) : ActionVerificationDecision()
    data object Inconclusive : ActionVerificationDecision()
}

class ActionVerificationStateMachine(
    private val timeoutMillis: Long = 350L,
    private val intervalMillis: Long = 50L,
) {
    init {
        require(timeoutMillis >= 0L)
        require(intervalMillis > 0L)
    }

    fun observe(
        elapsedMillis: Long,
        observation: ActionVerificationObservation,
    ): ActionVerificationDecision {
        if (!observation.windowCurrent) {
            return ActionVerificationDecision.Verified(ActionVerificationSignal.WindowChanged)
        }
        if (!observation.generationCurrent) {
            return ActionVerificationDecision.Verified(ActionVerificationSignal.GenerationChanged)
        }
        if (!observation.targetAvailable) {
            return ActionVerificationDecision.Verified(ActionVerificationSignal.TargetDisappeared)
        }
        if (elapsedMillis >= timeoutMillis) return ActionVerificationDecision.Inconclusive
        return ActionVerificationDecision.Wait(
            minOf(intervalMillis, timeoutMillis - elapsedMillis),
        )
    }
}

internal fun ActionResult.shouldObserveAfterAction(): Boolean {
    if (!result) return false
    return action != ActionPerformer.None.action && action != ActionPerformer.Swipe.action
}
