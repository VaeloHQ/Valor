package com.vaelohq.valor.ui;

import com.vaelohq.valor.module.Module;
import com.vaelohq.valor.module.Modules;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.GuiModList;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The mods menu (Right Shift): profiles on the left, every module as a card with a switch on the right, filtered by
 * category or search. Click a card to switch it, right-click (or its gear) for settings; type and press Enter to switch
 * the first match.
 */
public class ModsScreen extends ValorScreen {
    private static final int HUD = 100, FORGE = 101, DONE = 102, NEW_PROFILE = 103;
    private static final float CARD_H = 44, GAP = 7, SIDE_W = 96, PROFILE_H = 16;
    private static int category = -1;   // -1 = all

    private final GuiScreen parent;
    private final ScrollList list = new ScrollList();
    private final ValorTextField search = new ValorTextField("Search mods", false);
    private final Map<Module, float[]> anim = new HashMap<>();   // {hover, knob}
    private List<Module> shown = new ArrayList<>();
    private int columns;
    private float cardW, sideX, sideY, sideH;

    public ModsScreen(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        float total = Math.min(540, width - 30), lw = total - SIDE_W - 10, lx = (width - total) / 2 + SIDE_W + 10, top = 58;
        sideX = (width - total) / 2;
        sideY = 36;
        sideH = height - 34 - sideY;
        columns = lw >= 330 ? 3 : 2;
        cardW = (lw - (columns - 1) * GAP) / columns;
        list.place(lx, top, lw, height - top - 34);
        search.place(lx + lw - 110, 36, 110, 15);
        long t = opened + 60;
        float px = lx;
        Module.Category[] cats = Module.Category.values();
        for (int i = -1; i < cats.length; i++) {
            String label = i < 0 ? "All" : cats[i].label;
            float w = Ui.semibold().width(label, 5.5f) + 18;
            buttonList.add(new Widgets.Pill(i + 1, px, 36, w, 15, 7.5f, -1, label, category == i, t));
            px += w + 5;
        }
        buttonList.add(new Widgets.Pill(NEW_PROFILE, sideX + 6, sideY + sideH - 22, SIDE_W - 12, 16, 8, Ui.I_PLUS, "New profile", false, t));
        float bw = 110;
        buttonList.add(new Widgets.Pill(HUD, width / 2f - bw * 1.5f - 6, height - 25, bw, 16, 8, Ui.I_GRID, "Edit HUD layout", false, t));
        buttonList.add(new Widgets.Pill(FORGE, width / 2f - bw / 2, height - 25, bw, 16, 8, -1, "Forge mod list", false, t));
        buttonList.add(new Widgets.Pill(DONE, width / 2f + bw / 2 + 6, height - 25, bw, 16, 8, -1, "Done", true, t));
        filter();
    }

    private void filter() {
        shown = new ArrayList<>();
        String q = search.text.trim().toLowerCase(Locale.ROOT);
        for (Module m : Modules.all()) {
            if (category >= 0 && m.category.ordinal() != category) continue;
            if (!q.isEmpty() && !m.name.toLowerCase(Locale.ROOT).contains(q) && !m.description.toLowerCase(Locale.ROOT).contains(q)) continue;
            shown.add(m);
        }
    }

    private float cardX(int i) {
        return list.x + (i % columns) * (cardW + GAP);
    }

    private float cardY(int i) {
        return list.y + (i / columns) * (CARD_H + GAP) - list.scroll;
    }

    @Override
    protected void drawContent(int mx, int my) {
        float a = Ui.ease(time() / 0.3f);
        int on = 0;
        for (Module m : Modules.all()) if (m.enabled()) on++;
        Ui.bold().drawCentered("Mods", width / 2f, 8 + (1 - a) * 4, 12, Ui.alpha(Ui.TEXT, a));
        Ui.regular().drawCentered(on + " of " + Modules.all().size() + " on", width / 2f, 23 + (1 - a) * 4, 5, Ui.alpha(Ui.MUTED, a));
        search.draw(a);
        boolean in = list.contains(mx, my);
        int rows = (shown.size() + columns - 1) / columns;
        list.begin(rows * (CARD_H + GAP) - GAP);
        for (int i = 0; i < shown.size(); i++) {
            float x = cardX(i), y = cardY(i);
            if (y + CARD_H < list.y || y > list.y + list.h) continue;
            card(shown.get(i), x, y, in && mx >= x && mx < x + cardW && my >= y && my < y + CARD_H, mx, my,
                    a * Ui.ease((time() - 0.02f * Math.min(i, 12)) / 0.3f));
        }
        list.end(mx, my, a);
        profiles(mx, my, a);
        if (shown.isEmpty()) Ui.regular().drawCentered("No mods match \"" + search.text.trim() + "\"", width / 2f, list.y + 30, 6, Ui.alpha(Ui.MUTED, a));
    }

