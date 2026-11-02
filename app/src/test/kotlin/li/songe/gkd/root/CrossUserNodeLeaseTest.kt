package li.songe.gkd.root

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrossUserNodeLeaseTest {
    private val uid = 999 * 100000 + 12345
    private val camera = setOf("com.tencent.zebra")
    private val lease = CrossUserNodeLease(true, 2000, allowed = camera + "com.mihoyo.hyperion" + "example.allowed")

    @Test fun acceptsOnlyCloneCamera() { assertTrue(lease.permits(uid, camera, 1000)) }
    @Test fun acceptsCloneHyperion() { assertTrue(lease.permits(uid, setOf("com.mihoyo.hyperion"), 1000)) }
    @Test fun excludesCloneHyperion() {
        val app = setOf("com.mihoyo.hyperion")
        assertFalse(lease.copy(excluded = app).permits(uid, app, 1000))
    }
    @Test fun exclusionsArePerPackage() {
        assertTrue(lease.copy(excluded = camera).permits(uid, setOf("com.mihoyo.hyperion"), 1000))
    }
    @Test fun evenTwoTrialPackagesSharingUidAreDenied() {
        assertFalse(lease.permits(uid, camera + "com.mihoyo.hyperion", 1000))
    }
    @Test fun acceptsAnotherOwnerApprovedApp() { assertTrue(lease.permits(uid, setOf("example.allowed"), 1000)) }
    @Test fun emptyAllowedSetDenies() { assertFalse(lease.copy(allowed = emptySet()).permits(uid, camera, 1000)) }
    @Test fun defaultDenied() { assertFalse(CrossUserNodeLease().permits(uid, camera, 1000)) }
    @Test fun disabledDenied() { assertFalse(lease.copy(enabled = false).permits(uid, camera, 1000)) }
    @Test fun expiredDenied() { assertFalse(lease.permits(uid, camera, 2000)) }
    @Test fun staleDenied() { assertFalse(lease.permits(uid, camera, 3000)) }
    @Test fun invalidClockDenied() { assertFalse(lease.permits(uid, camera, -1)) }
    @Test fun ownerUsesOriginalPath() { assertFalse(lease.permits(12345, camera, 1000)) }
    @Test fun otherUserDenied() { assertFalse(lease.permits(10 * 100000 + 12345, camera, 1000)) }
    @Test fun systemUidDenied() { assertFalse(lease.permits(999 * 100000 + 1000, camera, 1000)) }
    @Test fun unknownUidDenied() { assertFalse(lease.permits(uid, emptySet(), 1000)) }
    @Test fun otherPackageDenied() { assertFalse(lease.permits(uid, setOf("example.other"), 1000)) }
    @Test fun sharedUidDenied() { assertFalse(lease.permits(uid, camera + "example.other", 1000)) }
    @Test fun whitelistedDenied() { assertFalse(lease.copy(excluded = camera).permits(uid, camera, 1000)) }
}
