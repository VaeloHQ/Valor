package com.vaelohq.valor.ui;

import com.google.common.collect.Lists;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.client.resources.data.PackMetadataSection;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Valor resource pack picker: available packs on the left, selected packs on the right (top wins), with the same
 * rules as vanilla's GuiScreenResourcePacks: the default pack stays last, incompatible packs ask first, and the
 * choice is applied and resources reloaded on Done.
 */
public class ResourcePacksScreen extends ValorScreen {
    private static final int FOLDER = 0, DONE = 1;
    private static final float ROW = 30, GAP = 4, ICON = ROW - 8;

    private final GuiScreen parent;
    private final ScrollList left = new ScrollList(), right = new ScrollList();
    private List<Pack> available, selected;
    private ResourceLocation defaultIcon;
    private String defaultDescription;
    private boolean changed;

    public ResourcePacksScreen(GuiScreen parent) {
        this.parent = parent;
    }

    /** A pack in either list; entry is null for the built-in default pack. */
    private final class Pack {
        final ResourcePackRepository.Entry entry;
        float hover;

        Pack(ResourcePackRepository.Entry entry) {
            this.entry = entry;
        }

        String name() {
            return entry == null ? "Default" : entry.getResourcePackName();
        }

        String description() {
            if (entry == null) return defaultDescription;
            String d = entry.getTexturePackDescription();
            return d == null ? "" : d;
        }

        /** Pack format: 1 is right for 1.8.9, lower is older, higher is newer. */
        int format() {
            return entry == null ? 1 : entry.func_183027_f();
        }

        void bindIcon() {
            if (entry == null) mc.getTextureManager().bindTexture(defaultIcon);
            else entry.bindTexturePackIcon(mc.getTextureManager());
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        if (available == null) load();
        float cw = Math.min(200, (width - 50) / 2f), x0 = (width - cw * 2 - 10) / 2, top = 52, h = height - top - 36;
        left.place(x0, top, cw, h);
        right.place(x0 + cw + 10, top, cw, h);
        long t = opened + 80;
        float bw = 110;
        buttonList.add(new Widgets.Pill(FOLDER, width / 2f - bw - 3, height - 26, bw, 16, 8, -1, "Open pack folder", false, t));
        buttonList.add(new Widgets.Pill(DONE, width / 2f + 3, height - 26, bw, 16, 8, -1, "Done", true, t));
    }

    /** Fills both lists from the repository, as vanilla does when the screen first opens. */
    private void load() {
        ResourcePackRepository repo = mc.getResourcePackRepository();
        repo.updateRepositoryEntriesAll();
        DynamicTexture icon;
        try {
            icon = new DynamicTexture(repo.rprDefaultResourcePack.getPackImage());
        } catch (IOException e) {
            icon = TextureUtil.missingTexture;
        }
        defaultIcon = mc.getTextureManager().getDynamicTextureLocation("texturepackicon", icon);
        try {
            PackMetadataSection meta = (PackMetadataSection) repo.rprDefaultResourcePack.getPackMetadata(repo.rprMetadataSerializer, "pack");
            defaultDescription = meta != null ? meta.getPackDescription().getFormattedText() : "";
        } catch (Exception e) {
            defaultDescription = "";
        }
        List<ResourcePackRepository.Entry> all = Lists.newArrayList(repo.getRepositoryEntriesAll());
        all.removeAll(repo.getRepositoryEntries());
        available = new ArrayList<>();
        for (ResourcePackRepository.Entry e : all) available.add(new Pack(e));
        selected = new ArrayList<>();
        for (ResourcePackRepository.Entry e : Lists.reverse(repo.getRepositoryEntries())) selected.add(new Pack(e));
        selected.add(new Pack(null));
    }

