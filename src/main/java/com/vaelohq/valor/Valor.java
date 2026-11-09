package com.vaelohq.valor;

import com.vaelohq.valor.style.ValorPack;
import com.vaelohq.valor.ui.CreateWorldScreen;
import com.vaelohq.valor.ui.DeathScreen;
import com.vaelohq.valor.ui.IntroScreen;
import com.vaelohq.valor.mixin.GuiCreateWorldAccessor;
import com.vaelohq.valor.mixin.GuiMultiplayerAccessor;
import com.vaelohq.valor.mixin.GuiSelectWorldAccessor;
import com.vaelohq.valor.ui.MainMenuScreen;
import com.vaelohq.valor.ui.MultiplayerScreen;
import com.vaelohq.valor.ui.PauseScreen;
import com.vaelohq.valor.ui.SingleplayerScreen;
import com.vaelohq.valor.ui.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.ref.WeakReference;

@Mod(modid = Valor.MODID, version = Valor.VERSION, useMetadata = true, clientSideOnly = true)
public class Valor {
    public static final String MODID = "valor";
    public static final String NAME = "Valor";
    public static final String VERSION = "0.1.0";

    public static final Logger LOGGER = LogManager.getLogger(NAME);
    private boolean introShown;
    private WeakReference<WorldClient> recorded = new WeakReference<>(null);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        Config.load();
        ValorPack.register();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        ValorPack.watchReloads();
        MinecraftForge.EVENT_BUS.register(new com.vaelohq.valor.style.Hud());
        Account.restore();
        LOGGER.info("{} {} loaded", NAME, VERSION);
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent e) {
        Minecraft mc = Minecraft.getMinecraft();
        if (e.gui instanceof GuiMainMenu) {
            // First title screen of the launch: intro, then the account panel, then the menu.
            e.gui = introShown ? new MainMenuScreen() : new IntroScreen();
            introShown = true;
        } else if (e.gui instanceof GuiIngameMenu) {
            e.gui = new PauseScreen();
        } else if (e.gui instanceof GuiGameOver) {
            e.gui = new DeathScreen();
        } else if (e.gui instanceof GuiMultiplayer) {
            e.gui = new MultiplayerScreen(((GuiMultiplayerAccessor) e.gui).valor$parent());
        } else if (e.gui instanceof GuiSelectWorld) {
            // The world list hands FML a GuiSelectWorld wrapping itself; showing that again means going back to it.
            GuiScreen parent = ((GuiSelectWorldAccessor) e.gui).valor$parent();
            e.gui = parent instanceof SingleplayerScreen ? parent : new SingleplayerScreen(parent);
        } else if (e.gui instanceof GuiCreateWorld) {
            CreateWorldScreen back = CreateWorldScreen.returnFrom((GuiCreateWorld) e.gui);
            e.gui = back != null ? back : new CreateWorldScreen(((GuiCreateWorldAccessor) e.gui).valor$parent());
        } else if (e.gui == null && mc.theWorld != null) {
            Ui.release(); // back in game: menu fonts, textures and shaders cost nothing while playing
            if (mc.theWorld != recorded.get()) {
                recorded = new WeakReference<>(mc.theWorld);
                if (mc.isSingleplayer() && mc.getIntegratedServer() != null)
                    Recent.save(mc.getIntegratedServer().getWorldName(), mc.getIntegratedServer().getFolderName(), true);
                else if (mc.getCurrentServerData() != null)
                    Recent.save(mc.getCurrentServerData().serverName, mc.getCurrentServerData().serverIP, false);
            }
        }
    }
}
