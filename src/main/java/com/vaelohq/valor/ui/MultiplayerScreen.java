package com.vaelohq.valor.ui;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.network.LanServerDetector;
import net.minecraft.client.network.OldServerPinger;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.FMLClientHandler;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Valor server list. Same behaviour as vanilla's GuiMultiplayer: saved servers are pinged in the background, LAN games
 * are found, servers can be added, edited, removed and reordered (the arrows on a hovered row, or Shift + Up/Down).
 * Enter, a double-click or a click on the icon joins; F5 refreshes.
 */
public class MultiplayerScreen extends ValorScreen {
    private static final ThreadPoolExecutor PINGS = new ScheduledThreadPoolExecutor(5,
            new ThreadFactoryBuilder().setNameFormat("Server Pinger #%d").setDaemon(true).build());
    private static final int JOIN = 0, DIRECT = 1, ADD = 2, EDIT = 3, DELETE = 4, REFRESH = 5, BACK = 6;
    private static final float ROW = 32, GAP = 4, SCAN = 18, ICON = ROW - 8;

    private final GuiScreen parent;
    private final OldServerPinger pinger = new OldServerPinger();
    private final ServerList servers;
    private final List<Entry> entries = new ArrayList<>();
    private final LanServerDetector.LanServerList lan = new LanServerDetector.LanServerList();
    private final Map<Object, Float> hovers = new HashMap<>();
    private List<LanServerDetector.LanServer> lanServers = new ArrayList<>();
    private LanServerDetector.ThreadLanServerFind lanThread;
    private int selected = -1;
    private final ScrollList list = new ScrollList();
    private float lx, ly, lw, lh;
    private long lastClick;
    private String tooltip;
    private Widgets.Pill join, edit, delete;

    public MultiplayerScreen(GuiScreen parent) {
        this.parent = parent;
        FMLClientHandler.instance().setupServerList();
        servers = new ServerList(Minecraft.getMinecraft());
        rebuild();
    }

    /** One saved server with its icon texture (registered as servers/&lt;ip&gt;/icon, as vanilla does). */
    private final class Entry {
        final ServerData data;
        final ResourceLocation iconLoc;
        DynamicTexture icon;
        String iconData;

        Entry(ServerData data) {
            this.data = data;
            iconLoc = new ResourceLocation("servers/" + data.serverIP + "/icon");
            icon = (DynamicTexture) Minecraft.getMinecraft().getTextureManager().getTexture(iconLoc);
        }

        void ping() {
            if (data.field_78841_f) return;
            data.field_78841_f = true;
            data.pingToServer = -2;
            data.serverMOTD = "";
            data.populationInfo = "";
            PINGS.submit(() -> {
                try {
                    pinger.ping(data);
                } catch (UnknownHostException e) {
                    data.pingToServer = -1;
                    data.serverMOTD = EnumChatFormatting.DARK_RED + "Can't resolve hostname";
                } catch (Exception e) {
                    data.pingToServer = -1;
                    data.serverMOTD = EnumChatFormatting.DARK_RED + "Can't connect to server.";
                }
            });
        }

        /** Uploads the icon the server sent (a 64x64 PNG in base64) when it changes, then saves the list, like vanilla. */
        void updateIcon() {
            String b64 = data.getBase64EncodedIconData();
            if (b64 == null || b64.equals(iconData)) return;
            iconData = b64;
            BufferedImage img = null;
            try {
                img = TextureUtil.readBufferedImage(new ByteArrayInputStream(Base64.getMimeDecoder().decode(b64)));
                if (img.getWidth() != 64 || img.getHeight() != 64) throw new IOException("icon must be 64x64");
            } catch (Exception e) {
                com.vaelohq.valor.Valor.LOGGER.error("Invalid icon for server {} ({})", data.serverName, data.serverIP, e);
                data.setBase64EncodedIconData(null);
                img = null;
            }
            if (img != null) {
                if (icon == null) {
                    icon = new DynamicTexture(64, 64);
                    mc.getTextureManager().loadTexture(iconLoc, icon);
                }
                img.getRGB(0, 0, 64, 64, icon.getTextureData(), 0, 64);
                icon.updateDynamicTexture();
            }
            servers.saveServerList();
        }
    }

