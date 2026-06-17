package g_vael.cmsall.mixin;

import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.entity.item.ItemEntity;

/** Reads ItemEntity's private thrower UUID so OriginPickup only grabs block drops, not player-tossed items. */
@Mixin(ItemEntity.class)
public interface ItemEntityAccessor {
    @Accessor("thrower")
    UUID cmsall$getThrower();
}
