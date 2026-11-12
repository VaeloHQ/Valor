package com.vaelohq.valor.mixin;

import com.vaelohq.valor.ui.LoadingScreen;
import net.minecraftforge.fml.common.ProgressManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Every Forge loading step redraws Valor's loading screen (which ignores steps once the game has started). */
@Mixin(value = ProgressManager.ProgressBar.class, remap = false)
public abstract class MixinProgressBar {
    @Inject(method = "step(Ljava/lang/String;)V", at = @At("TAIL"))
    private void valor$step(String message, CallbackInfo ci) {
        LoadingScreen.step();
    }
}
