package com.vaelohq.valor.ui;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.IntBuffer;

/** Theme and shared menu resources. Everything here is created on first use and freed by {@link #release()}. */
public final class Ui {
    public static final int PRIMARY = 0xFF2563FF, PRIMARY_HI = 0xFF4C85FF, CYAN = 0xFF00E5FF, TEXT = 0xFFF4F7FF,
            MUTED = 0xFF9AA6C4, SURFACE = 0xC40B1020, SURFACE_HI = 0xD8141B33, BORDER = 0x26FFFFFF, DANGER = 0xFFFF5A6E, SUCCESS = 0xFF5BE38E;

    /** Icon cells in textures/icons.png (4x4 grid, Lucide icons). */
    public static final int I_SETTINGS = 0, I_GRID = 1, I_PLACEHOLDER = 2, I_POWER = 3, I_PLAY = 4, I_SERVER = 5,
            I_USER = 6, I_EYE = 7, I_EYE_OFF = 8, I_CLOSE = 9, I_MINUS = 10, I_LOGIN = 11;

    private static ValorFont regular, semibold, bold;
    private static int logo = -1, icons = -1;

    public static ValorFont regular() {
        return regular != null ? regular : (regular = new ValorFont("Inter-Regular.ttf", 48, 64, 6, 255));
    }

    public static ValorFont semibold() {
        return semibold != null ? semibold : (semibold = new ValorFont("Inter-SemiBold.ttf", 48, 64, 6, 255));
    }

    /** Bold ASCII baked at 64px so large titles stay sharp at GUI scale 4. */
    public static ValorFont bold() {
        return bold != null ? bold : (bold = new ValorFont("Inter-Bold.ttf", 64, 88, 8, 126));
    }

    /** Call once per frame before drawing shapes (sets the shape shader's pixel size). */
    public static void frame() {
        Shapes.px = 1f / new net.minecraft.client.gui.ScaledResolution(net.minecraft.client.Minecraft.getMinecraft()).getScaleFactor();
    }

    /** The menu background: animated blue light out of game, a dark veil over the world in game. */
    public static void background(float w, float h) {
        if (net.minecraft.client.Minecraft.getMinecraft().theWorld != null) {
            GlStateManager.shadeModel(GL11.GL_SMOOTH);
            gradient(0, 0, w, h, 0xB8060A1A, 0xD8040712);
            GlStateManager.shadeModel(GL11.GL_FLAT);
        } else Background.draw(w, h);
    }

    /** The animated blue-light background, regardless of whether a world is loaded. */
    public static void menuBackground(float w, float h) {
        Background.draw(w, h);
    }

    /** Vertical gradient rectangle. */
    public static void gradient(float x1, float y1, float x2, float y2, int top, int bottom) {
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableAlpha();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        WorldRenderer wr = Tessellator.getInstance().getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(x1, y2, 0).color(bottom >> 16 & 255, bottom >> 8 & 255, bottom & 255, bottom >>> 24).endVertex();
        wr.pos(x2, y2, 0).color(bottom >> 16 & 255, bottom >> 8 & 255, bottom & 255, bottom >>> 24).endVertex();
        wr.pos(x2, y1, 0).color(top >> 16 & 255, top >> 8 & 255, top & 255, top >>> 24).endVertex();
        wr.pos(x1, y1, 0).color(top >> 16 & 255, top >> 8 & 255, top & 255, top >>> 24).endVertex();
        Tessellator.getInstance().draw();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    /** Eight dots chasing round a circle. */
    public static void spinner(float cx, float cy, float r, float alpha) {
        double t = net.minecraft.client.Minecraft.getSystemTime() / 110.0;
        for (int i = 0; i < 8; i++) {
            double ang = i * Math.PI / 4;
            float k = (float) (((i - t) % 8 + 8) % 8) / 8f, d = r * 0.22f;
            Shapes.round(cx + (float) Math.cos(ang) * r - d / 2, cy + (float) Math.sin(ang) * r - d / 2, d, d, d / 2,
                    alpha(mix(PRIMARY_HI, 0x334C85FF, k), alpha));
        }
    }

    /** Frees fonts, textures and shaders. Called when the player enters a world. */
    public static void release() {
        for (ValorFont f : new ValorFont[]{regular, semibold, bold}) if (f != null) f.delete();
        regular = semibold = bold = null;
        if (logo != -1) TextureUtil.deleteTexture(logo);
        if (icons != -1) TextureUtil.deleteTexture(icons);
        logo = icons = -1;
        Background.release();
        Shapes.release();
    }

    public static void logo(float x, float y, float size, float alpha) {
        if (logo == -1) logo = load("logo.png");
        quad(logo, x, y, size, size, 0, 0, 1, 1, alpha(0xFFFFFFFF, alpha));
    }

    public static void icon(int i, float x, float y, float size, int argb) {
        if (icons == -1) icons = load("icons.png");
        float u = (i % 4) / 4f, v = (i / 4) / 4f;
        quad(icons, x, y, size, size, u, v, u + 0.25f, v + 0.25f, argb);
    }

    private static int load(String name) {
        try {
            return texture(ImageIO.read(Ui.class.getResourceAsStream("/assets/valor/textures/" + name)));
        } catch (Exception e) {
            throw new RuntimeException("Couldn't load " + name, e);
        }
    }

    private static void quad(int tex, float x, float y, float w, float h, float u0, float v0, float u1, float v1, int argb) {
        GlStateManager.enableTexture2D();
        GlStateManager.bindTexture(tex);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableAlpha();
        color(argb);
        WorldRenderer wr = Tessellator.getInstance().getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        wr.pos(x, y + h, 0).tex(u0, v1).endVertex();
        wr.pos(x + w, y + h, 0).tex(u1, v1).endVertex();
        wr.pos(x + w, y, 0).tex(u1, v0).endVertex();
        wr.pos(x, y, 0).tex(u0, v0).endVertex();
        Tessellator.getInstance().draw();
        GlStateManager.enableAlpha();
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

    static void color(int argb) {
        GlStateManager.color((argb >> 16 & 255) / 255f, (argb >> 8 & 255) / 255f, (argb & 255) / 255f, (argb >>> 24) / 255f);
    }

    /** Multiplies the colour's own alpha by a (0..1). */
    public static int alpha(int argb, float a) {
        return (Math.round(clamp(a) * (argb >>> 24)) << 24) | (argb & 0xFFFFFF);
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

    /** Moves cur toward target; about 150 ms to settle at 60 fps. */
    public static float approach(float cur, float target) {
        return cur + (target - cur) * 0.28f;
    }
}
