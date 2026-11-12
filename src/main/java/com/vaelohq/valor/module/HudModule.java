package com.vaelohq.valor.module;

import com.vaelohq.valor.ui.Shapes;
import com.vaelohq.valor.ui.Ui;

/**
 * A module drawn on the HUD. Its place is stored as fractions of the free space (0 = left / top edge, 1 = right /
 * bottom edge), so it keeps its spot when the window size changes; the HUD editor moves and scales it.
 */
public abstract class HudModule extends Module {
    /** Gap kept between modules and the screen edge. */
    public static final float MARGIN = 3;

    public float x, y, scale = 1;
    private final float initialX, initialY;
    protected final Setting.Toggle background = add(new Setting.Toggle("background", "Background", true));

    protected HudModule(String id, String name, String description, Category category, float x, float y) {
        super(id, name, description, category);
        this.x = initialX = x;
        this.y = initialY = y;
    }

    @Override
    public void reset() {
        super.reset();
        x = initialX;
        y = initialY;
        scale = 1;
    }

    /** Unscaled size in GUI pixels. preview: no world is loaded (HUD editor from the main menu), so show sample data. */
    public abstract float width(boolean preview);

    public abstract float height(boolean preview);

    /** Draws at (0, 0) in unscaled GUI pixels. */
    public abstract void draw(boolean preview);

    /** Whether there is anything to show right now (outside the editor). */
    public boolean visible() {
        return true;
    }

    public float screenX(float screenW, boolean preview) {
        return MARGIN + x * (screenW - 2 * MARGIN - width(preview) * scale);
    }

    public float screenY(float screenH, boolean preview) {
        return MARGIN + y * (screenH - 2 * MARGIN - height(preview) * scale);
    }

    /** The rounded panel behind a module, when its background is on. */
    protected void panel(float w, float h) {
        if (background.value) Shapes.draw(0, 0, w, h, Math.min(5, h / 2), 0xA8101630, 0xA80A0F24, 0x26FFFFFF, 0.5f, 0, 0, 0);
    }

    protected static float textWidth(String s, float size) {
        return Ui.semibold().width(s, size);
    }
}
