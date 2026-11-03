package li.songe.gkd.data

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class BundledSnapshotTest {
    @Test fun onlineSnapshotHasOriginalIdentityAndAllApps() {
        val bytes = File("src/main/res/raw/bundled_online_667.json").readBytes()
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        assertEquals("a485bc074091e517c29bfb95f92b068c987f57094b28e2b0b35718c4bb6efdaf", hash)
        val subscription = RawSubscription.parse(bytes.toString(Charsets.UTF_8), json5 = false)
        assertEquals(667L, subscription.id); assertEquals(575, subscription.version)
        assertEquals(973, subscription.apps.size); assertEquals(3, subscription.globalGroups.size)
    }
    @Test fun localNoticeRemainsBundledSeparately() {
        val subscription = RawSubscription.parse(File("src/main/res/raw/keke_notice_rules.json").readText(), json5 = false)
        assertEquals(-2L, subscription.id); assertEquals(1, subscription.globalGroups.size)
    }
}
