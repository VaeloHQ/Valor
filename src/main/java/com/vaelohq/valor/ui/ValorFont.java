package com.vaelohq.valor.ui;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/** Smooth TTF text: glyphs 32..last baked once into a 1024x1024 atlas, drawn as linear-filtered quads. */
public final class ValorFont {
    private static final int ATLAS = 1024;
    private final int res, cell, pad, last, cols, tex;
    private final float[] adv = new float[256];

    ValorFont(String file, int res, int cell, int pad, int last) {
        this.res = res;
        this.cell = cell;
        this.pad = pad;
        this.last = last;
        this.cols = ATLAS / cell;
        try {
            Font font = Font.createFont(Font.TRUETYPE_FONT, ValorFont.class.getResourceAsStream("/assets/valor/font/" + file)).deriveFont((float) res);
            BufferedImage img = new BufferedImage(ATLAS, ATLAS, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g.setFont(font);
            g.setColor(Color.WHITE);
            FontMetrics fm = g.getFontMetrics();
            for (int c = 32; c <= last; c++) {
                if (!font.canDisplay(c)) continue;
                String s = String.valueOf((char) c);
                int i = c - 32;
                g.drawString(s, (i % cols) * cell + pad, (i / cols) * cell + pad + fm.getAscent());
                adv[c] = (float) font.getStringBounds(s, g.getFontRenderContext()).getWidth();
            }
            g.dispose();
            tex = Ui.texture(img);
        } catch (Exception e) {
            throw new RuntimeException("Couldn't load font " + file, e);
        }
    }

    private int glyph(char c) {
        return c >= 32 && c <= last ? c : '?';
    }

    public float width(String s, float size) {
        float w = 0;
        for (int i = 0; i < s.length(); i++) w += adv[glyph(s.charAt(i))];
        return w * size / res;
    }

    /** Draws s with its top-left at (x, y); size is the font size in GUI pixels. */
    public void draw(String s, float x, float y, float size, int argb) {
        draw(s, x, y, size, argb, argb);
    }

    /** Same as draw, with a left-to-right colour gradient across the whole string. */
    public void draw(String s, float x, float y, float size, int left, int right) {
        float k = size / res, c = cell * k, uv = (float) cell / ATLAS, x0 = x, w = Math.max(width(s, size), 0.001f);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.bindTexture(tex);
        WorldRenderer wr = Tessellator.getInstance().getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        x -= pad * k;
        y -= pad * k;
        for (int i = 0; i < s.length(); i++) {
            int ch = glyph(s.charAt(i)), n = ch - 32;
            float u = (n % cols) * uv, v = (n / cols) * uv;
            int cl = Ui.mix(left, right, Ui.clamp((x - x0) / w)), cr = Ui.mix(left, right, Ui.clamp((x + c - x0) / w));
            vert(wr, x, y + c, u, v + uv, cl);
            vert(wr, x + c, y + c, u + uv, v + uv, cr);
            vert(wr, x + c, y, u + uv, v, cr);
            vert(wr, x, y, u, v, cl);
            x += adv[ch] * k;
        }
        Tessellator.getInstance().draw();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableAlpha();
    }

    private static void vert(WorldRenderer wr, float x, float y, float u, float v, int argb) {
        wr.pos(x, y, 0).tex(u, v).color(argb >> 16 & 255, argb >> 8 & 255, argb & 255, argb >>> 24).endVertex();
    }

    public void drawCentered(String s, float cx, float y, float size, int argb) {
        draw(s, cx - width(s, size) / 2, y, size, argb, argb);
    }

    /** Extruded "block" text: depth layers stepping down from side to top, then a gradient front face. */
    public void draw3D(String s, float cx, float y, float size, int top, int topRight, int side, int sideDeep, float alpha) {
        float x = cx - width(s, size) / 2, step = size / 22f;
        int layers = 6;
        for (int i = layers; i >= 1; i--) {
            int c = Ui.alpha(Ui.mix(side, sideDeep, i / (float) layers), alpha);
            draw(s, x + i * step * 0.35f, y + i * step, size, c, c);
        }
        draw(s, x, y, size, Ui.alpha(top, alpha), Ui.alpha(topRight, alpha));
    }

    void delete() {
        TextureUtil.deleteTexture(tex);
    }
}