    private void card(Module m, float x, float y, boolean hov, int mx, int my, float a) {
        float[] s = anim.get(m);
        if (s == null) anim.put(m, s = new float[]{0, m.enabled() ? 1 : 0});
        s[0] = Ui.approach(s[0], hov ? 1 : 0);
        s[1] = Ui.approach(s[1], m.enabled() ? 1 : 0);
        float hv = s[0], k = s[1];
        Shapes.draw(x, y, cardW, CARD_H, 9, Ui.alpha(Ui.mix(0xB4141A30, 0xD01B2340, hv), a), Ui.alpha(Ui.mix(0xB40A0F20, 0xD0111933, hv), a),
                Ui.alpha(Ui.mix(Ui.mix(0x1FFFFFFF, 0x38FFFFFF, hv), 0xAA4C85FF, k), a), 0.5f + 0.3f * k, Ui.alpha(0x55000000, a), 5, 2);
        ValorFont semi = Ui.semibold(), reg = Ui.regular();
        // The icon lights up in Valor blue while the module is on.
        float tx = Ui.modIcon(m.id, x + 8, y + 7, 13, Ui.alpha(Ui.mix(Ui.MUTED, Ui.PRIMARY_HI, k), a)) ? x + 27 : x + 8;
        semi.draw(semi.fit(m.name, 6.5f, x + cardW - 8 - tx), tx, y + 6, 6.5f, Ui.alpha(Ui.TEXT, a));
        float dw = x + cardW - 8 - tx;
        List<String> desc = reg.wrap(m.description, 5, dw);
        if (desc.size() > 2) desc.set(1, reg.fit(String.join(" ", desc.subList(1, desc.size())), 5, dw));   // two lines, then ...
        for (int l = 0; l < Math.min(2, desc.size()); l++) reg.draw(desc.get(l), tx, y + 15 + l * 6.5f, 5, Ui.alpha(Ui.MUTED, a));
        Widgets.drawSwitch(x + cardW - 26, y + CARD_H - 15, k, a);
        reg.draw(m.category.label, x + 8, y + CARD_H - 13, 4.5f, Ui.alpha(0xFF6B7591, a));
        if (!m.settings.isEmpty()) {
            boolean hot = onGear(mx, my, x, y);
            if (hot) Shapes.round(x + cardW - 44, y + CARD_H - 17, 13, 13, 6.5f, Ui.alpha(0x26FFFFFF, a));
            Ui.icon(Ui.I_SETTINGS, x + cardW - 41, y + CARD_H - 14, 7, Ui.alpha(hot ? Ui.TEXT : Ui.MUTED, a));
        }
    }

    /** The profile list on the left: the active one highlighted, a gear on each to edit it. */
    private void profiles(int mx, int my, float a) {
        Shapes.draw(sideX, sideY, SIDE_W, sideH, 9, Ui.alpha(0xB4141A30, a), Ui.alpha(0xB40A0F20, a), Ui.alpha(0x1FFFFFFF, a), 0.5f,
                Ui.alpha(0x55000000, a), 5, 2);
        Ui.semibold().draw("Profiles", sideX + 8, sideY + 7, 5.5f, Ui.alpha(Ui.MUTED, a));
        List<Modules.Profile> list = Modules.profiles();
        for (int i = 0; i < list.size() && profileY(i) + PROFILE_H <= sideY + sideH - 26; i++) {
            Modules.Profile p = list.get(i);
            float x = sideX + 5, y = profileY(i), w = SIDE_W - 10;
            boolean on = p == Modules.active(), hov = mx >= x && mx < x + w && my >= y && my < y + PROFILE_H, gear = hov && mx >= x + w - 15;
            if (on || hov) Shapes.round(x, y, w, PROFILE_H, 6, Ui.alpha(on ? 0x502563FF : 0x14FFFFFF, a));
            Ui.semibold().draw(Ui.semibold().fit(p.name, 5.5f, w - 24), x + 6, y + 5, 5.5f, Ui.alpha(on ? Ui.TEXT : Ui.MUTED, a));
            if (!p.servers.isEmpty()) Shapes.round(x + w - 21, y + 7, 2.5f, 2.5f, 1.25f, Ui.alpha(Ui.CYAN, a));   // auto-switches
            Ui.icon(Ui.I_SETTINGS, x + w - 13, y + 4.5f, 7, Ui.alpha(gear ? Ui.TEXT : hov ? Ui.MUTED : 0x00000000, a));
        }
    }

