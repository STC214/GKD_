package li.songe.gkd.util

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test

class BrandIconSizingTest {
    private val res = File("src/main/res").takeIf { it.isDirectory }
        ?: File("app/src/main/res")
    private fun xml(path: String) = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
    }.newDocumentBuilder().parse(File(res, path)).documentElement
    private fun inset(path: String): Double = xml(path)
        .getAttributeNS("http://schemas.android.com/apk/res/android", "inset")
        .removeSuffix("%").toDouble() / 100

    @Test fun adaptiveColorFillsTheMaskedViewport() {
        val fraction = inset("drawable/ic_brand_adaptive_foreground.xml")
        assertEquals(0.1667, fraction, 0.00001)
        assertEquals(1.0, (1 - 2 * fraction) * 108 / 72, 0.001)
    }

    @Test fun adaptiveMonochromeMatchesColorScale() {
        assertEquals(inset("drawable/ic_brand_adaptive_foreground.xml"),
            inset("drawable/ic_brand_adaptive_monochrome.xml"), 0.0)
    }

    @Test fun splashSquareStaysInsideItsDifferentSafeCircle() {
        val fraction = inset("drawable/ic_brand_splash.xml")
        assertEquals(0.27, fraction, 0.00001)
        assertTrue((1 - 2 * fraction) * kotlin.math.sqrt(2.0) < 192.0 / 288)
        assertTrue(fraction > inset("drawable/ic_brand_adaptive_foreground.xml"))
    }

    @Test fun bothAdaptiveApiVariantsUseTheColorLayer() {
        for (api in listOf(26, 33)) {
            val icon = xml("drawable-anydpi-v$api/ic_launcher.xml")
            assertEquals("adaptive-icon", icon.tagName)
            assertEquals("@drawable/ic_brand_adaptive_foreground", icon
                .getElementsByTagName("foreground").item(0).attributes
                .getNamedItem("android:drawable").nodeValue)
        }
    }

    @Test fun themedIconUsesThePaddedAdaptiveMask() {
        assertEquals("@drawable/ic_brand_adaptive_monochrome", xml(
            "drawable-anydpi-v33/ic_launcher.xml").getElementsByTagName("monochrome")
            .item(0).attributes.getNamedItem("android:drawable").nodeValue)
    }

    @Test fun legacyAndNotificationAssetsDoNotReceiveAdaptivePadding() {
        assertEquals("bitmap", xml("drawable/ic_launcher.xml").tagName)
        assertEquals("vector", xml("drawable/ic_brand_monochrome.xml").tagName)
    }
}
