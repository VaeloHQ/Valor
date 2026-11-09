package com.vaelohq.valor.mixin;

import net.minecraft.client.gui.GuiErrorScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads an error screen's title and message. */
@Mixin(GuiErrorScreen.class)
public interface GuiErrorScreenAccessor {
    @Accessor("field_146313_a")
    String valor$title();

    @Accessor("field_146312_f")
    String valor$message();
}
