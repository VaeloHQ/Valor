package com.vaelohq.valor.mixin;

import com.vaelohq.valor.ui.Ui;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the "downloading terrain" screen in Valor's style; the keep-alive logic stays vanilla. */
@Mixin(GuiDownloadTerrain.class)
public abstract class MixinGuiDownloadTerrain extends GuiScreen {
    @Inject(method = "drawScreen", at = @At("HEAD"), cancellable = true)
    private void valor$draw(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        Ui.frame();
        Ui.menuBackground(width, height);
        float cx = width / 2f, cy = height / 2f;
        Ui.spinner(cx, cy - 16, 7, 1);
        Ui.semibold().drawCentered("Loading world", cx, cy, 8, Ui.TEXT);
        ci.cancel();
    }
}
