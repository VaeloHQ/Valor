package com.vaelohq.valor.mixin;

import com.vaelohq.valor.Config;
import com.vaelohq.valor.ui.Ui;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanilla and mod screens that use the default background get Valor's instead of dirt or the grey veil. */
@Mixin(GuiScreen.class)
public abstract class MixinGuiScreen {
    @Shadow
    public int width;
    @Shadow
    public int height;

    @Inject(method = "drawWorldBackground", at = @At("HEAD"), cancellable = true)
    private void valor$background(int tint, CallbackInfo ci) {
        if (!Config.valorStyle) return;
        Ui.frame();
        Ui.background(width, height);
        ci.cancel();
    }
}