    private void rebuild() {
        entries.clear();
        for (int i = 0; i < servers.countServers(); i++) entries.add(new Entry(servers.getServerData(i)));
    }

    private int total() {
        return entries.size() + lanServers.size();
    }

    /** Top of row i inside the list; LAN games come after the saved servers and the scanning line. */
    private float rowY(int i) {
        int n = entries.size();
        return i < n ? i * (ROW + GAP) : n * (ROW + GAP) + SCAN + GAP + (i - n) * (ROW + GAP);
    }

    private float contentHeight() {
        return entries.size() * (ROW + GAP) + SCAN + lanServers.size() * (ROW + GAP);
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        if (lanThread == null) {
            try {
                lanThread = new LanServerDetector.ThreadLanServerFind(lan);
                lanThread.start();
            } catch (Exception e) {
                com.vaelohq.valor.Valor.LOGGER.warn("Unable to start LAN server detection: {}", e.getMessage());
            }
        }
        lw = Math.min(330, width - 40);
        lx = (width - lw) / 2;
        ly = 44;
        lh = height - ly - 58;
        list.place(lx, ly, lw, lh);
        long t = opened + 120;
        float w3 = (lw - 12) / 3, w4 = (lw - 18) / 4, y1 = height - 50, y2 = height - 28;
        join = new Widgets.Pill(JOIN, lx, y1, w3, 16, 8, Ui.I_PLAY, "Join server", true, t);
        buttonList.add(join);
        buttonList.add(new Widgets.Pill(DIRECT, lx + w3 + 6, y1, w3, 16, 8, -1, "Direct connect", false, t));
        buttonList.add(new Widgets.Pill(ADD, lx + (w3 + 6) * 2, y1, w3, 16, 8, -1, "Add server", false, t));
        edit = new Widgets.Pill(EDIT, lx, y2, w4, 16, 8, -1, "Edit", false, t + 40);
        delete = new Widgets.Pill(DELETE, lx + w4 + 6, y2, w4, 16, 8, -1, "Delete", false, t + 40).danger();
        buttonList.add(edit);
        buttonList.add(delete);
        buttonList.add(new Widgets.Pill(REFRESH, lx + (w4 + 6) * 2, y2, w4, 16, 8, -1, "Refresh", false, t + 40));
        buttonList.add(new Widgets.Pill(BACK, lx + (w4 + 6) * 3, y2, w4, 16, 8, -1, "Back", false, t + 40));
        updateButtons();
    }

    private void updateButtons() {
        if (join == null) return;
        join.enabled = selected >= 0 && selected < total();
        edit.enabled = delete.enabled = selected >= 0 && selected < entries.size();
    }

    @Override
    public void updateScreen() {
        if (lan.getWasUpdated()) {
            lanServers = new ArrayList<>(lan.getLanServers());
            lan.setWasNotUpdated();
            if (selected >= total()) selected = -1;
            updateButtons();
        }
        for (Entry e : entries) {
            e.ping();
            e.updateIcon();
        }
        pinger.pingPendingNetworks();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        if (lanThread != null) {
            lanThread.interrupt();
            lanThread = null;
        }
        pinger.clearPendingNetworks();
    }

    @Override
    protected void drawContent(int mx, int my) {
        float a = Ui.ease(time() / 0.3f), cx = width / 2f;
        Ui.bold().drawCentered("Multiplayer", cx, 12 + (1 - a) * 4, 13, Ui.alpha(Ui.TEXT, a));
        int n = entries.size(), k = lanServers.size();
        String sub = (n == 1 ? "1 saved server" : n + " saved servers") + (k > 0 ? "  ·  " + k + " on your network" : "");
        Ui.regular().drawCentered(sub, cx, 29 + (1 - a) * 4, 5.5f, Ui.alpha(Ui.MUTED, a));

        tooltip = null;
        boolean inList = list.contains(mx, my);
        list.begin(contentHeight());
        for (int i = 0; i < total(); i++) {
            float y = ly + rowY(i) - list.scroll;
            if (y + ROW < ly || y > ly + lh) continue;
            boolean hov = inList && my >= y && my < y + ROW;
            float ra = a * Ui.ease((time() - 0.04f * Math.min(i, 8)) / 0.35f);
            if (i < n) drawServer(entries.get(i), i, y, hov, mx, my, ra);
            else drawLan(lanServers.get(i - n), i, y, hov, ra);
        }
        drawScan(ly + n * (ROW + GAP) - list.scroll, a);
        list.end(mx, my, a);
        if (n == 0 && k == 0) {
            float ey = ly + SCAN + GAP + 26;
            Ui.semibold().drawCentered("No servers yet", cx, ey, 7, Ui.alpha(Ui.TEXT, a));
            Ui.regular().drawCentered("Add one, or join by address with Direct connect.", cx, ey + 12, 5.5f, Ui.alpha(Ui.MUTED, a));
        }
        Ui.tooltip(tooltip, mx, my, width);
    }

