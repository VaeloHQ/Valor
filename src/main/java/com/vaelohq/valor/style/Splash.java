package com.vaelohq.valor.style;

import com.vaelohq.valor.Valor;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Properties;

/**
 * Forge's loading screen runs before any mod loads, so Valor cannot draw it, only configure it through
 * config/splash.properties: Valor colours and no logos. Forge multiplies both logos by the background colour (it
 * expects white), so on navy they would be dark smudges; they point at a fully transparent texture that ships with
 * vanilla, so there is no extra file that could go missing (a missing texture crashes Forge at startup).
 * Takes effect from the next launch.
 */
public final class Splash {
    private static final String TRANSPARENT = "textures/blocks/redstone_dust_cross_overlay.png";

    private Splash() {
    }

    public static void configure(File gameDir) {
        File file = new File(gameDir, "config/splash.properties");
        try {
            Properties p = new Properties();
            if (file.isFile()) try (Reader r = new FileReader(file)) {
                p.load(r);
            }
            p.setProperty("background", "0x060A1E");
            p.setProperty("font", "0xF4F7FF");
            p.setProperty("barBorder", "0x2563FF");
            p.setProperty("barBackground", "0x0B1020");
            p.setProperty("bar", "0x2563FF");
            p.setProperty("logoTexture", TRANSPARENT);
            p.setProperty("forgeTexture", TRANSPARENT);
            try (Writer w = new FileWriter(file)) {
                p.store(w, "Splash screen properties");
            }
        } catch (IOException e) {
            Valor.LOGGER.warn("Couldn't set up the Valor loading screen", e);
        }
    }
}
