package G_Vael.cmsall.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Mouse;

import com.google.gson.Gson;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

import G_Vael.cmsall.config.ClientConfigView;
import G_Vael.cmsall.config.ServerConfig;
import G_Vael.cmsall.core.ActivationMode;
import G_Vael.cmsall.core.Functions;
import G_Vael.cmsall.net.CmsAllNetwork;

/**
 * CM'sALL config screen, 1.12.2. Server tab = the frequently-used settings inline under dim category section
 * headers (scrollable), plus an [Advanced…] button to {@link CmsAllAdvancedGui}; client tab unchanged.
 */
public class CmsAllConfigGui extends GuiScreen {

    private static final Gson GSON = new Gson();
    private static final int BASE_W = 300;

    private static final int ID_TAB_SERVER = 100, ID_TAB_CLIENT = 101, ID_DONE = 199;
    private static final int ID_MODE = 30;
    private static final int ID_TOGGLENOW = 40, ID_REPLANT = 41, ID_PROTECT = 42;
    // server-tab toggles + nav buttons
    private static final int ID_MASTER = 50, ID_GATHER = 51, ID_DROPINV = 52, ID_DESPAWN = 53,
            ID_BREAKLEAVES = 54, ID_LEAFDROPS = 55, ID_CUTFROMTOP = 56;
    private static final int ID_FUNC = 60; // MINE/CUT/DIG edit buttons: ID_FUNC .. ID_FUNC+2
    private static final int ID_TRACKING = 70, ID_ADVANCED = 71;

    private final GuiScreen parent;
    private final boolean clientOnly;
    private final ScrollPane pane = new ScrollPane();
    private ServerConfig working;
    private boolean canEdit;
    private int tab = 0;
    private ActivationMode selectedMode;

    // Drawn section headers (key + logical y), painted in drawScreen() offset by the pane.
    private final List<String> headerKeys = new ArrayList<String>();
    private final List<Integer> headerY = new ArrayList<Integer>();

    private boolean headers;
    private int yModeHdr, yMode0, yToggleNow, yReplant, yProtect;

    public CmsAllConfigGui(GuiScreen parent) {
        this(parent, false);
    }