    /** Eased 0..1 hover amount for a row, kept per server so it survives list rebuilds. */
    private float hover(Object key, boolean on) {
        float v = Ui.approach(hovers.getOrDefault(key, 0f), on ? 1 : 0);
        hovers.put(key, v);
        return v;
    }

    private void card(float y, int i, float hv, float a) {
        boolean sel = i == selected;
        Shapes.draw(lx, y, lw, ROW, 8, Ui.alpha(sel ? 0xE0182A5C : Ui.mix(0xB4141A30, 0xD01B2340, hv), a),
                Ui.alpha(sel ? 0xE0101C44 : Ui.mix(0xB40A0F20, 0xD0111933, hv), a),
                Ui.alpha(sel ? 0xCC4C85FF : Ui.mix(0x1FFFFFFF, 0x38FFFFFF, hv), a), sel ? 0.8f : 0.5f, Ui.alpha(0x66000000, a), 5, 2);
    }

    private void drawServer(Entry e, int i, float y, boolean hov, int mx, int my, float a) {
        ServerData d = e.data;
        card(y, i, hover(d, hov), a);
        float ix = lx + 4, iy = y + 4;
        if (e.icon != null) {
            mc.getTextureManager().bindTexture(e.iconLoc);
            Shapes.image(ix, iy, ICON, ICON, 6, a);
        } else Ui.placeholder("server", ix, iy, ICON, 6, a);
        if (hov && inside(mx, my, ix, iy, ICON, ICON)) {
            Shapes.round(ix, iy, ICON, ICON, 6, Ui.alpha(0xA0000000, a));
            Ui.icon(Ui.I_PLAY, ix + ICON * 0.3f, iy + ICON * 0.3f, ICON * 0.4f, Ui.alpha(0xFFFFFFFF, a));
        }

        // Status, as vanilla works it out: version mismatch, pinged, or still pinging.
        boolean mismatch = d.version != 47, pinged = d.field_78841_f && d.pingToServer != -2;
        int level = 5;
        String status, players = null;
        if (mismatch) {
            status = d.version > 47 ? "Client out of date!" : "Server out of date!";
            players = d.playerList;
        } else if (pinged && d.pingToServer < 0) {
            status = "(no connection)";
        } else if (pinged) {
            long p = d.pingToServer;
            level = p < 150 ? 0 : p < 300 ? 1 : p < 600 ? 2 : p < 1000 ? 3 : 4;
            status = p + " ms";
            players = d.playerList;
        } else {
            level = -1;
            status = "Pinging...";
        }
        float bx = lx + lw - 8 - 11.2f;
        bars(bx, y + 5, level, i, a);
        if (hov && inside(mx, my, bx - 2, y + 3, 15, 10)) tooltip = status;

        ValorFont small = Ui.regular();
        String count = mismatch ? d.gameVersion : d.populationInfo == null ? "" : d.populationInfo;
        float cw = small.widthFormatted(count, 5.5f), cxp = bx - 5 - cw;
        if (mismatch) small.draw(count, cxp, y + 5.3f, 5.5f, Ui.alpha(Ui.DANGER, a));
        else small.drawFormatted(count, cxp, y + 5.3f, 5.5f, Ui.MUTED, a);
        if (hov && cw > 0 && inside(mx, my, cxp - 1, y + 3, cw + 2, 10)) tooltip = players;

        float tx = lx + ICON + 10;
        ValorFont semi = Ui.semibold();
        semi.draw(semi.fit(d.serverName, 6.5f, cxp - 6 - tx), tx, y + 4.5f, 6.5f, Ui.alpha(Ui.TEXT, a));
        String motd = d.serverMOTD == null ? "" : FMLClientHandler.instance().fixDescription(d.serverMOTD);
        List<String> lines = small.wrapFormatted(motd, 5.2f, lw - ICON - 10 - 30);
        boolean failed = pinged && d.pingToServer < 0;   // error text: vanilla's dark red is unreadable on navy
        for (int l = 0; l < lines.size(); l++) {
            if (failed) small.draw(lines.get(l).replaceAll("\u00a7.", ""), tx, y + 14 + l * 7.5f, 5.2f, Ui.alpha(Ui.DANGER, a));
            else small.drawFormatted(lines.get(l), tx, y + 14 + l * 7.5f, 5.2f, Ui.MUTED, a);
        }

        if (hov) {
            float ay = y + ROW - 13;
            Widgets.drawArrow(lx + lw - 24, ay, -90, i > 0, inside(mx, my, lx + lw - 24, ay, 10, 10), a);
            Widgets.drawArrow(lx + lw - 13, ay, 90, i < entries.size() - 1, inside(mx, my, lx + lw - 13, ay, 10, 10), a);
        }
    }

