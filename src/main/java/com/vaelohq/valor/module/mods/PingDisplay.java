package com.vaelohq.valor.module.mods;

import com.vaelohq.valor.module.TextHud;
import net.minecraft.client.network.NetworkPlayerInfo;

public final class PingDisplay extends TextHud {
    public PingDisplay() {
        super("ping", "Ping Display", "Your latency to the server.", Category.HUD, 0, 0.749f);
    }

    @Override
    public boolean visible() {
        return mc.getNetHandler() != null && mc.thePlayer != null;
    }

    @Override
    protected String text(boolean preview) {
        if (preview || !visible()) return "32 ms";
        NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
        return (info != null ? info.getResponseTime() : 0) + " ms";
    }
}
