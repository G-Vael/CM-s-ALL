package G_Vael.cmsall.core;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Per-tick break scheduler. */
public final class PendingBreaks {

    private static final class Job {
        final ServerLevel level;
        final ServerPlayer player;
        final BlockPos origin;
        final HarvestGroup group;
        final Deque<BlockPos> targets;
        final Deque<BlockPos> leaves;
        final Replanter replant;

        Job(ServerLevel level, ServerPlayer player, BlockPos origin, HarvestGroup group,
            List<BlockPos> targets, List<BlockPos> leaves, Replanter replant) {
            this.level = level;
            this.player = player;
            this.origin = origin;
            this.group = group;
            this.targets = new ArrayDeque<>(targets);
            this.leaves = new ArrayDeque<>(leaves);
            this.replant = replant;
        }

        boolean done() {
            return targets.isEmpty() && leaves.isEmpty();
        }
    }

    /** cap concurrent jobs per player so one player can't pile up unbounded scheduled work. */
    private static final int MAX_JOBS_PER_PLAYER = 4;

    private static final List<Job> JOBS = new ArrayList<>();

    private PendingBreaks() {
    }

    public static void enqueue(ServerLevel level, ServerPlayer player, BlockPos origin, HarvestGroup group,
                               List<BlockPos> targets, List<BlockPos> leaves, Replanter replant) {
        if (targets.isEmpty() && leaves.isEmpty()) {
            return;
        }
        int mine = 0;
        for (Job j : JOBS) {
            if (j.player == player) {
                mine++;
            }
        }
        if (mine >= MAX_JOBS_PER_PLAYER) {
            return;
        }
        JOBS.add(new Job(level, player, origin, group, targets, leaves, replant));
    }

    /** Drops all scheduled work (server shutdown). */
    public static void clear() {
        JOBS.clear();
    }

    /** Called every server tick. */
    public static void tick() {
        if (JOBS.isEmpty()) {
            return;
        }
        // guard the whole loop: PlatformHooks.canPlayerBreak posts a real BreakEvent that would otherwise re-enter the engine.
        HarvestEngine.runGuarded(PendingBreaks::tickGuarded);
    }

    private static void tickGuarded() {
        int remaining = Math.max(1, RuntimeConfig.perTickBudget);
        Iterator<Job> it = JOBS.iterator();
        while (it.hasNext() && remaining > 0) {
            Job job = it.next();
            if (!job.player.isAlive() || job.player.hasDisconnected()) {
                it.remove();
                continue;
            }
            ItemStack tool = job.player.getMainHandItem();
            boolean stop = false;
            int share = Math.min(remaining, Math.max(1, remaining / JOBS.size()));
            while (share-- > 0 && !job.done()) {
                boolean isLeaf = job.targets.isEmpty();
                BlockPos pos = isLeaf ? job.leaves.poll() : job.targets.poll();
                if (!HarvestEngine.breakOne(job.level, job.player, pos,
                        HarvestEngine.dropAt(job.origin, pos), tool, job.group, isLeaf, job.replant)) {
                    stop = true;
                    break;
                }
                remaining--;
            }
            if (stop || job.done()) {
                it.remove();
                ReplantQueue.schedule(job.replant);
            }
        }
    }
}
