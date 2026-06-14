package G_Vael.cmsall.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Per-tick break scheduler. */
public final class PendingBreaks {

    private static final class Job {
        final World world;
        final EntityPlayerMP player;
        final BlockPos origin;
        final HarvestGroup group;
        final Deque<BlockPos> targets;
        final Deque<BlockPos> leaves;
        final Replanter replant;

        Job(World world, EntityPlayerMP player, BlockPos origin, HarvestGroup group,
            List<BlockPos> targets, List<BlockPos> leaves, Replanter replant) {
            this.world = world;
            this.player = player;
            this.origin = origin;
            this.group = group;
            this.targets = new ArrayDeque<BlockPos>(targets);
            this.leaves = new ArrayDeque<BlockPos>(leaves);
            this.replant = replant;
        }

        boolean done() {
            return targets.isEmpty() && leaves.isEmpty();
        }
    }

    private static final int MAX_JOBS_PER_PLAYER = 4;
    private static final List<Job> JOBS = new ArrayList<Job>();

    private PendingBreaks() {
    }

    public static void enqueue(World world, EntityPlayerMP player, BlockPos origin, HarvestGroup group,
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
        JOBS.add(new Job(world, player, origin, group, targets, leaves, replant));
    }

    /** Drops all scheduled work (server shutdown). */
    public static void clear() {
        JOBS.clear();
    }

    public static void tick() {
        if (JOBS.isEmpty()) {
            return;
        }
        // guard the whole loop: canPlayerBreak posts a real BreakEvent that would otherwise re-enter the engine.
        HarvestEngine.runGuarded(new Runnable() {
            @Override
            public void run() {
                tickGuarded();
            }
        });
    }

    private static void tickGuarded() {
        int remaining = Math.max(1, RuntimeConfig.perTickBudget);
        Iterator<Job> it = JOBS.iterator();
        while (it.hasNext() && remaining > 0) {
            Job job = it.next();
            if (!job.player.isEntityAlive()) {
                it.remove();
                continue;
            }
            ItemStack tool = job.player.getHeldItemMainhand();
            boolean stop = false;
            int share = Math.min(remaining, Math.max(1, remaining / JOBS.size()));
            while (share-- > 0 && !job.done()) {
                boolean isLeaf = job.targets.isEmpty();
                BlockPos pos = isLeaf ? job.leaves.poll() : job.targets.poll();
                if (!HarvestEngine.breakOne(job.world, job.player, pos,
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
