package com.vaelohq.valor.style;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Draws Valor's versions of vanilla GUI textures at 2x resolution (32x look). The HUD sheet, hotbar and buttons are
 * designed here; containers are rebuilt from the player's own vanilla textures by finding their slots, panels and
 * bevels and repainting them in Valor colours. No Mojang pixels ship with Valor.
 */
final class ValorTextures {
    static final int K = 2;

    // Palette (ARGB)
    private static final int NAVY0 = 0xFF050816, NAVY1 = 0xFF0B1028, NAVY2 = 0xFF121B42, NAVY3 = 0xFF1C2A60, BLUE = 0xFF2563FF,
            BLUE_HI = 0xFF4C85FF, CYAN = 0xFF00E5FF, ICE = 0xFFBEE1FF, WHITE = 0xFFF0F6FF;
    private static final int PANEL = 0xEE0C122E, SLOT = 0xEB050818, SLOT_EDGE = 0xFF243A84;

    private static final String[] HEART = {".##...##.", "####.####", "#########", "#########", ".#######.", "..#####..", "...###...", "....#....", "........."};
    private static final String[] DRUMSTICK = {"...####..", "..######.", ".########", ".########", ".########", "..######.", ".bb......", "bb.......", "bb......."};
    private static final String[] CHESTPLATE = {"##.....##", "###...###", "#########", ".#######.", ".#######.", ".#######.", ".#######.", ".#######.", "........."};

    private ValorTextures() {
    }

    // ---------------------------------------------------------------- helpers

    private static int a(int c, int alpha) {
        return (alpha << 24) | (c & 0xFFFFFF);
    }

    private static int mix(int c1, int c2, float t) {
        int r = 0;
        for (int s = 0; s < 32; s += 8) r |= Math.round(((c1 >>> s) & 255) + (((c2 >>> s) & 255) - ((c1 >>> s) & 255)) * t) << s;
        return r;
    }

    private static void fill(BufferedImage img, int x, int y, int w, int h, int c) {
        for (int yy = Math.max(0, y); yy < Math.min(img.getHeight(), y + h); yy++)
            for (int xx = Math.max(0, x); xx < Math.min(img.getWidth(), x + w); xx++) img.setRGB(xx, yy, c);
    }

    /** Rounded panel in base (16x) units: edge colour, inner fill, faint top sheen. */
    private static void panel(BufferedImage img, int x, int y, int w, int h, int fillC, int edge) {
        int X = x * K, Y = y * K, W = w * K, H = h * K;
        fill(img, X + K, Y, W - 2 * K, H, edge);
        fill(img, X, Y + K, W, H - 2 * K, edge);
        fill(img, X + K, Y + K, W - 2 * K, H - 2 * K, fillC);
        fill(img, X + 2 * K, Y + K, W - 4 * K, 1, a(BLUE_HI, 70));
    }

    private static void slot(BufferedImage img, int x, int y, int w, int h) {
        int X = x * K, Y = y * K, W = w * K, H = h * K;
        fill(img, X, Y, W, H, SLOT_EDGE);
        fill(img, X + 1, Y + 1, W - 2, H - 2, SLOT);
        fill(img, X + 1, Y + H - 2, W - 2, 1, a(BLUE, 60));
    }

    private static boolean[][] pixmap(String[] rows) {
        int n = rows.length, N = n * K;
        boolean[][] m = new boolean[N][N];
        for (int y = 0; y < N; y++) for (int x = 0; x < N; x++) m[y][x] = rows[y / K].charAt(x / K) != '.';
        return m;
    }

    private static boolean[][] heartMask(int n) {
        boolean[][] m = new boolean[n][n];
        for (int y = 0; y < n; y++)
            for (int x = 0; x < n; x++) {
                double u = (x + 0.5) / n, v = (y + 0.5) / n, X = (u - 0.5) * 2.45, Y = (0.56 - v) * 2.45;
                m[y][x] = Math.pow(X * X + Y * Y - 1, 3) - X * X * Y * Y * Y <= 0;
            }
        return m;
    }

