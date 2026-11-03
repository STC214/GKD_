package li.songe.gkd.store

import org.junit.Assert.*
import org.junit.Test

class ModuleBootstrapTest {
    @Test fun freshModuleOwnerIsEligible() {
        assertTrue(shouldBootstrapModule(true, "1", false))
    }
    @Test fun cloneNeverStartsMatchingEngine() {
        assertFalse(shouldBootstrapModule(false, "1", false))
    }
    @Test fun ordinaryApkHasNoImplicitModuleBootstrap() {
        assertFalse(shouldBootstrapModule(true, null, false))
        assertFalse(shouldBootstrapModule(true, "broken", false))
    }
    @Test fun existingOrCorruptedStoreDoesNotReenable() {
        assertFalse(shouldBootstrapModule(true, "1", true))
    }
}
