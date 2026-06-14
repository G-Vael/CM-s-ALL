package G_Vael.cmsall.platform.fabric;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Fabric impl of G_Vael.cmsall.platform.PlatformHooks. */
public final class PlatformHooksImpl {

    private PlatformHooksImpl() {
    }

    public static boolean canPlayerBreak(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player) {
        BlockEntity be = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
        // BEFORE returns false if any listener (e.g. a claim mod) vetoes the break.
        return PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, player, pos, state, be);
    }
}
