package li.songe.gkd.root

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

const val ROOT_INPUT_ACTION_TAP = 1
const val ROOT_INPUT_ACTION_SWIPE = 2

const val ROOT_INPUT_RESULT_COMPLETED = 1
const val ROOT_INPUT_RESULT_REJECTED = 2
const val ROOT_INPUT_RESULT_UNAVAILABLE = 3
const val ROOT_INPUT_RESULT_FAILED = 4

@Parcelize
data class RootInputRequest(
    val action: Int,
    val displayId: Int,
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val durationMs: Long,
    val boundLeft: Int,
    val boundTop: Int,
    val boundRight: Int,
    val boundBottom: Int,
) : Parcelable

enum class RootInputFailure {
    UNSUPPORTED_ACTION,
    INVALID_DISPLAY,
    INVALID_BOUNDS,
    NON_FINITE_COORDINATE,
    COORDINATE_OUT_OF_BOUNDS,
    INVALID_DURATION,
    NON_CANONICAL_TAP,
}

private const val MAX_BOUND_COORDINATE = 1_000_000
private const val MAX_INPUT_DURATION_MS = 10_000L

fun validateRootInputRequest(request: RootInputRequest): RootInputFailure? {
    if (request.action != ROOT_INPUT_ACTION_TAP && request.action != ROOT_INPUT_ACTION_SWIPE) {
        return RootInputFailure.UNSUPPORTED_ACTION
    }
    if (request.displayId < 0) {
        return RootInputFailure.INVALID_DISPLAY
    }
    if (request.boundLeft < 0 || request.boundTop < 0 ||
        request.boundRight <= request.boundLeft || request.boundBottom <= request.boundTop ||
        request.boundRight > MAX_BOUND_COORDINATE ||
        request.boundBottom > MAX_BOUND_COORDINATE
    ) {
        return RootInputFailure.INVALID_BOUNDS
    }
    if (!request.x1.isFinite() || !request.y1.isFinite() ||
        !request.x2.isFinite() || !request.y2.isFinite()
    ) {
        return RootInputFailure.NON_FINITE_COORDINATE
    }
    fun contains(x: Float, y: Float): Boolean =
        x >= request.boundLeft && x < request.boundRight &&
            y >= request.boundTop && y < request.boundBottom

    if (!contains(request.x1, request.y1) || !contains(request.x2, request.y2)) {
        return RootInputFailure.COORDINATE_OUT_OF_BOUNDS
    }
    return when (request.action) {
        ROOT_INPUT_ACTION_TAP -> when {
            request.durationMs !in 0..MAX_INPUT_DURATION_MS -> RootInputFailure.INVALID_DURATION
            request.x1 != request.x2 || request.y1 != request.y2 -> RootInputFailure.NON_CANONICAL_TAP
            else -> null
        }

        ROOT_INPUT_ACTION_SWIPE -> {
            if (request.durationMs !in 1..MAX_INPUT_DURATION_MS) {
                RootInputFailure.INVALID_DURATION
            } else {
                null
            }
        }

        else -> RootInputFailure.UNSUPPORTED_ACTION
    }
}
