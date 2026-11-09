package com.vaelohq.valor.module.mods;

import com.vaelohq.valor.module.HudModule;
import com.vaelohq.valor.ui.Ui;

/** A compass strip that scrolls as you turn, with the exact heading underneath. */
public final class DirectionHud extends HudModule {
    private static final float W = 110, H = 22, RANGE = 120;
    private static final String[] LABELS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

    public DirectionHud() {
        super("direction", "Direction HUD", "A compass showing which way you're facing.", Category.HUD, 0.5f, 0);
    }

    @Override
    public boolean visible() {
        return mc.thePlayer != null;
    }

    @Override
    public float width(boolean preview) {
        return W;
    }

    @Override
    public float height(boolean preview) {
        return H;
    }

    @Override
    public void draw(boolean preview) {
        // Minecraft's yaw is 0 facing south; a compass heading is 0 facing north.
        float yaw = preview || mc.thePlayer == null ? 180 : mc.thePlayer.rotationYaw;
        float heading = ((yaw + 180) % 360 + 360) % 360;
        panel(W, H);
        float cx = W / 2, perDegree = (W - 12) / RANGE;
        for (int d = 0; d < 360; d += 15) {
            float off = ((d - heading + 540) % 360) - 180;
            if (Math.abs(off) > RANGE / 2) continue;
            float x = cx + off * perDegree, fade = 1 - Math.abs(off) / (RANGE / 2);
            if (d % 45 == 0) {
                String l = LABELS[d / 45];
                Ui.semibold().drawCentered(l, x, 3, l.length() == 1 ? 6 : 5, Ui.alpha(d == 0 ? Ui.DANGER : 0xFFF4F7FF, fade));
            } else Ui.rect(x - 0.25f, 5, x + 0.25f, 8, Ui.alpha(0xFF9AA6C4, fade));
        }
        Ui.rect(cx - 0.4f, 11, cx + 0.4f, 13, Ui.CYAN);
        Ui.regular().drawCentered(Math.round(heading) % 360 + "°", cx, 14, 4.5f, 0xFF9AA6C4);
    }
}
