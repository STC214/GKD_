package li.songe.gkd.root

import org.junit.Assert.*
import org.junit.Test

class NodeScopePolicyTest {
    @Test fun includesArbitraryUserAppsNotJustTrialTargets() {
        assertEquals(setOf("example.one", "example.two"), nodeScopeCandidates(setOf("example.one", "example.two")))
    }
    @Test fun excludesCoreComponents() {
        assertTrue(nodeScopeCandidates(setOf("android", "li.songe.gkd", "com.android.systemui", "com.android.settings")).isEmpty())
    }
    @Test fun excludesSeedDetectors() { assertTrue(nodeScopeCandidates(detectorFolderSeedPackages).isEmpty()) }
    @Test fun dropsBlankNames() { assertTrue(nodeScopeCandidates(setOf("", "  ")).isEmpty()) }
    @Test fun emptyInventoryFailsClosed() { assertTrue(nodeScopeCandidates(emptySet()).isEmpty()) }
    @Test fun runtimeWhitelistDoesNotShrinkScopeCeiling() {
        val desired = nodeScopeCandidates(setOf("example.one"))
        val uid = 999 * 100000 + 12345
        val allowed = CrossUserNodeLease(true, 2000, allowed = desired)
        assertTrue(allowed.permits(uid, desired, 1000))
        assertFalse(allowed.copy(excluded = desired).permits(uid, desired, 1000))
        assertTrue(allowed.copy(excluded = emptySet()).permits(uid, desired, 1000))
        assertEquals(desired, nodeScopeCandidates(setOf("example.one")))
    }
}
