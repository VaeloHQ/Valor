package com.vaelohq.valor.mixin;

import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.gui.GuiYesNoCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads a yes/no prompt so Valor can show it as its own dialog and answer the same callback. */
@Mixin(GuiYesNo.class)
public interface GuiYesNoAccessor {
    @Accessor("parentScreen")
    GuiYesNoCallback valor$callback();

    @Accessor("messageLine1")
    String valor$title();

    @Accessor("messageLine2")
    String valor$message();

    @Accessor("confirmButtonText")
    String valor$yes();

    @Accessor("cancelButtonText")
    String valor$no();

    @Accessor("parentButtonClickedId")
    int valor$id();

    @Accessor("ticksUntilEnable")
    int valor$delay();
}
