package com.vaelohq.valor.ui;

import com.vaelohq.valor.Account;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

/** Valor account panel: Log in, Sign up (username + password) and Signed in. Talks to Supabase through {@link Account}. */
public class AccountScreen extends ValorScreen {
    private enum Mode { LOGIN, SIGNUP, SIGNED_IN }

    private static final int SUBMIT = 0, SWITCH = 1, CLOSE = 2, SKIP = 3;
    private static final String NAME_RULE = "[A-Za-z0-9_]{3,16}";
    private final GuiScreen next;
    private final boolean atLaunch;
    private final ValorTextField user = new ValorTextField("Username", false), pass = new ValorTextField("Password", true),
            confirm = new ValorTextField("Confirm password", true);
    private Mode mode;
    private boolean remember = true, busy, messageIsError;
    private float knob = 1;
    private String message = "";
    /** Live username check on sign up: the name last asked about, and the answer (null while unknown). */
    private String checked = "", typedName = "";
    private Boolean free;
    private long typedAt;
    private float px, py, pw, ph;

    /** @param atLaunch shown before the main menu: adds "Skip for now" and closes to next */
    public AccountScreen(GuiScreen next, boolean atLaunch) {
        this.next = next;
        this.atLaunch = atLaunch;
        mode = Account.signedIn() ? Mode.SIGNED_IN : Mode.LOGIN;
        user.focus(true);
    }

    private ValorTextField[] fields() {
        switch (mode) {
            case SIGNUP: return new ValorTextField[]{user, pass, confirm};
            case LOGIN: return new ValorTextField[]{user, pass};
            default: return new ValorTextField[0];
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        pw = 176;
        float body = mode == Mode.SIGNUP ? 3 * 21 + 18 : mode == Mode.LOGIN ? 2 * 21 + 26 : 40;
        ph = 50 + body + 16 + (mode == Mode.SIGNED_IN && !atLaunch ? 0 : 12) + 32;
        px = (width - pw) / 2;
        py = Math.max(6, (height - ph) / 2);
        float fx = px + 16, fw = pw - 32, fy = py + 50;
        for (ValorTextField f : fields()) {
            f.place(fx, fy, fw, 16);
            fy += 21;
        }
        float by = py + 50 + body;
        long t = opened + 120;
        String label = mode == Mode.SIGNUP ? "Create account" : mode == Mode.LOGIN ? "Log in" : atLaunch ? "Continue" : "Log out";
        if (busy) label = mode == Mode.SIGNUP ? "Creating account..." : "Logging in...";
        Widgets.Pill submit = new Widgets.Pill(SUBMIT, fx, by, fw, 16, 3, -1, label, mode != Mode.SIGNED_IN || atLaunch, t);
        submit.enabled = !busy;
        buttonList.add(submit);
        String sw = mode == Mode.SIGNUP ? "Already have an account? Log in" : mode == Mode.LOGIN ? "Don't have an account? Sign up"
                : atLaunch ? "Log out" : null;
        if (sw != null) buttonList.add(new Widgets.Link(SWITCH, px + (pw - Ui.regular().width(sw, 5.2f)) / 2, by + 22, sw, 5.2f, 0xFF8FB4FF, t));
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
    public void updateScreen() {
        // Ask whether the username is free 400 ms after the player stops typing it.
        String name = user.text.trim();
        if (!name.equals(typedName)) {
            typedName = name;
            typedAt = Minecraft.getSystemTime();
            free = null;
        }
        if (mode == Mode.SIGNUP && name.matches(NAME_RULE) && !name.equalsIgnoreCase(checked) && Minecraft.getSystemTime() - typedAt > 400) {
            checked = name;
            Account.available(name, f -> {
                if (name.equals(user.text.trim())) free = f;
            });
        }
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
            Widgets.drawSwitch(tx, row, knob, a);
        } else if (mode == Mode.SIGNUP) {
            String name = user.text.trim(), status;
            int color = Ui.MUTED;
            if (name.isEmpty()) status = "3-16 letters, numbers or _";
            else if (!name.matches(NAME_RULE)) { status = "Use 3-16 letters, numbers or _"; color = Ui.DANGER; }
            else if (free == null) status = "Checking username...";
            else if (free) { status = "Username available"; color = Ui.SUCCESS; }
            else { status = "That username is taken"; color = Ui.DANGER; }
            r.drawCentered(status, cx, body + 3 * 21 + 3, 5, Ui.alpha(color, a));
        } else {
            r.drawCentered("Signed in as", cx, body + 4, 5.2f, Ui.alpha(Ui.MUTED, a));
            s.drawCentered(Account.username == null ? "" : Account.username, cx, body + 14, 10, Ui.alpha(Ui.TEXT, a));
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
                if (mode == Mode.SIGNED_IN) logOut();
                else setMode(mode == Mode.SIGNUP ? Mode.LOGIN : Mode.SIGNUP, "", false);
                break;
            case CLOSE:
            case SKIP: close(); break;
        }
    }

    private void setMode(Mode m, String msg, boolean error) {
        mode = m;
        for (ValorTextField f : new ValorTextField[]{user, pass, confirm}) {
            f.error = false;
            f.focus(false);
        }
        pass.text = confirm.text = "";
        checked = "";
        free = null;
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

    private void logOut() {
        Account.logOut();
        setMode(Mode.LOGIN, "Logged out.", false);
    }

    private void submit() {
        if (busy) return;
        if (mode == Mode.SIGNED_IN) {
            if (atLaunch) close();
            else logOut();
            return;
        }
        String err = null, name = user.text.trim();
        ValorTextField bad = null;
        if (!name.matches(NAME_RULE)) { err = "Usernames are 3-16 letters, numbers or _."; bad = user; }
        else if (mode == Mode.SIGNUP && Boolean.FALSE.equals(free)) { err = "That username is taken."; bad = user; }
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
        if (mode == Mode.SIGNUP) Account.signUp(name, pass.text, true, this::finish);
        else Account.logIn(name, pass.text, remember, this::finish);
    }

    private void finish(Account.Result res) {
        busy = false;
        if (!res.ok) {
            if (res.message.contains("username is taken")) user.error = true;
            else if (res.message.startsWith("Wrong username or password")) pass.error = true;
            show(res.message, true);
            return;
        }
        toast(res.message);
        if (atLaunch) close();
        else setMode(Mode.SIGNED_IN, "", false);
    }

    private void close() {
        mc.displayGuiScreen(next);
    }
}
