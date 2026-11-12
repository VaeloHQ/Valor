package com.vaelohq.valor.mixin;

import net.minecraft.client.gui.GuiConfirmOpenLink;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads the link prompt's extra text (warning and Copy label). */
@Mixin(GuiConfirmOpenLink.class)
public interface GuiConfirmOpenLinkAccessor {
    @Accessor("openLinkWarning")
    String valor$warning();

    @Accessor("copyLinkButtonText")
    String valor$copy();

    @Accessor("showSecurityWarning")
    boolean valor$showWarning();
}
