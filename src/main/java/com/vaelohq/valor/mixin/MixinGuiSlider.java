package com.vaelohq.valor.mixin;

import com.vaelohq.valor.Config;
import com.vaelohq.valor.ui.Widgets;
import net.minecraft.client.gui.GuiSlider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Slider knob drawn by {@link Widgets#sliderKnob} instead of two texture halves. */
@Mixin(GuiSlider.class)
public abstract class MixinGuiSlider {
    @Redirect(method = "mouseDragged", at = @At(value = "INVOKE", ordinal = 0,
            target = "Lnet/minecraft/client/gui/GuiSlider;drawTexturedModalRect(IIIIII)V"))
    private void valor$knob(GuiSlider self, int x, int y, int u, int v, int w, int h) {
        if (!Config.valorStyle) {
            self.drawTexturedModalRect(x, y, u, v, w, h);
            return;
        }
        Widgets.sliderKnob(self, x);
    }

    @Redirect(method = "mouseDragged", at = @At(value = "INVOKE", ordinal = 1,
            target = "Lnet/minecraft/client/gui/GuiSlider;drawTexturedModalRect(IIIIII)V"))
    private void valor$knobRight(GuiSlider self, int x, int y, int u, int v, int w, int h) {
        if (!Config.valorStyle) self.drawTexturedModalRect(x, y, u, v, w, h);
    }
}
