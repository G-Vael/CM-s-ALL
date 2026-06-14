package G_Vael.cmsall.client;

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

import G_Vael.cmsall.config.ServerConfig;
import G_Vael.cmsall.core.Functions;
import G_Vael.cmsall.net.CmsAllNetwork;

/** Per-function block/tool editor: Blocks/Tools tabs, search, scrollable list with ✕ remove, and + Add opening CmsAllAddGui. */
public class CmsAllFunctionGui extends GuiScreen {

    private static final int BASE_W = 320;

    private int panelW() {
        return Math.max(40, Math.min(BASE_W, this.width - 20));
    }
    private static final int LIST_TOP = 98;
    private static final int ROW_H = 22;
    private static final int ID_BLOCKS = 1, ID_TOOLS = 2, ID_ADD = 3, ID_DONE = 199;

    private final GuiScreen parent;
    private final ServerConfig working;
    private final Functions.Kind kind;
    private final boolean canEdit;
    private int serverList = 0; // 0 = blocks, 1 = tools
    private int listScroll;
    private GuiTextField searchField;

    public CmsAllFunctionGui(GuiScreen parent, ServerConfig working, Functions.Kind kind, boolean canEdit) {
        this.parent = parent;
        this.working = working;
        this.kind = kind;
        this.canEdit = canEdit;
    }

    private int panelX() {
        return this.width / 2 - panelW() / 2;
    }

    private boolean toolMode() {
        return serverList == 1;
    }

    List<String> currentList() {
        boolean tool = toolMode();
        switch (kind) {
            case MINE:
                return tool ? working.mineTools : working.mineBlocks;
            case CUT:
                return tool ? working.cutTools : working.cutBlocks;
            case DIG:
                return tool ? working.digTools : working.digBlocks;
            default:
                return working.mineBlocks;
        }
    }

    @Override
    public void initGui() {
        int x = panelX();
        String prev = searchField != null ? searchField.getText() : "";
        this.buttonList.clear();

        GuiButton blocks = new GuiButton(ID_BLOCKS, x, 50, 96, 20, I18n.format("cmsall.gui.list.blocks"));
        blocks.enabled = serverList != 0;
        this.buttonList.add(blocks);
        GuiButton tools = new GuiButton(ID_TOOLS, x + 100, 50, 96, 20, I18n.format("cmsall.gui.list.tools"));
        tools.enabled = serverList != 1;
        this.buttonList.add(tools);
        GuiButton add = new GuiButton(ID_ADD, x + panelW() - 96, 50, 96, 20, I18n.format("cmsall.gui.add"));
        add.enabled = canEdit;
        this.buttonList.add(add);

        searchField = new GuiTextField(0, this.fontRenderer, x, 74, panelW(), 18);
        searchField.setText(prev);
        searchField.setFocused(true);
        this.buttonList.add(new GuiButton(ID_DONE, this.width / 2 - 100, this.height - 28, 200, 20, I18n.format("gui.done")));
    }

    private String search() {
        return searchField == null ? "" : searchField.getText().toLowerCase(Locale.ROOT);
    }

    private List<RegistryLookup.Entry> entries() {
        List<RegistryLookup.Entry> out = new ArrayList<RegistryLookup.Entry>();
        boolean tool = toolMode();
        String q = search();
        for (String id : currentList()) {
            RegistryLookup.Entry e = RegistryLookup.resolve(tool, id);
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
        if (b.id == ID_BLOCKS) {
            serverList = 0;
            listScroll = 0;
            initGui();
        } else if (b.id == ID_TOOLS) {
            serverList = 1;
            listScroll = 0;
            initGui();
        } else if (b.id == ID_ADD) {
            this.mc.displayGuiScreen(new CmsAllAddGui(this, toolMode(), currentList(), kind, this::pushEdit));
        } else if (b.id == ID_DONE) {
            this.mc.displayGuiScreen(parent);
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
                    currentList().remove(list.get(listScroll + i).id);
                    pushEdit();
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
        this.drawCenteredString(this.fontRenderer, I18n.format(titleKey()), this.width / 2, 12, 0xFFFFFF);

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

    private String titleKey() {
        switch (kind) {
            case MINE:
                return "cmsall.func.mine";
            case CUT:
                return "cmsall.func.cut";
            case DIG:
                return "cmsall.func.dig";
            default:
                return "cmsall.func.mine";
        }
    }

    private void pushEdit() {
        if (canEdit) {
            CmsAllNetwork.sendEdit(working);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }
}
