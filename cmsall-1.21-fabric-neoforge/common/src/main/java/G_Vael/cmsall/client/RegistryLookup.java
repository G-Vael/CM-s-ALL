package G_Vael.cmsall.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import G_Vael.cmsall.core.Functions;

/** Client-side registry browse/resolve for the config GUI's "add by id" flow. */
public final class RegistryLookup {

    public record Entry(String id, ItemStack icon, Component name) {
    }

    private static final TagKey<Block> ORES_TAG = TagKey.create(Registries.BLOCK, ResourceLocation.tryParse("c:ores"));
    private static final TagKey<Block> FORGE_ORES_TAG = TagKey.create(Registries.BLOCK, ResourceLocation.tryParse("forge:ores"));

    /** World-bulk filler hidden from the block "add" search — adding these to MineAll would be catastrophic. */
    private static final Set<String> BULK_FILLER = Set.of(
            "minecraft:stone", "minecraft:deepslate", "minecraft:netherrack", "minecraft:end_stone");

    private RegistryLookup() {
    }

    /** Every registry id that belongs to this function's auto category (modded included), for the Add screen's "add all" button: blocks → ores (MineAll) / logs (CutAll) / shovel-dig blocks (DigAll); tools → pickaxes / axes / shovels by id suffix. */
    public static List<String> allMatching(Functions.Kind kind, boolean toolMode) {
        List<String> out = new ArrayList<>();
        if (toolMode) {
            String suffix = switch (kind) {
                case MINE -> "_pickaxe";
                case CUT -> "_axe";
                case DIG -> "_shovel";
            };
            for (var entry : BuiltInRegistries.ITEM.entrySet()) {
                if (entry.getKey().location().getPath().endsWith(suffix)) {
                    out.add(entry.getKey().location().toString());
                }
            }
        } else {
            for (var entry : BuiltInRegistries.BLOCK.entrySet()) {
                if (matchesBlockCategory(kind, entry.getValue(), entry.getKey().location().getPath())) {
                    out.add(entry.getKey().location().toString());
                }
            }
        }
        return out;
    }

    private static boolean matchesBlockCategory(Functions.Kind kind, Block block, String path) {
        BlockState state = block.defaultBlockState();
        return switch (kind) {
            case MINE -> path.endsWith("_ore") || state.is(ORES_TAG) || state.is(FORGE_ORES_TAG);
            case CUT -> state.is(BlockTags.LOGS) || path.endsWith("_log") || path.endsWith("_stem");
            case DIG -> state.is(BlockTags.MINEABLE_WITH_SHOVEL);
        };
    }

    public static List<Entry> search(boolean toolMode, String query, int cap) {
        String q = query.toLowerCase(Locale.ROOT);
        List<Entry> out = new ArrayList<>();
        if (toolMode) {
            for (var entry : BuiltInRegistries.ITEM.entrySet()) {
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
            for (var entry : BuiltInRegistries.BLOCK.entrySet()) {
                Block block = entry.getValue();
                if (BULK_FILLER.contains(entry.getKey().location().toString())) {
                    continue;
                }
                if (!isHarvestable(block)) {
                    continue; // skip air / fluids / portals / bedrock & other unbreakable blocks
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
            for (var entry : BuiltInRegistries.ITEM.entrySet()) {
                ItemStack stack = new ItemStack(entry.getValue());
                if (matches(entry.getKey().location(), stack.getHoverName(), q)) {
                    out.add(new Entry(entry.getKey().location().toString(), stack, stack.getHoverName()));
                    if (out.size() >= cap) {
                        break;
                    }
                }
            }
        } else {
            for (var entry : BuiltInRegistries.BLOCK.entrySet()) {
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

    /** Real harvestable blocks only: has an item form and is mineable with a proper tool. */
    private static boolean isHarvestable(Block block) {
        if (block.asItem() == Items.AIR) {
            return false; // no item form (water, lava, fire, portals, piston internals, …)
        }
        BlockState state = block.defaultBlockState();
        return state.is(BlockTags.MINEABLE_WITH_PICKAXE)
                || state.is(BlockTags.MINEABLE_WITH_AXE)
                || state.is(BlockTags.MINEABLE_WITH_SHOVEL)
                || state.is(BlockTags.MINEABLE_WITH_HOE);
    }

    /** A mining tool = tagged pickaxe/axe/shovel/hoe (incl. */
    private static boolean isMiningTool(ItemStack stack) {
        return stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.AXES)
                || stack.is(ItemTags.SHOVELS) || stack.is(ItemTags.HOES)
                || stack.getDestroySpeed(Blocks.STONE.defaultBlockState()) > 1.0f
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
                if (BuiltInRegistries.ITEM.getOptional(rl).isPresent()) {
                    ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.getOptional(rl).get());
                    return new Entry(id, stack, stack.getHoverName());
                }
            } else if (BuiltInRegistries.BLOCK.getOptional(rl).isPresent()) {
                Block block = BuiltInRegistries.BLOCK.getOptional(rl).get();
                return new Entry(id, new ItemStack(block), block.getName());
            }
        }
        return new Entry(id, new ItemStack(Items.BARRIER), Component.literal(id));
    }
}
