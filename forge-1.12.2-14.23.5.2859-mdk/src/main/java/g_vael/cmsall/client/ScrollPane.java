package g_vael.cmsall.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;

/**
 * Vertical scroller for a fixed set of 1.12.2 widgets (GuiButton + GuiTextField), ported from the
 * 1.20.1 ScrollPane. Register each widget with its logical (un-scrolled) Y, then relayout() offsets
 * each widget's public x/y by the scroll amount and toggles visibility so it stays inside a viewport.
 * No-op (no scroll) when everything fits. GuiButton exposes public x/y/visible and GuiTextField
 * exposes public x/y plus setVisible(boolean) in this MCP mapping, so both can be offset directly.
 */
public final class ScrollPane {

    /** One registered widget: either a GuiButton or a GuiTextField, with its logical base Y + height. */
    private static final class Item {
        final GuiButton button;
        final GuiTextField field;
        final int baseY;
        final int height;

        Item(GuiButton button, GuiTextField field, int baseY, int height) {
            this.button = button;
            this.field = field;
            this.baseY = baseY;
            this.height = height;
        }
    }

    private final List<Item> items = new ArrayList<Item>();
    private int top;
    private int bottom;
    private int contentBottom;
    private int scroll;

    /** Begin a new layout pass over [top, bottom) (the content viewport, between the header and the Done row). */
    public void reset(int top, int bottom) {
        items.clear();
        this.top = top;
        this.bottom = bottom;
        this.contentBottom = top;
        // keep scroll across re-inits so a resize doesn't jump; relayout() re-clamps it.
    }

    /** Register a GuiButton at its logical (un-scrolled) y. */
    public void add(GuiButton b, int y) {
        items.add(new Item(b, null, y, b.height));
        contentBottom = Math.max(contentBottom, y + b.height);
    }

    /** Register a GuiTextField at its logical (un-scrolled) y. */
    public void add(GuiTextField f, int y) {
        items.add(new Item(null, f, y, f.height));
        contentBottom = Math.max(contentBottom, y + f.height);
    }

    public int maxScroll() {
        return Math.max(0, contentBottom - bottom);
    }

    public int offset() {
        return scroll;
    }

    public boolean scrollable() {
        return maxScroll() > 0;
    }

    /** True only when a logical row [y, y+h) is FULLY inside the viewport (use to skip drawn labels at the edges). */
    public boolean visibleAt(int y, int h) {
        int ay = y - scroll;
        return ay >= top && ay + h <= bottom;
    }

    public void scrollBy(int dy) {
        scroll += dy;
        relayout();
    }

    /** Clamp scroll and apply the offset + visibility to every registered widget. Call after add()-ing all. */
    public void relayout() {
        scroll = Math.max(0, Math.min(maxScroll(), scroll));
        for (Item it : items) {
            int y = it.baseY - scroll;
            boolean vis = y >= top && y + it.height <= bottom; // fully inside only — never overlap tabs/Done
            if (it.button != null) {
                it.button.y = y;
                it.button.visible = vis;
            } else {
                it.field.y = y;
                it.field.setVisible(vis);
            }
        }
    }

    /** Draw a thin scrollbar in the right gutter, just OUTSIDE the panel, when scrollable (never over content). */
    public void renderBar(net.minecraft.client.gui.Gui g, int panelRight) {
        if (!scrollable()) {
            return;
        }
        int trackH = bottom - top;
        int x = panelRight + 1;
        net.minecraft.client.gui.Gui.drawRect(x, top, x + 2, bottom, 0x40FFFFFF);
        int thumbH = Math.max(12, (int) ((long) trackH * trackH / (trackH + maxScroll())));
        int thumbY = top + (int) ((long) (trackH - thumbH) * scroll / maxScroll());
        net.minecraft.client.gui.Gui.drawRect(x, thumbY, x + 2, thumbY + thumbH, 0xA0FFFFFF);
    }
}
