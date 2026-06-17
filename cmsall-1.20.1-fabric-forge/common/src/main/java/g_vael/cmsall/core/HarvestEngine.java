package g_vael.cmsall.core;

import java.util.ArrayList;
import java.util.List;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import g_vael.cmsall.platform.PlatformHooks;

/** Chain-break engine. */
public final class HarvestEngine {

    /** Re-entrancy guard: our own breaks must not retrigger the engine. */
    private static final ThreadLocal<Boolean> ACTIVE = new ThreadLocal<>();

    private static final int[][] OFFSETS_6 = {
            {1, 0, 0}, {-1, 0, 0},
            {0, 1, 0}, {0, -1, 0},
            {0, 0, 1}, {0, 0, -1}
    };

    private static final int[][] OFFSETS_26 = build26();

    /** max leaf-steps from a felled log when clearing a tree's canopy — big enough for a whole tree, small enough not to spill into a neighbouring tree whose leaves merely touch this one. */
    private static final int LEAF_REACH = 5;

    private HarvestEngine() {
    }

    /** Runs deferred breaking under the re-entrancy guard so our own breaks don't retrigger the engine. */
    static void runGuarded(Runnable work) {
        if (Boolean.TRUE.equals(ACTIVE.get())) {
            work.run();
            return;
        }
        ACTIVE.set(Boolean.TRUE);
        try {
            work.run();
        } finally {
            ACTIVE.remove();
        }
    }

