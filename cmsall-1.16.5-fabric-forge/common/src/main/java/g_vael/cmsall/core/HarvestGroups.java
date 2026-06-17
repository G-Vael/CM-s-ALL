package g_vael.cmsall.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** The global chain-break denylist. */
public final class HarvestGroups {

    /** Built-in blocks that must never be chain-broken regardless of hardness. */
    private static final Set<Block> DENYLIST = new HashSet<>(Arrays.asList(
            Blocks.SPAWNER,
            Blocks.END_PORTAL_FRAME
    ));

    /** Extra denylist from the runtime config ( [denylist]), applied on top of the built-ins. */
    private static volatile Set<Block> extraDenylist = Collections.emptySet();

    private HarvestGroups() {
    }

    public static void setExtraDenylist(Set<Block> blocks) {
        extraDenylist = Collections.unmodifiableSet(new HashSet<>(blocks));
    }

    public static boolean isDenied(BlockState state) {
        Block block = state.getBlock();
        return DENYLIST.contains(block) || extraDenylist.contains(block);
    }
}
