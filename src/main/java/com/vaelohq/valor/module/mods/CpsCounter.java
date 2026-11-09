package com.vaelohq.valor.module.mods;

import com.vaelohq.valor.module.Clicks;
import com.vaelohq.valor.module.Setting;
import com.vaelohq.valor.module.TextHud;

public final class CpsCounter extends TextHud {
    private final Setting.Toggle right = add(new Setting.Toggle("right", "Show right clicks", true));

    public CpsCounter() {
        super("cps", "CPS Counter", "Clicks per second for each mouse button.", Category.PVP, 0, 0.064f);
    }

    @Override
    protected String text(boolean preview) {
        int l = preview ? 12 : Clicks.left(), r = preview ? 4 : Clicks.right();
        return (right.value ? l + " | " + r : String.valueOf(l)) + " CPS";
    }
}
