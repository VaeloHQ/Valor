package com.vaelohq.valor;

import net.minecraftforge.fml.common.Loader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/** Valor settings, stored in config/valor.properties. */
public final class Config {
    /** Valor's own look for the HUD, inventories and vanilla GUI textures. Player resource packs still override it. */
    public static boolean valorStyle = true;

    private static File file() {
        return new File(Loader.instance().getConfigDir(), "valor.properties");
    }

    public static void load() {
        Properties p = new Properties();
        File f = file();
        if (f.isFile()) {
            try (InputStream in = new FileInputStream(f)) {
                p.load(in);
            } catch (Exception e) {
                Valor.LOGGER.warn("Couldn't read {}", f, e);
            }
        }
        valorStyle = Boolean.parseBoolean(p.getProperty("valorStyle", "true"));
    }

    public static void save() {
        Properties p = new Properties();
        p.setProperty("valorStyle", String.valueOf(valorStyle));
        try (OutputStream out = new FileOutputStream(file())) {
            p.store(out, "Valor settings");
        } catch (Exception e) {
            Valor.LOGGER.warn("Couldn't write {}", file(), e);
        }
    }
}
