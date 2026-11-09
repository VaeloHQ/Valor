package com.vaelohq.valor.ui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import java.util.List;
import java.util.function.Consumer;

/** Valor yes/no dialog. The answer goes to the callback; buttons can stay disabled for a moment to prevent misclicks. */
public class ConfirmScreen extends ValorScreen {
    private final GuiScreen behind;
    private final String title, message, yes, no;
    private final boolean dangerous;
    private final Consumer<Boolean> answer;
    private int delayTicks;
    private float px, py, pw, ph;
    private List<String> lines;

    /**
     * @param behind    screen drawn underneath (null for none); not returned to automatically
     * @param dangerous colours the yes button as destructive
     */
    public ConfirmScreen(GuiScreen behind, String title, String message, String yes, String no, boolean dangerous, Consumer<Boolean> answer) {
        this.behind = behind;
        this.title = title;
        this.message = message;
        this.yes = yes;
        this.no = no;
        this.dangerous = dangerous;
        this.answer = answer;
    }

    /** Keeps both buttons disabled for this many ticks (20 per second). */
    public ConfirmScreen delay(int ticks) {
        delayTicks = ticks;
        return this;
    }

    @Override
    public void initGui() {
        super.initGui();
        pw = 190;
        lines = Ui.regular().wrap(message, 5.6f, pw - 32);
        ph = 34 + (message.isEmpty() ? 0 : lines.size() * 8 + 6) + 16 + 16;
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        float bw = (pw - 32 - 6) / 2, by = py + ph - 16 - 16;
        long t = opened + 80;
        Widgets.Pill y = new Widgets.Pill(1, px + 16, by, bw, 16, 3, -1, yes, !dangerous, t);
        if (dangerous) y.danger();
        buttonList.add(y);
        buttonList.add(new Widgets.Pill(0, px + 16 + bw + 6, by, bw, 16, 3, -1, no, false, t));
        for (GuiButton b : buttonList) b.enabled = delayTicks <= 0;
    }

    @Override
    public void updateScreen() {
        if (delayTicks > 0 && --delayTicks == 0) for (GuiButton b : buttonList) b.enabled = true;
    }

    @Override
    protected void drawContent(int mouseX, int mouseY) {
        float a = Ui.ease(time() / 0.3f), y = py + (1 - a) * 5;
        Ui.rect(0, 0, width, height, Ui.alpha(0x66000000, a));
        Shapes.draw(px, y, pw, ph, 8, Ui.alpha(0xF5101426, a), Ui.alpha(0xF50C1430, a), Ui.alpha(0x40FFFFFF, a), 0.5f,
                Ui.alpha(0xAA000000, a), 12, 4);
        Ui.semibold().drawCentered(title, px + pw / 2, y + 14, 8, Ui.alpha(Ui.TEXT, a));
        float ly = y + 30;
        if (!message.isEmpty()) for (String l : lines) {
            Ui.regular().drawCentered(l, px + pw / 2, ly, 5.6f, Ui.alpha(0xFFC1CADF, a));
            ly += 8;
        }
    }

    @Override
    protected void drawBackground() {
        if (behind != null) behind.drawScreen(-1, -1, 0);
        else super.drawBackground();
    }

    @Override
    protected void actionPerformed(GuiButton b) {
        answer.accept(b.id == 1);
    }

    @Override
    protected void keyTyped(char c, int key) {
        if (key == org.lwjgl.input.Keyboard.KEY_ESCAPE && delayTicks <= 0) answer.accept(false);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return behind == null || behind.doesGuiPauseGame();
    }
}
