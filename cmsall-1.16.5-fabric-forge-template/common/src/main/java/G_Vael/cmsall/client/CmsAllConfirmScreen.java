package G_Vael.cmsall.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

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
        addButton(new Button(x, y, half, 20, new TranslatableComponent("cmsall.gui.confirm.yes"), b -> {
            onConfirm.run();
            back();
        }));
        addButton(new Button(x + half + 4, y, half, 20, new TranslatableComponent("cmsall.gui.confirm.no"), b -> back()));
    }

    private void back() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(pose);
        super.render(pose, mouseX, mouseY, partialTick);
        drawCenteredString(pose, this.font, this.message, this.width / 2, this.height / 2 - 24, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        back();
    }
}
