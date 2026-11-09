package com.vaelohq.valor.mixin;

import com.vaelohq.valor.Config;
import net.minecraft.client.gui.GuiNewChat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Chat line backgrounds in Valor navy instead of black (same fade). */
@Mixin(GuiNewChat.class)
public abstract class MixinGuiNewChat {
    @ModifyArg(method = "drawChat", index = 4, at = @At(value = "INVOKE", ordinal = 0,
            target = "Lnet/minecraft/client/gui/GuiNewChat;drawRect(IIIII)V"))
    private int valor$chatBackground(int color) {
        return Config.valorStyle ? (color & 0xFF000000) | 0x0A1030 : color;
    }
}
