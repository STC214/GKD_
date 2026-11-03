package li.songe.gkd.root

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Global injection must not turn the old subscription inventory into an implicit allow-list. */
class GlobalNodeLeaseTest {
    private val ownerUid = 12345
    private val cloneUid = 999 * 100000 + ownerUid
    private val app = setOf("example.newly.installed")
    private val lease = CrossUserNodeLease(
        enabled = true,
        expiresElapsed = 2000,
        allowed = emptySet(),
        global = true,
    )

    @Test fun ownerAppIsAllowedWithoutScopeOrInventoryEntry() {
        assertTrue(lease.permits(ownerUid, app, 1000))
    }

    @Test fun cloneAppIsAllowedWithoutScopeOrInventoryEntry() {
        assertTrue(lease.permits(cloneUid, app, 1000))
    }

    @Test fun globalModeDoesNotRequireAnAllowedPackageList() {
        val unrelatedInventory = lease.copy(allowed = setOf("example.other"))
        assertTrue(unrelatedInventory.permits(ownerUid, app, 1000))
        assertTrue(unrelatedInventory.permits(cloneUid, app, 1000))
    }

    @Test fun disablingGlobalRestoresLegacyCloneAllowListRequirement() {
        assertFalse(lease.copy(global = false).permits(cloneUid, app, 1000))
        assertFalse(lease.copy(global = false, allowed = app).permits(ownerUid, app, 1000))
    }

    @Test fun ownerWhitelistWinsOverGlobalMode() {
        assertFalse(lease.copy(excluded = app).permits(ownerUid, app, 1000))
    }

    @Test fun cloneWhitelistWinsOverGlobalMode() {
        assertFalse(lease.copy(excluded = app, allowed = app).permits(cloneUid, app, 1000))
    }

    @Test fun whitelistDoesNotBlockUnrelatedApplication() {
        assertTrue(lease.copy(excluded = setOf("example.other")).permits(ownerUid, app, 1000))
    }

    @Test fun protectedOwnerPackageIsStillDenied() {
        val gkd = setOf("li.songe.gkd")
        assertFalse(lease.copy(excluded = gkd).permits(ownerUid, gkd, 1000))
    }

    @Test fun disabledLeaseIsDeniedForBothProfiles() {
        assertFalse(lease.copy(enabled = false).permits(ownerUid, app, 1000))
        assertFalse(lease.copy(enabled = false).permits(cloneUid, app, 1000))
    }

    @Test fun exactExpiryAndExpiredLeaseAreDenied() {
        assertFalse(lease.permits(ownerUid, app, 2000))
        assertFalse(lease.permits(cloneUid, app, 2001))
    }

    @Test fun invalidElapsedClockIsDenied() {
        assertFalse(lease.permits(ownerUid, app, -1))
        assertFalse(lease.copy(expiresElapsed = 0).permits(cloneUid, app, 0))
    }

    @Test fun systemAndRootUidsAreDeniedForBothProfiles() {
        for (uid in listOf(0, 1000, 9999, 999 * 100000 + 1000, 999 * 100000 + 9999)) {
            assertFalse("uid=$uid", lease.permits(uid, app, 1000))
        }
    }

    @Test fun isolatedUidRangeIsDeniedForBothProfiles() {
        for (uid in listOf(90000, 98999, 99000, 99999, 999 * 100000 + 90000,
            999 * 100000 + 98999, 999 * 100000 + 99000, 999 * 100000 + 99999)) {
            assertFalse("uid=$uid", lease.permits(uid, app, 1000))
        }
    }

    @Test fun sdkSandboxAndOtherReservedUidRangesAreDenied() {
        for (appId in listOf(20000, 29999, 30000, 50000, 89999)) {
            for (user in listOf(0, 999)) {
                assertFalse("user=$user appId=$appId", lease.permits(user * 100000 + appId, app, 1000))
            }
        }
    }

    @Test fun ordinaryApplicationUidBoundariesAreAcceptedInBothProfiles() {
        for (appId in listOf(10000, 19999)) {
            for (user in listOf(0, 999)) {
                assertTrue("user=$user appId=$appId", lease.permits(user * 100000 + appId, app, 1000))
            }
        }
    }

    @Test fun otherUserProfilesAreDenied() {
        for (user in listOf(1, 10, 998, 1000)) {
            assertFalse("user=$user", lease.permits(user * 100000 + ownerUid, app, 1000))
        }
    }

    @Test fun unknownAndSharedUidsAreDenied() {
        assertFalse(lease.permits(ownerUid, emptySet(), 1000))
        assertFalse(lease.permits(cloneUid, app + "example.shared", 1000))
    }

    @Test fun aFreshGlobalLeaseIsStillDisabledUntilOwnerPublishesIt() {
        assertFalse(CrossUserNodeLease(global = true).permits(ownerUid, app, 0))
    }
}
