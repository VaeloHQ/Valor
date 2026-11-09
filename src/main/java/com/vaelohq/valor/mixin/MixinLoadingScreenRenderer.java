package com.vaelohq.valor.mixin;

import com.vaelohq.valor.ui.Shapes;
import com.vaelohq.valor.ui.Ui;
import net.minecraft.client.LoadingScreenRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.fml.client.FMLClientHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The world loading / saving progress screen ("Loading world", "Saving world") drawn in Valor's style. */
@Mixin(LoadingScreenRenderer.class)
public abstract class MixinLoadingScreenRenderer {
    @Shadow
    private String message;
    @Shadow
    private String currentlyDisplayedText;
    @Unique
    private int valor$progress;

    @Inject(method = "setLoadingProgress", at = @At("HEAD"))
    private void valor$progress(int progress, CallbackInfo ci) {
        valor$progress = progress;
    }

    /** Vanilla draws its dirt screen only when FML did not draw one; Valor draws instead and reports it handled. */
    @Redirect(method = "setLoadingProgress", at = @At(value = "INVOKE", remap = false,
            target = "Lnet/minecraftforge/fml/client/FMLClientHandler;handleLoadingScreen(Lnet/minecraft/client/gui/ScaledResolution;)Z"))
    private boolean valor$draw(FMLClientHandler fml, ScaledResolution sr) throws java.io.IOException {
        if (fml.handleLoadingScreen(sr)) return true;
        float w = sr.getScaledWidth(), h = sr.getScaledHeight(), cx = w / 2, cy = h / 2;
        Ui.frame();
        Ui.menuBackground(w, h);
        Ui.logo(cx - 14, cy - 50, 28, 1);
        Ui.semibold().drawCentered(currentlyDisplayedText, cx, cy - 12, 8, Ui.TEXT);
        if (!message.isEmpty()) Ui.regular().drawCentered(message, cx, cy + 2, 5.5f, Ui.MUTED);
        float bw = 120, by = cy + 16;
        if (valor$progress >= 0) {
            Shapes.round(cx - bw / 2, by, bw, 4, 2, 0xFF1A2140);
            Shapes.round(cx - bw / 2, by, Math.max(4, bw * Math.min(valor$progress, 100) / 100f), 4, 2, Ui.PRIMARY);
        } else Ui.spinner(cx, by + 6, 5, 1);
        return true;
    }
}
