package G_Vael.cmsall;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;

import G_Vael.cmsall.command.CmsAllCommand;
import G_Vael.cmsall.config.ConfigManager;
import G_Vael.cmsall.core.ActivationState;
import G_Vael.cmsall.core.BlockMoveContext;
import G_Vael.cmsall.core.DespawnTracker;
import G_Vael.cmsall.core.OriginPickup;
import G_Vael.cmsall.core.PendingBreaks;
import G_Vael.cmsall.core.PlacedBlocksTracker;
import G_Vael.cmsall.core.ReplantQueue;
import G_Vael.cmsall.net.CmsAllNetwork;

/** CM'sALL — common entry point (loader-independent). */
public final class CmsAll {
    public static final String MOD_ID = "cmsall";
    public static final Logger LOGGER = LoggerFactory.getLogger("CM'sALL");

    private CmsAll() {
    }

    /** Common setup shared by every loader. */
    public static void init() {
        LOGGER.info("[CM'sALL] common init");
        ConfigManager.load();
        LifecycleEvent.SERVER_STARTED.register(server -> {
            ConfigManager.apply();
            PlacedBlocksTracker.trim(server);
        });
        LifecycleEvent.SERVER_STOPPED.register(server -> {
            PendingBreaks.clear();
            OriginPickup.clear();
            ReplantQueue.clear();
            DespawnTracker.clear();
            PlacedBlocksTracker.resetSweep();
        });
        CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) ->
                CmsAllCommand.register(dispatcher));
        TickEvent.SERVER_POST.register(server -> {
            PendingBreaks.tick();
            ReplantQueue.tick();
            OriginPickup.tick();
            DespawnTracker.tick();
            PlacedBlocksTracker.sweep(server);
            BlockMoveContext.set(false); // backstop: a caller that leaked beginMove() can't suppress removal past one tick
        });
        CmsAllNetwork.register();
        PlayerEvent.PLAYER_JOIN.register(CmsAllNetwork::syncTo);
        PlayerEvent.PLAYER_QUIT.register(player -> {
            ActivationState.clear(player);
            CmsAllNetwork.forget(player);
        });
    }
}
