package li.songe.gkd.shizuku

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ProxyUiAutomationConnectionSourceTest {
    @Test
    fun `automation lifecycle never changes the user's rotation lock`() {
        val relativePath = "src/main/kotlin/li/songe/gkd/shizuku/ProxyUiAutomationConnection.kt"
        val sourceFile = sequenceOf(
            File(relativePath),
            File("app/$relativePath"),
        ).firstOrNull(File::isFile)

        assertTrue("ProxyUiAutomationConnection source file is missing", sourceFile != null)
        val source = sourceFile!!.readText()
        listOf(
            "storeRotationStateLocked",
            "restoreRotationStateLocked",
            "isRotationFrozen",
            "getDefaultDisplayRotation",
            "freezeRotation(",
            "thawRotation(",
        ).forEach { forbiddenCall ->
            assertFalse(
                "UiAutomation lifecycle must not invoke $forbiddenCall",
                source.contains(forbiddenCall),
            )
        }
    }
}
