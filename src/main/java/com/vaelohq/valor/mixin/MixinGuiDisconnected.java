package com.vaelohq.valor.mixin;

import com.vaelohq.valor.ui.MultiplayerScreen;
import com.vaelohq.valor.ui.Ui;
import com.vaelohq.valor.ui.Widgets;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.IChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.List;

/** Draws the "disconnected" / "failed to connect" screen in Valor's style; vanilla still decides where Back goes. */
@Mixin(GuiDisconnected.class)
public abstract class MixinGuiDisconnected extends GuiScreen {
    @Shadow
    private String reason;
    @Shadow
    private IChatComponent message;
    @Shadow
    private GuiScreen parentScreen;
    @Unique
    private List<String> valor$lines = Collections.emptyList();

    @Inject(method = "initGui", at = @At("TAIL"))
    private void valor$layout(CallbackInfo ci) {
        // Drop a leading Java exception name ("java.io.IOException: ") from connection errors.
        String text = message.getUnformattedText().replaceFirst("^([a-z]\\w*\\.)+\\w*(Exception|Error): ", "");
        valor$lines = Ui.regular().wrap(text, 6, Math.min(width - 40, 280));
        float y = height / 2f + valor$lines.size() * 9 / 2f + 14;
        buttonList.clear();
        String back = parentScreen instanceof GuiMultiplayer || parentScreen instanceof MultiplayerScreen ? "Back to server list" : "Back to title screen";
        buttonList.add(new Widgets.Pill(0, width / 2f - 65, y, 130, 16, 8, -1, back, true, 0));
    }

    @Inject(method = "drawScreen", at = @At("HEAD"), cancellable = true)
    private void valor$draw(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        Ui.frame();
        Ui.menuBackground(width, height);
        float cx = width / 2f, y = height / 2f - valor$lines.size() * 9 / 2f;
        Ui.semibold().drawCentered(reason, cx, y - 20, 9, Ui.TEXT);
        for (String line : valor$lines) {
            Ui.regular().drawCentered(line, cx, y, 6, 0xFFC1CADF);
            y += 9;
        }
        for (GuiButton b : buttonList) b.drawButton(mc, mouseX, mouseY);
        ci.cancel();
    }
}
