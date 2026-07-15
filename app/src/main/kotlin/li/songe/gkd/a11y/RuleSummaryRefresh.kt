package li.songe.gkd.a11y

import kotlinx.coroutines.flow.Flow

/**
 * Collects the current value as well as later updates.
 *
 * This must not drop the first emission: StateFlow may already contain the fully loaded rule
 * summary before the collector coroutine gets a chance to subscribe.
 */
internal suspend fun <T> Flow<T>.collectRuleSummaryUpdates(
    onSummary: suspend (T) -> Unit,
) = collect(onSummary)
