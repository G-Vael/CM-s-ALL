package g_vael.cmsall.forge;

import me.shedaniel.architectury.platform.forge.EventBuses;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

import g_vael.cmsall.CmsAll;
import g_vael.cmsall.core.HarvestEngine;
import g_vael.cmsall.forge.client.CmsAllForgeClient;

/** Forge entry point (1.16.5). */
@Mod(CmsAll.MOD_ID)
public final class CmsAllForge {
    public CmsAllForge() {
        EventBuses.registerModEventBus(CmsAll.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        CmsAll.init();

        // LOWEST so claim/protection mods veto the origin break before we start a chain.
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::onBlockBreak);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            CmsAllForgeClient.init();
        }
    }

    private void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer && event.getWorld() instanceof ServerLevel) {
            HarvestEngine.handleBreak((ServerLevel) event.getWorld(), (ServerPlayer) event.getPlayer(),
                    event.getPos(), event.getState());
        }
    }
}
