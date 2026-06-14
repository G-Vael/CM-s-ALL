package G_Vael.cmsall.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

import G_Vael.cmsall.config.ClientConfigView;
import G_Vael.cmsall.config.ServerConfig;
import G_Vael.cmsall.core.Functions;
import G_Vael.cmsall.net.CmsAllNetwork;

/** Per-function placed-block tracking editor. */
public class CmsAllTrackScreen extends Screen {

    private static final int BASE_W = 300;

    private final Screen parent;
    private final ServerConfig working;
    private final boolean canEdit;

    private final EditBox[] maxFields = new EditBox[3];

    private int refreshCd;
    private Button refreshBtn;

    public CmsAllTrackScreen(Screen parent, ServerConfig working, boolean canEdit) {
        super(new TranslatableComponent("cmsall.gui.tracking"));
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
    protected void init() {
        CmsAllNetwork.requestSync();
        refreshBtn = new Button(left() + panelW() - 46, 6, 44, 18,
                new TranslatableComponent("cmsall.gui.track.refresh"), b -> doRefresh());
        refreshBtn.active = refreshCd == 0;
        addButton(refreshBtn);
        int x = left();
        int w = panelW();
        int third = (w - 8) / 3;
        int y = 40;
        for (final Functions.Kind kind : Functions.Kind.values()) {
            final String name = funcName(kind);
            // line 1: enable toggle (full width)
            Button enable = new Button(x, y, w, 20, enableLabel(kind, name), btn -> {
                setEnabled(kind, !enabled(kind));
                btn.setMessage(enableLabel(kind, name));
                pushEdit();
            });
            enable.active = canEdit;
            addButton(enable);
            y += 22;

            // line 2: 1 < [input] < 131072  [Set][Reset]  (bounds labels drawn in render)
            final EditBox field = new EditBox(this.font, x + 16, y + 1, 52, 18,
                    new TranslatableComponent("cmsall.gui.track.max", name, max(kind)));
            field.setMaxLength(7);
            field.setValue(Integer.toString(max(kind)));
            field.setFilter(CmsAllTrackScreen::digitsOnly);
            field.setEditable(canEdit);
            maxFields[kind.ordinal()] = field;
            addButton(field);

            Button set = new Button(x + w - 86, y, 40, 20, new TranslatableComponent("cmsall.gui.track.set"),
                    btn -> applyMax(kind, name));
            set.active = canEdit;
            addButton(set);

            addButton(new Button(x + w - 44, y, 44, 20, new TranslatableComponent("controls.reset"),
                    btn -> confirm(new TranslatableComponent("cmsall.gui.track.reset_confirm", name),
                            () -> CmsAllClient.runCommand("cmsall track reset " + kind.name().toLowerCase()))));
            y += 22;

            y += 16; // line 3: usage bar + gap (drawn in render)
        }

        Button overflow = new Button(x, y, w, 20, overflowLabel(), btn -> {
            working.trackOverflow = "stop".equalsIgnoreCase(working.trackOverflow) ? "evict" : "stop";
            btn.setMessage(overflowLabel());
            pushEdit();
        });
        overflow.active = canEdit;
        addButton(overflow);
        y += 24;

        addButton(new Button(x, y, third, 20, new TranslatableComponent("cmsall.gui.track.reset_all"),
                btn -> confirm(new TranslatableComponent("cmsall.gui.track.reset_all_confirm"),
                        () -> CmsAllClient.runCommand("cmsall track reset all"))));
        addButton(new Button(x + third + 4, y, w - third - 4, 20, new TranslatableComponent("cmsall.gui.track.status"),
                btn -> CmsAllClient.runCommand("cmsall track status")));

        addButton(new Button(this.width / 2 - 100, doneY(), 200, 20, CommonComponents.GUI_DONE, b -> onClose()));
    }

    /** EditBox filter: accept only all-digit strings (empty allowed so the field can be cleared). */
    private static boolean digitsOnly(String s) {
        return s == null || s.chars().allMatch(c -> c >= '0' && c <= '9');
    }

    private void applyMax(final Functions.Kind kind, String name) {
        EditBox field = maxFields[kind.ordinal()];
        String text = field.getValue();
        if (text == null || text.isEmpty()) {
            field.setValue(Integer.toString(max(kind)));
            return;
        }
        int parsed;
        try {
            parsed = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            field.setValue(Integer.toString(max(kind)));
            return;
        }
        final int newMax = Math.min(131072, Math.max(1, parsed));
        int count = ClientConfigView.counts[kind.ordinal()];
        // revert the field; "Set"/Yes re-applies, "No" leaves the old value showing.
        field.setValue(Integer.toString(max(kind)));
        if (newMax < count) {
            confirm(new TranslatableComponent("cmsall.gui.track.lowered", count, name, newMax),
                    () -> commitMax(kind, newMax));
        } else {
            commitMax(kind, newMax);
        }
    }

    private void commitMax(Functions.Kind kind, int newMax) {
        setMax(kind, newMax);
        if (maxFields[kind.ordinal()] != null) {
            maxFields[kind.ordinal()].setValue(Integer.toString(newMax));
        }
        pushEdit();
    }

    private void confirm(Component message, Runnable onYes) {
        if (minecraft != null) {
            minecraft.setScreen(new CmsAllConfirmScreen(this, message, onYes));
        }
    }

    private Component enableLabel(Functions.Kind kind, String name) {
        return new TranslatableComponent("cmsall.gui.track.enable", name,
                new TranslatableComponent(enabled(kind) ? "cmsall.gui.on" : "cmsall.gui.off"));
    }

    private Component overflowLabel() {
        boolean stop = "stop".equalsIgnoreCase(working.trackOverflow);
        return new TranslatableComponent("cmsall.gui.track.overflow",
                new TranslatableComponent(stop ? "cmsall.gui.track.stop" : "cmsall.gui.track.evict"));
    }

    private void pushEdit() {
        if (canEdit) {
            CmsAllNetwork.sendEdit(working);
        }
    }

    @Override
    public void tick() {
        for (EditBox field : maxFields) {
            if (field != null) {
                field.tick();
            }
        }
        if (refreshCd > 0 && --refreshCd == 0 && refreshBtn != null) {
            refreshBtn.active = true; // re-enable after the ~5s cooldown
        }
    }

    private void doRefresh() {
        CmsAllNetwork.requestSync();
        refreshCd = 100; // ~5s client cooldown so the refresh button can't be spammed
        if (refreshBtn != null) {
            refreshBtn.active = false;
        }
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(pose);
        super.render(pose, mouseX, mouseY, partialTick);
        drawCenteredString(pose, this.font, this.title, this.width / 2, 12, 0xFFFFFF);

        int x = left();
        int w = panelW();
        int y = 40;
        for (Functions.Kind kind : Functions.Kind.values()) {
            y += 22; // enable row
            drawString(pose, this.font, "1 <", x, y + 6, 0xA0A0A0);
            drawString(pose, this.font, "< 131072", x + 72, y + 6, 0xA0A0A0);
            y += 22; // input row
            int count = ClientConfigView.counts[kind.ordinal()];
            int m = max(kind);
            int pct = m > 0 ? (int) Math.min(100L, (long) count * 100 / m) : 0;
            String usage = pct + "%  " + count + "/" + m;
            int textX = x + w - this.font.width(usage);
            int barW = Math.max(20, textX - 6 - x);
            int by = y + 1;
            fill(pose, x, by, x + barW, by + 6, 0xFF3A3A3A);
            int fillW = (int) ((long) barW * pct / 100);
            int color = pct >= 90 ? 0xFFE0603A : pct >= 70 ? 0xFFE0C040 : 0xFF55C055;
            if (fillW > 0) {
                fill(pose, x, by, x + fillW, by + 6, color);
            }
            drawString(pose, this.font, usage, textX, y, 0xC8C8C8);
            y += 16; // bar row
        }
        if (!canEdit) {
            drawCenteredString(pose, this.font, new TranslatableComponent("cmsall.gui.viewonly"),
                    this.width / 2, doneY() - 14, 0xFF5555);
        }
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }
}
