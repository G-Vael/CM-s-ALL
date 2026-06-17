package g_vael.cmsall.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import g_vael.cmsall.config.ServerConfig;

/** Flat id-list editor (denylist blocks / despawn items): browse + remove; "+ Add" opens the search screen. */
public class CmsAllListScreen extends Screen {

    private static final int BASE_W = 320;
    private static final int LIST_TOP = 74;
    private static final int ROW_H = 22;

    private final Screen parent;
    private final boolean canEdit;
    private final List<String> target;
    private final boolean itemMode;
    private final Runnable onChange;

    private String search = "";
    private int listScroll;

    public CmsAllListScreen(Screen parent, ServerConfig working, boolean canEdit, String titleKey,
                            List<String> target, boolean itemMode, Runnable onChange) {
        super(Component.translatable(titleKey));
        this.parent = parent;
        this.canEdit = canEdit;
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
    protected void init() {
        int x = panelX();
        EditBox searchBox = new EditBox(this.font, x, 50, panelW() - 70, 18, Component.translatable("cmsall.gui.search"));
        searchBox.setValue(search);
        searchBox.setHint(Component.translatable("cmsall.gui.search"));
        searchBox.setResponder(t -> {
            search = t == null ? "" : t.toLowerCase(Locale.ROOT);
            listScroll = 0;
        });
        addRenderableWidget(searchBox);

        Button add = Button.builder(Component.translatable("cmsall.gui.add"),
                        b -> minecraft.setScreen(new CmsAllAddScreen(this, target, itemMode, onChange)))
                .bounds(x + panelW() - 66, 50, 66, 20).build();
        add.active = canEdit;
        addRenderableWidget(add);

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(this.width / 2 - 100, this.height - 28, 200, 20).build());
    }

    /** The registered ids, filtered by the search box. */
    private List<RegistryLookup.Entry> entries() {
        List<RegistryLookup.Entry> out = new ArrayList<>();
        for (String id : target) {
            RegistryLookup.Entry e = RegistryLookup.resolve(itemMode, id);
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
                    target.remove(list.get(listScroll + i).id());
                    onChange.run();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0 && mouseY >= LIST_TOP) {
            int max = Math.max(0, entries().size() - visibleRows());
            listScroll = Math.max(0, Math.min(max, listScroll - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
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

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }
}
