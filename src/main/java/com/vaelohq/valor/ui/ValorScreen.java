package com.vaelohq.valor.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

/** Base for Valor screens: animated background, fade-in on open, and a small toast. */
public abstract class ValorScreen extends GuiScreen {
    private static String toast;
    private static long toastAt;
    protected long opened;

    @Override
    public void initGui() {
        if (opened == 0) opened = Minecraft.getSystemTime();
        buttonList.clear();
    }

    /** Seconds since this screen first opened. */
    protected float time() {
        return (Minecraft.getSystemTime() - opened) / 1000f;
    }

    protected void drawBackground() {
        Background.draw(width, height);
    }

    protected abstract void drawContent(int mouseX, int mouseY);

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawBackground();
        drawContent(mouseX, mouseY);
        super.drawScreen(mouseX, mouseY, partialTicks);
        long age = Minecraft.getSystemTime() - toastAt;
        if (toast != null && age < 2000) {
            float a = Ui.clamp((2000 - age) / 300f) * Ui.ease(age / 200f);
            ValorFont f = Ui.regular();
            float w = f.width(toast, 9) + 16, x = (width - w) / 2, y = 2 + 6 * Ui.ease(age / 200f);
            Ui.rect(x, y, x + w, y + 18, Ui.alpha(Ui.PANEL, a * 0.92f));
            Ui.rect(x, y + 17.5f, x + w, y + 18, Ui.alpha(Ui.CYAN, a));
            f.drawCentered(toast, width / 2f, y + 4.5f, 9, Ui.alpha(Ui.TEXT, a));
        }
        float fade = 1 - Ui.ease(time() / 0.4f);
        if (fade > 0) Ui.rect(0, 0, width, height, Ui.alpha(0xFF000000, fade));
    }

    public static void toast(String text) {
        toast = text;
        toastAt = Minecraft.getSystemTime();
    }
}
