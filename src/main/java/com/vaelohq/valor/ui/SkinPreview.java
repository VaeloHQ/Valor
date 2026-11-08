package com.vaelohq.valor.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.DefaultPlayerSkin;
import org.lwjgl.opengl.GL11;

import java.util.UUID;

/** The signed-in player's head (with hat layer) as a lit 3D cube, from ModelPlayer parts: no entity or world needed. */
final class SkinPreview {
    private static ModelPlayer model;

    /** (cx, cy) is the head's centre, size its edge length in GUI pixels; yaw/pitch in degrees. */
    static void head(float cx, float cy, float size, float yaw, float pitch) {
        Minecraft mc = Minecraft.getMinecraft();
        if (model == null) model = new ModelPlayer(0, false);
        UUID id = mc.getSession().getProfile().getId();
        mc.getTextureManager().bindTexture(id != null ? DefaultPlayerSkin.getDefaultSkin(id) : DefaultPlayerSkin.getDefaultSkinLegacy());
        float px = size / 8;
        model.bipedHead.rotateAngleX = model.bipedHeadwear.rotateAngleX = 0;
        model.bipedHead.rotateAngleY = model.bipedHeadwear.rotateAngleY = 0;

        GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);
        GlStateManager.enableDepth();
        GlStateManager.enableColorMaterial();
        GlStateManager.pushMatrix();
        GlStateManager.translate(cx, cy, 150);
        GlStateManager.scale(-px, px, px);
        GlStateManager.rotate(-135, 0, 1, 0);   // light from the front-left, as vanilla does for inventory models
        RenderHelper.enableStandardItemLighting();
        GlStateManager.rotate(135, 0, 1, 0);
        GlStateManager.rotate(180 + yaw, 0, 1, 0);
        GlStateManager.rotate(pitch, 1, 0, 0);
        GlStateManager.translate(0, 4, 0);      // head pivots at the neck; centre it on (cx, cy)
        GlStateManager.enableRescaleNormal();
        GlStateManager.color(1, 1, 1, 1);
        model.bipedHead.render(1);
        model.bipedHeadwear.render(1);
        GlStateManager.disableRescaleNormal();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.popMatrix();
        GlStateManager.disableColorMaterial();
        GlStateManager.disableDepth();
    }
}
