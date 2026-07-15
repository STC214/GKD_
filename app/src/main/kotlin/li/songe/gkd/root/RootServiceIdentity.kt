package li.songe.gkd.root

const val ROOT_SERVICE_PROTOCOL_VERSION = 2

data class RootServiceIdentity(
    val protocolVersion: Int,
    val remotePid: Int,
    val remoteUid: Int,
    val servicePackageName: String,
)

enum class RootIdentityFailure {
    PROTOCOL_MISMATCH,
    INVALID_PID,
    NOT_ROOT,
    PACKAGE_MISMATCH,
}

fun validateRootServiceIdentity(
    identity: RootServiceIdentity,
    expectedPackage: String,
): RootIdentityFailure? = when {
    identity.protocolVersion != ROOT_SERVICE_PROTOCOL_VERSION -> RootIdentityFailure.PROTOCOL_MISMATCH
    identity.remotePid <= 0 -> RootIdentityFailure.INVALID_PID
    identity.remoteUid != 0 -> RootIdentityFailure.NOT_ROOT
    identity.servicePackageName != expectedPackage -> RootIdentityFailure.PACKAGE_MISMATCH
    else -> null
}
