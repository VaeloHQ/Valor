package com.vaelohq.valor.ui;

import com.vaelohq.valor.module.HudModule;
import com.vaelohq.valor.module.Module;
import com.vaelohq.valor.module.Modules;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * HUD layout editor: drag modules to move them (they snap to the screen's centre lines), scroll over one to resize it,
 * right-click one for its settings. Out of game the modules show sample data.
 */
public class HudEditorScreen extends ValorScreen {
    private static final int DONE = 0;
    private static final float SNAP = 4;

    private final GuiScreen parent;
    private HudModule held;
    private float grabX, grabY;
    private boolean snapX, snapY;

    public HudEditorScreen(GuiScreen parent) {
        this.parent = parent;
    }

    private boolean preview() {
        return mc.theWorld == null;
    }

    private List<HudModule> modules() {
        List<HudModule> l = new ArrayList<>();
        for (Module m : Modules.all()) if (m instanceof HudModule && m.enabled()) l.add((HudModule) m);
        return l;
    }

    @Override
    public void initGui() {
        super.initGui();
        buttonList.add(new Widgets.Pill(DONE, width / 2f - 45, height / 2f + 14, 90, 16, 8, -1, "Done", true, opened + 60));
    }

    @Override
    protected void drawBackground() {
        if (preview()) Ui.menuBackground(width, height);
        else Ui.rect(0, 0, width, height, 0x40000000);   // keep the game visible behind the layout
    }

    @Override
    protected void drawContent(int mx, int my) {
        float a = Ui.ease(time() / 0.3f);
        if (snapX) Ui.rect(width / 2f - 0.25f, 0, width / 2f + 0.25f, height, Ui.alpha(Ui.CYAN, 0.7f));
        if (snapY) Ui.rect(0, height / 2f - 0.25f, width, height / 2f + 0.25f, Ui.alpha(Ui.CYAN, 0.7f));
        Ui.semibold().drawCentered("Edit HUD layout", width / 2f, height / 2f - 16, 8, Ui.alpha(Ui.TEXT, a * 0.9f));
        Ui.regular().drawCentered("Drag to move  ·  Scroll to resize  ·  Right-click for settings", width / 2f, height / 2f - 4, 5,
                Ui.alpha(Ui.MUTED, a));
        Modules.drawHud(width, height, preview());
        HudModule over = at(mx, my);
        for (HudModule h : modules()) {
            boolean p = preview();
            float x = h.screenX(width, p), y = h.screenY(height, p), w = h.width(p) * h.scale, hh = h.height(p) * h.scale;
            boolean hot = h == held || h == over && held == null;
            Shapes.draw(x - 1.5f, y - 1.5f, w + 3, hh + 3, 4, 0, 0, Ui.alpha(hot ? Ui.CYAN : 0x55FFFFFF, a), hot ? 0.9f : 0.5f, 0, 0, 0);
            if (hot) Ui.regular().draw(h.name + "  " + Math.round(h.scale * 100) + "%", x, y + hh + 3, 4.5f, Ui.alpha(Ui.TEXT, a));
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

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        super.mouseClicked(mx, my, button);
        HudModule h = at(mx, my);
        if (h == null) return;
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
        float w = held.width(p) * held.scale, h = held.height(p) * held.scale, x = mx - grabX, y = my - grabY;
        snapX = Math.abs(x + w / 2 - width / 2f) < SNAP;
        snapY = Math.abs(y + h / 2 - height / 2f) < SNAP;
        if (snapX) x = width / 2f - w / 2;
        if (snapY) y = height / 2f - h / 2;
        held.x = fraction(x, width, w);
        held.y = fraction(y, height, h);
    }

    /** Inverse of HudModule.screenX / screenY: a position as a fraction of the free space, kept on screen. */
    private static float fraction(float pos, float screen, float size) {
        float free = screen - 2 * HudModule.MARGIN - size;
        return free <= 0 ? 0 : Math.max(0, Math.min(1, (pos - HudModule.MARGIN) / free));
    }

    @Override
    protected void mouseReleased(int mx, int my, int state) {
        super.mouseReleased(mx, my, state);
        if (held != null) Modules.save();
        held = null;
        snapX = snapY = false;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;
        int mx = Mouse.getEventX() * width / mc.displayWidth, my = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        HudModule h = at(mx, my);
        if (h == null) return;
        h.scale = Math.max(0.5f, Math.min(2.5f, Math.round((h.scale + Math.signum(wheel) * 0.1f) * 10) / 10f));
        Modules.save();
    }

    @Override
    protected void actionPerformed(GuiButton b) {
        close();
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
        return false;
    }
}
