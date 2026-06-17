package g_vael.cmsall.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import g_vael.cmsall.core.PlacedBlocksTracker;

/** Drops a placed-block record the instant its block is removed or replaced (break/burn/explosion/decay, incl. creative). */
@Mixin(Level.class)
public abstract class LevelMixin {

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At("HEAD"))
    private void cmsall$untrackReplaced(BlockPos pos, BlockState newState, int flags, int recursionLeft,
            CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ServerLevel level) {
            PlacedBlocksTracker.onBlockReplaced(level, pos, newState);
        }
    }
}
