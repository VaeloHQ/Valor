package com.vaelohq.valor.ui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.WorldSettings;
import org.lwjgl.input.Keyboard;

/** Open to LAN, as a Valor dialog over the pause menu. Same choices and result message as vanilla's GuiShareToLan. */
public class LanScreen extends ValorScreen {
    private static final int START = 0, CANCEL = 1, MODE = 2, CHEATS = 3;
    private final GuiScreen parent;
    private String mode = "survival";
    private boolean cheats;
    private float px, py, pw = 200, ph = 112;

    public LanScreen(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        super.initGui();
        if (parent != null) parent.setWorldAndResolution(mc, width, height);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        float x = px + 16, w = pw - 32;
        long t = opened + 80;
        Widgets.Setting m = new Widgets.Setting(MODE, x, py + 36, w, 18, "Game mode", t);
        m.value = I18n.format("selectWorld.gameMode." + mode);
        buttonList.add(m);
        Widgets.Setting c = new Widgets.Setting(CHEATS, x, py + 58, w, 18, "Allow cheats", t);
        c.on = cheats;
        buttonList.add(c);
        float bw = (w - 6) / 2;
        buttonList.add(new Widgets.Pill(START, x, py + ph - 30, bw, 16, 8, -1, "Start LAN world", true, t));
        buttonList.add(new Widgets.Pill(CANCEL, x + bw + 6, py + ph - 30, bw, 16, 8, -1, "Cancel", false, t));
    }

    @Override
    protected void drawBackground() {
        if (parent != null) parent.drawScreen(-1, -1, 0);
        else super.drawBackground();
    }

    @Override
    protected void drawContent(int mouseX, int mouseY) {
        float a = Ui.ease(time() / 0.3f), y = py + (1 - a) * 5;
        Ui.dialog(width, height, px, y, pw, ph, a);
        Ui.semibold().drawCentered("Open to LAN", px + pw / 2, y + 12, 8, Ui.alpha(Ui.TEXT, a));
        Ui.regular().drawCentered("Settings for other players", px + pw / 2, y + 24, 5, Ui.alpha(Ui.MUTED, a));
    }

    @Override
    protected void actionPerformed(GuiButton b) {
        switch (b.id) {
            case CANCEL: mc.displayGuiScreen(parent); return;
            case MODE:   // vanilla's order
                mode = mode.equals("spectator") ? "creative" : mode.equals("creative") ? "adventure" : mode.equals("adventure") ? "survival" : "spectator";
                break;
            case CHEATS: cheats = !cheats; break;
            case START:
                mc.displayGuiScreen(null);
                String port = mc.getIntegratedServer().shareToLAN(WorldSettings.GameType.getByName(mode), cheats);
                IChatComponent msg = port != null ? new ChatComponentTranslation("commands.publish.started", port) : new ChatComponentText("commands.publish.failed");
                mc.ingameGUI.getChatGUI().printChatMessage(msg);
                return;
        }
        initGui();
    }

    @Override
    protected void keyTyped(char c, int key) {
        if (key == Keyboard.KEY_ESCAPE) mc.displayGuiScreen(parent);
    }
}
