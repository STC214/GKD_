package li.songe.gkd.root

enum class RootCallerFailure {
    UID_MISMATCH,
    PACKAGE_MISSING,
    SIGNATURE_MISMATCH,
}

data class RootCallerVerification(
    val failure: RootCallerFailure? = null,
) {
    val verified: Boolean
        get() = failure == null
}

/** Pure policy used by the root Binder before serving every request. */
fun verifyRootCaller(
    expectedUid: Int,
    callingUid: Int,
    expectedPackage: String,
    packagesForUid: Collection<String>,
    signaturesMatch: (String) -> Boolean,
): RootCallerVerification {
    if (callingUid != expectedUid) {
        return RootCallerVerification(RootCallerFailure.UID_MISMATCH)
    }
    if (expectedPackage !in packagesForUid) {
        return RootCallerVerification(RootCallerFailure.PACKAGE_MISSING)
    }
    if (packagesForUid.any { !signaturesMatch(it) }) {
        return RootCallerVerification(RootCallerFailure.SIGNATURE_MISMATCH)
    }
    return RootCallerVerification()
}
