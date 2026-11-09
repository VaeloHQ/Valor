package com.vaelohq.valor.mixin;

import com.vaelohq.valor.Config;
import net.minecraft.client.gui.GuiIngame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Sidebar scoreboard backgrounds in Valor colours. */
@Mixin(GuiIngame.class)
public abstract class MixinGuiIngame {
    @ModifyConstant(method = "renderScoreboard", constant = @Constant(intValue = 0x50000000))
    private int valor$rows(int color) {
        return Config.valorStyle ? 0x900A1030 : color;
    }

    @ModifyConstant(method = "renderScoreboard", constant = @Constant(intValue = 0x60000000))
    private int valor$title(int color) {
        return Config.valorStyle ? 0xB0142468 : color;
    }
}
