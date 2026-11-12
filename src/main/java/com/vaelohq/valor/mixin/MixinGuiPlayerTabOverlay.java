package com.vaelohq.valor.mixin;

import com.vaelohq.valor.Config;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Tab list panel and row tint in Valor colours. */
@Mixin(GuiPlayerTabOverlay.class)
public abstract class MixinGuiPlayerTabOverlay {
    @ModifyConstant(method = "renderPlayerlist", constant = @Constant(intValue = Integer.MIN_VALUE))
    private int valor$panel(int color) {
        return Config.valorStyle ? 0xC00A1030 : color;
    }

    @ModifyConstant(method = "renderPlayerlist", constant = @Constant(intValue = 0x20FFFFFF))
    private int valor$row(int color) {
        return Config.valorStyle ? 0x302563FF : color;
    }
}
