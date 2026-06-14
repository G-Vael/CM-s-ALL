package cmsall.api;

import java.util.Map;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import G_Vael.cmsall.core.BlockMoveContext;
import G_Vael.cmsall.core.PlacedBlocksTracker;

/**
 * Public API for custom block-movers (Create, etc.) to keep CM'sALL's player-placed protection following blocks they relocate.
 *
 * <p>Minecraft has no generic "a block moved" event, so a mover that does NOT go through vanilla mechanics must tell CM'sALL
 * where its blocks went. Typical use, called on the server thread while the records are still at their source positions:
 *
 * <pre>{@code
 * CmsAllTracking.beginMove();
 * try {
 *     CmsAllTracking.relocate(world, fromToMap); // map each source BlockPos to its destination
 *     // ... your mod performs the actual block moves (setBlock calls) here ...
 * } finally {
 *     CmsAllTracking.endMove();
 * }
 * }</pre>
 *
 * <p>If your move places the final block at the destination in a single step (no transient placeholder), the
 * {@code beginMove()/endMove()} bracket is optional — a bare {@link #relocate} before the move is enough.
 *
 * <p>This lives in the neutral {@code cmsall.api} package so dependents need not touch the mod's internal package.
 */
public final class CmsAllTracking {

    private CmsAllTracking() {
    }

    /** Carry the placed-block record (with its block id) from one position to another. Call before the source is cleared. */
    public static void relocate(World world, BlockPos from, BlockPos to) {
        PlacedBlocksTracker.relocate(world, from, to);
    }

    /** Bulk relocate: each entry maps a source position to its destination. Two-phase, so shifting/overlapping moves are safe. */
    public static void relocate(World world, Map<BlockPos, BlockPos> fromTo) {
        PlacedBlocksTracker.relocate(world, fromTo);
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
