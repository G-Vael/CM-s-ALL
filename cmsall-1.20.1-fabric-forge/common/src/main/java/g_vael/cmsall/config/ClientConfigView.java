package g_vael.cmsall.config;

/** The last server config snapshot received on this client. */
public final class ClientConfigView {

    private static volatile ServerConfig current = new ServerConfig();
    /** Saved per-kind tracked counts (Kind-ordinal indexed) for this player's world. */
    public static volatile int[] counts = { 0, 0, 0 };
    /** Client-only hook fired when a sync arrives (proof the server runs the mod). */
    private static volatile Runnable onSync;

    private ClientConfigView() {
    }

    /** The client sets this to re-apply its saved per-player preferences once the server is confirmed. */
    public static void setSyncListener(Runnable listener) {
        onSync = listener;
    }

    public static void set(ServerConfig config, int[] newCounts) {
        if (config != null) {
            current = config;
            counts = newCounts != null && newCounts.length >= 3 ? newCounts : new int[]{ 0, 0, 0 };
            Runnable listener = onSync;
            if (listener != null) {
                listener.run();
            }
        }
    }

    public static ServerConfig get() {
        return current;
    }
}
