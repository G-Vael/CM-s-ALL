package G_Vael.cmsall.client;

import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import G_Vael.cmsall.core.Functions;

/** "Add by id" search screen. */
public class CmsAllAddScreen extends Screen {

    private static final int BASE_W = 320;
    private static final int ROW_H = 22;
    private static final int TOP = 56;
    private static final int RESULT_CAP = 300;

    private int panelW() {
        return Math.max(40, Math.min(BASE_W, this.width - 20));
    }

    private final Screen parent;
    private final boolean toolMode;
    private final List<String> target;
    private final Functions.Kind kind;
    private final Runnable onChange;
    private final boolean unfiltered;
    private final boolean itemMode;

    private EditBox query;
    private List<RegistryLookup.Entry> results = List.of();
    private int scroll;

    public CmsAllAddScreen(Screen parent, boolean toolMode, List<String> target, Functions.Kind kind, Runnable onChange) {
        super(Component.translatable(toolMode ? "cmsall.gui.add_tool" : "cmsall.gui.add_block"));
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
        super(Component.translatable(itemMode ? "cmsall.gui.add_item" : "cmsall.gui.add_block"));
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
        query = new EditBox(this.font, x, 30, unfiltered ? panelW() : panelW() - 104, 18, Component.translatable("cmsall.gui.search"));
        query.setHint(Component.translatable("cmsall.gui.search"));
        query.setResponder(text -> {
            results = doSearch(text == null ? "" : text);
            scroll = 0;
        });
        addRenderableWidget(query);
        setInitialFocus(query);
        results = doSearch("");

        if (!unfiltered) {
            addRenderableWidget(Button.builder(Component.translatable("cmsall.gui.add_all"), b -> addAllMatching())
                    .bounds(x + panelW() - 100, 29, 100, 20)
                    .tooltip(Tooltip.create(Component.translatable("cmsall.tip.add_all"))).build());
        }

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(this.width / 2 - 100, this.height - 28, 200, 20).build());
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
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick); // vanilla dark + blurred backdrop
        int x = panelX();
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);

        int rows = visibleRows();
        int addX = x + panelW() - 52;
        for (int i = 0; i < rows && scroll + i < results.size(); i++) {
            RegistryLookup.Entry e = results.get(scroll + i);
            int rowY = TOP + i * ROW_H;
            boolean hovered = mouseX >= x && mouseX <= x + panelW() && mouseY >= rowY && mouseY <= rowY + ROW_H - 2;
            graphics.fill(x, rowY, x + panelW(), rowY + ROW_H - 2, hovered ? 0x50FFFFFF : 0x30000000);
            graphics.renderItem(e.icon(), x + 4, rowY + 2);
            graphics.drawString(this.font, e.name(), x + 26, rowY + 1, 0xFFFFFF, false);
            graphics.drawString(this.font, e.id(), x + 26, rowY + 11, 0xA0A0A0, false);

            boolean added = target.contains(e.id());
            graphics.fill(addX, rowY + 2, addX + 48, rowY + 18, added ? 0x6033AA33 : 0x60FFFFFF);
            Component label = added ? Component.literal("✔") : Component.translatable("cmsall.gui.add_btn");
            graphics.drawCenteredString(this.font, label, addX + 24, rowY + 5, added ? 0x88FF88 : 0xFFFFFF);
        }
        if (results.isEmpty()) {
            graphics.drawCenteredString(this.font, Component.translatable("cmsall.gui.no_results"),
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
