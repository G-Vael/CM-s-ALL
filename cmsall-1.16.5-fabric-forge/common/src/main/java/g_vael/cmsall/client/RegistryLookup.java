package g_vael.cmsall.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;

import g_vael.cmsall.core.Functions;

/** Client-side registry browse/resolve for the config GUI's "add by id" flow. */
public final class RegistryLookup {

    /** Search/resolve result: id + icon stack + display name. */
    public static final class Entry {
        private final String id;
        private final ItemStack icon;
        private final Component name;

        public Entry(String id, ItemStack icon, Component name) {
            this.id = id;
            this.icon = icon;
            this.name = name;
        }

        public String id() {
            return id;
        }

        public ItemStack icon() {
            return icon;
        }

        public Component name() {
            return name;
        }
    }

    /** World-bulk filler hidden from the block "add" search — adding these to MineAll would be catastrophic. */
    private static final Set<String> BULK_FILLER = new HashSet<>(Arrays.asList(
            "minecraft:stone", "minecraft:deepslate", "minecraft:netherrack", "minecraft:end_stone"));

    private RegistryLookup() {
    }

    /** Every registry id that belongs to this function's auto category (modded included), for the Add screen's "add all" button: blocks → ores (MineAll) / logs (CutAll) / shovel-dig blocks (DigAll); tools → pickaxes / axes / shovels by id suffix. */
    public static List<String> allMatching(Functions.Kind kind, boolean toolMode) {
        List<String> out = new ArrayList<>();
        if (toolMode) {
            String suffix;
            switch (kind) {
                case CUT:
                    suffix = "_axe";
                    break;
                case DIG:
                    suffix = "_shovel";
                    break;
                case MINE:
                default:
                    suffix = "_pickaxe";
                    break;
            }
            for (Map.Entry<ResourceKey<Item>, Item> entry : Registry.ITEM.entrySet()) {
                if (entry.getKey().location().getPath().endsWith(suffix)) {
                    out.add(entry.getKey().location().toString());
                }
            }
        } else {
            for (Map.Entry<ResourceKey<Block>, Block> entry : Registry.BLOCK.entrySet()) {
                if (matchesBlockCategory(kind, entry.getValue(), entry.getKey().location().getPath())) {
                    out.add(entry.getKey().location().toString());
                }
            }
        }
        return out;
    }

    private static boolean matchesBlockCategory(Functions.Kind kind, Block block, String path) {
        BlockState state = block.defaultBlockState();
        switch (kind) {
            case MINE:
                return path.endsWith("_ore");
            case CUT:
                return state.is(BlockTags.LOGS) || path.endsWith("_log") || path.endsWith("_stem");
            case DIG:
                return isShovelMaterial(state.getMaterial());
            default:
                return false;
        }
    }

    /** 1.16.5 shovel-mineable blocks identified by their vanilla Material (no shovel tag). */
    private static boolean isShovelMaterial(Material m) {
        return m == Material.DIRT || m == Material.GRASS || m == Material.SAND
                || m == Material.CLAY || m == Material.TOP_SNOW || m == Material.SNOW;
    }

    public static List<Entry> search(boolean toolMode, String query, int cap) {
        String q = query.toLowerCase(Locale.ROOT);
        List<Entry> out = new ArrayList<>();
        if (toolMode) {
            for (Map.Entry<ResourceKey<Item>, Item> entry : Registry.ITEM.entrySet()) {
                ItemStack stack = new ItemStack(entry.getValue());
                if (!isMiningTool(stack)) {
                    continue; // mining tools only (incl. modded); skips swords/armor/food/blocks
                }
                if (matches(entry.getKey().location(), stack.getHoverName(), q)) {
                    out.add(new Entry(entry.getKey().location().toString(), stack, stack.getHoverName()));
                    if (out.size() >= cap) {
                        break;
                    }
                }
            }
        } else {
            for (Map.Entry<ResourceKey<Block>, Block> entry : Registry.BLOCK.entrySet()) {
                Block block = entry.getValue();
                if (BULK_FILLER.contains(entry.getKey().location().toString())) {
                    continue;
                }
                if (!isHarvestable(block)) {
                    continue; // skip air / fluids / portals (no item form)
                }
                Component name = block.getName();
                if (matches(entry.getKey().location(), name, q)) {
                    out.add(new Entry(entry.getKey().location().toString(), new ItemStack(block), name));
                    if (out.size() >= cap) {
                        break;
                    }
                }
            }
        }
        return out;
    }

    /** Unfiltered registry browse for the denylist/despawn editors: every item (itemMode) or every block (no harvest/bulk-filler exclusion). */
    public static List<Entry> searchAny(boolean itemMode, String query, int cap) {
        String q = query.toLowerCase(Locale.ROOT);
        List<Entry> out = new ArrayList<>();
        if (itemMode) {
            for (Map.Entry<ResourceKey<Item>, Item> entry : Registry.ITEM.entrySet()) {
                ItemStack stack = new ItemStack(entry.getValue());
                if (matches(entry.getKey().location(), stack.getHoverName(), q)) {
                    out.add(new Entry(entry.getKey().location().toString(), stack, stack.getHoverName()));
                    if (out.size() >= cap) {
                        break;
                    }
                }
            }
        } else {
            for (Map.Entry<ResourceKey<Block>, Block> entry : Registry.BLOCK.entrySet()) {
                Block block = entry.getValue();
                Component name = block.getName();
                if (matches(entry.getKey().location(), name, q)) {
                    out.add(new Entry(entry.getKey().location().toString(), new ItemStack(block), name));
                    if (out.size() >= cap) {
                        break;
                    }
                }
            }
        }
        return out;
    }

    /** Real blocks only: must have an item form. */
    private static boolean isHarvestable(Block block) {
        return block.asItem() != Items.AIR;
    }

    /** A mining tool = breaks stone/log/dirt faster than a fist (catches tagged and modded tools). */
    private static boolean isMiningTool(ItemStack stack) {
        return stack.getDestroySpeed(Blocks.STONE.defaultBlockState()) > 1.0f
                || stack.getDestroySpeed(Blocks.OAK_LOG.defaultBlockState()) > 1.0f
                || stack.getDestroySpeed(Blocks.DIRT.defaultBlockState()) > 1.0f;
    }

    private static boolean matches(ResourceLocation id, Component name, String lowerQuery) {
        if (lowerQuery.isEmpty()) {
            return true;
        }
        return id.toString().toLowerCase(Locale.ROOT).contains(lowerQuery)
                || name.getString().toLowerCase(Locale.ROOT).contains(lowerQuery);
    }

    /** Resolves a stored id to an icon + name; unknown ids (mod not installed) show a barrier + raw id. */
    public static Entry resolve(boolean toolMode, String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl != null) {
            if (toolMode) {
                Optional<Item> o = Registry.ITEM.getOptional(rl);
                if (o.isPresent()) {
                    ItemStack stack = new ItemStack(o.get());
                    return new Entry(id, stack, stack.getHoverName());
                }
            } else {
                Optional<Block> o = Registry.BLOCK.getOptional(rl);
                if (o.isPresent()) {
                    Block block = o.get();
                    return new Entry(id, new ItemStack(block), block.getName());
                }
            }
        }
        return new Entry(id, new ItemStack(Items.BARRIER), new TextComponent(id));
    }
}
