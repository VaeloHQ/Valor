package com.vaelohq.valor.ui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;

/** Valor death screen. Same behaviour as vanilla's GuiGameOver: buttons unlock after one second, hardcore leaves the world. */
public class DeathScreen extends ValorScreen {
    private static final int RESPAWN = 0, LEAVE = 1;
    private int ticks;

    private boolean hardcore() {
        return mc.theWorld.getWorldInfo().isHardcoreModeEnabled();
    }

    @Override
    public void initGui() {
        super.initGui();
        float w = 150, x = (width - w) / 2, y = height / 2f + 6;
        long t = opened + 900;
        if (hardcore()) {
            buttonList.add(new Widgets.Pill(LEAVE, x, y + 10, w, 16, 8, -1, mc.isIntegratedServerRunning() ? "Delete world" : "Leave server",
                    true, t));
        } else {
            buttonList.add(new Widgets.Pill(RESPAWN, x, y, w, 16, 8, -1, "Respawn", true, t));
            buttonList.add(new Widgets.Pill(LEAVE, x, y + 22, w, 16, 8, -1, "Title screen", false, t + 60));
        }
        for (GuiButton b : buttonList) b.enabled = ticks >= 20;
    }

    @Override
    public void updateScreen() {
        if (++ticks == 20) for (GuiButton b : buttonList) b.enabled = true;
    }

    @Override
    protected void drawBackground() {
        Ui.gradient(0, 0, width, height, 0x70500010, 0xC0200008);
    }

    @Override
    protected void drawContent(int mouseX, int mouseY) {
        float a = Ui.ease(time() / 0.6f), cx = width / 2f, y = height / 2f - 62 + (1 - a) * 6;
        Ui.bold().draw3D(hardcore() ? "Game over!" : "You died!", cx, y, 24, 0xFFFFFFFF, 0xFFFFB3B3, 0xFFE0303C, 0xFF4A0A12, a);
        String score = "Score: " + mc.thePlayer.getScore();
        Ui.semibold().drawCentered(score, cx, y + 36, 6.5f, Ui.alpha(0xFFFFD9D9, a));
        if (hardcore())
            Ui.regular().drawCentered("You cannot respawn in hardcore mode!", cx, y + 48, 5.5f, Ui.alpha(0xFFFFB3B3, a));
    }

    @Override
    protected void keyTyped(char c, int key) {
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == RESPAWN) respawn();
        else if (hardcore()) mc.displayGuiScreen(new GuiMainMenu());
        else mc.displayGuiScreen(new ConfirmScreen(this, "Are you sure you want to quit?", "", "Title screen", "Respawn", false, quit -> {
            if (quit) {
                mc.theWorld.sendQuittingDisconnectingPacket();
                mc.loadWorld(null);
                mc.displayGuiScreen(new GuiMainMenu());
            } else respawn();
        }).delay(20));
    }

    private void respawn() {
        mc.thePlayer.respawnPlayer();
        mc.displayGuiScreen(null);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
