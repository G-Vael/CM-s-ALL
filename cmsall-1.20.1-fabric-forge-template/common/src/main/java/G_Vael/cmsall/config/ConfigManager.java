package G_Vael.cmsall.config;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import dev.architectury.platform.Platform;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import G_Vael.cmsall.CmsAll;
import G_Vael.cmsall.core.ActivationMode;
import G_Vael.cmsall.core.Functions;
import G_Vael.cmsall.core.HarvestGroups;
import G_Vael.cmsall.core.RuntimeConfig;

/** Loads/saves the Layer-2 server config and pushes it into the live RuntimeConfig and the denylist. */
public final class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ServerConfig server = new ServerConfig();

    private ConfigManager() {
    }

    public static ServerConfig server() {
        return server;
    }

    /** Replaces the in-memory config wholesale (used by the edit receiver). */
    public static void replace(ServerConfig config) {
        if (config != null) {
            server = config;
        }
    }

    private static Path path() {
        return Platform.getConfigFolder().resolve("cmsall-server.json");
    }

    /** Loads from disk (creating defaults if absent) and applies to the live state. */
    public static void load() {
        Path p = path();
        try {
            if (Files.exists(p)) {
                try (Reader r = Files.newBufferedReader(p)) {
                    ServerConfig loaded = GSON.fromJson(r, ServerConfig.class);
                    if (loaded != null) {
                        server = loaded;
                    }
                }
            } else {
                save();
            }
        } catch (Exception e) {
            CmsAll.LOGGER.error("[CM'sALL] failed to load {}", p, e);
        }
        apply();
    }

    public static void save() {
        Path p = path();
        try {
            Files.createDirectories(p.getParent());
            try (Writer w = Files.newBufferedWriter(p)) {
                GSON.toJson(server, w);
            }
        } catch (Exception e) {
            CmsAll.LOGGER.error("[CM'sALL] failed to save {}", p, e);
        }
    }

    /** Pushes the in-memory config into the engine-facing state (no disk I/O). */
    public static void apply() {
        try {
            applyInternal();
        } catch (Exception e) {
            CmsAll.LOGGER.error("[CM'sALL] failed to apply config", e);
        }
    }

    private static void applyInternal() {
        server.editPermissionLevel = Math.min(4, Math.max(1, server.editPermissionLevel)); // never 0 (= everyone can edit)
        RuntimeConfig.enabled = server.enabled;
        RuntimeConfig.allowInCreative = server.allowInCreative;
        RuntimeConfig.defaultMode = ActivationMode.parse(server.defaultMode, ActivationMode.HOLD);
        RuntimeConfig.allowedModes = server.allowedModes == null
                ? Set.of("hold") : new HashSet<>(server.allowedModes);
        RuntimeConfig.globalMaxBlocks = Math.min(8192, Math.max(1, server.globalMaxBlocks));
        RuntimeConfig.perTickBudget = Math.min(8192, Math.max(0, server.perTickBudget));
        if (RuntimeConfig.perTickBudget == 0 && RuntimeConfig.globalMaxBlocks > 1024) {
            CmsAll.LOGGER.warn("[CM'sALL] perTickBudget=0 with globalMaxBlocks={} — one break can process that many blocks in a single tick; set perTickBudget>0 on busy/public servers.",
                    RuntimeConfig.globalMaxBlocks);
        }
        RuntimeConfig.durabilityPerBlock = clampDouble(server.durabilityPerBlock, 0.0, 1000.0, 1.0);
        RuntimeConfig.exhaustionPerBlock = clampDouble(server.exhaustionPerBlock, 0.0, 100.0, 0.005);
        RuntimeConfig.dropToInventory = server.dropToInventory;
        RuntimeConfig.gatherDrops = server.gatherDrops;
        RuntimeConfig.chainBreakEffects = server.chainBreakEffects;
        RuntimeConfig.cutBreakLeaves = server.cutBreakLeaves;
        RuntimeConfig.cutLeafDrops = server.cutLeafDrops;
        RuntimeConfig.cutFromTop = server.cutFromTop;
        RuntimeConfig.respectProtectionPerBlock = server.respectProtectionPerBlock;
        RuntimeConfig.recordCommandPlace = server.recordCommandPlace;

        RuntimeConfig.trackEnabled = new boolean[]{ server.trackMine, server.trackCut, server.trackDig };
        RuntimeConfig.trackMax = new int[]{ clamp(server.trackMineMax), clamp(server.trackCutMax), clamp(server.trackDigMax) };
        RuntimeConfig.trackEvict = !"stop".equalsIgnoreCase(server.trackOverflow);
        RuntimeConfig.anyTrackEnabled = server.trackMine || server.trackCut || server.trackDig;

        HarvestGroups.setExtraDenylist(resolveBlocks(server.denylist));

        // registries are populated.
        Set<Block> mineBlocks = resolveBlocks(server.mineBlocks);
        Set<Item> mineTools = resolveItems(server.mineTools);
        Functions.setBlocks(Functions.Kind.MINE, mineBlocks);
        Functions.setTools(Functions.Kind.MINE, mineTools);

        Set<Block> cutBlocks = resolveBlocks(server.cutBlocks);
        Set<Item> cutTools = resolveItems(server.cutTools);
        Functions.setBlocks(Functions.Kind.CUT, cutBlocks);
        Functions.setTools(Functions.Kind.CUT, cutTools);

        Set<Block> digBlocks = resolveBlocks(server.digBlocks);
        Set<Item> digTools = resolveItems(server.digTools);
        Functions.setBlocks(Functions.Kind.DIG, digBlocks);
        Functions.setTools(Functions.Kind.DIG, digTools);

        CmsAll.LOGGER.info("[CM'sALL] lists ready — Mine {} blocks/{} tools, Cut {} blocks/{} tools, Dig {} blocks/{} tools",
                mineBlocks.size(), mineTools.size(), cutBlocks.size(), cutTools.size(), digBlocks.size(), digTools.size());

        RuntimeConfig.despawnEnabled = server.despawnEnabled;
        RuntimeConfig.despawnSeconds = Math.max(1, server.despawnSeconds);
        RuntimeConfig.despawnItems = resolveItems(server.despawnItems);
    }

    private static double clampDouble(double v, double lo, double hi, double def) {
        if (Double.isNaN(v)) {
            return def;
        }
        return Math.min(hi, Math.max(lo, v));
    }

    private static int clamp(int x) {
        return Math.min(131072, Math.max(1, x));
    }

    private static Set<Block> resolveBlocks(Iterable<String> ids) {
        Set<Block> out = new HashSet<>();
        if (ids == null) {
            return out;
        }
        for (String id : ids) {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            if (rl != null) {
                BuiltInRegistries.BLOCK.getOptional(rl).ifPresent(out::add);
            }
        }
        return out;
    }

    private static Set<Item> resolveItems(Iterable<String> ids) {
        Set<Item> out = new HashSet<>();
        if (ids == null) {
            return out;
        }
        for (String id : ids) {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            if (rl != null) {
                BuiltInRegistries.ITEM.getOptional(rl).ifPresent(out::add);
            }
        }
        return out;
    }
}
