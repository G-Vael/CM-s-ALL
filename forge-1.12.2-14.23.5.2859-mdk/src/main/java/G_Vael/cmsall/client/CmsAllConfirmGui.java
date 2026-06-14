package G_Vael.cmsall.client;

import java.io.IOException;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

/** Shared Yes/No confirmation prompt, 1.12.2. */
public class CmsAllConfirmGui extends GuiScreen {

    private static final int BASE_W = 300;
    private static final int ID_YES = 1, ID_NO = 2;

    private final GuiScreen parent;
    private final String message;
    private final Runnable onConfirm;

    public CmsAllConfirmGui(GuiScreen parent, String message, Runnable onConfirm) {
        this.parent = parent;
        this.message = message;
        this.onConfirm = onConfirm;
    }

    private int panelW() {
        return Math.max(40, Math.min(BASE_W, this.width - 20));
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        int w = panelW();
        int x = this.width / 2 - w / 2;
        int half = (w - 4) / 2;
        int y = this.height / 2 + 6;
        this.buttonList.add(new GuiButton(ID_YES, x, y, half, 20, I18n.format("cmsall.gui.confirm.yes")));
        this.buttonList.add(new GuiButton(ID_NO, x + half + 4, y, half, 20, I18n.format("cmsall.gui.confirm.no")));
    }

    @Override
    protected void actionPerformed(GuiButton b) throws IOException {
        if (b.id == ID_YES) {
            onConfirm.run();
        }
        this.mc.displayGuiScreen(parent);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.drawCenteredString(this.fontRenderer, message, this.width / 2, this.height / 2 - 24, 0xFFFFFF);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }
}
