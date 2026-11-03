package li.songe.gkd.data

/** Existing files/metadata and a user's later deletion win over packaged defaults. */
internal data class BundledDefaultPlan(val createMetadata: Boolean, val writeSnapshot: Boolean)

internal fun planBundledDefaultRules(
    completed: Boolean,
    hasMetadata: Boolean,
    hasFile: Boolean,
): BundledDefaultPlan? = if (completed) null else
    BundledDefaultPlan(createMetadata = !hasMetadata, writeSnapshot = !hasFile)

internal suspend fun applyBundledDefaultRules(
    plan: BundledDefaultPlan,
    metadata: suspend () -> Unit,
    snapshot: () -> Unit,
    marker: () -> Unit,
) {
    if (plan.createMetadata) metadata()
    if (plan.writeSnapshot) snapshot()
    marker()
}
