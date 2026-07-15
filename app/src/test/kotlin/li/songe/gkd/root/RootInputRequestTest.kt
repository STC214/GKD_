package li.songe.gkd.root

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RootInputRequestTest {
    private val validTap = RootInputRequest(
        action = ROOT_INPUT_ACTION_TAP,
        displayId = 0,
        x1 = 500f,
        y1 = 800f,
        x2 = 500f,
        y2 = 800f,
        durationMs = 0,
        boundLeft = 0,
        boundTop = 0,
        boundRight = 1080,
        boundBottom = 2400,
    )

    @Test
    fun acceptsCanonicalTapAndSwipe() {
        assertNull(validateRootInputRequest(validTap))
        assertNull(
            validateRootInputRequest(
                validTap.copy(
                    action = ROOT_INPUT_ACTION_SWIPE,
                    x2 = 900f,
                    y2 = 1600f,
                    durationMs = 350,
                ),
            ),
        )
    }

    @Test
    fun rejectsUnsupportedAction() {
        assertEquals(
            RootInputFailure.UNSUPPORTED_ACTION,
            validateRootInputRequest(validTap.copy(action = 99)),
        )
    }

    @Test
    fun rejectsInvalidDisplay() {
        assertEquals(
            RootInputFailure.INVALID_DISPLAY,
            validateRootInputRequest(validTap.copy(displayId = -1)),
        )
    }

    @Test
    fun rejectsInvertedOrUnboundedRegion() {
        assertEquals(
            RootInputFailure.INVALID_BOUNDS,
            validateRootInputRequest(validTap.copy(boundRight = 0)),
        )
        assertEquals(
            RootInputFailure.INVALID_BOUNDS,
            validateRootInputRequest(validTap.copy(boundBottom = 1_000_001)),
        )
    }

    @Test
    fun rejectsNonFiniteCoordinates() {
        assertEquals(
            RootInputFailure.NON_FINITE_COORDINATE,
            validateRootInputRequest(validTap.copy(x1 = Float.NaN, x2 = Float.NaN)),
        )
        assertEquals(
            RootInputFailure.NON_FINITE_COORDINATE,
            validateRootInputRequest(
                validTap.copy(y1 = Float.POSITIVE_INFINITY, y2 = Float.POSITIVE_INFINITY),
            ),
        )
    }

    @Test
    fun rejectsCoordinatesOutsideHalfOpenBounds() {
        assertEquals(
            RootInputFailure.COORDINATE_OUT_OF_BOUNDS,
            validateRootInputRequest(validTap.copy(x1 = 1080f, x2 = 1080f)),
        )
        assertEquals(
            RootInputFailure.COORDINATE_OUT_OF_BOUNDS,
            validateRootInputRequest(validTap.copy(y1 = -1f, y2 = -1f)),
        )
    }

    @Test
    fun rejectsInvalidDurations() {
        assertEquals(
            RootInputFailure.INVALID_DURATION,
            validateRootInputRequest(validTap.copy(durationMs = 10_001)),
        )
        assertEquals(
            RootInputFailure.INVALID_DURATION,
            validateRootInputRequest(validTap.copy(action = ROOT_INPUT_ACTION_SWIPE)),
        )
    }

    @Test
    fun rejectsTapWithSecondCoordinate() {
        assertEquals(
            RootInputFailure.NON_CANONICAL_TAP,
            validateRootInputRequest(validTap.copy(x2 = 501f)),
        )
    }
}
