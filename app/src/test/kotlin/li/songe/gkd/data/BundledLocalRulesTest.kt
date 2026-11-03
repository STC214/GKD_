package li.songe.gkd.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.*
import org.junit.Test

class BundledLocalRulesTest {
    private fun obj(s: String) = Json.parseToJsonElement(s).jsonObject
    private val bundle = obj("""{"id":-2,"globalGroups":[{"key":2026110301,"name":"notice","rules":[]}]}""")

    @Test fun `empty local receives bundle and is eligible for first time enable`() {
        val current = obj("""{"id":-2,"version":0,"apps":[],"custom":"preserve"}""")
        val plan = BundledLocalRules.merge(current, bundle)
        assertTrue(plan.changed); assertTrue(plan.enableEmptyLocal)
        assertEquals(current["custom"], plan.subscription["custom"])
        assertEquals("1", plan.subscription["version"].toString())
    }
    @Test fun `existing same key is never overwritten or reenabled`() {
        val current = obj("""{"id":-2,"globalGroups":[{"key":2026110301,"enable":false,"name":"user edit"}]}""")
        val plan = BundledLocalRules.merge(current, bundle)
        assertFalse(plan.changed); assertFalse(plan.enableEmptyLocal); assertSame(current, plan.subscription)
    }
    @Test fun `existing rules and disabled subscription choice remain intact`() {
        val current = obj("""{"id":-2,"version":5,"apps":[{"id":"test.app","groups":[{"key":1}]}],"globalGroups":[{"key":3}]}""")
        val plan = BundledLocalRules.merge(current, bundle)
        assertTrue(plan.changed); assertFalse(plan.enableEmptyLocal)
        assertEquals(current["apps"], plan.subscription["apps"])
        assertEquals("6", plan.subscription["version"].toString())
    }
    @Test fun `merging is idempotent`() {
        val first = BundledLocalRules.merge(obj("""{"id":-2}"""), bundle)
        assertFalse(BundledLocalRules.merge(first.subscription, bundle).changed)
    }
    @Test(expected = IllegalArgumentException::class)
    fun `rejects nonlocal subscription`() { BundledLocalRules.merge(obj("""{"id":667}"""), bundle) }
}