    /** clientOnly = opened from the mod list with no world: show only the client (world-independent) preferences. */
    public CmsAllConfigGui(GuiScreen parent, boolean clientOnly) {
        this.parent = parent;
        this.clientOnly = clientOnly;
        if (clientOnly) {
            this.tab = 1;
        }
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

    private void computeLayout() {
        headers = this.height >= 312;
        int cy = 52;
        if (headers) {
            yModeHdr = cy;
            cy += 14;
        }
        yMode0 = cy;
        cy += 24 * 4;
        yToggleNow = cy + 6;
        cy += 30;
        yReplant = cy;
        cy += 30;
        yProtect = cy;
    }

    @Override
    public void initGui() {
        if (working == null) {
            working = GSON.fromJson(GSON.toJson(ClientConfigView.get()), ServerConfig.class);
            if (working == null) {
                working = new ServerConfig();
            }
        }
        if (selectedMode == null) {
            selectedMode = ActivationMode.parse(ClientConfig.get().mode, ActivationMode.HOLD);
        }
        canEdit = this.mc.isSingleplayer() || (this.mc.player != null
                && this.mc.player.canUseCommand(ClientConfigView.get().editPermissionLevel, "cmsall"));

        computeLayout();
        this.buttonList.clear();
        if (!clientOnly) {
            int tw = (panelW() - 4) / 2;
            addTab(ID_TAB_SERVER, left(), tw, "cmsall.gui.tab.server", 0);
            addTab(ID_TAB_CLIENT, left() + tw + 4, tw, "cmsall.gui.tab.client", 1);
        }
        if (tab == 0) {
            initServer();
        } else {
            initClient();
        }
        this.buttonList.add(new GuiButton(ID_DONE, this.width / 2 - 100, doneY(), 200, 20, I18n.format("gui.done")));
    }

    private void addTab(int id, int x, int w, String key, int target) {
        GuiButton b = new GuiButton(id, x, 28, w, 20, I18n.format(key));
        b.enabled = tab != target;
        this.buttonList.add(b);
    }

    private void initServer() {
        headerKeys.clear();
        headerY.clear();
        int x = left();
        int w = panelW();
        int cw = colW();
        int x2 = col2();
        int y = 52;
        pane.reset(52, doneY() - 4);

        header("cmsall.gui.cat.general", y);
        y += 12;
        addToggle(ID_MASTER, x, y, w, "cmsall.gui.master", working.enabled);
        y += 24;

        header("cmsall.gui.cat.drops", y);
        y += 12;
        addToggle(ID_GATHER, x, y, cw, "cmsall.gui.gather", working.gatherDrops);
        addToggle(ID_DROPINV, x2, y, cw, "cmsall.gui.dropinv", working.dropToInventory);
        y += 24;
        addToggle(ID_DESPAWN, x, y, cw, "cmsall.gui.despawn", working.despawnEnabled);
        y += 24;

        header("cmsall.gui.cat.tree", y);
        y += 12;
        addToggle(ID_BREAKLEAVES, x, y, cw, "cmsall.gui.breakleaves", working.cutBreakLeaves);
        addToggle(ID_LEAFDROPS, x2, y, cw, "cmsall.gui.leafdrops", working.cutLeafDrops);
        y += 24;
        addToggle(ID_CUTFROMTOP, x, y, cw, "cmsall.gui.cutfromtop", working.cutFromTop);
        y += 24;

        header("cmsall.gui.cat.targets", y);
        y += 12;
        int i = 0;
        for (Functions.Kind kind : Functions.Kind.values()) {
            GuiButton edit = new GuiButton(ID_FUNC + i, x, y, w, 20,
                    I18n.format("cmsall.gui.edit", I18n.format(funcKey(kind))));
            this.buttonList.add(edit);
            pane.add(edit, y);
            y += 24;
            i++;
        }
        GuiButton tracking = new GuiButton(ID_TRACKING, x, y, w, 20, I18n.format("cmsall.gui.tracking"));
        this.buttonList.add(tracking);
        pane.add(tracking, y);
        y += 28;

        GuiButton advanced = new GuiButton(ID_ADVANCED, x, y, w, 20, I18n.format("cmsall.gui.advanced"));
        this.buttonList.add(advanced);
        pane.add(advanced, y);
        pane.relayout();
    }

    private static String funcKey(Functions.Kind kind) {
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

    private void initClient() {
        headerKeys.clear();
        headerY.clear();
        int x = left();
        int w = panelW();
        pane.reset(headers ? yModeHdr : yMode0, doneY() - 4);
        int y = yMode0;
        int i = 0;
        for (ActivationMode m : ActivationMode.values()) {
            GuiButton b = new GuiButton(ID_MODE + i, x, y, w, 20, I18n.format("cmsall.mode." + m.id()));
            b.enabled = m != selectedMode;
            this.buttonList.add(b);
            pane.add(b, y);
            y += 24;
            i++;
        }
        // "Toggle now" is an in-world action; hide it when there's no world (e.g. mod-list client settings).
        if (this.mc != null && this.mc.player != null) {
            GuiButton tg = new GuiButton(ID_TOGGLENOW, x, yToggleNow, w, 20, I18n.format("cmsall.gui.toggle_now"));
            tg.enabled = selectedMode == ActivationMode.TOGGLE;
            this.buttonList.add(tg);
            pane.add(tg, yToggleNow);
        }
        GuiButton replant = new GuiButton(ID_REPLANT, x, yReplant, w, 20, label("cmsall.gui.replant", ClientConfig.get().replant));
        this.buttonList.add(replant);
        pane.add(replant, yReplant);
        GuiButton protect = new GuiButton(ID_PROTECT, x, yProtect, w, 20, label("cmsall.gui.protect", ClientConfig.get().protect));
        this.buttonList.add(protect);
        pane.add(protect, yProtect);
        pane.relayout();
    }

    private static String onOff(boolean on) {
        return I18n.format(on ? "cmsall.gui.on" : "cmsall.gui.off");
    }

    private static String label(String key, boolean on) {
        return I18n.format(key, onOff(on));
    }

    private void pushEdit() {
        if (canEdit) {
            CmsAllNetwork.sendEdit(working);
        }
    }

    @Override
    protected void actionPerformed(GuiButton b) throws IOException {
        switch (b.id) {
            case ID_TAB_SERVER:
                tab = 0;
                rebuild();
                return;
            case ID_TAB_CLIENT:
                tab = 1;
                rebuild();
                return;
            case ID_DONE:
                this.mc.displayGuiScreen(parent);
                return;
            case ID_TOGGLENOW:
                CmsAllClient.runCommand("cmsall toggle");
                return;
            case ID_REPLANT: {
                ClientConfig c = ClientConfig.get();
                c.replant = !c.replant;
                ClientConfig.save();
                b.displayString = label("cmsall.gui.replant", c.replant);
                CmsAllClient.runCommand("cmsall replant " + c.replant);
                return;
            }
            case ID_PROTECT: {
                ClientConfig c = ClientConfig.get();
                c.protect = !c.protect;
                ClientConfig.save();
                b.displayString = label("cmsall.gui.protect", c.protect);
                CmsAllClient.runCommand("cmsall protect " + c.protect);
                return;
            }
            case ID_MASTER:
                working.enabled = !working.enabled;
                b.displayString = label("cmsall.gui.master", working.enabled);
                pushEdit();
                return;
            case ID_GATHER:
                working.gatherDrops = !working.gatherDrops;
                b.displayString = label("cmsall.gui.gather", working.gatherDrops);
                pushEdit();
                return;
            case ID_DROPINV:
                working.dropToInventory = !working.dropToInventory;
                b.displayString = label("cmsall.gui.dropinv", working.dropToInventory);
                pushEdit();
                return;
            case ID_DESPAWN:
                working.despawnEnabled = !working.despawnEnabled;
                b.displayString = label("cmsall.gui.despawn", working.despawnEnabled);
                pushEdit();
                return;
            case ID_BREAKLEAVES:
                working.cutBreakLeaves = !working.cutBreakLeaves;
                b.displayString = label("cmsall.gui.breakleaves", working.cutBreakLeaves);
                pushEdit();
                return;
            case ID_LEAFDROPS:
                working.cutLeafDrops = !working.cutLeafDrops;
                b.displayString = label("cmsall.gui.leafdrops", working.cutLeafDrops);
                pushEdit();
                return;
            case ID_CUTFROMTOP:
                working.cutFromTop = !working.cutFromTop;
                b.displayString = label("cmsall.gui.cutfromtop", working.cutFromTop);
                pushEdit();
                return;
            case ID_TRACKING:
                this.mc.displayGuiScreen(new CmsAllTrackGui(this, working, canEdit));
                return;
            case ID_ADVANCED:
                this.mc.displayGuiScreen(new CmsAllAdvancedGui(this, working, canEdit));
                return;
            default:
                break;
        }
        if (b.id >= ID_FUNC && b.id < ID_FUNC + 3) {
            Functions.Kind kind = Functions.Kind.values()[b.id - ID_FUNC];
            this.mc.displayGuiScreen(new CmsAllFunctionGui(this, working, kind, canEdit));
            return;
        }
        if (b.id >= ID_MODE && b.id < ID_MODE + 4) {
            selectMode(ActivationMode.values()[b.id - ID_MODE]);
        }
    }

    private void selectMode(ActivationMode m) {
        selectedMode = m;
        ClientConfig.get().mode = m.id();
        ClientConfig.save();
        CmsAllClient.runCommand("cmsall mode " + m.id());
        rebuild();
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getDWheel();
        if (dWheel != 0 && pane.scrollable()) {
            pane.scrollBy(dWheel > 0 ? -18 : 18);
        }
    }

    private void rebuild() {
        this.buttonList.clear();
        this.initGui();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        computeLayout();
        int x = left();
        this.drawCenteredString(this.fontRenderer, "CM'sALL", this.width / 2, 12, 0xFFFFFF);
        if (tab == 0) {
            for (int i = 0; i < headerKeys.size(); i++) {
                int hy = headerY.get(i).intValue();
                if (pane.visibleAt(hy, 10)) {
                    this.fontRenderer.drawString(I18n.format(headerKeys.get(i)), x, hy - pane.offset(), 0xA0A0A0);
                }
            }
            if (!canEdit) {
                this.drawCenteredString(this.fontRenderer, I18n.format("cmsall.gui.viewonly"),
                        this.width / 2, doneY() - 14, 0xFF5555);
            }
        } else {
            if (headers && pane.visibleAt(yModeHdr, 10)) {
                this.fontRenderer.drawString(I18n.format("cmsall.lbl.mode"), x, yModeHdr - pane.offset(), 0xA0A0A0);
            }
            this.drawCenteredString(this.fontRenderer, I18n.format("cmsall.gui.keyhint"),
                    this.width / 2, doneY() - 14, 0xCCCCCC);
        }
        pane.renderBar(this, left() + panelW());
    }

    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }
}
