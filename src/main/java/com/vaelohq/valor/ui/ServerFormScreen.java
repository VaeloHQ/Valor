package com.vaelohq.valor.ui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.ServerData;
import org.lwjgl.input.Keyboard;

import java.util.function.Consumer;

/**
 * Add server, edit server and direct connect, as one Valor dialog over the server list. Same rules as vanilla's
 * GuiScreenAddServer and GuiScreenServerList. The ServerData is only written when the player confirms.
 */
public class ServerFormScreen extends ValorScreen {
    public enum Mode { ADD, EDIT, DIRECT }

    private static final int DONE = 0, CANCEL = 1, PACKS = 2;
    private final GuiScreen behind;
    private final Mode mode;
    private final ServerData data;
    private final Consumer<Boolean> answer;
    private final ValorTextField name = new ValorTextField("Minecraft Server", false), address = new ValorTextField("play.example.net", false);
    private ServerData.ServerResourceMode packs;
    private float px, py, pw, ph;
    private Widgets.Pill done, packsButton;

    /** @param answer true when confirmed (data has been updated), false when cancelled */
    public ServerFormScreen(GuiScreen behind, Mode mode, ServerData data, Consumer<Boolean> answer) {
        this.behind = behind;
        this.mode = mode;
        this.data = data;
        this.answer = answer;
        name.text = data.serverName;
        name.max = 32;
        address.max = 128;
        address.text = mode == Mode.DIRECT ? net.minecraft.client.Minecraft.getMinecraft().gameSettings.lastServer : data.serverIP;
        packs = data.getResourceMode();
        (mode == Mode.DIRECT ? address : name).focus(true);
    }

    private ValorTextField[] fields() {
        return mode == Mode.DIRECT ? new ValorTextField[]{address} : new ValorTextField[]{name, address};
    }

    @Override
    public void initGui() {
        super.initGui();
        behind.setWorldAndResolution(mc, width, height);
        Keyboard.enableRepeatEvents(true);
        pw = 200;
        ph = mode == Mode.DIRECT ? 98 : 152;
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        float fx = px + 16, fw = pw - 32, cy = py + 32;
        for (ValorTextField f : fields()) {
            f.place(fx, cy + 8, fw, 16);
            cy += 30;
        }
        long t = opened + 80;
        if (mode != Mode.DIRECT) {
            packsButton = new Widgets.Pill(PACKS, fx, cy, fw, 16, 8, -1, packsLabel(), false, t);
            buttonList.add(packsButton);
            cy += 24;
        }
        float bw = (fw - 6) / 2;
        done = new Widgets.Pill(DONE, fx, cy + 4, bw, 16, 8, -1, mode == Mode.DIRECT ? "Join server" : "Done", true, t);
        buttonList.add(done);
        buttonList.add(new Widgets.Pill(CANCEL, fx + bw + 6, cy + 4, bw, 16, 8, -1, "Cancel", false, t));
        done.enabled = valid();
    }

    private String packsLabel() {
        return "Server resource packs: " + packs.getMotd().getUnformattedText();
    }

    /** Vanilla's rule: an address with a host part, and a name unless this is direct connect. */
    private boolean valid() {
        String ip = address.text;
        return !ip.isEmpty() && ip.split(":").length > 0 && (mode == Mode.DIRECT || !name.text.isEmpty());
    }

    @Override
    public void updateScreen() {
        if (done != null) done.enabled = valid();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        if (mode == Mode.DIRECT) {
            mc.gameSettings.lastServer = address.text;
            mc.gameSettings.saveOptions();
        }
    }

    @Override
    protected void drawBackground() {
        behind.drawScreen(-1, -1, 0);
    }

    @Override
    protected void drawContent(int mouseX, int mouseY) {
        float a = Ui.ease(time() / 0.3f), y = (1 - a) * 5;
        Ui.dialog(width, height, px, py + y, pw, ph, a);
        String title = mode == Mode.ADD ? "Add server" : mode == Mode.EDIT ? "Edit server" : "Direct connect";
        Ui.semibold().drawCentered(title, px + pw / 2, py + y + 14, 8, Ui.alpha(Ui.TEXT, a));
        String[] labels = mode == Mode.DIRECT ? new String[]{"Server address"} : new String[]{"Server name", "Server address"};
        ValorTextField[] fs = fields();
        for (int i = 0; i < fs.length; i++) {
            Ui.regular().draw(labels[i], fs[i].x + 1, fs[i].y - 8, 5, Ui.alpha(Ui.MUTED, a));
            fs[i].draw(a);
        }
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws java.io.IOException {
        for (ValorTextField f : fields()) f.focus(f.inside(mx, my));
        super.mouseClicked(mx, my, button);
    }

    @Override
    protected void keyTyped(char c, int key) {
        ValorTextField[] fs = fields();
        int cur = -1;
        for (int i = 0; i < fs.length; i++) if (fs[i].focused) cur = i;
        if (key == Keyboard.KEY_ESCAPE) answer.accept(false);
        else if (key == Keyboard.KEY_TAB) {
            int next = (cur + (isShiftKeyDown() ? fs.length - 1 : 1)) % fs.length;
            for (int i = 0; i < fs.length; i++) fs[i].focus(i == next);
        } else if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) confirm();
        else if (cur >= 0) fs[cur].key(c, key);
        done.enabled = valid();
    }

    @Override
    protected void actionPerformed(GuiButton b) {
        switch (b.id) {
            case DONE: confirm(); break;
            case CANCEL: answer.accept(false); break;
            case PACKS:
                ServerData.ServerResourceMode[] all = ServerData.ServerResourceMode.values();
                packs = all[(packs.ordinal() + 1) % all.length];
                packsButton.displayString = packsLabel();
                break;
        }
    }

    private void confirm() {
        if (!valid()) return;
        data.serverIP = address.text;
        if (mode != Mode.DIRECT) {
            data.serverName = name.text;
            data.setResourceMode(packs);
        }
        answer.accept(true);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return behind.doesGuiPauseGame();
    }
}
