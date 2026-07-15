package li.songe.gkd.root

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RootCallerVerifierTest {
    @Test
    fun acceptsExpectedUidPackageAndSignatures() {
        val result = verifyRootCaller(10234, 10234, "li.songe.gkd", listOf("li.songe.gkd")) { true }
        assertTrue(result.verified)
    }

    @Test
    fun rejectsDifferentUidBeforePackageChecks() {
        val result = verifyRootCaller(10234, 0, "li.songe.gkd", listOf("li.songe.gkd")) { true }
        assertEquals(RootCallerFailure.UID_MISMATCH, result.failure)
    }

    @Test
    fun rejectsUidWithoutExpectedPackage() {
        val result = verifyRootCaller(10234, 10234, "li.songe.gkd", listOf("example.other")) { true }
        assertEquals(RootCallerFailure.PACKAGE_MISSING, result.failure)
    }

    @Test
    fun rejectsUnsignedPackageSharingUid() {
        val result = verifyRootCaller(
            10234,
            10234,
            "li.songe.gkd",
            listOf("li.songe.gkd", "example.shared"),
        ) { it == "li.songe.gkd" }
        assertEquals(RootCallerFailure.SIGNATURE_MISMATCH, result.failure)
    }
}