    private float profileY(int i) {
        return sideY + 18 + i * (PROFILE_H + 2);
    }

    /** Clicks on the profile list: a row switches to it, its gear edits it. Returns true if the click was on the list. */
    private boolean clickProfiles(int mx, int my) {
        List<Modules.Profile> list = Modules.profiles();
        float x = sideX + 5, w = SIDE_W - 10;
        for (int i = 0; i < list.size() && profileY(i) + PROFILE_H <= sideY + sideH - 26; i++) {
            float y = profileY(i);
            if (mx < x || mx >= x + w || my < y || my >= y + PROFILE_H) continue;
            Modules.Profile p = list.get(i);
            if (mx >= x + w - 15) mc.displayGuiScreen(new ProfileScreen(this, p));
            else if (p != Modules.active()) {
                Modules.switchTo(p);
                anim.clear();   // cards jump to the new profile's switches
                mc.getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
            }
            return true;
        }
        return false;
    }

    private boolean onGear(int mx, int my, float x, float y) {
        return mx >= x + cardW - 44 && mx < x + cardW - 31 && my >= y + CARD_H - 17 && my < y + CARD_H - 4;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        list.wheel(Mouse.getEventDWheel(), CARD_H + GAP);
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        search.focus(search.inside(mx, my));
        super.mouseClicked(mx, my, button);
        if (button == 0 && clickProfiles(mx, my)) return;
        if (button > 1 || button == 0 && list.press(mx, my) || !list.contains(mx, my)) return;
        for (int i = 0; i < shown.size(); i++) {
            float x = cardX(i), y = cardY(i);
            if (mx < x || mx >= x + cardW || my < y || my >= y + CARD_H) continue;
            Module m = shown.get(i);
            // Right-click anywhere on a card, or click its gear: settings. Click anywhere else: on / off.
            if (!m.settings.isEmpty() && (button == 1 || onGear(mx, my, x, y))) mc.displayGuiScreen(new ModuleSettingsScreen(this, m));
            else if (button == 0) toggle(m);
            return;
        }
    }

    private void toggle(Module m) {
        m.setEnabled(!m.enabled());
        Modules.save();
        mc.getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
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
        if (key == Keyboard.KEY_ESCAPE || (!search.focused && key == Modules.MENU_KEY.getKeyCode())) {
            close();
            return;
        }
        if (!search.focused && c >= ' ' && c != 127) search.focus(true);   // just start typing to search
        if (search.focused && (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER)) {
            if (!shown.isEmpty() && !search.text.trim().isEmpty()) toggle(shown.get(0));   // type a name, Enter: on / off
            return;
        }
        if (search.focused) {
            search.key(c, key);
            filter();
        }
    }

    private void close() {
        Modules.save();
        mc.displayGuiScreen(parent);
    }

    @Override
    protected void actionPerformed(GuiButton b) {
        switch (b.id) {
            case HUD: mc.displayGuiScreen(new HudEditorScreen(this)); return;
            case FORGE: mc.displayGuiScreen(new GuiModList(this)); return;
            case NEW_PROFILE: {
                int n = Modules.profiles().size() + 1;
                while (Modules.find("Profile " + n) != null) n++;
                mc.displayGuiScreen(new ProfileScreen(this, Modules.create("Profile " + n)));   // a copy of this setup, named next
                return;
            }
            case DONE: close(); return;
            default:
                category = b.id - 1;
                initGui();
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
