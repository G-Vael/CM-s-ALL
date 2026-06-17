package g_vael.cmsall.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent;

/** Chain-break engine for 1.12.2 Forge. */
public final class HarvestEngine {

    private static final ThreadLocal<Boolean> ACTIVE = new ThreadLocal<Boolean>();

    private static final int[][] OFFSETS_6 = {
            {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}
    };
    private static final int[][] OFFSETS_26 = build26();

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

    /** Entry point called by the block-break event handler. */
    public static void handleBreak(World world, EntityPlayerMP player, BlockPos origin, IBlockState originState) {
        if (Boolean.TRUE.equals(ACTIVE.get())) {
            return;
        }
        if (!RuntimeConfig.enabled || player == null || world == null || originState == null) {
            return;
        }
        Functions.Kind kind = Functions.functionFor(originState.getBlock());
        if (kind == null) {
            return;
        }
        HarvestGroup group = Functions.mechanics(kind);

        boolean tracked = RuntimeConfig.trackEnabled[kind.ordinal()];
        boolean originPlaced = tracked && PlacedBlocksTracker.isPlaced(world, kind, origin);

        if (player.isCreative() && !RuntimeConfig.allowInCreative) {
            return;
        }
        if (!ActivationState.isActive(player)) {
            return;
        }
        if (!Functions.tools(kind).isEmpty() && !Functions.toolAllowed(kind, player.getHeldItemMainhand())) {
            return;
        }
        if (group.requireCorrectTool() && !originState.getBlock().canHarvestBlock(world, origin, player)) {
            return;
        }
        if (tracked && originPlaced) {
            return; // never start a chain from a player-placed block
        }
        // committed to the chain: drop the origin's placed-record only now (the PRE-break event is still cancelable).
        if (tracked) {
            PlacedBlocksTracker.remove(world, kind, origin);
        }
        ACTIVE.set(Boolean.TRUE);
        try {
            run(world, player, origin, originState, group, kind);
        } finally {
            ACTIVE.remove();
        }
    }

    private static void run(World world, EntityPlayerMP player, BlockPos origin, IBlockState originState,
                            HarvestGroup group, Functions.Kind kind) {
        ItemStack tool = player.getHeldItemMainhand();
        int cap = Math.min(group.maxBlocks(), RuntimeConfig.globalMaxBlocks);

        if (RuntimeConfig.dropToInventory) {
            OriginPickup.schedule(world, player, origin);
        }

        List<BlockPos> targets = collectTargets(world, origin, originState, group, kind, cap);
        if (kind == Functions.Kind.CUT && RuntimeConfig.cutFromTop) {
            targets.sort((a, b) -> Integer.compare(b.getY(), a.getY())); // fell top-down so the tree doesn't float
        }

        List<BlockPos> leaves = Collections.emptyList();
        if (group.mode() == HarvestMode.TREE && RuntimeConfig.cutBreakLeaves) {
            int leafBudget = cap - targets.size();
            if (leafBudget > 0) {
                leaves = collectLeaves(world, origin, targets, group, leafBudget);
            }
        }

        Replanter replant = Replanter.maybe(world, player, origin, originState, group, targets);

        if (RuntimeConfig.perTickBudget <= 0) {
            try {
                for (BlockPos pos : targets) {
                    if (!breakOne(world, player, pos, dropAt(origin, pos), tool, group, false, replant)) {
                        return;
                    }
                }
                for (BlockPos pos : leaves) {
                    if (!breakOne(world, player, pos, dropAt(origin, pos), tool, group, true, replant)) {
                        return;
                    }
                }
            } finally {
                ReplantQueue.schedule(replant);
            }
        } else {
            PendingBreaks.enqueue(world, player, origin, group, targets, leaves, replant);
        }
    }

    static BlockPos dropAt(BlockPos origin, BlockPos broken) {
        return RuntimeConfig.gatherDrops ? origin : broken;
    }

