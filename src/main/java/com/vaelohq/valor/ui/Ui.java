package com.vaelohq.valor.ui;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.texture.TextureUtil;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.IntBuffer;

/** Shared menu resources and draw helpers. Everything here is created on first use and freed by {@link #release()}. */
public final class Ui {
    public static final int PRIMARY = 0xFF0A4DFF, AZURE = 0xFF00B4FF, CYAN = 0xFF00E5FF, BG = 0xFF060A1E,
            PANEL = 0xFF0A1033, TEXT = 0xFFEAF4FF, MUTED = 0xFF8FA6D6;
    private static ValorFont regular, semibold, display;
    private static int logo = -1;

    public static ValorFont regular() {
        return regular != null ? regular : (regular = new ValorFont("Inter-Regular.ttf", 48, 64, 6, 255));
    }

    public static ValorFont semibold() {
        return semibold != null ? semibold : (semibold = new ValorFont("Inter-SemiBold.ttf", 48, 64, 6, 255));
    }

    /** Large-size SemiBold (ASCII only) baked at a higher resolution so big titles stay sharp at GUI scale 4. */
    public static ValorFont display() {
        return display != null ? display : (display = new ValorFont("Inter-SemiBold.ttf", 64, 88, 8, 126));
    }

    /** Frees fonts, the logo texture and the background shader. Called when the player enters a world. */
    public static void release() {
        if (regular != null) regular.delete();
        if (semibold != null) semibold.delete();
        if (display != null) display.delete();
        regular = semibold = display = null;
        if (logo != -1) TextureUtil.deleteTexture(logo);
        logo = -1;
        Background.release();
    }

    public static void logo(float x, float y, float size, float alpha) {
        if (logo == -1) {
            try {
                logo = texture(ImageIO.read(Ui.class.getResourceAsStream("/assets/valor/textures/logo.png")));
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

    /** Uploads img as a mipmapped, linear-filtered texture so it stays crisp when drawn much smaller than its size. */
    static int texture(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight(), id = GL11.glGenTextures();
        boolean gl30 = GLContext.getCapabilities().OpenGL30;
        GlStateManager.bindTexture(id);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_LOD_BIAS, -0.4f);
        if (!gl30) GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL14.GL_GENERATE_MIPMAP, GL11.GL_TRUE);
        IntBuffer buf = BufferUtils.createIntBuffer(w * h);
        buf.put(img.getRGB(0, 0, w, h, null, 0, w)).flip();
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, w, h, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, buf);
        if (gl30) GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
        return id;
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

    public static void circle(float cx, float cy, float r, int argb) {
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        color(argb);
        WorldRenderer wr = Tessellator.getInstance().getWorldRenderer();
        wr.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION);
        wr.pos(cx, cy, 0).endVertex();
        for (int i = 16; i >= 0; i--) wr.pos(cx + r * Math.cos(i * Math.PI / 8), cy + r * Math.sin(i * Math.PI / 8), 0).endVertex();
        Tessellator.getInstance().draw();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1, 1, 1, 1);
    }

    /** Soft radial ellipse: argb at the centre fading to transparent at the rim. */
    public static void glow(float cx, float cy, float rx, float ry, int argb) {
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        int r = argb >> 16 & 255, g = argb >> 8 & 255, b = argb & 255;
        WorldRenderer wr = Tessellator.getInstance().getWorldRenderer();
        wr.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(cx, cy, 0).color(r, g, b, argb >>> 24).endVertex();
        for (int i = 24; i >= 0; i--) wr.pos(cx + rx * Math.cos(i * Math.PI / 12), cy + ry * Math.sin(i * Math.PI / 12), 0).color(r, g, b, 0).endVertex();
        Tessellator.getInstance().draw();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableTexture2D();
        GlStateManager.color(1, 1, 1, 1);
    }

    /** 0.5px outline of a rectangle. */
    public static void outline(float x1, float y1, float x2, float y2, int argb) {
        rect(x1, y1, x2, y1 + 0.5f, argb);
        rect(x1, y2 - 0.5f, x2, y2, argb);
        rect(x1, y1 + 0.5f, x1 + 0.5f, y2 - 0.5f, argb);
        rect(x2 - 0.5f, y1 + 0.5f, x2, y2 - 0.5f, argb);
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
