package G_Vael.cmsall.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.world.item.Item;

/** Global runtime limits. */
public final class RuntimeConfig {
    public static boolean enabled = true;
    public static boolean allowInCreative = false;
    public static ActivationMode defaultMode = ActivationMode.HOLD;
    public static Set<String> allowedModes = new HashSet<>(Arrays.asList("hold", "toggle", "always", "sneak_invert"));
    public static int globalMaxBlocks = 256;
    public static int perTickBudget = 0;
    public static double durabilityPerBlock = 1.0;
    public static double exhaustionPerBlock = 0.005;
    public static boolean dropToInventory = false;
    public static boolean gatherDrops = false;
    public static boolean chainBreakEffects = true;
    public static boolean cutBreakLeaves = false;
    public static boolean cutLeafDrops = true;
    public static boolean cutFromTop = false;
    public static boolean respectProtectionPerBlock = true;
    public static boolean recordCommandPlace = false;

    // per-function placed-block tracking (Kind-ordinal indexed)
    public static boolean[] trackEnabled = { false, true, false };
    public static int[] trackMax = { 4096, 16384, 4096 };
    public static boolean trackEvict = true;
    /** false only when all three kinds' tracking is off; lets the per-setBlock hook skip its lookup. */
    public static volatile boolean anyTrackEnabled = true;

    public static boolean despawnEnabled = false;
    public static int despawnSeconds = 30;
    public static Set<Item> despawnItems = Collections.emptySet();

    private RuntimeConfig() {
    }
}
