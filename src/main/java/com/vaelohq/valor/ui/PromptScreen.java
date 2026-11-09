package com.vaelohq.valor.ui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

import java.util.function.Consumer;

/** One-field Valor dialog (for example "Rename world"). The trimmed text goes to the callback, or null when cancelled. */
public class PromptScreen extends ValorScreen {
    private static final int DONE = 0, CANCEL = 1;
    private final GuiScreen behind;
    private final String title, label, confirm;
    private final Consumer<String> answer;
    private final ValorTextField field;
    private float px, py, pw, ph;
    private Widgets.Pill done;

    public PromptScreen(GuiScreen behind, String title, String label, String text, int max, String confirm, Consumer<String> answer) {
        this.behind = behind;
        this.title = title;
        this.label = label;
        this.confirm = confirm;
        this.answer = answer;
        field = new ValorTextField("", false);
        field.text = text;
        field.max = max;
        field.focus(true);
    }

    @Override
    public void initGui() {
        super.initGui();
        behind.setWorldAndResolution(mc, width, height);
        Keyboard.enableRepeatEvents(true);
        pw = 200;
        ph = 98;
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        float fx = px + 16, fw = pw - 32, bw = (fw - 6) / 2;
        field.place(fx, py + 40, fw, 16);
        long t = opened + 80;
        done = new Widgets.Pill(DONE, fx, py + 66, bw, 16, 8, -1, confirm, true, t);
        buttonList.add(done);
        buttonList.add(new Widgets.Pill(CANCEL, fx + bw + 6, py + 66, bw, 16, 8, -1, "Cancel", false, t));
        done.enabled = valid();
    }

    private boolean valid() {
        return !field.text.trim().isEmpty();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    protected void drawBackground() {
        behind.drawScreen(-1, -1, 0);
    }

    @Override
    protected void drawContent(int mouseX, int mouseY) {
        float a = Ui.ease(time() / 0.3f), y = (1 - a) * 5;
        Ui.dialog(width, height, px, py + y, pw, ph, a);
        Ui.semibold().drawCentered(title, px + pw / 2, py + y + 14, 8, Ui.alpha(Ui.TEXT, a));
        Ui.regular().draw(label, field.x + 1, field.y - 8, 5, Ui.alpha(Ui.MUTED, a));
        field.draw(a);
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws java.io.IOException {
        field.focus(field.inside(mx, my));
        super.mouseClicked(mx, my, button);
    }

    @Override
    protected void keyTyped(char c, int key) {
        if (key == Keyboard.KEY_ESCAPE) answer.accept(null);
        else if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) submit();
        else if (field.focused) field.key(c, key);
        done.enabled = valid();
    }

    @Override
    protected void actionPerformed(GuiButton b) {
        if (b.id == DONE) submit();
        else answer.accept(null);
    }

    private void submit() {
        if (valid()) answer.accept(field.text.trim());
    }

    @Override
    public boolean doesGuiPauseGame() {
        return behind.doesGuiPauseGame();
    }
}
