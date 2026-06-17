package g_vael.cmsall.core;

import java.util.List;
import java.util.Set;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** The three harvest functions: MineAll (vein), CutAll (tree), DigAll (layer). */
public final class Functions {

    public enum Kind {
        MINE, CUT, DIG
    }

    private static volatile Set<Block> mineBlocks = Set.of();
    private static volatile Set<Block> cutBlocks = Set.of();
    private static volatile Set<Block> digBlocks = Set.of();
    private static volatile Set<Item> mineTools = Set.of();
    private static volatile Set<Item> cutTools = Set.of();
    private static volatile Set<Item> digTools = Set.of();

    private Functions() {
    }

    public static void setBlocks(Kind kind, Set<Block> blocks) {
        switch (kind) {
            case MINE -> mineBlocks = Set.copyOf(blocks);
            case CUT -> cutBlocks = Set.copyOf(blocks);
            case DIG -> digBlocks = Set.copyOf(blocks);
        }
    }

    public static void setTools(Kind kind, Set<Item> tools) {
        switch (kind) {
            case MINE -> mineTools = Set.copyOf(tools);
            case CUT -> cutTools = Set.copyOf(tools);
            case DIG -> digTools = Set.copyOf(tools);
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
        return switch (kind) {
            case MINE -> mineBlocks.contains(block);
            case CUT -> cutBlocks.contains(block);
            case DIG -> digBlocks.contains(block);
        };
    }

    public static Set<Item> tools(Kind kind) {
        return switch (kind) {
            case MINE -> mineTools;
            case CUT -> cutTools;
            case DIG -> digTools;
        };
    }

    /** True if stack is one of the configured trigger tools for the kind (only if configured). */
    public static boolean toolAllowed(Kind kind, ItemStack stack) {
        return tools(kind).contains(stack.getItem());
    }

    /** fixed mechanics are immutable literals — share one instance each instead of allocating a HarvestGroup on every break. */
    private static final HarvestGroup MINE_MECH = new HarvestGroup("mineall", HarvestMode.VEIN, List.of(), Set.of(), Set.of(),
            true, 26, 256, 16, true, DropMode.NORMAL, 1.0f, false, true, PlayerPlaced.INCLUDE, true);
    private static final HarvestGroup CUT_MECH = new HarvestGroup("cutall", HarvestMode.TREE, List.of(), Set.of(), Set.of(),
            false, 26, 256, 24, true, DropMode.NORMAL, 1.0f, false, true, PlayerPlaced.EXCLUDE, true);
    private static final HarvestGroup DIG_MECH = new HarvestGroup("digall", HarvestMode.LAYER, List.of(), Set.of(), Set.of(),
            false, 26, 256, 12, true, DropMode.NORMAL, 1.0f, false, true, PlayerPlaced.INCLUDE, true);

    /** Fixed propagation mechanics per function. */
    public static HarvestGroup mechanics(Kind kind) {
        return switch (kind) {
            case MINE -> MINE_MECH;
            case CUT -> CUT_MECH;
            case DIG -> DIG_MECH;
        };
    }

    /** The function whose placement-tracking is enabled for this block, or null. */
    public static Kind trackedKind(Block block) {
        Kind k = functionFor(block);
        return (k != null && RuntimeConfig.trackEnabled[k.ordinal()]) ? k : null;
    }
}
