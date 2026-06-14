package G_Vael.cmsall.core;

/** Marks the server thread while a /setblock or /fill is placing a block, so the tracker can record it. */
public final class CommandPlaceContext {
    private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private CommandPlaceContext() {
    }
    public static void set(boolean on) {
        ACTIVE.set(on);
    }
    public static boolean active() {
        return ACTIVE.get();
    }
}