    /** Five signal bars: level 0 (best) to 4 lit by quality, 5 = no connection, -1 = pinging (a lit bar sweeps back and forth). */
    private void bars(float x, float y, int level, int row, float a) {
        int sweep = (int) (Minecraft.getSystemTime() / 100 + row * 2 & 7);
        if (sweep > 4) sweep = 8 - sweep;
        int lit = level <= 1 ? Ui.SUCCESS : level <= 3 ? 0xFFFFC84A : Ui.DANGER;
        for (int b = 0; b < 5; b++) {
            float h = 2 + b;
            boolean on = level < 0 ? b == sweep : b < 5 - level;
            int c = level < 0 ? (on ? Ui.MUTED : 0x33FFFFFF) : on ? lit : level == 5 ? 0x66FF5A6E : 0x33FFFFFF;
            Shapes.round(x + b * 2.4f, y + 6 - h, 1.6f, h, 0.8f, Ui.alpha(c, a));
        }
    }

    private void drawLan(LanServerDetector.LanServer s, int i, float y, boolean hov, float a) {
        card(y, i, hover(s, hov), a);
        float ix = lx + 4, iy = y + 4;
        Ui.placeholder("server", ix, iy, ICON, 6, a);
        float tx = lx + ICON + 10;
        ValorFont semi = Ui.semibold(), small = Ui.regular();
        semi.draw(semi.fit(s.getServerMotd(), 6.5f, lw - ICON - 18), tx, y + 4.5f, 6.5f, Ui.alpha(Ui.TEXT, a));
        small.draw("LAN world", tx, y + 14, 5.2f, Ui.alpha(Ui.MUTED, a));
        String ip = mc.gameSettings.hideServerAddress ? "Address hidden" : s.getServerIpPort();
        small.draw(ip, tx, y + 21.5f, 5.2f, Ui.alpha(0xFF6B7591, a));
    }

    private void drawScan(float y, float a) {
        String text = "Scanning for games on your local network";
        ValorFont f = Ui.regular();
        float w = f.width(text, 5.2f) + 11, x = (width - w) / 2;
        Ui.spinner(x + 3.5f, y + SCAN / 2, 3.2f, a);
        f.draw(text, x + 11, y + SCAN / 2 - 3.2f, 5.2f, Ui.alpha(0xFF6B7591, a));
    }

