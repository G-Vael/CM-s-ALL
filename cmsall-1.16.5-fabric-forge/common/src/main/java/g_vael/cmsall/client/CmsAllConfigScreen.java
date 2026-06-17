package g_vael.cmsall.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import com.google.gson.Gson;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

import g_vael.cmsall.config.ClientConfigView;
import g_vael.cmsall.config.ServerConfig;
import g_vael.cmsall.core.ActivationMode;
import g_vael.cmsall.core.Functions;
import g_vael.cmsall.net.CmsAllNetwork;

/** CM'sALL config screen, 1.16.5. Server tab = frequently-used settings inline under category headers (scrollable) + an Advanced button; client tab unchanged. */
public class CmsAllConfigScreen extends Screen {

    private static final Gson GSON = new Gson();
    private static final int TAB_SERVER = 0;
    private static final int TAB_CLIENT = 1;
    private static final int BASE_W = 300;

    private final Screen parent;
    private final boolean clientOnly;
    private final ScrollPane pane = new ScrollPane();
    private ServerConfig working;
    private boolean canEdit;
    private int tab = TAB_SERVER;
    private ActivationMode selectedMode;

    // Drawn section headers (key + logical y), painted in render() offset by the pane (server tab).
    private final List<String> headerKeys = new ArrayList<>();
    private final List<Integer> headerY = new ArrayList<>();

    // Computed each init/resize (client tab).
    private boolean headers;
    private int yModeHdr, yMode0, yToggleNow, yReplant, yProtect;

    public CmsAllConfigScreen(Screen parent) {
        this(parent, false);
    }

