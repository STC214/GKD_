package li.songe.gkd.shizuku

import java.util.concurrent.atomic.AtomicReference

internal class ConnectionCallbackGate<T>(
    private val disposeLateValue: (T) -> Unit,
) {
    private val callback = AtomicReference<((T?) -> Unit)?>(null)

    fun arm(value: (T?) -> Unit) {
        check(callback.compareAndSet(null, value)) { "connection callback is already armed" }
    }

    fun complete(value: T): Boolean {
        val current = callback.getAndSet(null)
        if (current == null) {
            disposeLateValue(value)
            return false
        }
        current(value)
        return true
    }

    fun fail(beforeDelivery: () -> Unit = {}): Boolean {
        val current = callback.getAndSet(null) ?: return false
        beforeDelivery()
        current(null)
        return true
    }

    fun cancel() {
        callback.set(null)
    }
}
