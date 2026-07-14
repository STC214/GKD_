package li.songe.gkd.shizuku

enum class RootBridgePhase {
    Disconnected,
    Checking,
    Partial,
    Root,
    NonRoot,
    Failed,
}

enum class UserServiceFailure {
    BindException,
    InvalidBinder,
    Timeout,
}

data class RootBridgeDiagnostics(
    val phase: RootBridgePhase = RootBridgePhase.Disconnected,
    val checkedAt: Long? = null,
    val attempt: Int = 0,
    val maxAttempts: Int = 0,
    val userServiceConnected: Boolean = false,
    val shellCommandAvailable: Boolean = false,
    val remoteUid: Int? = null,
    val binderAvailable: Int = 0,
    val binderTotal: Int = 8,
    val uiAutomationConnected: Boolean = false,
    val topActivity: String? = null,
    val failure: UserServiceFailure? = null,
    val error: String? = null,
) {
    val statusText: String
        get() = when (phase) {
            RootBridgePhase.Disconnected -> "未连接"
            RootBridgePhase.Checking -> "检测中"
            RootBridgePhase.Partial -> "部分可用"
            RootBridgePhase.Root -> "Root 已连接"
            RootBridgePhase.NonRoot -> "已连接但不是 Root"
            RootBridgePhase.Failed -> "Root 桥连接失败"
        }
}

internal fun parseRemoteUid(identity: String): Int? =
    Regex("(?:^|\\s)uid=(\\d+)(?:\\([^)]*\\))?")
        .find(identity)
        ?.groupValues
        ?.getOrNull(1)
        ?.toIntOrNull()

internal fun rootBridgePhase(
    contextAvailable: Boolean,
    userServiceConnected: Boolean,
    shellCommandAvailable: Boolean,
    remoteUid: Int?,
    exhausted: Boolean,
): RootBridgePhase = when {
    !contextAvailable -> RootBridgePhase.Disconnected
    userServiceConnected && shellCommandAvailable && remoteUid == 0 -> RootBridgePhase.Root
    userServiceConnected && shellCommandAvailable && remoteUid != null -> RootBridgePhase.NonRoot
    exhausted -> RootBridgePhase.Failed
    else -> RootBridgePhase.Partial
}
