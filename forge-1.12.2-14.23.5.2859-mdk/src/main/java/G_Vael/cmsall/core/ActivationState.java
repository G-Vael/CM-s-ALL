package G_Vael.cmsall.core;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.minecraft.entity.player.EntityPlayerMP;

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

    private static final ConcurrentMap<UUID, State> STATES = new ConcurrentHashMap<UUID, State>();
    private static final ConcurrentMap<UUID, Boolean> REPLANT = new ConcurrentHashMap<UUID, Boolean>();
    private static final ConcurrentMap<UUID, Boolean> PROTECT = new ConcurrentHashMap<UUID, Boolean>();

    private ActivationState() {
    }

    public static boolean replant(EntityPlayerMP player) {
        Boolean v = REPLANT.get(player.getUniqueID());
        return v != null && v;
    }

    public static void setReplant(EntityPlayerMP player, boolean on) {
        REPLANT.put(player.getUniqueID(), on);
    }

    public static boolean flipReplant(EntityPlayerMP player) {
        boolean next = !replant(player);
        setReplant(player, next);
        return next;
    }

    /** per-player tool protection — defaults ON so a tool never breaks by accident. */
    public static boolean protectTool(EntityPlayerMP player) {
        Boolean v = PROTECT.get(player.getUniqueID());
        return v == null || v;
    }

    public static void setProtectTool(EntityPlayerMP player, boolean on) {
        PROTECT.put(player.getUniqueID(), on);
    }

    public static boolean flipProtectTool(EntityPlayerMP player) {
        boolean next = !protectTool(player);
        setProtectTool(player, next);
        return next;
    }

    public static ActivationMode mode(EntityPlayerMP player) {
        State s = STATES.get(player.getUniqueID());
        return s != null ? s.mode : RuntimeConfig.defaultMode;
    }

    public static void setMode(EntityPlayerMP player, ActivationMode mode) {
        State s = STATES.get(player.getUniqueID());
        STATES.put(player.getUniqueID(), new State(mode, s != null && s.toggledOn));
    }

    public static boolean toggledOn(EntityPlayerMP player) {
        State s = STATES.get(player.getUniqueID());
        return s != null && s.toggledOn;
    }

    public static void setToggle(EntityPlayerMP player, boolean on) {
        State s = STATES.get(player.getUniqueID());
        STATES.put(player.getUniqueID(), new State(s != null ? s.mode : RuntimeConfig.defaultMode, on));
    }

    public static boolean flipToggle(EntityPlayerMP player) {
        boolean next = !toggledOn(player);
        setToggle(player, next);
        return next;
    }

    public static void clear(EntityPlayerMP player) {
        STATES.remove(player.getUniqueID());
        REPLANT.remove(player.getUniqueID());
        PROTECT.remove(player.getUniqueID());
    }

    /** The live activation decision used by the engine (). */
    public static boolean isActive(EntityPlayerMP player) {
        switch (mode(player)) {
            case HOLD:
                return player.isSneaking();
            case SNEAK_INVERT:
                return !player.isSneaking();
            case ALWAYS:
                return true;
            case TOGGLE:
                return toggledOn(player);
            default:
                return false;
        }
    }
}