    private static int[][] build26() {
        int[][] out = new int[26][3];
        int i = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    out[i][0] = dx;
                    out[i][1] = dy;
                    out[i][2] = dz;
                    i++;
                }
            }
        }
        return out;
    }

    /** Entry point called by each loader when a player breaks a block. */
    public static void handleBreak(ServerLevel level, ServerPlayer player, BlockPos origin, BlockState originState) {
        if (Boolean.TRUE.equals(ACTIVE.get())) {
            return;
        }
        if (!RuntimeConfig.enabled || player == null || level == null || originState == null) {
            return;
        }
        // function's list and return here — touching neither SavedData nor any allocation.
        Functions.Kind kind = Functions.functionFor(originState.getBlock());
        if (kind == null) {
            return;
        }
        HarvestGroup group = Functions.mechanics(kind);

        boolean tracked = RuntimeConfig.trackEnabled[kind.ordinal()];
        boolean originPlaced = tracked && PlacedBlocksTracker.isPlaced(level, kind, origin);

        if (player.isCreative() && !RuntimeConfig.allowInCreative) {
            return;
        }
        if (!ActivationState.isActive(player)) {
            return;
        }
        // the trigger source of truth — only listed tools chain (empty list = any tool).
        if (!Functions.tools(kind).isEmpty() && !Functions.toolAllowed(kind, player.getMainHandItem())) {
            return; // not one of this function's configured trigger tools
        }
        if (group.requireCorrectTool() && !player.hasCorrectToolForDrops(originState)) {
            return;
        }
        if (tracked && originPlaced) {
            return; // never start a chain from a player-placed block
        }
        // committed to the chain: drop the origin's placed-record only now (the PRE-break event is still cancelable).
        if (tracked) {
            PlacedBlocksTracker.remove(level, kind, origin);
        }
        ACTIVE.set(Boolean.TRUE);
        try {
            run(level, player, origin, originState, group, kind);
        } finally {
            ACTIVE.remove();
        }
    }

    private static void run(ServerLevel level, ServerPlayer player, BlockPos origin,
                            BlockState originState, HarvestGroup group, Functions.Kind kind) {
        ItemStack tool = player.getMainHandItem();
        int cap = Math.min(group.maxBlocks(), RuntimeConfig.globalMaxBlocks);

        if (RuntimeConfig.dropToInventory) {
            OriginPickup.schedule(level, player, origin);
        }

        List<BlockPos> targets = collectTargets(level, origin, originState, group, kind, cap);
        if (kind == Functions.Kind.CUT && RuntimeConfig.cutFromTop) {
            targets.sort((a, b) -> Integer.compare(b.getY(), a.getY())); // fell top-down so the tree doesn't float
        }

        List<BlockPos> leaves = List.of();
        if (group.mode() == HarvestMode.TREE && RuntimeConfig.cutBreakLeaves) {
            int leafBudget = cap - targets.size();
            if (leafBudget > 0) {
                leaves = collectLeaves(level, origin, targets, group, leafBudget);
            }
        }

        Replanter replant = Replanter.maybe(level, player, origin, originState, group, targets);

        if (RuntimeConfig.perTickBudget <= 0) {
            try {
                for (BlockPos pos : targets) {
                    if (!breakOne(level, player, pos, dropAt(origin, pos), tool, group, false, replant)) {
                        return; // tool exhausted / protected: stop entirely
                    }
                }
                for (BlockPos pos : leaves) {
                    if (!breakOne(level, player, pos, dropAt(origin, pos), tool, group, true, replant)) {
                        return;
                    }
                }
            } finally {
                ReplantQueue.schedule(replant); // plant next tick, once the base is cleared
            }
        } else {
            PendingBreaks.enqueue(level, player, origin, group, targets, leaves, replant);
        }
    }

    /** where drops appear: the origin block when gathering, otherwise the broken block. */
    static BlockPos dropAt(BlockPos origin, BlockPos broken) {
        return RuntimeConfig.gatherDrops ? origin : broken;
    }

    /** BFS the connected same-function blocks (excluding the origin) up to cap. */
    private static List<BlockPos> collectTargets(ServerLevel level, BlockPos origin, BlockState originState,
                                                 HarvestGroup group, Functions.Kind kind, int cap) {
        List<BlockPos> targets = new ArrayList<>();
        LongOpenHashSet visited = new LongOpenHashSet();
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
        long start = origin.asLong();
        visited.add(start);
        queue.enqueue(start);

        int ox = origin.getX(), oy = origin.getY(), oz = origin.getZ(), r = group.maxRadius();
        int[][] offsets = group.neighbours() == 6 ? OFFSETS_6 : OFFSETS_26;
        boolean tracked = RuntimeConfig.trackEnabled[kind.ordinal()];
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        while (!queue.isEmpty() && targets.size() < cap) {
            long packed = queue.dequeueLong();
            int px = BlockPos.getX(packed), py = BlockPos.getY(packed), pz = BlockPos.getZ(packed);
            for (int[] off : offsets) {
                int nx = px + off[0], ny = py + off[1], nz = pz + off[2];
                if (Math.max(Math.abs(nx - ox), Math.max(Math.abs(ny - oy), Math.abs(nz - oz))) > r) {
                    continue;
                }
                long np = BlockPos.asLong(nx, ny, nz);
                if (!visited.add(np)) {
                    continue;
                }
                cursor.set(nx, ny, nz);
                if (!level.isLoaded(cursor)) {
                    continue; // never force-load/generate a neighbouring chunk just to extend the chain (tick-stall/grief guard)
                }
                BlockState state = level.getBlockState(cursor);
                if (!Functions.blockInKind(kind, state.getBlock())) {
                    continue;
                }
                if (group.sameBlockOnly() && !state.is(originState.getBlock())) {
                    continue; // veins (MineAll): only the same ore type propagates
                }
                if (tracked && PlacedBlocksTracker.isPlaced(level, kind, cursor)) {
                    continue; // skip player-placed neighbours
                }
                if (!canBreak(level, cursor, state)) {
                    continue;
                }
                targets.add(cursor.immutable()); // only accepted positions become real BlockPos (<= cap)
                queue.enqueue(np);
                if (targets.size() >= cap) {
                    break;
                }
            }
        }
        return targets;
    }

    /** Flood-fill the connected natural leaves of the felled tree. */
    private static List<BlockPos> collectLeaves(ServerLevel level, BlockPos origin, List<BlockPos> logs,
                                                HarvestGroup group, int budget) {
        List<BlockPos> leaves = new ArrayList<>();
        LongOpenHashSet seen = new LongOpenHashSet();
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
        IntArrayFIFOQueue depths = new IntArrayFIFOQueue(); // leaf-steps from the nearest felled log

        long originKey = origin.asLong();
        seen.add(originKey);
        queue.enqueue(originKey);
        depths.enqueue(0);
        for (BlockPos log : logs) {
            long k = log.asLong();
            if (seen.add(k)) {
                queue.enqueue(k);
                depths.enqueue(0);
            }
        }

        int ox = origin.getX(), oy = origin.getY(), oz = origin.getZ(), r = group.maxRadius();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        while (!queue.isEmpty() && leaves.size() < budget) {
            long packed = queue.dequeueLong();
            int depth = depths.dequeueInt();
            if (depth >= LEAF_REACH) {
                continue; // far enough from the felled logs — don't spread into neighbouring canopies
            }
            int px = BlockPos.getX(packed), py = BlockPos.getY(packed), pz = BlockPos.getZ(packed);
            for (int[] off : OFFSETS_26) {
                int nx = px + off[0], ny = py + off[1], nz = pz + off[2];
                if (Math.max(Math.abs(nx - ox), Math.max(Math.abs(ny - oy), Math.abs(nz - oz))) > r) {
                    continue;
                }
                long np = BlockPos.asLong(nx, ny, nz);
                if (!seen.add(np)) {
                    continue;
                }
                cursor.set(nx, ny, nz);
                if (!level.isLoaded(cursor)) {
                    continue; // don't force-load/generate a chunk to flood leaves into it
                }
                BlockState state = level.getBlockState(cursor);
                if (!state.is(BlockTags.LEAVES)) {
                    continue; // not a leaf: don't flood through it
                }
                if (state.hasProperty(LeavesBlock.PERSISTENT) && state.getValue(LeavesBlock.PERSISTENT)) {
                    continue;
                }
                if (!canBreak(level, cursor, state)) {
                    continue;
                }
                leaves.add(cursor.immutable());
                queue.enqueue(np); // keep flooding through the connected canopy
                depths.enqueue(depth + 1);
                if (leaves.size() >= budget) {
                    return leaves;
                }
            }
        }
        return leaves;
    }

    /** Breaks one block applying drop control + tool/exhaustion costs. */
    static boolean breakOne(ServerLevel level, ServerPlayer player, BlockPos pos, BlockPos dropAt,
                            ItemStack tool, HarvestGroup group, boolean isLeaf, Replanter replant) {
        int durabilityCost = isLeaf ? 0 : (int) Math.max(0, Math.round(RuntimeConfig.durabilityPerBlock));

        int protectFloor = isLeaf ? durabilityCost : durabilityCost + 1;
        if (ActivationState.protectTool(player) && tool.isDamageableItem()) {
            int remaining = tool.getMaxDamage() - tool.getDamageValue();
            if (remaining <= protectFloor) {
                ToolFeedback.protectedStop(player, Math.max(1, remaining - 1));
                return false; // stop before the tool breaks
            }
        }

        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return true;
        }
        if (RuntimeConfig.respectProtectionPerBlock && !PlatformHooks.canPlayerBreak(level, pos, state, player)) {
            return true;
        }
        BlockEntity be = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;

        boolean drop;
        if (isLeaf) {
            drop = RuntimeConfig.cutLeafDrops;
        } else {
            drop = switch (group.dropMode()) {
                case NONE -> false;
                case CHANCE -> level.getRandom().nextFloat() < group.dropChance();
                case NORMAL -> true;
            };
        }

        if (RuntimeConfig.chainBreakEffects) {
            level.levelEvent(2001, pos, Block.getId(state));
        }
        level.removeBlock(pos, false);
        if (drop) {
            spawnDrops(level, pos, dropAt, state, be, player, tool, replant);
            state.spawnAfterBreak(level, pos, tool, true);
        }

        if (!isLeaf && RuntimeConfig.exhaustionPerBlock > 0) {
            player.causeFoodExhaustion((float) RuntimeConfig.exhaustionPerBlock);
        }
        if (durabilityCost > 0 && tool.isDamageableItem()) {
            tool.hurtAndBreak(durabilityCost, player, p -> p.broadcastBreakEvent(InteractionHand.MAIN_HAND));
            if (tool.isEmpty()) {
                return false; // tool broke
            }
        }
        return true;
    }

    /** Spawns enchantment-correct drops ( ) at dropAt (origin when gathering, ), registering despawn for configured items ( ). */
    private static void spawnDrops(ServerLevel level, BlockPos pos, BlockPos dropAt, BlockState state,
                                   BlockEntity be, ServerPlayer player, ItemStack tool, Replanter replant) {
        List<ItemStack> drops = Block.getDrops(state, level, pos, be, player, tool);
        if (drops.isEmpty()) {
            return;
        }
        if (replant != null) {
            for (ItemStack stack : drops) {
                replant.offerDrop(stack);
            }
        }
        if (RuntimeConfig.dropToInventory) {
            for (ItemStack stack : drops) {
                if (stack.isEmpty()) {
                    continue;
                }
                player.getInventory().add(stack);
                if (!stack.isEmpty()) {
                    player.drop(stack, false); // inventory full: drop the remainder
                }
            }
            return;
        }
        boolean despawn = RuntimeConfig.despawnEnabled && !RuntimeConfig.despawnItems.isEmpty();
        long deadline = despawn ? level.getGameTime() + (long) RuntimeConfig.despawnSeconds * 20L : 0L;
        double x = dropAt.getX() + 0.5;
        double y = dropAt.getY() + 0.5;
        double z = dropAt.getZ() + 0.5;
        for (ItemStack stack : drops) {
            if (stack.isEmpty()) {
                continue;
            }
            ItemEntity entity = new ItemEntity(level, x, y, z, stack);
            entity.setDefaultPickUpDelay();
            level.addFreshEntity(entity);
            if (despawn && RuntimeConfig.despawnItems.contains(stack.getItem())) {
                DespawnTracker.register(entity, deadline);
            }
        }
    }

    /** Protections that don't require loader events ( #1–#3). */
    private static boolean canBreak(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.isAir()) {
            return false;
        }
        if (HarvestGroups.isDenied(state)) {
            return false;
        }
        if (state.getDestroySpeed(level, pos) < 0) {
            return false;
        }
        return true;
    }
}
