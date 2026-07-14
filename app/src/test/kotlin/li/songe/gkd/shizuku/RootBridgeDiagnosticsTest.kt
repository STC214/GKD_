package li.songe.gkd.shizuku

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RootBridgeDiagnosticsTest {

    @Test
    fun parseRemoteUidSupportsRootAndShellIdentity() {
        assertEquals(
            0,
            parseRemoteUid("uid=0(root) gid=0(root) groups=0(root) context=u:r:ksu:s0")
        )
        assertEquals(
            2000,
            parseRemoteUid("uid=2000(shell) gid=2000(shell) groups=1003(graphics)")
        )
        assertNull(parseRemoteUid("permission denied"))
    }

    @Test
    fun rootBridgePhaseDoesNotTreatPartialBinderAsRoot() {
        assertEquals(
            RootBridgePhase.Disconnected,
            rootBridgePhase(
                contextAvailable = false,
                userServiceConnected = false,
                shellCommandAvailable = false,
                remoteUid = null,
                exhausted = false,
            )
        )
        assertEquals(
            RootBridgePhase.Partial,
            rootBridgePhase(
                contextAvailable = true,
                userServiceConnected = false,
                shellCommandAvailable = false,
                remoteUid = null,
                exhausted = false,
            )
        )
        assertEquals(
            RootBridgePhase.Failed,
            rootBridgePhase(
                contextAvailable = true,
                userServiceConnected = false,
                shellCommandAvailable = false,
                remoteUid = null,
                exhausted = true,
            )
        )
    }

    @Test
    fun rootBridgePhaseDistinguishesRootFromNonRoot() {
        assertEquals(
            RootBridgePhase.Root,
            rootBridgePhase(
                contextAvailable = true,
                userServiceConnected = true,
                shellCommandAvailable = true,
                remoteUid = 0,
                exhausted = false,
            )
        )
        assertEquals(
            RootBridgePhase.NonRoot,
            rootBridgePhase(
                contextAvailable = true,
                userServiceConnected = true,
                shellCommandAvailable = true,
                remoteUid = 2000,
                exhausted = false,
            )
        )
    }
}
