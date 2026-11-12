package com.vaelohq.valor.mixin;

import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads the screen GuiOptions returns to, so Valor's settings can return there too. */
@Mixin(GuiOptions.class)
public interface GuiOptionsAccessor {
    @Accessor("field_146441_g")
    GuiScreen valor$parent();
}
