package g_vael.cmsall.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import g_vael.cmsall.CmsAll;
import g_vael.cmsall.core.HarvestEngine;

public final class CmsAllFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        CmsAll.init();

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world instanceof ServerLevel && player instanceof ServerPlayer) {
                HarvestEngine.handleBreak((ServerLevel) world, (ServerPlayer) player, pos, state);
            }
        });
    }
}
