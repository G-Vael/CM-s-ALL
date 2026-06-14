package cmsall.api;

import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import G_Vael.cmsall.core.BlockMoveContext;
import G_Vael.cmsall.core.PlacedBlocksTracker;

/**
 * Public API for custom block-movers (Create, etc.) to keep CM'sALL's player-placed protection following blocks they relocate.
 *
 * <p>Minecraft has no generic "a block moved" event, so a mover that does NOT go through vanilla pistons must tell CM'sALL
 * where its blocks went. Typical use, called on the server thread while the records are still at their source positions:
 *
 * <pre>{@code
 * CmsAllTracking.beginMove();
 * try {
 *     CmsAllTracking.relocate(level, fromToMap); // map each source BlockPos to its destination
 *     // ... your mod performs the actual block moves (setBlock calls) here ...
 * } finally {
 *     CmsAllTracking.endMove();
 * }
 * }</pre>
 *
 * <p>If your move places the final block at the destination in a single step (no transient placeholder), the
 * {@code beginMove()/endMove()} bracket is optional — a bare {@link #relocate} before the move is enough.
 */
public final class CmsAllTracking {

    private CmsAllTracking() {
    }

    /** Carry the placed-block record (with its block id) from one position to another. Call before the source is cleared. */
    public static void relocate(ServerLevel level, BlockPos from, BlockPos to) {
        PlacedBlocksTracker.relocate(level, from, to);
    }

    /** Bulk relocate: each entry maps a source position to its destination. Two-phase, so shifting/overlapping moves are safe. */
    public static void relocate(ServerLevel level, Map<BlockPos, BlockPos> fromTo) {
        PlacedBlocksTracker.relocate(level, fromTo);
    }

    /** Suppress placed-record removal while your mover runs its setBlock calls, so transient states don't drop the records. */
    public static void beginMove() {
        BlockMoveContext.set(true);
    }

    /** End the suppression started by {@link #beginMove()}. Always pair them (use try/finally). */
    public static void endMove() {
        BlockMoveContext.set(false);
    }
}
