package G_Vael.cmsall.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Mouse;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;

import G_Vael.cmsall.config.ServerConfig;
import G_Vael.cmsall.core.ActivationMode;
import G_Vael.cmsall.net.CmsAllNetwork;

/** 詳細設定: rarely-used server settings, 1.12.2, inline under dim category headers with mouse-wheel scroll. */
public class CmsAllAdvancedGui extends GuiScreen {

    private static final int BASE_W = 300;
    private static final int NUM_ROWS = 6;

    private static final int ID_DONE = 199;
    private static final int ID_ALLOWCREATIVE = 10, ID_CHAINFX = 11, ID_PROTECTPERBLOCK = 12, ID_RECORD = 13;
    private static final int ID_SET = 20; // 6 set buttons: ID_SET .. ID_SET+5
    private static final int ID_DEFAULT = 30; // 4 default-mode buttons
    private static final int ID_ALLOW = 40; // 4 allowed-mode toggles
    private static final int ID_DENY = 50, ID_DESPAWNITEMS = 51;

    private final GuiScreen parent;
    private final ServerConfig working;
    private final boolean canEdit;
    private final ScrollPane pane = new ScrollPane();

    private final List<String> headerKeys = new ArrayList<String>();
    private final List<Integer> headerY = new ArrayList<Integer>();

    private final GuiTextField[] numFields = new GuiTextField[NUM_ROWS];
    private final int[] numY = new int[NUM_ROWS];
    private final String[] numKeys = {
            "cmsall.gui.num.maxblocks", "cmsall.gui.num.pertick", "cmsall.gui.num.despawnsec",
            "cmsall.gui.num.editperm", "cmsall.gui.num.durability", "cmsall.gui.num.exhaustion"
    };

    public CmsAllAdvancedGui(GuiScreen parent, ServerConfig working, boolean canEdit) {
        this.parent = parent;
        this.working = working;
        this.canEdit = canEdit;
    }

    private int panelW() {
        return Math.max(40, Math.min(BASE_W, this.width - 20));
    }

    private int left() {
        return this.width / 2 - panelW() / 2;
    }

    private int colW() {
        return (panelW() - 4) / 2;
    }

    private int col2() {
        return left() + colW() + 4;
    }

    private int doneY() {
        return this.height - 28;
    }

    private static boolean decimal(int row) {
        return row >= 4; // durability, exhaustion
    }

    private String current(int row) {
        switch (row) {
            case 0:
                return Integer.toString(working.globalMaxBlocks);
            case 1:
                return Integer.toString(working.perTickBudget);
            case 2:
                return Integer.toString(working.despawnSeconds);
            case 3:
                return Integer.toString(working.editPermissionLevel);
            case 4:
                return trim(working.durabilityPerBlock);
            default:
                return trim(working.exhaustionPerBlock);
        }
    }

    private static String trim(double d) {
        if (d == Math.rint(d)) {
            return Long.toString((long) d);
        }
        return Double.toString(d);
    }

