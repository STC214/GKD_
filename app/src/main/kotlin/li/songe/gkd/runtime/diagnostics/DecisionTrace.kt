package li.songe.gkd.runtime.diagnostics

data class DecisionTrace(
    val correlationId: Long,
    val timestamp: Long,
    val stage: DecisionStage,
    val outcome: DecisionOutcome,
    val reason: DecisionReason,
    val appId: String? = null,
    val activityId: String? = null,
    val ruleId: String? = null,
    val detail: String? = null,
) {
    val summary: String
        get() = buildString {
            append(reason.label)
            ruleId?.let { append(" · ").append(it) }
            appId?.let { append(" · ").append(it) }
        }

    fun exportLine(): String = buildString {
        append(timestamp)
        append(" id=").append(correlationId)
        append(" stage=").append(stage.name)
        append(" outcome=").append(outcome.name)
        append(" reason=").append(reason.name)
        appId?.let { append(" app=").append(it) }
        activityId?.let { append(" activity=").append(it) }
        ruleId?.let { append(" rule=").append(it) }
        detail?.takeIf { it.isNotBlank() }?.let {
            append(" detail=").append(it.replace('\n', ' '))
        }
    }
}
