package g_vael.cmsall.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

import g_vael.cmsall.config.ServerConfig;
import g_vael.cmsall.core.ActivationMode;
import g_vael.cmsall.net.CmsAllNetwork;

/** 詳細設定: rarely-used server settings, inline under category headers with scroll. */
public class CmsAllAdvancedScreen extends Screen {

    private static final int BASE_W = 300;
    private static final int NUM_ROWS = 6;

    private final Screen parent;
    private final ServerConfig working;
    private final boolean canEdit;
    private final ScrollPane pane = new ScrollPane();

    // Drawn section headers (key + logical y), painted in render() offset by the pane.
    private final List<String> headerKeys = new ArrayList<>();
    private final List<Integer> headerY = new ArrayList<>();

    // Number rows: label drawn at numY[r], an EditBox + Set button.
    private final EditBox[] numFields = new EditBox[NUM_ROWS];
    private final int[] numY = new int[NUM_ROWS];
    private final String[] numKeys = {
            "cmsall.gui.num.maxblocks", "cmsall.gui.num.pertick", "cmsall.gui.num.despawnsec",
            "cmsall.gui.num.editperm", "cmsall.gui.num.durability", "cmsall.gui.num.exhaustion"
    };

    public CmsAllAdvancedScreen(Screen parent, ServerConfig working, boolean canEdit) {
        super(new TranslatableComponent("cmsall.gui.advanced"));
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

    private void reinit() {
        if (this.minecraft != null) {
            this.init(this.minecraft, this.width, this.height);
        }
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
    protected void init() {
        headerKeys.clear();
        headerY.clear();
        int x = left();
        int w = panelW();
        int cw = colW();
        int x2 = col2();
        int y = 44;
        pane.reset(44, doneY() - 4);

        header("cmsall.gui.cat.general", y);
        y += 12;
        addToggle(x, y, cw, "cmsall.gui.allowcreative", () -> working.allowInCreative, v -> working.allowInCreative = v);
        addToggle(x2, y, cw, "cmsall.gui.chaineffects", () -> working.chainBreakEffects, v -> working.chainBreakEffects = v);
        y += 24;

        header("cmsall.gui.cat.protection", y);
        y += 12;
        addToggle(x, y, cw, "cmsall.gui.protectperblock", () -> working.respectProtectionPerBlock, v -> working.respectProtectionPerBlock = v);
        addToggle(x2, y, cw, "cmsall.gui.recordcommand", () -> working.recordCommandPlace, v -> working.recordCommandPlace = v);
        y += 24;

        header("cmsall.gui.cat.limits", y);
        y += 12;
        int setW = 40;
        int boxW = 80;
        int boxX = x + w - setW - 4 - boxW;
        int setX = x + w - setW;
        for (int row = 0; row < NUM_ROWS; row++) {
            numY[row] = y;
            EditBox field = new EditBox(this.font, boxX, y, boxW, 18, new TranslatableComponent(numKeys[row]));
            field.setMaxLength(12);
            field.setValue(current(row));
            field.setFilter(decimal(row) ? CmsAllAdvancedScreen::decimalOnly : CmsAllAdvancedScreen::digitsOnly);
            field.setEditable(canEdit);
            numFields[row] = field;
            addButton(field);
            pane.add(field, y);

            final int r = row;
            Button set = new Button(setX, y - 1, setW, 20, new TranslatableComponent("cmsall.gui.track.set"), btn -> applyNum(r));
            set.active = canEdit;
            addButton(set);
            pane.add(set, y - 1);
            y += 26;
        }
        y += 2;

        header("cmsall.gui.cat.modes", y);
        y += 12;
        ActivationMode[] modes = ActivationMode.values();
        int dw = (w - (modes.length - 1) * 4) / modes.length;
        for (int i = 0; i < modes.length; i++) {
            final ActivationMode mode = modes[i];
            Button b = new Button(x + i * (dw + 4), y, dw, 20, new TranslatableComponent("cmsall.mode." + mode.id()), btn -> {
                working.defaultMode = mode.id();
                pushEdit();
                reinit();
            });
            b.active = canEdit && !mode.id().equals(working.defaultMode);
            addButton(b);
            pane.add(b, y);
        }
        y += 24;
        for (int i = 0; i < modes.length; i++) {
            final ActivationMode mode = modes[i];
            int bx = (i % 2 == 0) ? x : x2;
            Button b = new Button(bx, y, cw, 20, allowLabel(mode), btn -> {
                if (working.allowedModes.contains(mode.id())) {
                    working.allowedModes.remove(mode.id());
                } else {
                    working.allowedModes.add(mode.id());
                }
                btn.setMessage(allowLabel(mode));
                pushEdit();
            });
            b.active = canEdit;
            addButton(b);
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
        Button deny = new Button(x, y, w, 20, new TranslatableComponent("cmsall.gui.denylist"),
                btn -> minecraft.setScreen(new CmsAllListScreen(this, working, canEdit, "cmsall.gui.denylist", working.denylist, false, this::pushEdit)));
        addButton(deny);
        pane.add(deny, y);
        y += 24;
        Button despawnItems = new Button(x, y, w, 20, new TranslatableComponent("cmsall.gui.despawnitems"),
                btn -> minecraft.setScreen(new CmsAllListScreen(this, working, canEdit, "cmsall.gui.despawnitems", working.despawnItems, true, this::pushEdit)));
        addButton(despawnItems);
        pane.add(despawnItems, y);
        pane.relayout();

        addButton(new Button(this.width / 2 - 100, doneY(), 200, 20, CommonComponents.GUI_DONE, b -> onClose()));
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

    private Component allowLabel(ActivationMode mode) {
        boolean on = working.allowedModes.contains(mode.id());
        return new TranslatableComponent("cmsall.gui.allowmode",
                new TranslatableComponent("cmsall.mode." + mode.id()),
                new TranslatableComponent(on ? "cmsall.gui.on" : "cmsall.gui.off"));
    }

    /** EditBox filter: digits only (empty allowed). */
    private static boolean digitsOnly(String s) {
        return s == null || s.chars().allMatch(c -> c >= '0' && c <= '9');
    }

    /** EditBox filter: digits with at most one dot (empty allowed). */
    private static boolean decimalOnly(String s) {
        if (s == null || s.isEmpty()) {
            return true;
        }
        boolean dot = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '.') {
                if (dot) {
                    return false;
                }
                dot = true;
            } else if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    private void applyNum(int row) {
        EditBox field = numFields[row];
        String text = field.getValue();
        if (decimal(row)) {
            double parsed;
            try {
                parsed = Double.parseDouble(text);
            } catch (NumberFormatException e) {
                field.setValue(current(row));
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
                field.setValue(current(row));
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
        field.setValue(current(row));
        pushEdit();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (pane.scrollable()) {
            pane.scrollBy((int) (-delta * 18));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private Component onOff(boolean on) {
        return new TranslatableComponent(on ? "cmsall.gui.on" : "cmsall.gui.off");
    }

    private void pushEdit() {
        if (canEdit) {
            CmsAllNetwork.sendEdit(working);
        }
    }

    @Override
    public void tick() {
        super.tick();
        for (EditBox field : numFields) {
            if (field != null) {
                field.tick();
            }
        }
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(pose);
        super.render(pose, mouseX, mouseY, partialTick);
        int x = left();
        drawCenteredString(pose, this.font, this.title, this.width / 2, 12, 0xFFFFFF);
        for (int i = 0; i < headerKeys.size(); i++) {
            int hy = headerY.get(i);
            if (pane.visibleAt(hy, 10)) {
                drawString(pose, this.font, new TranslatableComponent(headerKeys.get(i)), x, hy - pane.offset(), 0xA0A0A0);
            }
        }
        for (int row = 0; row < NUM_ROWS; row++) {
            int ry = numY[row];
            if (pane.visibleAt(ry, 18)) {
                drawString(pose, this.font, new TranslatableComponent(numKeys[row]), x, ry + 5 - pane.offset(), 0xC8C8C8);
            }
        }
        if (!canEdit) {
            drawCenteredString(pose, this.font, new TranslatableComponent("cmsall.gui.viewonly"),
                    this.width / 2, doneY() - 14, 0xFF5555);
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
