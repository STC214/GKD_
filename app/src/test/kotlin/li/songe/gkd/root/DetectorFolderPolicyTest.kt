package li.songe.gkd.root

import org.junit.Assert.*
import org.junit.Test

class DetectorFolderPolicyTest {
    @Test fun includesSiblingsNotOtherFolders() {
        val entries = listOf(
            LauncherFolderEntry(1, 439, "com.zhenxi.hunter"),
            LauncherFolderEntry(2, 439, "com.chunqiunativecheck"),
            LauncherFolderEntry(3, 439, "example.newdetector"),
            LauncherFolderEntry(4, 440, "example.other"),
        )
        assertEquals(setOf("com.zhenxi.hunter", "com.chunqiunativecheck", "example.newdetector"), findDetectorFolderPackages(entries))
    }
    @Test fun desktopAnchorDoesNotWhitelistDesktop() {
        assertTrue(findDetectorFolderPackages(listOf(
            LauncherFolderEntry(1, -100, "com.zhenxi.hunter"),
            LauncherFolderEntry(2, -100, "example.other"),
        )).isEmpty())
    }
    @Test fun everyWhitelistAndUnknownScopeDenyHook() {
        assertFalse(canRecoverHiddenNodes("example.app", false, emptySet(), emptySet(), emptySet()))
        assertFalse(canRecoverHiddenNodes("example.app", true, setOf("example.app"), emptySet(), emptySet()))
        assertFalse(canRecoverHiddenNodes("example.app", true, emptySet(), setOf("example.app"), emptySet()))
        assertFalse(canRecoverHiddenNodes("example.app", true, emptySet(), emptySet(), setOf("example.app")))
        assertFalse(canRecoverHiddenNodes("", true, emptySet(), emptySet(), emptySet()))
        assertTrue(canRecoverHiddenNodes("example.app", true, emptySet(), emptySet(), emptySet()))
    }
    @Test fun builtInFolderContainsBothAnchorsAndTwelveMembers() {
        assertEquals(12, detectorFolderSeedPackages.size)
        assertTrue(detectorFolderSeedPackages.containsAll(setOf("com.zhenxi.hunter", "com.chunqiunativecheck")))
    }
}
