package g_vael.cmsall.core;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import g_vael.cmsall.mixin.ItemEntityAccessor;

/** drop-to-inventory completion. */
public final class OriginPickup {

    private static final class Job {
        final ServerLevel level;
        final ServerPlayer player;
        final BlockPos origin;

        Job(ServerLevel level, ServerPlayer player, BlockPos origin) {
            this.level = level;
            this.player = player;
            this.origin = origin;
        }
    }

    private static final List<Job> JOBS = new ArrayList<>();

    private OriginPickup() {
    }

    static void schedule(ServerLevel level, ServerPlayer player, BlockPos origin) {
        JOBS.add(new Job(level, player, origin.immutable()));
    }

    /** Drops all scheduled work (server shutdown). */
    public static void clear() {
        JOBS.clear();
    }

    /** Called every server tick, after this tick's breaks (and their vanilla drops) have landed. */
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
        AABB box = new AABB(x - 0.5, y - 0.5, z - 0.5, x + 1.5, y + 1.5, z + 1.5);
        for (ItemEntity entity : job.level.getEntitiesOfClass(ItemEntity.class, box)) {
            if (entity.removed || entity.tickCount > 3) {
                continue; // only the just-broken origin drop — never pre-existing ground items
            }
            if (((ItemEntityAccessor) entity).cmsall$getThrower() != null) {
                continue; // a player-tossed item that happened to land here, not the origin block's own drop
            }
            ItemStack stack = entity.getItem().copy();
            job.player.inventory.add(stack);
            if (stack.isEmpty()) {
                entity.remove();
            } else {
                entity.setItem(stack); // inventory full: leave the remainder on the ground
            }
        }
    }
}
