package G_Vael.cmsall.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import G_Vael.cmsall.config.ServerConfig;
import G_Vael.cmsall.core.Functions;
import G_Vael.cmsall.net.CmsAllNetwork;

/** Per-function block/tool editor for one of MineAll/CutAll/DigAll. */
public class CmsAllFunctionScreen extends Screen {

    private static final int BASE_W = 320;
    private static final int LIST_TOP = 98;
    private static final int ROW_H = 22;

    private int panelW() {
        return Math.max(40, Math.min(BASE_W, this.width - 20));
    }

    private final Screen parent;
    private final ServerConfig working;
    private final Functions.Kind kind;
    private final boolean canEdit;
    private int serverList = 0; // 0 = blocks, 1 = tools
    private int listScroll;
    private String search = "";

    public CmsAllFunctionScreen(Screen parent, ServerConfig working, Functions.Kind kind, boolean canEdit) {
        super(Component.translatable(titleKey(kind)));
        this.parent = parent;
        this.working = working;
        this.kind = kind;
        this.canEdit = canEdit;
    }

    private static String titleKey(Functions.Kind kind) {
        return switch (kind) {
            case MINE -> "cmsall.func.mine";
            case CUT -> "cmsall.func.cut";
            case DIG -> "cmsall.func.dig";
        };
    }

    List<String> currentList() {
        boolean tool = serverList == 1;
        return switch (kind) {
            case MINE -> tool ? working.mineTools : working.mineBlocks;
            case CUT -> tool ? working.cutTools : working.cutBlocks;
            case DIG -> tool ? working.digTools : working.digBlocks;
        };
    }

    boolean toolMode() {
        return serverList == 1;
    }

    private int panelX() {
        return this.width / 2 - panelW() / 2;
    }

    @Override
    protected void init() {
        int x = panelX();

        Button blocks = Button.builder(Component.translatable("cmsall.gui.list.blocks"), b -> {
            serverList = 0;
            listScroll = 0;
            rebuildWidgets();
        }).bounds(x, 50, 96, 20).build();
        blocks.active = serverList != 0;
        addRenderableWidget(blocks);

        Button tools = Button.builder(Component.translatable("cmsall.gui.list.tools"), b -> {
            serverList = 1;
            listScroll = 0;
            rebuildWidgets();
        }).bounds(x + 100, 50, 96, 20).build();
        tools.active = serverList != 1;
        addRenderableWidget(tools);

        Button add = Button.builder(Component.translatable("cmsall.gui.add"),
                b -> minecraft.setScreen(new CmsAllAddScreen(this, toolMode(), currentList(), kind, this::pushEdit)))
                .bounds(x + panelW() - 96, 50, 96, 20).build();
        add.active = canEdit;
        addRenderableWidget(add);

        EditBox searchBox = new EditBox(this.font, x, 74, panelW(), 18, Component.translatable("cmsall.gui.search"));
        searchBox.setValue(search);
        searchBox.setHint(Component.translatable("cmsall.gui.search"));
        searchBox.setResponder(text -> {
            search = text == null ? "" : text.toLowerCase(Locale.ROOT);
            listScroll = 0;
        });
        addRenderableWidget(searchBox);

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(this.width / 2 - 100, this.height - 28, 200, 20).build());
    }

    private List<RegistryLookup.Entry> entries() {
        List<RegistryLookup.Entry> out = new ArrayList<>();
        boolean tool = toolMode();
        for (String id : currentList()) {
            RegistryLookup.Entry e = RegistryLookup.resolve(tool, id);
            if (search.isEmpty()
                    || id.toLowerCase(Locale.ROOT).contains(search)
                    || e.name().getString().toLowerCase(Locale.ROOT).contains(search)) {
                out.add(e);
            }
        }
        return out;
    }

    private int visibleRows() {
        return Math.max(1, (this.height - 36 - LIST_TOP) / ROW_H);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (canEdit && button == 0) {
            int x = panelX();
            int remX = x + panelW() - 18;
            List<RegistryLookup.Entry> list = entries();
            int rows = visibleRows();
            for (int i = 0; i < rows && listScroll + i < list.size(); i++) {
                int rowY = LIST_TOP + i * ROW_H;
                if (mouseX >= remX && mouseX <= remX + 16 && mouseY >= rowY + 2 && mouseY <= rowY + 18) {
                    currentList().remove(list.get(listScroll + i).id());
                    pushEdit(); // apply the removal immediately — no Save button
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta != 0 && mouseY >= LIST_TOP) {
            int max = Math.max(0, entries().size() - visibleRows());
            listScroll = Math.max(0, Math.min(max, listScroll - (int) Math.signum(delta)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics); // 1.20.1: render() doesn't dim/blur by itself
        super.render(graphics, mouseX, mouseY, partialTick);
        int x = panelX();
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);

        List<RegistryLookup.Entry> list = entries();
        int rows = visibleRows();
        int remX = x + panelW() - 18;
        for (int i = 0; i < rows && listScroll + i < list.size(); i++) {
            RegistryLookup.Entry e = list.get(listScroll + i);
            int rowY = LIST_TOP + i * ROW_H;
            boolean hovered = mouseX >= x && mouseX <= x + panelW() && mouseY >= rowY && mouseY <= rowY + ROW_H - 2;
            graphics.fill(x, rowY, x + panelW(), rowY + ROW_H - 2, hovered ? 0x50FFFFFF : 0x30000000);
            graphics.renderItem(e.icon(), x + 4, rowY + 2);
            graphics.drawString(this.font, e.name(), x + 26, rowY + 1, 0xFFFFFF, false);
            graphics.drawString(this.font, e.id(), x + 26, rowY + 11, 0xA0A0A0, false);
            if (canEdit) {
                graphics.drawString(this.font, Component.literal("✕"), remX + 4, rowY + 6, 0xFF6060, false);
            }
        }
        if (list.isEmpty()) {
            graphics.drawCenteredString(this.font, Component.translatable("cmsall.gui.empty_list"),
                    this.width / 2, LIST_TOP + 8, 0xA0A0A0);
        }
        if (!canEdit) {
            graphics.drawCenteredString(this.font, Component.translatable("cmsall.gui.viewonly"),
                    this.width / 2, this.height - 40, 0xFF5555);
        }
    }

    /** Push the working config to the server now (add/remove apply immediately; ). */
    private void pushEdit() {
        if (canEdit) {
            CmsAllNetwork.sendEdit(working);
        }
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }
}
