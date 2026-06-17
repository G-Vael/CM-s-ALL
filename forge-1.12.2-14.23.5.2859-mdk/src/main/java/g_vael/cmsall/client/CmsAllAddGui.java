package g_vael.cmsall.client;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.lwjgl.input.Mouse;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;

import g_vael.cmsall.core.Functions;

/**
 * "Add by id" search screen, 1.12.2: type a block/item id or name; matching registry entries are listed with a +ADD hit-area.
 *
 * <p>Two flavours:
 * <ul>
 *   <li>filtered (per-function targets): blocks/tools restricted to the function's category, with a "+ Add all" bulk button;</li>
 *   <li>unfiltered (denylist / despawn items): every block or every item via {@link RegistryLookup#searchAny}, no bulk button,
 *       title cmsall.gui.add_block / cmsall.gui.add_item.</li>
 * </ul>
 */
public class CmsAllAddGui extends GuiScreen {

    private static final int BASE_W = 320;

    private int panelW() {
        return Math.max(40, Math.min(BASE_W, this.width - 20));
    }
    private static final int ROW_H = 22;
    private static final int TOP = 56;
    private static final int RESULT_CAP = 300;
    private static final int ID_ADD_ALL = 1, ID_DONE = 199;

    private final GuiScreen parent;
    private final boolean toolMode;
    private final List<String> target;
    private final Functions.Kind kind;
    private final Runnable onChange;
    private final boolean unfiltered;
    private final boolean itemMode;

    private GuiTextField query;
    private List<RegistryLookup.Entry> results = Collections.emptyList();
    private int scroll;

    public CmsAllAddGui(GuiScreen parent, boolean toolMode, List<String> target, Functions.Kind kind, Runnable onChange) {
        this.parent = parent;
        this.toolMode = toolMode;
        this.target = target;
        this.kind = kind;
        this.onChange = onChange;
        this.unfiltered = false;
        this.itemMode = toolMode;
    }

    /** Unfiltered variant for denylist (any block) / despawn items (any item). */
    public CmsAllAddGui(GuiScreen parent, List<String> target, boolean itemMode, Runnable onChange) {
        this.parent = parent;
        this.toolMode = false;
        this.target = target;
        this.kind = null;
        this.onChange = onChange;
        this.unfiltered = true;
        this.itemMode = itemMode;
    }

    private int panelX() {
        return this.width / 2 - panelW() / 2;
    }

    private List<RegistryLookup.Entry> doSearch(String q) {
        return unfiltered ? RegistryLookup.searchAny(itemMode, q, RESULT_CAP)
                : RegistryLookup.search(toolMode, q, RESULT_CAP);
    }

    private String titleKey() {
        if (unfiltered) {
            return itemMode ? "cmsall.gui.add_item" : "cmsall.gui.add_block";
        }
        return toolMode ? "cmsall.gui.add_tool" : "cmsall.gui.add_block";
    }

    @Override
    public void initGui() {
        int x = panelX();
        String prev = query != null ? query.getText() : "";
        this.buttonList.clear();
        query = new GuiTextField(0, this.fontRenderer, x, 30, unfiltered ? panelW() : panelW() - 104, 18);
        query.setText(prev);
        query.setFocused(true);
        if (!unfiltered) {
            this.buttonList.add(new GuiButton(ID_ADD_ALL, x + panelW() - 100, 29, 100, 20, I18n.format("cmsall.gui.add_all")));
        }
        this.buttonList.add(new GuiButton(ID_DONE, this.width / 2 - 100, this.height - 28, 200, 20, I18n.format("gui.done")));
        results = doSearch(prev);
    }

    private int visibleRows() {
        return Math.max(1, (this.height - 36 - TOP) / ROW_H);
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
            onChange.run();
        }
    }

    @Override
    public void updateScreen() {
        if (query != null) {
            query.updateCursorCounter();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (query != null && query.textboxKeyTyped(typedChar, keyCode)) {
            results = doSearch(query.getText());
            scroll = 0;
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void actionPerformed(GuiButton b) throws IOException {
        if (b.id == ID_ADD_ALL) {
            addAllMatching();
        } else if (b.id == ID_DONE) {
            this.mc.displayGuiScreen(parent);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (query != null) {
            query.mouseClicked(mouseX, mouseY, mouseButton);
        }
        if (mouseButton == 0) {
            int x = panelX();
            int addX = x + panelW() - 52;
            int rows = visibleRows();
            for (int i = 0; i < rows && scroll + i < results.size(); i++) {
                int rowY = TOP + i * ROW_H;
                if (mouseX >= addX && mouseX <= addX + 48 && mouseY >= rowY + 2 && mouseY <= rowY + 18) {
                    String id = results.get(scroll + i).id;
                    if (!target.contains(id)) {
                        target.add(id);
                        onChange.run();
                    }
                    return;
                }
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getDWheel();
        if (dWheel != 0) {
            int max = Math.max(0, results.size() - visibleRows());
            scroll = Math.max(0, Math.min(max, scroll - (dWheel > 0 ? 1 : -1)));
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (query != null) {
            query.drawTextBox();
        }
        int x = panelX();
        this.drawCenteredString(this.fontRenderer, I18n.format(titleKey()), this.width / 2, 12, 0xFFFFFF);

        int rows = visibleRows();
        int addX = x + panelW() - 52;
        RenderHelper.enableGUIStandardItemLighting();
        for (int i = 0; i < rows && scroll + i < results.size(); i++) {
            RegistryLookup.Entry e = results.get(scroll + i);
            int rowY = TOP + i * ROW_H;
            boolean hovered = mouseX >= x && mouseX <= x + panelW() && mouseY >= rowY && mouseY <= rowY + ROW_H - 2;
            drawRect(x, rowY, x + panelW(), rowY + ROW_H - 2, hovered ? 0x50FFFFFF : 0x30000000);
            this.mc.getRenderItem().renderItemIntoGUI(e.icon, x + 4, rowY + 2);
            this.fontRenderer.drawString(e.name, x + 26, rowY + 1, 0xFFFFFF);
            this.fontRenderer.drawString(e.id, x + 26, rowY + 11, 0xA0A0A0);

            boolean added = target.contains(e.id);
            drawRect(addX, rowY + 2, addX + 48, rowY + 18, added ? 0x6033AA33 : 0x60FFFFFF);
            String lbl = added ? "✔" : I18n.format("cmsall.gui.add_btn");
            this.drawCenteredString(this.fontRenderer, lbl, addX + 24, rowY + 5, added ? 0x88FF88 : 0xFFFFFF);
        }
        RenderHelper.disableStandardItemLighting();
        if (results.isEmpty()) {
            this.drawCenteredString(this.fontRenderer, I18n.format("cmsall.gui.no_results"),
                    this.width / 2, TOP + 8, 0xA0A0A0);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }
}
