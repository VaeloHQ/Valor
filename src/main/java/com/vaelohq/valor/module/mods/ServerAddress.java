package com.vaelohq.valor.module.mods;

import com.vaelohq.valor.module.TextHud;

public final class ServerAddress extends TextHud {
    public ServerAddress() {
        super("server", "Server Address", "The server you're playing on.", Category.HUD, 1, 0.127f);
    }

    @Override
    public boolean visible() {
        return mc.theWorld != null;
    }

    @Override
    protected String text(boolean preview) {
        if (preview) return "play.example.net";
        return mc.getCurrentServerData() != null ? mc.getCurrentServerData().serverIP : "Singleplayer";
    }
}
