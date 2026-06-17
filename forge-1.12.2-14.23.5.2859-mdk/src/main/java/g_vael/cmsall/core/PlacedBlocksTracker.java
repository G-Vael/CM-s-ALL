package g_vael.cmsall.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.state.IBlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/** Thin per-function facade over PlacedBlocksData. */
public final class PlacedBlocksTracker {

    private static final int SWEEP_INTERVAL = 200; // ~10s
    private static final int SWEEP_BUDGET = 128; // positions checked per kind per world per sweep
    private static int sweepTick;

    private PlacedBlocksTracker() {
    }

    /** Resets the sweep cursor (server shutdown). */
    public static void resetSweep() {
        sweepTick = 0;
    }

    /** bounded periodic orphan cleanup across all loaded worlds (called each server tick). */
    public static void sweep(MinecraftServer server) {
        if (++sweepTick < SWEEP_INTERVAL) {
            return;
        }
        sweepTick = 0;
        for (WorldServer world : server.worlds) {
            PlacedBlocksData data = PlacedBlocksData.get(world);
            for (Functions.Kind kind : Functions.Kind.values()) {
                if (RuntimeConfig.trackEnabled[kind.ordinal()]) {
                    data.sweep(world, kind, SWEEP_BUDGET);
                }
            }
        }
    }

    public static void record(World world, Functions.Kind kind, BlockPos pos, IBlockState state) {
        PlacedBlocksData.get(world).add(kind, pos.toLong(), PlacedBlocksData.blockKey(state));
    }

    public static void remove(World world, Functions.Kind kind, BlockPos pos) {
        PlacedBlocksData.get(world).remove(kind, pos.toLong());
    }

    public static boolean isPlaced(World world, Functions.Kind kind, BlockPos pos) {
        return PlacedBlocksData.get(world).contains(kind, pos.toLong());
    }

    /** A block was broken (incl. creative); drop its placed-record for the broken block's function, if any. */
    public static void onBlockBroken(World world, BlockPos pos, IBlockState brokenState) {
        Functions.Kind k = Functions.trackedKind(brokenState.getBlock());
        if (k != null) {
            remove(world, k, pos);
        }
    }

    /** A piston moved these blocks one step along dir; carry their placed-records to the new positions. */
    public static void onPistonMove(World world, List<BlockPos> pushed, EnumFacing dir) {
        if (!(world instanceof WorldServer) || pushed.isEmpty()) {
            return;
        }
        Map<BlockPos, BlockPos> moves = new HashMap<BlockPos, BlockPos>();
        for (BlockPos p : pushed) {
            moves.put(p, p.offset(dir));
        }
        relocate(world, moves);
    }

    /** Carry a placed-record from one position to another, keeping its block id. Returns the number of records moved. */
    public static int relocate(World world, BlockPos from, BlockPos to) {
        if (world == null || from == null || to == null) {
            return 0;
        }
        Map<BlockPos, BlockPos> one = new HashMap<BlockPos, BlockPos>();
        one.put(from, to);
        return relocate(world, one);
    }

    /** Carry placed-records source->destination for a moved set (two-phase so shifting columns don't clobber).
     *  Returns how many placed-records were actually carried (summed across functions). */
    public static int relocate(World world, Map<BlockPos, BlockPos> fromTo) {
        if (!(world instanceof WorldServer) || fromTo == null || fromTo.isEmpty()) {
            return 0;
        }
        PlacedBlocksData data = PlacedBlocksData.get(world);
        int moved = 0;
        for (Functions.Kind kind : Functions.Kind.values()) {
            List<long[]> carry = new ArrayList<long[]>(); // [destPacked, id] for each source that held a record
            for (Map.Entry<BlockPos, BlockPos> e : fromTo.entrySet()) {
                if (e.getKey() == null || e.getValue() == null) {
                    continue;
                }
                long src = e.getKey().toLong();
                if (data.contains(kind, src)) {
                    carry.add(new long[]{ e.getValue().toLong(), data.key(kind, src) });
                }
            }
            if (carry.isEmpty()) {
                continue;
            }
            for (BlockPos src : fromTo.keySet()) {
                if (src != null) {
                    data.remove(kind, src.toLong());
                }
            }
            for (long[] c : carry) {
                data.add(kind, c[0], (int) c[1]);
            }
            moved += carry.size();
        }
        return moved;
    }

    public static int count(World world, Functions.Kind kind) {
        return PlacedBlocksData.get(world).count(kind);
    }

    /** Trims every server world's sets down to the configured per-kind cap (safe to call always). */
    public static void trim(MinecraftServer server) {
        for (WorldServer world : server.worlds) {
            PlacedBlocksData data = PlacedBlocksData.get(world);
            for (Functions.Kind kind : Functions.Kind.values()) {
                data.trimTo(kind, RuntimeConfig.trackMax[kind.ordinal()]);
            }
        }
    }

    /** Clears tracking across every server world: one kind, or all when kindOrNull is null. */
    public static void resetWorlds(MinecraftServer server, Functions.Kind kindOrNull) {
        for (WorldServer world : server.worlds) {
            PlacedBlocksData data = PlacedBlocksData.get(world);
            if (kindOrNull == null) {
                data.resetAll();
            } else {
                data.reset(kindOrNull);
            }
        }
    }
}
