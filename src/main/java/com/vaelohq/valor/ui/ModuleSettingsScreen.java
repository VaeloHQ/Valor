package com.vaelohq.valor.ui;

import com.vaelohq.valor.module.HudModule;
import com.vaelohq.valor.module.Module;
import com.vaelohq.valor.module.Modules;
import com.vaelohq.valor.module.Setting;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A module's settings as a dialog over the mods menu or HUD editor: switches, sliders (drag, or scroll over one to step
 * it), choices and colour swatches, plus Reset to put everything back to the defaults.
 */
public class ModuleSettingsScreen extends ValorScreen {
    private static final int DONE = 0, RESET = 1;
    private static final float ROW = 18, GAP = 4, SWATCH = 9, TRACK = 80;

    private final GuiScreen parent;
    private final Module module;
    private final List<Setting> rows = new ArrayList<>();
    private final ScrollList list = new ScrollList();
    private final float[] hover, knobs;
    private final Setting.Slider scale = new Setting.Slider("scale", "Size", 1, 0.5f, 2.5f, 0.1f, "x");
    private float px, py, pw, ph;
    private Setting dragging;

    public ModuleSettingsScreen(GuiScreen parent, Module module) {
        this.parent = parent;
        this.module = module;
        if (module instanceof HudModule) {
            scale.value = ((HudModule) module).scale;
            rows.add(scale);
        }
        rows.addAll(module.settings);
        hover = new float[rows.size()];
        knobs = new float[rows.size()];
        for (int i = 0; i < rows.size(); i++) knobs[i] = -1;
    }

    @Override
    public void initGui() {
        super.initGui();
        parent.setWorldAndResolution(mc, width, height);
        pw = Math.min(250, width - 20);
        float content = rows.size() * (ROW + GAP) - GAP;
        ph = Math.min(height - 20, 46 + content + 32);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        list.place(px + 14, py + 38, pw - 28, ph - 38 - 30);
        buttonList.add(new Widgets.Pill(RESET, px + pw / 2 - 84, py + ph - 24, 80, 16, 8, -1, "Reset", false, opened + 60));
        buttonList.add(new Widgets.Pill(DONE, px + pw / 2 + 4, py + ph - 24, 80, 16, 8, -1, "Done", true, opened + 60));
    }

    @Override
    protected void drawBackground() {
        parent.drawScreen(-1, -1, 0);
    }

    @Override
    protected void drawContent(int mx, int my) {
        float a = Ui.ease(time() / 0.3f);
        Ui.dialog(width, height, px, py, pw, ph, a);
        Ui.semibold().drawCentered(module.name, px + pw / 2, py + 10, 8, Ui.alpha(Ui.TEXT, a));
        Ui.regular().drawCentered(Ui.regular().fit(module.description, 5, pw - 28), px + pw / 2, py + 22, 5, Ui.alpha(Ui.MUTED, a));
        boolean in = list.contains(mx, my);
        list.begin(rows.size() * (ROW + GAP) - GAP);
        for (int i = 0; i < rows.size(); i++) {
            float y = list.y + i * (ROW + GAP) - list.scroll;
            hover[i] = Ui.approach(hover[i], rows.get(i) == dragging || in && my >= y && my < y + ROW ? 1 : 0);
            if (y + ROW >= list.y && y <= list.y + list.h) row(i, list.x, y, list.w, mx, a);
        }
        list.end(mx, my, a);
    }

