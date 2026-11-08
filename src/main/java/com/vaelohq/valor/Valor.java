package com.vaelohq.valor;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = Valor.MODID, version = Valor.VERSION, useMetadata = true, clientSideOnly = true)
public class Valor {
    public static final String MODID = "valor";
    public static final String NAME = "Valor";
    public static final String VERSION = "0.1.0";

    public static final Logger LOGGER = LogManager.getLogger(NAME);

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        LOGGER.info("{} {} loaded", NAME, VERSION);
    }
}
