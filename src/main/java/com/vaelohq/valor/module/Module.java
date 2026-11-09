package com.vaelohq.valor.module;

import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;

import java.util.ArrayList;
import java.util.List;

/**
 * A feature the player can turn on and off in the mods menu. While on it receives Forge events (its @SubscribeEvent
 * methods); its settings are saved with it.
 */
public abstract class Module {
    public enum Category {
        PVP("PvP"), HUD("HUD"), UTILITY("Utility");
        public final String label;

        Category(String label) {
            this.label = label;
        }
    }

    protected static final Minecraft mc = Minecraft.getMinecraft();
    public final String id, name, description;
    public final Category category;
    public final List<Setting> settings = new ArrayList<>();
    private boolean enabled;

    protected Module(String id, String name, String description, Category category) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
    }

    protected <S extends Setting> S add(S setting) {
        settings.add(setting);
        return setting;
    }

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean on) {
        if (on == enabled) return;
        enabled = on;
        if (on) {
            MinecraftForge.EVENT_BUS.register(this);
            onEnable();
        } else {
            MinecraftForge.EVENT_BUS.unregister(this);
            onDisable();
        }
    }

    protected void onEnable() {
    }

    protected void onDisable() {
    }
}
