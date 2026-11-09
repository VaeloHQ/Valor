package com.vaelohq.valor.module;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vaelohq.valor.Valor;
import com.vaelohq.valor.module.mods.*;
import com.vaelohq.valor.ui.HudEditorScreen;
import com.vaelohq.valor.ui.ModsScreen;
import com.vaelohq.valor.ui.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** All modules: creates them, saves them to config/valor-modules.json, draws HUD modules and opens the mods menu. */
public final class Modules {
    /** Opens the mods menu in game (Right Shift by default; changeable in Controls). */
    public static final KeyBinding MENU_KEY = new KeyBinding("Mods menu", Keyboard.KEY_RSHIFT, "Valor");
    private static final List<Module> ALL = new ArrayList<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private Modules() {
    }

    public static List<Module> all() {
        return Collections.unmodifiableList(ALL);
    }

    public static void init() {
        Collections.addAll(ALL, new Keystrokes(), new CpsCounter(), new ComboCounter(), new ReachDisplay(), new ArmorStatus(),
                new PotionStatus(), new ToggleSprint(), new ToggleSneak(), new Zoom(), new Hitboxes(), new ArrowCounter(),
                new FpsCounter(), new PingDisplay(), new Coordinates(), new DirectionHud(), new Clock(), new MemoryUsage(),
                new ServerAddress(), new Fullbright());
        ClientRegistry.registerKeyBinding(MENU_KEY);
        for (Module m : ALL) if (m instanceof Zoom) ClientRegistry.registerKeyBinding(((Zoom) m).key);
        MinecraftForge.EVENT_BUS.register(new Modules.Events());
        MinecraftForge.EVENT_BUS.register(new Clicks());
        load();
    }

    private static File file() {
        return new File(Loader.instance().getConfigDir(), "valor-modules.json");
    }

    private static void load() {
        JsonObject root = null;
        File f = file();
        if (f.isFile()) {
            try (Reader r = new FileReader(f)) {
                root = new JsonParser().parse(r).getAsJsonObject();
            } catch (Exception e) {
                Valor.LOGGER.warn("Couldn't read {}, using defaults", f, e);
            }
        }
        for (Module m : ALL) {
            JsonObject o = root != null && root.has(m.id) ? root.getAsJsonObject(m.id) : null;
            if (o == null) {
                m.setEnabled(DEFAULT_ON.contains(m.id));
                continue;
            }
            try {
                if (m instanceof HudModule) {
                    HudModule h = (HudModule) m;
                    if (o.has("x")) h.x = clamp(o.get("x").getAsFloat());
                    if (o.has("y")) h.y = clamp(o.get("y").getAsFloat());
                    if (o.has("scale")) h.scale = Math.max(0.5f, Math.min(2.5f, o.get("scale").getAsFloat()));
                }
                JsonObject s = o.has("settings") ? o.getAsJsonObject("settings") : new JsonObject();
                for (Setting setting : m.settings) {
                    JsonElement v = s.get(setting.id);
                    if (v != null) setting.load(v);
                }
                m.setEnabled(o.has("enabled") && o.get("enabled").getAsBoolean());
            } catch (Exception e) {
                Valor.LOGGER.warn("Bad saved settings for module {}, using defaults", m.id, e);
            }
        }
    }

    /** Modules that start switched on for new players. */
    private static final List<String> DEFAULT_ON = java.util.Arrays.asList("keystrokes", "cps", "fps", "armor", "potions");

    public static void save() {
        JsonObject root = new JsonObject();
        for (Module m : ALL) {
            JsonObject o = new JsonObject();
            o.addProperty("enabled", m.enabled());
            if (m instanceof HudModule) {
                HudModule h = (HudModule) m;
                o.addProperty("x", h.x);
                o.addProperty("y", h.y);
                o.addProperty("scale", h.scale);
            }
            JsonObject s = new JsonObject();
            for (Setting setting : m.settings) s.add(setting.id, setting.save());
            o.add("settings", s);
            root.add(m.id, o);
        }
        try (Writer w = new FileWriter(file())) {
            GSON.toJson(root, w);
        } catch (Exception e) {
            Valor.LOGGER.warn("Couldn't save {}", file(), e);
        }
    }

    private static float clamp(float v) {
        return Math.max(0, Math.min(1, v));
    }

    /** Draws all visible HUD modules at their places (also used by the HUD editor, with preview data out of game). */
    public static void drawHud(float screenW, float screenH, boolean preview) {
        Ui.frame();
        for (Module m : ALL) {
            if (!(m instanceof HudModule) || !m.enabled()) continue;
            HudModule h = (HudModule) m;
            if (!preview && !h.visible()) continue;
            GlStateManager.pushMatrix();
            GlStateManager.translate(h.screenX(screenW, preview), h.screenY(screenH, preview), 0);
            GlStateManager.scale(h.scale, h.scale, 1);
            h.draw(preview);
            GlStateManager.popMatrix();
        }
        GlStateManager.color(1, 1, 1, 1);
    }

    public static final class Events {
        @SubscribeEvent
        public void onOverlay(RenderGameOverlayEvent.Post e) {
            Minecraft mc = Minecraft.getMinecraft();
            if (e.type != RenderGameOverlayEvent.ElementType.ALL || mc.currentScreen instanceof HudEditorScreen) return;
            ScaledResolution sr = e.resolution;
            drawHud(sr.getScaledWidth(), sr.getScaledHeight(), false);
        }

        @SubscribeEvent
        public void onTick(TickEvent.ClientTickEvent e) {
            Minecraft mc = Minecraft.getMinecraft();
            if (e.phase == TickEvent.Phase.END && MENU_KEY.isPressed() && mc.currentScreen == null && mc.theWorld != null)
                mc.displayGuiScreen(new ModsScreen(null));
        }
    }
}
