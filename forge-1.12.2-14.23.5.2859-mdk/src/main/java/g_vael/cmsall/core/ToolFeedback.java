package g_vael.cmsall.core;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

/** Player-facing tool-durability warnings (). */
public final class ToolFeedback {

    private ToolFeedback() {
    }

    /** Red+bold action-bar notice that the tool is at its protected floor (no sound). */
    public static void lowDurability(EntityPlayerMP player, int remaining) {
        TextComponentTranslation msg = new TextComponentTranslation("cmsall.msg.tool_low", Integer.valueOf(remaining));
        msg.setStyle(new Style().setColor(TextFormatting.RED).setBold(Boolean.TRUE));
        player.sendStatusMessage(msg, true);
    }

    /** protect_tool stopped the chain to keep the tool alive: the low-durability notice plus a ping. */
    public static void protectedStop(EntityPlayerMP player, int remaining) {
        lowDurability(player, remaining);
        player.world.playSound((net.minecraft.entity.player.EntityPlayer) null, player.posX, player.posY, player.posZ,
                SoundEvents.BLOCK_NOTE_PLING, SoundCategory.PLAYERS, 1.0f, 1.5f);
    }
}
