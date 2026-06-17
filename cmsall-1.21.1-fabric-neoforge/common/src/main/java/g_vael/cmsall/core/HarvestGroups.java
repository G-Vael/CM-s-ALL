package g_vael.cmsall.core;

import java.util.Set;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** The global chain-break denylist. */
public final class HarvestGroups {

    /** Built-in blocks that must never be chain-broken regardless of hardness. */
    private static final Set<Block> DENYLIST = Set.of(
            Blocks.SPAWNER,
            Blocks.END_PORTAL_FRAME,
            Blocks.REINFORCED_DEEPSLATE
    );

    /** Extra denylist from the runtime config ( [denylist]), applied on top of the built-ins. */
    private static volatile Set<Block> extraDenylist = Set.of();

    private HarvestGroups() {
    }

    public static void setExtraDenylist(Set<Block> blocks) {
        extraDenylist = Set.copyOf(blocks);
    }

    public static boolean isDenied(BlockState state) {
        Block block = state.getBlock();
        return DENYLIST.contains(block) || extraDenylist.contains(block);
    }
}