    @Override
    protected void drawContent(int mx, int my) {
        float a = Ui.ease(time() / 0.3f), cx = width / 2f;
        Ui.bold().drawCentered("Resource Packs", cx, 10 + (1 - a) * 4, 12, Ui.alpha(Ui.TEXT, a));
        Ui.regular().drawCentered("Hover a pack to add, remove or move it. Packs at the top win.", cx, 26 + (1 - a) * 4, 5, Ui.alpha(Ui.MUTED, a));
        Ui.semibold().draw("Available", left.x + 2, 41, 6, Ui.alpha(0xFFC9D2E8, a));
        Ui.semibold().draw("Selected", right.x + 2, 41, 6, Ui.alpha(0xFFC9D2E8, a));
        drawList(left, available, false, mx, my, a);
        drawList(right, selected, true, mx, my, a);
        if (available.isEmpty())
            Ui.regular().drawCentered("Put packs in the pack folder", left.x + left.w / 2, left.y + 20, 5.5f, Ui.alpha(0xFF6B7591, a));
    }

    private void drawList(ScrollList list, List<Pack> packs, boolean isSelected, int mx, int my, float a) {
        boolean in = list.contains(mx, my);
        list.begin(packs.size() * (ROW + GAP) - GAP);
        for (int i = 0; i < packs.size(); i++) {
            Pack p = packs.get(i);
            float y = list.y + i * (ROW + GAP) - list.scroll;
            boolean hov = in && my >= y && my < y + ROW;
            p.hover = Ui.approach(p.hover, hov ? 1 : 0);
            if (y + ROW < list.y || y > list.y + list.h) continue;
            drawPack(list, packs, p, i, y, isSelected, hov, mx, my, a * Ui.ease((time() - 0.03f * Math.min(i, 10)) / 0.3f));
        }
        list.end(mx, my, a);
    }

    private void drawPack(ScrollList list, List<Pack> packs, Pack p, int i, float y, boolean isSelected, boolean hov, int mx, int my, float a) {
        float x = list.x, w = list.w, hv = p.hover;
        boolean bad = p.format() != 1;
        Shapes.draw(x, y, w, ROW, 8, Ui.alpha(Ui.mix(0xB4141A30, 0xD01B2340, hv), a), Ui.alpha(Ui.mix(0xB40A0F20, 0xD0111933, hv), a),
                Ui.alpha(bad ? 0x99FF5A6E : Ui.mix(0x1FFFFFFF, 0x38FFFFFF, hv), a), 0.5f, Ui.alpha(0x55000000, a), 4, 1.5f);
        p.bindIcon();
        Shapes.image(x + 4, y + 4, ICON, ICON, 5, a);
        float tx = x + ICON + 9, room = w - ICON - 13 - (isSelected ? 40 : 16);
        ValorFont semi = Ui.semibold(), small = Ui.regular();
        semi.draw(semi.fit(p.name().replaceAll("§.", ""), 6, room), tx, y + 4, 6, Ui.alpha(Ui.TEXT, a));
        if (bad) {
            String why = I18n.format(p.format() < 1 ? "resourcePack.incompatible.old" : "resourcePack.incompatible.new");
            small.draw(small.fit(why, 5, room), tx, y + 13, 5, Ui.alpha(Ui.DANGER, a));
        } else {
            List<String> lines = small.wrapFormatted(p.description(), 5, room);
            for (int l = 0; l < lines.size(); l++) small.drawFormatted(lines.get(l), tx, y + 13 + l * 6.5f, 5, Ui.MUTED, a);
        }
        if (!hov || p.entry == null) return;
        float ay = y + (ROW - 10) / 2;
        if (!isSelected) Widgets.drawArrow(x + w - 13, ay, 0, true, inside(mx, my, x + w - 13, ay), a);
        else {
            Widgets.drawArrow(x + w - 37, ay, 180, true, inside(mx, my, x + w - 37, ay), a);
            Widgets.drawArrow(x + w - 25, ay, -90, i > 0, inside(mx, my, x + w - 25, ay), a);
            Widgets.drawArrow(x + w - 13, ay, 90, i < packs.size() - 2, inside(mx, my, x + w - 13, ay), a);
        }
    }

