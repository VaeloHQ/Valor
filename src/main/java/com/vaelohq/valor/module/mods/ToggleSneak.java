package com.vaelohq.valor.module.mods;

import net.minecraft.client.settings.KeyBinding;

public final class ToggleSneak extends ToggleKey {
    public ToggleSneak() {
        super("togglesneak", "ToggleSneak", "Press the sneak key once to keep sneaking.", "Sneak", 0.97f);
    }

    @Override
    KeyBinding key() {
        return mc.gameSettings.keyBindSneak;
    }
}
