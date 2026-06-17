package g_vael.cmsall.core;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** The three harvest functions: MineAll (vein), CutAll (tree), DigAll (layer). */
public final class Functions {

    public enum Kind {
        MINE, CUT, DIG
    }

    private static volatile Set<Block> mineBlocks = Collections.emptySet();
    private static volatile Set<Block> cutBlocks = Collections.emptySet();
    private static volatile Set<Block> digBlocks = Collections.emptySet();
    private static volatile Set<Item> mineTools = Collections.emptySet();
    private static volatile Set<Item> cutTools = Collections.emptySet();
    private static volatile Set<Item> digTools = Collections.emptySet();

    private Functions() {
    }

    public static void setBlocks(Kind kind, Set<Block> blocks) {
        Set<Block> copy = Collections.unmodifiableSet(new HashSet<>(blocks));
        switch (kind) {
            case MINE:
                mineBlocks = copy;
                break;
            case CUT:
                cutBlocks = copy;
                break;
            case DIG:
                digBlocks = copy;
                break;
        }
    }

    public static void setTools(Kind kind, Set<Item> tools) {
        Set<Item> copy = Collections.unmodifiableSet(new HashSet<>(tools));
        switch (kind) {
            case MINE:
                mineTools = copy;
                break;
            case CUT:
                cutTools = copy;
                break;
            case DIG:
                digTools = copy;
                break;
        }
    }

    /** Which function (if any) owns this block; MineAll wins ties, then CutAll, then DigAll. */
    public static Kind functionFor(Block block) {
        if (mineBlocks.contains(block)) {
            return Kind.MINE;
        }
        if (cutBlocks.contains(block)) {
            return Kind.CUT;
        }
        if (digBlocks.contains(block)) {
            return Kind.DIG;
        }
        return null;
    }

    public static boolean blockInKind(Kind kind, Block block) {
        switch (kind) {
            case MINE:
                return mineBlocks.contains(block);
            case CUT:
                return cutBlocks.contains(block);
            case DIG:
                return digBlocks.contains(block);
            default:
                return false;
        }
    }

    public static Set<Item> tools(Kind kind) {
        switch (kind) {
            case MINE:
                return mineTools;
            case CUT:
                return cutTools;
            case DIG:
                return digTools;
            default:
                return Collections.emptySet();
        }
    }

    /** True if stack is one of the configured trigger tools for the kind (only if configured). */
    public static boolean toolAllowed(Kind kind, ItemStack stack) {
        return tools(kind).contains(stack.getItem());
    }

    /** fixed mechanics are immutable literals — share one instance each instead of allocating a HarvestGroup on every break. */
    private static final HarvestGroup MINE_MECH = new HarvestGroup("mineall", HarvestMode.VEIN,
            Collections.emptyList(), Collections.emptySet(), Collections.emptySet(),
            true, 26, 256, 16, true, DropMode.NORMAL, 1.0f, false, true, PlayerPlaced.INCLUDE, true);
    private static final HarvestGroup CUT_MECH = new HarvestGroup("cutall", HarvestMode.TREE,
            Collections.emptyList(), Collections.emptySet(), Collections.emptySet(),
            false, 26, 256, 24, true, DropMode.NORMAL, 1.0f, false, true, PlayerPlaced.EXCLUDE, true);
    private static final HarvestGroup DIG_MECH = new HarvestGroup("digall", HarvestMode.LAYER,
            Collections.emptyList(), Collections.emptySet(), Collections.emptySet(),
            false, 26, 256, 12, true, DropMode.NORMAL, 1.0f, false, true, PlayerPlaced.INCLUDE, true);

    /** Fixed propagation mechanics per function. */
    public static HarvestGroup mechanics(Kind kind) {
        switch (kind) {
            case MINE:
                return MINE_MECH;
            case CUT:
                return CUT_MECH;
            case DIG:
                return DIG_MECH;
            default:
                return MINE_MECH;
        }
    }

    /** The function whose placement-tracking is enabled for this block, or null. */
    public static Kind trackedKind(Block block) {
        Kind k = functionFor(block);
        return (k != null && RuntimeConfig.trackEnabled[k.ordinal()]) ? k : null;
    }
}
