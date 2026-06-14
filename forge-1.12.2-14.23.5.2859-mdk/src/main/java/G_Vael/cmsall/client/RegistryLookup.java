package G_Vael.cmsall.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import G_Vael.cmsall.core.Functions;

/** Client-side registry browse/resolve for the config GUI "add by id" flow. */
public final class RegistryLookup {

    public static final class Entry {
        public final String id;
        public final ItemStack icon;
        public final String name;

        public Entry(String id, ItemStack icon, String name) {
            this.id = id;
            this.icon = icon;
            this.name = name;
        }
    }

    private static final Set<String> BULK_FILLER = new HashSet<String>(Arrays.asList(
            "minecraft:stone", "minecraft:netherrack", "minecraft:end_stone"));

    private RegistryLookup() {
    }

    public static List<String> allMatching(Functions.Kind kind, boolean toolMode) {
        List<String> out = new ArrayList<String>();
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
            for (Item item : ForgeRegistries.ITEMS) {
                ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
                if (rl != null && rl.getResourcePath().endsWith(suffix)) {
                    out.add(rl.toString());
                }
            }
        } else {
            for (Block block : ForgeRegistries.BLOCKS) {
                ResourceLocation rl = ForgeRegistries.BLOCKS.getKey(block);
                if (rl != null && matchesBlockCategory(kind, block, rl.getResourcePath())) {
                    out.add(rl.toString());
                }
            }
        }
        return out;
    }

    private static boolean matchesBlockCategory(Functions.Kind kind, Block block, String path) {
        switch (kind) {
            case MINE:
                return path.endsWith("_ore");
            case CUT:
                return path.equals("log") || path.equals("log2") || path.endsWith("_log");
            case DIG:
                return isShovelMaterial(block.getDefaultState().getMaterial());
            default:
                return false;
        }
    }

    private static boolean isShovelMaterial(Material m) {
        return m == Material.GROUND || m == Material.GRASS || m == Material.SAND
                || m == Material.CLAY || m == Material.SNOW || m == Material.CRAFTED_SNOW;
    }

    public static List<Entry> search(boolean toolMode, String query, int cap) {
        String q = query.toLowerCase(Locale.ROOT);
        List<Entry> out = new ArrayList<Entry>();
        if (toolMode) {
            for (Item item : ForgeRegistries.ITEMS) {
                ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
                if (rl == null) {
                    continue;
                }
                ItemStack stack = new ItemStack(item);
                if (!isMiningTool(stack)) {
                    continue;
                }
                if (matches(rl, stack.getDisplayName(), q)) {
                    out.add(new Entry(rl.toString(), stack, stack.getDisplayName()));
                    if (out.size() >= cap) {
                        break;
                    }
                }
            }
        } else {
            for (Block block : ForgeRegistries.BLOCKS) {
                ResourceLocation rl = ForgeRegistries.BLOCKS.getKey(block);
                if (rl == null || BULK_FILLER.contains(rl.toString())) {
                    continue;
                }
                if (!isHarvestable(block)) {
                    continue;
                }
                ItemStack stack = new ItemStack(block);
                String name = stack.getDisplayName();
                if (matches(rl, name, q)) {
                    out.add(new Entry(rl.toString(), stack, name));
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
        List<Entry> out = new ArrayList<Entry>();
        if (itemMode) {
            for (Item item : ForgeRegistries.ITEMS) {
                ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
                if (rl == null) {
                    continue;
                }
                ItemStack stack = new ItemStack(item);
                if (matches(rl, stack.getDisplayName(), q)) {
                    out.add(new Entry(rl.toString(), stack, stack.getDisplayName()));
                    if (out.size() >= cap) {
                        break;
                    }
                }
            }
        } else {
            for (Block block : ForgeRegistries.BLOCKS) {
                ResourceLocation rl = ForgeRegistries.BLOCKS.getKey(block);
                if (rl == null) {
                    continue;
                }
                ItemStack stack = new ItemStack(block);
                String name = stack.getDisplayName();
                if (matches(rl, name, q)) {
                    out.add(new Entry(rl.toString(), stack, name));
                    if (out.size() >= cap) {
                        break;
                    }
                }
            }
        }
        return out;
    }

    private static boolean isHarvestable(Block block) {
        return Item.getItemFromBlock(block) != Items.AIR;
    }

    private static boolean isMiningTool(ItemStack stack) {
        return stack.getDestroySpeed(Blocks.STONE.getDefaultState()) > 1.0f
                || stack.getDestroySpeed(Blocks.LOG.getDefaultState()) > 1.0f
                || stack.getDestroySpeed(Blocks.DIRT.getDefaultState()) > 1.0f;
    }

    private static boolean matches(ResourceLocation id, String name, String lowerQuery) {
        if (lowerQuery.isEmpty()) {
            return true;
        }
        return id.toString().toLowerCase(Locale.ROOT).contains(lowerQuery)
                || name.toLowerCase(Locale.ROOT).contains(lowerQuery);
    }

    public static Entry resolve(boolean toolMode, String id) {
        ResourceLocation rl;
        try {
            rl = new ResourceLocation(id);
        } catch (Exception e) {
            rl = null;
        }
        if (rl != null) {
            if (toolMode) {
                if (ForgeRegistries.ITEMS.containsKey(rl)) {
                    ItemStack stack = new ItemStack(ForgeRegistries.ITEMS.getValue(rl));
                    return new Entry(id, stack, stack.getDisplayName());
                }
            } else if (ForgeRegistries.BLOCKS.containsKey(rl)) {
                ItemStack stack = new ItemStack(ForgeRegistries.BLOCKS.getValue(rl));
                return new Entry(id, stack, stack.getDisplayName());
            }
        }
        return new Entry(id, new ItemStack(Blocks.BARRIER), id);
    }
}
