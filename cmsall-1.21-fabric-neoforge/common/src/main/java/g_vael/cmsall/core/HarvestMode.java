package g_vael.cmsall.core;

/** Propagation mode for a harvest group. */
public enum HarvestMode {
    /** MineAll: ore-style 3D connected vein. */
    VEIN,
    /** CutAll: tree trunk, optionally leaves (). */
    TREE,
    /** Cactus / bamboo / cake: vertical only. */
    COLUMN,
    /** DigAll: same-Y connected dirt/sand layer. */
    LAYER
}
