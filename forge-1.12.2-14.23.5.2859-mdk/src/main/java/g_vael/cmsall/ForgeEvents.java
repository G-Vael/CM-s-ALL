package g_vael.cmsall;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import g_vael.cmsall.core.ActivationState;
import g_vael.cmsall.core.BlockMoveContext;
import g_vael.cmsall.core.DespawnTracker;
import g_vael.cmsall.core.Functions;
import g_vael.cmsall.core.HarvestEngine;
import g_vael.cmsall.core.OriginPickup;
import g_vael.cmsall.core.PendingBreaks;
import g_vael.cmsall.core.PlacedBlocksTracker;
import g_vael.cmsall.core.ReplantQueue;
import g_vael.cmsall.net.CmsAllNetwork;

/** Server-side Forge event wiring. */
public final class ForgeEvents {

    // LOWEST so claim/protection mods veto the origin break before we start a chain.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        World world = event.getWorld();
        if (world.isRemote) {
            return;
        }
        EntityPlayer player = event.getPlayer();
        if (player instanceof EntityPlayerMP) {
            HarvestEngine.handleBreak(world, (EntityPlayerMP) player, event.getPos(), event.getState());
        }
    }

    // LOWEST so other mods' cancellations are seen first; BreakEvent fires for creative breaks too,
    // so this drops the placed-record the instant a tracked block is broken, including creative.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onTrackedBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || event.getWorld().isRemote) {
            return;
        }
        PlacedBlocksTracker.onBlockBroken(event.getWorld(), event.getPos(), event.getState());
    }

    @SubscribeEvent
    public void onPlace(BlockEvent.PlaceEvent event) {
        recordPlace(event.getWorld(), event.getPlayer(), event.getPos(), event.getPlacedBlock());
    }

    @SubscribeEvent
    public void onMultiPlace(BlockEvent.MultiPlaceEvent event) {
        recordPlace(event.getWorld(), event.getPlayer(), event.getPos(), event.getPlacedBlock());
    }

    private void recordPlace(World world, EntityPlayer player, BlockPos pos, IBlockState state) {
        if (world.isRemote || !(player instanceof EntityPlayerMP)) {
            return;
        }
        Functions.Kind k = Functions.trackedKind(state.getBlock());
        if (k != null) {
            PlacedBlocksTracker.record(world, k, pos, state);
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        BlockMoveContext.reset(); // backstop: clear any move flag leaked by a caller that forgot endMove()
        PendingBreaks.tick();
        ReplantQueue.tick();
        OriginPickup.tick();
        DespawnTracker.tick();
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server != null) {
            PlacedBlocksTracker.sweep(server);
        }
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            CmsAllNetwork.syncTo((EntityPlayerMP) event.player);
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) event.player;
            ActivationState.clear(player);
            CmsAllNetwork.forget(player);
        }
    }

    /** Drop all transient queues on shutdown (FML lifecycle event, dispatched from the @Mod class). */
    public static void onServerStopped(FMLServerStoppedEvent event) {
        PendingBreaks.clear();
        OriginPickup.clear();
        ReplantQueue.clear();
        DespawnTracker.clear();
        PlacedBlocksTracker.resetSweep();
    }
}
