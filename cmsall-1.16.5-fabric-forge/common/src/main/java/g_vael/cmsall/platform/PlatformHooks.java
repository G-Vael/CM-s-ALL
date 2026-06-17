package g_vael.cmsall.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

/** Loader-specific hooks resolved at build time by Architectury's @ExpectPlatform transformer. */
public final class PlatformHooks {

    private PlatformHooks() {
    }

    /** Fires the loader's native block-break event so claim/protection mods can veto a single block. */
    @ExpectPlatform
    public static boolean canPlayerBreak(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player) {
        throw new AssertionError("@ExpectPlatform stub — replaced per loader at build time");
    }
}
