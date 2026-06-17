package g_vael.cmsall.net;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.IThreadListener;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

import g_vael.cmsall.CmsAll;
import g_vael.cmsall.config.ClientConfigView;
import g_vael.cmsall.config.ConfigManager;
import g_vael.cmsall.config.ServerConfig;
import g_vael.cmsall.core.Functions;
import g_vael.cmsall.core.PlacedBlocksTracker;

/** Server-authoritative config sync over a Forge SimpleNetworkWrapper. */
public final class CmsAllNetwork {

    private static final Gson GSON = new Gson();
    private static final int EDIT_COOLDOWN_TICKS = 10;
    private static final int RESYNC_COOLDOWN_TICKS = 20;
    private static final Map<UUID, Integer> LAST_EDIT_TICK = new ConcurrentHashMap<UUID, Integer>();
    private static final Map<UUID, Integer> LAST_RESYNC_TICK = new ConcurrentHashMap<UUID, Integer>();

    public static SimpleNetworkWrapper CHANNEL;

    private CmsAllNetwork() {
    }

    public static void register() {
        CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(CmsAll.MOD_ID);
        CHANNEL.registerMessage(SyncHandler.class, ConfigSyncMessage.class, 0, Side.CLIENT);
        CHANNEL.registerMessage(EditHandler.class, ConfigEditMessage.class, 1, Side.SERVER);
        CHANNEL.registerMessage(ConfigRequestMessage.Handler.class, ConfigRequestMessage.class, 2, Side.SERVER);
    }

    public static void syncTo(EntityPlayerMP player) {
        CHANNEL.sendTo(new ConfigSyncMessage(payloadFor(player)), player);
    }

    public static void syncToAll(MinecraftServer server) {
        if (server == null) {
            return;
        }
        for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
            CHANNEL.sendTo(new ConfigSyncMessage(payloadFor(player)), player);
        }
    }

    /** Builds the wrapper JSON {cfg, counts} for one player's current world. */
    private static String payloadFor(EntityPlayerMP player) {
        SyncPayload payload = new SyncPayload();
        payload.cfg = ConfigManager.server();
        payload.counts = new int[]{
                PlacedBlocksTracker.count(player.world, Functions.Kind.MINE),
                PlacedBlocksTracker.count(player.world, Functions.Kind.CUT),
                PlacedBlocksTracker.count(player.world, Functions.Kind.DIG)
        };
        return GSON.toJson(payload);
    }

    /** Wire wrapper: server config plus the receiving player's per-kind tracked counts. */
    private static final class SyncPayload {
        ServerConfig cfg;
        int[] counts;
    }

    /** Client → server: submit an edited config for validation (called from the GUI). */
    public static void sendEdit(ServerConfig edited) {
        CHANNEL.sendToServer(new ConfigEditMessage(GSON.toJson(edited)));
    }

    /** Client → server: request a fresh config + counts snapshot (refreshes the tracking screen). */
    public static void requestSync() {
        CHANNEL.sendToServer(new ConfigRequestMessage());
    }

    public static void forget(EntityPlayerMP player) {
        LAST_EDIT_TICK.remove(player.getUniqueID());
        LAST_RESYNC_TICK.remove(player.getUniqueID());
    }

    /** per-player throttle: a tiny resync request triggers a full-config serialize + response, so cap the rate (anti-amplification). */
    static boolean allowResync(MinecraftServer server, EntityPlayerMP player) {
        if (server == null) {
            return false;
        }
        int tick = server.getTickCounter();
        Integer last = LAST_RESYNC_TICK.get(player.getUniqueID());
        if (last != null && tick - last.intValue() < RESYNC_COOLDOWN_TICKS) {
            return false;
        }
        LAST_RESYNC_TICK.put(player.getUniqueID(), Integer.valueOf(tick));
        return true;
    }

    private static boolean isOwner(MinecraftServer server, EntityPlayerMP player) {
        return server.isSinglePlayer() && server.getServerOwner() != null
                && server.getServerOwner().equalsIgnoreCase(player.getGameProfile().getName());
    }

    public static final class SyncHandler implements IMessageHandler<ConfigSyncMessage, IMessage> {
        @Override
        public IMessage onMessage(ConfigSyncMessage message, MessageContext ctx) {
            final String json = message.json;
            IThreadListener thread = FMLCommonHandler.instance().getWorldThread(ctx.netHandler);
            thread.addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    try {
                        SyncPayload payload = GSON.fromJson(json, SyncPayload.class);
                        if (payload != null && payload.cfg != null) {
                            ClientConfigView.set(payload.cfg, payload.counts);
                        }
                    } catch (Exception e) {
                        CmsAll.LOGGER.warn("[CM'sALL] ignored malformed config sync from server");
                    }
                }
            });
            return null;
        }
    }

    public static final class EditHandler implements IMessageHandler<ConfigEditMessage, IMessage> {
        @Override
        public IMessage onMessage(ConfigEditMessage message, MessageContext ctx) {
            final String json = message.json;
            final EntityPlayerMP player = ctx.getServerHandler().player;
            IThreadListener thread = FMLCommonHandler.instance().getWorldThread(ctx.netHandler);
            thread.addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    MinecraftServer server = player.getServer();
                    if (server == null) {
                        return;
                    }
                    boolean authorized = isOwner(server, player)
                            || player.canUseCommand(ConfigManager.server().editPermissionLevel, "cmsall");
                    if (!authorized) {
                        return;
                    }
                    // light per-player throttle so an authorized client can't spam replace/save/sync.
                    int tick = server.getTickCounter();
                    Integer last = LAST_EDIT_TICK.get(player.getUniqueID());
                    if (last != null && tick - last.intValue() < EDIT_COOLDOWN_TICKS) {
                        return;
                    }
                    LAST_EDIT_TICK.put(player.getUniqueID(), Integer.valueOf(tick));
                    ServerConfig edited;
                    try {
                        edited = GSON.fromJson(json, ServerConfig.class);
                    } catch (Exception e) {
                        return;
                    }
                    if (edited == null) {
                        return;
                    }
                    ConfigManager.replace(edited);
                    ConfigManager.apply();
                    ConfigManager.save();
                    PlacedBlocksTracker.trim(server);
                    CmsAll.LOGGER.info("[CM'sALL] " + player.getName() + " edited server config");
                    syncToAll(server);
                }
            });
            return null;
        }
    }
}
