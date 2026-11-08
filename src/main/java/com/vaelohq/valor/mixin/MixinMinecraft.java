package com.vaelohq.valor.mixin;

import com.vaelohq.valor.Valor;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    // Replaces the "Minecraft 1.8.9" window title once the window exists.
    @Inject(method = "createDisplay", at = @At("RETURN"))
    private void valor$setTitle(CallbackInfo ci) {
        Display.setTitle(Valor.NAME + " " + Valor.VERSION);
    }
}
