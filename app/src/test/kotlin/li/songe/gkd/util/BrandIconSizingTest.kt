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

    @Test fun splashSafeCircleAllowsIntegerInsetRounding() {
        val fraction = inset("drawable/ic_brand_splash.xml")
        for (pixels in listOf(192, 216, 240, 288, 384, 432, 576, 768, 864)) {
            val roundedInset = (pixels * fraction).toInt()
            assertTrue("Splash square clipped at $pixels pixels",
                (pixels - 2 * roundedInset) * kotlin.math.sqrt(2.0) <= pixels * 2.0 / 3)
        }
    }

    @Test fun bothSplashThemesReferenceTheIndependentSplashLayer() {
        val styles = xml("values/themes.xml").getElementsByTagName("style")
        for (name in listOf("SplashScreenLightTheme", "SplashScreenNightTheme")) {
            val style = (0 until styles.length).map { styles.item(it) }
                .single { it.attributes.getNamedItem("name").nodeValue == name }
            val items = style.childNodes
            val icon = (0 until items.length).map { items.item(it) }.single {
                it.nodeName == "item" &&
                    it.attributes.getNamedItem("name").nodeValue == "windowSplashScreenAnimatedIcon"
            }
            assertEquals("@drawable/ic_brand_splash", icon.textContent.trim())
        }
    }

    @Test fun launcherAndTileManifestKeepSeparateIconContracts() {
        val manifest = res.parentFile.resolve("AndroidManifest.xml")
        val document = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder().parse(manifest)
        val ns = "http://schemas.android.com/apk/res/android"
        val application = document.getElementsByTagName("application").item(0) as org.w3c.dom.Element
        assertEquals("@drawable/ic_launcher", application.getAttributeNS(ns, "icon"))
        assertEquals("@drawable/ic_launcher", application.getAttributeNS(ns, "roundIcon"))
        val services = document.getElementsByTagName("service")
        val tile = (0 until services.length).map { services.item(it) as org.w3c.dom.Element }
            .single { it.getAttributeNS(ns, "name") == ".service.GkdTileService" }
        assertEquals("@drawable/ic_brand_monochrome", tile.getAttributeNS(ns, "icon"))
    }

    @Test fun notificationDefaultsToTheUnpaddedMonochromeVector() {
        val source = res.parentFile.resolve("kotlin/li/songe/gkd/notif/Notif.kt").readText()
        assertTrue(source.contains("val smallIcon: Int = R.drawable.ic_brand_monochrome"))
        assertFalse(xml("drawable/ic_brand_monochrome.xml").hasAttributeNS(
            "http://schemas.android.com/apk/res/android", "inset"))
    }
}
