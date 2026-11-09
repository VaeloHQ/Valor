package com.vaelohq.valor.ui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiShareToLan;
import net.minecraft.realms.RealmsBridge;

/** Valor pause menu. Same actions as vanilla's GuiIngameMenu, including Forge's mod list and Realms handling. */
public class PauseScreen extends ValorScreen {
    private static final int QUIT = 1, BACK = 4, ACHIEVEMENTS = 5, STATS = 6, LAN = 7, OPTIONS = 0, MOD_OPTIONS = 12;

    @Override
    public void initGui() {
        super.initGui();
        float w = 184, x = (width - w) / 2, half = (w - 6) / 2, y = height / 2f - 54, h = 16;
        long t = opened + 60;
        buttonList.add(new Widgets.Pill(BACK, x, y, w, h, 8, Ui.I_PLAY, "Back to game", true, t));
        buttonList.add(new Widgets.Pill(ACHIEVEMENTS, x, y + 22, half, h, 8, -1, "Achievements", false, t + 30));
        buttonList.add(new Widgets.Pill(STATS, x + half + 6, y + 22, half, h, 8, -1, "Statistics", false, t + 30));
        Widgets.Pill lan = new Widgets.Pill(LAN, x, y + 44, w, h, 8, -1, "Open to LAN", false, t + 60);
        lan.enabled = mc.isSingleplayer() && mc.getIntegratedServer() != null && !mc.getIntegratedServer().getPublic();
        buttonList.add(lan);
        buttonList.add(new Widgets.Pill(OPTIONS, x, y + 66, half, h, 8, Ui.I_SETTINGS, "Options", false, t + 90));
        buttonList.add(new Widgets.Pill(MOD_OPTIONS, x + half + 6, y + 66, half, h, 8, Ui.I_GRID, "Mods", false, t + 90));
        String quit = mc.isIntegratedServerRunning() ? "Save and quit to title" : "Disconnect";
        buttonList.add(new Widgets.Pill(QUIT, x, y + 92, w, h, 8, Ui.I_POWER, quit, false, t + 120).danger());
    }

    @Override
    protected void drawContent(int mouseX, int mouseY) {
        float a = Ui.ease(time() / 0.3f), cx = width / 2f, y = height / 2f - 90 + (1 - a) * 4;
        Ui.bold().drawCentered("Game paused", cx, y, 13, Ui.alpha(Ui.TEXT, a));
        String where = mc.isIntegratedServerRunning() && mc.getIntegratedServer() != null ? "Singleplayer  ·  " + mc.getIntegratedServer().getWorldName()
                : mc.getCurrentServerData() != null ? mc.getCurrentServerData().serverIP : "";
        if (!where.isEmpty()) Ui.regular().drawCentered(where, cx, y + 18, 5.5f, Ui.alpha(Ui.MUTED, a));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case OPTIONS: mc.displayGuiScreen(new OptionsScreen(this)); break;
            case QUIT:
                boolean integrated = mc.isIntegratedServerRunning(), realms = mc.isConnectedToRealms();
                button.enabled = false;
                mc.theWorld.sendQuittingDisconnectingPacket();
                mc.loadWorld(null);
                if (integrated) mc.displayGuiScreen(new GuiMainMenu());
                else if (realms) new RealmsBridge().switchToRealms(new GuiMainMenu());
                else mc.displayGuiScreen(new MultiplayerScreen(new GuiMainMenu()));
                break;
            case BACK:
                mc.displayGuiScreen(null);
                mc.setIngameFocus();
                break;
            case ACHIEVEMENTS: if (mc.thePlayer != null) mc.displayGuiScreen(new AchievementsScreen(this, mc.thePlayer.getStatFileWriter())); break;
            case STATS: if (mc.thePlayer != null) mc.displayGuiScreen(new StatsScreen(this, mc.thePlayer.getStatFileWriter())); break;
            case LAN: mc.displayGuiScreen(new GuiShareToLan(this)); break;
            case MOD_OPTIONS: mc.displayGuiScreen(new ModsScreen(this)); break;
        }
    }
}
