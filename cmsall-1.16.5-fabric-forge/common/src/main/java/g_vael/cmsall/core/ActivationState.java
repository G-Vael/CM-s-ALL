package g_vael.cmsall.core;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.minecraft.server.level.ServerPlayer;

/** Per-player activation. */
public final class ActivationState {

    private static final class State {
        final ActivationMode mode;
        final boolean toggledOn;

        State(ActivationMode mode, boolean toggledOn) {
            this.mode = mode;
            this.toggledOn = toggledOn;
        }
    }

    private static final ConcurrentMap<UUID, State> STATES = new ConcurrentHashMap<>();

    /** Per-player auto-replant preference (client/keybind toggled, default off). */
    private static final ConcurrentMap<UUID, Boolean> REPLANT = new ConcurrentHashMap<>();

    /** Per-player tool-protection preference (client setting, default ON). */
    private static final ConcurrentMap<UUID, Boolean> PROTECT = new ConcurrentHashMap<>();

    private ActivationState() {
    }

    public static boolean replant(ServerPlayer player) {
        return REPLANT.getOrDefault(player.getUUID(), Boolean.FALSE);
    }

    public static void setReplant(ServerPlayer player, boolean on) {
        REPLANT.put(player.getUUID(), on);
    }

    public static boolean flipReplant(ServerPlayer player) {
        boolean next = !replant(player);
        setReplant(player, next);
        return next;
    }

    /** per-player tool protection — defaults ON so a tool never breaks by accident. */
    public static boolean protectTool(ServerPlayer player) {
        return PROTECT.getOrDefault(player.getUUID(), Boolean.TRUE);
    }

    public static void setProtectTool(ServerPlayer player, boolean on) {
        PROTECT.put(player.getUUID(), on);
    }

    public static boolean flipProtectTool(ServerPlayer player) {
        boolean next = !protectTool(player);
        setProtectTool(player, next);
        return next;
    }

    public static ActivationMode mode(ServerPlayer player) {
        State s = STATES.get(player.getUUID());
        return s != null ? s.mode : RuntimeConfig.defaultMode;
    }

    public static void setMode(ServerPlayer player, ActivationMode mode) {
        STATES.compute(player.getUUID(),
                (id, s) -> new State(mode, s != null && s.toggledOn));
    }

    public static boolean toggledOn(ServerPlayer player) {
        State s = STATES.get(player.getUUID());
        return s != null && s.toggledOn;
    }

    public static void setToggle(ServerPlayer player, boolean on) {
        STATES.compute(player.getUUID(),
                (id, s) -> new State(s != null ? s.mode : RuntimeConfig.defaultMode, on));
    }

    public static boolean flipToggle(ServerPlayer player) {
        boolean next = !toggledOn(player);
        setToggle(player, next);
        return next;
    }

    public static void clear(ServerPlayer player) {
        STATES.remove(player.getUUID());
        REPLANT.remove(player.getUUID());
        PROTECT.remove(player.getUUID());
    }

    /** The live activation decision used by the engine (). */
    public static boolean isActive(ServerPlayer player) {
        switch (mode(player)) {
            case HOLD:
                return player.isShiftKeyDown();
            case SNEAK_INVERT:
                return !player.isShiftKeyDown();
            case ALWAYS:
                return true;
            case TOGGLE:
                return toggledOn(player);
            default:
                return false;
        }
    }
}
