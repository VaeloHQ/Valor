package com.vaelohq.valor.mixin;

import com.vaelohq.valor.Valor;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
}
