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
 * Turns Forge's own loading screen off in config/splash.properties, so Minecraft runs its plain startup screen, which
 * Valor replaces with its own (LoadingScreen). Forge reads the file before any mod loads, so this takes effect from
 * the next launch; the very first launch with Valor still shows Forge's screen.
 */
public final class Splash {
    private Splash() {
    }

    public static void configure(File gameDir) {
        File file = new File(gameDir, "config/splash.properties");
        try {
            Properties p = new Properties();
            if (file.isFile()) try (Reader r = new FileReader(file)) {
                p.load(r);
            }
            if ("false".equals(p.getProperty("enabled"))) return;
            p.setProperty("enabled", "false");
            try (Writer w = new FileWriter(file)) {
                p.store(w, "Splash screen properties");
            }
        } catch (IOException e) {
            Valor.LOGGER.warn("Couldn't turn off Forge's loading screen", e);
        }
    }
}
