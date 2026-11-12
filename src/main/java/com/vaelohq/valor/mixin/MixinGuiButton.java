package com.vaelohq.valor.mixin;

import com.vaelohq.valor.Config;
import com.vaelohq.valor.ui.Shapes;
import com.vaelohq.valor.ui.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws every vanilla-style button (Options and its sub-menus, other mods' screens) as a smooth Valor pill. */
@Mixin(GuiButton.class)
public abstract class MixinGuiButton extends Gui {
    @Shadow public int width;
    @Shadow public int height;
    @Shadow public int xPosition;
    @Shadow public int yPosition;
    @Shadow public String displayString;
    @Shadow public boolean enabled;
    @Shadow public boolean visible;
    @Shadow(remap = false) public int packedFGColour;   // added by Forge
    @Shadow protected boolean hovered;
    @Unique private float valor$hover;

    @Shadow
    protected abstract void mouseDragged(Minecraft mc, int mouseX, int mouseY);

    @Shadow
    protected abstract int getHoverState(boolean mouseOver);

    @Inject(method = "drawButton", at = @At("HEAD"), cancellable = true)
    private void valor$pill(Minecraft mc, int mouseX, int mouseY, CallbackInfo ci) {
        if (!Config.valorStyle) return;
        ci.cancel();
        if (!visible) return;
        hovered = mouseX >= xPosition && mouseY >= yPosition && mouseX < xPosition + width && mouseY < yPosition + height;
        boolean slider = enabled && getHoverState(true) == 0;   // sliders report the disabled state so vanilla never highlights them
        valor$hover = Ui.approach(valor$hover, enabled && hovered && !slider ? 1 : 0);
        float h = valor$hover;
        Ui.frame();
        if (enabled) Shapes.draw(xPosition, yPosition, width, height, height / 2f, Ui.mix(0xDC1A2650, 0xF04C85FF, h), Ui.mix(0xDC0D1434, 0xF02563FF, h),
                Ui.mix(0x30FFFFFF, 0x80FFFFFF, h), 0.5f, Ui.mix(0x66000000, 0x882563FF, h), 4, 1.5f);
        else Shapes.draw(xPosition, yPosition, width, height, height / 2f, 0x900B1028, 0x90070B1C, 0x18FFFFFF, 0.5f, 0, 0, 0);
        mouseDragged(mc, mouseX, mouseY);   // slider knobs
        int color = packedFGColour != 0 ? packedFGColour : !enabled ? 0x7C88A8 : hovered ? 0xFFFFFF : 0xE6ECFA;
        drawCenteredString(mc.fontRendererObj, displayString, xPosition + width / 2, yPosition + (height - 8) / 2, color);
    }
}
