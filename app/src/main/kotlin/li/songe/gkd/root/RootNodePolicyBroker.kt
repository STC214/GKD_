package li.songe.gkd.root

import android.content.pm.PackageManager
import android.net.LocalServerSocket
import android.os.SystemClock
import android.util.Log
import java.io.DataOutputStream

/** Read-only, root-authenticated abstract socket. Never grants input or accepts client commands. */
internal class RootNodePolicyBroker(private val packages: PackageManager) : AutoCloseable {
    @Volatile private var lease = CrossUserNodeLease()
    @Volatile private var server: LocalServerSocket? = null
    @Volatile private var closed = false

    fun publish(enabled: Boolean, excluded: Set<String>, allowed: Set<String>) {
        if (lease.enabled != enabled) Log.i("GKD-NodeBroker", "Owner policy enabled=$enabled")
        lease = CrossUserNodeLease(enabled, SystemClock.elapsedRealtime() + 15_000, excluded, allowed)
    }

    fun start() {
        try {
            val listener = LocalServerSocket("li.songe.gkd.node-policy.v1.owner0")
            server = listener
            Thread({
                while (!closed) {
                    try {
                        listener.accept().use { socket ->
                            socket.soTimeout = 250
                            val uid = socket.peerCredentials.uid
                            val peerPackages = packages.getPackagesForUid(uid)?.toSet().orEmpty()
                            val snapshot = lease
                            val now = SystemClock.elapsedRealtime()
                            val allowed = snapshot.permits(uid, peerPackages, now)
                            DataOutputStream(socket.outputStream).use { output ->
                                output.writeBoolean(allowed)
                                output.writeLong(if (allowed) minOf(snapshot.expiresElapsed, now + 3000) else 0)
                            }
                        }
                    } catch (error: Exception) {
                        if (!closed) Log.w("GKD-NodeBroker", "Client rejected or disconnected", error)
                    }
                }
            }, "GkdNodePolicyBroker").apply { isDaemon = true; start() }
        } catch (error: Exception) {
            Log.w("GKD-NodeBroker", "Broker unavailable; clone policy stays disabled", error)
        }
    }

    override fun close() {
        closed = true
        lease = CrossUserNodeLease()
        runCatching { server?.close() }
        server = null
    }
}