    private static boolean edgeAt(boolean[][] m, int x, int y) {
        int n = m.length;
        int[][] d = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] o : d) {
            int u = x + o[0], v = y + o[1];
            if (u < 0 || v < 0 || u >= n || v >= n || !m[v][u]) return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- HUD sheet (gui/icons.png)

    static BufferedImage icons() {
        BufferedImage img = new BufferedImage(256 * K, 256 * K, BufferedImage.TYPE_INT_ARGB);
        int n = 9 * K;

        // crosshair: four arms with a gap round a centre dot (white; drawn inverted over the world)
        int c = 7 * K;
        for (int i = K; i < 5 * K; i++)
            for (int t = 0; t < K; t++) {
                img.setRGB(c + i, c + t, WHITE);
                img.setRGB(c - i, c + t, WHITE);
                img.setRGB(c + t, c + i, WHITE);
                img.setRGB(c + t, c - i, WHITE);
            }
        fill(img, c, c, K, K, WHITE);

        boolean[][] heart = heartMask(n);
        int red = 0xFFE83446, redHi = 0xFFFF96A0, flash = 0xFFFFEBF0, flashHi = 0xFFFFFFFF, poison = 0xFF78C83C, poisonHi = 0xFFBEF08C,
                wither = 0xFF2C2C34, witherHi = 0xFF6E6E7D, gold = 0xFFF5C432, goldHi = 0xFFFFEC96;
        for (int row : new int[]{0, 45}) {          // normal and hardcore rows
            int edge = row == 0 ? NAVY0 : 0xFF3C0A12;
            for (int x : new int[]{16, 34}) heart(img, heart, x, row, 0, 0, false, edge);
            for (int x : new int[]{25, 43}) heart(img, heart, x, row, 0, 0, false, WHITE);
            int[][] kinds = {{52, red, redHi}, {88, poison, poisonHi}, {124, wither, witherHi}};
            for (int[] kd : kinds) {
                heart(img, heart, kd[0], row, kd[1], kd[2], false, edge);
                heart(img, heart, kd[0] + 9, row, kd[1], kd[2], true, edge);
                heart(img, heart, kd[0] + 18, row, flash, flashHi, false, edge);
                heart(img, heart, kd[0] + 27, row, flash, flashHi, true, edge);
            }
            heart(img, heart, 160, row, gold, goldHi, false, edge);
            heart(img, heart, 169, row, gold, goldHi, true, edge);
        }
        heart(img, heart, 52, 9, 0, 0, false, NAVY0);   // mount hearts
        heart(img, heart, 61, 9, 0, 0, false, WHITE);
        heart(img, heart, 88, 9, 0xFFFF783C, 0xFFFFC896, false, NAVY0);
        heart(img, heart, 97, 9, 0xFFFF783C, 0xFFFFC896, true, NAVY0);

        boolean[][] armor = pixmap(CHESTPLATE);        // armor: empty, half, full
        for (int i = 0; i < 3; i++)
            for (int y = 0; y < n; y++)
                for (int x = 0; x < n; x++) {
                    if (!armor[y][x]) continue;
                    boolean full = i == 2 || (i == 1 && x < n / 2);
                    img.setRGB((16 + i * 9) * K + x, 9 * K + y, edgeAt(armor, x, y) ? NAVY0 : full ? mix(ICE, BLUE_HI, y / (float) n) : a(NAVY1, 225));
                }

        for (int y = 0; y < n; y++)                     // air bubbles: full, popped
            for (int x = 0; x < n; x++) {
                double d = Math.hypot((x + 0.5) / n - 0.5, (y + 0.5) / n - 0.5);
                if (d < 0.42) img.setRGB(16 * K + x, 18 * K + y, d > 0.33 ? NAVY0 : (x < n * 0.4 && y < n * 0.4) ? WHITE : a(CYAN, 200));
                if (d > 0.28 && d < 0.4 && (x + y) % 2 == 0) img.setRGB(25 * K + x, 18 * K + y, ICE);
            }

        boolean[][] food = pixmap(DRUMSTICK);
        int meat = 0xFFD68446, meatHi = 0xFFF6BA78, rot = 0xFF82A046, rotHi = 0xFFB4D26E;
        food(img, food, 16, 0, 0, false, NAVY0);
        food(img, food, 25, 0, 0, false, WHITE);
        food(img, food, 133, 0, 0, false, 0xFF46641E);
        food(img, food, 52, meat, meatHi, false, NAVY0);
        food(img, food, 61, meat, meatHi, true, NAVY0);
        food(img, food, 70, 0xFFFFEBDC, WHITE, false, NAVY0);
        food(img, food, 79, 0xFFFFEBDC, WHITE, true, NAVY0);
        food(img, food, 88, rot, rotHi, false, NAVY0);
        food(img, food, 97, rot, rotHi, true, NAVY0);
        food(img, food, 106, 0xFFE6F5D2, WHITE, false, NAVY0);
        food(img, food, 115, 0xFFE6F5D2, WHITE, true, NAVY0);

        bar(img, 64, 0, 0);                             // XP
        bar(img, 69, 0xFF00AAFF, CYAN);
        bar(img, 74, 0, 0);                             // boss
        bar(img, 79, BLUE, 0xFF965AFF);
        bar(img, 84, 0, 0);                             // horse jump
        bar(img, 89, 0xFF3CC8FF, WHITE);

        for (int col : new int[]{0, 10})                // tab-list ping: m 0 = five bars ... 4 = one, 5 = no connection
            for (int m = 0; m < 6; m++) {
                int y = 176 + m * 8;
                for (int b = 0; b < 5; b++) {
                    int h = 2 + b + (b > 2 ? 1 : 0);
                    boolean on = m < 5 && b < 5 - m;
                    fill(img, (col + b * 2) * K, (y + 8 - h) * K, K, h * K, on ? mix(BLUE, CYAN, b / 4f) : a(NAVY3, 200));
                }
                if (m == 5) for (int i = 0; i < 7; i++) {
                    fill(img, (col + 1 + i) * K, (y + i) * K, K, K, 0xFFFF465A);
                    fill(img, (col + 7 - i) * K, (y + i) * K, K, K, 0xFFFF465A);
                }
            }
        return img;
    }

    private static void heart(BufferedImage img, boolean[][] m, int x, int y, int fillC, int hi, boolean half, int edge) {
        int n = m.length;
        for (int yy = 0; yy < n; yy++)
            for (int xx = 0; xx < n; xx++) {
                if (!m[yy][xx]) continue;
                int c;
                if (edgeAt(m, xx, yy)) c = edge;
                else if (fillC == 0 || (half && xx >= n / 2)) c = a(NAVY1, 225);
                else c = (yy < n * 0.42 && xx < n * 0.5 && xx + yy < n * 0.62 && yy > n * 0.12) ? hi : fillC;
                img.setRGB(x * K + xx, y * K + yy, c);
            }
    }

    private static void food(BufferedImage img, boolean[][] m, int x, int fillC, int hi, boolean half, int edge) {
        int n = m.length;
        for (int yy = 0; yy < n; yy++)
            for (int xx = 0; xx < n; xx++) {
                if (!m[yy][xx]) continue;
                int c;
                if (DRUMSTICK[yy / K].charAt(xx / K) == 'b') c = fillC != 0 ? WHITE : edge;  // bone stays visible
                else if (edgeAt(m, xx, yy)) c = edge;
                else if (fillC == 0 || (half && xx >= n / 2)) c = a(NAVY1, 225);
                else c = yy < 2 * K ? hi : fillC;
                img.setRGB(x * K + xx, 27 * K + yy, c);
            }
    }

    private static void bar(BufferedImage img, int y, int from, int to) {
        panel(img, 0, y, 182, 5, a(NAVY0, 220), NAVY0);
        if (from == 0) return;
        int W = 182 * K;
        for (int i = 0; i < W; i++) fill(img, i, (y + 1) * K, 1, 3 * K, mix(from, to, i / (float) W));
        fill(img, K, (y + 1) * K, W - 2 * K, 1, a(WHITE, 90));
    }

    // ---------------------------------------------------------------- hotbar and buttons (gui/widgets.png)

    static BufferedImage widgets(BufferedImage vanilla) {
        BufferedImage img = upscale(vanilla);           // keeps the small Realms sprites; everything below is redrawn
        fill(img, 0, 0, 200 * K, 146 * K, 0);
        panel(img, 0, 0, 182, 22, a(NAVY1, 200), a(NAVY0, 230));                 // hotbar
        for (int i = 0; i < 9; i++) {
            fill(img, (3 + i * 20) * K, 3 * K, 16 * K, 16 * K, a(NAVY0, 120));
            if (i > 0) fill(img, (1 + i * 20) * K, 4 * K, 1, 14 * K, a(BLUE, 90));
        }
        int X = 0, Y = 22 * K, W = 24 * K, H = 22 * K, th = K + 1;            // selection (the game draws 24x22 of it)
        for (int i = 0; i < th; i++) {
            fill(img, X + i + K, Y + i, W - 2 * (i + K), 1, CYAN);
            fill(img, X + i + K, Y + H - 1 - i, W - 2 * (i + K), 1, CYAN);
            fill(img, X + i, Y + i + K, 1, H - 2 * (i + K), CYAN);
            fill(img, X + W - 1 - i, Y + i + K, 1, H - 2 * (i + K), CYAN);
        }
        button(img, 46, a(NAVY1, 200), a(NAVY0, 200), a(NAVY3, 255), a(WHITE, 15));   // disabled
        button(img, 66, 0xEB182456, 0xEB0D1434, 0xFF3050BE, a(WHITE, 40));             // normal
        button(img, 86, 0xF53468FF, 0xF51E46DC, CYAN, a(WHITE, 90));                   // hover
        for (int y : new int[]{106, 126}) {                                             // language button: globe
            panel(img, 0, y, 20, 20, 0xF0141E4A, y == 106 ? 0xFF3050BE : CYAN);
            Graphics2D g = img.createGraphics();
            g.setColor(new Color(ICE, true));
            g.setStroke(new BasicStroke(K));
            int cx = 10 * K, cy = (y + 10) * K, r = 6 * K;
            g.drawOval(cx - r, cy - r, 2 * r - 1, 2 * r - 1);
            g.setStroke(new BasicStroke(1));
            g.drawLine(cx - r, cy, cx + r - 1, cy);
            g.drawOval(cx - r / 2, cy - r, r - 1, 2 * r - 1);
            g.dispose();
        }
        return img;
    }

    private static void button(BufferedImage img, int y, int top, int bottom, int edge, int sheen) {
        panel(img, 0, y, 200, 20, top, edge);
        for (int i = 1; i < 19 * K; i++) fill(img, K, y * K + K + i - 1, 200 * K - 2 * K, 1, mix(top, bottom, i / (19f * K)));
        fill(img, 2 * K, y * K + K, 196 * K, 1, sheen);
    }

    // ---------------------------------------------------------------- vanilla screen background

    static BufferedImage optionsBackground() {
        int n = 16 * K;
        BufferedImage img = new BufferedImage(n, n, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) img.setRGB(x, y, (x + y * 3) % 7 == 0 ? 0xFF121A3C : 0xFF0E1430);
        img.setRGB(n / 2, n / 2, 0xFF283C82);
        return img;
    }

    // ---------------------------------------------------------------- containers, rebuilt from vanilla

    /**
     * Repaints a vanilla container texture: slots (dark top-left / white bottom-right boxes) are redrawn as Valor slots,
     * the grey panel and its bevel become navy with a blue edge, and grey glyphs (arrows, frames) turn blue. Coloured
     * sprites (flames, enchanting glyphs, potion icons) are kept.
     */
    static BufferedImage container(BufferedImage vanilla) {
        int w = vanilla.getWidth(), h = vanilla.getHeight();
        List<int[]> slots = findSlots(vanilla);
        boolean[][] inSlot = new boolean[h][w];
        for (int[] s : slots) for (int y = s[1]; y < s[1] + s[3]; y++) for (int x = s[0]; x < s[0] + s[2]; x++) inSlot[y][x] = true;
        BufferedImage out = new BufferedImage(w * K, h * K, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                if (inSlot[y][x]) continue;
                int c = vanilla.getRGB(x, y);
                if ((c >>> 24) == 0) continue;
                fill(out, x * K, y * K, K, K, recolor(c));
            }
        for (int[] s : slots) slot(out, s[0], s[1], s[2], s[3]);
        return out;
    }

    private static int recolor(int c) {
        int r = c >> 16 & 255, g = c >> 8 & 255, b = c & 255, alpha = c >>> 24;
        if (r != g || g != b) return c;                 // coloured sprite: keep
        int mapped;
        switch (r) {
            case 198: mapped = PANEL; break;            // panel fill
            case 255: mapped = 0xFF2648B0; break;       // bevel highlight
            case 85: mapped = NAVY2; break;             // bevel shadow
            case 0: mapped = NAVY0; break;              // outline / dark boxes
            case 139: mapped = BLUE_HI; break;          // arrows, glyphs
            case 55: mapped = NAVY3; break;
            default: mapped = mix(NAVY2, ICE, (float) Math.pow(r / 255.0, 1.6));
        }
        return a(mapped, Math.min(alpha, mapped >>> 24));
    }

    /** Slot boxes: a dark (55,55,55) top and left edge meeting a white bottom-right corner. Returns {x, y, w, h}. */
    private static List<int[]> findSlots(BufferedImage im) {
        int W = im.getWidth(), H = im.getHeight();
        List<int[]> found = new ArrayList<>();
        for (int y = 0; y < H - 1; y++)
            for (int x = 0; x < W - 1; x++) {
                if (!dark(im, x, y) || (x > 0 && dark(im, x - 1, y)) || (y > 0 && dark(im, x, y - 1))) continue;
                int w = 0, h = 0;
                while (x + w < W && dark(im, x + w, y)) w++;
                while (y + h < H && dark(im, x, y + h)) h++;
                if (w >= 16 && h >= 16 && x + w < W && y + h < H && (im.getRGB(x + w, y + h) & 0xFFFFFF) == 0xFFFFFF)
                    found.add(new int[]{x, y, w + 1, h + 1});
            }
        return found;
    }

    private static boolean dark(BufferedImage im, int x, int y) {
        int c = im.getRGB(x, y);
        return (c >>> 24) == 255 && (c & 0xFFFFFF) == 0x373737;
    }

    private static BufferedImage upscale(BufferedImage src) {
        BufferedImage out = new BufferedImage(src.getWidth() * K, src.getHeight() * K, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < src.getHeight(); y++) for (int x = 0; x < src.getWidth(); x++) fill(out, x * K, y * K, K, K, src.getRGB(x, y));
        return out;
    }

    /** Reads a texture from the game's own default resources (never from resource packs). */
    static BufferedImage vanilla(String path) {
        try (InputStream in = Minecraft.getMinecraft().mcDefaultResourcePack.getInputStream(new ResourceLocation(path))) {
            BufferedImage raw = ImageIO.read(in), argb = new BufferedImage(raw.getWidth(), raw.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = argb.createGraphics();
            g.drawImage(raw, 0, 0, null);
            g.dispose();
            return argb;
        } catch (Exception e) {
            return null;
        }
    }
}
