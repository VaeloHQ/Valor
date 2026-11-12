package com.vaelohq.valor.mixin;

import com.vaelohq.valor.module.mods.Fullbright;
import com.vaelohq.valor.module.mods.Zoom;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.settings.GameSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Hooks for the Zoom (field of view) and Fullbright (light map gamma) modules. */
@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {
    @Inject(method = "getFOVModifier", at = @At("RETURN"), cancellable = true)
    private void valor$zoom(float partialTicks, boolean useFovSetting, CallbackInfoReturnable<Float> cir) {
        if (!useFovSetting) return;   // the held hand keeps its normal field of view
        float d = Zoom.divisor();
        if (d != 1) cir.setReturnValue(cir.getReturnValue() / d);
    }

    @Redirect(method = "updateLightmap", at = @At(value = "FIELD", target = "Lnet/minecraft/client/settings/GameSettings;gammaSetting:F"))
    private float valor$gamma(GameSettings settings) {
        return Fullbright.gamma(settings.gammaSetting);
    }
}
