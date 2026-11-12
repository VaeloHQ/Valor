package com.vaelohq.valor.mixin;

import com.vaelohq.valor.ui.Ui;
import com.vaelohq.valor.ui.Widgets;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.NetworkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the "connecting" screen in Valor's style; the connection logic stays vanilla. */
@Mixin(GuiConnecting.class)
public abstract class MixinGuiConnecting extends GuiScreen {
    @Shadow
    private NetworkManager networkManager;

    @Inject(method = "initGui", at = @At("TAIL"))
    private void valor$buttons(CallbackInfo ci) {
        buttonList.clear();
        buttonList.add(new Widgets.Pill(0, width / 2f - 55, height / 2f + 24, 110, 16, 8, -1, "Cancel", false, 0));
    }

    @Inject(method = "drawScreen", at = @At("HEAD"), cancellable = true)
    private void valor$draw(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        Ui.frame();
        Ui.menuBackground(width, height);
        float cx = width / 2f, cy = height / 2f;
        Ui.spinner(cx, cy - 34, 7, 1);
        Ui.semibold().drawCentered(networkManager == null ? "Connecting to server" : "Logging in", cx, cy - 18, 8, Ui.TEXT);
        ServerData server = mc.getCurrentServerData();
        if (server != null) Ui.regular().drawCentered(server.serverIP, cx, cy - 4, 5.5f, Ui.MUTED);
        for (GuiButton b : buttonList) b.drawButton(mc, mouseX, mouseY);
        ci.cancel();
    }
}
