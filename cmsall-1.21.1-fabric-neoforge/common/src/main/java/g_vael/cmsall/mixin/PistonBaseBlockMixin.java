package g_vael.cmsall.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonStructureResolver;

import g_vael.cmsall.core.BlockMoveContext;
import g_vael.cmsall.core.PlacedBlocksTracker;
import g_vael.cmsall.core.RuntimeConfig;

/** Carries placed-block records along with blocks a piston pushes or pulls, so protection follows the block. */
@Mixin(PistonBaseBlock.class)
public abstract class PistonBaseBlockMixin {

    private static final String MOVE_BLOCKS =
        "moveBlocks(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Z)Z";

    /** Suppress record removal for every setBlock this whole move makes (head clear, transient MOVING_PISTON, final landing). */
    @Inject(method = MOVE_BLOCKS, at = @At("HEAD"))
    private void cmsall$pistonHead(Level level, BlockPos pos, Direction facing, boolean extending,
            CallbackInfoReturnable<Boolean> cir) {
        if (!(level instanceof ServerLevel)) {
            return;
        }
        if (!RuntimeConfig.anyTrackEnabled) {
            return; // nothing tracked anywhere — skip
        }
        BlockMoveContext.push();
    }

    /**
     * Relocate records right AFTER vanilla's {@code resolve()} call — not at HEAD.
     *
     * <p>For a RETRACT, moveBlocks first clears the piston head at the front position and only then resolves the
     * structure to pull. Resolving at HEAD (head still present) makes the resolver report the pull as blocked, so the
     * records were left stranded at the source and the pulled block ended up untracked. Re-resolving here, against the
     * exact world state vanilla resolves against (head already cleared, blocks still at their source positions), yields
     * the correct pushed/pulled set for both extend and retract — and needs no @Local capture, so it ports to old Mixin.
     */
    @Inject(method = MOVE_BLOCKS,
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/world/level/block/piston/PistonStructureResolver;resolve()Z",
                     shift = At.Shift.AFTER))
    private void cmsall$pistonResolved(Level level, BlockPos pos, Direction facing, boolean extending,
            CallbackInfoReturnable<Boolean> cir) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!RuntimeConfig.anyTrackEnabled) {
            return;
        }
        PistonStructureResolver resolver = new PistonStructureResolver(level, pos, facing, extending);
        if (!resolver.resolve()) {
            return;
        }
        Direction moveDir = extending ? facing : facing.getOpposite();
        PlacedBlocksTracker.onPistonMove(serverLevel, resolver.getToPush(), moveDir);
    }

    @Inject(method = MOVE_BLOCKS, at = @At("RETURN"))
    private void cmsall$pistonReturn(Level level, BlockPos pos, Direction facing, boolean extending,
            CallbackInfoReturnable<Boolean> cir) {
        BlockMoveContext.pop();
    }
}
