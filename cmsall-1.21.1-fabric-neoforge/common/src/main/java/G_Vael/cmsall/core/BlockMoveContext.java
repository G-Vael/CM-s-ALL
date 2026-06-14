package G_Vael.cmsall.core;

/** Marks the server thread while blocks are being relocated (piston or an external mover via the API), so the tracker
 *  suppresses placed-record removal for the transient setBlocks the move makes. */
public final class BlockMoveContext {
    private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private BlockMoveContext() {
    }

    public static void set(boolean on) {
        ACTIVE.set(on);
    }

    public static boolean active() {
        return ACTIVE.get();
    }
}
