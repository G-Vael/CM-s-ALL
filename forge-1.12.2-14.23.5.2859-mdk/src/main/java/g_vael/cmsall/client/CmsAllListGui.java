package g_vael.cmsall.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.lwjgl.input.Mouse;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;

import g_vael.cmsall.config.ServerConfig;

/**
 * Flat id-list editor (denylist blocks / despawn items), 1.12.2: browse the current target list with ✕ remove,
 * a search box that filters the list, and a "+ Add" button that opens the unfiltered {@link CmsAllAddGui} search screen.
 */
public class CmsAllListGui extends GuiScreen {

    private static final int BASE_W = 320;
    private static final int LIST_TOP = 74;
    private static final int ROW_H = 22;
    private static final int ID_ADD = 1, ID_DONE = 199;

    private final GuiScreen parent;
    private final ServerConfig working;
    private final boolean canEdit;
    private final String titleKey;
    private final List<String> target;
    private final boolean itemMode;
    private final Runnable onChange;

    private GuiTextField searchField;
    private int listScroll;

    public CmsAllListGui(GuiScreen parent, ServerConfig working, boolean canEdit, String titleKey,
                         List<String> target, boolean itemMode, Runnable onChange) {
        this.parent = parent;
        this.working = working;
        this.canEdit = canEdit;
        this.titleKey = titleKey;
        this.target = target;
        this.itemMode = itemMode;
        this.onChange = onChange;
    }

    private int panelW() {
        return Math.max(40, Math.min(BASE_W, this.width - 20));
    }

    private int panelX() {
        return this.width / 2 - panelW() / 2;
    }

    @Override
    public void initGui() {
        int x = panelX();
        String prev = searchField != null ? searchField.getText() : "";
        this.buttonList.clear();
        searchField = new GuiTextField(0, this.fontRenderer, x, 50, panelW() - 70, 18);
        searchField.setText(prev);
        searchField.setFocused(true);

        GuiButton add = new GuiButton(ID_ADD, x + panelW() - 66, 50, 66, 20, I18n.format("cmsall.gui.add"));
        add.enabled = canEdit;
        this.buttonList.add(add);

        this.buttonList.add(new GuiButton(ID_DONE, this.width / 2 - 100, this.height - 28, 200, 20, I18n.format("gui.done")));
    }

    private String search() {
        return searchField == null ? "" : searchField.getText().toLowerCase(Locale.ROOT);
    }

    /** The registered ids, filtered by the search box. */
    private List<RegistryLookup.Entry> entries() {
        List<RegistryLookup.Entry> out = new ArrayList<RegistryLookup.Entry>();
        String q = search();
        for (String id : target) {
            RegistryLookup.Entry e = RegistryLookup.resolve(itemMode, id);
            if (q.isEmpty() || id.toLowerCase(Locale.ROOT).contains(q) || e.name.toLowerCase(Locale.ROOT).contains(q)) {
                out.add(e);
            }
        }
        return out;
    }

    private int visibleRows() {
        return Math.max(1, (this.height - 36 - LIST_TOP) / ROW_H);
    }

    @Override
    public void updateScreen() {
        if (searchField != null) {
            searchField.updateCursorCounter();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (searchField != null && searchField.textboxKeyTyped(typedChar, keyCode)) {
            listScroll = 0;
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void actionPerformed(GuiButton b) throws IOException {
        if (b.id == ID_DONE) {
            this.mc.displayGuiScreen(parent);
        } else if (b.id == ID_ADD) {
            this.mc.displayGuiScreen(new CmsAllAddGui(this, target, itemMode, onChange));
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (searchField != null) {
            searchField.mouseClicked(mouseX, mouseY, mouseButton);
        }
        if (canEdit && mouseButton == 0) {
            int x = panelX();
            int remX = x + panelW() - 18;
            List<RegistryLookup.Entry> list = entries();
            int rows = visibleRows();
            for (int i = 0; i < rows && listScroll + i < list.size(); i++) {
                int rowY = LIST_TOP + i * ROW_H;
                if (mouseX >= remX && mouseX <= remX + 16 && mouseY >= rowY + 2 && mouseY <= rowY + 18) {
                    target.remove(list.get(listScroll + i).id);
                    onChange.run();
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
            int max = Math.max(0, entries().size() - visibleRows());
            listScroll = Math.max(0, Math.min(max, listScroll - (dWheel > 0 ? 1 : -1)));
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (searchField != null) {
            searchField.drawTextBox();
        }
        int x = panelX();
        this.drawCenteredString(this.fontRenderer, I18n.format(titleKey), this.width / 2, 12, 0xFFFFFF);

        List<RegistryLookup.Entry> list = entries();
        int rows = visibleRows();
        int remX = x + panelW() - 18;
        RenderHelper.enableGUIStandardItemLighting();
        for (int i = 0; i < rows && listScroll + i < list.size(); i++) {
            RegistryLookup.Entry e = list.get(listScroll + i);
            int rowY = LIST_TOP + i * ROW_H;
            boolean hovered = mouseX >= x && mouseX <= x + panelW() && mouseY >= rowY && mouseY <= rowY + ROW_H - 2;
            drawRect(x, rowY, x + panelW(), rowY + ROW_H - 2, hovered ? 0x50FFFFFF : 0x30000000);
            this.mc.getRenderItem().renderItemIntoGUI(e.icon, x + 4, rowY + 2);
            this.fontRenderer.drawString(e.name, x + 26, rowY + 1, 0xFFFFFF);
            this.fontRenderer.drawString(e.id, x + 26, rowY + 11, 0xA0A0A0);
            if (canEdit) {
                this.fontRenderer.drawString("✕", remX + 4, rowY + 6, 0xFF6060);
            }
        }
        RenderHelper.disableStandardItemLighting();
        if (list.isEmpty()) {
            this.drawCenteredString(this.fontRenderer, I18n.format("cmsall.gui.empty_list"),
                    this.width / 2, LIST_TOP + 8, 0xA0A0A0);
        }
        if (!canEdit) {
            this.drawCenteredString(this.fontRenderer, I18n.format("cmsall.gui.viewonly"),
                    this.width / 2, this.height - 40, 0xFF5555);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }
}
