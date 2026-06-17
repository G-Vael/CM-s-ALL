package g_vael.cmsall.config;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import g_vael.cmsall.CmsAll;
import g_vael.cmsall.core.ActivationMode;
import g_vael.cmsall.core.Functions;
import g_vael.cmsall.core.HarvestGroups;
import g_vael.cmsall.core.RuntimeConfig;

/** Loads/saves the Layer-2 server config and pushes it into the live RuntimeConfig. */
public final class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ServerConfig server = new ServerConfig();
    private static File configDir;

    private ConfigManager() {
    }

    public static void setConfigDir(File dir) {
        configDir = dir;
    }

    public static ServerConfig server() {
        return server;
    }

    public static void replace(ServerConfig config) {
        if (config != null) {
            server = config;
        }
    }

    private static File file() {
        return new File(configDir, "cmsall-server.json");
    }

    public static void load() {
        File p = file();
        try {
            if (p.exists()) {
                Reader r = new FileReader(p);
                try {
                    ServerConfig loaded = GSON.fromJson(r, ServerConfig.class);
                    if (loaded != null) {
                        server = loaded;
                    }
                } finally {
                    r.close();
                }
            } else {
                save();
            }
        } catch (Exception e) {
            CmsAll.LOGGER.error("[CM'sALL] failed to load " + p, e);
        }
        apply();
    }

    public static void save() {
        File p = file();
        try {
            if (p.getParentFile() != null) {
                p.getParentFile().mkdirs();
            }
            Writer w = new FileWriter(p);
            try {
                GSON.toJson(server, w);
            } finally {
                w.close();
            }
        } catch (Exception e) {
            CmsAll.LOGGER.error("[CM'sALL] failed to save " + p, e);
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
                ? Collections.singleton("hold") : new HashSet<String>(server.allowedModes);
        RuntimeConfig.globalMaxBlocks = Math.min(8192, Math.max(1, server.globalMaxBlocks));
        RuntimeConfig.perTickBudget = Math.min(8192, Math.max(0, server.perTickBudget));
        if (RuntimeConfig.perTickBudget == 0 && RuntimeConfig.globalMaxBlocks > 1024) {
            CmsAll.LOGGER.warn("[CM'sALL] perTickBudget=0 with globalMaxBlocks=" + RuntimeConfig.globalMaxBlocks
                    + " — one break can process that many blocks in a single tick; set perTickBudget>0 on busy/public servers.");
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

        HarvestGroups.setExtraDenylist(resolveBlocks(server.denylist));

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

        CmsAll.LOGGER.info("[CM'sALL] lists ready — Mine " + mineBlocks.size() + "/" + mineTools.size()
                + ", Cut " + cutBlocks.size() + "/" + cutTools.size()
                + ", Dig " + digBlocks.size() + "/" + digTools.size());

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
        Set<Block> out = new HashSet<Block>();
        if (ids == null) {
            return out;
        }
        for (String id : ids) {
            ResourceLocation rl = parse(id);
            if (rl != null && ForgeRegistries.BLOCKS.containsKey(rl)) {
                out.add(ForgeRegistries.BLOCKS.getValue(rl));
            }
        }
        return out;
    }

    private static Set<Item> resolveItems(Iterable<String> ids) {
        Set<Item> out = new HashSet<Item>();
        if (ids == null) {
            return out;
        }
        for (String id : ids) {
            ResourceLocation rl = parse(id);
            if (rl != null && ForgeRegistries.ITEMS.containsKey(rl)) {
                out.add(ForgeRegistries.ITEMS.getValue(rl));
            }
        }
        return out;
    }

    private static ResourceLocation parse(String id) {
        try {
            return new ResourceLocation(id);
        } catch (Exception e) {
            return null;
        }
    }
}