    @Override
    public void initGui() {
        headerKeys.clear();
        headerY.clear();
        this.buttonList.clear();
        int x = left();
        int w = panelW();
        int cw = colW();
        int x2 = col2();
        int y = 44;
        pane.reset(44, doneY() - 4);

        header("cmsall.gui.cat.general", y);
        y += 12;
        addToggle(ID_ALLOWCREATIVE, x, y, cw, "cmsall.gui.allowcreative", working.allowInCreative);
        addToggle(ID_CHAINFX, x2, y, cw, "cmsall.gui.chaineffects", working.chainBreakEffects);
        y += 24;

        header("cmsall.gui.cat.protection", y);
        y += 12;
        addToggle(ID_PROTECTPERBLOCK, x, y, cw, "cmsall.gui.protectperblock", working.respectProtectionPerBlock);
        addToggle(ID_RECORD, x2, y, cw, "cmsall.gui.recordcommand", working.recordCommandPlace);
        y += 24;

        header("cmsall.gui.cat.limits", y);
        y += 12;
        int setW = 40;
        int boxW = 80;
        int boxX = x + w - setW - 4 - boxW;
        int setX = x + w - setW;
        for (int row = 0; row < NUM_ROWS; row++) {
            numY[row] = y;
            GuiTextField field = new GuiTextField(row, this.fontRenderer, boxX, y, boxW, 18);
            field.setMaxStringLength(12);
            field.setText(current(row));
            field.setEnabled(canEdit);
            numFields[row] = field;
            pane.add(field, y);

            GuiButton set = new GuiButton(ID_SET + row, setX, y - 1, setW, 20, I18n.format("cmsall.gui.track.set"));
            set.enabled = canEdit;
            this.buttonList.add(set);
            pane.add(set, y - 1);
            y += 26;
        }
        y += 2;

        header("cmsall.gui.cat.modes", y);
        y += 12;
        ActivationMode[] modes = ActivationMode.values();
        int dw = (w - (modes.length - 1) * 4) / modes.length;
        for (int i = 0; i < modes.length; i++) {
            ActivationMode mode = modes[i];
            GuiButton b = new GuiButton(ID_DEFAULT + i, x + i * (dw + 4), y, dw, 20, I18n.format("cmsall.mode." + mode.id()));
            b.enabled = canEdit && !mode.id().equals(working.defaultMode);
            this.buttonList.add(b);
            pane.add(b, y);
        }
        y += 24;
        for (int i = 0; i < modes.length; i++) {
            ActivationMode mode = modes[i];
            int bx = (i % 2 == 0) ? x : x2;
            GuiButton b = new GuiButton(ID_ALLOW + i, bx, y, cw, 20, allowLabel(mode));
            b.enabled = canEdit;
            this.buttonList.add(b);
            pane.add(b, y);
            if (i % 2 == 1) {
                y += 24;
            }
        }
        if (modes.length % 2 == 1) {
            y += 24;
        }

        header("cmsall.gui.cat.targets", y);
        y += 12;
        GuiButton deny = new GuiButton(ID_DENY, x, y, w, 20, I18n.format("cmsall.gui.denylist"));
        this.buttonList.add(deny);
        pane.add(deny, y);
        y += 24;
        GuiButton despawnItems = new GuiButton(ID_DESPAWNITEMS, x, y, w, 20, I18n.format("cmsall.gui.despawnitems"));
        this.buttonList.add(despawnItems);
        pane.add(despawnItems, y);
        pane.relayout();

        this.buttonList.add(new GuiButton(ID_DONE, this.width / 2 - 100, doneY(), 200, 20, I18n.format("gui.done")));
    }

    private void header(String key, int y) {
        headerKeys.add(key);
        headerY.add(Integer.valueOf(y));
    }

    private void addToggle(int id, int x, int y, int w, String key, boolean on) {
        GuiButton b = new GuiButton(id, x, y, w, 20, label(key, on));
        b.enabled = canEdit;
        this.buttonList.add(b);
        pane.add(b, y);
    }

    private String allowLabel(ActivationMode mode) {
        boolean on = working.allowedModes.contains(mode.id());
        return I18n.format("cmsall.gui.allowmode",
                I18n.format("cmsall.mode." + mode.id()),
                I18n.format(on ? "cmsall.gui.on" : "cmsall.gui.off"));
    }

    private static String onOff(boolean on) {
        return I18n.format(on ? "cmsall.gui.on" : "cmsall.gui.off");
    }

    private static String label(String key, boolean on) {
        return I18n.format(key, onOff(on));
    }

