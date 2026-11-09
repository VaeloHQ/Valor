package com.vaelohq.valor.ui;

import com.google.common.collect.Iterables;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.IProgressMeter;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityList;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatCrafting;
import net.minecraft.stats.StatFileWriter;
import net.minecraft.stats.StatList;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Valor statistics. Same data as vanilla's GuiStats: general stats, blocks and items (only those used at least once,
 * sortable by clicking a column) and mobs, after the statistics arrive from the server.
 */
public class StatsScreen extends ValorScreen implements IProgressMeter {
    private enum Tab { GENERAL, BLOCKS, ITEMS, MOBS }

    private static final int DONE = 10;
    private static final float ROW = 18, GAP = 3, COL = 52;

    private final GuiScreen parent;
    private boolean requested;
    private final StatFileWriter stats;
    private final ScrollList list = new ScrollList();
    private final List<Row> rows = new ArrayList<>();
    private Tab tab = Tab.GENERAL;
    private String[] columns = {};
    private int sortColumn = -1;
    private boolean descending = true, loading = true;

    public StatsScreen(GuiScreen parent, StatFileWriter stats) {
        this.parent = parent;
        this.stats = stats;
    }

    /** One line: an optional icon, a name and one value per column (raw numbers for sorting, -1 when the stat does not exist). */
    private static final class Row {
        final ItemStack icon;
        final String name;
        final String[] text;
        final int[] raw;

        Row(ItemStack icon, String name, String[] text, int[] raw) {
            this.icon = icon;
            this.name = name;
            this.text = text;
            this.raw = raw;
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        if (!requested) mc.getNetHandler().addToSendQueue(new C16PacketClientStatus(C16PacketClientStatus.EnumState.REQUEST_STATS));
        requested = true;
        float lw = Math.min(380, width - 40), lx = (width - lw) / 2;
        list.place(lx, 64, lw, height - 64 - 36);
        long t = opened + 80;
        String[] tabs = {"General", "Blocks", "Items", "Mobs"};
        float tw = 66;
        for (int i = 0; i < 4; i++)
            buttonList.add(new Widgets.Pill(i, width / 2f - (tw * 4 + 18) / 2 + i * (tw + 6), 30, tw, 14, 7, -1, tabs[i], tab.ordinal() == i, t));
        buttonList.add(new Widgets.Pill(DONE, width / 2f - 60, height - 26, 120, 16, 8, -1, "Done", false, t));
        build();
    }

    @Override
    public void doneLoading() {
        loading = false;
        build();
    }

    private void build() {
        rows.clear();
        switch (tab) {
            case GENERAL:
                columns = new String[]{""};
                for (StatBase s : StatList.generalStats)
                    rows.add(new Row(null, s.getStatName().getUnformattedText(), new String[]{s.format(stats.readStat(s))}, new int[]{stats.readStat(s)}));
                break;
            case BLOCKS:
                columns = new String[]{"Crafted", "Used", "Mined"};
                for (StatCrafting s : StatList.objectMineStats) {
                    int id = Item.getIdFromItem(s.func_150959_a());
                    addItem(s.func_150959_a(), StatList.objectCraftStats[id], StatList.objectUseStats[id], id < StatList.mineBlockStatArray.length ? StatList.mineBlockStatArray[id] : null);
                }
                break;
            case ITEMS:
                columns = new String[]{"Broken", "Crafted", "Used"};
                for (StatCrafting s : StatList.itemStats) {
                    int id = Item.getIdFromItem(s.func_150959_a());
                    addItem(s.func_150959_a(), StatList.objectBreakStats[id], StatList.objectCraftStats[id], StatList.objectUseStats[id]);
                }
                break;
            case MOBS:
                columns = new String[]{"Kills", "Killed you"};
                for (EntityList.EntityEggInfo egg : Iterables.concat(EntityList.entityEggs.values(), EntityRegistry.getEggs().values())) {
                    int kills = stats.readStat(egg.field_151512_d), deaths = stats.readStat(egg.field_151513_e);
                    if (kills <= 0 && deaths <= 0) continue;
                    rows.add(new Row(null, I18n.format("entity." + egg.name + ".name"), new String[]{String.valueOf(kills), String.valueOf(deaths)},
                            new int[]{kills, deaths}));
                }
                break;
        }
        sort();
    }

    /** Adds an item row when any of its stats is above zero (vanilla shows only those). */
    private void addItem(Item item, StatBase... s) {
        int[] raw = new int[s.length];
        String[] text = new String[s.length];
        boolean any = false;
        for (int i = 0; i < s.length; i++) {
            raw[i] = s[i] == null ? -1 : stats.readStat(s[i]);
            text[i] = s[i] == null ? "-" : s[i].format(raw[i]);
            any |= raw[i] > 0;
        }
        if (any) rows.add(new Row(new ItemStack(item), new ItemStack(item).getDisplayName(), text, raw));
    }

