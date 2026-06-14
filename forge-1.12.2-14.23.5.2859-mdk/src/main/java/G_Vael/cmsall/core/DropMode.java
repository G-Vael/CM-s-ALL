package G_Vael.cmsall.core;

/** Per-group drop behaviour for chain-broken blocks. */
public enum DropMode {
    /** Drops as normal (subject to tool enchantments). */
    NORMAL,
    /** Block is broken but yields no drops at all. */
    NONE,
    /** Each broken block drops with probability dropChance. */
    CHANCE
}
