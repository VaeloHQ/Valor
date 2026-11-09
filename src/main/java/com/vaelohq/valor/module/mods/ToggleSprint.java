package com.vaelohq.valor.module.mods;

import net.minecraft.client.settings.KeyBinding;

public final class ToggleSprint extends ToggleKey {
    public ToggleSprint() {
        super("togglesprint", "ToggleSprint", "Press the sprint key once to keep sprinting.", "Sprint", 0.9f);
    }

    @Override
    KeyBinding key() {
        return mc.gameSettings.keyBindSprint;
    }
}