    private void sort() {
        if (sortColumn < 0 || sortColumn >= columns.length || tab == Tab.GENERAL) return;
        Comparator<Row> by = Comparator.comparingInt(r -> r.raw[sortColumn]);
        rows.sort(descending ? by.reversed() : by);
    }

    @Override
    protected void drawContent(int mx, int my) {
        float a = Ui.ease(time() / 0.3f), cx = width / 2f;
        Ui.bold().drawCentered("Statistics", cx, 10 + (1 - a) * 4, 12, Ui.alpha(Ui.TEXT, a));
        if (loading) {
            Ui.spinner(cx, height / 2f - 6, 6, a);
            Ui.regular().drawCentered(I18n.format("multiplayer.downloadingStats"), cx, height / 2f + 8, 5.5f, Ui.alpha(Ui.MUTED, a));
            return;
        }
        float right = list.x + list.w - 10;
        if (tab != Tab.GENERAL) for (int c = 0; c < columns.length; c++) {
            float colRight = right - (columns.length - 1 - c) * COL;
            String label = columns[c] + (c == sortColumn ? (descending ? " ↓" : " ↑") : "");
            boolean hot = my >= 50 && my < 60 && mx >= colRight - COL + 6 && mx < colRight;
            ValorFont f = Ui.semibold();
            f.draw(label, colRight - f.width(label, 5), 52, 5, Ui.alpha(hot || c == sortColumn ? Ui.TEXT : Ui.MUTED, a));
        }
        list.begin(rows.size() * (ROW + GAP) - GAP);
        for (int i = 0; i < rows.size(); i++) {
            float y = list.y + i * (ROW + GAP) - list.scroll;
            if (y + ROW < list.y || y > list.y + list.h) continue;
            Row r = rows.get(i);
            float ra = a * Ui.ease((time() - 0.02f * Math.min(i, 12)) / 0.3f);
            Shapes.draw(list.x, y, list.w, ROW, ROW / 2, Ui.alpha(i % 2 == 0 ? 0xB4141A30 : 0xB40F1428, ra), Ui.alpha(i % 2 == 0 ? 0xB40A0F20 : 0xB4080C1A, ra),
                    Ui.alpha(0x1AFFFFFF, ra), 0.5f, 0, 0, 0);
            float tx = list.x + 9;
            if (r.icon != null) {
                Ui.item(r.icon, tx - 2, y + 1, 1);
                tx += 16;
            }
            float room = (right - (tab == Tab.GENERAL ? 70 : columns.length * COL)) - tx;
            Ui.regular().draw(Ui.regular().fit(r.name, 5.5f, room), tx, y + 5.7f, 5.5f, Ui.alpha(0xFFC9D2E8, ra));
            for (int c = 0; c < r.text.length; c++) {
                float colRight = right - (r.text.length - 1 - c) * COL;
                ValorFont f = Ui.semibold();
                f.draw(r.text[c], colRight - f.width(r.text[c], 5.5f), y + 5.7f, 5.5f, Ui.alpha(r.raw[c] > 0 ? 0xFF8FB4FF : 0xFF6B7591, ra));
            }
        }
        list.end(mx, my, a);
        if (rows.isEmpty()) Ui.regular().drawCentered("Nothing here yet", cx, list.y + 20, 5.5f, Ui.alpha(0xFF6B7591, a));
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        list.wheel(Mouse.getEventDWheel(), (ROW + GAP) * 2);
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        super.mouseClicked(mx, my, button);
        if (button != 0 || loading || list.press(mx, my) || tab == Tab.GENERAL || my < 50 || my >= 60) return;
        float right = list.x + list.w - 10;
        for (int c = 0; c < columns.length; c++) {   // column headers sort: first click high to low, second low to high
            float colRight = right - (columns.length - 1 - c) * COL;
            if (mx >= colRight - COL + 6 && mx < colRight) {
                descending = c != sortColumn || !descending;
                sortColumn = c;
                sort();
            }
        }
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
    protected void actionPerformed(GuiButton b) {
        if (loading) return;
        if (b.id == DONE) {
            mc.displayGuiScreen(parent);
            return;
        }
        tab = Tab.values()[b.id];
        sortColumn = -1;
        initGui();
    }

    @Override
    protected void keyTyped(char c, int key) {
        if (key == Keyboard.KEY_ESCAPE) {   // vanilla: Escape goes back to the game
            mc.displayGuiScreen(null);
            mc.setIngameFocus();
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return !loading;
    }
}