    private void row(int i, float x, float y, float w, int mx, float a) {
        Setting s = rows.get(i);
        float pad = Widgets.drawRow(x, y, w, ROW, s.name, hover[i], a), right = x + w - pad;
        if (s instanceof Setting.Toggle) {
            boolean on = ((Setting.Toggle) s).value;
            knobs[i] = knobs[i] < 0 ? (on ? 1 : 0) : Ui.approach(knobs[i], on ? 1 : 0);
            Widgets.drawSwitch(right - 18, y + (ROW - 9) / 2, knobs[i], a);
        } else if (s instanceof Setting.Slider) {
            Setting.Slider sl = (Setting.Slider) s;
            Widgets.drawSlider(right - TRACK, y + ROW / 2, TRACK, sl.fraction(), hover[i], a);
            Widgets.drawValue(sl.text(), right - TRACK - 7, y, ROW, a);
        } else if (s instanceof Setting.Choice) {
            Widgets.drawValue(((Setting.Choice) s).value(), right, y, ROW, a);
        } else if (s instanceof Setting.Color) {
            int[] p = Setting.Color.PALETTE;
            for (int c = 0; c < p.length; c++) {
                float sx = right - (p.length - c) * (SWATCH + 2) + 2, sy = y + (ROW - SWATCH) / 2;
                boolean sel = p[c] == ((Setting.Color) s).value;
                Shapes.draw(sx, sy, SWATCH, SWATCH, SWATCH / 2, Ui.alpha(p[c], a), Ui.alpha(p[c], a), Ui.alpha(sel ? 0xFFFFFFFF : 0x40000000, a),
                        sel ? 1 : 0.5f, 0, 0, 0);
            }
        }
    }

    private int rowAt(int my) {
        int i = (int) Math.floor((my - list.y + list.scroll) / (ROW + GAP));
        return i >= 0 && i < rows.size() && my < list.y + i * (ROW + GAP) - list.scroll + ROW ? i : -1;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;
        int mx = Mouse.getEventX() * width / mc.displayWidth, my = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        int i = list.contains(mx, my) ? rowAt(my) : -1;
        // Over a slider's track or value: step it. Anywhere else: scroll the list.
        if (i >= 0 && rows.get(i) instanceof Setting.Slider && mx >= list.x + list.w - ROW / 2 * 0.9f - TRACK - 30) {
            ((Setting.Slider) rows.get(i)).step(wheel);
            changed();
        } else list.wheel(wheel, ROW + GAP);
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        super.mouseClicked(mx, my, button);
        if (button != 0 || list.press(mx, my) || !list.contains(mx, my)) return;
        int i = rowAt(my);
        if (i < 0) return;
        Setting s = rows.get(i);
        float right = list.x + list.w - ROW / 2 * 0.9f;
        if (s instanceof Setting.Toggle) ((Setting.Toggle) s).value = !((Setting.Toggle) s).value;
        else if (s instanceof Setting.Choice) ((Setting.Choice) s).next();
        else if (s instanceof Setting.Slider) {
            dragging = s;
            drag(mx);
            return;
        } else if (s instanceof Setting.Color) {
            int[] p = Setting.Color.PALETTE;
            int c = p.length - 1 - (int) Math.floor((right - mx) / (SWATCH + 2));
            if (c < 0 || c >= p.length) return;
            ((Setting.Color) s).value = p[c];
        }
        changed();
        mc.getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
    }

    private void drag(int mx) {
        float right = list.x + list.w - ROW / 2 * 0.9f;
        ((Setting.Slider) dragging).setFraction(Math.max(0, Math.min(1, (mx - (right - TRACK)) / TRACK)));
        changed();
    }

    private void changed() {
        if (module instanceof HudModule) ((HudModule) module).scale = scale.value;
    }

    @Override
    protected void mouseClickMove(int mx, int my, int button, long held) {
        list.drag(my);
        if (dragging != null) drag(mx);
    }

    @Override
    protected void mouseReleased(int mx, int my, int state) {
        super.mouseReleased(mx, my, state);
        list.release();
        dragging = null;
    }

    @Override
    protected void actionPerformed(GuiButton b) {
        if (b.id != RESET) {
            close();
            return;
        }
        module.reset();
        if (module instanceof HudModule) scale.value = ((HudModule) module).scale;
        for (int i = 0; i < rows.size(); i++) knobs[i] = -1;   // switches jump to their reset state
        Modules.save();
    }

    @Override
    protected void keyTyped(char c, int key) {
        if (key == Keyboard.KEY_ESCAPE) close();
    }

    private void close() {
        Modules.save();
        mc.displayGuiScreen(parent);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return parent.doesGuiPauseGame();
    }
}