    private static boolean inside(int mx, int my, float x, float y, float w, float h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    private int rowAt(int mx, int my) {
        if (!list.contains(mx, my)) return -1;
        for (int i = 0; i < total(); i++) {
            float y = ly + rowY(i) - list.scroll;
            if (my >= y && my < y + ROW) return i;
        }
        return -1;
    }

    private void select(int i) {
        selected = i;
        updateButtons();
        if (i >= 0) list.reveal(rowY(i), ROW);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        list.wheel(Mouse.getEventDWheel(), ROW + GAP);
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        super.mouseClicked(mx, my, button);
        if (button != 0) return;
        if (list.press(mx, my)) return;
        int i = rowAt(mx, my), n = entries.size();
        if (i < 0) return;
        float y = ly + rowY(i) - list.scroll, ay = y + ROW - 13;
        if (i < n) {
            if (inside(mx, my, lx + lw - 24, ay, 10, 10)) {
                if (i > 0) move(i, -1);
                return;
            }
            if (inside(mx, my, lx + lw - 13, ay, 10, 10)) {
                if (i < n - 1) move(i, 1);
                return;
            }
            if (inside(mx, my, lx + 4, y + 4, ICON, ICON)) {
                select(i);
                join(i);
                return;
            }
        }
        long now = Minecraft.getSystemTime();
        boolean twice = i == selected && now - lastClick < 250;
        select(i);
        lastClick = now;
        if (twice) join(i);
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
        int n = entries.size();
        if (key == Keyboard.KEY_ESCAPE) mc.displayGuiScreen(parent);
        else if (key == Keyboard.KEY_F5) refresh();
        else if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
            if (join.enabled) join(selected);
        } else if (key == Keyboard.KEY_UP || key == Keyboard.KEY_DOWN) {
            int dir = key == Keyboard.KEY_UP ? -1 : 1;
            if (isShiftKeyDown()) {
                if (selected >= 0 && selected < n && selected + dir >= 0 && selected + dir < n) move(selected, dir);
            } else if (total() > 0) {
                select(selected < 0 ? (dir > 0 ? 0 : total() - 1) : Math.max(0, Math.min(total() - 1, selected + dir)));
            }
        }
    }

    /** Swaps a saved server with its neighbour (ServerList saves itself). */
    private void move(int i, int dir) {
        servers.swapServers(i, i + dir);
        rebuild();
        select(i + dir);
    }

    private void refresh() {
        mc.displayGuiScreen(new MultiplayerScreen(parent));
    }

    private void join(int i) {
        int n = entries.size();
        if (i < 0 || i >= total()) return;
        if (i < n) connect(entries.get(i).data);
        else {
            LanServerDetector.LanServer s = lanServers.get(i - n);
            connect(new ServerData(s.getServerMotd(), s.getServerIpPort(), true));
        }
    }

    private void connect(ServerData d) {
        FMLClientHandler.instance().connectToServer(this, d);
    }

    @Override
    protected void actionPerformed(GuiButton b) {
        switch (b.id) {
            case JOIN: join(selected); break;
            case DIRECT: {
                ServerData d = new ServerData("Minecraft Server", "", false);
                mc.displayGuiScreen(new ServerFormScreen(this, ServerFormScreen.Mode.DIRECT, d, ok -> {
                    if (ok) connect(d);
                    else mc.displayGuiScreen(this);
                }));
                break;
            }
            case ADD: {
                ServerData d = new ServerData("Minecraft Server", "", false);
                mc.displayGuiScreen(new ServerFormScreen(this, ServerFormScreen.Mode.ADD, d, ok -> {
                    if (ok) {
                        servers.addServerData(d);
                        servers.saveServerList();
                        rebuild();
                        select(entries.size() - 1);
                    }
                    mc.displayGuiScreen(this);
                }));
                break;
            }
            case EDIT: {
                ServerData src = entries.get(selected).data, d = new ServerData(src.serverName, src.serverIP, false);
                d.copyFrom(src);
                mc.displayGuiScreen(new ServerFormScreen(this, ServerFormScreen.Mode.EDIT, d, ok -> {
                    if (ok) {
                        if (!d.serverIP.equals(src.serverIP)) src.field_78841_f = false;   // new address: ping it again
                        src.copyFrom(d);
                        servers.saveServerList();
                        rebuild();
                    }
                    mc.displayGuiScreen(this);
                }));
                break;
            }
            case DELETE: {
                int i = selected;
                String name = entries.get(i).data.serverName;
                mc.displayGuiScreen(new ConfirmScreen(this, "Remove this server?",
                        "'" + name + "' will be lost forever! (A long time!)", "Delete", "Cancel", true, yes -> {
                    if (yes) {
                        servers.removeServerData(i);
                        servers.saveServerList();
                        rebuild();
                        select(-1);
                    }
                    mc.displayGuiScreen(this);
                }));
                break;
            }
            case REFRESH: refresh(); break;
            case BACK: mc.displayGuiScreen(parent); break;
        }
    }
}
