package g_vael.cmsall.core;

/** Mechanics carrier for a harvest function. */
public final class HarvestGroup {

    private final String id;
    private final HarvestMode mode;
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

    public HarvestGroup(String id, HarvestMode mode, boolean sameBlockOnly, int neighbours, int maxBlocks,
                        int maxRadius, boolean requireCorrectTool, DropMode dropMode, float dropChance,
                        boolean breakLeaves, boolean leafDrops, PlayerPlaced playerPlaced, boolean enabled) {
        this.id = id;
        this.mode = mode;
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
