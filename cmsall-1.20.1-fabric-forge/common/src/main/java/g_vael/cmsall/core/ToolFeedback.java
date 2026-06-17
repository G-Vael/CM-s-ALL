package g_vael.cmsall.core;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** Player-facing tool-durability warnings (). */
public final class ToolFeedback {

    private ToolFeedback() {
    }

    /** Red+bold action-bar notice that the tool is at its protected floor (no sound). */
    public static void lowDurability(ServerPlayer player, int remaining) {
        player.displayClientMessage(
                Component.translatable("cmsall.msg.tool_low", remaining)
                        .withStyle(ChatFormatting.RED, ChatFormatting.BOLD), true);
    }

    /** protect_tool stopped the chain to keep the tool alive: the low-durability notice plus a ping. */
    public static void protectedStop(ServerPlayer player, int remaining) {
        lowDurability(player, remaining);
        player.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 1.0f, 1.5f);
    }
}
