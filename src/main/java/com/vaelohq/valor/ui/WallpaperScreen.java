package com.vaelohq.valor.ui;

import com.vaelohq.valor.Config;
import com.vaelohq.valor.Valor;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.input.Keyboard;

import java.awt.FileDialog;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Wallpaper picker: Valor's two shaders (live previews) and the player's own pictures, GIFs and MP4 / MOV videos from
 * config/valor-wallpapers. "Add your own" copies a chosen file into that folder; files dropped there show up too.
 */
public class WallpaperScreen extends ValorScreen {
    private static final int ADD = 0, FOLDER = 1, DONE = 2;
    private static final float TW = 96, TH = 54, GAP = 10;
    private static final ExecutorService THUMBS = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Valor wallpaper thumbnails");
        t.setDaemon(true);
        return t;
    });

    private final GuiScreen parent;
    private final List<String> items = new ArrayList<>();
    private final Map<String, Integer> thumbs = new HashMap<>();
    private final Map<String, BufferedImage> ready = new HashMap<>();
    private final Map<String, Boolean> requested = new HashMap<>();
    private final Map<String, Framebuffer> previews = new HashMap<>();
    private float px, py, pw, ph, gx, gy;
    private int columns, scanTicks;

    public WallpaperScreen(GuiScreen parent) {
        this.parent = parent;
    }

    private void scan() {
        items.clear();
        items.add(Background.SILK);
        items.add(Background.GLOW);
        String[] names = Background.folder().list((dir, name) -> WallpaperMedia.supported(name));
        if (names != null) {
            Arrays.sort(names, String.CASE_INSENSITIVE_ORDER);
            items.addAll(Arrays.asList(names));
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        parent.setWorldAndResolution(mc, width, height);
        scan();
        columns = (int) Math.max(2, Math.min(4, (width - 60) / (TW + GAP)));
        int rows = Math.max(1, (int) Math.ceil(items.size() / (float) columns));
        pw = columns * TW + (columns - 1) * GAP + 32;
        ph = Math.min(height - 20, 40 + rows * (TH + 20) + 30);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        gx = px + 16;
        gy = py + 34;
        long t = opened + 60;
        float bw = (pw - 32 - 12) / 3, by = py + ph - 26;
        buttonList.add(new Widgets.Pill(ADD, px + 16, by, bw, 16, 8, Ui.I_PLUS, "Add your own", false, t));
        buttonList.add(new Widgets.Pill(FOLDER, px + 16 + bw + 6, by, bw, 16, 8, Ui.I_FOLDER, "Open folder", false, t));
        buttonList.add(new Widgets.Pill(DONE, px + 16 + (bw + 6) * 2, by, bw, 16, 8, -1, "Done", true, t));
    }

    @Override
    public void updateScreen() {
        if (++scanTicks % 40 == 0) {   // pick up files dropped into the folder
            int before = items.size();
            scan();
            if (items.size() != before) initGui();
        }
    }

    @Override
    protected void drawBackground() {
        parent.drawScreen(-1, -1, 0);
    }

    @Override
    protected void drawContent(int mx, int my) {
        float a = Ui.ease(time() / 0.3f);
        Ui.dialog(width, height, px, py, pw, ph, a);
        Ui.semibold().drawCentered("Wallpaper", px + pw / 2, py + 12, 8, Ui.alpha(Ui.TEXT, a));
        String hint = "Pictures, GIFs and MP4 videos";
        Ui.regular().drawCentered(hint, px + pw / 2, py + 23, 5, Ui.alpha(Ui.MUTED, a));
        synchronized (ready) {
            for (Map.Entry<String, BufferedImage> e : ready.entrySet()) thumbs.put(e.getKey(), e.getValue() == null ? 0 : Ui.texture(e.getValue()));
            ready.clear();
        }
        for (int i = 0; i < items.size(); i++) {
            float x = gx + (i % columns) * (TW + GAP), y = gy + (i / columns) * (TH + 20);
            if (y + TH > py + ph - 32) break;
            drawTile(items.get(i), x, y, mx, my, a);
        }
    }

    private void drawTile(String name, float x, float y, int mx, int my, float a) {
        boolean hov = inside(mx, my, x, y, TW, TH), sel = name.equals(Config.wallpaper);
        if (Background.isShader(name)) preview(name, x, y);
        else {
            Integer tex = thumbs.get(name);
            if (tex == null) {
                requestThumb(name);
                Shapes.round(x, y, TW, TH, 6, Ui.alpha(0xFF141A33, a));
                Ui.spinner(x + TW / 2, y + TH / 2, 4, a);
            } else if (tex == 0) {
                Shapes.round(x, y, TW, TH, 6, Ui.alpha(0xFF141A33, a));
                Ui.regular().drawCentered("Can't play this file", x + TW / 2, y + TH / 2 - 3, 5, Ui.alpha(Ui.DANGER, a));
            } else {
                GlStateManager.bindTexture(tex);
                Shapes.image(x, y, TW, TH, 6, a);
            }
        }
        Shapes.draw(x - 1.5f, y - 1.5f, TW + 3, TH + 3, 7.5f, 0, 0, Ui.alpha(sel ? Ui.CYAN : hov ? 0x66FFFFFF : 0x22FFFFFF, a), sel ? 1 : 0.6f, 0, 0, 0);
        String label = name.equals(Background.SILK) ? "Silk" : name.equals(Background.GLOW) ? "Glow" : name;
        ValorFont f = sel ? Ui.semibold() : Ui.regular();
        f.drawCentered(f.fit(label, 5.5f, TW), x + TW / 2, y + TH + 4, 5.5f, Ui.alpha(sel ? Ui.TEXT : 0xFFC9D2E8, a));
        if (hov && !Background.isShader(name)) {
            boolean hot = inside(mx, my, x + TW - 15, y + 3, 12, 12);
            Shapes.round(x + TW - 15, y + 3, 12, 12, 6, Ui.alpha(hot ? 0xE6FF5A6E : 0xB3000000, a));
            Ui.icon(Ui.I_TRASH, x + TW - 12.5f, y + 5.5f, 7, Ui.alpha(0xFFFFFFFF, a));
        }
    }

    /** Renders a shader into a small buffer each frame, so the preview is live and gets rounded corners like the rest. */
    private void preview(String name, float x, float y) {
        if (!OpenGlHelper.isFramebufferEnabled()) {
            Ui.clip(x, y, TW, TH);
            GlStateManager.pushMatrix();
            GlStateManager.translate(x, y, 0);
            Background.shader(name, TW, TH);
            GlStateManager.popMatrix();
            Ui.endClip();
            return;
        }
        int scale = new net.minecraft.client.gui.ScaledResolution(mc).getScaleFactor(), bw = Math.round(TW * scale), bh = Math.round(TH * scale);
        Framebuffer fb = previews.get(name);
        if (fb == null || fb.framebufferWidth != bw || fb.framebufferHeight != bh) {
            if (fb != null) fb.deleteFramebuffer();
            previews.put(name, fb = new Framebuffer(bw, bh, false));
            fb.setFramebufferFilter(org.lwjgl.opengl.GL11.GL_LINEAR);
        }
        fb.bindFramebuffer(true);
        Background.shader(name, width, height, bw, bh);   // fills the whole buffer
        mc.getFramebuffer().bindFramebuffer(true);
        fb.bindFramebufferTexture();
        Shapes.image(x, y, TW, TH, 6, 1);
    }

    private void requestThumb(String name) {
        if (requested.put(name, true) != null) return;
        File file = new File(Background.folder(), name);
        THUMBS.execute(() -> {
            BufferedImage img = WallpaperMedia.thumbnail(file, Math.round(TW * 3));
            synchronized (ready) {
                ready.put(name, img);
            }
        });
    }

    private static boolean inside(int mx, int my, float x, float y, float w, float h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws java.io.IOException {
        super.mouseClicked(mx, my, button);
        if (button != 0) return;
        for (int i = 0; i < items.size(); i++) {
            float x = gx + (i % columns) * (TW + GAP), y = gy + (i / columns) * (TH + 20);
            if (!inside(mx, my, x, y, TW, TH)) continue;
            String name = items.get(i);
            if (!Background.isShader(name) && inside(mx, my, x + TW - 15, y + 3, 12, 12)) delete(name);
            else {
                Config.wallpaper = name;
                Config.save();
            }
            return;
        }
    }

    private void delete(String name) {
        mc.displayGuiScreen(new ConfirmScreen(this, "Delete this wallpaper?", name + " will be removed from the wallpaper folder.", "Delete", "Cancel",
                true, yes -> {
            if (yes) {
                if (name.equals(Config.wallpaper)) {
                    Config.wallpaper = Background.SILK;
                    Config.save();
                }
                Background.release();   // stops playback before the file is deleted
                if (!new File(Background.folder(), name).delete()) toast("Couldn't delete " + name);
                Integer tex = thumbs.remove(name);
                if (tex != null && tex > 0) TextureUtil.deleteTexture(tex);
                requested.remove(name);
            }
            mc.displayGuiScreen(this);
        }));
    }

    /** Opens the system file picker on its own thread; falls back to opening the folder where AWT can't show one. */
    private void addOwn() {
        if (GraphicsEnvironment.isHeadless()) {
            Ui.openFolder(Background.folder());
            toast("Put your files in this folder");
            return;
        }
        Thread t = new Thread(() -> {
            try {
                FileDialog d = new FileDialog((java.awt.Frame) null, "Choose a wallpaper", FileDialog.LOAD);
                d.setFile("*.png;*.jpg;*.jpeg;*.gif;*.mp4;*.m4v;*.mov");   // filter on Windows
                d.setFilenameFilter((dir, name) -> WallpaperMedia.supported(name));   // filter on macOS and Linux
                d.setVisible(true);
                String dir = d.getDirectory(), name = d.getFile();
                d.dispose();
                if (dir == null || name == null) return;
                File src = new File(dir, name);
                mc.addScheduledTask(() -> add(src));
            } catch (Throwable e) {
                Valor.LOGGER.warn("Couldn't open the file picker", e);
                mc.addScheduledTask(() -> {
                    Ui.openFolder(Background.folder());
                    toast("Put your files in this folder");
                });
            }
        }, "Valor file picker");
        t.setDaemon(true);
        t.start();
    }

    /** Copies a chosen file into the wallpaper folder (keeping both on a name clash) and selects it. */
    private void add(File src) {
        if (!WallpaperMedia.supported(src.getName())) {
            toast("Use a PNG, JPG, GIF or MP4 file");
            return;
        }
        File dest = new File(Background.folder(), src.getName());
        String base = src.getName().replaceFirst("\\.[^.]+$", ""), ext = src.getName().substring(base.length());
        for (int i = 2; dest.exists(); i++) dest = new File(Background.folder(), base + " (" + i + ")" + ext);
        try {
            Files.copy(src.toPath(), dest.toPath());
        } catch (Exception e) {
            Valor.LOGGER.warn("Couldn't copy wallpaper {}", src, e);
            toast("Couldn't add that file");
            return;
        }
        Config.wallpaper = dest.getName();
        Config.save();
        initGui();
    }

    @Override
    protected void actionPerformed(GuiButton b) {
        switch (b.id) {
            case ADD: addOwn(); break;
            case FOLDER: Ui.openFolder(Background.folder()); break;
            case DONE: mc.displayGuiScreen(parent); break;
        }
    }

    @Override
    protected void keyTyped(char c, int key) {
        if (key == Keyboard.KEY_ESCAPE) mc.displayGuiScreen(parent);
    }

    @Override
    public void onGuiClosed() {
        for (Framebuffer fb : previews.values()) fb.deleteFramebuffer();
        previews.clear();
        for (int tex : thumbs.values()) if (tex > 0) TextureUtil.deleteTexture(tex);
        thumbs.clear();
        requested.clear();
    }
}
