package com.vaelohq.valor.ui;

import com.vaelohq.valor.Valor;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.jcodec.api.FrameGrab;
import org.jcodec.api.PictureWithMetadata;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.scale.AWTUtil;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.w3c.dom.Node;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.IntBuffer;
import java.util.Locale;

/**
 * A wallpaper file: a still picture (PNG, JPG), an animated GIF, or an MP4 / MOV video (H.264). Animations are decoded
 * on a background thread that keeps their timing; the game thread only uploads the newest frame into one texture.
 * Everything loops.
 */
final class WallpaperMedia {
    /** Frames wider than this are scaled down before upload, to keep texture uploads cheap. */
    private static final int MAX_WIDTH = 1920;

    final File file;
    private final Thread decoder;
    private volatile Frame pending;
    private volatile boolean closed, failed;
    private int tex = -1, tw, th;

    /** Pixels ready to upload (ARGB ints in the order GL_BGRA reads them). */
    private static final class Frame {
        final int w, h;
        final IntBuffer pixels;

        Frame(BufferedImage img) {
            w = img.getWidth();
            h = img.getHeight();
            pixels = BufferUtils.createIntBuffer(w * h);
            pixels.put(img.getRGB(0, 0, w, h, null, 0, w)).flip();
        }
    }

    WallpaperMedia(File file) {
        this.file = file;
        decoder = new Thread(this::decode, "Valor wallpaper");
        decoder.setDaemon(true);
        decoder.start();
    }

