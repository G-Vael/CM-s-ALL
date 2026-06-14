package G_Vael.cmsall.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Shared Yes/No confirmation prompt. */
public class CmsAllConfirmScreen extends Screen {

    private static final int BASE_W = 300;

    private final Screen parent;
    private final Component message;
    private final Runnable onConfirm;

    public CmsAllConfirmScreen(Screen parent, Component message, Runnable onConfirm) {
        super(message);
        this.parent = parent;
        this.message = message;
        this.onConfirm = onConfirm;
    }

    private int panelW() {
        return Math.max(40, Math.min(BASE_W, this.width - 20));
    }

    @Override
    protected void init() {
        int w = panelW();
        int x = this.width / 2 - w / 2;
        int half = (w - 4) / 2;
        int y = this.height / 2 + 6;
        addRenderableWidget(Button.builder(Component.translatable("cmsall.gui.confirm.yes"), b -> {
            onConfirm.run();
            back();
        }).bounds(x, y, half, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("cmsall.gui.confirm.no"), b -> back())
                .bounds(x + half + 4, y, half, 20).build());
    }

    private void back() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.message, this.width / 2, this.height / 2 - 24, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        back();
    }
}
