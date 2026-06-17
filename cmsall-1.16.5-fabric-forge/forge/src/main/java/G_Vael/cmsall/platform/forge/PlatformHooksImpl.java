package g_vael.cmsall.platform.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent;

/** Forge impl of g_vael.cmsall.platform.PlatformHooks. */
public final class PlatformHooksImpl {

    private PlatformHooksImpl() {
    }

    public static boolean canPlayerBreak(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player) {
        // Per-block claim-veto channel: a cancelable BreakEvent per chain block. Other mods' BreakEvent
        // side effects (stats/quests/tool-xp/anti-cheat) will observe these synthetic events — a documented trade-off.
        BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(level, pos, state, player);
        MinecraftForge.EVENT_BUS.post(event);
        return !event.isCanceled();
    }
}