    private static String strip(String text, boolean decimal) {
        if (text == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        boolean dot = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= '0' && c <= '9') {
                sb.append(c);
            } else if (decimal && c == '.' && !dot) {
                dot = true;
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private void applyNum(int row) {
        GuiTextField field = numFields[row];
        String text = field.getText();
        if (decimal(row)) {
            double parsed;
            try {
                parsed = Double.parseDouble(text);
            } catch (NumberFormatException e) {
                field.setText(current(row));
                return;
            }
            double v = Math.max(0.0, parsed);
            if (row == 4) {
                working.durabilityPerBlock = v;
            } else {
                working.exhaustionPerBlock = v;
            }
        } else {
            int parsed;
            try {
                parsed = Integer.parseInt(text);
            } catch (NumberFormatException e) {
                field.setText(current(row));
                return;
            }
            switch (row) {
                case 0:
                    working.globalMaxBlocks = Math.min(8192, Math.max(1, parsed));
                    break;
                case 1:
                    working.perTickBudget = Math.min(8192, Math.max(0, parsed));
                    break;
                case 2:
                    working.despawnSeconds = Math.max(1, parsed);
                    break;
                default:
                    working.editPermissionLevel = Math.min(4, Math.max(0, parsed));
                    break;
            }
        }
        field.setText(current(row));
        pushEdit();
    }

    @Override
    protected void actionPerformed(GuiButton b) throws IOException {
        switch (b.id) {
            case ID_DONE:
                this.mc.displayGuiScreen(parent);
                return;
            case ID_ALLOWCREATIVE:
                working.allowInCreative = !working.allowInCreative;
                b.displayString = label("cmsall.gui.allowcreative", working.allowInCreative);
                pushEdit();
                return;
            case ID_CHAINFX:
                working.chainBreakEffects = !working.chainBreakEffects;
                b.displayString = label("cmsall.gui.chaineffects", working.chainBreakEffects);
                pushEdit();
                return;
            case ID_PROTECTPERBLOCK:
                working.respectProtectionPerBlock = !working.respectProtectionPerBlock;
                b.displayString = label("cmsall.gui.protectperblock", working.respectProtectionPerBlock);
                pushEdit();
                return;
            case ID_RECORD:
                working.recordCommandPlace = !working.recordCommandPlace;
                b.displayString = label("cmsall.gui.recordcommand", working.recordCommandPlace);
                pushEdit();
                return;
            case ID_DENY:
                this.mc.displayGuiScreen(new CmsAllListGui(this, working, canEdit, "cmsall.gui.denylist",
                        working.denylist, false, this::pushEdit));
                return;
            case ID_DESPAWNITEMS:
                this.mc.displayGuiScreen(new CmsAllListGui(this, working, canEdit, "cmsall.gui.despawnitems",
                        working.despawnItems, true, this::pushEdit));
                return;
            default:
                break;
        }
        if (b.id >= ID_SET && b.id < ID_SET + NUM_ROWS) {
            applyNum(b.id - ID_SET);
            return;
        }
        if (b.id >= ID_DEFAULT && b.id < ID_DEFAULT + 4) {
            ActivationMode mode = ActivationMode.values()[b.id - ID_DEFAULT];
            working.defaultMode = mode.id();
            pushEdit();
            rebuild();
            return;
        }
        if (b.id >= ID_ALLOW && b.id < ID_ALLOW + 4) {
            ActivationMode mode = ActivationMode.values()[b.id - ID_ALLOW];
            if (working.allowedModes.contains(mode.id())) {
                working.allowedModes.remove(mode.id());
            } else {
                working.allowedModes.add(mode.id());
            }
            b.displayString = allowLabel(mode);
            pushEdit();
        }
    }

    private void rebuild() {
        this.buttonList.clear();
        this.initGui();
    }

    @Override
    public void updateScreen() {
        for (GuiTextField field : numFields) {
            if (field != null) {
                field.updateCursorCounter();
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        for (int row = 0; row < NUM_ROWS; row++) {
            GuiTextField field = numFields[row];
            if (field != null && field.textboxKeyTyped(typedChar, keyCode)) {
                String t = field.getText();
                String d = strip(t, decimal(row));
                if (!d.equals(t)) {
                    field.setText(d);
                }
                return;
            }
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        for (GuiTextField field : numFields) {
            if (field != null && field.getVisible()) {
                field.mouseClicked(mouseX, mouseY, mouseButton);
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getDWheel();
        if (dWheel != 0 && pane.scrollable()) {
            pane.scrollBy(dWheel > 0 ? -18 : 18);
        }
    }

    private void pushEdit() {
        if (canEdit) {
            CmsAllNetwork.sendEdit(working);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        int x = left();
        this.drawCenteredString(this.fontRenderer, I18n.format("cmsall.gui.advanced"), this.width / 2, 12, 0xFFFFFF);
        for (int i = 0; i < headerKeys.size(); i++) {
            int hy = headerY.get(i).intValue();
            if (pane.visibleAt(hy, 10)) {
                this.fontRenderer.drawString(I18n.format(headerKeys.get(i)), x, hy - pane.offset(), 0xA0A0A0);
            }
        }
        for (int row = 0; row < NUM_ROWS; row++) {
            GuiTextField field = numFields[row];
            if (field != null && field.getVisible()) {
                field.drawTextBox();
                this.fontRenderer.drawString(I18n.format(numKeys[row]), x, numY[row] + 5 - pane.offset(), 0xC8C8C8);
            }
        }
        if (!canEdit) {
            this.drawCenteredString(this.fontRenderer, I18n.format("cmsall.gui.viewonly"),
                    this.width / 2, doneY() - 14, 0xFF5555);
        }
        pane.renderBar(this, left() + panelW());
    }

    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }
}
