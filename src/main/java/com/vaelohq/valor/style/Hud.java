package com.vaelohq.valor.style;

import com.vaelohq.valor.Config;
import com.vaelohq.valor.ui.Shapes;
import com.vaelohq.valor.ui.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Valor's hotbar (rounded glass pill with a gliding selection) and XP bar (thin pill), drawn instead of vanilla's. */
public final class Hud {
    private float selX = Float.NaN;
    private long lastFrame;

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Pre e) {
        if (!Config.valorStyle) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (e.type == RenderGameOverlayEvent.ElementType.HOTBAR && !mc.playerController.isSpectator()) {
            e.setCanceled(true);
            hotbar(mc, e.resolution, e.partialTicks);
        } else if (e.type == RenderGameOverlayEvent.ElementType.EXPERIENCE) {
            e.setCanceled(true);
            if (mc.playerController.gameIsSurvivalOrAdventure()) xp(mc, e.resolution);
        }
    }

    private void hotbar(Minecraft mc, ScaledResolution res, float partialTicks) {
        EntityPlayer player = mc.thePlayer;
        int x0 = res.getScaledWidth() / 2 - 91, y0 = res.getScaledHeight() - 22;
        Ui.frame();
        Shapes.draw(x0, y0, 182, 22, 11, 0xB80D1430, 0xC8070B1C, 0x26FFFFFF, 0.5f, 0x55000000, 4, 1);

        long now = System.nanoTime();
        float dt = lastFrame == 0 ? 1 : Math.min(1, (now - lastFrame) / 1e9f);
        lastFrame = now;
        float target = x0 + 1 + player.inventory.currentItem * 20;
        selX = Float.isNaN(selX) || Math.abs(target - selX) > 200 ? target : selX + (target - selX) * (1 - (float) Math.exp(-dt * 22));
        Shapes.draw(selX, y0 + 1, 20, 20, 7, 0x3800E5FF, 0x1400E5FF, 0xFF00E5FF, 0.8f, 0x6600E5FF, 3, 0);

        GlStateManager.enableRescaleNormal();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        RenderHelper.enableGUIStandardItemLighting();
        for (int i = 0; i < 9; i++) item(mc, player.inventory.mainInventory[i], x0 + 1 + i * 20 + 2, y0 + 3, partialTicks);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableBlend();
    }

    /** Same as vanilla's renderHotbarItem, including the pick-up bounce. */
    private static void item(Minecraft mc, ItemStack stack, int x, int y, float partialTicks) {
        if (stack == null) return;
        float f = stack.animationsToGo - partialTicks;
        if (f > 0) {
            GlStateManager.pushMatrix();
            float g = 1 + f / 5;
            GlStateManager.translate(x + 8, y + 12, 0);
            GlStateManager.scale(1 / g, (g + 1) / 2, 1);
            GlStateManager.translate(-(x + 8), -(y + 12), 0);
        }
        mc.getRenderItem().renderItemAndEffectIntoGUI(stack, x, y);
        if (f > 0) GlStateManager.popMatrix();
        mc.getRenderItem().renderItemOverlays(mc.fontRendererObj, stack, x, y);
    }

    private static void xp(Minecraft mc, ScaledResolution res) {
        int x = res.getScaledWidth() / 2 - 91, y = res.getScaledHeight() - 28;
        Ui.frame();
        if (mc.thePlayer.xpBarCap() > 0) {
            Shapes.draw(x, y, 182, 3, 1.5f, 0xB0070B1C, 0xB0070B1C, 0x20FFFFFF, 0.5f, 0, 0, 0);
            float w = mc.thePlayer.experience * 182;
            if (w > 1) Shapes.draw(x, y, w, 3, 1.5f, 0xFF6FE4FF, 0xFF0096FF, 0, 0, 0x7700C8FF, 2, 0);
        }
        int level = mc.thePlayer.experienceLevel;
        if (level > 0) {
            String s = String.valueOf(level);
            int tx = (res.getScaledWidth() - mc.fontRendererObj.getStringWidth(s)) / 2, ty = res.getScaledHeight() - 37;
            for (int[] o : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) mc.fontRendererObj.drawString(s, tx + o[0], ty + o[1], 0x050816);
            mc.fontRendererObj.drawString(s, tx, ty, 0x6EE7FF);
        }
        GlStateManager.color(1, 1, 1, 1);
    }
}
