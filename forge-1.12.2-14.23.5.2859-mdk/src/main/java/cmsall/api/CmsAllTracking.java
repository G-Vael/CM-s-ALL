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
 * where its blocks went. Call everything here on the <b>server thread</b>, while the records are still at their source positions:
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
 * {@code beginMove()/endMove()} bracket is optional - a bare {@link #relocate} before the move is enough.
 *
 * <p><b>Partial failures:</b> if your move can fail partway, bracket with {@code beginMove()/endMove()} and call
 * {@link #relocate} only for the blocks that actually landed, AFTER the move. Blocks that failed to move then keep their
 * source record (fail-safe - they stay protected). Relocating first and then failing leaves the still-present source block
 * protected by nothing: CM'sALL's orphan sweep drops the now-bogus destination record (its stored block id no longer
 * matches the block actually there), but it cannot re-create a record for an unrecorded source block.
 *
 * <p><b>Soft dependency:</b> these methods delegate to CM'sALL internals, so calling them when CM'sALL is not installed
 * throws {@link NoClassDefFoundError}. Gate calls behind a mod-presence check, or depend on CM'sALL as {@code compileOnly}
 * and reference this class only from paths that run when CM'sALL is present.
 *
 * <p>This lives in the neutral {@code cmsall.api} package so dependents need not touch the mod's internal package.
 */
public final class CmsAllTracking {

    private CmsAllTracking() {
    }

    /** Carry the placed-block record (with its block id) from one position to another, on the server thread. Call before the source is cleared. */
    public static void relocate(World world, BlockPos from, BlockPos to) {
        PlacedBlocksTracker.relocate(world, from, to);
    }

    /** Bulk relocate on the server thread: each entry maps a source position to its destination. Two-phase, so shifting/overlapping moves are safe. */
    public static void relocate(World world, Map<BlockPos, BlockPos> fromTo) {
        PlacedBlocksTracker.relocate(world, fromTo);
    }

    /** Suppress placed-record removal (server thread) while your mover runs its setBlock calls. Re-entrant; always pair with {@link #endMove()} via try/finally. */
    public static void beginMove() {
        BlockMoveContext.push();
    }

    /** End one suppression scope started by {@link #beginMove()}. Always pair them (use try/finally). */
    public static void endMove() {
        BlockMoveContext.pop();
    }
}