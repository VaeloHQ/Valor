package com.vaelohq.valor.module.mods;

import com.vaelohq.valor.module.TextHud;
import net.minecraft.client.Minecraft;

public final class FpsCounter extends TextHud {
    public FpsCounter() {
        super("fps", "FPS Counter", "Frames per second.", Category.HUD, 0, 0);
    }

    @Override
    protected String text(boolean preview) {
        return (preview ? 144 : Minecraft.getDebugFPS()) + " FPS";
    }
}
