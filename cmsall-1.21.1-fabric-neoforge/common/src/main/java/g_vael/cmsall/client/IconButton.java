package g_vael.cmsall.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** A square button that renders an ItemStack icon centred on it. */
public class IconButton extends Button {

    private final ItemStack icon;

    public IconButton(int x, int y, int size, ItemStack icon, OnPress onPress) {
        super(x, y, size, size, Component.empty(), onPress, DEFAULT_NARRATION);
        this.icon = icon;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
        graphics.renderItem(icon, getX() + (width - 16) / 2, getY() + (height - 16) / 2);
    }
}
