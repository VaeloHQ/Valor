package com.vaelohq.valor.module.mods;

import com.vaelohq.valor.module.Setting;
import com.vaelohq.valor.module.TextHud;

import java.text.SimpleDateFormat;
import java.util.Date;

public final class Clock extends TextHud {
    private final Setting.Choice format = add(new Setting.Choice("format", "Format", 0, "12 hour", "24 hour"));

    public Clock() {
        super("clock", "Clock", "Your computer's time.", Category.HUD, 1, 0);
    }

    @Override
    protected String text(boolean preview) {
        return new SimpleDateFormat(format.index == 0 ? "h:mm a" : "HH:mm").format(new Date());
    }
}
