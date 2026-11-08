package com.vaelohq.valor.ui;

import com.vaelohq.valor.Valor;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraftforge.fml.client.GuiModList;
import org.lwjgl.input.Keyboard;

public class MainMenuScreen extends ValorScreen {
    private static final String[] LABELS = {"Singleplayer", "Multiplayer", "Settings", "Mods", "Client", "Quit Game"};

    @Override
    public void initGui() {
        super.initGui();
        for (int i = 0; i < LABELS.length; i++)
            buttonList.add(new ValorButton(i, width / 2, height / 2 - 16 + i * 17, LABELS[i], opened + 250 + i * 70L));
    }

    @Override
    protected void drawContent(int mouseX, int mouseY) {
        float a = Ui.ease(time() / 0.6f), cx = width / 2f, y = height / 2f - 62 + (1 - a) * 6;
        ValorFont title = Ui.semibold(), small = Ui.regular();
        float tw = title.width("Valor", 16), x = cx - (22 + 4 + tw) / 2;
        Ui.logo(x, y, 22, a);
        title.draw("Valor", x + 26, y + 3, 16, Ui.alpha(Ui.TEXT, a));
        small.draw("Valor " + Valor.VERSION + "  ·  Minecraft 1.8.9", 4, height - 11, 7, Ui.alpha(Ui.MUTED, a * 0.8f));
        String c = "Copyright Mojang AB. Do not distribute!";
        small.draw(c, width - small.width(c, 7) - 4, height - 11, 7, Ui.alpha(Ui.MUTED, a * 0.8f));
    }

    @Override
    protected void actionPerformed(GuiButton b) {
        switch (b.id) {
            case 0: mc.displayGuiScreen(new GuiSelectWorld(this)); break;
            case 1: mc.displayGuiScreen(new GuiMultiplayer(this)); break;
            case 2: mc.displayGuiScreen(new GuiOptions(this, mc.gameSettings)); break;
            case 3: mc.displayGuiScreen(new GuiModList(this)); break;
            case 4: toast("Coming soon"); break;
            case 5: mc.shutdown(); break;
        }
    }

    @Override
    protected void keyTyped(char c, int key) {
        if (key == Keyboard.KEY_RSHIFT) toast("Coming soon");
    }
}
