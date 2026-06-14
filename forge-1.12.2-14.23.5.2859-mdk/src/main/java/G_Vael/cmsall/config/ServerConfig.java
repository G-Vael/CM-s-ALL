package G_Vael.cmsall.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Layer-2 runtime config. */
public final class ServerConfig {
    public boolean enabled = true;
    public boolean allowInCreative = false;
    public String defaultMode = "hold";
    public List<String> allowedModes = new ArrayList<String>(Arrays.asList("hold", "toggle", "always", "sneak_invert"));
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
    public boolean recordCommandPlace = false; // no-op on 1.12.2 (no command-place hook)

    // [tracking] per-function placed-block protection
    public boolean trackMine = false;
    public boolean trackCut = true;
    public boolean trackDig = false;
    public int trackMineMax = 4096;
    public int trackCutMax = 16384;
    public int trackDigMax = 4096;
    public String trackOverflow = "evict"; // "evict"=drop oldest (FIFO) | "stop"=stop recording

    public int editPermissionLevel = 3;

    public List<String> mineBlocks = new ArrayList<String>(Arrays.asList(
            "minecraft:coal_ore", "minecraft:iron_ore", "minecraft:gold_ore",
            "minecraft:redstone_ore", "minecraft:lit_redstone_ore", "minecraft:lapis_ore",
            "minecraft:diamond_ore", "minecraft:emerald_ore", "minecraft:quartz_ore"
    ));
    public List<String> mineTools = new ArrayList<String>(Arrays.asList(
            "minecraft:wooden_pickaxe", "minecraft:stone_pickaxe", "minecraft:iron_pickaxe",
            "minecraft:golden_pickaxe", "minecraft:diamond_pickaxe"
    ));

    public List<String> cutBlocks = new ArrayList<String>(Arrays.asList(
            "minecraft:log", "minecraft:log2"
    ));
    public List<String> cutTools = new ArrayList<String>(Arrays.asList(
            "minecraft:wooden_axe", "minecraft:stone_axe", "minecraft:iron_axe",
            "minecraft:golden_axe", "minecraft:diamond_axe"
    ));

    public List<String> digBlocks = new ArrayList<String>(Arrays.asList(
            "minecraft:dirt", "minecraft:grass", "minecraft:sand", "minecraft:gravel",
            "minecraft:clay", "minecraft:soul_sand", "minecraft:mycelium"
    ));
    public List<String> digTools = new ArrayList<String>(Arrays.asList(
            "minecraft:wooden_shovel", "minecraft:stone_shovel", "minecraft:iron_shovel",
            "minecraft:golden_shovel", "minecraft:diamond_shovel"
    ));

    public List<String> denylist = new ArrayList<String>(Arrays.asList(
            "minecraft:mob_spawner",
            "minecraft:end_portal_frame"
    ));

    public boolean despawnEnabled = false;
    public int despawnSeconds = 30;
    public List<String> despawnItems = new ArrayList<String>(Arrays.asList(
            "minecraft:cobblestone",
            "minecraft:dirt",
            "minecraft:netherrack"
    ));
}
