package li.songe.gkd.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class BundledDefaultRulesTest {
    @Test fun freshInstallSeedsBothComponents() {
        assertEquals(BundledDefaultPlan(true, true), planBundledDefaultRules(false, false, false))
    }
    @Test fun existingSubscriptionAndEditsArePreserved() {
        assertEquals(BundledDefaultPlan(false, false), planBundledDefaultRules(false, true, true))
    }
    @Test fun orphanedFileOnlyNeedsMetadata() {
        assertEquals(BundledDefaultPlan(true, false), planBundledDefaultRules(false, false, true))
    }
    @Test fun missingFileCanBeSeededWithoutChangingDisabledMetadata() {
        assertEquals(BundledDefaultPlan(false, true), planBundledDefaultRules(false, true, false))
    }
    @Test fun completedMigrationRespectsLaterUserDeletion() {
        assertNull(planBundledDefaultRules(true, false, false))
    }
    @Test fun writesMetadataThenSnapshotThenMarker() = runBlocking {
        val calls = mutableListOf<String>()
        applyBundledDefaultRules(BundledDefaultPlan(true, true),
            { calls += "metadata" }, { calls += "snapshot" }, { calls += "marker" })
        assertEquals(listOf("metadata", "snapshot", "marker"), calls)
    }
    @Test fun failedSnapshotDoesNotMarkCompletion() = runBlocking {
        var marked = false
        try {
            applyBundledDefaultRules(BundledDefaultPlan(true, true), {},
                { error("interrupted") }, { marked = true })
            fail("Expected interrupted write")
        } catch (_: IllegalStateException) { assertFalse(marked) }
    }
    @Test fun existingFilesNeverInvokeWriters() = runBlocking {
        var marked = false
        applyBundledDefaultRules(BundledDefaultPlan(false, false),
            { error("metadata overwritten") }, { error("file overwritten") }, { marked = true })
        assertTrue(marked)
    }
}
