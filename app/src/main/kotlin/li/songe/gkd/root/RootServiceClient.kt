package li.songe.gkd.root

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import com.topjohnwu.superuser.ipc.RootService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import li.songe.gkd.runtime.foreground.ForegroundTask
import li.songe.gkd.util.LogUtils

sealed interface RootServiceState {
    data object Disconnected : RootServiceState
    data object Connecting : RootServiceState
    data class Connected(val identity: RootServiceIdentity) : RootServiceState
    data class Failed(
        val reason: String,
        val retryable: Boolean = false,
    ) : RootServiceState
}

object RootServiceClient {
    private const val CONNECT_TIMEOUT_MS = 8_000L

    private val mutableState = MutableStateFlow<RootServiceState>(RootServiceState.Disconnected)
    val state = mutableState.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())
    private var connectionGeneration = 0L
    private var bound = false
    private var activeConnection: ServiceConnection? = null
    private var activeConnectTimeout: Runnable? = null
    private var activeDeathRecipient: IBinder.DeathRecipient? = null
    private var remoteBinder: IBinder? = null
    private var remoteService: IRootService? = null

    private fun newConnection(generation: Long) = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            if (!isActive(generation, this)) return
            clearConnectTimeout()
            try {
                val service = IRootService.Stub.asInterface(binder)
                val identity = RootServiceIdentity(
                    protocolVersion = service.protocolVersion,
                    remotePid = service.remotePid,
                    remoteUid = service.remoteUid,
                    servicePackageName = service.servicePackageName,
                )
                val failure = validateRootServiceIdentity(identity, name.packageName)
                if (failure != null) {
                    releaseBinding()
                    mutableState.value = RootServiceState.Failed("identity rejected: $failure")
                    return
                }
                val deathRecipient = IBinder.DeathRecipient {
                    if (connectionGeneration != generation || remoteBinder !== binder) return@DeathRecipient
                    activeDeathRecipient = null
                    remoteBinder = null
                    remoteService = null
                    bound = false
                    activeConnection = null
                    mutableState.value = RootServiceState.Failed("binder died", retryable = true)
                }
                binder.linkToDeath(deathRecipient, 0)
                activeDeathRecipient = deathRecipient
                remoteBinder = binder
                remoteService = service
                bound = true
                mutableState.value = RootServiceState.Connected(identity)
                LogUtils.d("RootService connected", identity)
            } catch (e: Throwable) {
                releaseBinding()
                mutableState.value = RootServiceState.Failed(e.message ?: e::class.java.simpleName)
                LogUtils.d("RootService handshake failed", e)
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            if (!isActive(generation, this)) return
            clearConnectTimeout()
            clearBinder()
            bound = false
            activeConnection = null
            // Binder death can deliver both DeathRecipient and this callback.
            // Preserve the specific death reason instead of degrading it to a
            // generic disconnected state. Explicit disconnect() sets the final
            // Disconnected state itself.
            if (mutableState.value !is RootServiceState.Failed &&
                mutableState.value != RootServiceState.Disconnected
            ) {
                mutableState.value = RootServiceState.Failed("service disconnected", retryable = true)
            }
        }

        override fun onBindingDied(name: ComponentName) {
            if (!isActive(generation, this)) return
            clearConnectTimeout()
            clearBinder()
            bound = false
            activeConnection = null
            mutableState.value = RootServiceState.Failed("binding died", retryable = true)
        }

        override fun onNullBinding(name: ComponentName) {
            if (!isActive(generation, this)) return
            clearConnectTimeout()
            releaseBinding()
            mutableState.value = RootServiceState.Failed("null binder")
        }
    }

    fun connect(context: Context) {
        if (bound || mutableState.value == RootServiceState.Connecting) return
        mutableState.value = RootServiceState.Connecting
        val generation = ++connectionGeneration
        val connection = newConnection(generation)
        activeConnection = connection
        bound = true
        try {
            RootService.bind(Intent(context, GkdRootService::class.java), connection)
            if (mutableState.value == RootServiceState.Connecting) {
                val timeout = Runnable {
                    if (connectionGeneration == generation &&
                        activeConnection === connection &&
                        mutableState.value == RootServiceState.Connecting
                    ) {
                        releaseBinding()
                        mutableState.value = RootServiceState.Failed("connection timeout")
                    }
                }
                activeConnectTimeout = timeout
                mainHandler.postDelayed(timeout, CONNECT_TIMEOUT_MS)
            }
        } catch (e: Throwable) {
            clearConnectTimeout()
            bound = false
            activeConnection = null
            mutableState.value = RootServiceState.Failed(
                e.message ?: e::class.java.simpleName,
                retryable = true,
            )
            LogUtils.d("RootService bind failed", e)
        }
    }

    fun disconnect() {
        connectionGeneration += 1
        clearConnectTimeout()
        releaseBinding()
        clearBinder()
        mutableState.value = RootServiceState.Disconnected
    }

    fun performInput(request: RootInputRequest): Int {
        val service = remoteService ?: return ROOT_INPUT_RESULT_UNAVAILABLE
        return try {
            service.performInput(request)
        } catch (e: Throwable) {
            releaseBinding()
            clearBinder()
            mutableState.value = RootServiceState.Failed(
                e.message ?: e::class.java.simpleName,
                retryable = true,
            )
            // The remote process may have accepted the transaction before the Binder failed.
            // Treat this as ambiguous/failed so callers never repeat a possible side effect.
            ROOT_INPUT_RESULT_FAILED
        }
    }

    fun getForegroundTask(displayId: Int): ForegroundTask? {
        if (displayId < 0) return null
        val service = remoteService ?: return null
        return try {
            service.getForegroundTask(displayId)?.toForegroundTask()
        } catch (e: Throwable) {
            releaseBinding()
            clearBinder()
            mutableState.value = RootServiceState.Failed(
                e.message ?: e::class.java.simpleName,
                retryable = true,
            )
            LogUtils.d("RootService foreground task failed", e)
            null
        }
    }

    private fun releaseBinding() {
        val connection = activeConnection
        activeConnection = null
        if (bound && connection != null) {
            runCatching { RootService.unbind(connection) }
        }
        bound = false
    }

    private fun clearBinder() {
        val deathRecipient = activeDeathRecipient
        activeDeathRecipient = null
        remoteBinder?.let { binder ->
            if (deathRecipient != null) {
                runCatching { binder.unlinkToDeath(deathRecipient, 0) }
            }
        }
        remoteBinder = null
        remoteService = null
    }

    private fun clearConnectTimeout() {
        activeConnectTimeout?.let(mainHandler::removeCallbacks)
        activeConnectTimeout = null
    }

    private fun isActive(generation: Long, connection: ServiceConnection) =
        connectionGeneration == generation && activeConnection === connection
}
