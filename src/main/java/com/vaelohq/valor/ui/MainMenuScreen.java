package com.vaelohq.valor.ui;

import com.vaelohq.valor.Recent;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiLanguage;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.gui.GuiWinGame;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraftforge.fml.client.FMLClientHandler;
import org.lwjgl.input.Keyboard;

import java.util.Calendar;

/** Main menu: 3D head, greeting, title, action pills, Continue card and an icon dock. */
public class MainMenuScreen extends ValorScreen {
    private static final int MULTI = 0, SINGLE = 1, ACCOUNT = 2, CONTINUE = 3, SETTINGS = 4, MODS = 5, SOON_A = 6, SOON_B = 7,
            QUIT = 8, LANGUAGE = 9, CREDITS = 10;
    private Widgets.Card card;
    private int focus = -1;
    private float top;

    @Override
    public void initGui() {
        super.initGui();
        Recent.load();
        float cx = width / 2f, dockH = 19;
        top = Math.max(6, (height - dockH - 12 - 134) / 2);
        float py = top + 89, rowW = 186, x = cx - rowW / 2;
        long t = opened + 180;
        buttonList.add(new Widgets.Pill(MULTI, x, py, 66, 15, 7.5f, Ui.I_SERVER, "Multiplayer", true, t));
        buttonList.add(new Widgets.Pill(SINGLE, x + 71, py, 64, 15, 7.5f, Ui.I_PLAY, "Singleplayer", false, t + 40));
        buttonList.add(new Widgets.Pill(ACCOUNT, x + 140, py, 46, 15, 7.5f, Ui.I_USER, "Account", false, t + 80));
        card = new Widgets.Card(CONTINUE, x, py + 21, rowW, 24, t + 130);
        buttonList.add(card);

        float s = 15, gap = 2, dw = 5 * s + 4 * gap + 9, dx = cx - dw / 2 + 2, dy = height - dockH - 8 + 2;
        int[] icons = {Ui.I_SETTINGS, Ui.I_GRID, Ui.I_PLACEHOLDER, Ui.I_PLACEHOLDER, Ui.I_POWER};
        String[] labels = {"Settings", "Mods menu", "Coming soon", "Coming soon", "Quit game"};
        for (int i = 0; i < 5; i++) {
            float ix = dx + i * (s + gap) + (i == 4 ? 5 : 0);
            buttonList.add(new Widgets.DockIcon(SETTINGS + i, ix, dy, s, icons[i], labels[i], i == 2 || i == 3, t + 200));
        }
        float lx = width - 12;
        for (String[] l : new String[][]{{"Credits", "" + CREDITS}, {"Language", "" + LANGUAGE}}) {
            lx -= Ui.regular().width(l[0], 5.5f);
            buttonList.add(new Widgets.Link(Integer.parseInt(l[1]), lx, height - 15, l[0], 5.5f, Ui.MUTED, t + 200));
            lx -= 12;
        }
    }

    private static String greeting(String name) {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        return (h >= 5 && h < 12 ? "Good morning, " : h >= 12 && h < 18 ? "Good afternoon, " : "Good night, ") + name;
    }

    @Override
    protected void drawContent(int mouseX, int mouseY) {
        float cx = width / 2f, a = Ui.ease(time() / 0.5f), rise = (1 - a) * 6;

        float head = 30, hy = top + head / 2 + rise;
        float yaw = Math.max(-1, Math.min(1, (mouseX - cx) / (width * 0.5f))) * 22;
        float pitch = Math.max(-1, Math.min(1, (mouseY - hy) / (height * 0.5f))) * 14;
        Shapes.draw(cx - head * 0.4f, hy + head * 0.6f, head * 0.8f, 1, 0.5f, 0, 0, 0, 0, Ui.alpha(0x8C000000, a), head * 0.12f, 0);
        SkinPreview.head(cx, hy, head, yaw, pitch);

        Ui.bold().draw3D(greeting(mc.getSession().getUsername()), cx, top + 38 + rise, 9.5f, Ui.TEXT, 0xFFC4D8FF, 0xFF2D5BFF,
                0xFF0B1A5C, a);
        ValorFont b = Ui.bold();
        float tw = b.width("Valor", 24);
        b.draw("Valor", cx - tw / 2, top + 55 + rise, 24, Ui.alpha(0xFFFFFFFF, a), Ui.alpha(0xFF8DB2FF, a));

        card.empty = !Recent.has() || Recent.name.isEmpty();
        card.title = card.empty ? "No recent server" : Recent.name;
        card.sub = Recent.has() ? "Continue  ·  " + Recent.ago() : "Join a server to pick up here next time";

        float dw = 5 * 15 + 4 * 2 + 9 + 8, dy = height - 19 - 8;
        Shapes.draw(cx - dw / 2, dy, dw, 19, 9.5f, Ui.alpha(0xD0141A2E, a), Ui.alpha(0xD00A0E1C, a), Ui.alpha(0x24FFFFFF, a), 0.5f,
                Ui.alpha(0x99000000, a), 7, 3);
        float divX = cx - dw / 2 + 4 + 4 * 17 + 2;
        Ui.rect(divX, dy + 5, divX + 0.5f, dy + 14, Ui.alpha(0x30FFFFFF, a));

        Ui.regular().draw("Valor", 12, height - 15, 5.5f, Ui.alpha(Ui.MUTED, a));
        for (GuiButton btn : buttonList) ((Widgets.Base) btn).focused = btn.id == focus;
    }

    @Override
    protected void actionPerformed(GuiButton b) {
        switch (b.id) {
            case MULTI: mc.displayGuiScreen(new GuiMultiplayer(this)); break;
            case SINGLE: mc.displayGuiScreen(new GuiSelectWorld(this)); break;
            case ACCOUNT: mc.displayGuiScreen(new AccountScreen(this, false)); break;
            case CONTINUE: resume(); break;
            case SETTINGS: mc.displayGuiScreen(new GuiOptions(this, mc.gameSettings)); break;
            case MODS:
            case SOON_A:
            case SOON_B: toast("Coming soon"); break;
            case QUIT: mc.shutdown(); break;
            case LANGUAGE: mc.displayGuiScreen(new GuiLanguage(this, mc.gameSettings, mc.getLanguageManager())); break;
            case CREDITS: mc.displayGuiScreen(new GuiWinGame()); break;
        }
    }

    private void resume() {
        if (!Recent.has()) mc.displayGuiScreen(new GuiMultiplayer(this));
        else if (Recent.world) FMLClientHandler.instance().tryLoadExistingWorld(new GuiSelectWorld(this), Recent.target, Recent.name);
        else mc.displayGuiScreen(new GuiConnecting(this, mc, new ServerData(Recent.name, Recent.target, false)));
    }

    @Override
    protected void keyTyped(char c, int key) {
        int n = buttonList.size();
        if (key == Keyboard.KEY_TAB || key == Keyboard.KEY_RIGHT || key == Keyboard.KEY_DOWN) focus = (focus + 1) % n;
        else if (key == Keyboard.KEY_LEFT || key == Keyboard.KEY_UP) focus = (focus + n - 1) % n;
        else if ((key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) && focus >= 0) {
            GuiButton b = buttonList.get(focus);
            b.playPressSound(mc.getSoundHandler());
            actionPerformed(b);
        } else if (key == Keyboard.KEY_RSHIFT) toast("Mods menu coming soon");
    }
}
