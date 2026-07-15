package li.songe.gkd.a11y

/**
 * A bounded single-runner wake state.
 *
 * One request owns the active query. Requests arriving while it runs are merged into one pending
 * request, so the latest UI state is queried without building an unbounded queue.
 */
internal data class QueryWakeRequest<E : Any, R : Any>(
    val event: E? = null,
    val forced: Boolean = false,
    val delayRule: R? = null,
    val correlationId: Long? = null,
)

internal class QueryWakeState<E : Any, R : Any> {
    private var running = false
    private var pending: QueryWakeRequest<E, R>? = null

    /** Returns true only when the caller must start a query immediately. */
    @Synchronized
    fun request(request: QueryWakeRequest<E, R>): Boolean {
        if (!running) {
            running = true
            return true
        }
        pending = merge(pending, request)
        return false
    }

    /**
     * Completes the active query. A returned request already owns the next active slot and must be
     * launched directly instead of being passed through [request] again.
     */
    @Synchronized
    fun complete(): QueryWakeRequest<E, R>? {
        check(running) { "query wake state completed while idle" }
        val next = pending
        pending = null
        if (next == null) {
            running = false
        }
        return next
    }

    @Synchronized
    fun isRunning(): Boolean = running

    @Synchronized
    fun hasPending(): Boolean = pending != null

    private fun merge(
        current: QueryWakeRequest<E, R>?,
        incoming: QueryWakeRequest<E, R>,
    ): QueryWakeRequest<E, R> {
        if (current == null) return incoming

        // A real event carries the newest UI context and queries all eligible rules.
        if (incoming.event != null) return incoming
        if (current.event != null) return current

        val incomingIsNormal = !incoming.forced && incoming.delayRule == null
        val currentIsNormal = !current.forced && current.delayRule == null
        // A normal wake also queries all eligible rules, so it subsumes forced and delayed wakes.
        if (incomingIsNormal) return incoming
        if (currentIsNormal) return current

        // Keep a matured delayed rule ahead of a forced scan. For repeated requests, keep latest.
        if (incoming.delayRule != null) return incoming
        if (current.delayRule != null) return current
        return incoming
    }
}

internal data class QueryEventBatch<E : Any>(
    val hadEvents: Boolean,
    val events: List<E>?,
)

/** Retains at most two equivalent events, or one root-query marker after mixed events arrive. */
internal class QueryEventBuffer<E : Any>(
    private val sameAs: (E, E) -> Boolean,
) {
    private var previous: E? = null
    private var latest: E? = null
    private var mixed = false

    @Synchronized
    fun addAll(events: Collection<E>) {
        events.forEach { event ->
            val oldLatest = latest
            if (oldLatest != null) {
                if (!sameAs(oldLatest, event)) mixed = true
                previous = oldLatest
            }
            latest = event
        }
    }

    @Synchronized
    fun drain(): QueryEventBatch<E> {
        val last = latest
        val batch = when {
            last == null -> QueryEventBatch(hadEvents = false, events = null)
            mixed -> QueryEventBatch(hadEvents = true, events = null)
            previous != null -> QueryEventBatch(hadEvents = true, events = listOf(previous!!, last))
            else -> QueryEventBatch(hadEvents = true, events = listOf(last))
        }
        previous = null
        latest = null
        mixed = false
        return batch
    }

    @Synchronized
    fun retainedCount(): Int = when {
        latest == null -> 0
        previous == null -> 1
        else -> 2
    }
}
