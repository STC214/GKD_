package li.songe.gkd.root

import android.content.pm.PackageManager
import android.net.LocalServerSocket
import android.os.SystemClock
import android.util.Log
import java.io.DataOutputStream
import java.io.File
import android.util.AtomicFile

/** Read-only, root-authenticated abstract socket. Never grants input or accepts client commands. */
internal class RootNodePolicyBroker(private val packages: PackageManager) : AutoCloseable {
    @Volatile private var lease = CrossUserNodeLease()
    @Volatile private var server: LocalServerSocket? = null
    @Volatile private var closed = false

    private var lastProtection: String? = null

    @Synchronized fun publish(enabled: Boolean, scopeReady: Boolean, excluded: Set<String>, allowed: Set<String>, protectedPackages: Set<String>) {
        if (closed) return
        val module = File("/data/adb/modules/gkd_ksu_sukisu")
        val active = enabled && scopeReady && server != null && File(module, "common/global_bridge.dex").isFile &&
            !File(module, "disable").exists() && !File(module, "remove").exists()
        if (lease.enabled != active) Log.i("GKD-NodeBroker", "Owner policy enabled=$active")
        try {
            writeProtection(scopeReady && server != null && File(module, "common/global_bridge.dex").isFile &&
                !File(module, "disable").exists() && !File(module, "remove").exists(), protectedPackages)
            lease = CrossUserNodeLease(active, SystemClock.elapsedRealtime() + 15_000, excluded, allowed, global = true)
        } catch (error: Throwable) {
            lease = CrossUserNodeLease()
            throw error
        }
    }

    private fun writeProtection(ready: Boolean, packages: Set<String>) {
        val text = if (ready) "GKDGLOBAL1\n" + packages.sorted().joinToString("\n", postfix = "\n") else "NOTREADY\n"
        val dir = File("/data/adb/gkd_node_bridge")
        if (text == lastProtection && runCatching { File(dir, "protected.v1").readText() == text }.getOrDefault(false)) return
        if (!dir.isDirectory) check(dir.mkdirs())
        android.system.Os.chmod(dir.path, 448) // 0700; contains only protected package names.
        val file = AtomicFile(File(dir, "protected.v1"))
        val stream = file.startWrite()
        try {
            stream.write(text.toByteArray()); file.finishWrite(stream)
            android.system.Os.chmod(file.baseFile.path, 384) // 0600
            lastProtection = text
        } catch (error: Throwable) { file.failWrite(stream); throw error }
    }

    fun start() {
        if (closed) return
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

    @Synchronized override fun close() {
        closed = true
        lease = CrossUserNodeLease()
        runCatching { server?.close() }
        server = null
        runCatching { writeProtection(false, emptySet()) }
    }
}
