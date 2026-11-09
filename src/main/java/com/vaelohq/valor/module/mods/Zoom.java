package com.vaelohq.valor.module.mods;

import com.vaelohq.valor.module.Module;
import com.vaelohq.valor.module.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

/** Hold the zoom key (C by default) to zoom in; scroll while zooming to change how far. */
public final class Zoom extends Module {
    public final KeyBinding key = new KeyBinding("Zoom", Keyboard.KEY_C, "Valor");
    private static Zoom instance;
    private final Setting.Slider amount = add(new Setting.Slider("amount", "Zoom", 4, 2, 10, 0.5f, "x"));
    private final Setting.Toggle smooth = add(new Setting.Toggle("smooth", "Smooth zoom", true));
    private final Setting.Toggle scroll = add(new Setting.Toggle("scroll", "Scroll to adjust", true));
    private final Setting.Toggle cinematic = add(new Setting.Toggle("cinematic", "Smooth camera while zoomed", false));
    private float current = 1, extra = 1;
    private long last;
    private boolean cameraWasSmooth, zooming;

    public Zoom() {
        super("zoom", "Zoom", "Hold a key to zoom in like a spyglass.", Category.PVP);
        instance = this;
    }

    private boolean held() {
        return key.isKeyDown() && mc.currentScreen == null;
    }

    /** How much to divide the field of view by right now (called from MixinEntityRenderer every frame). */
    public static float divisor() {
        Zoom z = instance;
        if (z == null || !z.enabled()) return 1;
        long now = Minecraft.getSystemTime();
        float dt = z.last == 0 ? 0 : Math.min(0.1f, (now - z.last) / 1000f);
        z.last = now;
        float target = z.held() ? z.amount.value * z.extra : 1;
        z.current = z.smooth.value ? z.current + (target - z.current) * (1 - (float) Math.exp(-dt * 14)) : target;
        return z.current;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        boolean now = held();
        if (now && !zooming) {
            extra = 1;
            cameraWasSmooth = mc.gameSettings.smoothCamera;
            if (cinematic.value) mc.gameSettings.smoothCamera = true;
        } else if (!now && zooming && cinematic.value) mc.gameSettings.smoothCamera = cameraWasSmooth;
        zooming = now;
    }

    @SubscribeEvent
    public void onMouse(MouseEvent e) {
        if (!zooming || !scroll.value || e.dwheel == 0) return;
        extra = Math.max(0.5f, Math.min(4, extra * (e.dwheel > 0 ? 1.25f : 0.8f)));
        e.setCanceled(true);   // don't scroll the hotbar while zooming
    }

    @Override
    protected void onDisable() {
        if (zooming && cinematic.value) mc.gameSettings.smoothCamera = cameraWasSmooth;
        zooming = false;
        current = 1;
    }
}
