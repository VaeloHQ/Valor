package com.vaelohq.valor.ui;

import net.minecraft.client.Minecraft;
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

/**
 * Smooth TTF text: glyphs 32..last baked once into a 1024x1024 atlas, drawn as linear-filtered quads. Characters the
 * atlas lacks (other scripts, symbols) are drawn with Minecraft's own font, which covers all of Unicode.
 */
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

    private boolean has(char c) {
        return c >= 32 && c <= last && adv[c] > 0;
    }

    /** Scale that makes Minecraft's font about as tall as this one at size. */
    private static float mcScale(float size) {
        return size / 9.6f;
    }

    public float width(String s, float size) {
        float w = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            w += has(c) ? adv[c] * size / res : Math.max(0, Minecraft.getMinecraft().fontRendererObj.getCharWidth(c)) * mcScale(size);
        }
        return w;
    }

    /** Draws s with its top-left at (x, y); size is the font size in GUI pixels. */
    public void draw(String s, float x, float y, float size, int argb) {
        draw(s, x, y, size, argb, argb);
    }

    /** Same as draw, with a left-to-right colour gradient across the whole string. */
    public void draw(String s, float x, float y, float size, int left, int right) {
        float w = Math.max(width(s, size), 0.001f), cx = x;
        for (int i = 0; i < s.length(); ) {
            boolean ok = has(s.charAt(i));
            int j = i;
            while (j < s.length() && has(s.charAt(j)) == ok) j++;
            String run = j - i == s.length() ? s : s.substring(i, j);
            if (ok) atlas(run, cx, y, size, left, right, x, w);
            else minecraft(run, cx, y, size, Ui.mix(left, right, Ui.clamp((cx - x) / w)));
            cx += width(run, size);
            i = j;
        }
    }

    /** Draws characters the atlas has; the gradient runs from x0 across w. */
    private void atlas(String s, float x, float y, float size, int left, int right, float x0, float w) {
        float k = size / res, c = cell * k, uv = (float) cell / ATLAS;
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
            int ch = s.charAt(i), n = ch - 32;
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

    /** Draws characters the atlas lacks with Minecraft's font, scaled and placed on this font's baseline. */
    private static void minecraft(String s, float x, float y, float size, int argb) {
        if ((argb >>> 24) < 4) return;   // Minecraft treats an almost-zero alpha as opaque
        float k = mcScale(size);
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y + size * 0.97f - 7 * k, 0);
        GlStateManager.scale(k, k, 1);
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        Minecraft.getMinecraft().fontRendererObj.drawString(s, 0, 0, argb, false);
        GlStateManager.popMatrix();
        GlStateManager.color(1, 1, 1, 1);
    }

    private static void vert(WorldRenderer wr, float x, float y, float u, float v, int argb) {
        wr.pos(x, y, 0).tex(u, v).color(argb >> 16 & 255, argb >> 8 & 255, argb & 255, argb >>> 24).endVertex();
    }

    /** Minecraft's 16 chat colours, indexed by their code 0-f. */
    private static final int[] CODES = {0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA, 0x555555,
            0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF};

    /** Width of text containing \u00a7 colour/format codes (the codes take no space). */
    public float widthFormatted(String s, float size) {
        return width(s.replaceAll("\u00a7.", ""), size);
    }

    /** Draws text honouring \u00a7 colour codes (format codes such as bold are ignored). */
    public void drawFormatted(String s, float x, float y, float size, int defaultColor, float alpha) {
        int color = defaultColor;
        StringBuilder run = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\u00a7' && i + 1 < s.length()) {
                if (run.length() > 0) {
                    draw(run.toString(), x, y, size, Ui.alpha(color, alpha));
                    x += width(run.toString(), size);
                    run.setLength(0);
                }
                int idx = "0123456789abcdef".indexOf(Character.toLowerCase(s.charAt(++i)));
                if (idx >= 0) color = 0xFF000000 | CODES[idx];
                else if (Character.toLowerCase(s.charAt(i)) == 'r') color = defaultColor;
            } else run.append(c);
        }
        if (run.length() > 0) draw(run.toString(), x, y, size, Ui.alpha(color, alpha));
    }

    /** Cuts text (with \u00a7 codes) to fit maxWidth, adding an ellipsis when cut. */
    public String fit(String s, float size, float maxWidth) {
        if (widthFormatted(s, size) <= maxWidth) return s;
        int n = s.length();
        while (n > 0 && (widthFormatted(s.substring(0, n) + "...", size) > maxWidth || s.charAt(n - 1) == '\u00a7')) n--;
        return s.substring(0, n) + "...";
    }

    /** Splits text into lines no wider than maxWidth (breaking at spaces; long words are cut). */
    public java.util.List<String> wrap(String text, float size, float maxWidth) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        for (String para : text.split("\n")) {
            StringBuilder line = new StringBuilder();
            for (String word : para.split(" ")) {
                String tryLine = line.length() == 0 ? word : line + " " + word;
                if (width(tryLine, size) <= maxWidth) {
                    line.setLength(0);
                    line.append(tryLine);
                    continue;
                }
                if (line.length() > 0) lines.add(line.toString());
                line.setLength(0);
                while (width(word, size) > maxWidth && word.length() > 1) {
                    int n = word.length();
                    while (n > 1 && width(word.substring(0, n), size) > maxWidth) n--;
                    lines.add(word.substring(0, n));
                    word = word.substring(n);
                }
                line.append(word);
            }
            lines.add(line.toString());
        }
        return lines;
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
