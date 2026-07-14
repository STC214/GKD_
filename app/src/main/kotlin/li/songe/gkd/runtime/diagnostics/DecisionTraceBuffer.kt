package li.songe.gkd.runtime.diagnostics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import li.songe.gkd.store.storeFlow
import java.util.concurrent.atomic.AtomicLong

class DecisionTraceBuffer(
    private val capacity: Int = 2048,
    private val enabled: () -> Boolean = { true },
) {
    init {
        require(capacity > 0)
    }

    private val nextId = AtomicLong(0)
    private val records = ArrayDeque<DecisionTrace>(capacity)
    private val _latestSkippedFlow = MutableStateFlow<DecisionTrace?>(null)
    val latestSkippedFlow = _latestSkippedFlow.asStateFlow()

    fun newCorrelationId(): Long? = if (enabled()) nextId.incrementAndGet() else null

    @Synchronized
    fun append(
        correlationId: Long?,
        stage: DecisionStage,
        outcome: DecisionOutcome,
        reason: DecisionReason,
        appId: String? = null,
        activityId: String? = null,
        ruleId: String? = null,
        detail: String? = null,
        timestamp: Long = System.currentTimeMillis(),
    ): DecisionTrace? {
        if (correlationId == null || !enabled()) return null
        val trace = DecisionTrace(
            correlationId = correlationId,
            timestamp = timestamp,
            stage = stage,
            outcome = outcome,
            reason = reason,
            appId = appId,
            activityId = activityId,
            ruleId = ruleId,
            detail = detail,
        )
        if (records.size == capacity) records.removeFirst()
        records.addLast(trace)
        if (outcome == DecisionOutcome.Skipped || outcome == DecisionOutcome.Failed) {
            _latestSkippedFlow.value = trace
        }
        return trace
    }

    @Synchronized
    fun snapshot(): List<DecisionTrace> = records.toList()

    @Synchronized
    fun clear() {
        records.clear()
        _latestSkippedFlow.value = null
    }

    fun exportText(): String {
        val currentRecords = snapshot()
        return buildString {
            appendLine("GKD rule decision diagnostics")
            appendLine("capacity=$capacity records=${currentRecords.size}")
            currentRecords.forEach { appendLine(it.exportLine()) }
        }.trimEnd()
    }
}

val decisionTraceBuffer = DecisionTraceBuffer(
    enabled = { storeFlow.value.enableRuleDiagnostics },
)
