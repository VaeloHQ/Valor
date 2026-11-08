package com.vaelohq.valor.ui;

import com.vaelohq.valor.Valor;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraftforge.fml.client.GuiModList;
import org.lwjgl.input.Keyboard;

/** Big-type main menu: Multiplayer leads bottom-left, the player's 3D skin stands right of centre. */
public class MainMenuScreen extends ValorScreen {
    private static final String[] ROW = {"Settings", "Mods", "Client", "Quit"};
    private int sel, lastMouseX = -1, lastMouseY = -1;

    @Override
    public void initGui() {
        super.initGui();
        long t = opened + 200;
        buttonList.add(new ValorButton(0, 19, height - 70, "Multiplayer", Ui.display(), 15, Ui.TEXT, null, t));
        buttonList.add(new ValorButton(1, 19, height - 48, "Singleplayer", Ui.regular(), 8.5f, Ui.mix(Ui.TEXT, Ui.MUTED, 0.35f), null, t + 60));
        int x = 19;
        for (int i = 0; i < ROW.length; i++) {
            ValorButton b = new ValorButton(2 + i, x, height - 31, ROW[i], Ui.regular(), 6,
                    i == 3 ? Ui.MUTED : Ui.mix(Ui.TEXT, Ui.MUTED, 0.35f), i == 2 ? "Soon" : null, t + 120);
            buttonList.add(b);
            x += b.width + 9;
        }
    }

    @Override
    protected void drawContent(int mouseX, int mouseY) {
        if (mouseX != lastMouseX || mouseY != lastMouseY) {
            for (GuiButton b : buttonList)
                if (mouseX >= b.xPosition && mouseY >= b.yPosition && mouseX < b.xPosition + b.width && mouseY < b.yPosition + b.height) sel = b.id;
            lastMouseX = mouseX;
            lastMouseY = mouseY;
        }
        float a = Ui.ease(time() / 0.5f);

        Ui.logo(17, 13, 14, a);
        Ui.semibold().draw("Valor", 35, 15.5f, 9.5f, Ui.alpha(Ui.TEXT, a));

        ValorFont f = Ui.regular();
        String name = mc.getSession().getUsername(), kind = "0".equals(mc.getSession().getToken()) ? "Offline account" : "Minecraft account";
        float tw = Math.max(f.width(name, 5.4f), f.width(kind, 4.8f)), ax = width - 15 - tw;
        SkinPreview.face((int) (ax - 13), 14, 10);
        f.draw(name, ax, 14, 5.4f, Ui.alpha(Ui.TEXT, a));
        f.draw(kind, ax, 20.5f, 4.8f, Ui.alpha(Ui.MUTED, a));

        float px = height / 62f, sx = width * 0.72f, feet = height - 52;
        Ui.glow(sx, feet + px * 0.4f, px * 10, px * 2.2f, Ui.alpha(0xFF000000, 0.6f * a));
        float yaw = -28 + Math.max(-1, Math.min(1, (mouseX - sx) / (width * 0.5f))) * 14;
        SkinPreview.draw(sx, feet, px, yaw, time());

        for (GuiButton b : buttonList) ((ValorButton) b).selected = b.id == sel;
        ValorButton s = (ValorButton) buttonList.get(sel);
        Ui.circle(13, s.midY(), 2.6f, Ui.alpha(Ui.CYAN, 0.18f * a));
        Ui.circle(13, s.midY(), 1.1f, Ui.alpha(Ui.CYAN, a));

        f.draw("Valor " + Valor.VERSION + "  ·  Minecraft 1.8.9", 10, height - 11, 5, Ui.alpha(Ui.MUTED, a * 0.85f));
        String c = "Copyright Mojang AB. Do not distribute!";
        f.draw(c, width - f.width(c, 5) - 10, height - 11, 5, Ui.alpha(Ui.MUTED, a * 0.85f));
    }

    @Override
    protected void actionPerformed(GuiButton b) {
        switch (b.id) {
            case 0: mc.displayGuiScreen(new GuiMultiplayer(this)); break;
            case 1: mc.displayGuiScreen(new GuiSelectWorld(this)); break;
            case 2: mc.displayGuiScreen(new GuiOptions(this, mc.gameSettings)); break;
            case 3: mc.displayGuiScreen(new GuiModList(this)); break;
            case 4: toast("Coming soon"); break;
            case 5: mc.shutdown(); break;
        }
    }

    @Override
    protected void keyTyped(char c, int key) {
        int n = buttonList.size();
        if (key == Keyboard.KEY_UP || key == Keyboard.KEY_LEFT) sel = (sel + n - 1) % n;
        else if (key == Keyboard.KEY_DOWN || key == Keyboard.KEY_RIGHT || key == Keyboard.KEY_TAB) sel = (sel + 1) % n;
        else if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
            GuiButton b = buttonList.get(sel);
            b.playPressSound(mc.getSoundHandler());
            actionPerformed(b);
        } else if (key == Keyboard.KEY_RSHIFT) toast("Coming soon");
    }
}
