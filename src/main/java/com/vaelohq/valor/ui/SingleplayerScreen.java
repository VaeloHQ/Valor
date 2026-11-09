package com.vaelohq.valor.ui;

import com.vaelohq.valor.Recent;
import net.minecraft.client.AnvilConverterException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiErrorScreen;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.storage.ISaveFormat;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.SaveFormatComparator;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.fml.client.FMLClientHandler;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Valor world list. Same behaviour as vanilla's GuiSelectWorld: worlds sorted by last played, play, create, rename,
 * delete and re-create. Enter or a double-click plays.
 */
public class SingleplayerScreen extends ValorScreen {
    private static final int PLAY = 0, CREATE = 1, RENAME = 2, DELETE = 3, RECREATE = 4, BACK = 5;
    private static final float ROW = 32, GAP = 4, ICON = ROW - 8;

    private final GuiScreen parent;
    private final ScrollList list = new ScrollList();
    private final Map<String, Float> hovers = new HashMap<>();
    private List<SaveFormatComparator> worlds = new ArrayList<>();
    private int selected = -1;
    private long lastClick;
    private boolean loading;
    private Widgets.Pill play, rename, delete, recreate;

    public SingleplayerScreen(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        super.initGui();
        loading = false;   // back here after a load was cancelled (vanilla stays stuck)
        String keep = selected >= 0 && selected < worlds.size() ? worlds.get(selected).getFileName() : null;
        try {
            worlds = mc.getSaveLoader().getSaveList();
            Collections.sort(worlds);
        } catch (AnvilConverterException e) {
            com.vaelohq.valor.Valor.LOGGER.error("Couldn't load level list", e);
            mc.displayGuiScreen(new GuiErrorScreen("Unable to load worlds", e.getMessage()));
            return;
        }
        selected = -1;
        for (int i = 0; i < worlds.size(); i++) if (worlds.get(i).getFileName().equals(keep)) selected = i;

        float lw = Math.min(330, width - 40), lx = (width - lw) / 2, ly = 44;
        list.place(lx, ly, lw, height - ly - 58);
        long t = opened + 120;
        float w2 = (lw - 6) / 2, w4 = (lw - 18) / 4, y1 = height - 50, y2 = height - 28;
        play = new Widgets.Pill(PLAY, lx, y1, w2, 16, 8, Ui.I_PLAY, "Play world", true, t);
        buttonList.add(play);
        buttonList.add(new Widgets.Pill(CREATE, lx + w2 + 6, y1, w2, 16, 8, -1, "Create new world", false, t));
        rename = new Widgets.Pill(RENAME, lx, y2, w4, 16, 8, -1, "Rename", false, t + 40);
        delete = new Widgets.Pill(DELETE, lx + w4 + 6, y2, w4, 16, 8, -1, "Delete", false, t + 40).danger();
        recreate = new Widgets.Pill(RECREATE, lx + (w4 + 6) * 2, y2, w4, 16, 8, -1, "Re-create", false, t + 40);
        buttonList.add(rename);
        buttonList.add(delete);
        buttonList.add(recreate);
        buttonList.add(new Widgets.Pill(BACK, lx + (w4 + 6) * 3, y2, w4, 16, 8, -1, "Back", false, t + 40));
        select(selected);
    }

    private void select(int i) {
        selected = i;
        if (play != null) play.enabled = rename.enabled = delete.enabled = recreate.enabled = i >= 0 && i < worlds.size();
        if (i >= 0) list.reveal(i * (ROW + GAP), ROW);
    }

    /** Display name, or "World N" for a world saved without one (as vanilla). */
    private String name(int i) {
        String s = worlds.get(i).getDisplayName();
        return s == null || s.isEmpty() ? "World " + (i + 1) : s;
    }

    @Override
    protected void drawContent(int mx, int my) {
        float a = Ui.ease(time() / 0.3f), cx = width / 2f;
        Ui.bold().drawCentered("Singleplayer", cx, 12 + (1 - a) * 4, 13, Ui.alpha(Ui.TEXT, a));
        int n = worlds.size();
        Ui.regular().drawCentered(n == 1 ? "1 world" : n + " worlds", cx, 29 + (1 - a) * 4, 5.5f, Ui.alpha(Ui.MUTED, a));
        boolean inList = list.contains(mx, my);
        list.begin(n * (ROW + GAP));
        for (int i = 0; i < n; i++) {
            float y = list.y + i * (ROW + GAP) - list.scroll;
            if (y + ROW < list.y || y > list.y + list.h) continue;
            drawWorld(i, y, inList && my >= y && my < y + ROW, a * Ui.ease((time() - 0.04f * Math.min(i, 8)) / 0.35f));
        }
        list.end(mx, my, a);
        if (n == 0) {
            float ey = list.y + 40;
            Ui.semibold().drawCentered("No worlds yet", cx, ey, 7, Ui.alpha(Ui.TEXT, a));
            Ui.regular().drawCentered("Create one to start playing.", cx, ey + 12, 5.5f, Ui.alpha(Ui.MUTED, a));
        }
    }

