package com.vaelohq.valor.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraftforge.fml.common.ProgressManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Iterator;

/**
 * Valor's startup screen: the logo above a blue progress bar, like Minecraft 1.20's loading overlay. Forge's own
 * loading screen is turned off (see Splash), so this is drawn on the game thread whenever Forge reports progress.
 * Those reports come in the middle of resource loading, so each frame puts back every bit of GL state it touches.
 * When loading ends, IntroScreen takes over the same layout and turns the bar into "Press to enter".
 */
public final class LoadingScreen {
    /** Bar layout, shared with IntroScreen so the hand-off lines up. */
    static final float BAR_W = 120, BAR_H = 4.5f, BAR_Y = 34;

    private static boolean active;
    private static long last;
    private static float shown;

    private LoadingScreen() {
    }

    /** Called instead of vanilla's Mojang screen, which only runs when Forge's loading screen is off. */
    public static void begin() {
        active = true;
        frame(true);
    }

    /** Called on every Forge progress step. */
    public static void step() {
        if (active) frame(false);
    }

    /** Called when the game has finished starting (and nothing took over the loading screen). */
    public static void finish() {
        active = false;
    }

    /**
     * Called by the intro screen, which Minecraft creates near the end of startup: stops this screen and returns true
     * if it was showing, so the intro continues its animation.
     */
    static boolean takeHandOff() {
        boolean was = active;
        active = false;
        return was;
    }

    static float shown() {
        return shown;
    }

    /** Overall progress from Forge's nested bars: steps of the outer bar plus the fraction of the one inside it. */
    private static float target() {
        Iterator<ProgressManager.ProgressBar> it = ProgressManager.barIterator();
        if (!it.hasNext()) return shown;
        ProgressManager.ProgressBar outer = it.next();
        float inner = 0;
        if (it.hasNext()) {
            ProgressManager.ProgressBar b = it.next();
            if (b.getSteps() > 0) inner = (float) b.getStep() / b.getSteps();
        }
        return outer.getSteps() > 0 ? Math.min(1, (outer.getStep() + inner) / outer.getSteps()) : shown;
    }

    private static void frame(boolean force) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || !mc.isCallingFromMinecraftThread()) return;
        long now = Minecraft.getSystemTime();
        if (!force && now - last < 16) return;
        float dt = last == 0 ? 0 : Math.min(0.1f, (now - last) / 1000f);
        last = now;
        shown = Math.max(shown, shown + (target() - shown) * (1 - (float) Math.exp(-dt * 8)));

        // Save the state that the loading code may be relying on.
        IntBuffer viewport = BufferUtils.createIntBuffer(16);
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);
        FloatBuffer clear = BufferUtils.createFloatBuffer(16);
        GL11.glGetFloat(GL11.GL_COLOR_CLEAR_VALUE, clear);
        int framebuffer = OpenGlHelper.isFramebufferEnabled() ? GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING) : 0;
        int unit = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        int texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int src = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB), dst = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),
                srcA = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA), dstA = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND), alpha = GL11.glIsEnabled(GL11.GL_ALPHA_TEST), tex2d = GL11.glIsEnabled(GL11.GL_TEXTURE_2D),
                depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST), cull = GL11.glIsEnabled(GL11.GL_CULL_FACE), lighting = GL11.glIsEnabled(GL11.GL_LIGHTING),
                fog = GL11.glIsEnabled(GL11.GL_FOG);

        ScaledResolution sr = new ScaledResolution(mc);
        float w = sr.getScaledWidth(), h = sr.getScaledHeight();
        if (OpenGlHelper.isFramebufferEnabled()) OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, 0);
        GL11.glViewport(0, 0, mc.displayWidth, mc.displayHeight);
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GlStateManager.ortho(0, w, h, 0, 1000, 3000);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GlStateManager.translate(0, 0, -2000);
        GlStateManager.disableLighting();
        GlStateManager.disableFog();
        GlStateManager.disableDepth();
        GlStateManager.disableCull();
        GlStateManager.clearColor(0, 0, 0, 1);
        GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT);
        Ui.frame();
        draw(w, h, shown, 1, 1);
        Display.update();

        // Put it all back.
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.popMatrix();
        if (OpenGlHelper.isFramebufferEnabled()) OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, framebuffer);
        GL11.glViewport(viewport.get(0), viewport.get(1), viewport.get(2), viewport.get(3));
        GlStateManager.clearColor(clear.get(0), clear.get(1), clear.get(2), clear.get(3));
        GlStateManager.tryBlendFuncSeparate(src, dst, srcA, dstA);
        GlStateManager.color(1, 1, 1, 1);
        if (blend) GlStateManager.enableBlend(); else GlStateManager.disableBlend();
        if (alpha) GlStateManager.enableAlpha(); else GlStateManager.disableAlpha();
        if (tex2d) GlStateManager.enableTexture2D(); else GlStateManager.disableTexture2D();
        if (depth) GlStateManager.enableDepth(); else GlStateManager.disableDepth();
        if (cull) GlStateManager.enableCull(); else GlStateManager.disableCull();
        if (lighting) GlStateManager.enableLighting(); else GlStateManager.disableLighting();
        if (fog) GlStateManager.enableFog(); else GlStateManager.disableFog();
        GlStateManager.bindTexture(texture);
        GlStateManager.setActiveTexture(unit);
    }

    /**
     * The logo with the bar below it on black. barAlpha fades the bar and barWidth (0..1) narrows it towards the
     * centre, which IntroScreen uses to turn the bar into "Press to enter".
     */
    static void draw(float w, float h, float progress, float barAlpha, float barWidth) {
        float cx = w / 2, cy = h / 2;
        Ui.rect(0, 0, w, h, 0xFF000000);
        Ui.logo(cx - 24, cy - 44, 48, 1);
        if (barAlpha <= 0) return;
        float bw = BAR_W * barWidth, x = cx - bw / 2, y = cy + BAR_Y;
        Shapes.draw(x, y, bw, BAR_H, BAR_H / 2, Ui.alpha(0xFF141A33, barAlpha), Ui.alpha(0xFF0E1328, barAlpha), Ui.alpha(0x30FFFFFF, barAlpha),
                0.5f, 0, 0, 0);
        Shapes.draw(x, y, Math.max(BAR_H, bw * progress), BAR_H, BAR_H / 2, Ui.alpha(Ui.PRIMARY_HI, barAlpha), Ui.alpha(Ui.PRIMARY, barAlpha),
                0, 0, Ui.alpha(0x992563FF, barAlpha), 4, 0);
    }
}
