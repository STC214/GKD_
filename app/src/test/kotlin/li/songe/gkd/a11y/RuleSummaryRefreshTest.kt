package li.songe.gkd.a11y

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertSame
import org.junit.Test

class RuleSummaryRefreshTest {
    @Test
    fun `late collector still receives already loaded summary`() = runBlocking {
        val initial = Any()
        val loaded = Any()
        val summaries = MutableStateFlow(initial)
        summaries.value = loaded
        val received = CompletableDeferred<Any>()

        val collector = launch {
            summaries.collectRuleSummaryUpdates { summary ->
                received.complete(summary)
            }
        }

        assertSame(loaded, withTimeout(1_000) { received.await() })
        collector.cancelAndJoin()
    }
}
