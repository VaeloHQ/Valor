package com.vaelohq.valor.ui;

/**
 * Black "Press to enter" splash shown once per launch before the main menu. Straight after the loading screen it
 * continues that screen: the bar fills, then narrows and fades into "Press to enter" while "Valor" appears.
 */
public class IntroScreen extends ValorScreen {
    private final boolean fromLoading = LoadingScreen.takeHandOff();
    private final float startProgress = LoadingScreen.shown();

    @Override
    protected boolean fadeIn() {
        return !fromLoading;
    }

    @Override
    protected void drawBackground() {
        Ui.rect(0, 0, width, height, 0xFF000000);
    }

    @Override
    protected void drawContent(int mouseX, int mouseY) {
        float t = time(), cx = width / 2f, cy = height / 2f, a, p;
        if (fromLoading) {
            float fill = Ui.ease(t / 0.3f), out = Ui.ease((t - 0.3f) / 0.45f);
            LoadingScreen.draw(width, height, startProgress + (1 - startProgress) * fill, 1 - out, 1 - 0.7f * out);
            a = Ui.ease((t - 0.45f) / 0.6f);
            Ui.semibold().drawCentered("Valor", cx, cy + 8 + (1 - a) * 4, 16, Ui.alpha(Ui.TEXT, a));
            p = Ui.ease((t - 0.6f) / 0.5f);
        } else {
            a = Ui.ease((t - 0.2f) / 0.8f);
            float up = (1 - a) * 6;
            Ui.logo(cx - 24, cy - 44 + up, 48, a);
            Ui.semibold().drawCentered("Valor", cx, cy + 8 + up, 16, Ui.alpha(Ui.TEXT, a));
            p = Ui.ease((t - 0.9f) / 0.6f);
        }
        p *= 0.8f + 0.2f * (float) Math.sin(t * 2.4);
        ValorFont f = Ui.regular();
        f.drawCentered("Press to enter", cx, cy + 32, 8, Ui.alpha(Ui.MUTED, p));
        float lw = f.width("Press to enter", 8) / 2;
        Ui.rect(cx - lw, cy + 42, cx + lw, cy + 42.5f, Ui.alpha(Ui.MUTED, p * 0.7f));
    }

    private void enter() {
        if (time() > 0.4f) mc.displayGuiScreen(com.vaelohq.valor.Account.signedIn() ? new MainMenuScreen() : new AccountScreen(new MainMenuScreen(), true));
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
