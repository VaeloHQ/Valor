package com.vaelohq.valor.ui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

/**
 * Valor account Login / Sign up panel. Valor has no account server yet, so submitting validates the form and says
 * accounts are coming; nothing typed here is stored or sent anywhere.
 */
public class AccountScreen extends ValorScreen {
    private static final int SUBMIT = 0, SWITCH = 1, CLOSE = 2, SKIP = 3;
    private final GuiScreen next;
    private final boolean atLaunch;
    private final ValorTextField user = new ValorTextField("Username", false), email = new ValorTextField("Email", false),
            pass = new ValorTextField("Password", true), confirm = new ValorTextField("Confirm password", true);
    private boolean signup, remember;
    private float knob;
    private String message = "";
    private boolean messageIsError;
    private float px, py, pw, ph;

    /** @param atLaunch shown before the main menu: adds "Skip for now" and closes to next */
    public AccountScreen(GuiScreen next, boolean atLaunch) {
        this.next = next;
        this.atLaunch = atLaunch;
        user.focus(true);
    }

    private ValorTextField[] fields() {
        return signup ? new ValorTextField[]{user, email, pass, confirm} : new ValorTextField[]{user, pass};
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        pw = 176;
        ph = signup ? 190 : 158;
        px = (width - pw) / 2;
        py = Math.max(6, (height - ph) / 2);
        float fx = px + 16, fw = pw - 32, fy = py + 50;
        for (ValorTextField f : fields()) {
            f.place(fx, fy, fw, 16);
            fy += 21;
        }
        float by = signup ? fy + 10 : fy + 18;
        long t = opened + 120;
        buttonList.add(new Widgets.Pill(SUBMIT, fx, by, fw, 16, 3, -1, signup ? "Create account" : "Log in", true, t));
        String sw = signup ? "Already have an account? Log in" : "Don't have an account? Sign up";
        buttonList.add(new Widgets.Link(SWITCH, px + (pw - Ui.regular().width(sw, 5.2f)) / 2, by + 22, sw, 5.2f, 0xFF8FB4FF, t));
        buttonList.add(new Widgets.DockIcon(CLOSE, px + pw - 17, py + 5, 12, Ui.I_CLOSE, atLaunch ? "Skip" : "Close", false, t));
        if (atLaunch) {
            String s = "Skip for now";
            buttonList.add(new Widgets.Link(SKIP, (width - Ui.regular().width(s, 5.5f)) / 2, py + ph + 8, s, 5.5f, Ui.MUTED, t));
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    protected void drawContent(int mouseX, int mouseY) {
        Ui.rect(0, 0, width, height, 0x59000000);
        float a = Ui.ease(time() / 0.4f), y = py + (1 - a) * 6;
        Shapes.draw(px, y, pw, ph, 9, Ui.alpha(0xF20E1120, a), Ui.alpha(0xF20B1636, a), Ui.alpha(0x804C6BFF, a), 0.7f,
                Ui.alpha(0x662563FF, a), 14, 4);
        float d = y - py;
        float lx = px + (pw - (17 + 5 + Ui.bold().width("Valor", 15))) / 2;
        Ui.logo(lx, y + 15, 17, a);
        Ui.bold().draw("Valor", lx + 22, y + 16, 15, Ui.alpha(Ui.TEXT, a));
        for (ValorTextField f : fields()) {
            f.y += d;
            f.draw(a);
            f.y -= d;
        }
        float below = fields()[fields().length - 1].y + 16 + d;
        if (!signup) {
            Ui.regular().draw("Remember me", px + 16, below + 6, 5.5f, Ui.alpha(0xFFC9D2E8, a));
            knob = Ui.approach(knob, remember ? 1 : 0);
            float tx = px + pw - 16 - 18, ty = below + 5.5f;
            Shapes.round(tx, ty, 18, 9, 4.5f, Ui.alpha(Ui.mix(0xFF2A2F42, Ui.PRIMARY, knob), a));
            Shapes.draw(tx + 1.2f + knob * 9, ty + 1.2f, 6.6f, 6.6f, 3.3f, Ui.alpha(0xFFFFFFFF, a), Ui.alpha(0xFFE6EBF7, a), 0, 0,
                    Ui.alpha(0x66000000, a), 1.5f, 0.6f);
        } else {
            Ui.regular().drawCentered("More content coming soon.", px + pw / 2, below + 3, 5, Ui.alpha(Ui.MUTED, a));
        }
        if (!message.isEmpty())
            Ui.regular().drawCentered(message, px + pw / 2, y + ph - 13, 5, Ui.alpha(messageIsError ? Ui.DANGER : 0xFF8FB4FF, a));
    }

    private boolean onToggle(int mx, int my) {
        float below = fields()[fields().length - 1].y + 16;
        return !signup && mx >= px + 16 && mx < px + pw - 16 && my >= below + 3 && my < below + 17;
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws java.io.IOException {
        for (ValorTextField f : fields()) {
            if (f.onEye(mx, my)) f.reveal = !f.reveal;
            f.focus(f.inside(mx, my));
        }
        if (onToggle(mx, my)) remember = !remember;
        super.mouseClicked(mx, my, button);
    }

    @Override
    protected void keyTyped(char c, int key) {
        ValorTextField[] fs = fields();
        int cur = -1;
        for (int i = 0; i < fs.length; i++) if (fs[i].focused) cur = i;
        if (key == Keyboard.KEY_ESCAPE) close();
        else if (key == Keyboard.KEY_TAB) {
            int nxt = (cur + (isShiftKeyDown() ? fs.length - 1 : 1)) % fs.length;
            for (int i = 0; i < fs.length; i++) fs[i].focus(i == nxt);
        } else if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) submit();
        else if (cur >= 0) fs[cur].key(c, key);
    }

    @Override
    protected void actionPerformed(GuiButton b) {
        switch (b.id) {
            case SUBMIT: submit(); break;
            case SWITCH:
                signup = !signup;
                message = "";
                for (ValorTextField f : new ValorTextField[]{user, email, pass, confirm}) f.error = false;
                user.focus(true);
                for (ValorTextField f : new ValorTextField[]{email, pass, confirm}) f.focus(false);
                initGui();
                break;
            case CLOSE:
            case SKIP: close(); break;
        }
    }

    private void submit() {
        String err = null;
        ValorTextField bad = null;
        if (user.text.trim().isEmpty()) { err = "Enter your username."; bad = user; }
        else if (signup && !user.text.matches("[A-Za-z0-9_]{3,16}")) { err = "Usernames are 3-16 letters, numbers or _."; bad = user; }
        else if (signup && !email.text.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) { err = "Enter a valid email address."; bad = email; }
        else if (pass.text.isEmpty()) { err = "Enter your password."; bad = pass; }
        else if (signup && pass.text.length() < 8) { err = "Use at least 8 characters for your password."; bad = pass; }
        else if (signup && !pass.text.equals(confirm.text)) { err = "Passwords don't match."; bad = confirm; }
        messageIsError = err != null;
        if (bad != null) {
            bad.error = true;
            for (ValorTextField f : fields()) f.focus(f == bad);
            message = err;
            return;
        }
        message = "Valor accounts aren't live yet. Nothing was saved.";
        toast("Accounts are coming soon");
    }

    private void close() {
        mc.displayGuiScreen(next);
    }
}
