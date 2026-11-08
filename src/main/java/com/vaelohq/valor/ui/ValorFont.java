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
        float k = size / res, c = cell * k, uv = (float) cell / ATLAS;
        Ui.color(argb);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.bindTexture(tex);
        WorldRenderer wr = Tessellator.getInstance().getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        x -= pad * k;
        y -= pad * k;
        for (int i = 0; i < s.length(); i++) {
            int ch = glyph(s.charAt(i)), n = ch - 32;
            float u = (n % cols) * uv, v = (n / cols) * uv;
            wr.pos(x, y + c, 0).tex(u, v + uv).endVertex();
            wr.pos(x + c, y + c, 0).tex(u + uv, v + uv).endVertex();
            wr.pos(x + c, y, 0).tex(u + uv, v).endVertex();
            wr.pos(x, y, 0).tex(u, v).endVertex();
            x += adv[ch] * k;
        }
        Tessellator.getInstance().draw();
        GlStateManager.enableAlpha();
        GlStateManager.color(1, 1, 1, 1);
    }

    public void drawCentered(String s, float cx, float y, float size, int argb) {
        draw(s, cx - width(s, size) / 2, y, size, argb);
    }

    void delete() {
        TextureUtil.deleteTexture(tex);
    }
}
