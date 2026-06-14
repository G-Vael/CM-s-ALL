package G_Vael.cmsall.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import G_Vael.cmsall.core.CommandPlaceContext;

/** Marks the thread while /setblock and /fill place a block, so the tracker can record command placements. */
@Mixin(BlockInput.class)
public class BlockInputMixin {

    @Redirect(method = "place(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;I)Z",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean cmsall$recordCommandPlace(ServerLevel level, BlockPos pos, BlockState state, int flags) {
        CommandPlaceContext.set(true);
        try {
            return level.setBlock(pos, state, flags);
        } finally {
            CommandPlaceContext.set(false);
        }
    }
}
