package g_vael.cmsall.core;

import java.util.List;
import java.util.Set;

import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/** A harvest group definition. */
public record HarvestGroup(
        String id,
        HarvestMode mode,
        List<TagKey<Block>> blockTags,
        Set<Block> blocks,
        Set<Block> blocksExclude,
        boolean sameBlockOnly,
        int neighbours,
        int maxBlocks,
        int maxRadius,
        boolean requireCorrectTool,
        DropMode dropMode,
        float dropChance,
        boolean breakLeaves,
        boolean leafDrops,
        PlayerPlaced playerPlaced,
        boolean enabled
) {
    /** True if this group tracks player placement (anything other than the default INCLUDE). */
    public boolean tracksPlacement() {
        return playerPlaced != PlayerPlaced.INCLUDE;
    }
}
