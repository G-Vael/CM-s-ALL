package g_vael.cmsall.client;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractWidget;

/** Vertical scroller for a fixed set of widgets: register each with its logical Y, then relayout() offsets+hides them to fit a viewport. No-op (no scroll) when everything fits. 1.16.5 widget port. */
public final class ScrollPane {
    private final List<AbstractWidget> widgets = new ArrayList<>();
    private final List<Integer> baseY = new ArrayList<>();
    private int top;
    private int bottom;
    private int contentBottom;
    private int scroll;

    /** Begin a new layout pass over [top, bottom) (the content viewport, between the header and the Done row). */
    public void reset(int top, int bottom) {
        widgets.clear();
        baseY.clear();
        this.top = top;
        this.bottom = bottom;
        this.contentBottom = top;
        // keep scroll across re-inits so a resize doesn't jump; relayout() re-clamps it.
    }

    /** Register a content widget at its logical (un-scrolled) y. */
    public void add(AbstractWidget w, int y) {
        widgets.add(w);
        baseY.add(y);
        contentBottom = Math.max(contentBottom, y + w.getHeight());
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

    /** Clamp scroll and apply the offset + visibility to every registered widget. Call after add()-ing all. 1.16.5 AbstractWidget has no setY(): set the public y field and visible flag directly. */
    public void relayout() {
        scroll = Math.max(0, Math.min(maxScroll(), scroll));
        for (int i = 0; i < widgets.size(); i++) {
            AbstractWidget w = widgets.get(i);
            int y = baseY.get(i) - scroll;
            w.y = y;
            w.visible = y >= top && y + w.getHeight() <= bottom; // fully inside only — never overlap tabs/Done
        }
    }

    /** Draw a thin scrollbar in the right gutter, just OUTSIDE the panel, when scrollable (never over content). */
    public void renderBar(PoseStack pose, int panelRight) {
        if (!scrollable()) {
            return;
        }
        int trackH = bottom - top;
        int x = panelRight + 1;
        GuiComponent.fill(pose, x, top, x + 2, bottom, 0x40FFFFFF);
        int thumbH = Math.max(12, (int) ((long) trackH * trackH / (trackH + maxScroll())));
        int thumbY = top + (int) ((long) (trackH - thumbH) * scroll / maxScroll());
        GuiComponent.fill(pose, x, thumbY, x + 2, thumbY + thumbH, 0xA0FFFFFF);
    }
}
