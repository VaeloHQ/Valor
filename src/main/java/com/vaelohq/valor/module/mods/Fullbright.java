package com.vaelohq.valor.module.mods;

import com.vaelohq.valor.module.Module;
import com.vaelohq.valor.module.Setting;

/** Lights the world as if gamma were very high, without changing your saved brightness setting. */
public final class Fullbright extends Module {
    private static Fullbright instance;
    private final Setting.Slider level = add(new Setting.Slider("level", "Brightness", 10, 2, 15, 1, "x"));

    public Fullbright() {
        super("fullbright", "Fullbright", "See in the dark without changing your brightness setting.", Category.UTILITY);
        instance = this;
    }

    /** The gamma the light map should use (called from MixinEntityRenderer). */
    public static float gamma(float setting) {
        return instance != null && instance.enabled() ? instance.level.value : setting;
    }
}
