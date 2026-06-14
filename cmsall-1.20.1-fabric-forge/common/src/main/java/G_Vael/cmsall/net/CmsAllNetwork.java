package G_Vael.cmsall.net;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;

import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import G_Vael.cmsall.CmsAll;
import G_Vael.cmsall.config.ClientConfigView;
import G_Vael.cmsall.config.ConfigManager;
import G_Vael.cmsall.config.ServerConfig;
import G_Vael.cmsall.core.Functions;
import G_Vael.cmsall.core.PlacedBlocksTracker;

/** Server-authoritative config sync. */
public final class CmsAllNetwork {

    private static final Gson GSON = new Gson();

    /** minimum server-tick gap between processed edit requests, per player (~0.5s). */
    private static final int EDIT_COOLDOWN_TICKS = 10;
    private static final int RESYNC_COOLDOWN_TICKS = 20;
    /** bounded max bytes for the config-sync JSON — keeps the pre-auth allocation small while lifting the 32767-char functional limit. */
    private static final int MAX_CONFIG_BYTES = 262144;
    private static final Map<UUID, Integer> LAST_EDIT_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> LAST_RESYNC_TICK = new ConcurrentHashMap<>();

    public static final ResourceLocation CONFIG_SYNC = new ResourceLocation(CmsAll.MOD_ID, "config_sync");
    public static final ResourceLocation CONFIG_EDIT = new ResourceLocation(CmsAll.MOD_ID, "config_edit");
    public static final ResourceLocation CONFIG_RESYNC = new ResourceLocation(CmsAll.MOD_ID, "config_resync");

    private CmsAllNetwork() {
    }

    public static void register() {
        // S2C (config sync) is received on the client; architectury's registerS2CReceiver is @OnlyIn(CLIENT),
        // so calling it on a dedicated server throws NoSuchMethodError — register it client-side only.
        EnvExecutor.runInEnv(Env.CLIENT, () -> CmsAllNetwork::registerClient);

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, CONFIG_EDIT, (buf, context) -> {
            String json = buf.readUtf(MAX_CONFIG_BYTES);
            context.queue(() -> {
                if (!(context.getPlayer() instanceof ServerPlayer player)) {
                    return;
                }
                MinecraftServer server = player.getServer();
                if (server == null) {
                    return;
                }
                // worlds leave them below the op level).
                boolean authorized = server.isSingleplayerOwner(player.getGameProfile())
                        || player.hasPermissions(ConfigManager.server().editPermissionLevel);
                if (!authorized) {
                    return;
                }
                // light per-player throttle so an authorized client can't spam replace/save/sync.
                int tick = server.getTickCount();
                Integer last = LAST_EDIT_TICK.get(player.getUUID());
                if (last != null && tick - last < EDIT_COOLDOWN_TICKS) {
                    return;
                }
                LAST_EDIT_TICK.put(player.getUUID(), tick);
                ServerConfig edited;
                try {
                    edited = GSON.fromJson(json, ServerConfig.class);
                } catch (Exception e) {
                    CmsAll.LOGGER.warn("[CM'sALL] rejected malformed config edit from {}", player.getGameProfile().getName());
                    return;
                }
                if (edited == null) {
                    return;
                }
                ConfigManager.replace(edited);
                ConfigManager.save();
                ConfigManager.apply();
                PlacedBlocksTracker.trim(server);
                CmsAll.LOGGER.info("[CM'sALL] {} edited server config", player.getGameProfile().getName());
                syncToAll(server);
            });
        });

        // read-only: a client asks for a fresh config + counts snapshot (refreshes the tracking screen).
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, CONFIG_RESYNC, (buf, context) -> context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) {
                return;
            }
            MinecraftServer server = player.getServer();
            if (server == null) {
                return;
            }
            // per-player throttle: a tiny request triggers a full-config serialize + response, so cap the rate (anti-amplification).
            int tick = server.getTickCount();
            Integer last = LAST_RESYNC_TICK.get(player.getUUID());
            if (last != null && tick - last < RESYNC_COOLDOWN_TICKS) {
                return;
            }
            LAST_RESYNC_TICK.put(player.getUUID(), tick);
            syncTo(player);
        }));
    }

    /** client-only: the S2C config-sync receiver (registerS2CReceiver is @OnlyIn(CLIENT)). */
    private static void registerClient() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, CONFIG_SYNC, (buf, context) -> {
            String json = buf.readUtf(MAX_CONFIG_BYTES);
            context.queue(() -> {
                try {
                    SyncPayload payload = GSON.fromJson(json, SyncPayload.class);
                    if (payload != null && payload.cfg != null) {
                        ClientConfigView.set(payload.cfg, payload.counts);
                    }
                } catch (Exception e) {
                    CmsAll.LOGGER.warn("[CM'sALL] ignored malformed config sync from server");
                }
            });
        });
    }

    /** drop per-player state when they disconnect. */
    public static void forget(ServerPlayer player) {
        LAST_EDIT_TICK.remove(player.getUUID());
        LAST_RESYNC_TICK.remove(player.getUUID());
    }

    /** Client → server: submit an edited config for validation. */
    public static void sendEdit(ServerConfig edited) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUtf(GSON.toJson(edited), MAX_CONFIG_BYTES);
        NetworkManager.sendToServer(CONFIG_EDIT, buf);
    }

    /** Client → server: request a fresh config + counts snapshot (refreshes the tracking screen). */
    public static void requestSync() {
        NetworkManager.sendToServer(CONFIG_RESYNC, new FriendlyByteBuf(Unpooled.buffer()));
    }

    /** Sends the current config snapshot + this player's world counts (login). */
    public static void syncTo(ServerPlayer player) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUtf(payloadFor(player), MAX_CONFIG_BYTES);
        NetworkManager.sendToPlayer(player, CONFIG_SYNC, buf);
    }

    /** Broadcasts the current config snapshot to everyone online (each player gets their own world counts). */
    public static void syncToAll(MinecraftServer server) {
        if (server == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeUtf(payloadFor(player), MAX_CONFIG_BYTES);
            NetworkManager.sendToPlayer(player, CONFIG_SYNC, buf);
        }
    }

    /** Builds the wrapper JSON {cfg, counts} for one player's current world. */
    private static String payloadFor(ServerPlayer player) {
        SyncPayload payload = new SyncPayload();
        payload.cfg = ConfigManager.server();
        ServerLevel level = player.serverLevel();
        payload.counts = new int[]{
                PlacedBlocksTracker.count(level, Functions.Kind.MINE),
                PlacedBlocksTracker.count(level, Functions.Kind.CUT),
                PlacedBlocksTracker.count(level, Functions.Kind.DIG)
        };
        return GSON.toJson(payload);
    }

    /** Wire wrapper: server config plus the receiving player's per-kind tracked counts. */
    private static final class SyncPayload {
        ServerConfig cfg;
        int[] counts;
    }
}
