package com.vaelohq.valor.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;

/** Menu controls. Each eases in after showAt, animates hover in about 150 ms, and shows a cyan ring when keyboard-focused. */
public final class Widgets {
    private Widgets() {
    }

    /** On/off switch, 18x9, with knob position k (0 = off, 1 = on). */
    public static void drawSwitch(float x, float y, float k, float a) {
        Shapes.round(x, y, 18, 9, 4.5f, Ui.alpha(Ui.mix(0xFF2A2F42, Ui.PRIMARY, k), a));
        Shapes.draw(x + 1.2f + k * 9, y + 1.2f, 6.6f, 6.6f, 3.3f, Ui.alpha(0xFFFFFFFF, a), Ui.alpha(0xFFE6EBF7, a), 0, 0,
                Ui.alpha(0x66000000, a), 1.5f, 0.6f);
    }

    /** Small 10x10 arrow button: the play icon turned by angle degrees (-90 up, 90 down, 180 left). */
    public static void drawArrow(float x, float y, float angle, boolean enabled, boolean hot, float a) {
        if (hot && enabled) Shapes.round(x, y, 10, 10, 3, Ui.alpha(0x26FFFFFF, a));
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 5, y + 5, 0);
        GlStateManager.rotate(angle, 0, 0, 1);
        Ui.icon(Ui.I_PLAY, -3, -3, 6, Ui.alpha(!enabled ? 0x33FFFFFF : hot ? Ui.TEXT : Ui.MUTED, a));
        GlStateManager.popMatrix();
    }

    /** Valor look for a vanilla slider knob at knobX: a blue fill up to the knob, then a dark knob with a cyan ring (text stays readable over it). */
    public static void sliderKnob(GuiButton b, int knobX) {
        Ui.frame();
        int h = b.height;
        Shapes.draw(b.xPosition, b.yPosition, knobX + 8 - b.xPosition, h, h / 2f, 0x9A3B74FF, 0x9A1F55E8, 0, 0, 0, 0, 0);
        Shapes.draw(knobX, b.yPosition + 2, 8, h - 4, 4, 0xFF13204A, 0xFF0A1230, Ui.CYAN, 1, 0x9900B4FF, 3, 0);
    }

    public abstract static class Base extends GuiButton {
        final long showAt;
        float hover;
        boolean focused;

        Base(int id, float x, float y, float w, float h, String text, long showAt) {
            super(id, Math.round(x), Math.round(y), Math.round(w), Math.round(h), text);
            this.showAt = showAt;
        }

        /** 0..1 entrance progress. */
        float enter() {
            return Ui.ease((Minecraft.getSystemTime() - showAt) / 380f);
        }

        @Override
        public void drawButton(Minecraft mc, int mx, int my) {
            if (!visible) return;
            hovered = mx >= xPosition && my >= yPosition && mx < xPosition + width && my < yPosition + height;
            hover = Ui.approach(hover, enabled && (hovered || focused) ? 1 : 0);
            float k = enter();
            if (k > 0) paint(yPosition + (1 - k) * 5, k * (enabled ? 1 : 0.4f));
        }

        abstract void paint(float y, float a);

        void ring(float y, float r, float a) {
            if (focused) Shapes.draw(xPosition - 1.5f, y - 1.5f, width + 3, height + 3, r + 1.5f, 0, 0, Ui.alpha(Ui.CYAN, a), 0.7f, 0, 0, 0);
        }
    }

    /** Icon + label pill. Primary is filled blue; the others are dark glass. */
    public static class Pill extends Base {
        final int icon;
        final boolean primary;
        final float radius;
        boolean danger;

        public Pill(int id, float x, float y, float w, float h, float radius, int icon, String label, boolean primary, long showAt) {
            super(id, x, y, w, h, label, showAt);
            this.icon = icon;
            this.primary = primary;
            this.radius = radius;
        }

        public Pill danger() {
            danger = true;
            return this;
        }

        @Override
        void paint(float y, float a) {
            float h = height, lift = hover * 0.5f;
            y -= lift;
            if (primary) {
                Shapes.draw(xPosition, y, width, h, radius, Ui.alpha(Ui.mix(Ui.PRIMARY_HI, 0xFF7FA8FF, hover * 0.35f), a),
                        Ui.alpha(Ui.mix(Ui.PRIMARY, Ui.PRIMARY_HI, hover * 0.5f), a), Ui.alpha(0x40FFFFFF, a), 0.5f,
                        Ui.alpha(0x992563FF, a * (0.55f + 0.45f * hover)), 7, 2.5f);
            } else {
                Shapes.draw(xPosition, y, width, h, radius, Ui.alpha(Ui.mix(0xC8171D33, 0xDC232B48, hover), a),
                        Ui.alpha(Ui.mix(0xC80A0F20, 0xDC121A33, hover), a), Ui.alpha(Ui.mix(0x24FFFFFF, 0x44FFFFFF, hover), a), 0.5f,
                        Ui.alpha(0x88000000, a), 6, 2.5f);
            }
            ring(y, radius, a);
            ValorFont f = Ui.semibold();
            float size = h * 0.38f, is = h * 0.38f, gap = icon >= 0 ? h * 0.26f : 0;
            float tw = f.width(displayString, size) + (icon >= 0 ? is + gap : 0), x = xPosition + (width - tw) / 2;
            int c = Ui.alpha(danger ? 0xFFFF8F9C : Ui.TEXT, a);
            if (icon >= 0) Ui.icon(icon, x, y + (h - is) / 2, is, c);
            f.draw(displayString, x + (icon >= 0 ? is + gap : 0), y + (h - size * 1.2f) / 2, size, c);
        }
    }

    /** Square icon button for the dock, with a hover highlight and a label above it. */
    public static class DockIcon extends Base {
        final int icon;
        final boolean dim;

        public DockIcon(int id, float x, float y, float size, int icon, String label, boolean dim, long showAt) {
            super(id, x, y, size, size, label, showAt);
            this.icon = icon;
            this.dim = dim;
        }

        @Override
        void paint(float y, float a) {
            float s = width;
            if (hover > 0.01f) Shapes.round(xPosition, y, s, s, s / 2, Ui.alpha(0x1FFFFFFF, hover * a));
            ring(y, s / 2, a);
            float is = s * 0.46f;
            int c = Ui.alpha(Ui.mix(dim ? 0xFF6B7591 : 0xFFC9D2E8, Ui.TEXT, hover), a);
            Ui.icon(icon, xPosition + (s - is) / 2, y + (s - is) / 2, is, c);
            if (hover > 0.01f) {
                ValorFont f = Ui.semibold();
                float tw = f.width(displayString, 5) + 10, tx = xPosition + s / 2 - tw / 2, ty = y - 15 + (1 - hover) * 2;
                Shapes.draw(tx, ty, tw, 11, 5.5f, Ui.alpha(Ui.SURFACE_HI, hover * a), Ui.alpha(Ui.SURFACE_HI, hover * a),
                        Ui.alpha(Ui.BORDER, hover * a), 0.5f, Ui.alpha(0x88000000, hover * a), 4, 1.5f);
                f.drawCentered(displayString, xPosition + s / 2, ty + 3.1f, 5, Ui.alpha(Ui.TEXT, hover * a));
            }
        }
    }

    /** "Continue" card for the last server or world. */
    public static class Card extends Base {
        String title, sub;
        boolean empty, world;

        public Card(int id, float x, float y, float w, float h, long showAt) {
            super(id, x, y, w, h, "", showAt);
        }

        @Override
        void paint(float y, float a) {
            float h = height, r = h * 0.24f;
            Shapes.draw(xPosition, y, width, h, r, Ui.alpha(Ui.mix(0xC8151B30, 0xDC1E2642, hover), a),
                    Ui.alpha(Ui.mix(0xC80A0F1F, 0xDC111933, hover), a), Ui.alpha(Ui.mix(0x22FFFFFF, 0x40FFFFFF, hover), a), 0.5f,
                    Ui.alpha(0x88000000, a), 7, 3);
            ring(y, r, a);
            float pad = h * 0.18f, tile = h - pad * 2;
            Ui.placeholder(!empty && world ? "world" : "server", xPosition + pad, y + pad, tile, tile * 0.28f, a);
            float tx = xPosition + pad * 2 + tile;
            Ui.regular().draw(sub, tx, y + h * 0.2f, h * 0.22f, Ui.alpha(0xFF8FB4FF, a));
            Ui.semibold().draw(title, tx, y + h * 0.5f, h * 0.27f, Ui.alpha(Ui.TEXT, a));
            float b = h * 0.55f, bx = xPosition + width - pad - b, by = y + (h - b) / 2;
            Shapes.draw(bx, by, b, b, b / 2, Ui.alpha(Ui.mix(Ui.PRIMARY_HI, 0xFF7FA8FF, hover * 0.4f), a), Ui.alpha(Ui.PRIMARY, a), 0, 0,
                    Ui.alpha(0x772563FF, a), 4, 1.5f);
            float is = b * 0.42f;
            Ui.icon(Ui.I_PLAY, bx + (b - is) / 2 + is * 0.06f, by + (b - is) / 2, is, Ui.alpha(0xFFFFFFFF, a));
        }
    }

    /** Settings row: the label on the left and, on the right, a switch (when {@code on} is set) or the current {@code value}. */
    public static class Setting extends Base {
        public Boolean on;
        public String value;
        private float knob = -1;

        public Setting(int id, float x, float y, float w, float h, String label, long showAt) {
            super(id, x, y, w, h, label, showAt);
        }

        @Override
        void paint(float y, float a) {
            float h = height, pad = drawRow(xPosition, y, width, h, displayString, hover, a);
            ring(y, h / 2, a);
            if (on != null) {
                knob = knob < 0 ? (on ? 1 : 0) : Ui.approach(knob, on ? 1 : 0);
                drawSwitch(xPosition + width - pad - 18, y + (h - 9) / 2, knob, a);
            } else if (value != null) drawValue(value, xPosition + width - pad, y, h, a);
        }
    }

    /** Pill-shaped settings row with its label; returns the side padding used, for placing controls on the right. */
    public static float drawRow(float x, float y, float w, float h, String label, float hover, float a) {
        float r = h / 2, size = h * 0.31f, pad = r * 0.9f;
        Shapes.draw(x, y, w, h, r, Ui.alpha(Ui.mix(0xB4141A30, 0xD01B2340, hover), a), Ui.alpha(Ui.mix(0xB40A0F20, 0xD0111933, hover), a),
                Ui.alpha(Ui.mix(0x1FFFFFFF, 0x38FFFFFF, hover), a), 0.5f, Ui.alpha(0x55000000, a), 4, 1.5f);
        Ui.regular().draw(label, x + pad, y + (h - size * 1.2f) / 2, size, Ui.alpha(Ui.mix(0xFFC9D2E8, Ui.TEXT, hover), a));
        return pad;
    }

    /** A row's current value, right-aligned at right. */
    public static void drawValue(String value, float right, float y, float h, float a) {
        ValorFont f = Ui.semibold();
        float size = h * 0.31f;
        f.draw(value, right - f.width(value, size), y + (h - size * 1.2f) / 2, size, Ui.alpha(0xFF8FB4FF, a));
    }

    /** Slider track of width w centred on cy, filled up to v (0..1), with a round knob. */
    public static void drawSlider(float x, float cy, float w, float v, float hover, float a) {
        Shapes.round(x, cy - 1.75f, w, 3.5f, 1.75f, Ui.alpha(0xFF2A2F42, a));
        Shapes.round(x, cy - 1.75f, Math.max(3.5f, w * v), 3.5f, 1.75f, Ui.alpha(Ui.PRIMARY, a));
        float k = 7 + hover;
        Shapes.draw(x + w * v - k / 2, cy - k / 2, k, k, k / 2, Ui.alpha(0xFFFFFFFF, a), Ui.alpha(0xFFE6EBF7, a), 0, 0, Ui.alpha(0x66000000, a), 1.5f, 0.6f);
    }

    /** Small text link. */
    public static class Link extends Base {
        final float size;
        final int color;

        public Link(int id, float x, float y, String text, float size, int color, long showAt) {
            super(id, x, y, Ui.regular().width(text, size), size * 1.3f, text, showAt);
            this.size = size;
            this.color = color;
        }

        @Override
        void paint(float y, float a) {
            ring(y, 2, a);
            Ui.regular().draw(displayString, xPosition, y, size, Ui.alpha(Ui.mix(color, Ui.TEXT, hover), a));
        }
    }
}