    private void drawWorld(int i, float y, boolean hov, float a) {
        SaveFormatComparator w = worlds.get(i);
        float hv = Ui.approach(hovers.getOrDefault(w.getFileName(), 0f), hov ? 1 : 0);
        hovers.put(w.getFileName(), hv);
        boolean sel = i == selected;
        float x = list.x, lw = list.w;
        Shapes.draw(x, y, lw, ROW, 8, Ui.alpha(sel ? 0xE0182A5C : Ui.mix(0xB4141A30, 0xD01B2340, hv), a),
                Ui.alpha(sel ? 0xE0101C44 : Ui.mix(0xB40A0F20, 0xD0111933, hv), a),
                Ui.alpha(sel ? 0xCC4C85FF : Ui.mix(0x1FFFFFFF, 0x38FFFFFF, hv), a), sel ? 0.8f : 0.5f, Ui.alpha(0x66000000, a), 5, 2);

        String name = name(i);
        float ix = x + 4, iy = y + 4;
        boolean hardcore = w.isHardcoreModeEnabled();
        Ui.placeholder(hardcore ? "world_hardcore" : "world", ix, iy, ICON, 6, a);

        float tx = x + ICON + 10, room = lw - ICON - 18;
        ValorFont semi = Ui.semibold(), small = Ui.regular();
        semi.draw(semi.fit(name, 6.5f, room), tx, y + 4.5f, 6.5f, Ui.alpha(Ui.TEXT, a));
        if (w.requiresConversion()) {
            small.draw("Must be converted to the new save format", tx, y + 14, 5.2f, Ui.alpha(0xFFFFC84A, a));
        } else {
            String mode = hardcore ? "Hardcore" : mode(w.getEnumGameType());
            small.draw(mode, tx, y + 14, 5.2f, Ui.alpha(hardcore ? Ui.DANGER : Ui.MUTED, a));
            if (w.getCheatsEnabled()) small.draw("  ·  Cheats", tx + small.width(mode, 5.2f), y + 14, 5.2f, Ui.alpha(Ui.MUTED, a));
        }
        String info = "Played " + Recent.ago(w.getLastTimePlayed()) + "  ·  " + w.getFileName();
        small.draw(small.fit(info, 5.2f, room), tx, y + 21.5f, 5.2f, Ui.alpha(0xFF6B7591, a));
    }

    private static String mode(WorldSettings.GameType type) {
        switch (type) {
            case CREATIVE: return "Creative";
            case ADVENTURE: return "Adventure";
            case SPECTATOR: return "Spectator";
            default: return "Survival";
        }
    }

    private int rowAt(int mx, int my) {
        if (!list.contains(mx, my)) return -1;
        int i = (int) Math.floor((my - list.y + list.scroll) / (ROW + GAP));
        float y = list.y + i * (ROW + GAP) - list.scroll;
        return i >= 0 && i < worlds.size() && my < y + ROW ? i : -1;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        list.wheel(Mouse.getEventDWheel(), ROW + GAP);
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        super.mouseClicked(mx, my, button);
        if (button != 0 || list.press(mx, my)) return;
        int i = rowAt(mx, my);
        if (i < 0) return;
        long now = Minecraft.getSystemTime();
        boolean twice = i == selected && now - lastClick < 250;
        select(i);
        lastClick = now;
        if (twice) play(i);
    }

    @Override
    protected void mouseClickMove(int mx, int my, int button, long held) {
        list.drag(my);
    }

    @Override
    protected void mouseReleased(int mx, int my, int state) {
        super.mouseReleased(mx, my, state);
        list.release();
    }

    @Override
    protected void keyTyped(char c, int key) {
        int n = worlds.size();
        if (key == Keyboard.KEY_ESCAPE) mc.displayGuiScreen(parent);
        else if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
            if (play.enabled) play(selected);
        } else if ((key == Keyboard.KEY_UP || key == Keyboard.KEY_DOWN) && n > 0) {
            int dir = key == Keyboard.KEY_UP ? -1 : 1;
            select(selected < 0 ? (dir > 0 ? 0 : n - 1) : Math.max(0, Math.min(n - 1, selected + dir)));
        }
    }

    /** Loads the world the way vanilla does; FML wants a GuiSelectWorld to return to if loading needs a question. */
    private void play(int i) {
        mc.displayGuiScreen(null);
        if (loading) return;
        loading = true;
        String folder = worlds.get(i).getFileName();
        if (mc.getSaveLoader().canLoadWorld(folder))
            FMLClientHandler.instance().tryLoadExistingWorld(new GuiSelectWorld(this), folder, name(i));
    }

    @Override
    protected void actionPerformed(GuiButton b) {
        switch (b.id) {
            case PLAY: play(selected); break;
            case CREATE: mc.displayGuiScreen(new CreateWorldScreen(this)); break;
            case RENAME: {
                String folder = worlds.get(selected).getFileName();
                WorldInfo info = mc.getSaveLoader().getWorldInfo(folder);
                String current = info != null ? info.getWorldName() : name(selected);
                mc.displayGuiScreen(new PromptScreen(this, "Rename world", "World name", current, 32, "Rename", text -> {
                    if (text != null) mc.getSaveLoader().renameWorld(folder, text);
                    mc.displayGuiScreen(this);
                }));
                break;
            }
            case DELETE: {
                String folder = worlds.get(selected).getFileName();
                mc.displayGuiScreen(new ConfirmScreen(this, "Delete this world?", "'" + name(selected) + "' will be lost forever! (A long time!)",
                        "Delete", "Cancel", true, yes -> {
                    if (yes) {
                        ISaveFormat saves = mc.getSaveLoader();
                        saves.flushCache();
                        saves.deleteWorldDirectory(folder);
                        selected = -1;
                    }
                    mc.displayGuiScreen(this);
                }));
                break;
            }
            case RECREATE: {
                ISaveHandler handler = mc.getSaveLoader().getSaveLoader(worlds.get(selected).getFileName(), false);
                WorldInfo info = handler.loadWorldInfo();
                handler.flush();
                mc.displayGuiScreen(new CreateWorldScreen(this).recreate(info));
                break;
            }
            case BACK: mc.displayGuiScreen(parent); break;
        }
    }
}
