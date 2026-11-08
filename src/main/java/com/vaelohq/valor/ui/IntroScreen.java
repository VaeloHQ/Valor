package com.vaelohq.valor.ui;

/** Black "Press to enter" splash shown once per launch before the main menu. */
public class IntroScreen extends ValorScreen {

    @Override
    protected void drawBackground() {
        Ui.rect(0, 0, width, height, 0xFF000000);
    }

    @Override
    protected void drawContent(int mouseX, int mouseY) {
        float t = time(), cx = width / 2f, cy = height / 2f;
        float a = Ui.ease((t - 0.2f) / 0.8f), up = (1 - a) * 6;
        Ui.logo(cx - 24, cy - 44 + up, 48, a);
        Ui.semibold().drawCentered("Valor", cx, cy + 8 + up, 16, Ui.alpha(Ui.TEXT, a));
        float p = Ui.ease((t - 0.9f) / 0.6f) * (0.8f + 0.2f * (float) Math.sin(t * 2.4));
        ValorFont f = Ui.regular();
        f.drawCentered("Press to enter", cx, cy + 32, 8, Ui.alpha(Ui.MUTED, p));
        float lw = f.width("Press to enter", 8) / 2;
        Ui.rect(cx - lw, cy + 42, cx + lw, cy + 42.5f, Ui.alpha(Ui.MUTED, p * 0.7f));
    }

    private void enter() {
        if (time() > 0.4f) mc.displayGuiScreen(new MainMenuScreen());
    }

    @Override
    protected void keyTyped(char c, int key) {
        enter();
    }

    @Override
    protected void mouseClicked(int x, int y, int button) {
        enter();
    }
}
