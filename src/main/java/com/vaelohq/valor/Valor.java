package com.vaelohq.valor;

import com.vaelohq.valor.ui.IntroScreen;
import com.vaelohq.valor.ui.MainMenuScreen;
import com.vaelohq.valor.ui.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = Valor.MODID, version = Valor.VERSION, useMetadata = true, clientSideOnly = true)
public class Valor {
    public static final String MODID = "valor";
    public static final String NAME = "Valor";
    public static final String VERSION = "0.1.0";

    public static final Logger LOGGER = LogManager.getLogger(NAME);
    private boolean introShown;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        LOGGER.info("{} {} loaded", NAME, VERSION);
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent e) {
        if (e.gui instanceof GuiMainMenu) {
            e.gui = introShown ? new MainMenuScreen() : new IntroScreen();
            introShown = true;
        } else if (e.gui == null && Minecraft.getMinecraft().theWorld != null) {
            Ui.release(); // back in game: menu fonts, logo and shader cost nothing while playing
        }
    }
}
