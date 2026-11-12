package com.vaelohq.valor.mixin;

import com.vaelohq.valor.Config;
import com.vaelohq.valor.ui.Ui;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.renderer.Tessellator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla scrolling lists (video settings rows, flat world layers, presets, mod screens): a soft dark band instead of
 * the dirt texture, and the rows clipped to the list instead of hidden behind dirt strips at the top and bottom.
 */
@Mixin(GuiSlot.class)
public abstract class MixinGuiSlot {
    @Shadow
    public int top;
    @Shadow
    public int bottom;
    @Shadow
    public int left;
    @Shadow
    public int right;

    @Shadow
    protected abstract void overlayBackground(int startY, int endY, int startAlpha, int endAlpha);

    @Inject(method = "drawContainerBackground", at = @At("HEAD"), cancellable = true, remap = false)   // added by Forge
    private void valor$listBackground(Tessellator tessellator, CallbackInfo ci) {
        if (!Config.valorStyle) return;
        Ui.rect(left, top, right, bottom, 0x55000000);
        Ui.clip(left, top, right - left, bottom - top);   // ended where vanilla draws its top strip
        ci.cancel();
    }

    @Redirect(method = "drawScreen", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/client/gui/GuiSlot;overlayBackground(IIII)V"))
    private void valor$topStrip(GuiSlot self, int startY, int endY, int startAlpha, int endAlpha) {
        if (Config.valorStyle) Ui.endClip();
        else overlayBackground(startY, endY, startAlpha, endAlpha);
    }

    @Redirect(method = "drawScreen", at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/client/gui/GuiSlot;overlayBackground(IIII)V"))
    private void valor$bottomStrip(GuiSlot self, int startY, int endY, int startAlpha, int endAlpha) {
        if (!Config.valorStyle) overlayBackground(startY, endY, startAlpha, endAlpha);
    }
}
