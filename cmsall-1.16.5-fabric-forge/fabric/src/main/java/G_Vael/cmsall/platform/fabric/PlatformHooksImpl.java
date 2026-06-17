package g_vael.cmsall.platform.fabric;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Fabric impl of g_vael.cmsall.platform.PlatformHooks. */
public final class PlatformHooksImpl {

    private PlatformHooksImpl() {
    }

    public static boolean canPlayerBreak(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player) {
        BlockEntity be = level.getBlockEntity(pos); // 1.16.5: no BlockState#hasBlockEntity; null if none
        // BEFORE returns false if any listener (e.g. a claim mod) vetoes the break.
        return PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, player, pos, state, be);
    }
}
