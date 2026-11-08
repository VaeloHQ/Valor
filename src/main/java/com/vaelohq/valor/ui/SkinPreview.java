package com.vaelohq.valor.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.DefaultPlayerSkin;
import org.lwjgl.opengl.GL11;

import java.util.UUID;

/** Draws the signed-in player's skin as a 3D model straight from ModelPlayer parts: no entity or world needed. */
final class SkinPreview {
    private static ModelPlayer model;
    private static boolean slim;

    /** (x, feetY) is the point between the feet; px is GUI pixels per skin pixel. */
    static void draw(float x, float feetY, float px, float yaw, float time) {
        Minecraft mc = Minecraft.getMinecraft();
        UUID id = mc.getSession().getProfile().getId();
        boolean s = id != null && "slim".equals(DefaultPlayerSkin.getSkinType(id));
        if (model == null || s != slim) {
            model = new ModelPlayer(0, s);
            slim = s;
        }
        mc.getTextureManager().bindTexture(id != null ? DefaultPlayerSkin.getDefaultSkin(id) : DefaultPlayerSkin.getDefaultSkinLegacy());

        float sway = (float) Math.sin(time * 1.1) * 0.04f + 0.05f;
        model.bipedRightArm.rotateAngleZ = model.bipedRightArmwear.rotateAngleZ = sway;
        model.bipedLeftArm.rotateAngleZ = model.bipedLeftArmwear.rotateAngleZ = -sway;

        GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);
        GlStateManager.enableDepth();
        GlStateManager.enableColorMaterial();
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, feetY - 24 * px, 100);
        GlStateManager.scale(-px, px, px);
        GlStateManager.rotate(180 + yaw, 0, 1, 0);
        GlStateManager.rotate(-8, 1, 0, 0);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.enableRescaleNormal();
        GlStateManager.color(1, 1, 1, 1);
        for (ModelRenderer part : new ModelRenderer[]{model.bipedHead, model.bipedBody, model.bipedRightArm, model.bipedLeftArm,
                model.bipedRightLeg, model.bipedLeftLeg, model.bipedHeadwear, model.bipedBodyWear, model.bipedRightArmwear,
                model.bipedLeftArmwear, model.bipedRightLegwear, model.bipedLeftLegwear}) {
            part.render(1);
        }
        GlStateManager.disableRescaleNormal();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.popMatrix();
        GlStateManager.disableColorMaterial();
        GlStateManager.disableDepth();
    }

    /** The skin's 8x8 face plus hat layer, for the account chip. */
    static void face(int x, int y, int size) {
        Minecraft mc = Minecraft.getMinecraft();
        UUID id = mc.getSession().getProfile().getId();
        mc.getTextureManager().bindTexture(id != null ? DefaultPlayerSkin.getDefaultSkin(id) : DefaultPlayerSkin.getDefaultSkinLegacy());
        GlStateManager.enableBlend();
        GlStateManager.color(1, 1, 1, 1);
        net.minecraft.client.gui.Gui.drawScaledCustomSizeModalRect(x, y, 8, 8, 8, 8, size, size, 64, 64);
        net.minecraft.client.gui.Gui.drawScaledCustomSizeModalRect(x, y, 40, 8, 8, 8, size, size, 64, 64);
    }
}
