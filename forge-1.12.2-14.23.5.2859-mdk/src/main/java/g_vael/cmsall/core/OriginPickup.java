package g_vael.cmsall.core;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** drop-to-inventory completion for the ORIGIN block. */
public final class OriginPickup {

    private static final class Job {
        final World world;
        final EntityPlayerMP player;
        final BlockPos origin;

        Job(World world, EntityPlayerMP player, BlockPos origin) {
            this.world = world;
            this.player = player;
            this.origin = origin;
        }
    }

    private static final List<Job> JOBS = new ArrayList<Job>();

    private OriginPickup() {
    }

    static void schedule(World world, EntityPlayerMP player, BlockPos origin) {
        JOBS.add(new Job(world, player, origin.toImmutable()));
    }

    /** Drops all scheduled work (server shutdown). */
    public static void clear() {
        JOBS.clear();
    }

    public static void tick() {
        if (JOBS.isEmpty()) {
            return;
        }
        for (Job job : JOBS) {
            collect(job);
        }
        JOBS.clear();
    }

    private static void collect(Job job) {
        double x = job.origin.getX();
        double y = job.origin.getY();
        double z = job.origin.getZ();
        AxisAlignedBB box = new AxisAlignedBB(x - 0.5, y - 0.5, z - 0.5, x + 1.5, y + 1.5, z + 1.5);
        for (EntityItem entity : job.world.getEntitiesWithinAABB(EntityItem.class, box)) {
            if (entity.isDead || entity.ticksExisted > 3) {
                continue; // only the just-broken origin drop — never pre-existing ground items
            }
            if (entity.getThrower() != null) {
                continue; // a player-tossed item that happened to land here, not the origin block's own drop
            }
            ItemStack stack = entity.getItem().copy();
            job.player.inventory.addItemStackToInventory(stack);
            if (stack.isEmpty()) {
                entity.setDead();
            } else {
                entity.setItem(stack);
            }
        }
    }
}
