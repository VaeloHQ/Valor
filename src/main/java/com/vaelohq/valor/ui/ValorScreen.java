package com.vaelohq.valor.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

/** Base for Valor screens: animated background, fade-in on open, and a toast pill. */
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
        Ui.background(width, height);
    }

    protected abstract void drawContent(int mouseX, int mouseY);

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        Ui.frame();
        drawBackground();
        drawContent(mouseX, mouseY);
        super.drawScreen(mouseX, mouseY, partialTicks);
        long age = Minecraft.getSystemTime() - toastAt;
        if (toast != null && age < 2200) {
            float a = Ui.clamp((2200 - age) / 300f) * Ui.ease(age / 220f);
            ValorFont f = Ui.semibold();
            float w = f.width(toast, 6) + 18, x = (width - w) / 2, y = 8 + 4 * Ui.ease(age / 220f);
            Shapes.draw(x, y, w, 15, 7.5f, Ui.alpha(Ui.SURFACE_HI, a), Ui.alpha(Ui.SURFACE_HI, a), Ui.alpha(Ui.BORDER, a), 0.5f,
                    Ui.alpha(0x99000000, a), 5, 2);
            f.drawCentered(toast, width / 2f, y + 4.2f, 6, Ui.alpha(Ui.TEXT, a));
        }
        float fade = fadeIn() ? 1 - Ui.ease(time() / 0.35f) : 0;
        if (fade > 0) Ui.rect(0, 0, width, height, Ui.alpha(0xFF000000, fade));
    }

    /** Whether the screen fades in from black when it opens. */
    protected boolean fadeIn() {
        return true;
    }

    public static void toast(String text) {
        toast = text;
        toastAt = Minecraft.getSystemTime();
    }
}
