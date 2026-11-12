package com.vaelohq.valor.ui;

import com.vaelohq.valor.module.HudModule;
import com.vaelohq.valor.module.Module;
import com.vaelohq.valor.module.Modules;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * HUD layout editor. Drag a module to move it: its edges and centre snap to the other modules, the screen edges and the
 * screen's centre lines. Scroll over one to resize it, right-click it for its settings, arrow keys nudge the selected one
 * (Shift: 10 pixels), Ctrl+Z / Ctrl+Y undo and redo. Out of game the modules show sample data.
 */
public class HudEditorScreen extends ValorScreen {
    private static final int DONE = 0, HISTORY = 50;
    private static final float SNAP = 4;

    private final GuiScreen parent;
    private final Deque<Map<HudModule, float[]>> undo = new ArrayDeque<>(), redo = new ArrayDeque<>();
    private Map<HudModule, float[]> before;   // layout when a drag started or a settings dialog opened
    private HudModule held, selected;
    private float grabX, grabY;
    private float guideX = Float.NaN, guideY = Float.NaN;

    public HudEditorScreen(GuiScreen parent) {
        this.parent = parent;
    }

    private boolean preview() {
        return mc.theWorld == null;
    }

    /** The modules on screen right now (hidden ones, such as Potion Status with no effects, aren't snapped to). */
    private List<HudModule> modules() {
        List<HudModule> l = new ArrayList<>();
        for (Module m : Modules.all())
            if (m instanceof HudModule && m.enabled() && (preview() || ((HudModule) m).visible())) l.add((HudModule) m);
        return l;
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);   // holding an arrow key keeps nudging
        buttonList.add(new Widgets.Pill(DONE, width / 2f - 45, height / 2f + 22, 90, 16, 8, -1, "Done", true, opened + 60));
    }

    // ---- Undo ----

    private static Map<HudModule, float[]> snapshot() {
        Map<HudModule, float[]> s = new LinkedHashMap<>();
        for (Module m : Modules.all())
            if (m instanceof HudModule) s.put((HudModule) m, new float[]{((HudModule) m).x, ((HudModule) m).y, ((HudModule) m).scale});
        return s;
    }

    private static void restore(Map<HudModule, float[]> s) {
        for (Map.Entry<HudModule, float[]> e : s.entrySet()) {
            e.getKey().x = e.getValue()[0];
            e.getKey().y = e.getValue()[1];
            e.getKey().scale = e.getValue()[2];
        }
        Modules.save();
    }

    private static boolean same(Map<HudModule, float[]> a, Map<HudModule, float[]> b) {
        for (Map.Entry<HudModule, float[]> e : a.entrySet()) if (!Arrays.equals(e.getValue(), b.get(e.getKey()))) return false;
        return true;
    }

    /** Remembers the layout before a change, so it can be undone. */
    private void remember(Map<HudModule, float[]> s) {
        undo.push(s);
        if (undo.size() > HISTORY) undo.removeLast();
        redo.clear();
    }

    /** Records `before` if the layout has changed since. */
    private void commit() {
        if (before != null && !same(before, snapshot())) remember(before);
        before = null;
    }

    private void step(Deque<Map<HudModule, float[]>> from, Deque<Map<HudModule, float[]>> to) {
        if (from.isEmpty()) return;
        to.push(snapshot());
        restore(from.pop());
    }

    // ---- Drawing ----

    @Override
    protected void drawBackground() {
        if (preview()) Ui.menuBackground(width, height);
        else Ui.rect(0, 0, width, height, 0x40000000);   // keep the game visible behind the layout
    }

    @Override
    protected void drawContent(int mx, int my) {
        // Back from a settings dialog (it draws this screen behind it, so wait until this one is on top again): its
        // Reset or Size change can be undone.
        if (before != null && held == null && mc.currentScreen == this) commit();
        float a = Ui.ease(time() / 0.3f);
        if (!Float.isNaN(guideX)) Ui.rect(guideX - 0.25f, 0, guideX + 0.25f, height, Ui.alpha(Ui.CYAN, 0.8f));
        if (!Float.isNaN(guideY)) Ui.rect(0, guideY - 0.25f, width, guideY + 0.25f, Ui.alpha(Ui.CYAN, 0.8f));
        Ui.semibold().drawCentered("Edit HUD layout", width / 2f, height / 2f - 20, 8, Ui.alpha(Ui.TEXT, a * 0.9f));
        Ui.regular().drawCentered("Drag to move  ·  Scroll to resize  ·  Right-click for settings", width / 2f, height / 2f - 8, 5,
                Ui.alpha(Ui.MUTED, a));
        Ui.regular().drawCentered("Arrow keys nudge (Shift: 10 px)  ·  Ctrl+Z undo  ·  Ctrl+Y redo", width / 2f, height / 2f + 1, 5,
                Ui.alpha(Ui.MUTED, a));
        Modules.drawHud(width, height, preview());
        HudModule over = at(mx, my);
        boolean p = preview();
        for (HudModule h : modules()) {
            float x = h.screenX(width, p), y = h.screenY(height, p), w = h.width(p) * h.scale, hh = h.height(p) * h.scale;
            boolean hot = h == held || h == selected || h == over && held == null;
            Shapes.draw(x - 1.5f, y - 1.5f, w + 3, hh + 3, 4, 0, 0, Ui.alpha(hot ? Ui.CYAN : 0x55FFFFFF, a), hot ? 0.9f : 0.5f, 0, 0, 0);
            if (hot) {
                float ly = y + hh + 10 > height ? y - 9 : y + hh + 3;   // label above when there's no room below
                Ui.regular().draw(h.name + "  " + Math.round(h.scale * 100) + "%", x, ly, 4.5f, Ui.alpha(Ui.TEXT, a));
            }
        }
    }

    /** The topmost module under the mouse (drawn last = on top). */
    private HudModule at(int mx, int my) {
        List<HudModule> l = modules();
        boolean p = preview();
        for (int i = l.size() - 1; i >= 0; i--) {
            HudModule h = l.get(i);
            float x = h.screenX(width, p), y = h.screenY(height, p);
            if (mx >= x && my >= y && mx < x + h.width(p) * h.scale && my < y + h.height(p) * h.scale) return h;
        }
        return null;
    }

    // ---- Moving ----

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        super.mouseClicked(mx, my, button);
        HudModule h = at(mx, my);
        if (button == 0) selected = h;
        if (h == null) return;
        before = snapshot();
        if (button == 1) {
            mc.displayGuiScreen(new ModuleSettingsScreen(this, h));
            return;
        }
        if (button != 0) return;
        held = h;
        grabX = mx - h.screenX(width, preview());
        grabY = my - h.screenY(height, preview());
    }

    @Override
    protected void mouseClickMove(int mx, int my, int button, long time) {
        if (held == null) return;
        boolean p = preview();
        float w = held.width(p) * held.scale, h = held.height(p) * held.scale;
        float[] sx = snap(mx - grabX, w, true), sy = snap(my - grabY, h, false);
        guideX = sx[1];
        guideY = sy[1];
        held.x = fraction(sx[0], width, w);
        held.y = fraction(sy[0], height, h);
    }

    /**
     * Snaps one axis of the held module: its start, centre or end lines up with another module's start, centre or end,
     * the screen edge or the screen centre, whichever is closest within SNAP pixels. Returns {position, guide line or NaN}.
     */
    private float[] snap(float pos, float size, boolean horizontal) {
        boolean p = preview();
        float screen = horizontal ? width : height;
        List<Float> targets = new ArrayList<>(Arrays.asList(HudModule.MARGIN, screen / 2, screen - HudModule.MARGIN));
        for (HudModule o : modules()) {
            if (o == held) continue;
            float start = horizontal ? o.screenX(width, p) : o.screenY(height, p);
            float len = (horizontal ? o.width(p) : o.height(p)) * o.scale;
            Collections.addAll(targets, start, start + len / 2, start + len);
        }
        float best = SNAP, out = pos, guide = Float.NaN;
        float[] offsets = {0, size / 2, size};   // the held module's start, centre and end
        for (float t : targets)
            for (float off : offsets) {
                float d = Math.abs(pos + off - t);
                if (d < best) {
                    best = d;
                    out = t - off;
                    guide = t;
                }
            }
        return new float[]{out, guide};
    }

    /** Inverse of HudModule.screenX / screenY: a position as a fraction of the free space, kept on screen. */
    private static float fraction(float pos, float screen, float size) {
        float free = screen - 2 * HudModule.MARGIN - size;
        return free <= 0 ? 0 : Math.max(0, Math.min(1, (pos - HudModule.MARGIN) / free));
    }

    @Override
    protected void mouseReleased(int mx, int my, int state) {
        super.mouseReleased(mx, my, state);
        if (held != null) {
            commit();
            Modules.save();
        }
        held = null;
        guideX = guideY = Float.NaN;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;
        int mx = Mouse.getEventX() * width / mc.displayWidth, my = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        HudModule h = at(mx, my);
        if (h == null) return;
        float scale = Math.max(0.5f, Math.min(2.5f, Math.round((h.scale + Math.signum(wheel) * 0.1f) * 10) / 10f));
        if (scale == h.scale) return;
        remember(snapshot());
        h.scale = scale;
        Modules.save();
    }

    private void nudge(float dx, float dy) {
        boolean p = preview();
        remember(snapshot());
        float w = selected.width(p) * selected.scale, h = selected.height(p) * selected.scale;
        selected.x = fraction(selected.screenX(width, p) + dx, width, w);
        selected.y = fraction(selected.screenY(height, p) + dy, height, h);
        Modules.save();
    }

    @Override
    protected void actionPerformed(GuiButton b) {
        close();
    }

    @Override
    protected void keyTyped(char c, int key) {
        float step = isShiftKeyDown() ? 10 : 1;
        if (key == Keyboard.KEY_ESCAPE) close();
        else if (isCtrlKeyDown() && key == Keyboard.KEY_Z) step(isShiftKeyDown() ? redo : undo, isShiftKeyDown() ? undo : redo);
        else if (isCtrlKeyDown() && key == Keyboard.KEY_Y) step(redo, undo);
        else if (selected != null && key == Keyboard.KEY_LEFT) nudge(-step, 0);
        else if (selected != null && key == Keyboard.KEY_RIGHT) nudge(step, 0);
        else if (selected != null && key == Keyboard.KEY_UP) nudge(0, -step);
        else if (selected != null && key == Keyboard.KEY_DOWN) nudge(0, step);
    }

    private void close() {
        Modules.save();
        mc.displayGuiScreen(parent);
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
