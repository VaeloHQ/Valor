package com.vaelohq.valor.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

/** Text-only button: slides in after a delay, glows cyan with a growing underline on hover. */
public class ValorButton extends GuiButton {
    private final long showAt;
    private float hover;

    public ValorButton(int id, int centerX, int y, String text, long showAt) {
        super(id, centerX - 60, y, 120, 14, text);
        this.showAt = showAt;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) return;
        hovered = mouseX >= xPosition && mouseY >= yPosition && mouseX < xPosition + width && mouseY < yPosition + height;
        hover += ((hovered ? 1 : 0) - hover) * 0.3f;
        float k = Ui.ease((Minecraft.getSystemTime() - showAt) / 450f);
        if (k <= 0) return;
        ValorFont f = Ui.regular();
        float cx = xPosition + width / 2f, y = yPosition + 3 + (1 - k) * 10, size = 9;
        if (hover > 0.02f) {
            int glow = Ui.alpha(Ui.AZURE, 0.35f * hover * k);
            f.drawCentered(displayString, cx - 0.5f, y, size, glow);
            f.drawCentered(displayString, cx + 0.5f, y, size, glow);
            float lw = f.width(displayString, size) * hover / 2;
            Ui.rect(cx - lw, y + 11, cx + lw, y + 11.5f, Ui.alpha(Ui.CYAN, hover * k));
        }
        f.drawCentered(displayString, cx, y, size, Ui.alpha(Ui.mix(0xFFB8C4E0, Ui.CYAN, hover), k));
    }
}
