package G_Vael.cmsall.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import com.google.gson.Gson;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import G_Vael.cmsall.config.ClientConfigView;
import G_Vael.cmsall.config.ServerConfig;
import G_Vael.cmsall.core.ActivationMode;
import G_Vael.cmsall.core.Functions;
import G_Vael.cmsall.net.CmsAllNetwork;

/** CM'sALL config screen. */
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

    // Drawn section headers (key + logical y), painted in render() offset by the pane.
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
        super(Component.literal("CM'sALL"));
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

    /** Recompute the vertical flow from the current screen size (idempotent; called by init + render). */
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
            addRenderableWidget(tab(left(), tw, "cmsall.gui.tab.server", "cmsall.tip.tab.server", TAB_SERVER));
            addRenderableWidget(tab(left() + tw + 4, tw, "cmsall.gui.tab.client", "cmsall.tip.tab.client", TAB_CLIENT));
        }

        if (tab == TAB_SERVER) {
            initServerTab();
        } else {
            initClientTab();
        }
    }

    private Button tab(int x, int w, String key, String tipKey, int target) {
        Button b = Button.builder(Component.translatable(key), btn -> {
            tab = target;
            rebuildWidgets();
        }).bounds(x, 28, w, 20).tooltip(Tooltip.create(Component.translatable(tipKey))).build();
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
        addToggle(x, y, w, "cmsall.gui.master", "cmsall.tip.master", () -> working.enabled, v -> working.enabled = v);
        y += 24;

        header("cmsall.gui.cat.drops", y);
        y += 12;
        addToggle(x, y, cw, "cmsall.gui.gather", "cmsall.tip.gather", () -> working.gatherDrops, v -> working.gatherDrops = v);
        addToggle(x2, y, cw, "cmsall.gui.dropinv", "cmsall.tip.dropinv", () -> working.dropToInventory, v -> working.dropToInventory = v);
        y += 24;
        addToggle(x, y, cw, "cmsall.gui.despawn", "cmsall.tip.despawn", () -> working.despawnEnabled, v -> working.despawnEnabled = v);
        y += 24;

        header("cmsall.gui.cat.tree", y);
        y += 12;
        addToggle(x, y, cw, "cmsall.gui.breakleaves", "cmsall.tip.breakleaves", () -> working.cutBreakLeaves, v -> working.cutBreakLeaves = v);
        addToggle(x2, y, cw, "cmsall.gui.leafdrops", "cmsall.tip.leafdrops", () -> working.cutLeafDrops, v -> working.cutLeafDrops = v);
        y += 24;
        addToggle(x, y, cw, "cmsall.gui.cutfromtop", "cmsall.tip.cutfromtop", () -> working.cutFromTop, v -> working.cutFromTop = v);
        y += 24;

        header("cmsall.gui.cat.targets", y);
        y += 12;
        for (Functions.Kind kind : Functions.Kind.values()) {
            Button edit = Button.builder(
                    Component.translatable("cmsall.gui.edit", Component.translatable(funcKey(kind))),
                    btn -> minecraft.setScreen(new CmsAllFunctionScreen(this, working, kind, canEdit)))
                    .bounds(x, y, w, 20)
                    .tooltip(Tooltip.create(Component.translatable("cmsall.tip." + kind.name().toLowerCase()))).build();
            addRenderableWidget(edit);
            pane.add(edit, y);
            y += 24;
        }
        Button tracking = Button.builder(Component.translatable("cmsall.gui.tracking"),
                        btn -> minecraft.setScreen(new CmsAllTrackScreen(this, working, canEdit)))
                .bounds(x, y, w, 20).build();
        addRenderableWidget(tracking);
        pane.add(tracking, y);
        y += 28;

        Button advanced = Button.builder(Component.translatable("cmsall.gui.advanced"),
                        btn -> minecraft.setScreen(new CmsAllAdvancedScreen(this, working, canEdit)))
                .bounds(x, y, w, 20)
                .tooltip(Tooltip.create(Component.translatable("cmsall.tip.advanced"))).build();
        addRenderableWidget(advanced);
        pane.add(advanced, y);
        pane.relayout();

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(this.width / 2 - 100, doneY(), 200, 20).build());
    }

    private static String funcKey(Functions.Kind kind) {
        return switch (kind) {
            case MINE -> "cmsall.func.mine";
            case CUT -> "cmsall.func.cut";
            case DIG -> "cmsall.func.dig";
        };
    }

    private void header(String key, int y) {
        headerKeys.add(key);
        headerY.add(y);
    }

    private void addToggle(int x, int y, int w, String labelKey, String tipKey,
                           BooleanSupplier getter, Consumer<Boolean> setter) {
        Button b = Button.builder(Component.translatable(labelKey, onOff(getter.getAsBoolean())), btn -> {
            setter.accept(!getter.getAsBoolean());
            btn.setMessage(Component.translatable(labelKey, onOff(getter.getAsBoolean())));
            pushEdit();
        }).bounds(x, y, w, 20).tooltip(Tooltip.create(Component.translatable(tipKey))).build();
        b.active = canEdit;
        addRenderableWidget(b);
        pane.add(b, y);
    }

    private void pushEdit() {
        if (canEdit) {
            CmsAllNetwork.sendEdit(working);
        }
    }

    private void initClientTab() {
        headerKeys.clear();
        headerY.clear();
        int x = left();
        int w = panelW();
        pane.reset(headers ? yModeHdr : yMode0, doneY() - 4);
        int y = yMode0;
        for (ActivationMode mode : ActivationMode.values()) {
            Button b = Button.builder(Component.translatable("cmsall.mode." + mode.id()), btn -> selectMode(mode))
                    .bounds(x, y, w, 20)
                    .tooltip(Tooltip.create(Component.translatable("cmsall.tip.mode." + mode.id()))).build();
            b.active = mode != selectedMode;
            addRenderableWidget(b);
            pane.add(b, y);
            y += 24;
        }
        // "Toggle now" is an in-world action; hide it when there's no server connection (e.g. mod-list client settings).
        if (minecraft != null && minecraft.getConnection() != null) {
            Button toggleNow = Button.builder(Component.translatable("cmsall.gui.toggle_now"),
                            b -> CmsAllClient.runCommand("cmsall toggle"))
                    .bounds(x, yToggleNow, w, 20)
                    .tooltip(Tooltip.create(Component.translatable("cmsall.tip.toggle_now"))).build();
            toggleNow.active = selectedMode == ActivationMode.TOGGLE;
            addRenderableWidget(toggleNow);
            pane.add(toggleNow, yToggleNow);
        }

        Button replant = Button.builder(
                        Component.translatable("cmsall.gui.replant", onOff(ClientConfig.get().replant)), btn -> {
                            ClientConfig cfg = ClientConfig.get();
                            cfg.replant = !cfg.replant;
                            ClientConfig.save();
                            btn.setMessage(Component.translatable("cmsall.gui.replant", onOff(cfg.replant)));
                            CmsAllClient.runCommand("cmsall replant " + cfg.replant);
                        })
                .bounds(x, yReplant, w, 20)
                .tooltip(Tooltip.create(Component.translatable("cmsall.tip.replant"))).build();
        addRenderableWidget(replant);
        pane.add(replant, yReplant);

        Button protect = Button.builder(
                        Component.translatable("cmsall.gui.protect", onOff(ClientConfig.get().protect)), btn -> {
                            ClientConfig cfg = ClientConfig.get();
                            cfg.protect = !cfg.protect;
                            ClientConfig.save();
                            btn.setMessage(Component.translatable("cmsall.gui.protect", onOff(cfg.protect)));
                            CmsAllClient.runCommand("cmsall protect " + cfg.protect);
                        })
                .bounds(x, yProtect, w, 20)
                .tooltip(Tooltip.create(Component.translatable("cmsall.tip.protect"))).build();
        addRenderableWidget(protect);
        pane.add(protect, yProtect);
        pane.relayout();

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(this.width / 2 - 100, doneY(), 200, 20).build());
    }

    private Component onOff(boolean on) {
        return Component.translatable(on ? "cmsall.gui.on" : "cmsall.gui.off");
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
        rebuildWidgets();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics); // 1.20.1: render() doesn't dim/blur by itself
        super.render(graphics, mouseX, mouseY, partialTick);
        computeLayout();
        int x = left();
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
        if (tab == TAB_SERVER) {
            for (int i = 0; i < headerKeys.size(); i++) {
                int hy = headerY.get(i);
                if (pane.visibleAt(hy, 10)) {
                    graphics.drawString(this.font, Component.translatable(headerKeys.get(i)), x, hy - pane.offset(), 0xA0A0A0, false);
                }
            }
            if (!canEdit) {
                graphics.drawCenteredString(this.font, Component.translatable("cmsall.gui.viewonly"),
                        this.width / 2, doneY() - 14, 0xFF5555);
            }
        } else {
            if (headers && pane.visibleAt(yModeHdr, 10)) {
                graphics.drawString(this.font, Component.translatable("cmsall.lbl.mode"), x, yModeHdr - pane.offset(), 0xA0A0A0, false);
            }
            graphics.drawCenteredString(this.font, Component.translatable("cmsall.gui.keyhint"),
                    this.width / 2, doneY() - 14, 0xCCCCCC);
        }
        pane.renderBar(graphics, left() + panelW());
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }
}
