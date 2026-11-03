package li.songe.gkd.a11y

/** Identity and generation, not mode or wall time, decide which engine may execute. */
internal class ServiceSession<T : Any> {
    private var owner: T? = null
    private var generation = 0L

    @Synchronized fun activate(value: T): Long {
        owner = value
        return ++generation
    }
    @Synchronized fun current(): T? = owner
    @Synchronized fun isCurrent(value: T, ticket: Long): Boolean =
        owner === value && ticket == generation
    @Synchronized fun release(value: T) {
        if (owner === value) owner = null
    }
}

/** A sender's timestamp is advisory. Never let a future timestamp poison other senders. */
internal fun validEventTimestamp(eventUptime: Long, nowUptime: Long): Boolean =
    eventUptime >= 0 && nowUptime >= 0 && eventUptime <= nowUptime
