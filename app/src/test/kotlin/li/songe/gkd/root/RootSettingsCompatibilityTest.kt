package li.songe.gkd.root

import kotlinx.serialization.decodeFromString
import li.songe.gkd.store.SettingsStore
import li.songe.gkd.util.json
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RootSettingsCompatibilityTest {
    @Test
    fun oldSettingsWithoutRootFieldStayDisabled() {
        val oldJson = """{"actionToast":"GKD","customNotifTitle":"GKD","updateChannel":0}"""
        assertFalse(json.decodeFromString<SettingsStore>(oldJson).enableApkRoot)
    }

    @Test
    fun rootSettingRoundTripsWithoutChangingSchemaConsumers() {
        val encoded = json.encodeToString(
            SettingsStore.serializer(),
            SettingsStore(
                enableApkRoot = true,
                actionToast = "GKD",
                customNotifTitle = "GKD",
                updateChannel = 0,
            ),
        )
        assertTrue(json.decodeFromString<SettingsStore>(encoded).enableApkRoot)
    }
}
