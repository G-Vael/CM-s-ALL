package g_vael.cmsall;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.shedaniel.architectury.event.events.CommandRegistrationEvent;
import me.shedaniel.architectury.event.events.LifecycleEvent;
import me.shedaniel.architectury.event.events.PlayerEvent;
import me.shedaniel.architectury.event.events.TickEvent;

import g_vael.cmsall.command.CmsAllCommand;
import g_vael.cmsall.config.ConfigManager;
import g_vael.cmsall.core.ActivationState;
import g_vael.cmsall.core.BlockMoveContext;
import g_vael.cmsall.core.DespawnTracker;
import g_vael.cmsall.core.OriginPickup;
import g_vael.cmsall.core.PendingBreaks;
import g_vael.cmsall.core.PlacedBlocksTracker;
import g_vael.cmsall.core.ReplantQueue;
import g_vael.cmsall.net.CmsAllNetwork;

/** CM'sALL — common entry point (loader-independent). */
public final class CmsAll {
    public static final String MOD_ID = "cmsall";
    public static final Logger LOGGER = LogManager.getLogger("CM'sALL");

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
        CommandRegistrationEvent.EVENT.register((dispatcher, selection) ->
                CmsAllCommand.register(dispatcher));
        TickEvent.SERVER_POST.register(server -> {
            PendingBreaks.tick();
            ReplantQueue.tick();
            OriginPickup.tick();
            DespawnTracker.tick();
            PlacedBlocksTracker.sweep(server);
            BlockMoveContext.reset(); // backstop: a caller that leaked beginMove() can't suppress removal past one tick
        });
        CmsAllNetwork.register();
        PlayerEvent.PLAYER_JOIN.register(CmsAllNetwork::syncTo);
        PlayerEvent.PLAYER_QUIT.register(player -> {
            ActivationState.clear(player);
            CmsAllNetwork.forget(player);
        });
    }
}
