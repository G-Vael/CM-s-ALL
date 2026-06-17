package g_vael.cmsall.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;

/** A square GuiButton that renders an ItemStack icon centred on it. */
public class IconButton extends GuiButton {

    private final ItemStack icon;

    public IconButton(int id, int x, int y, int size, ItemStack icon) {
        super(id, x, y, size, size, "");
        this.icon = icon;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        super.drawButton(mc, mouseX, mouseY, partialTicks);
        if (this.visible) {
            RenderHelper.enableGUIStandardItemLighting();
            mc.getRenderItem().renderItemIntoGUI(icon, this.x + (this.width - 16) / 2, this.y + (this.height - 16) / 2);
            RenderHelper.disableStandardItemLighting();
        }
    }
}
