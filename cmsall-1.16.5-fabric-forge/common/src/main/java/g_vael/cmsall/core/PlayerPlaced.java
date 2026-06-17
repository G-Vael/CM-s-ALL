package g_vael.cmsall.core;

/** How a group treats player-placed blocks. */
public enum PlayerPlaced {
    /** Natural and player-placed both chain (default). */
    INCLUDE,
    /** Player-placed blocks do not chain. */
    EXCLUDE,
    /** Only player-placed blocks chain. */
    ONLY
}
