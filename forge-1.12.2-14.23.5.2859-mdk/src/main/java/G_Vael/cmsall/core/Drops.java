package G_Vael.cmsall.core;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Replicates vanilla 1.12.2 harvest drops as a List<ItemStack> (so the engine can redirect them to the origin / inventory / suppress them) plus the XP amount. */
public final class Drops {

    private Drops() {
    }

    public static List<ItemStack> compute(World world, BlockPos pos, IBlockState state, EntityPlayer player, ItemStack tool) {
        Block block = state.getBlock();
        List<ItemStack> out = new ArrayList<ItemStack>();
        if (silkTouches(world, pos, state, player, tool)) {
            Item item = Item.getItemFromBlock(block);
            if (item != Items.AIR) {
                int meta = item.getHasSubtypes() ? block.getMetaFromState(state) : 0;
                out.add(new ItemStack(item, 1, meta));
            }
            return out;
        }
        int fortune = EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, tool);
        NonNullList<ItemStack> list = NonNullList.<ItemStack>create();
        block.getDrops(list, world, pos, state, fortune);
        out.addAll(list);
        return out;
    }

    public static int expFor(World world, BlockPos pos, IBlockState state, EntityPlayer player, ItemStack tool) {
        if (silkTouches(world, pos, state, player, tool)) {
            return 0; // silk-touched ores drop no experience in vanilla
        }
        int fortune = EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, tool);
        return state.getBlock().getExpDrop(state, world, pos, fortune);
    }

    private static boolean silkTouches(World world, BlockPos pos, IBlockState state, EntityPlayer player, ItemStack tool) {
        return EnchantmentHelper.getEnchantmentLevel(Enchantments.SILK_TOUCH, tool) > 0
                && state.getBlock().canSilkHarvest(world, pos, state, player);
    }
}
