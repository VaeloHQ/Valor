package com.vaelohq.valor.module.mods;

import com.vaelohq.valor.module.TextHud;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Distance from your eyes to where your last hit landed on the target's hitbox. */
public final class ReachDisplay extends TextHud {
    private double reach = -1;

    public ReachDisplay() {
        super("reach", "Reach Display", "How far away your last hit landed.", Category.PVP, 0, 0.191f);
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent e) {
        MovingObjectPosition hit = mc.objectMouseOver;
        if (e.entityPlayer != mc.thePlayer || hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.ENTITY || hit.entityHit != e.target)
            return;
        reach = mc.thePlayer.getPositionEyes(1).distanceTo(hit.hitVec);
    }

    @Override
    protected String text(boolean preview) {
        if (preview) return "3.02 blocks";
        return reach < 0 ? "- blocks" : String.format("%.2f blocks", reach);
    }
}
