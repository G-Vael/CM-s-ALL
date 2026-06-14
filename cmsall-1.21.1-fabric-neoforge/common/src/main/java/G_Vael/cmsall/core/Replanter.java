package G_Vael.cmsall.core;

import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Per-player auto-replant after a CutAll tree is felled (opt-in via /cmsall replant). */
public final class Replanter {

    /** log/stem → sapling/fungus/propagule for the vanilla species (best effort). */
    private static final Map<Block, Block> SAPLINGS = Map.ofEntries(
            Map.entry(Blocks.OAK_LOG, Blocks.OAK_SAPLING),
            Map.entry(Blocks.SPRUCE_LOG, Blocks.SPRUCE_SAPLING),
            Map.entry(Blocks.BIRCH_LOG, Blocks.BIRCH_SAPLING),
            Map.entry(Blocks.JUNGLE_LOG, Blocks.JUNGLE_SAPLING),
            Map.entry(Blocks.ACACIA_LOG, Blocks.ACACIA_SAPLING),
            Map.entry(Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_SAPLING),
            Map.entry(Blocks.CHERRY_LOG, Blocks.CHERRY_SAPLING),
            Map.entry(Blocks.MANGROVE_LOG, Blocks.MANGROVE_PROPAGULE),
            Map.entry(Blocks.CRIMSON_STEM, Blocks.CRIMSON_FUNGUS),
            Map.entry(Blocks.WARPED_STEM, Blocks.WARPED_FUNGUS));

    private final ServerLevel level;
    private final ServerPlayer player;
    private final BlockPos pos;
    private final Block sapling;
    private final Item saplingItem;
    private boolean reserved;

    private Replanter(ServerLevel level, ServerPlayer player, BlockPos pos, Block sapling) {
        this.level = level;
        this.player = player;
        this.pos = pos.immutable();
        this.sapling = sapling;
        this.saplingItem = sapling.asItem();
    }

    /** Build a replanter for a felled tree, or null when it doesn't apply: not TREE mode, the player hasn't enabled replant, the species has no known sapling, or no chain position has soil that can sustain it. */
    static Replanter maybe(ServerLevel level, ServerPlayer player, BlockPos origin, BlockState originState,
                           HarvestGroup group, List<BlockPos> targets) {
        if (group.mode() != HarvestMode.TREE || !ActivationState.replant(player)) {
            return null;
        }
        Block sap = SAPLINGS.get(originState.getBlock());
        if (sap == null) {
            return null; // unknown / modded species: nothing safe to plant
        }
        BlockState sapState = sap.defaultBlockState();
        BlockPos best = sapState.canSurvive(level, origin) ? origin : null;
        for (BlockPos p : targets) {
            if (!sapState.canSurvive(level, p)) {
                continue;
            }
            if (best == null || p.getY() < best.getY()) {
                best = p;
            }
        }
        return best == null ? null : new Replanter(level, player, best, sap);
    }

    /** Reserve one sapling out of a chain drop before it spawns (drops-first sourcing). */
    void offerDrop(ItemStack stack) {
        if (reserved || stack.isEmpty() || stack.getItem() != saplingItem) {
            return;
        }
        stack.shrink(1);
        reserved = true;
    }

    /** Plant the sapling. */
    void finish() {
        BlockState at = level.getBlockState(pos);
        BlockState sapState = sapling.defaultBlockState();
        if (!at.isAir() || !sapState.canSurvive(level, pos)) {
            if (reserved) {
                giveBack(); // couldn't plant: don't swallow the sapling we pulled from the drops
            }
            return;
        }
        if (reserved || consumeOne()) {
            level.setBlock(pos, sapState, 3);
        }
    }

    private boolean consumeOne() {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && s.getItem() == saplingItem) {
                s.shrink(1);
                return true;
            }
        }
        return false;
    }

    private void giveBack() {
        ItemStack s = new ItemStack(saplingItem);
        if (!player.getInventory().add(s)) {
            player.drop(s, false);
        }
    }
}
