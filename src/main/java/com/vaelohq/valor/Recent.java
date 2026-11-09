package com.vaelohq.valor;

import net.minecraftforge.fml.common.Loader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/** The last server or world the player joined, for the main menu's Continue card. Stored in config/valor-recent.properties. */
public final class Recent {
    public static String name = "", target = "";
    public static boolean world;
    public static long time;
    private static boolean loaded;

    private static File file() {
        return new File(Loader.instance().getConfigDir(), "valor-recent.properties");
    }

    public static void load() {
        if (loaded) return;
        loaded = true;
        File f = file();
        if (!f.isFile()) return;
        Properties p = new Properties();
        try (InputStream in = new FileInputStream(f)) {
            p.load(in);
            name = p.getProperty("name", "");
            target = p.getProperty("target", "");
            world = Boolean.parseBoolean(p.getProperty("world"));
            time = Long.parseLong(p.getProperty("time", "0"));
        } catch (Exception e) {
            Valor.LOGGER.warn("Couldn't read {}", f, e);
        }
    }

    static void save(String name, String target, boolean world) {
        Recent.name = name;
        Recent.target = target;
        Recent.world = world;
        Recent.time = System.currentTimeMillis();
        loaded = true;
        Properties p = new Properties();
        p.setProperty("name", name);
        p.setProperty("target", target);
        p.setProperty("world", String.valueOf(world));
        p.setProperty("time", String.valueOf(time));
        try (OutputStream out = new FileOutputStream(file())) {
            p.store(out, "Valor: last server or world joined");
        } catch (Exception e) {
            Valor.LOGGER.warn("Couldn't write {}", file(), e);
        }
    }

    public static boolean has() {
        return !target.isEmpty();
    }

    public static String ago() {
        return ago(time);
    }

    /** "just now", "5m ago", "3h ago" or "2d ago" for a System.currentTimeMillis() timestamp. */
    public static String ago(long when) {
        long m = (System.currentTimeMillis() - when) / 60000;
        if (m < 1) return "just now";
        if (m < 60) return m + "m ago";
        if (m < 1440) return m / 60 + "h ago";
        return m / 1440 + "d ago";
    }
}
