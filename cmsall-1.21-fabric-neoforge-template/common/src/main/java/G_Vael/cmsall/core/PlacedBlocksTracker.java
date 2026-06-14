package G_Vael.cmsall.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** Thin per-function facade over PlacedBlocksData. */
public final class PlacedBlocksTracker {

    /** how often (server ticks) and how much to sweep for orphaned positions. */
    private static final int SWEEP_INTERVAL = 200; // ~10s
    private static final int SWEEP_BUDGET = 128; // positions checked per kind per level per sweep
    private static int sweepTick;

    private PlacedBlocksTracker() {
    }

    /** Resets the sweep cursor (server shutdown). */
    public static void resetSweep() {
        sweepTick = 0;
    }

    /** bounded periodic orphan cleanup across all loaded levels (called each server tick). */
    public static void sweep(MinecraftServer server) {
        if (++sweepTick < SWEEP_INTERVAL) {
            return;
        }
        sweepTick = 0;
        for (ServerLevel level : server.getAllLevels()) {
            PlacedBlocksData data = PlacedBlocksData.get(level);
            for (Functions.Kind kind : Functions.Kind.values()) {
                if (RuntimeConfig.trackEnabled[kind.ordinal()]) {
                    data.sweep(level, kind, SWEEP_BUDGET);
                }
            }
        }
    }

    public static void record(ServerLevel level, Functions.Kind kind, BlockPos pos, BlockState state) {
        PlacedBlocksData.get(level).add(kind, pos.asLong(), PlacedBlocksData.blockKey(state));
    }

    public static void remove(ServerLevel level, Functions.Kind kind, BlockPos pos) {
        PlacedBlocksData.get(level).remove(kind, pos.asLong());
    }

    public static boolean isPlaced(ServerLevel level, Functions.Kind kind, BlockPos pos) {
        return PlacedBlocksData.get(level).contains(kind, pos.asLong());
    }

    /** A tracked position was set to newState; drop its placed-record once it's no longer that function's block
     *  (covers break/burn/explosion/decay/replace, including creative — anything that changes the block). */
    public static void onBlockReplaced(ServerLevel level, BlockPos pos, BlockState newState) {
        if (!RuntimeConfig.anyTrackEnabled) {
            return; // all tracking off — skip the data lookup on the hot setBlock path
        }
        if (BlockMoveContext.active()) {
            return; // a piston/API move is in progress; the records were already relocated — don't let the move's setBlocks drop them
        }
        PlacedBlocksData data = PlacedBlocksData.get(level);
        long packed = pos.asLong();
        if (RuntimeConfig.recordCommandPlace && CommandPlaceContext.active()) {
            Functions.Kind k = Functions.trackedKind(newState.getBlock());
            if (k != null) {
                data.add(k, packed, PlacedBlocksData.blockKey(newState));
            }
        }
        if (data.isEmpty()) {
            return; // nothing tracked here — skip the per-kind autoboxing/lookups on the hot setBlock path
        }
        for (Functions.Kind kind : Functions.Kind.values()) {
            if (data.contains(kind, packed)) {
                int rec = data.key(kind, packed);
                if (rec == 0) {
                    // unmigrated record: fall back to the list check until the sweep migrates it
                    if (Functions.trackedKind(newState.getBlock()) != kind) {
                        data.remove(kind, packed);
                    }
                } else if (PlacedBlocksData.blockKey(newState) != rec) {
                    data.remove(kind, packed); // block identity truly changed
                }
            }
        }
    }

    /** A piston moved these blocks one step along dir; carry their placed-records to the new positions. */
    public static void onPistonMove(ServerLevel level, List<BlockPos> pushed, Direction dir) {
        if (pushed.isEmpty()) {
            return;
        }
        Map<BlockPos, BlockPos> moves = new HashMap<>();
        for (BlockPos p : pushed) {
            moves.put(p, p.relative(dir));
        }
        relocate(level, moves);
    }

    /** Carry a placed-record from one position to another, keeping its block id. */
    public static void relocate(ServerLevel level, BlockPos from, BlockPos to) {
        if (level == null || from == null || to == null) {
            return;
        }
        Map<BlockPos, BlockPos> one = new HashMap<>();
        one.put(from, to);
        relocate(level, one);
    }

    /** Carry placed-records source->destination for a moved set (two-phase so shifting columns don't clobber). */
    public static void relocate(ServerLevel level, Map<BlockPos, BlockPos> fromTo) {
        if (level == null || fromTo == null || fromTo.isEmpty()) {
            return;
        }
        PlacedBlocksData data = PlacedBlocksData.get(level);
        for (Functions.Kind kind : Functions.Kind.values()) {
            List<long[]> carry = new ArrayList<>(); // [destPacked, id] for each source that held a record
            for (Map.Entry<BlockPos, BlockPos> e : fromTo.entrySet()) {
                if (e.getKey() == null || e.getValue() == null) {
                    continue;
                }
                long src = e.getKey().asLong();
                if (data.contains(kind, src)) {
                    carry.add(new long[]{ e.getValue().asLong(), data.key(kind, src) });
                }
            }
            if (carry.isEmpty()) {
                continue;
            }
            for (BlockPos src : fromTo.keySet()) {
                data.remove(kind, src.asLong());
            }
            for (long[] c : carry) {
                data.add(kind, c[0], (int) c[1]);
            }
        }
    }

    public static int count(ServerLevel level, Functions.Kind kind) {
        return PlacedBlocksData.get(level).count(kind);
    }

    /** Trims every server world's sets down to the configured per-kind cap (safe to call always). */
    public static void trim(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            PlacedBlocksData data = PlacedBlocksData.get(level);
            for (Functions.Kind kind : Functions.Kind.values()) {
                data.trimTo(kind, RuntimeConfig.trackMax[kind.ordinal()]);
            }
        }
    }

    /** Clears tracking across every server world: one kind, or all when kindOrNull is null. */
    public static void resetWorlds(MinecraftServer server, Functions.Kind kindOrNull) {
        for (ServerLevel level : server.getAllLevels()) {
            PlacedBlocksData data = PlacedBlocksData.get(level);
            if (kindOrNull == null) {
                data.resetAll();
            } else {
                data.reset(kindOrNull);
            }
        }
    }
}
