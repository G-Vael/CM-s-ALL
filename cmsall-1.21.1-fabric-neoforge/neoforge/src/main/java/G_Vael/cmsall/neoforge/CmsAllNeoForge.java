package G_Vael.cmsall.neoforge;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

import G_Vael.cmsall.CmsAll;
import G_Vael.cmsall.core.HarvestEngine;
import G_Vael.cmsall.neoforge.client.CmsAllNeoForgeClient;

@Mod(CmsAll.MOD_ID)
public final class CmsAllNeoForge {
    public CmsAllNeoForge() {
        CmsAll.init();

        // LOWEST so claim/protection mods veto the origin break before we start a chain.
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::onBlockBreak);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            CmsAllNeoForgeClient.init();
        }
    }

    private void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && event.getLevel() instanceof ServerLevel level) {
            HarvestEngine.handleBreak(level, player, event.getPos(), event.getState());
        }
    }
}
