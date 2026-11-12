package com.vaelohq.valor.mixin;

import com.vaelohq.valor.Config;
import com.vaelohq.valor.ui.Shapes;
import com.vaelohq.valor.ui.Ui;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiTextField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Vanilla text boxes (signs, command blocks, mod screens) as rounded Valor fields; vanilla still draws the text. */
@Mixin(GuiTextField.class)
public abstract class MixinGuiTextField {
    @Shadow
    private boolean isFocused;

    @Redirect(method = "drawTextBox", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/client/gui/GuiTextField;drawRect(IIIII)V"))
    private void valor$frame(int x1, int y1, int x2, int y2, int color) {
        if (!Config.valorStyle) {
            Gui.drawRect(x1, y1, x2, y2, color);
            return;
        }
        Ui.frame();
        Shapes.draw(x1 + 1, y1 + 1, x2 - x1 - 2, y2 - y1 - 2, 3, 0xFF161925, 0xFF12141E, isFocused ? Ui.PRIMARY_HI : 0x1FFFFFFF,
                isFocused ? 0.8f : 0.5f, isFocused ? 0x552563FF : 0, isFocused ? 4 : 0, 0);
    }

    @Redirect(method = "drawTextBox", at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/client/gui/GuiTextField;drawRect(IIIII)V"))
    private void valor$fill(int x1, int y1, int x2, int y2, int color) {
        if (!Config.valorStyle) Gui.drawRect(x1, y1, x2, y2, color);
    }
}
