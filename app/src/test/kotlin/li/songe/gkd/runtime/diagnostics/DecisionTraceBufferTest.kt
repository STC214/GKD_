package li.songe.gkd.runtime.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DecisionTraceBufferTest {
    @Test
    fun `ring buffer evicts oldest record and keeps correlation id`() {
        val buffer = DecisionTraceBuffer(capacity = 2)
        val id = buffer.newCorrelationId()
        repeat(3) { index ->
            buffer.append(
                correlationId = id,
                stage = DecisionStage.Rule,
                outcome = DecisionOutcome.Skipped,
                reason = DecisionReason.SelectorMiss,
                detail = index.toString(),
                timestamp = index.toLong(),
            )
        }

        val records = buffer.snapshot()
        assertEquals(listOf("1", "2"), records.map { it.detail })
        assertTrue(records.all { it.correlationId == id })
        assertEquals("2", buffer.latestSkippedFlow.value?.detail)
    }

    @Test
    fun `disabled buffer does not allocate ids or records`() {
        val buffer = DecisionTraceBuffer(capacity = 2, enabled = { false })
        val id = buffer.newCorrelationId()

        assertNull(id)
        assertNull(
            buffer.append(
                correlationId = 1,
                stage = DecisionStage.Query,
                outcome = DecisionOutcome.Skipped,
                reason = DecisionReason.AutoMatchDisabled,
            )
        )
        assertTrue(buffer.snapshot().isEmpty())
    }

    @Test
    fun `export uses stable enum names`() {
        val buffer = DecisionTraceBuffer(capacity = 2)
        val id = buffer.newCorrelationId()
        buffer.append(
            correlationId = id,
            stage = DecisionStage.Window,
            outcome = DecisionOutcome.Skipped,
            reason = DecisionReason.WindowRootUnavailable,
            appId = "example.app",
            ruleId = "1/app/2/3",
            timestamp = 10,
        )

        val text = buffer.exportText()
        assertTrue(text.contains("reason=WindowRootUnavailable"))
        assertTrue(text.contains("app=example.app"))
        assertTrue(text.contains("rule=1/app/2/3"))
    }

    @Test
    fun `observed records do not replace latest skipped reason`() {
        val buffer = DecisionTraceBuffer(capacity = 4)
        val id = buffer.newCorrelationId()
        buffer.append(
            correlationId = id,
            stage = DecisionStage.Selector,
            outcome = DecisionOutcome.Skipped,
            reason = DecisionReason.SelectorMiss,
        )
        buffer.append(
            correlationId = id,
            stage = DecisionStage.Rule,
            outcome = DecisionOutcome.Observed,
            reason = DecisionReason.NoApplicableRules,
        )

        assertEquals(DecisionReason.SelectorMiss, buffer.latestSkippedFlow.value?.reason)
        buffer.clear()
        assertNull(buffer.latestSkippedFlow.value)
        assertTrue(buffer.snapshot().isEmpty())
    }

    @Test
    fun `every append and clear advances ui revision`() {
        val buffer = DecisionTraceBuffer(capacity = 2)
        val initialRevision = buffer.revisionFlow.value
        val id = buffer.newCorrelationId()

        buffer.append(
            correlationId = id,
            stage = DecisionStage.Event,
            outcome = DecisionOutcome.Observed,
            reason = DecisionReason.EventReceived,
        )
        assertEquals(initialRevision + 1, buffer.revisionFlow.value)

        buffer.clear()
        assertEquals(initialRevision + 2, buffer.revisionFlow.value)
    }
}
