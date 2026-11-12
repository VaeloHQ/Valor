package com.vaelohq.valor.module.mods;

import com.vaelohq.valor.module.Clicks;
import com.vaelohq.valor.module.HudModule;
import com.vaelohq.valor.module.Setting;
import com.vaelohq.valor.ui.Shapes;
import com.vaelohq.valor.ui.Ui;
import com.vaelohq.valor.ui.ValorFont;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;

/** Movement keys, mouse buttons (with CPS) and the space bar, lighting up while held. */
public final class Keystrokes extends HudModule {
    private static final float K = 20, G = 2, W = K * 3 + G * 2;
    private final Setting.Toggle mouse = add(new Setting.Toggle("mouse", "Mouse buttons", true));
    private final Setting.Toggle cps = add(new Setting.Toggle("cps", "CPS on mouse buttons", true));
    private final Setting.Toggle space = add(new Setting.Toggle("space", "Space bar", true));
    private final Setting.Color pressed = add(new Setting.Color("pressed", "Pressed colour", Setting.Color.PALETTE[2]));
    private final float[] glow = new float[7];

    public Keystrokes() {
        super("keystrokes", "Keystrokes", "Shows which movement keys and mouse buttons you're pressing.", Category.PVP, 0, 0.339f);
    }

    @Override
    public float width(boolean preview) {
        return W;
    }

    @Override
    public float height(boolean preview) {
        return K * 2 + G + (mouse.value ? G + K : 0) + (space.value ? G + 9 : 0);
    }

    @Override
    public void draw(boolean preview) {
        GameSettings gs = mc.gameSettings;
        KeyBinding[] keys = {gs.keyBindForward, gs.keyBindLeft, gs.keyBindBack, gs.keyBindRight, gs.keyBindAttack, gs.keyBindUseItem, gs.keyBindJump};
        key(0, K + G, 0, K, K, keys[0], GameSettings.getKeyDisplayString(keys[0].getKeyCode()), null, preview);
        for (int i = 1; i < 4; i++)
            key(i, (i - 1) * (K + G), K + G, K, K, keys[i], GameSettings.getKeyDisplayString(keys[i].getKeyCode()), null, preview);
        float y = (K + G) * 2;
        if (mouse.value) {
            float mw = (W - G) / 2;
            key(4, 0, y, mw, K, keys[4], "LMB", cps.value ? (preview ? 12 : Clicks.left()) + " CPS" : null, preview);
            key(5, mw + G, y, mw, K, keys[5], "RMB", cps.value ? (preview ? 4 : Clicks.right()) + " CPS" : null, preview);
            y += K + G;
        }
        if (space.value) key(6, 0, y, W, 9, keys[6], null, null, preview);
    }

    private void key(int i, float x, float y, float w, float h, KeyBinding k, String label, String sub, boolean preview) {
        glow[i] = Ui.approach(glow[i], !preview && k.isKeyDown() ? 1 : 0);
        float g = glow[i];
        int top = Ui.mix(background.value ? 0xA8101630 : 0x00101630, pressed.value, g), bottom = Ui.mix(background.value ? 0xA80A0F24 : 0x000A0F24, pressed.value, g * 0.85f);
        Shapes.draw(x, y, w, h, Math.min(5, h / 2), top, bottom, Ui.mix(0x26FFFFFF, 0x66FFFFFF, g), 0.5f, 0, 0, 0);
        int text = Ui.mix(0xFFF4F7FF, 0xFFFFFFFF, g);
        ValorFont f = Ui.semibold();
        if (label == null) {   // space bar: a short line
            Ui.rect(x + w / 2 - 8, y + h / 2 - 0.5f, x + w / 2 + 8, y + h / 2 + 0.5f, text);
        } else if (sub == null) {
            f.drawCentered(f.fit(label, 6.5f, w - 4), x + w / 2, y + (h - 6.5f * 1.2f) / 2, 6.5f, text);
        } else {
            f.drawCentered(label, x + w / 2, y + 3.5f, 6, text);
            Ui.regular().drawCentered(sub, x + w / 2, y + 11.5f, 4.5f, Ui.alpha(text, 0.8f));
        }
    }
}
