package g_vael.cmsall.client;

import java.io.IOException;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;

import g_vael.cmsall.config.ClientConfigView;
import g_vael.cmsall.config.ServerConfig;
import g_vael.cmsall.core.Functions;
import g_vael.cmsall.net.CmsAllNetwork;

/** Per-function placed-block tracking editor, 1.12.2. */
public class CmsAllTrackGui extends GuiScreen {

    private static final int BASE_W = 300;

    private static final int ID_ENABLE = 10, ID_SET = 20, ID_RESET = 30;
    private static final int ID_OVERFLOW = 40, ID_RESET_ALL = 50, ID_STATUS = 51, ID_REFRESH = 52, ID_DONE = 199;

    private final GuiScreen parent;
    private final ServerConfig working;
    private final boolean canEdit;

    private final GuiTextField[] maxFields = new GuiTextField[3];

    private int refreshCd;
    private GuiButton refreshBtn;

    public CmsAllTrackGui(GuiScreen parent, ServerConfig working, boolean canEdit) {
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

    private int doneY() {
        return this.height - 28;
    }

    private boolean enabled(Functions.Kind kind) {
        switch (kind) {
            case MINE:
                return working.trackMine;
            case CUT:
                return working.trackCut;
            case DIG:
                return working.trackDig;
            default:
                return false;
        }
    }

    private void setEnabled(Functions.Kind kind, boolean v) {
        switch (kind) {
            case MINE:
                working.trackMine = v;
                break;
            case CUT:
                working.trackCut = v;
                break;
            case DIG:
                working.trackDig = v;
                break;
        }
    }

    private int max(Functions.Kind kind) {
        switch (kind) {
            case MINE:
                return working.trackMineMax;
            case CUT:
                return working.trackCutMax;
            case DIG:
                return working.trackDigMax;
            default:
                return 0;
        }
    }

    private void setMax(Functions.Kind kind, int v) {
        switch (kind) {
            case MINE:
                working.trackMineMax = v;
                break;
            case CUT:
                working.trackCutMax = v;
                break;
            case DIG:
                working.trackDigMax = v;
                break;
        }
    }

    private static String funcName(Functions.Kind kind) {
        switch (kind) {
            case MINE:
                return "MineAll";
            case CUT:
                return "CutAll";
            case DIG:
                return "DigAll";
            default:
                return "";
        }
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        int x = left();
        int w = panelW();
        int third = (w - 8) / 3;
        int y = 40;
        int i = 0;
        for (Functions.Kind kind : Functions.Kind.values()) {
            // line 1: enable toggle (full width)
            GuiButton enable = new GuiButton(ID_ENABLE + i, x, y, w, 20, enableLabel(kind));
            enable.enabled = canEdit;
            this.buttonList.add(enable);
            y += 22;

            // line 2: 1 < [input] < 131072  [Set][Reset]  (bounds labels drawn in drawScreen)
            GuiTextField field = new GuiTextField(i, this.fontRenderer, x + 16, y + 1, 52, 18);
            field.setMaxStringLength(7);
            field.setText(Integer.toString(max(kind)));
            field.setEnabled(canEdit);
            maxFields[i] = field;

            GuiButton set = new GuiButton(ID_SET + i, x + w - 86, y, 40, 20, I18n.format("cmsall.gui.track.set"));
            set.enabled = canEdit;
            this.buttonList.add(set);

            this.buttonList.add(new GuiButton(ID_RESET + i, x + w - 44, y, 44, 20, I18n.format("controls.reset")));
            y += 22;

            y += 16; // line 3: usage bar + gap (drawn in drawScreen)
            i++;
        }

        GuiButton overflow = new GuiButton(ID_OVERFLOW, x, y, w, 20, overflowLabel());
        overflow.enabled = canEdit;
        this.buttonList.add(overflow);
        y += 24;

        this.buttonList.add(new GuiButton(ID_RESET_ALL, x, y, third, 20, I18n.format("cmsall.gui.track.reset_all")));
        this.buttonList.add(new GuiButton(ID_STATUS, x + third + 4, y, w - third - 4, 20, I18n.format("cmsall.gui.track.status")));

        this.buttonList.add(new GuiButton(ID_DONE, this.width / 2 - 100, doneY(), 200, 20, I18n.format("gui.done")));

        refreshBtn = new GuiButton(ID_REFRESH, left() + panelW() - 46, 6, 44, 20, I18n.format("cmsall.gui.track.refresh"));
        refreshBtn.enabled = refreshCd == 0;
        this.buttonList.add(refreshBtn);

        CmsAllNetwork.requestSync(); // pull fresh tracked-block counts for the usage bars
    }

    private String enableLabel(Functions.Kind kind) {
        return I18n.format("cmsall.gui.track.enable", funcName(kind),
                I18n.format(enabled(kind) ? "cmsall.gui.on" : "cmsall.gui.off"));
    }

    private String overflowLabel() {
        boolean stop = "stop".equalsIgnoreCase(working.trackOverflow);
        return I18n.format("cmsall.gui.track.overflow",
                I18n.format(stop ? "cmsall.gui.track.stop" : "cmsall.gui.track.evict"));
    }

    private static String stripDigits(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= '0' && c <= '9') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private void applyMax(Functions.Kind kind) {
        GuiTextField field = maxFields[kind.ordinal()];
        String text = field.getText();
        if (text == null || text.isEmpty()) {
            field.setText(Integer.toString(max(kind)));
            return;
        }
        int parsed;
        try {
            parsed = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            field.setText(Integer.toString(max(kind)));
            return;
        }
        final int newMax = Math.min(131072, Math.max(1, parsed));
        int count = ClientConfigView.counts[kind.ordinal()];
        // revert the field; "Set"/Yes re-applies, "No" leaves the old value showing.
        field.setText(Integer.toString(max(kind)));
        final Functions.Kind k = kind;
        if (newMax < count) {
            confirm(I18n.format("cmsall.gui.track.lowered", Integer.valueOf(count), funcName(kind), Integer.valueOf(newMax)),
                    new Runnable() {
                        @Override
                        public void run() {
                            commitMax(k, newMax);
                        }
                    });
        } else {
            commitMax(kind, newMax);
        }
    }

    private void commitMax(Functions.Kind kind, int newMax) {
        setMax(kind, newMax);
        if (maxFields[kind.ordinal()] != null) {
            maxFields[kind.ordinal()].setText(Integer.toString(newMax));
        }
        pushEdit();
    }

    private void doRefresh() {
        CmsAllNetwork.requestSync();
        refreshCd = 100;
        if (refreshBtn != null) {
            refreshBtn.enabled = false;
        }
    }

    private void confirm(String message, Runnable onYes) {
        this.mc.displayGuiScreen(new CmsAllConfirmGui(this, message, onYes));
    }

    @Override
    protected void actionPerformed(GuiButton b) throws IOException {
        if (b.id == ID_DONE) {
            this.mc.displayGuiScreen(parent);
            return;
        }
        if (b.id == ID_OVERFLOW) {
            working.trackOverflow = "stop".equalsIgnoreCase(working.trackOverflow) ? "evict" : "stop";
            b.displayString = overflowLabel();
            pushEdit();
            return;
        }
        if (b.id == ID_RESET_ALL) {
            confirm(I18n.format("cmsall.gui.track.reset_all_confirm"), new Runnable() {
                @Override
                public void run() {
                    CmsAllClient.runCommand("cmsall track reset all");
                }
            });
            return;
        }
        if (b.id == ID_STATUS) {
            CmsAllClient.runCommand("cmsall track status");
            return;
        }
        if (b.id == ID_REFRESH) {
            doRefresh();
            return;
        }
        if (b.id >= ID_ENABLE && b.id < ID_ENABLE + 3) {
            Functions.Kind kind = Functions.Kind.values()[b.id - ID_ENABLE];
            setEnabled(kind, !enabled(kind));
            b.displayString = enableLabel(kind);
            pushEdit();
            return;
        }
        if (b.id >= ID_SET && b.id < ID_SET + 3) {
            applyMax(Functions.Kind.values()[b.id - ID_SET]);
            return;
        }
        if (b.id >= ID_RESET && b.id < ID_RESET + 3) {
            final Functions.Kind kind = Functions.Kind.values()[b.id - ID_RESET];
            confirm(I18n.format("cmsall.gui.track.reset_confirm", funcName(kind)), new Runnable() {
                @Override
                public void run() {
                    CmsAllClient.runCommand("cmsall track reset " + kind.name().toLowerCase());
                }
            });
        }
    }

    @Override
    public void updateScreen() {
        for (GuiTextField field : maxFields) {
            if (field != null) {
                field.updateCursorCounter();
            }
        }
        if (refreshCd > 0 && --refreshCd == 0 && refreshBtn != null) {
            refreshBtn.enabled = true;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        for (GuiTextField field : maxFields) {
            if (field != null && field.textboxKeyTyped(typedChar, keyCode)) {
                String t = field.getText();
                String d = stripDigits(t);
                if (!d.equals(t)) {
                    field.setText(d); // only rewrite when changed so the caret doesn't snap to the end
                }
                return;
            }
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        for (GuiTextField field : maxFields) {
            if (field != null) {
                field.mouseClicked(mouseX, mouseY, mouseButton);
            }
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
        this.drawCenteredString(this.fontRenderer, I18n.format("cmsall.gui.tracking"), this.width / 2, 12, 0xFFFFFF);

        int x = left();
        int w = panelW();
        int y = 40;
        for (Functions.Kind kind : Functions.Kind.values()) {
            y += 22; // enable row
            this.fontRenderer.drawString("1 <", x, y + 6, 0xA0A0A0);
            this.fontRenderer.drawString("< 131072", x + 72, y + 6, 0xA0A0A0);
            maxFields[kind.ordinal()].drawTextBox();
            y += 22; // input row
            int count = ClientConfigView.counts[kind.ordinal()];
            int m = max(kind);
            int pct = m > 0 ? (int) Math.min(100L, (long) count * 100 / m) : 0;
            String usage = pct + "%  " + count + "/" + m;
            int textX = x + w - this.fontRenderer.getStringWidth(usage);
            int barW = Math.max(20, textX - 6 - x);
            int by = y + 1;
            drawRect(x, by, x + barW, by + 6, 0xFF3A3A3A);
            int fillW = (int) ((long) barW * pct / 100);
            int color = pct >= 90 ? 0xFFE0603A : pct >= 70 ? 0xFFE0C040 : 0xFF55C055;
            if (fillW > 0) {
                drawRect(x, by, x + fillW, by + 6, color);
            }
            this.fontRenderer.drawString(usage, textX, y, 0xC8C8C8);
            y += 16; // bar row
        }
        if (!canEdit) {
            this.drawCenteredString(this.fontRenderer, I18n.format("cmsall.gui.viewonly"),
                    this.width / 2, doneY() - 14, 0xFF5555);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }
}
