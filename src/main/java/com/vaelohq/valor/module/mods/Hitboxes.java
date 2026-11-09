package com.vaelohq.valor.module.mods;

import com.vaelohq.valor.module.Module;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Shows entity hitboxes (vanilla's F3 + B) while on. */
public final class Hitboxes extends Module {
    public Hitboxes() {
        super("hitboxes", "Hitboxes", "Outlines around players, mobs and items.", Category.PVP);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase == TickEvent.Phase.END && mc.getRenderManager() != null && !mc.getRenderManager().isDebugBoundingBox())
            mc.getRenderManager().setDebugBoundingBox(true);
    }

    @Override
    protected void onDisable() {
        if (mc.getRenderManager() != null) mc.getRenderManager().setDebugBoundingBox(false);
    }
}
