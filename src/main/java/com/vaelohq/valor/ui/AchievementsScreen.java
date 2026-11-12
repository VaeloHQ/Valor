package com.vaelohq.valor.ui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.IProgressMeter;
import net.minecraft.client.resources.I18n;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.stats.Achievement;
import net.minecraft.stats.AchievementList;
import net.minecraft.stats.StatFileWriter;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraftforge.common.AchievementPage;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Valor achievement map. Same rules as vanilla's GuiAchievements: statistics are requested from the server first,
 * achievements far beyond what is unlocked stay hidden, Forge's achievement pages can be switched, the map pans by
 * dragging and zooms with the wheel, and the inventory key returns to the game.
 */
public class AchievementsScreen extends ValorScreen implements IProgressMeter {
    private static final int DONE = 0, PAGE = 1;
    private static final float CELL = 24, TILE = 22;

    private final GuiScreen parent;
    private boolean requested;
    private final StatFileWriter stats;
    private boolean loading = true;
    private int page = -1;
    private float panX, panY, zoom = 1, mx0, my0, x0, y0, w0, h0;
    private boolean dragging;
    private Widgets.Pill pageButton;

    public AchievementsScreen(GuiScreen parent, StatFileWriter stats) {
        this.parent = parent;
        this.stats = stats;
        panX = AchievementList.openInventory.displayColumn * CELL;
        panY = AchievementList.openInventory.displayRow * CELL;
    }

    /** The current page: vanilla achievements, or a Forge page. */
    private List<Achievement> achievements() {
        if (page != -1) return AchievementPage.getAchievementPage(page).getAchievements();
        List<Achievement> list = new ArrayList<>();
        for (Achievement a : AchievementList.achievementList) if (!AchievementPage.isAchievementInPages(a)) list.add(a);
        return list;
    }

    @Override
    public void initGui() {
        super.initGui();
        if (!requested) mc.getNetHandler().addToSendQueue(new C16PacketClientStatus(C16PacketClientStatus.EnumState.REQUEST_STATS));
        requested = true;
        x0 = 20;
        y0 = 42;
        w0 = width - 40;
        h0 = height - y0 - 36;
        long t = opened + 80;
        if (!AchievementPage.getAchievementPages().isEmpty()) {
            pageButton = new Widgets.Pill(PAGE, width / 2f - 126, height - 26, 120, 16, 8, -1, AchievementPage.getTitle(page), false, t);
            buttonList.add(pageButton);
            buttonList.add(new Widgets.Pill(DONE, width / 2f + 6, height - 26, 120, 16, 8, -1, "Done", true, t));
        } else buttonList.add(new Widgets.Pill(DONE, width / 2f - 60, height - 26, 120, 16, 8, -1, "Done", true, t));
    }

    @Override
    public void doneLoading() {
        loading = false;
    }

    @Override
    protected void drawContent(int mx, int my) {
        float a = Ui.ease(time() / 0.3f), cx = width / 2f;
        Ui.bold().drawCentered("Achievements", cx, 10 + (1 - a) * 4, 12, Ui.alpha(Ui.TEXT, a));
        if (loading) {
            Ui.spinner(cx, height / 2f - 6, 6, a);
            Ui.regular().drawCentered(I18n.format("multiplayer.downloadingStats"), cx, height / 2f + 8, 5.5f, Ui.alpha(Ui.MUTED, a));
            return;
        }
        List<Achievement> list = achievements();
        int got = 0;
        for (Achievement ach : list) if (stats.hasAchievementUnlocked(ach)) got++;
        Ui.regular().drawCentered(got + " of " + list.size() + " unlocked  ·  drag to move, scroll to zoom", cx, 26 + (1 - a) * 4, 5,
                Ui.alpha(Ui.MUTED, a));
        clampPan(list);
        Shapes.draw(x0, y0, w0, h0, 10, Ui.alpha(0xE60A0F20, a), Ui.alpha(0xE6070B18, a), Ui.alpha(0x1FFFFFFF, a), 0.5f, 0, 0, 0);
        Ui.clip(x0, y0, w0, h0);
        for (Achievement ach : list) {   // L-shaped links to the parent, as vanilla draws them
            if (ach.parentAchievement == null || !list.contains(ach.parentAchievement) || stats.func_150874_c(ach) > 4) continue;
            float ax = sx(ach.displayColumn), ay = sy(ach.displayRow), px = sx(ach.parentAchievement.displayColumn), py = sy(ach.parentAchievement.displayRow);
            int c = Ui.alpha(stats.hasAchievementUnlocked(ach) ? Ui.PRIMARY_HI : stats.canUnlockAchievement(ach) ? 0xB0FFFFFF : 0x30FFFFFF, a);
            Ui.rect(Math.min(ax, px), ay - 0.5f, Math.max(ax, px), ay + 0.5f, c);
            Ui.rect(px - 0.5f, Math.min(ay, py), px + 0.5f, Math.max(ay, py), c);
        }
        Achievement hovered = null;
        float s = TILE * zoom;
        for (Achievement ach : list) {
            int depth = stats.func_150874_c(ach);
            boolean have = stats.hasAchievementUnlocked(ach), can = stats.canUnlockAchievement(ach);
            if (!have && !can && depth > 4) continue;
            float x = sx(ach.displayColumn) - s / 2, y = sy(ach.displayRow) - s / 2;
            if (x + s < x0 || y + s < y0 || x > x0 + w0 || y > y0 + h0) continue;
            float fade = have || can ? 1 : depth < 3 ? 0.55f : depth == 3 ? 0.35f : 0.2f;
            int ring = ach.getSpecial() ? Ui.CYAN : have ? 0x80FFFFFF : 0x30FFFFFF;
            Shapes.draw(x, y, s, s, s * 0.3f, Ui.alpha(have ? Ui.PRIMARY_HI : 0xFF1B2340, a * fade), Ui.alpha(have ? Ui.PRIMARY : 0xFF111933, a * fade),
                    Ui.alpha(ring, a * fade), ach.getSpecial() ? 0.9f : 0.5f, have ? Ui.alpha(0x662563FF, a) : 0, have ? 4 : 0, 0);
            Ui.item(ach.theItemStack, x + (s - 16 * zoom) / 2, y + (s - 16 * zoom) / 2, zoom);
            if (!have && !can) Shapes.round(x, y, s, s, s * 0.3f, Ui.alpha(0xB0070B18, a));
            if (mx >= x && my >= y && mx < x + s && my < y + s && mx >= x0 && my >= y0 && mx < x0 + w0 && my < y0 + h0) hovered = ach;
        }
        Ui.endClip();
        if (hovered != null && !dragging) Ui.tooltip(tooltip(hovered), mx, my, width);
    }

