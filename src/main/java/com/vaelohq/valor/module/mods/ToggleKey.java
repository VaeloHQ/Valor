package com.vaelohq.valor.module.mods;

import com.vaelohq.valor.module.TextHud;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** Turns a hold key (sprint, sneak) into a toggle: press it once to keep it held, again to let go. */
abstract class ToggleKey extends TextHud {
    private final String label;
    private boolean toggled;

    ToggleKey(String id, String name, String description, String label, float y) {
        super(id, name, description, Category.PVP, 0, y);
        this.label = label;
    }

    abstract KeyBinding key();

    private static boolean physicallyDown(KeyBinding k) {
        int code = k.getKeyCode();
        return code != 0 && (code < 0 ? Mouse.isButtonDown(code + 100) : Keyboard.isKeyDown(code));
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.START || mc.thePlayer == null) return;
        KeyBinding k = key();
        while (k.isPressed()) toggled = !toggled;
        if (toggled) KeyBinding.setKeyBindState(k.getKeyCode(), true);
    }

    @Override
    protected void onDisable() {
        toggled = false;
        KeyBinding k = key();
        KeyBinding.setKeyBindState(k.getKeyCode(), physicallyDown(k));
    }

    @Override
    public boolean visible() {
        return toggled;
    }

    @Override
    protected String text(boolean preview) {
        return label + " toggled";
    }
}
