package li.songe.gkd.data

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import org.junit.Assert.*
import org.junit.Test

class BundledMigrationRecoveryTest {
    private fun obj(text: String) = Json.parseToJsonElement(text).jsonObject
    private val empty = obj("""{"id":-2,"version":0}""")
    private val bundle = obj("""{"id":-2,"globalGroups":[{"key":1}]}""")
    @Test fun `retry recovers interruption after metadata or file commit`() = runBlocking {
        for (failAfter in 0..2) {
            var file = empty; var metadata = false; var enabled = false; var marker: String? = null
            suspend fun attempt(fail: Int?) {
                val plan = BundledLocalRules.merge(file, bundle)
                applyBundledRulePlan(plan, ensureMetadata = {
                    metadata = true
                    if (plan.enableEmptyLocal) enabled = true
                    if (fail == 0) error("simulated crash after metadata")
                }, writeSubscription = {
                    check(metadata); file = plan.subscription
                    if (fail == 1) error("simulated crash after file")
                }, writeMarker = {
                    if (fail == 2) error("simulated marker failure")
                    marker = "1"
                })
            }
            runCatching { attempt(failAfter) }; attempt(null)
            assertTrue(metadata); assertTrue(enabled); assertEquals("1", marker)
            assertEquals(1, file["globalGroups"]!!.jsonArray.size)
        }
    }
    @Test fun `metadata failure never publishes a new file or completion marker`() = runBlocking {
        var fileWritten = false; var markerWritten = false
        runCatching { applyBundledRulePlan(BundledLocalRules.merge(empty, bundle),
            ensureMetadata = { error("DB unavailable") },
            writeSubscription = { fileWritten = true }, writeMarker = { markerWritten = true }) }
        assertFalse(fileWritten); assertFalse(markerWritten)
    }
    @Test fun `torn migration marker is retried`() {
        assertFalse(bundledMigrationComplete(null)); assertFalse(bundledMigrationComplete(""))
        assertFalse(bundledMigrationComplete("garbage")); assertTrue(bundledMigrationComplete("1"))
    }
    @Test fun `maximum version never wraps negative`() {
        val plan = BundledLocalRules.merge(obj("""{"id":-2,"version":2147483647}"""), bundle)
        assertEquals("2147483647", plan.subscription["version"].toString()); assertTrue(plan.changed)
    }
}
