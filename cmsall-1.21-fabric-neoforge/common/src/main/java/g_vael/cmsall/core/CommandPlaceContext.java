package g_vael.cmsall.core;

/**
 * Marks the server thread while a /setblock or /fill is placing a block, so the tracker can record it.
 *
 * <p>Re-entrant depth counter (per server thread), mirroring {@link BlockMoveContext}: nested command-driven placements
 * (e.g. a command block / datapack chain that triggers another placing command within the same synchronous call) push
 * and pop a depth so an inner pop cannot clear an outer scope early. {@link #reset()} is the end-of-tick backstop.
 */
public final class CommandPlaceContext {
    private static final ThreadLocal<int[]> DEPTH = ThreadLocal.withInitial(() -> new int[1]);

    private CommandPlaceContext() {
    }

    /** Enter a command-place scope. Always pair with {@link #pop()} via try/finally. */
    public static void push() {
        DEPTH.get()[0]++;
    }

    /** Leave a command-place scope. No-op if the depth is already zero. */
    public static void pop() {
        int[] d = DEPTH.get();
        if (d[0] > 0) {
            d[0]--;
        }
    }

    /** Force depth to zero (end-of-tick backstop for a leaked push). */
    public static void reset() {
        DEPTH.get()[0] = 0;
    }

    public static boolean active() {
        return DEPTH.get()[0] > 0;
    }
}