    private static List<BlockPos> collectTargets(World world, BlockPos origin, IBlockState originState,
                                                 HarvestGroup group, Functions.Kind kind, int cap) {
        List<BlockPos> targets = new ArrayList<BlockPos>();
        Set<Long> visited = new HashSet<Long>();
        Deque<Long> queue = new ArrayDeque<Long>();
        visited.add(Long.valueOf(origin.toLong()));
        queue.add(Long.valueOf(origin.toLong()));

        int ox = origin.getX(), oy = origin.getY(), oz = origin.getZ(), r = group.maxRadius();
        int[][] offsets = group.neighbours() == 6 ? OFFSETS_6 : OFFSETS_26;
        boolean tracked = RuntimeConfig.trackEnabled[kind.ordinal()];
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        while (!queue.isEmpty() && targets.size() < cap) {
            BlockPos cur = BlockPos.fromLong(queue.poll().longValue());
            int px = cur.getX(), py = cur.getY(), pz = cur.getZ();
            for (int[] off : offsets) {
                int nx = px + off[0], ny = py + off[1], nz = pz + off[2];
                if (Math.max(Math.abs(nx - ox), Math.max(Math.abs(ny - oy), Math.abs(nz - oz))) > r) {
                    continue;
                }
                cursor.setPos(nx, ny, nz);
                Long np = Long.valueOf(cursor.toLong());
                if (!visited.add(np)) {
                    continue;
                }
                if (!world.isBlockLoaded(cursor)) {
                    continue; // never force-load/generate a chunk to extend the chain
                }
                IBlockState state = world.getBlockState(cursor);
                if (!Functions.blockInKind(kind, state.getBlock())) {
                    continue;
                }
                if (group.sameBlockOnly() && state.getBlock() != originState.getBlock()) {
                    continue;
                }
                if (tracked && PlacedBlocksTracker.isPlaced(world, kind, cursor)) {
                    continue; // skip player-placed neighbours
                }
                if (!canBreak(world, cursor, state)) {
                    continue;
                }
                targets.add(cursor.toImmutable());
                queue.add(np);
                if (targets.size() >= cap) {
                    break;
                }
            }
        }
        return targets;
    }

    private static List<BlockPos> collectLeaves(World world, BlockPos origin, List<BlockPos> logs,
                                                HarvestGroup group, int budget) {
        List<BlockPos> leaves = new ArrayList<BlockPos>();
        Set<Long> seen = new HashSet<Long>();
        Deque<Long> queue = new ArrayDeque<Long>();
        Deque<Integer> depths = new ArrayDeque<Integer>();

        seen.add(Long.valueOf(origin.toLong()));
        queue.add(Long.valueOf(origin.toLong()));
        depths.add(Integer.valueOf(0));
        for (BlockPos log : logs) {
            Long k = Long.valueOf(log.toLong());
            if (seen.add(k)) {
                queue.add(k);
                depths.add(Integer.valueOf(0));
            }
        }

        int ox = origin.getX(), oy = origin.getY(), oz = origin.getZ(), r = group.maxRadius();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        while (!queue.isEmpty() && leaves.size() < budget) {
            BlockPos cur = BlockPos.fromLong(queue.poll().longValue());
            int depth = depths.poll().intValue();
            if (depth >= LEAF_REACH) {
                continue;
            }
            int px = cur.getX(), py = cur.getY(), pz = cur.getZ();
            for (int[] off : OFFSETS_26) {
                int nx = px + off[0], ny = py + off[1], nz = pz + off[2];
                if (Math.max(Math.abs(nx - ox), Math.max(Math.abs(ny - oy), Math.abs(nz - oz))) > r) {
                    continue;
                }
                cursor.setPos(nx, ny, nz);
                Long np = Long.valueOf(cursor.toLong());
                if (!seen.add(np)) {
                    continue;
                }
                if (!world.isBlockLoaded(cursor)) {
                    continue; // never force-load/generate a chunk to extend the leaf search
                }
                IBlockState state = world.getBlockState(cursor);
                if (state.getMaterial() != Material.LEAVES) {
                    continue;
                }
                if (state.getBlock() instanceof BlockLeaves
                        && !state.getValue(BlockLeaves.DECAYABLE).booleanValue()) {
                    continue;
                }
                if (!canBreak(world, cursor, state)) {
                    continue;
                }
                leaves.add(cursor.toImmutable());
                queue.add(np);
                depths.add(Integer.valueOf(depth + 1));
                if (leaves.size() >= budget) {
                    return leaves;
                }
            }
        }
        return leaves;
    }

