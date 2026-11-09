package com.vaelohq.valor.style;

import com.google.common.collect.ImmutableSet;
import com.vaelohq.valor.Config;
import com.vaelohq.valor.Valor;
import com.vaelohq.valor.mixin.MinecraftAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.IMetadataSerializer;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Built-in texture layer that serves Valor's generated GUI and HUD textures. It sits with the default resources, so
 * any resource pack the player selects still overrides it; turning Valor style off makes it serve nothing.
 */
public final class ValorPack implements IResourcePack {
    public static final ValorPack INSTANCE = new ValorPack();
    private static final String NAME = "Valor style";
    private final Map<String, byte[]> cache = new ConcurrentHashMap<>();
    private final Map<String, Boolean> exists = new ConcurrentHashMap<>();
    private static volatile boolean containersShown;

    /** Adds the layer to the game's default resources. Call before resources first load (pre-init). */
    public static void register() {
        ((MinecraftAccessor) Minecraft.getMinecraft()).valor$defaultResourcePacks().add(INSTANCE);
    }

    /** Watches resource reloads to know whether Valor's container art is what's actually showing. */
    public static void watchReloads() {
        ((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).registerReloadListener(rm -> {
            try {
                IResource r = rm.getResource(new ResourceLocation("textures/gui/container/inventory.png"));
                containersShown = NAME.equals(r.getResourcePackName());
                r.getInputStream().close();
            } catch (Exception e) {
                containersShown = false;
            }
        });
    }

    /** True when Valor's dark container panels are on screen (Valor style on and not overridden by a resource pack). */
    public static boolean containersShown() {
        return containersShown;
    }

    public static void setEnabled(boolean on) {
        Config.valorStyle = on;
        Config.save();
        Minecraft.getMinecraft().refreshResources();
    }

    private boolean serves(String path) {
        if (path.equals("textures/gui/icons.png") || path.equals("textures/gui/widgets.png") || path.equals("textures/gui/options_background.png"))
            return true;
        return (path.startsWith("textures/gui/container/") || path.startsWith("textures/gui/achievement/")) && path.endsWith(".png")
                && exists.computeIfAbsent(path, p -> ValorTextures.vanilla(p) != null);
    }

    @Override
    public boolean resourceExists(ResourceLocation loc) {
        return Config.valorStyle && "minecraft".equals(loc.getResourceDomain()) && serves(loc.getResourcePath());
    }

    @Override
    public InputStream getInputStream(ResourceLocation loc) throws FileNotFoundException {
        if (!resourceExists(loc)) throw new FileNotFoundException(loc.toString());
        byte[] data = cache.computeIfAbsent(loc.getResourcePath(), ValorPack::generate);
        if (data == null) throw new FileNotFoundException(loc.toString());
        return new ByteArrayInputStream(data);
    }

    private static byte[] generate(String path) {
        try {
            BufferedImage img;
            if (path.equals("textures/gui/icons.png")) img = ValorTextures.icons();
            else if (path.equals("textures/gui/widgets.png")) img = ValorTextures.widgets(ValorTextures.vanilla(path));
            else if (path.equals("textures/gui/options_background.png")) img = ValorTextures.optionsBackground();
            else img = ValorTextures.container(ValorTextures.vanilla(path));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, "png", out);
            return out.toByteArray();
        } catch (Exception e) {
            Valor.LOGGER.error("Couldn't generate {}", path, e);
            return null;
        }
    }

    @Override
    public Set<String> getResourceDomains() {
        return ImmutableSet.of("minecraft");
    }

    @Override
    public <T extends IMetadataSection> T getPackMetadata(IMetadataSerializer serializer, String section) {
        return null;
    }

    @Override
    public BufferedImage getPackImage() {
        return null;
    }

    @Override
    public String getPackName() {
        return NAME;
    }
}
