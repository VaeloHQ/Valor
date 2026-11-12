package com.vaelohq.valor.ui;

import com.vaelohq.valor.module.Modules;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;

/** Edits a profile over the mods menu: its name, the servers that switch to it, or deletes it. */
public class ProfileScreen extends ValorScreen {
    private static final int SAVE = 0, CANCEL = 1, DELETE = 2;
    private final GuiScreen behind;
    private final Modules.Profile profile;
    private final ValorTextField name = new ValorTextField("Profile name", false);
    private final ValorTextField servers = new ValorTextField("e.g. hypixel.net, minemen.club", false);
    private float px, py, pw, ph;
    private Widgets.Pill save;
    private String error;

    public ProfileScreen(GuiScreen behind, Modules.Profile profile) {
        this.behind = behind;
        this.profile = profile;
        name.text = profile.name;
        name.max = 24;
        servers.text = String.join(", ", profile.servers);
        servers.max = 200;
        name.focus(true);
        name.selectAll();   // type straight away to rename
    }

    @Override
    public void initGui() {
        super.initGui();
        behind.setWorldAndResolution(mc, width, height);
        Keyboard.enableRepeatEvents(true);
        pw = 220;
        ph = 140;
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        float fx = px + 16, fw = pw - 32, bw = (fw - 12) / 3;
        name.place(fx, py + 40, fw, 16);
        servers.place(fx, py + 74, fw, 16);
        long t = opened + 80;
        Widgets.Pill delete = new Widgets.Pill(DELETE, fx, py + ph - 26, bw, 16, 8, Ui.I_TRASH, "Delete", false, t).danger();
        delete.enabled = Modules.profiles().size() > 1;
        buttonList.add(delete);
        buttonList.add(new Widgets.Pill(CANCEL, fx + bw + 6, py + ph - 26, bw, 16, 8, -1, "Cancel", false, t));
        save = new Widgets.Pill(SAVE, fx + 2 * (bw + 6), py + ph - 26, bw, 16, 8, -1, "Save", true, t);
        buttonList.add(save);
        check();
    }

    /** Enables Save for a usable name; otherwise notes why it can't be used. */
    private void check() {
        String n = name.text.trim();
        Modules.Profile other = Modules.find(n);
        error = n.isEmpty() ? null : other != null && other != profile ? "There's already a profile called " + other.name : null;
        save.enabled = !n.isEmpty() && error == null;
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    protected void drawBackground() {
        behind.drawScreen(-1, -1, 0);
    }

    @Override
    protected void drawContent(int mouseX, int mouseY) {
        float a = Ui.ease(time() / 0.3f);
        Ui.dialog(width, height, px, py, pw, ph, a);
        Ui.semibold().drawCentered("Edit profile", px + pw / 2, py + 14, 8, Ui.alpha(Ui.TEXT, a));
        Ui.regular().draw("Name", name.x + 1, name.y - 8, 5, Ui.alpha(Ui.MUTED, a));
        Ui.regular().draw("Switch to this profile when joining", servers.x + 1, servers.y - 8, 5, Ui.alpha(Ui.MUTED, a));
        name.draw(a);
        servers.draw(a);
        String note = error != null ? error : "Separate servers with commas. hypixel.net also covers mc.hypixel.net.";
        Ui.regular().draw(Ui.regular().fit(note, 4.5f, pw - 32), servers.x + 1, servers.y + 20, 4.5f,
                Ui.alpha(error != null ? Ui.DANGER : 0xFF6B7591, a));
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws java.io.IOException {
        name.focus(name.inside(mx, my));
        servers.focus(servers.inside(mx, my));
        super.mouseClicked(mx, my, button);
    }

    @Override
    protected void keyTyped(char c, int key) {
        if (key == Keyboard.KEY_ESCAPE) mc.displayGuiScreen(behind);
        else if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) submit();
        else if (key == Keyboard.KEY_TAB) {
            boolean toServers = name.focused;
            name.focus(!toServers);
            servers.focus(toServers);
        } else if (name.focused) name.key(c, key);
        else if (servers.focused) servers.key(c, key);
        check();
    }

    @Override
    protected void actionPerformed(GuiButton b) {
        if (b.id == SAVE) submit();
        else if (b.id == DELETE) {
            Modules.delete(profile);
            mc.displayGuiScreen(behind);
        } else mc.displayGuiScreen(behind);
    }

    private void submit() {
        check();
        if (!save.enabled) return;
        profile.name = name.text.trim();
        List<String> hosts = new ArrayList<>();
        for (String h : servers.text.split(",")) if (!h.trim().isEmpty()) hosts.add(h.trim().toLowerCase(java.util.Locale.ROOT));
        profile.servers.clear();
        profile.servers.addAll(hosts);
        Modules.save();
        mc.displayGuiScreen(behind);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return behind.doesGuiPauseGame();
    }
}
