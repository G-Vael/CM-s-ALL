package G_Vael.cmsall.core;

import java.util.List;
import java.util.Set;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

/** A harvest group definition. */
public final class HarvestGroup {

    private final String id;
    private final HarvestMode mode;
    private final List<ResourceLocation> blockTags;
    private final Set<Block> blocks;
    private final Set<Block> blocksExclude;
    private final boolean sameBlockOnly;
    private final int neighbours;
    private final int maxBlocks;
    private final int maxRadius;
    private final boolean requireCorrectTool;
    private final DropMode dropMode;
    private final float dropChance;
    private final boolean breakLeaves;
    private final boolean leafDrops;
    private final PlayerPlaced playerPlaced;
    private final boolean enabled;

    public HarvestGroup(String id, HarvestMode mode, List<ResourceLocation> blockTags, Set<Block> blocks,
                        Set<Block> blocksExclude, boolean sameBlockOnly, int neighbours, int maxBlocks,
                        int maxRadius, boolean requireCorrectTool, DropMode dropMode, float dropChance,
                        boolean breakLeaves, boolean leafDrops, PlayerPlaced playerPlaced, boolean enabled) {
        this.id = id;
        this.mode = mode;
        this.blockTags = blockTags;
        this.blocks = blocks;
        this.blocksExclude = blocksExclude;
        this.sameBlockOnly = sameBlockOnly;
        this.neighbours = neighbours;
        this.maxBlocks = maxBlocks;
        this.maxRadius = maxRadius;
        this.requireCorrectTool = requireCorrectTool;
        this.dropMode = dropMode;
        this.dropChance = dropChance;
        this.breakLeaves = breakLeaves;
        this.leafDrops = leafDrops;
        this.playerPlaced = playerPlaced;
        this.enabled = enabled;
    }

    public String id() {
        return id;
    }

    public HarvestMode mode() {
        return mode;
    }

    public List<ResourceLocation> blockTags() {
        return blockTags;
    }

    public Set<Block> blocks() {
        return blocks;
    }

    public Set<Block> blocksExclude() {
        return blocksExclude;
    }

    public boolean sameBlockOnly() {
        return sameBlockOnly;
    }

    public int neighbours() {
        return neighbours;
    }

    public int maxBlocks() {
        return maxBlocks;
    }

    public int maxRadius() {
        return maxRadius;
    }

    public boolean requireCorrectTool() {
        return requireCorrectTool;
    }

    public DropMode dropMode() {
        return dropMode;
    }

    public float dropChance() {
        return dropChance;
    }

    public boolean breakLeaves() {
        return breakLeaves;
    }

    public boolean leafDrops() {
        return leafDrops;
    }

    public PlayerPlaced playerPlaced() {
        return playerPlaced;
    }

    public boolean enabled() {
        return enabled;
    }

    /** True if this group tracks player placement (anything other than the default INCLUDE). */
    public boolean tracksPlacement() {
        return playerPlaced != PlayerPlaced.INCLUDE;
    }
}
