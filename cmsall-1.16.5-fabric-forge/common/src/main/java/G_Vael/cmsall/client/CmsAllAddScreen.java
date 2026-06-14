package G_Vael.cmsall.client;

import java.util.Collections;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

import G_Vael.cmsall.core.Functions;

/** "Add by id" search screen. */
public class CmsAllAddScreen extends Screen {

    private static final int BASE_W = 320;

    private int panelW() {
        return Math.max(40, Math.min(BASE_W, this.width - 20));
    }
    private static final int ROW_H = 22;
    private static final int TOP = 56;
    private static final int RESULT_CAP = 300;

    private final Screen parent;
    private final boolean toolMode;
    private final List<String> target;
    private final Functions.Kind kind;
    private final Runnable onChange;
    private final boolean unfiltered;
    private final boolean itemMode;

    private EditBox query;
    private List<RegistryLookup.Entry> results = Collections.emptyList();
    private int scroll;

    public CmsAllAddScreen(Screen parent, boolean toolMode, List<String> target, Functions.Kind kind, Runnable onChange) {
        super(new TranslatableComponent(toolMode ? "cmsall.gui.add_tool" : "cmsall.gui.add_block"));
        this.parent = parent;
        this.toolMode = toolMode;
        this.target = target;
        this.kind = kind;
        this.onChange = onChange;
        this.unfiltered = false;
        this.itemMode = toolMode;
    }

    /** Unfiltered variant for denylist (any block) / despawn items (any item). */
    public CmsAllAddScreen(Screen parent, List<String> target, boolean itemMode, Runnable onChange) {
        super(new TranslatableComponent(itemMode ? "cmsall.gui.add_item" : "cmsall.gui.add_block"));
        this.parent = parent;
        this.toolMode = false;
        this.target = target;
        this.kind = null;
        this.onChange = onChange;
        this.unfiltered = true;
        this.itemMode = itemMode;
    }

    private List<RegistryLookup.Entry> doSearch(String q) {
        return unfiltered ? RegistryLookup.searchAny(itemMode, q, RESULT_CAP)
                : RegistryLookup.search(toolMode, q, RESULT_CAP);
    }

    private int panelX() {
        return this.width / 2 - panelW() / 2;
    }

    @Override
    protected void init() {
        int x = panelX();
        query = new EditBox(this.font, x, 30, unfiltered ? panelW() : panelW() - 104, 18, new TranslatableComponent("cmsall.gui.search"));
        query.setResponder(text -> {
            results = doSearch(text == null ? "" : text);
            scroll = 0;
        });
        addButton(query);
        setInitialFocus(query);
        results = doSearch("");

        if (!unfiltered) {
            addButton(new Button(x + panelW() - 100, 29, 100, 20, new TranslatableComponent("cmsall.gui.add_all"),
                    b -> addAllMatching()));
        }

        addButton(new Button(this.width / 2 - 100, this.height - 28, 200, 20, CommonComponents.GUI_DONE, b -> onClose()));
    }

    private void addAllMatching() {
        boolean changed = false;
        for (String id : RegistryLookup.allMatching(kind, toolMode)) {
            if (!target.contains(id)) {
                target.add(id);
                changed = true;
            }
        }
        if (changed) {
            onChange.run(); // apply the bulk add immediately
        }
    }

    private int visibleRows() {
        return Math.max(1, (this.height - 36 - TOP) / ROW_H);
    }

    private int maxScroll() {
        return Math.max(0, results.size() - visibleRows());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button == 0) {
            int x = panelX();
            int addX = x + panelW() - 52;
            int rows = visibleRows();
            for (int i = 0; i < rows && scroll + i < results.size(); i++) {
                int rowY = TOP + i * ROW_H;
                if (mouseX >= addX && mouseX <= addX + 48 && mouseY >= rowY + 2 && mouseY <= rowY + 18) {
                    String id = results.get(scroll + i).id();
                    if (!target.contains(id)) {
                        target.add(id);
                        onChange.run(); // apply the addition immediately — no Save button
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta != 0) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(delta)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(pose);
        super.render(pose, mouseX, mouseY, partialTick);
        int x = panelX();
        drawCenteredString(pose, this.font, this.title, this.width / 2, 12, 0xFFFFFF);

        int rows = visibleRows();
        int addX = x + panelW() - 52;
        for (int i = 0; i < rows && scroll + i < results.size(); i++) {
            RegistryLookup.Entry e = results.get(scroll + i);
            int rowY = TOP + i * ROW_H;
            boolean hovered = mouseX >= x && mouseX <= x + panelW() && mouseY >= rowY && mouseY <= rowY + ROW_H - 2;
            fill(pose, x, rowY, x + panelW(), rowY + ROW_H - 2, hovered ? 0x50FFFFFF : 0x30000000);
            this.minecraft.getItemRenderer().renderGuiItem(e.icon(), x + 4, rowY + 2);
            drawString(pose, this.font, e.name(), x + 26, rowY + 1, 0xFFFFFF);
            drawString(pose, this.font, new TextComponent(e.id()), x + 26, rowY + 11, 0xA0A0A0);

            boolean added = target.contains(e.id());
            fill(pose, addX, rowY + 2, addX + 48, rowY + 18, added ? 0x6033AA33 : 0x60FFFFFF);
            Component label = added ? new TextComponent("✔") : new TranslatableComponent("cmsall.gui.add_btn");
            drawCenteredString(pose, this.font, label, addX + 24, rowY + 5, added ? 0x88FF88 : 0xFFFFFF);
        }
        if (results.isEmpty()) {
            drawCenteredString(pose, this.font, new TranslatableComponent("cmsall.gui.no_results"),
                    this.width / 2, TOP + 8, 0xA0A0A0);
        }
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }
}
