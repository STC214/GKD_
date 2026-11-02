package li.songe.gkd.root

/** Owner-0/XSpace-999 trial only. No package or user supplied by the socket client is trusted. */
internal data class CrossUserNodeLease(
    val enabled: Boolean = false,
    val expiresElapsed: Long = 0,
    val excluded: Set<String> = emptySet(),
    val allowed: Set<String> = emptySet(),
) {
    fun permits(peerUid: Int, peerPackages: Set<String>, nowElapsed: Long): Boolean =
        enabled && nowElapsed >= 0 && nowElapsed < expiresElapsed &&
            peerUid / 100000 == 999 && peerUid % 100000 >= 10000 &&
            peerPackages.size == 1 && peerPackages.single() in allowed &&
            peerPackages.single() !in excluded
}
