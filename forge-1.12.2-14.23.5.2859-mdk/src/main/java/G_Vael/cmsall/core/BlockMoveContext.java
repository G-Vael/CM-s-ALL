package G_Vael.cmsall.core;

/** Marks the server thread while blocks are being relocated (an external mover via the API), so the tracker
 *  suppresses placed-record removal for the transient setBlocks the move makes.
 *
 *  <p>1.12.2 has no setBlock hook / no piston event, so there is no piston-follow or onBlockReplaced suppression
 *  to wire here — this flag exists for parity and as a backstop reset (the SERVER tick END resets it). */
public final class BlockMoveContext {
    private static final ThreadLocal<Boolean> ACTIVE = new ThreadLocal<Boolean>();

    private BlockMoveContext() {
    }

    public static void set(boolean on) {
        if (on) {
            ACTIVE.set(Boolean.TRUE);
        } else {
            ACTIVE.remove();
        }
    }

    public static boolean active() {
        return Boolean.TRUE.equals(ACTIVE.get());
    }
}
