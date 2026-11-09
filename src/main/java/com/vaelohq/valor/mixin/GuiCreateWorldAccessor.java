package com.vaelohq.valor.mixin;

import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads the screen GuiCreateWorld returns to, so Valor's create-world screen can return there too. */
@Mixin(GuiCreateWorld.class)
public interface GuiCreateWorldAccessor {
    @Accessor("parentScreen")
    GuiScreen valor$parent();
}
