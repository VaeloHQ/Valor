package com.vaelohq.valor.mixin;

import com.vaelohq.valor.Config;
import com.vaelohq.valor.ui.Shapes;
import com.vaelohq.valor.ui.Ui;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** The chat input bar as a rounded navy pill. */
@Mixin(GuiChat.class)
public abstract class MixinGuiChat {
    @Redirect(method = "drawScreen", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/client/gui/GuiChat;drawRect(IIIII)V"))
    private void valor$inputBar(int x1, int y1, int x2, int y2, int color) {
        if (!Config.valorStyle) {
            Gui.drawRect(x1, y1, x2, y2, color);
            return;
        }
        Ui.frame();
        Shapes.draw(x1, y1, x2 - x1, y2 - y1, 4, 0xD0141B3A, 0xD00A1030, 0x40FFFFFF, 0.5f, 0, 0, 0);
    }
}
