package G_Vael.cmsall.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;

/** The global chain-break denylist. */
public final class HarvestGroups {

    /** Built-in blocks that must never be chain-broken regardless of hardness. */
    private static final Set<Block> DENYLIST = new HashSet<Block>(Arrays.asList(
            Blocks.MOB_SPAWNER,
            Blocks.END_PORTAL_FRAME
    ));

    /** Extra denylist from the runtime config ([denylist]), applied on top of the built-ins. */
    private static volatile Set<Block> extraDenylist = Collections.emptySet();

    private HarvestGroups() {
    }

    public static void setExtraDenylist(Set<Block> blocks) {
        extraDenylist = Collections.unmodifiableSet(new HashSet<Block>(blocks));
    }

    public static boolean isDenied(IBlockState state) {
        Block block = state.getBlock();
        return DENYLIST.contains(block) || extraDenylist.contains(block);
    }
}