    static boolean supported(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        return n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".gif") || isVideo(n);
    }

    private static boolean isVideo(String lowerName) {
        return lowerName.endsWith(".mp4") || lowerName.endsWith(".m4v") || lowerName.endsWith(".mov");
    }

    boolean failed() {
        return failed;
    }

    private void decode() {
        try {
            String n = file.getName().toLowerCase(Locale.ROOT);
            if (isVideo(n)) video();
            else if (n.endsWith(".gif")) gif();
            else {
                BufferedImage img = ImageIO.read(file);
                if (img == null) throw new IllegalArgumentException("not a picture");
                pending = new Frame(fit(img));
            }
        } catch (InterruptedException ignored) {
            // closed
        } catch (Throwable t) {
            Valor.LOGGER.warn("Couldn't play wallpaper {}", file, t);
            failed = true;
        }
    }

    /** Plays an MP4 / MOV with JCodec, reopening it at the end to loop. */
    private void video() throws Exception {
        while (!closed) {
            int shown = 0;
            try (SeekableByteChannel ch = NIOUtils.readableChannel(file)) {
                FrameGrab grab = FrameGrab.createFrameGrab(ch);
                PictureWithMetadata p;
                long next = System.nanoTime();
                while (!closed && (p = grab.getNativeFrameWithMetadata()) != null) {
                    double d = p.getDuration();
                    long frameNanos = (long) ((d > 0 && d < 1 ? d : 1 / 30.0) * 1e9);
                    // Too slow to keep up (H.264 can't skip decoding): drop late frames instead of playing in slow motion.
                    boolean late = shown > 0 && System.nanoTime() > next + frameNanos;
                    if (!late) {
                        Frame f = new Frame(fit(AWTUtil.toBufferedImage(p.getPicture())));
                        sleepUntil(next);
                        pending = f;
                    }
                    next += frameNanos;
                    shown++;
                }
            }
            if (shown == 0) throw new IllegalArgumentException("no video frames (only H.264 MP4/MOV is supported)");
        }
    }

    /** Plays a GIF, compositing each frame onto the canvas with its offset and disposal, as browsers do. */
    private void gif() throws Exception {
        ImageReader reader = ImageIO.getImageReadersByFormatName("gif").next();
        try (ImageInputStream in = ImageIO.createImageInputStream(file)) {
            reader.setInput(in, false);
            int count = reader.getNumImages(true);
            Node screen = child(reader.getStreamMetadata().getAsTree("javax_imageio_gif_stream_1.0"), "LogicalScreenDescriptor");
            int sw = screen != null ? attr(screen, "logicalScreenWidth", 0) : 0, sh = screen != null ? attr(screen, "logicalScreenHeight", 0) : 0;
            if (sw <= 0 || sh <= 0) {
                BufferedImage first = reader.read(0);
                sw = first.getWidth();
                sh = first.getHeight();
            }
            BufferedImage canvas = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = canvas.createGraphics();
            long next = System.nanoTime();
            while (!closed) {
                g.setComposite(AlphaComposite.Clear);
                g.fillRect(0, 0, sw, sh);
                for (int i = 0; i < count && !closed; i++) {
                    BufferedImage img = reader.read(i);
                    Node meta = reader.getImageMetadata(i).getAsTree("javax_imageio_gif_image_1.0");
                    Node desc = child(meta, "ImageDescriptor"), ctl = child(meta, "GraphicControlExtension");
                    int x = desc != null ? attr(desc, "imageLeftPosition", 0) : 0, y = desc != null ? attr(desc, "imageTopPosition", 0) : 0;
                    String disposal = ctl != null ? ((IIOMetadataNode) ctl).getAttribute("disposalMethod") : "none";
                    int delay = ctl != null ? attr(ctl, "delayTime", 10) : 10;
                    BufferedImage before = "restoreToPrevious".equals(disposal) ? copy(canvas) : null;
                    g.setComposite(AlphaComposite.SrcOver);
                    g.drawImage(img, x, y, null);
                    Frame f = new Frame(fit(canvas));
                    sleepUntil(next);
                    pending = f;
                    next += (delay <= 1 ? 100 : delay * 10) * 1_000_000L;   // browsers treat 0-1 hundredths as 100 ms
                    next = Math.max(next, System.nanoTime() - 200_000_000L);
                    if ("restoreToBackgroundColor".equals(disposal)) {
                        g.setComposite(AlphaComposite.Clear);
                        g.fillRect(x, y, img.getWidth(), img.getHeight());
                    } else if (before != null) {
                        g.setComposite(AlphaComposite.Src);
                        g.drawImage(before, 0, 0, null);
                    }
                }
                if (count <= 1) return;   // a single-frame GIF is a still picture
            }
        } finally {
            reader.dispose();
        }
    }

    private static void sleepUntil(long nanoTime) throws InterruptedException {
        long ms = (nanoTime - System.nanoTime()) / 1_000_000L;
        if (ms > 0) Thread.sleep(ms);
    }

    private static Node child(Node parent, String name) {
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) if (name.equals(n.getNodeName())) return n;
        return null;
    }

    private static int attr(Node node, String name, int fallback) {
        try {
            return Integer.parseInt(((IIOMetadataNode) node).getAttribute(name));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static BufferedImage copy(BufferedImage img) {
        BufferedImage c = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
        c.getGraphics().drawImage(img, 0, 0, null);
        return c;
    }

    /** Scales frames wider than MAX_WIDTH down (keeping the aspect ratio). */
    static BufferedImage fit(BufferedImage img) {
        return scale(img, MAX_WIDTH);
    }

    static BufferedImage scale(BufferedImage img, int maxWidth) {
        if (img.getWidth() <= maxWidth) return img;
        int w = maxWidth, h = Math.max(1, img.getHeight() * maxWidth / img.getWidth());
        BufferedImage s = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = s.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img, 0, 0, w, h, null);
        g.dispose();
        return s;
    }

    /** Draws the newest frame to cover the screen (cropping, not stretching). False until the first frame is ready. */
    boolean draw(float w, float h) {
        Frame f = pending;
        if (f != null) {
            pending = null;
            upload(f);
        }
        if (tex == -1) return false;
        float sa = w / h, ia = (float) tw / th, u0 = 0, v0 = 0, u1 = 1, v1 = 1;
        if (ia > sa) {
            u0 = (1 - sa / ia) / 2;
            u1 = 1 - u0;
        } else {
            v0 = (1 - ia / sa) / 2;
            v1 = 1 - v0;
        }
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.bindTexture(tex);
        WorldRenderer wr = Tessellator.getInstance().getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        wr.pos(0, h, 0).tex(u0, v1).endVertex();
        wr.pos(w, h, 0).tex(u1, v1).endVertex();
        wr.pos(w, 0, 0).tex(u1, v0).endVertex();
        wr.pos(0, 0, 0).tex(u0, v0).endVertex();
        Tessellator.getInstance().draw();
        GlStateManager.enableBlend();
        Ui.gradient(0, 0, w, h, 0x33000000, 0x8C000000);   // keeps white text readable on bright pictures
        return true;
    }

    private void upload(Frame f) {
        if (tex == -1 || f.w != tw || f.h != th) {
            if (tex != -1) TextureUtil.deleteTexture(tex);
            tex = GL11.glGenTextures();
            tw = f.w;
            th = f.h;
            GlStateManager.bindTexture(tex);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, tw, th, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, f.pixels);
        } else {
            GlStateManager.bindTexture(tex);
            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, tw, th, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, f.pixels);
        }
    }

    void close() {
        closed = true;
        decoder.interrupt();
        if (tex != -1) TextureUtil.deleteTexture(tex);
        tex = -1;
        pending = null;
    }

    /** First frame of a wallpaper file, scaled to width w, for the picker (null when unreadable). Runs off the game thread. */
    static BufferedImage thumbnail(File file, int w) {
        try {
            String n = file.getName().toLowerCase(Locale.ROOT);
            BufferedImage img;
            if (isVideo(n)) {
                try (SeekableByteChannel ch = NIOUtils.readableChannel(file)) {
                    org.jcodec.common.model.Picture p = FrameGrab.createFrameGrab(ch).getNativeFrame();
                    img = p == null ? null : AWTUtil.toBufferedImage(p);
                }
            } else img = ImageIO.read(file);   // GIF: first frame
            return img == null ? null : scale(img, w);
        } catch (Throwable t) {
            Valor.LOGGER.warn("Couldn't read wallpaper {}", file, t);
            return null;
        }
    }
}
