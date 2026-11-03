package li.songe.gkd.data

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull

/** Add missing groups only. Existing keys, edits, app rules and unknown fields remain authoritative. */
internal object BundledLocalRules {
    data class Plan(val subscription: JsonObject, val changed: Boolean, val enableEmptyLocal: Boolean)

    fun merge(current: JsonObject, bundle: JsonObject): Plan {
        require(current["id"]?.jsonPrimitive?.content == "-2")
        require(bundle["id"]?.jsonPrimitive?.content == "-2")
        val groups = current["globalGroups"]?.jsonArray.orEmpty()
        val existingKeys = groups.map { it.jsonObject["key"]?.jsonPrimitive?.content }.toSet()
        val missing = bundle["globalGroups"]?.jsonArray.orEmpty()
            .distinctBy { it.jsonObject["key"]?.jsonPrimitive?.content }
            .filter { it.jsonObject["key"]?.jsonPrimitive?.content !in existingKeys }
        if (missing.isEmpty()) return Plan(current, changed = false, enableEmptyLocal = false)
        val emptyLocal = groups.isEmpty() && current["apps"]?.jsonArray.orEmpty().all {
            it.jsonObject["groups"]?.jsonArray.orEmpty().isEmpty()
        }
        val next = current.toMutableMap().apply {
            put("globalGroups", JsonArray(groups + missing))
            val version = current["version"]?.jsonPrimitive?.intOrNull ?: 0
            put("version", JsonPrimitive((version.toLong() + 1).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()))
        }
        return Plan(JsonObject(next), changed = true, enableEmptyLocal = emptyLocal)
    }
}

/** Metadata is durable before publishing a new file. Retrying after any interruption is idempotent. */
internal suspend fun applyBundledRulePlan(
    plan: BundledLocalRules.Plan,
    ensureMetadata: suspend () -> Unit,
    writeSubscription: () -> Unit,
    writeMarker: () -> Unit,
) {
    ensureMetadata()
    if (plan.changed) writeSubscription()
    writeMarker()
}

internal fun bundledMigrationComplete(marker: String?): Boolean = marker == "1"
