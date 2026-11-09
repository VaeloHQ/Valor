package com.vaelohq.valor.mixin;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads the screen GuiSelectWorld returns to, so Valor's world list can return there too. */
@Mixin(GuiSelectWorld.class)
public interface GuiSelectWorldAccessor {
    @Accessor("parentScreen")
    GuiScreen valor$parent();
}
