package com.vaelohq.valor.mixin;

import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads the screen GuiMultiplayer returns to, so Valor's server list can return there too. */
@Mixin(GuiMultiplayer.class)
public interface GuiMultiplayerAccessor {
    @Accessor("parentScreen")
    GuiScreen valor$parent();
}
