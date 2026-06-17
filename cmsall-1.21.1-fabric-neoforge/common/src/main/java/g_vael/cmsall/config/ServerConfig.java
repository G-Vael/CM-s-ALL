package g_vael.cmsall.config;

import java.util.ArrayList;
import java.util.List;

/** Layer-2 runtime config. */
public final class ServerConfig {
    // [general] + [limits]
    public boolean enabled = true;
    public boolean allowInCreative = false;
    public String defaultMode = "hold";
    public List<String> allowedModes = new ArrayList<>(List.of("hold", "toggle", "always", "sneak_invert"));
    public int globalMaxBlocks = 256;
    public int perTickBudget = 0;
    public double durabilityPerBlock = 1.0;
    public double exhaustionPerBlock = 0.005;
    public boolean dropToInventory = false;
    public boolean gatherDrops = false;
    public boolean chainBreakEffects = true;
    public boolean cutBreakLeaves = false;
    public boolean cutLeafDrops = true;
    public boolean cutFromTop = false;
    public boolean respectProtectionPerBlock = true;
    public boolean recordCommandPlace = false;

    // [tracking] per-function placed-block protection
    public boolean trackMine = false;
    public boolean trackCut = true;
    public boolean trackDig = false;
    public int trackMineMax = 4096;
    public int trackCutMax = 16384;
    public int trackDigMax = 4096;
    public String trackOverflow = "evict"; // "evict"=drop oldest (FIFO) | "stop"=stop recording

    // [permissions]
    public int editPermissionLevel = 3;

    // modded ids via the GUI or /cmsall <mine|cut|dig> <block|tool> add <id>.
    public List<String> mineBlocks = new ArrayList<>(List.of(
            "minecraft:coal_ore", "minecraft:deepslate_coal_ore",
            "minecraft:iron_ore", "minecraft:deepslate_iron_ore",
            "minecraft:copper_ore", "minecraft:deepslate_copper_ore",
            "minecraft:gold_ore", "minecraft:deepslate_gold_ore",
            "minecraft:redstone_ore", "minecraft:deepslate_redstone_ore",
            "minecraft:lapis_ore", "minecraft:deepslate_lapis_ore",
            "minecraft:diamond_ore", "minecraft:deepslate_diamond_ore",
            "minecraft:emerald_ore", "minecraft:deepslate_emerald_ore",
            "minecraft:nether_gold_ore", "minecraft:nether_quartz_ore"
    ));
    public List<String> mineTools = new ArrayList<>(List.of(
            "minecraft:wooden_pickaxe", "minecraft:stone_pickaxe", "minecraft:iron_pickaxe",
            "minecraft:golden_pickaxe", "minecraft:diamond_pickaxe", "minecraft:netherite_pickaxe"
    ));

    public List<String> cutBlocks = new ArrayList<>(List.of(
            "minecraft:oak_log", "minecraft:spruce_log", "minecraft:birch_log",
            "minecraft:jungle_log", "minecraft:acacia_log", "minecraft:dark_oak_log",
            "minecraft:mangrove_log", "minecraft:cherry_log",
            "minecraft:crimson_stem", "minecraft:warped_stem"
    ));
    public List<String> cutTools = new ArrayList<>(List.of(
            "minecraft:wooden_axe", "minecraft:stone_axe", "minecraft:iron_axe",
            "minecraft:golden_axe", "minecraft:diamond_axe", "minecraft:netherite_axe"
    ));

    public List<String> digBlocks = new ArrayList<>(List.of(
            "minecraft:dirt", "minecraft:coarse_dirt", "minecraft:rooted_dirt", "minecraft:grass_block",
            "minecraft:podzol", "minecraft:mycelium", "minecraft:sand", "minecraft:red_sand",
            "minecraft:gravel", "minecraft:clay", "minecraft:soul_sand", "minecraft:soul_soil"
    ));
    public List<String> digTools = new ArrayList<>(List.of(
            "minecraft:wooden_shovel", "minecraft:stone_shovel", "minecraft:iron_shovel",
            "minecraft:golden_shovel", "minecraft:diamond_shovel", "minecraft:netherite_shovel"
    ));

    // [denylist] extra blocks that must never be chain-broken
    public List<String> denylist = new ArrayList<>(List.of(
            "minecraft:spawner",
            "minecraft:end_portal_frame"
    ));

    public boolean despawnEnabled = false;
    public int despawnSeconds = 30;
    public List<String> despawnItems = new ArrayList<>(List.of(
            "minecraft:cobblestone",
            "minecraft:dirt",
            "minecraft:netherrack"
    ));
}
