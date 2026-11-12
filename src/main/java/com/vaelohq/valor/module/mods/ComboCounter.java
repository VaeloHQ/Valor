package com.vaelohq.valor.module.mods;

import com.vaelohq.valor.module.Setting;
import com.vaelohq.valor.module.TextHud;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Hits landed in a row: a hit counts when the entity you attacked shows its hurt animation; taking a hit or 3 seconds without one resets it. */
public final class ComboCounter extends TextHud {
    private final Setting.Toggle hideZero = add(new Setting.Toggle("hideZero", "Hide when 0", false));
    private int combo, target = -1;
    private long attackedAt, hitAt;

    public ComboCounter() {
        super("combo", "Combo Counter", "Hits landed in a row without being hit.", Category.PVP, 0, 0.127f);
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent e) {
        if (e.entityPlayer != mc.thePlayer) return;
        target = e.target.getEntityId();
        attackedAt = Minecraft.getSystemTime();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (mc.thePlayer == null || mc.theWorld == null) {
            combo = 0;
            return;
        }
        long now = Minecraft.getSystemTime();
        if (mc.thePlayer.maxHurtTime > 0 && mc.thePlayer.hurtTime >= mc.thePlayer.maxHurtTime - 1) combo = 0;
        Entity t = target == -1 ? null : mc.theWorld.getEntityByID(target);
        if (t instanceof EntityLivingBase) {
            EntityLivingBase l = (EntityLivingBase) t;
            if (l.maxHurtTime > 0 && l.hurtTime >= l.maxHurtTime - 1 && now - attackedAt < 600) {
                combo++;
                hitAt = now;
                target = -1;
            }
        }
        if (combo > 0 && now - hitAt > 3000) combo = 0;
    }

    @Override
    public boolean visible() {
        return !hideZero.value || combo > 0;
    }

    @Override
    protected String text(boolean preview) {
        int c = preview ? 3 : combo;
        return c + (c == 1 ? " hit" : " combo");
    }
}
