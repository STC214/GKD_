package li.songe.gkd.root

import li.songe.gkd.hook.NodeBridgeLease
import org.junit.Assert.*
import org.junit.Test

class NodeBridgeLeaseTest {
    @Test fun `only short elapsed lease grants repair`() {
        assertTrue(NodeBridgeLease.isValid(true, 16000, 1000))
        assertFalse(NodeBridgeLease.isValid(false, 16000, 1000))
        assertFalse(NodeBridgeLease.isValid(true, 1000, 1000))
        assertFalse(NodeBridgeLease.isValid(true, 0, 1000))
        assertFalse(NodeBridgeLease.isValid(true, 16001, 1000))
    }
    @Test fun `invalid clock or giant deadline fails closed`() {
        assertFalse(NodeBridgeLease.isValid(true, Long.MAX_VALUE, 1000))
        assertFalse(NodeBridgeLease.isValid(true, 1000, -1))
        assertFalse(NodeBridgeLease.isValid(true, -1, 0))
    }
    @Test fun `expiry cannot be extended by a wall clock rollback`() {
        assertTrue(NodeBridgeLease.isValid(true, 15000, 14999))
        assertFalse(NodeBridgeLease.isValid(true, 15000, 15000))
    }
    @Test fun `reboot cannot replay expired wall deadline with matching elapsed range`() {
        assertFalse(NodeBridgeLease.isValid(true, 16000, 1000, 100000, 200000))
        assertTrue(NodeBridgeLease.isValid(true, 16000, 1000, 215000, 200000))
    }
    @Test fun `wall rollback never extends existing lease`() {
        assertFalse(NodeBridgeLease.isValid(true, 16000, 1000, 215000, 100000))
        assertFalse(NodeBridgeLease.isValid(true, 1000, 2000, 215000, 200000))
    }
}
