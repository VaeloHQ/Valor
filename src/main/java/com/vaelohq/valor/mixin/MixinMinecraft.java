package com.vaelohq.valor.mixin;

import com.vaelohq.valor.Valor;
import com.vaelohq.valor.ui.LoadingScreen;
import com.vaelohq.valor.ui.ValorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import org.lwjgl.opengl.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {

    @Shadow
    private ByteBuffer readImageToBuffer(InputStream stream) throws IOException {
        throw new AssertionError();
    }

    @ModifyArg(method = "createDisplay", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/Display;setTitle(Ljava/lang/String;)V", remap = false))
    private String valor$title(String title) {
        return Valor.NAME;
    }

    // Swap the vanilla icon for the Valor V. On failure, vanilla's icon is used instead.
    @Inject(method = "setWindowIcon", at = @At("HEAD"), cancellable = true)
    private void valor$icon(CallbackInfo ci) {
        try (InputStream s16 = Valor.class.getResourceAsStream("/assets/valor/icon_16.png");
             InputStream s32 = Valor.class.getResourceAsStream("/assets/valor/icon_32.png")) {
            Display.setIcon(new ByteBuffer[]{readImageToBuffer(s16), readImageToBuffer(s32)});
            ci.cancel();
        } catch (Exception e) {
            Valor.LOGGER.error("Couldn't set Valor icon", e);
        }
    }

    // Vanilla caps menus at 30 FPS; Valor screens animate, so allow up to 60 (never above the player's own limit).
    @Inject(method = "getLimitFramerate", at = @At("HEAD"), cancellable = true)
    private void valor$menuFps(CallbackInfoReturnable<Integer> cir) {
        Minecraft mc = (Minecraft) (Object) this;
        if (mc.theWorld == null && mc.currentScreen instanceof ValorScreen) cir.setReturnValue(Math.min(mc.gameSettings.limitFramerate, 60));
    }

    /** Minecraft draws its Mojang screen only when Forge's loading screen is off; Valor's loading screen replaces it. */
    @Inject(method = "drawSplashScreen", at = @At("HEAD"), cancellable = true)
    private void valor$loadingScreen(TextureManager textures, CallbackInfo ci) {
        LoadingScreen.begin();
        ci.cancel();
    }

    @Inject(method = "startGame", at = @At("RETURN"))
    private void valor$started(CallbackInfo ci) {
        LoadingScreen.finish();
    }
}
