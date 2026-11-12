package com.vaelohq.valor.module.mods;

import com.vaelohq.valor.module.TextHud;

public final class MemoryUsage extends TextHud {
    public MemoryUsage() {
        super("memory", "Memory Usage", "Java memory in use out of the maximum.", Category.HUD, 1, 0.064f);
    }

    @Override
    protected String text(boolean preview) {
        Runtime r = Runtime.getRuntime();
        long used = (r.totalMemory() - r.freeMemory()) >> 20, max = r.maxMemory() >> 20;
        return "Mem " + used * 100 / Math.max(1, max) + "%  " + used + "/" + max + " MB";
    }
}
