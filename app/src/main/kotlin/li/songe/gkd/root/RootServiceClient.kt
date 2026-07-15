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
import li.songe.gkd.util.LogUtils

sealed interface RootServiceState {
    data object Disconnected : RootServiceState
    data object Connecting : RootServiceState
    data class Connected(val identity: RootServiceIdentity) : RootServiceState
    data class Failed(val reason: String) : RootServiceState
}

object RootServiceClient {
    private const val CONNECT_TIMEOUT_MS = 8_000L

    private val mutableState = MutableStateFlow<RootServiceState>(RootServiceState.Disconnected)
    val state = mutableState.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())
    private var bound = false
    private var remoteBinder: IBinder? = null
    private var remoteService: IRootService? = null
    private val connectTimeout = Runnable {
        if (mutableState.value == RootServiceState.Connecting) {
            releaseBinding()
            mutableState.value = RootServiceState.Failed("connection timeout")
        }
    }

    private val deathRecipient = IBinder.DeathRecipient {
        remoteBinder = null
        remoteService = null
        bound = false
        mutableState.value = RootServiceState.Failed("binder died")
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            mainHandler.removeCallbacks(connectTimeout)
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
                binder.linkToDeath(deathRecipient, 0)
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
            mainHandler.removeCallbacks(connectTimeout)
            clearBinder()
            bound = false
            // Binder death can deliver both DeathRecipient and this callback.
            // Preserve the specific death reason instead of degrading it to a
            // generic disconnected state. Explicit disconnect() sets the final
            // Disconnected state itself.
            if (mutableState.value !is RootServiceState.Failed &&
                mutableState.value != RootServiceState.Disconnected
            ) {
                mutableState.value = RootServiceState.Failed("service disconnected")
            }
        }

        override fun onBindingDied(name: ComponentName) {
            mainHandler.removeCallbacks(connectTimeout)
            clearBinder()
            bound = false
            mutableState.value = RootServiceState.Failed("binding died")
        }

        override fun onNullBinding(name: ComponentName) {
            mainHandler.removeCallbacks(connectTimeout)
            releaseBinding()
            mutableState.value = RootServiceState.Failed("null binder")
        }
    }

    fun connect(context: Context) {
        if (bound || mutableState.value == RootServiceState.Connecting) return
        mutableState.value = RootServiceState.Connecting
        bound = true
        try {
            RootService.bind(Intent(context, GkdRootService::class.java), connection)
            if (mutableState.value == RootServiceState.Connecting) {
                mainHandler.postDelayed(connectTimeout, CONNECT_TIMEOUT_MS)
            }
        } catch (e: Throwable) {
            mainHandler.removeCallbacks(connectTimeout)
            bound = false
            mutableState.value = RootServiceState.Failed(e.message ?: e::class.java.simpleName)
            LogUtils.d("RootService bind failed", e)
        }
    }

    fun disconnect() {
        mainHandler.removeCallbacks(connectTimeout)
        releaseBinding()
        clearBinder()
        mutableState.value = RootServiceState.Disconnected
    }

    fun performInput(request: RootInputRequest): Int {
        val service = remoteService ?: return ROOT_INPUT_RESULT_UNAVAILABLE
        return try {
            service.performInput(request)
        } catch (e: Throwable) {
            clearBinder()
            bound = false
            mutableState.value = RootServiceState.Failed(e.message ?: e::class.java.simpleName)
            ROOT_INPUT_RESULT_UNAVAILABLE
        }
    }

    private fun releaseBinding() {
        if (bound) {
            runCatching { RootService.unbind(connection) }
        }
        bound = false
    }

    private fun clearBinder() {
        remoteBinder?.let { binder ->
            runCatching { binder.unlinkToDeath(deathRecipient, 0) }
        }
        remoteBinder = null
        remoteService = null
    }
}
