package com.vaelohq.valor.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ChatAllowedCharacters;
import org.lwjgl.input.Keyboard;

/** Single-line text input drawn with Valor's font: caret, select-all, paste, optional password masking and reveal. */
final class ValorTextField {
    final String placeholder;
    final boolean password;
    float x, y, w, h;
    String text = "";
    boolean focused, reveal, error;
    int max = 64;
    private int caret;
    private boolean all;
    private long typed;

    ValorTextField(String placeholder, boolean password) {
        this.placeholder = placeholder;
        this.password = password;
    }

    void place(float x, float y, float w, float h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    boolean inside(int mx, int my) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    /** Right-hand eye button area for password fields. */
    boolean onEye(int mx, int my) {
        return password && inside(mx, my) && mx >= x + w - h;
    }

    void draw(float a) {
        caret = Math.min(caret, text.length()); // text may be replaced from outside (e.g. cleared on a mode switch)
        int border = error ? Ui.DANGER : focused ? Ui.PRIMARY_HI : 0x1FFFFFFF;
        Shapes.draw(x, y, w, h, 3, Ui.alpha(0xFF161925, a), Ui.alpha(0xFF12141E, a), Ui.alpha(border, a), focused || error ? 0.8f : 0.5f,
                focused ? Ui.alpha(0x552563FF, a) : 0, focused ? 4 : 0, 0);
        ValorFont f = Ui.regular();
        float size = h * 0.4f, pad = h * 0.36f, ty = y + (h - size * 1.2f) / 2, room = w - pad * 2 - (password ? h * 0.7f : 0);
        String shown = password && !reveal ? stars(text.length()) : text;
        int start = 0;
        while (start < caret && f.width(shown.substring(start, caret), size) > room) start++;
        int end = shown.length();
        while (end > caret && f.width(shown.substring(start, end), size) > room) end--;
        String vis = shown.substring(start, end);
        if (text.isEmpty()) f.draw(placeholder, x + pad, ty, size, Ui.alpha(0xFF6B7591, a));
        else {
            if (all && focused) Ui.rect(x + pad, ty, x + pad + f.width(vis, size), ty + size * 1.2f, Ui.alpha(0x662563FF, a));
            f.draw(vis, x + pad, ty, size, Ui.alpha(Ui.TEXT, a));
        }
        if (focused && (Minecraft.getSystemTime() - typed) % 1000 < 550) {
            float cx = x + pad + f.width(shown.substring(start, caret), size);
            Ui.rect(cx, ty + size * 0.1f, cx + 0.6f, ty + size * 1.15f, Ui.alpha(Ui.TEXT, a));
        }
        if (password) {
            float is = h * 0.42f;
            Ui.icon(reveal ? Ui.I_EYE_OFF : Ui.I_EYE, x + w - pad - is, y + (h - is) / 2, is, Ui.alpha(0xFF8A94AE, a));
        }
    }

    void key(char c, int key) {
        caret = Math.min(caret, text.length());
        typed = Minecraft.getSystemTime();
        error = false;
        if (GuiScreen.isKeyComboCtrlA(key)) {
            all = true;
            caret = text.length();
        } else if (GuiScreen.isKeyComboCtrlV(key)) {
            insert(GuiScreen.getClipboardString());
        } else if (key == Keyboard.KEY_BACK) {
            if (all) clear();
            else if (caret > 0) {
                text = text.substring(0, caret - 1) + text.substring(caret);
                caret--;
            }
        } else if (key == Keyboard.KEY_DELETE) {
            if (all) clear();
            else if (caret < text.length()) text = text.substring(0, caret) + text.substring(caret + 1);
        } else if (key == Keyboard.KEY_LEFT) {
            all = false;
            caret = Math.max(0, caret - 1);
        } else if (key == Keyboard.KEY_RIGHT) {
            all = false;
            caret = Math.min(text.length(), caret + 1);
        } else if (key == Keyboard.KEY_HOME) {
            all = false;
            caret = 0;
        } else if (key == Keyboard.KEY_END) {
            all = false;
            caret = text.length();
        } else if (ChatAllowedCharacters.isAllowedCharacter(c)) {
            insert(String.valueOf(c));
        }
    }

    /** Selects the whole text, so typing replaces it (for renaming). */
    void selectAll() {
        all = !text.isEmpty();
        caret = text.length();
    }

    void focus(boolean f) {
        focused = f;
        all = false;
        caret = text.length();
        typed = Minecraft.getSystemTime();
    }

    private void insert(String s) {
        StringBuilder ok = new StringBuilder();
        for (char ch : s.toCharArray()) if (ChatAllowedCharacters.isAllowedCharacter(ch)) ok.append(ch);
        if (all) clear();
        String t = text.substring(0, caret) + ok + text.substring(caret);
        if (t.length() > max) return;
        text = t;
        caret += ok.length();
    }

    private void clear() {
        text = "";
        caret = 0;
        all = false;
    }

    private static String stars(int n) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < n; i++) b.append('*');
        return b.toString();
    }
}
