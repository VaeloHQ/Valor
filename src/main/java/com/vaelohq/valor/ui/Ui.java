package com.vaelohq.valor.ui;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.texture.TextureUtil;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;

/** Shared menu resources and draw helpers. Everything here is created on first use and freed by {@link #release()}. */
public final class Ui {
    public static final int PRIMARY = 0xFF0A4DFF, AZURE = 0xFF00B4FF, CYAN = 0xFF00E5FF, BG = 0xFF060A1E,
            PANEL = 0xFF0A1033, TEXT = 0xFFEAF4FF, MUTED = 0xFF8FA6D6;
    private static ValorFont regular, semibold;
    private static int logo = -1;

    public static ValorFont regular() {
        return regular != null ? regular : (regular = new ValorFont("Inter-Regular.ttf"));
    }

    public static ValorFont semibold() {
        return semibold != null ? semibold : (semibold = new ValorFont("Inter-SemiBold.ttf"));
    }

    /** Frees fonts, the logo texture and the background shader. Called when the player enters a world. */
    public static void release() {
        if (regular != null) regular.delete();
        if (semibold != null) semibold.delete();
        regular = semibold = null;
        if (logo != -1) TextureUtil.deleteTexture(logo);
        logo = -1;
        Background.release();
    }

    public static void logo(float x, float y, float size, float alpha) {
        if (logo == -1) {
            try {
                logo = TextureUtil.uploadTextureImageAllocate(TextureUtil.glGenTextures(),
                        ImageIO.read(Ui.class.getResourceAsStream("/assets/valor/textures/logo.png")), true, true);
            } catch (Exception e) {
                throw new RuntimeException("Couldn't load Valor logo", e);
            }
        }
        GlStateManager.enableTexture2D();
        GlStateManager.bindTexture(logo);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1, 1, 1, alpha);
        WorldRenderer wr = Tessellator.getInstance().getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        wr.pos(x, y + size, 0).tex(0, 1).endVertex();
        wr.pos(x + size, y + size, 0).tex(1, 1).endVertex();
        wr.pos(x + size, y, 0).tex(1, 0).endVertex();
        wr.pos(x, y, 0).tex(0, 0).endVertex();
        Tessellator.getInstance().draw();
        GlStateManager.color(1, 1, 1, 1);
    }

    public static void rect(float x1, float y1, float x2, float y2, int argb) {
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        color(argb);
        WorldRenderer wr = Tessellator.getInstance().getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        wr.pos(x1, y2, 0).endVertex();
        wr.pos(x2, y2, 0).endVertex();
        wr.pos(x2, y1, 0).endVertex();
        wr.pos(x1, y1, 0).endVertex();
        Tessellator.getInstance().draw();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1, 1, 1, 1);
    }

    static void color(int argb) {
        GlStateManager.color((argb >> 16 & 255) / 255f, (argb >> 8 & 255) / 255f, (argb & 255) / 255f, (argb >>> 24) / 255f);
    }

    public static int alpha(int rgb, float a) {
        return (Math.round(clamp(a) * ((rgb >>> 24) == 0 ? 255 : rgb >>> 24)) << 24) | (rgb & 0xFFFFFF);
    }

    public static int mix(int a, int b, float t) {
        int r = 0;
        for (int s = 0; s < 32; s += 8) r |= Math.round(((a >>> s) & 255) + (((b >>> s) & 255) - ((a >>> s) & 255)) * t) << s;
        return r;
    }

    public static float clamp(float v) {
        return v < 0 ? 0 : v > 1 ? 1 : v;
    }

    /** Ease-out cubic on 0..1. */
    public static float ease(float t) {
        t = 1 - clamp(t);
        return 1 - t * t * t;
    }
}
