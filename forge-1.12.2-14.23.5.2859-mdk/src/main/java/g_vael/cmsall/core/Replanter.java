package g_vael.cmsall.core;

import java.util.List;

import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockNewLog;
import net.minecraft.block.BlockOldLog;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.BlockSapling;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Per-player auto-replant after a CutAll tree is felled (opt-in via /cmsall replant). */
public final class Replanter {

    private final World world;
    private final EntityPlayerMP player;
    private final BlockPos pos;
    private final BlockPlanks.EnumType type;
    private final int saplingMeta;
    private boolean reserved;

    private Replanter(World world, EntityPlayerMP player, BlockPos pos, BlockPlanks.EnumType type) {
        this.world = world;
        this.player = player;
        this.pos = pos.toImmutable();
        this.type = type;
        this.saplingMeta = type.getMetadata();
    }

    static Replanter maybe(World world, EntityPlayerMP player, BlockPos origin, IBlockState originState,
                           HarvestGroup group, List<BlockPos> targets) {
        if (group.mode() != HarvestMode.TREE || !ActivationState.replant(player)) {
            return null;
        }
        net.minecraft.block.Block b = originState.getBlock();
        BlockPlanks.EnumType type;
        if (b == Blocks.LOG) {
            type = originState.getValue(BlockOldLog.VARIANT);
        } else if (b == Blocks.LOG2) {
            type = originState.getValue(BlockNewLog.VARIANT);
        } else {
            return null; // not a vanilla overworld log species
        }
        IBlockState sap = Blocks.SAPLING.getDefaultState().withProperty(BlockSapling.TYPE, type);
        BlockBush bush = (BlockBush) Blocks.SAPLING;
        BlockPos best = bush.canBlockStay(world, origin, sap) ? origin : null;
        for (BlockPos p : targets) {
            if (!bush.canBlockStay(world, p, sap)) {
                continue;
            }
            if (best == null || p.getY() < best.getY()) {
                best = p;
            }
        }
        return best == null ? null : new Replanter(world, player, best, type);
    }

    /** Reserve one matching sapling out of a chain drop before it spawns (drops-first sourcing). */
    void offerDrop(ItemStack stack) {
        if (reserved || stack.isEmpty()) {
            return;
        }
        if (stack.getItem() == Item.getItemFromBlock(Blocks.SAPLING) && stack.getMetadata() == saplingMeta) {
            stack.shrink(1);
            reserved = true;
        }
    }

    /** Plant the sapling. */
    void finish() {
        IBlockState sap = Blocks.SAPLING.getDefaultState().withProperty(BlockSapling.TYPE, type);
        if (!world.isAirBlock(pos) || !((BlockBush) Blocks.SAPLING).canBlockStay(world, pos, sap)) {
            if (reserved) {
                giveBack();
            }
            return;
        }
        if (reserved || consumeOne()) {
            world.setBlockState(pos, sap, 3);
        }
    }

    private boolean consumeOne() {
        InventoryPlayer inv = player.inventory;
        Item saplingItem = Item.getItemFromBlock(Blocks.SAPLING);
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack s = inv.getStackInSlot(i);
            if (!s.isEmpty() && s.getItem() == saplingItem && s.getMetadata() == saplingMeta) {
                s.shrink(1);
                return true;
            }
        }
        return false;
    }

    private void giveBack() {
        ItemStack s = new ItemStack(Blocks.SAPLING, 1, saplingMeta);
        if (!player.inventory.addItemStackToInventory(s)) {
            player.dropItem(s, false);
        }
    }
}
