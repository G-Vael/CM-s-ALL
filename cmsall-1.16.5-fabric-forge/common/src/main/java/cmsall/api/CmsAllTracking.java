package cmsall.api;

import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import g_vael.cmsall.core.BlockMoveContext;
import g_vael.cmsall.core.PlacedBlocksTracker;

/**
 * Public API for custom block-movers (custom pistons, conveyors, etc.) to keep CM'sALL's player-placed protection following blocks they relocate.
 *
 * <p>Minecraft has no generic "a block moved" event, so a mover that does NOT go through vanilla pistons must tell CM'sALL
 * where its blocks went. Call everything here on the <b>server thread</b>. Recommended pattern: bracket the move and
 * {@link #relocate} only the blocks that actually landed, AFTER moving, so a move that fails partway leaves the un-moved
 * source blocks protected:
 *
 * <pre>{@code
 * CmsAllTracking.beginMove();
 * try {
 *     // ... your mod performs the actual block moves (setBlock calls) here ...
 *     CmsAllTracking.relocate(level, landedFromTo); // only blocks that landed; map each source BlockPos to its destination
 * } finally {
 *     CmsAllTracking.endMove();
 * }
 * }</pre>
 *
 * <p>The {@code beginMove()/endMove()} bracket is <b>re-entrant</b> - nesting it (e.g. helpers that each bracket a move) is
 * safe. You may instead {@code relocate} before clearing the source and skip the bracket ONLY when the move is a single
 * step that <b>cannot fail</b>.
 *
 * <p><b>Partial failures:</b> relocating first and then failing leaves the still-present source block protected by nothing:
 * CM'sALL's orphan sweep drops the now-bogus destination record (its stored block id no longer matches the block actually
 * there), but it cannot re-create a record for an unrecorded source block. So relocate-after-landing is the fail-safe order.
 *
 * <p><b>Soft dependency:</b> referencing these methods when CM'sALL is not installed risks {@link NoClassDefFoundError}.
 * The robust idiom is to isolate every CM'sALL reference in one dedicated integration class and touch it only when CM'sALL
 * is present (class linking can load referenced types before a guard line runs, so "this path doesn't execute" is not always
 * enough). A presence check around an isolated reference, or a {@code compileOnly} dependency, both work.
 *
 * <p>This lives in the neutral {@code cmsall.api} package so dependents need not touch the mod's internal package.
 */
public final class CmsAllTracking {

    private CmsAllTracking() {
    }

    /** Carry the placed-block record (with its block id) from one position to another, on the server thread. When using
     *  {@link #beginMove()}/{@link #endMove()}, call this after the block has successfully landed at its destination (see the class doc for the fail-safe ordering). Returns the number of records moved (0 if nothing was tracked there, otherwise 1). */
    public static int relocate(ServerLevel level, BlockPos from, BlockPos to) {
        return PlacedBlocksTracker.relocate(level, from, to);
    }

    /** Bulk relocate on the server thread: each entry maps a source position to its destination. Two-phase, so
     *  shifting/overlapping moves are safe. Returns how many placed-records were actually carried. */
    public static int relocate(ServerLevel level, Map<BlockPos, BlockPos> fromTo) {
        return PlacedBlocksTracker.relocate(level, fromTo);
    }

    /** Suppress placed-record removal (server thread) while your mover runs its setBlock calls. Re-entrant (safe to nest);
     *  always pair with {@link #endMove()} via try/finally. */
    public static void beginMove() {
        BlockMoveContext.push();
    }

    /** End one suppression scope started by {@link #beginMove()}. Always pair them (use try/finally). */
    public static void endMove() {
        BlockMoveContext.pop();
    }
}