package g_vael.cmsall.core;

/**
 * Marks the server thread while blocks are being relocated (a vanilla piston, or an external mover via
 * {@code cmsall.api.CmsAllTracking}), so the tracker suppresses placed-record removal for the transient setBlocks
 * the move makes.
 *
 * <p>Re-entrant by design: {@link #push()}/{@link #pop()} maintain a depth counter (per server thread), so nested
 * moves - a mover whose setBlocks re-enter this path, or helpers that each bracket a move - never let an inner
 * {@code pop()} lift an outer move's suppression. {@link #reset()} is the end-of-tick backstop that clears a depth
 * leaked by a caller that pushed without popping.
 */
public final class BlockMoveContext {
    private static final ThreadLocal<int[]> DEPTH = ThreadLocal.withInitial(() -> new int[1]);

    private BlockMoveContext() {
    }

    /** Enter a move scope (suppress placed-record removal). Always pair with {@link #pop()} via try/finally. */
    public static void push() {
        DEPTH.get()[0]++;
    }

    /** Leave a move scope. No-op if the depth is already zero. */
    public static void pop() {
        int[] d = DEPTH.get();
        if (d[0] > 0) {
            d[0]--;
        }
    }

    /** Force depth to zero - end-of-tick backstop for a caller that leaked {@link #push()}. */
    public static void reset() {
        DEPTH.get()[0] = 0;
    }

    /** True while at least one move scope is open on this server thread. */
    public static boolean active() {
        return DEPTH.get()[0] > 0;
    }
}