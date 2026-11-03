package li.songe.gkd.root

/** Owner-0/XSpace-999 trial only. No package or user supplied by the socket client is trusted. */
internal data class CrossUserNodeLease(
    val enabled: Boolean = false,
    val expiresElapsed: Long = 0,
    val excluded: Set<String> = emptySet(),
    val allowed: Set<String> = emptySet(),
    val global: Boolean = false,
) {
    fun permits(peerUid: Int, peerPackages: Set<String>, nowElapsed: Long): Boolean =
        enabled && nowElapsed >= 0 && nowElapsed < expiresElapsed &&
            (peerUid / 100000 == 999 || (global && peerUid / 100000 == 0)) &&
            peerUid % 100000 in 10000..19999 &&
            peerPackages.size == 1 && (global || peerPackages.single() in allowed) &&
            peerPackages.single() !in excluded
}
