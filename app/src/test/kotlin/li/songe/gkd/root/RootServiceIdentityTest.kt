package li.songe.gkd.root

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RootServiceIdentityTest {
    private val validIdentity = RootServiceIdentity(
        protocolVersion = ROOT_SERVICE_PROTOCOL_VERSION,
        remotePid = 1234,
        remoteUid = 0,
        servicePackageName = "li.songe.gkd",
    )

    @Test
    fun acceptsValidRootIdentity() {
        assertNull(validateRootServiceIdentity(validIdentity, "li.songe.gkd"))
    }

    @Test
    fun rejectsProtocolMismatch() {
        assertEquals(
            RootIdentityFailure.PROTOCOL_MISMATCH,
            validateRootServiceIdentity(validIdentity.copy(protocolVersion = 2), "li.songe.gkd"),
        )
    }

    @Test
    fun rejectsNonRootUid() {
        assertEquals(
            RootIdentityFailure.NOT_ROOT,
            validateRootServiceIdentity(validIdentity.copy(remoteUid = 2000), "li.songe.gkd"),
        )
    }

    @Test
    fun rejectsWrongPackage() {
        assertEquals(
            RootIdentityFailure.PACKAGE_MISMATCH,
            validateRootServiceIdentity(validIdentity.copy(servicePackageName = "example.other"), "li.songe.gkd"),
        )
    }
}
