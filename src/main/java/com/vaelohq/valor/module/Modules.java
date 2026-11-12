package com.vaelohq.valor.module;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.vaelohq.valor.Valor;
import com.vaelohq.valor.module.mods.*;
import com.vaelohq.valor.ui.HudEditorScreen;
import com.vaelohq.valor.ui.ModsScreen;
import com.vaelohq.valor.ui.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
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
    /** Switches to the next profile; not bound by default so it can't fire by accident mid-fight (set it in Controls). */
    public static final KeyBinding PROFILE_KEY = new KeyBinding("Next profile", Keyboard.KEY_NONE, "Valor");
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
        ClientRegistry.registerKeyBinding(PROFILE_KEY);
        for (Module m : ALL) if (m instanceof Zoom) ClientRegistry.registerKeyBinding(((Zoom) m).key);
        MinecraftForge.EVENT_BUS.register(new Modules.Events());
        MinecraftForge.EVENT_BUS.register(new Clicks());
        load();
    }

    // ---- Profiles ----
    // config/valor-profiles.json: {"active": name, "profiles": [{"name", "servers": [hosts], "modules": {id: module}}]}.
    // The launcher reads the names and sets "active" before starting the game.

    /** A saved setup: which modules are on, their settings and HUD layout, and the servers that switch to it. */
    public static final class Profile {
        public String name;
        public final List<String> servers = new ArrayList<>();
        JsonObject modules;   // null: defaults

        Profile(String name, JsonObject modules) {
            this.name = name;
            this.modules = modules;
        }
    }

    private static final List<Profile> PROFILES = new ArrayList<>();
    private static Profile active;

    public static List<Profile> profiles() {
        return Collections.unmodifiableList(PROFILES);
    }

    public static Profile active() {
        return active;
    }

    private static File file() {
        return new File(Loader.instance().getConfigDir(), "valor-profiles.json");
    }

    private static void load() {
        try {
            File f = file();
            if (f.isFile()) {
                JsonObject root;
                try (Reader r = new FileReader(f)) {
                    root = new JsonParser().parse(r).getAsJsonObject();
                }
                for (JsonElement e : root.getAsJsonArray("profiles")) {
                    JsonObject o = e.getAsJsonObject();
                    Profile p = new Profile(o.get("name").getAsString(), o.has("modules") ? o.getAsJsonObject("modules") : null);
                    if (o.has("servers")) for (JsonElement h : o.getAsJsonArray("servers")) p.servers.add(h.getAsString());
                    if (find(p.name) == null) PROFILES.add(p);
                }
                if (root.has("active")) active = find(root.get("active").getAsString());
            }
        } catch (Exception e) {
            Valor.LOGGER.warn("Couldn't read {}, starting with the PvP profile", file(), e);
            PROFILES.clear();
        }
        if (PROFILES.isEmpty()) {
            // First start, or the setup from before profiles (config/valor-modules.json) becomes the PvP profile.
            JsonObject old = null;
            File f = new File(Loader.instance().getConfigDir(), "valor-modules.json");
            if (f.isFile()) {
                try (Reader r = new FileReader(f)) {
                    old = new JsonParser().parse(r).getAsJsonObject();
                } catch (Exception e) {
                    Valor.LOGGER.warn("Couldn't read {}", f, e);
                }
            }
            PROFILES.add(new Profile("PvP", old));
        }
        if (active == null) active = PROFILES.get(0);
        apply(active.modules);
        save();
    }

    public static Profile find(String name) {
        for (Profile p : PROFILES) if (p.name.equalsIgnoreCase(name)) return p;
        return null;
    }

    /** Puts every module into the state stored in json (modules missing from it get their defaults). */
    private static void apply(JsonObject json) {
        for (Module m : ALL) {
            m.reset();
            JsonObject o = json != null && json.has(m.id) ? json.getAsJsonObject(m.id) : null;
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
                m.reset();
                m.setEnabled(DEFAULT_ON.contains(m.id));
            }
        }
    }

    /** Modules that start switched on for new players (the PvP profile). */
    private static final List<String> DEFAULT_ON = java.util.Arrays.asList("keystrokes", "cps", "combo", "reach", "armor", "potions",
            "togglesprint");

    /** The modules as they are now. */
    private static JsonObject current() {
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
        return root;
    }

    /** Stores the modules in the active profile and writes every profile to disk. */
    public static void save() {
        active.modules = current();
        JsonObject root = new JsonObject();
        root.addProperty("active", active.name);
        JsonArray list = new JsonArray();
        for (Profile p : PROFILES) {
            JsonObject o = new JsonObject();
            o.addProperty("name", p.name);
            JsonArray hosts = new JsonArray();
            for (String h : p.servers) hosts.add(new JsonPrimitive(h));
            o.add("servers", hosts);
            if (p.modules != null) o.add("modules", p.modules);
            list.add(o);
        }
        root.add("profiles", list);
        try (Writer w = new FileWriter(file())) {
            GSON.toJson(root, w);
        } catch (Exception e) {
            Valor.LOGGER.warn("Couldn't save {}", file(), e);
        }
    }

    public static void switchTo(Profile p) {
        if (p == active) return;
        save();
        active = p;
        apply(p.modules);
        save();
    }

    /** A new profile with the current setup, switched to. */
    public static Profile create(String name) {
        save();
        Profile p = new Profile(name, new JsonParser().parse(active.modules.toString()).getAsJsonObject());
        PROFILES.add(p);
        active = p;
        save();
        return p;
    }

    /** Deletes a profile (never the last one); deleting the active one switches to the first left. */
    public static void delete(Profile p) {
        if (PROFILES.size() < 2) return;
        if (p == active) switchTo(PROFILES.get(PROFILES.get(0) == p ? 1 : 0));
        PROFILES.remove(p);
        save();
    }

    /** The next profile in the list (the profile hotkey). */
    public static Profile next() {
        return PROFILES.get((PROFILES.indexOf(active) + 1) % PROFILES.size());
    }

    /** The profile that lists this server (host, or host:port), or null. "hypixel.net" also matches "mc.hypixel.net". */
    public static Profile forServer(String address) {
        String host = hostOf(address);
        for (Profile p : PROFILES)
            for (String s : p.servers) {
                String h = hostOf(s);
                if (!h.isEmpty() && (host.equals(h) || host.endsWith("." + h))) return p;
            }
        return null;
    }

    private static String hostOf(String address) {
        String h = address.trim().toLowerCase(java.util.Locale.ROOT);
        int colon = h.lastIndexOf(':');
        if (colon >= 0 && h.indexOf(':') == colon) h = h.substring(0, colon);   // strip a port, leave IPv6 alone
        return h.endsWith(".") ? h.substring(0, h.length() - 1) : h;
    }

    /** Shows the active profile above the hotbar. */
    private static void announce() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.ingameGUI != null) mc.ingameGUI.setRecordPlaying("Profile: " + active.name, false);
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
            if (e.phase != TickEvent.Phase.END) return;
            if (MENU_KEY.isPressed() && mc.currentScreen == null && mc.theWorld != null) mc.displayGuiScreen(new ModsScreen(null));
            if (PROFILE_KEY.isPressed() && PROFILES.size() > 1) {
                switchTo(next());
                announce();
            }
        }

        /** Joining a server that a profile lists switches to that profile. Fires on the network thread. */
        @SubscribeEvent
        public void onConnect(FMLNetworkEvent.ClientConnectedToServerEvent e) {
            if (e.isLocal) return;
            Minecraft mc = Minecraft.getMinecraft();
            java.net.SocketAddress remote = e.manager.getRemoteAddress();
            String fallback = remote instanceof java.net.InetSocketAddress ? ((java.net.InetSocketAddress) remote).getHostString() : "";
            mc.addScheduledTask(() -> {
                ServerData d = mc.getCurrentServerData();
                Profile p = forServer(d != null ? d.serverIP : fallback);
                if (p == null || p == active) return;
                switchTo(p);
                announce();
            });
        }
    }
}
