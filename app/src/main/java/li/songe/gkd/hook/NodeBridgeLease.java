package li.songe.gkd.hook;

/** Owner and framework share the same device elapsed clock; wall-clock edits never extend a lease. */
public final class NodeBridgeLease {
    private NodeBridgeLease() { }
    public static boolean isValid(boolean enabled, long deadlineElapsed, long nowElapsed) {
        return enabled && nowElapsed >= 0 && deadlineElapsed > nowElapsed &&
            deadlineElapsed - nowElapsed <= 15_000L;
    }
    public static boolean isValid(boolean enabled, long deadlineElapsed, long nowElapsed,
            long deadlineWall, long nowWall) {
        // The elapsed bound prevents wall-clock rollback extension. The wall bound also
        // rejects a persisted lease replayed after reboot when elapsed values start over.
        return isValid(enabled, deadlineElapsed, nowElapsed) && nowWall >= 0 &&
            deadlineWall > nowWall && deadlineWall - nowWall <= 15_000L;
    }
}
