package com.vaelohq.valor.ui;

import com.vaelohq.valor.Account;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

/** Valor account panel: Log in, Sign up, Check your email, and Signed in. Talks to Supabase through {@link Account}. */
public class AccountScreen extends ValorScreen {
    private enum Mode { LOGIN, SIGNUP, CHECK, SIGNED_IN }

    private static final int SUBMIT = 0, SWITCH = 1, CLOSE = 2, SKIP = 3, RESEND = 4;
    private final GuiScreen next;
    private final boolean atLaunch;
    private final ValorTextField user = new ValorTextField("Username", false), email = new ValorTextField("Email", false),
            pass = new ValorTextField("Password", true), confirm = new ValorTextField("Confirm password", true);
    private Mode mode;
    private boolean remember = true, busy, canResend, messageIsError;
    private float knob = 1;
    private String message = "", pendingEmail = "";
    private float px, py, pw, ph;

    /** @param atLaunch shown before the main menu: adds "Skip for now" and closes to next */
    public AccountScreen(GuiScreen next, boolean atLaunch) {
        this.next = next;
        this.atLaunch = atLaunch;
        mode = Account.signedIn() ? Mode.SIGNED_IN : Mode.LOGIN;
        email.focus(true);
    }

    private ValorTextField[] fields() {
        switch (mode) {
            case SIGNUP: return new ValorTextField[]{user, email, pass, confirm};
            case LOGIN: return new ValorTextField[]{email, pass};
            default: return new ValorTextField[0];
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        pw = 176;
        float body = mode == Mode.SIGNUP ? 4 * 21 + 18 : mode == Mode.LOGIN ? 2 * 21 + 26 : 44;
        ph = 50 + body + 16 + 22 + (canResend || mode == Mode.CHECK ? 12 : 0) + 22;
        px = (width - pw) / 2;
        py = Math.max(6, (height - ph) / 2);
        float fx = px + 16, fw = pw - 32, fy = py + 50;
        for (ValorTextField f : fields()) {
            f.place(fx, fy, fw, 16);
            fy += 21;
        }
        float by = py + 50 + body;
        long t = opened + 120;
        String label = mode == Mode.SIGNUP ? "Create account" : mode == Mode.LOGIN ? "Log in" : mode == Mode.CHECK ? "Back to log in"
                : atLaunch ? "Continue" : "Log out";
        if (busy) label = mode == Mode.SIGNUP ? "Creating account..." : "Logging in...";
        Widgets.Pill submit = new Widgets.Pill(SUBMIT, fx, by, fw, 16, 3, -1, label, mode != Mode.SIGNED_IN || atLaunch, t);
        submit.enabled = !busy;
        buttonList.add(submit);
        float ly = by + 22;
        if (mode == Mode.LOGIN || mode == Mode.SIGNUP) {
            String sw = mode == Mode.SIGNUP ? "Already have an account? Log in" : "Don't have an account? Sign up";
            buttonList.add(new Widgets.Link(SWITCH, px + (pw - Ui.regular().width(sw, 5.2f)) / 2, ly, sw, 5.2f, 0xFF8FB4FF, t));
            ly += 12;
        } else if (mode == Mode.SIGNED_IN && atLaunch) {
            buttonList.add(new Widgets.Link(SWITCH, px + (pw - Ui.regular().width("Log out", 5.2f)) / 2, ly, "Log out", 5.2f, 0xFF8FB4FF, t));
            ly += 12;
        }
        if (canResend || mode == Mode.CHECK) {
            String r = "Resend confirmation email";
            buttonList.add(new Widgets.Link(RESEND, px + (pw - Ui.regular().width(r, 5.2f)) / 2, ly, r, 5.2f, 0xFF8FB4FF, t));
        }
        buttonList.add(new Widgets.DockIcon(CLOSE, px + pw - 17, py + 5, 12, Ui.I_CLOSE, atLaunch ? "Skip" : "Close", false, t));
        if (atLaunch && mode != Mode.SIGNED_IN) {
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
        float a = Ui.ease(time() / 0.4f), y = py + (1 - a) * 6, d = y - py, cx = px + pw / 2;
        Shapes.draw(px, y, pw, ph, 9, Ui.alpha(0xF20E1120, a), Ui.alpha(0xF20B1636, a), Ui.alpha(0x804C6BFF, a), 0.7f,
                Ui.alpha(0x662563FF, a), 14, 4);
        float lx = px + (pw - (17 + 5 + Ui.bold().width("Valor", 15))) / 2;
        Ui.logo(lx, y + 15, 17, a);
        Ui.bold().draw("Valor", lx + 22, y + 16, 15, Ui.alpha(Ui.TEXT, a));

        for (ValorTextField f : fields()) {
            f.y += d;
            f.draw(a);
            f.y -= d;
        }
        float body = y + 50;
        ValorFont r = Ui.regular(), s = Ui.semibold();
        if (mode == Mode.LOGIN) {
            float row = body + 2 * 21 + 4;
            r.draw("Remember me", px + 16, row + 1, 5.5f, Ui.alpha(0xFFC9D2E8, a));
            knob = Ui.approach(knob, remember ? 1 : 0);
            float tx = px + pw - 16 - 18;
            Shapes.round(tx, row, 18, 9, 4.5f, Ui.alpha(Ui.mix(0xFF2A2F42, Ui.PRIMARY, knob), a));
            Shapes.draw(tx + 1.2f + knob * 9, row + 1.2f, 6.6f, 6.6f, 3.3f, Ui.alpha(0xFFFFFFFF, a), Ui.alpha(0xFFE6EBF7, a), 0, 0,
                    Ui.alpha(0x66000000, a), 1.5f, 0.6f);
        } else if (mode == Mode.SIGNUP) {
            r.drawCentered("More content coming soon.", cx, body + 4 * 21 + 3, 5, Ui.alpha(Ui.MUTED, a));
        } else if (mode == Mode.CHECK) {
            s.drawCentered("Confirm your email", cx, body + 2, 8, Ui.alpha(Ui.TEXT, a));
            r.drawCentered("We sent a link to " + pendingEmail + ".", cx, body + 16, 5.2f, Ui.alpha(0xFFC9D2E8, a));
            r.drawCentered("Open it, then log in here.", cx, body + 25, 5.2f, Ui.alpha(0xFFC9D2E8, a));
        } else {
            r.drawCentered("Signed in as", cx, body + 2, 5.2f, Ui.alpha(Ui.MUTED, a));
            s.drawCentered(Account.username == null ? "" : Account.username, cx, body + 11, 9, Ui.alpha(Ui.TEXT, a));
            r.drawCentered(Account.email == null ? "" : Account.email, cx, body + 25, 5.2f, Ui.alpha(Ui.MUTED, a));
        }
        if (!message.isEmpty())
            r.drawCentered(message, cx, y + ph - 14, 5, Ui.alpha(messageIsError ? Ui.DANGER : 0xFF8FB4FF, a));
    }

    private boolean onToggle(int mx, int my) {
        float row = py + 50 + 2 * 21 + 4;
        return mode == Mode.LOGIN && mx >= px + 16 && mx < px + pw - 16 && my >= row - 2 && my < row + 11;
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
        else if (key == Keyboard.KEY_TAB && fs.length > 0) {
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
                if (mode == Mode.SIGNED_IN) {
                    Account.logOut();
                    setMode(Mode.LOGIN, "Logged out.", false);
                } else setMode(mode == Mode.SIGNUP ? Mode.LOGIN : Mode.SIGNUP, "", false);
                break;
            case RESEND:
                String to = mode == Mode.CHECK ? pendingEmail : email.text.trim();
                busy = true;
                Account.resend(to, res -> {
                    busy = false;
                    show(res.message, !res.ok);
                });
                break;
            case CLOSE:
            case SKIP: close(); break;
        }
    }

    private void setMode(Mode m, String msg, boolean error) {
        mode = m;
        canResend = false;
        for (ValorTextField f : new ValorTextField[]{user, email, pass, confirm}) {
            f.error = false;
            f.focus(false);
        }
        pass.text = confirm.text = "";
        ValorTextField[] fs = fields();
        if (fs.length > 0) fs[0].focus(true);
        message = msg;
        messageIsError = error;
        initGui();
    }

    private void show(String msg, boolean error) {
        message = msg;
        messageIsError = error;
        initGui();
    }

    private void submit() {
        if (busy) return;
        if (mode == Mode.CHECK) {
            setMode(Mode.LOGIN, "", false);
            return;
        }
        if (mode == Mode.SIGNED_IN) {
            if (atLaunch) close();
            else {
                Account.logOut();
                setMode(Mode.LOGIN, "Logged out.", false);
            }
            return;
        }
        String err = null;
        ValorTextField bad = null;
        String mail = email.text.trim();
        if (mode == Mode.SIGNUP && !user.text.matches("[A-Za-z0-9_]{3,16}")) { err = "Usernames are 3-16 letters, numbers or _."; bad = user; }
        else if (!mail.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) { err = "Enter a valid email address."; bad = email; }
        else if (pass.text.isEmpty()) { err = "Enter your password."; bad = pass; }
        else if (mode == Mode.SIGNUP && pass.text.length() < 8) { err = "Use at least 8 characters for your password."; bad = pass; }
        else if (mode == Mode.SIGNUP && !pass.text.equals(confirm.text)) { err = "Passwords don't match."; bad = confirm; }
        if (bad != null) {
            bad.error = true;
            for (ValorTextField f : fields()) f.focus(f == bad);
            show(err, true);
            return;
        }
        busy = true;
        message = "";
        initGui();
        if (mode == Mode.SIGNUP) {
            Account.signUp(user.text, mail, pass.text, res -> {
                busy = false;
                if (!res.ok) show(res.message, true);
                else if (Account.signedIn()) setMode(Mode.SIGNED_IN, res.message, false);
                else {
                    pendingEmail = mail;
                    setMode(Mode.CHECK, "", false);
                }
            });
        } else {
            Account.logIn(mail, pass.text, remember, res -> {
                busy = false;
                if (res.ok) {
                    toast(res.message);
                    if (atLaunch) close();
                    else setMode(Mode.SIGNED_IN, "", false);
                } else {
                    canResend = res.needsConfirm;
                    if (!res.needsConfirm) pass.error = true;
                    show(res.message, true);
                }
            });
        }
    }

    private void close() {
        mc.displayGuiScreen(next);
    }
}
