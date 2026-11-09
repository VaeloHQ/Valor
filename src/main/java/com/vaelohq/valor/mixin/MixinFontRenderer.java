package com.vaelohq.valor.mixin;

import com.vaelohq.valor.style.ValorPack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.inventory.GuiContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Container titles ("Inventory", "Chest") are hard-coded dark grey (0x404040), which can't be read on Valor's dark
 * container panels. While those panels are showing, that colour is lightened inside container screens.
 */
@Mixin(FontRenderer.class)
public abstract class MixinFontRenderer {

    @ModifyVariable(method = "drawString(Ljava/lang/String;III)I", at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private int valor$lightContainerLabels(int color) {
        if (color != 0x404040 || !ValorPack.containersShown() || !(Minecraft.getMinecraft().currentScreen instanceof GuiContainer)) return color;
        return 0xC9D6F5;
    }
}
