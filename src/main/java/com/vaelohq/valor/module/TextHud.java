package com.vaelohq.valor.module;

import com.vaelohq.valor.ui.Ui;

/** A HUD module that shows one line of text, such as "120 FPS". */
public abstract class TextHud extends HudModule {
    protected static final float SIZE = 6, PAD_X = 5, H = 13;
    protected final Setting.Color color = add(new Setting.Color("color", "Text colour", Setting.Color.PALETTE[0]));

    protected TextHud(String id, String name, String description, Category category, float x, float y) {
        super(id, name, description, category, x, y);
    }

    /** The text to show; preview means sample data (no world). */
    protected abstract String text(boolean preview);

    @Override
    public float width(boolean preview) {
        return textWidth(text(preview), SIZE) + PAD_X * 2;
    }

    @Override
    public float height(boolean preview) {
        return H;
    }

    @Override
    public void draw(boolean preview) {
        String s = text(preview);
        float w = textWidth(s, SIZE) + PAD_X * 2;
        panel(w, H);
        Ui.semibold().draw(s, PAD_X, (H - SIZE * 1.2f) / 2, SIZE, color.value);
    }
}
