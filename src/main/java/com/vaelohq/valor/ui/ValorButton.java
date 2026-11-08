package com.vaelohq.valor.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

/** Left-aligned text control. Cyan when selected; rises into place after showAt. */
public class ValorButton extends GuiButton {
    private final ValorFont font;
    private final float size;
    private final int color;
    private final String tag;
    private final long showAt;
    public boolean selected;

    public ValorButton(int id, int x, int y, String text, ValorFont font, float size, int color, String tag, long showAt) {
        super(id, x, y, (int) Math.ceil(font.width(text, size) + (tag != null ? Ui.regular().width(tag, size * 0.75f) + size * 1.4f : 0)),
                (int) Math.ceil(size * 1.3f), text);
        this.font = font;
        this.size = size;
        this.color = color;
        this.tag = tag;
        this.showAt = showAt;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) return;
        hovered = mouseX >= xPosition && mouseY >= yPosition && mouseX < xPosition + width && mouseY < yPosition + height;
        float k = Ui.ease((Minecraft.getSystemTime() - showAt) / 420f);
        if (k <= 0) return;
        float y = yPosition + (1 - k) * 6;
        font.draw(displayString, xPosition, y, size, Ui.alpha(selected ? Ui.CYAN : color, k));
        if (tag != null) {
            float ts = size * 0.75f, tx = xPosition + font.width(displayString, size) + size * 0.6f, tw = Ui.regular().width(tag, ts);
            Ui.outline(tx, y + size * 0.12f, tx + tw + ts * 1.1f, y + size * 1.08f, Ui.alpha(Ui.MUTED, 0.45f * k));
            Ui.regular().draw(tag, tx + ts * 0.55f, y + size * 0.27f, ts, Ui.alpha(Ui.MUTED, k));
        }
    }

    /** Vertical centre of the text, for the selection dot. */
    float midY() {
        return yPosition + size * 0.62f;
    }
}
