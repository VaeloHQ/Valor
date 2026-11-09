package com.vaelohq.valor.ui;

/** Scroll state for a clipped vertical list: eased wheel scrolling, a draggable scrollbar, and keeping a row in view. */
final class ScrollList {
    float x, y, w, h, scroll;
    private float target, content;
    private boolean dragging;

    void place(float x, float y, float w, float h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    /** Starts a frame: eases toward the scroll target and clips drawing to the list (with room for row shadows). */
    void begin(float contentHeight) {
        content = contentHeight;
        target = clamp(target);
        scroll = Ui.approach(scroll, target);
        Ui.clip(x - 8, y, w + 16, h);
    }

    /** Ends clipping and draws the scrollbar when the content is taller than the list. */
    void end(int mx, int my, float a) {
        Ui.endClip();
        if (max() <= 0) return;
        float th = thumb(), ty = y + (h - th) * scroll / max(), sx = x + w + 5;
        boolean on = dragging || (mx >= sx - 2 && mx < sx + 5 && my >= y && my < y + h);
        Shapes.round(sx, ty, 2.5f, th, 1.25f, Ui.alpha(on ? 0x99FFFFFF : 0x40FFFFFF, a));
    }

    boolean contains(int mx, int my) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    void wheel(int dwheel, float step) {
        if (dwheel != 0) target = clamp(target - Math.signum(dwheel) * step);
    }

    /** Starts a scrollbar drag when the press is on the bar; returns true if it took the click. */
    boolean press(int mx, int my) {
        if (max() <= 0 || mx < x + w + 3 || mx >= x + w + 10 || my < y || my >= y + h) return false;
        dragging = true;
        drag(my);
        return true;
    }

    void drag(int my) {
        if (!dragging) return;
        float th = thumb();
        scroll = target = clamp((my - y - th / 2) / (h - th) * max());
    }

    void release() {
        dragging = false;
    }

    /** Scrolls just enough to show the band [top, top + height) of the content. */
    void reveal(float top, float height) {
        if (top < target) target = top;
        else if (top + height > target + h) target = top + height - h;
    }

    private float max() {
        return Math.max(0, content - h);
    }

    private float thumb() {
        return Math.max(16, h * h / content);
    }

    private float clamp(float v) {
        return Math.max(0, Math.min(max(), v));
    }
}