    static boolean breakOne(World world, EntityPlayerMP player, BlockPos pos, BlockPos dropAt,
                            ItemStack tool, HarvestGroup group, boolean isLeaf, Replanter replant) {
        int durabilityCost = isLeaf ? 0 : (int) Math.max(0, Math.round(RuntimeConfig.durabilityPerBlock));

        int protectFloor = isLeaf ? durabilityCost : durabilityCost + 1;
        if (ActivationState.protectTool(player) && tool.isItemStackDamageable()) {
            int remaining = tool.getMaxDamage() - tool.getItemDamage();
            if (remaining <= protectFloor) {
                ToolFeedback.protectedStop(player, Math.max(1, remaining - 1));
                return false;
            }
        }

        if (world.isAirBlock(pos)) {
            return true;
        }
        IBlockState state = world.getBlockState(pos);
        if (RuntimeConfig.respectProtectionPerBlock && !canPlayerBreak(world, pos, state, player)) {
            return true; // claim/protection mod vetoed this block; skip it, keep the chain going
        }

        boolean drop;
        if (isLeaf) {
            drop = RuntimeConfig.cutLeafDrops;
        } else {
            switch (group.dropMode()) {
                case NONE:
                    drop = false;
                    break;
                case CHANCE:
                    drop = world.rand.nextFloat() < group.dropChance();
                    break;
                case NORMAL:
                default:
                    drop = true;
                    break;
            }
        }

        int exp = drop ? Drops.expFor(world, pos, state, player, tool) : 0;
        List<ItemStack> drops = drop ? Drops.compute(world, pos, state, player, tool)
                : Collections.<ItemStack>emptyList();

        if (RuntimeConfig.chainBreakEffects) {
            world.playEvent(2001, pos, Block.getStateId(state));
        }
        world.setBlockToAir(pos);
        if (drop) {
            if (replant != null) {
                for (ItemStack s : drops) {
                    replant.offerDrop(s);
                }
            }
            placeDrops(world, dropAt, drops, player);
            if (exp > 0) {
                state.getBlock().dropXpOnBlockBreak(world, pos, exp);
            }
        }

        if (!isLeaf && RuntimeConfig.exhaustionPerBlock > 0) {
            player.addExhaustion((float) RuntimeConfig.exhaustionPerBlock);
        }
        if (durabilityCost > 0 && tool.isItemStackDamageable()) {
            tool.damageItem(durabilityCost, player);
            if (tool.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static void placeDrops(World world, BlockPos dropAt, List<ItemStack> drops, EntityPlayerMP player) {
        if (drops.isEmpty()) {
            return;
        }
        if (RuntimeConfig.dropToInventory) {
            for (ItemStack stack : drops) {
                if (stack.isEmpty()) {
                    continue;
                }
                player.inventory.addItemStackToInventory(stack);
                if (!stack.isEmpty()) {
                    player.dropItem(stack, false);
                }
            }
            return;
        }
        boolean despawn = RuntimeConfig.despawnEnabled && !RuntimeConfig.despawnItems.isEmpty();
        long deadline = despawn ? world.getTotalWorldTime() + (long) RuntimeConfig.despawnSeconds * 20L : 0L;
        double x = dropAt.getX() + 0.5;
        double y = dropAt.getY() + 0.5;
        double z = dropAt.getZ() + 0.5;
        for (ItemStack stack : drops) {
            if (stack.isEmpty()) {
                continue;
            }
            EntityItem entity = new EntityItem(world, x, y, z, stack);
            entity.setDefaultPickupDelay();
            world.spawnEntity(entity);
            if (despawn && RuntimeConfig.despawnItems.contains(stack.getItem())) {
                DespawnTracker.register(entity, deadline);
            }
        }
    }

    /** #4 — fire the loader break event so claim/protection mods can veto a single block. */
    private static boolean canPlayerBreak(World world, BlockPos pos, IBlockState state, EntityPlayerMP player) {
        // Per-block claim-veto channel: a cancelable BreakEvent per chain block. Other mods' BreakEvent
        // side effects (stats/quests/tool-xp/anti-cheat) will observe these synthetic events — a documented trade-off.
        BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(world, pos, state, player);
        MinecraftForge.EVENT_BUS.post(event);
        return !event.isCanceled();
    }

    private static boolean canBreak(World world, BlockPos pos, IBlockState state) {
        if (world.isAirBlock(pos)) {
            return false;
        }
        if (HarvestGroups.isDenied(state)) {
            return false;
        }
        if (state.getBlock().getBlockHardness(state, world, pos) < 0) {
            return false; // unbreakable (bedrock, barrier, …)
        }
        return true;
    }
}
