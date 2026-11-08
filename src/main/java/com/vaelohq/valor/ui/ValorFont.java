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

/** Smooth TTF text: glyphs 32-255 baked once into a 1024x1024 atlas, drawn as linear-filtered quads. */
public final class ValorFont {
    private static final int RES = 48, CELL = 64, PAD = 6, ATLAS = 1024;
    private final float[] adv = new float[256];
    private final int tex;

    ValorFont(String file) {
        try {
            Font font = Font.createFont(Font.TRUETYPE_FONT, ValorFont.class.getResourceAsStream("/assets/valor/font/" + file)).deriveFont((float) RES);
            BufferedImage img = new BufferedImage(ATLAS, ATLAS, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g.setFont(font);
            g.setColor(Color.WHITE);
            FontMetrics fm = g.getFontMetrics();
            for (int c = 32; c < 256; c++) {
                if (!font.canDisplay(c)) continue;
                String s = String.valueOf((char) c);
                g.drawString(s, (c % 16) * CELL + PAD, (c / 16) * CELL + PAD + fm.getAscent());
                adv[c] = (float) font.getStringBounds(s, g.getFontRenderContext()).getWidth();
            }
            g.dispose();
            tex = TextureUtil.glGenTextures();
            TextureUtil.uploadTextureImageAllocate(tex, img, true, true);
        } catch (Exception e) {
            throw new RuntimeException("Couldn't load font " + file, e);
        }
    }

    public float width(String s, float size) {
        float w = 0;
        for (int i = 0; i < s.length(); i++) w += adv[s.charAt(i) & 0xFF];
        return w * size / RES;
    }

    /** Draws s with its top-left at (x, y); size is the font size in GUI pixels. */
    public void draw(String s, float x, float y, float size, int argb) {
        float k = size / RES, c = CELL * k, uv = (float) CELL / ATLAS;
        Ui.color(argb);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.bindTexture(tex);
        WorldRenderer wr = Tessellator.getInstance().getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        x -= PAD * k;
        y -= PAD * k;
        for (int i = 0; i < s.length(); i++) {
            int ch = s.charAt(i) & 0xFF;
            float u = (ch % 16) * uv, v = (ch / 16) * uv;
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