    /** clientOnly = opened from the mod list with no world: show only the client (world-independent) preferences. */
    public CmsAllConfigScreen(Screen parent, boolean clientOnly) {
        super(new TextComponent("CM'sALL"));
        this.parent = parent;
        this.clientOnly = clientOnly;
        if (clientOnly) {
            this.tab = TAB_CLIENT;
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

    private void reinit() {
        if (this.minecraft != null) {
            this.init(this.minecraft, this.width, this.height);
        }
    }

    /** Recompute the client-tab vertical flow from the current screen size. */
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
    protected void init() {
        if (working == null) {
            ServerConfig src = ClientConfigView.get();
            working = GSON.fromJson(GSON.toJson(src), ServerConfig.class);
            if (working == null) {
                working = new ServerConfig();
            }
        }
        if (selectedMode == null) {
            selectedMode = ActivationMode.parse(ClientConfig.get().mode, ActivationMode.HOLD);
        }
        canEdit = minecraft != null && minecraft.player != null
                && (minecraft.hasSingleplayerServer() || minecraft.player.hasPermissions(working.editPermissionLevel));

        computeLayout();

        if (!clientOnly) {
            int tw = (panelW() - 4) / 2;
            addButton(tab(left(), tw, "cmsall.gui.tab.server", TAB_SERVER));
            addButton(tab(left() + tw + 4, tw, "cmsall.gui.tab.client", TAB_CLIENT));
        }

        if (tab == TAB_SERVER) {
            initServerTab();
        } else {
            initClientTab();
        }
    }

    private Button tab(int x, int w, String key, int target) {
        Button b = new Button(x, 28, w, 20, new TranslatableComponent(key), btn -> {
            tab = target;
            reinit();
        });
        b.active = tab != target;
        return b;
    }

    private void initServerTab() {
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
        addToggle(x, y, w, "cmsall.gui.master", () -> working.enabled, v -> working.enabled = v);
        y += 24;

        header("cmsall.gui.cat.drops", y);
        y += 12;
        addToggle(x, y, cw, "cmsall.gui.gather", () -> working.gatherDrops, v -> working.gatherDrops = v);
        addToggle(x2, y, cw, "cmsall.gui.dropinv", () -> working.dropToInventory, v -> working.dropToInventory = v);
        y += 24;
        addToggle(x, y, cw, "cmsall.gui.despawn", () -> working.despawnEnabled, v -> working.despawnEnabled = v);
        y += 24;

        header("cmsall.gui.cat.tree", y);
        y += 12;
        addToggle(x, y, cw, "cmsall.gui.breakleaves", () -> working.cutBreakLeaves, v -> working.cutBreakLeaves = v);
        addToggle(x2, y, cw, "cmsall.gui.leafdrops", () -> working.cutLeafDrops, v -> working.cutLeafDrops = v);
        y += 24;
        addToggle(x, y, cw, "cmsall.gui.cutfromtop", () -> working.cutFromTop, v -> working.cutFromTop = v);
        y += 24;

        header("cmsall.gui.cat.targets", y);
        y += 12;
        for (final Functions.Kind kind : Functions.Kind.values()) {
            Button edit = new Button(x, y, w, 20,
                    new TranslatableComponent("cmsall.gui.edit", new TranslatableComponent(funcKey(kind))),
                    btn -> minecraft.setScreen(new CmsAllFunctionScreen(this, working, kind, canEdit)));
            addButton(edit);
            pane.add(edit, y);
            y += 24;
        }
        Button tracking = new Button(x, y, w, 20, new TranslatableComponent("cmsall.gui.tracking"),
                btn -> minecraft.setScreen(new CmsAllTrackScreen(this, working, canEdit)));
        addButton(tracking);
        pane.add(tracking, y);
        y += 28;

        Button advanced = new Button(x, y, w, 20, new TranslatableComponent("cmsall.gui.advanced"),
                btn -> minecraft.setScreen(new CmsAllAdvancedScreen(this, working, canEdit)));
        addButton(advanced);
        pane.add(advanced, y);
        pane.relayout();

        addButton(new Button(this.width / 2 - 100, doneY(), 200, 20, CommonComponents.GUI_DONE, b -> onClose()));
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
        headerY.add(y);
    }

    private void addToggle(int x, int y, int w, final String labelKey,
                           final BooleanSupplier getter, final Consumer<Boolean> setter) {
        Button b = new Button(x, y, w, 20, new TranslatableComponent(labelKey, onOff(getter.getAsBoolean())), btn -> {
            setter.accept(!getter.getAsBoolean());
            btn.setMessage(new TranslatableComponent(labelKey, onOff(getter.getAsBoolean())));
            pushEdit();
        });
        b.active = canEdit;
        addButton(b);
        pane.add(b, y);
    }

    private void pushEdit() {
        if (canEdit) {
            CmsAllNetwork.sendEdit(working);
        }
    }

    private void initClientTab() {
        int x = left();
        int w = panelW();
        pane.reset(headers ? yModeHdr : yMode0, doneY() - 4);
        int y = yMode0;
        for (final ActivationMode mode : ActivationMode.values()) {
            Button b = new Button(x, y, w, 20, new TranslatableComponent("cmsall.mode." + mode.id()), btn -> selectMode(mode));
            b.active = mode != selectedMode;
            addButton(b);
            pane.add(b, y);
            y += 24;
        }
        // "Toggle now" is an in-world action; hide it when there's no server connection (e.g. mod-list client settings).
        if (minecraft != null && minecraft.getConnection() != null) {
            Button toggleNow = new Button(x, yToggleNow, w, 20, new TranslatableComponent("cmsall.gui.toggle_now"),
                    b -> CmsAllClient.runCommand("cmsall toggle"));
            toggleNow.active = selectedMode == ActivationMode.TOGGLE;
            addButton(toggleNow);
            pane.add(toggleNow, yToggleNow);
        }

        Button replant = new Button(x, yReplant, w, 20,
                new TranslatableComponent("cmsall.gui.replant", onOff(ClientConfig.get().replant)), btn -> {
                    ClientConfig cfg = ClientConfig.get();
                    cfg.replant = !cfg.replant;
                    ClientConfig.save();
                    btn.setMessage(new TranslatableComponent("cmsall.gui.replant", onOff(cfg.replant)));
                    CmsAllClient.runCommand("cmsall replant " + cfg.replant);
                });
        addButton(replant);
        pane.add(replant, yReplant);

        Button protect = new Button(x, yProtect, w, 20,
                new TranslatableComponent("cmsall.gui.protect", onOff(ClientConfig.get().protect)), btn -> {
                    ClientConfig cfg = ClientConfig.get();
                    cfg.protect = !cfg.protect;
                    ClientConfig.save();
                    btn.setMessage(new TranslatableComponent("cmsall.gui.protect", onOff(cfg.protect)));
                    CmsAllClient.runCommand("cmsall protect " + cfg.protect);
                });
        addButton(protect);
        pane.add(protect, yProtect);
        pane.relayout();

        addButton(new Button(this.width / 2 - 100, doneY(), 200, 20, CommonComponents.GUI_DONE, b -> onClose()));
    }

    private Component onOff(boolean on) {
        return new TranslatableComponent(on ? "cmsall.gui.on" : "cmsall.gui.off");
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (pane.scrollable()) {
            pane.scrollBy((int) (-delta * 18));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void selectMode(ActivationMode mode) {
        selectedMode = mode;
        ClientConfig.get().mode = mode.id();
        ClientConfig.save();
        CmsAllClient.runCommand("cmsall mode " + mode.id());
        reinit();
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(pose);
        super.render(pose, mouseX, mouseY, partialTick);
        computeLayout();
        int x = left();
        drawCenteredString(pose, this.font, this.title, this.width / 2, 12, 0xFFFFFF);
        if (tab == TAB_SERVER) {
            for (int i = 0; i < headerKeys.size(); i++) {
                int hy = headerY.get(i);
                if (pane.visibleAt(hy, 10)) {
                    drawString(pose, this.font, new TranslatableComponent(headerKeys.get(i)), x, hy - pane.offset(), 0xA0A0A0);
                }
            }
            if (!canEdit) {
                drawCenteredString(pose, this.font, new TranslatableComponent("cmsall.gui.viewonly"),
                        this.width / 2, doneY() - 14, 0xFF5555);
            }
        } else {
            if (headers && pane.visibleAt(yModeHdr, 10)) {
                drawString(pose, this.font, new TranslatableComponent("cmsall.lbl.mode"), x, yModeHdr - pane.offset(), 0xA0A0A0);
            }
            drawCenteredString(pose, this.font, new TranslatableComponent("cmsall.gui.keyhint"),
                    this.width / 2, doneY() - 14, 0xCCCCCC);
        }
        pane.renderBar(pose, left() + panelW());
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }
}