    /** Vanilla's tooltip: name and description when it can be taken, otherwise what it requires (or "?" when far away). */
    private String tooltip(Achievement ach) {
        int depth = stats.func_150874_c(ach);
        String name = ach.getStatName().getUnformattedText();
        if (stats.canUnlockAchievement(ach)) {
            StringBuilder b = new StringBuilder(ach.getSpecial() ? "§e" : "§f").append(name);
            for (String l : Ui.regular().wrap(ach.getDescription(), 5.5f, 150)) b.append("\n§7").append(l);
            if (stats.hasAchievementUnlocked(ach)) b.append("\n§b").append(I18n.format("achievement.taken"));
            return b.toString();
        }
        if (depth > 3) return null;
        String requires = new ChatComponentTranslation("achievement.requires", ach.parentAchievement.getStatName()).getUnformattedText();
        StringBuilder b = new StringBuilder("§8").append(depth == 3 ? I18n.format("achievement.unknown") : name);
        for (String l : Ui.regular().wrap(requires, 5.5f, 150)) b.append("\n§c").append(l);
        return b.toString();
    }

    /** Screen position of a map column / row. */
    private float sx(int column) {
        return x0 + w0 / 2 + (column * CELL - panX) * zoom;
    }

    private float sy(int row) {
        return y0 + h0 / 2 + (row * CELL - panY) * zoom;
    }

    /** Keeps the view over the page's achievements. */
    private void clampPan(List<Achievement> list) {
        if (list.isEmpty()) return;
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (Achievement ach : list) {
            minX = Math.min(minX, ach.displayColumn * CELL);
            maxX = Math.max(maxX, ach.displayColumn * CELL);
            minY = Math.min(minY, ach.displayRow * CELL);
            maxY = Math.max(maxY, ach.displayRow * CELL);
        }
        panX = Math.max(minX, Math.min(maxX, panX));
        panY = Math.max(minY, Math.min(maxY, panY));
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0 && !loading) zoom = Math.max(0.5f, Math.min(1.5f, zoom + Math.signum(wheel) * 0.125f));
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        super.mouseClicked(mx, my, button);
        if (button == 0 && mx >= x0 && my >= y0 && mx < x0 + w0 && my < y0 + h0) {
            dragging = true;
            mx0 = mx;
            my0 = my;
        }
    }

    @Override
    protected void mouseClickMove(int mx, int my, int button, long held) {
        if (!dragging) return;
        panX -= (mx - mx0) / zoom;
        panY -= (my - my0) / zoom;
        mx0 = mx;
        my0 = my;
    }

    @Override
    protected void mouseReleased(int mx, int my, int state) {
        super.mouseReleased(mx, my, state);
        dragging = false;
    }

    @Override
    protected void actionPerformed(GuiButton b) {
        if (loading) return;
        if (b.id == DONE) mc.displayGuiScreen(parent);
        else {
            page = page + 1 >= AchievementPage.getAchievementPages().size() ? -1 : page + 1;
            pageButton.displayString = AchievementPage.getTitle(page);
        }
    }

    @Override
    protected void keyTyped(char c, int key) {
        if (key == mc.gameSettings.keyBindInventory.getKeyCode() || key == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null);
            mc.setIngameFocus();
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return !loading;
    }
}