    private static boolean inside(int mx, int my, float x, float y) {
        return mx >= x && my >= y && mx < x + 10 && my < y + 10;
    }

    private int rowAt(ScrollList list, List<Pack> packs, int mx, int my) {
        if (!list.contains(mx, my)) return -1;
        int i = (int) Math.floor((my - list.y + list.scroll) / (ROW + GAP));
        return i >= 0 && i < packs.size() && my < list.y + i * (ROW + GAP) - list.scroll + ROW ? i : -1;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel(), mx = Mouse.getEventX() * width / mc.displayWidth;
        (mx < right.x ? left : right).wheel(wheel, ROW + GAP);
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        super.mouseClicked(mx, my, button);
        if (button != 0 || left.press(mx, my) || right.press(mx, my)) return;
        int i = rowAt(left, available, mx, my);
        if (i >= 0) {
            add(available.get(i));
            return;
        }
        i = rowAt(right, selected, mx, my);
        if (i < 0 || selected.get(i).entry == null) return;
        Pack p = selected.get(i);
        float y = right.y + i * (ROW + GAP) - right.scroll, ay = y + (ROW - 10) / 2, x = right.x + right.w;
        if (inside(mx, my, x - 37, ay)) move(p, selected, available, 0);
        else if (inside(mx, my, x - 25, ay) && i > 0) move(p, selected, selected, i - 1);
        else if (inside(mx, my, x - 13, ay) && i < selected.size() - 2) move(p, selected, selected, i + 1);
    }

    /** Adds a pack to the top of the selected list; an incompatible one asks first (vanilla's confirmation). */
    private void add(Pack p) {
        if (p.format() == 1) {
            move(p, available, selected, 0);
            return;
        }
        String question = I18n.format("resourcePack.incompatible.confirm." + (p.format() > 1 ? "new" : "old"));
        mc.displayGuiScreen(new ConfirmScreen(this, I18n.format("resourcePack.incompatible.confirm.title"), question, "Yes", "No", false, yes -> {
            if (yes) move(p, available, selected, 0);
            mc.displayGuiScreen(this);
        }));
    }

    private void move(Pack p, List<Pack> from, List<Pack> to, int index) {
        from.remove(p);
        to.add(index, p);
        changed = true;
    }

    @Override
    protected void mouseClickMove(int mx, int my, int button, long held) {
        left.drag(my);
        right.drag(my);
    }

    @Override
    protected void mouseReleased(int mx, int my, int state) {
        super.mouseReleased(mx, my, state);
        left.release();
        right.release();
    }

    @Override
    protected void actionPerformed(GuiButton b) {
        if (b.id == FOLDER) Ui.openFolder(mc.getResourcePackRepository().getDirResourcepacks());
        else done();
    }

    @Override
    protected void keyTyped(char c, int key) {
        if (key == Keyboard.KEY_ESCAPE) done();
    }

    /** Applies the selection the way vanilla's Done does, then goes back. */
    private void done() {
        if (changed) {
            List<ResourcePackRepository.Entry> chosen = new ArrayList<>();
            for (Pack p : selected) if (p.entry != null) chosen.add(p.entry);
            Collections.reverse(chosen);
            mc.getResourcePackRepository().setRepositories(chosen);
            mc.gameSettings.resourcePacks.clear();
            mc.gameSettings.incompatibleResourcePacks.clear();
            for (ResourcePackRepository.Entry e : chosen) {
                mc.gameSettings.resourcePacks.add(e.getResourcePackName());
                if (e.func_183027_f() != 1) mc.gameSettings.incompatibleResourcePacks.add(e.getResourcePackName());
            }
            mc.gameSettings.saveOptions();
            mc.refreshResources();
        }
        mc.displayGuiScreen(parent);
    }
}
