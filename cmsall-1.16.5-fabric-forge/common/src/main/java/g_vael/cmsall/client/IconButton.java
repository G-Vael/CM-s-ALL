package g_vael.cmsall.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.item.ItemStack;

/** A square button that renders an ItemStack icon centred on it. */
public class IconButton extends Button {

    private final ItemStack icon;

    public IconButton(int x, int y, int size, ItemStack icon, OnPress onPress) {
        super(x, y, size, size, new TextComponent(""), onPress);
        this.icon = icon;
    }

    @Override
    public void renderButton(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        super.renderButton(pose, mouseX, mouseY, partialTick);
        Minecraft.getInstance().getItemRenderer()
                .renderGuiItem(icon, this.x + (this.width - 16) / 2, this.y + (this.height - 16) / 2);
    }
}
