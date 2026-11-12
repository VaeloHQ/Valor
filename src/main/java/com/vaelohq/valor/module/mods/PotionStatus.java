package com.vaelohq.valor.module.mods;

import com.vaelohq.valor.module.HudModule;
import com.vaelohq.valor.ui.Ui;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Active potion effects with their icon, level and time left. */
public final class PotionStatus extends HudModule {
    private static final ResourceLocation ICONS = new ResourceLocation("textures/gui/container/inventory.png");
    private static final float ROW = 20;

    public PotionStatus() {
        super("potions", "Potion Status", "Your active effects and how long they last.", Category.PVP, 1, 1);
    }

    private List<PotionEffect> effects(boolean preview) {
        if (preview || mc.thePlayer == null) {
            List<PotionEffect> sample = new ArrayList<>();
            sample.add(new PotionEffect(Potion.moveSpeed.id, 1800, 1));
            sample.add(new PotionEffect(Potion.damageBoost.id, 900, 0));
            return sample;
        }
        Collection<PotionEffect> active = mc.thePlayer.getActivePotionEffects();
        return new ArrayList<>(active);
    }

    private static String name(PotionEffect e) {
        String n = I18n.format(Potion.potionTypes[e.getPotionID()].getName());
        return e.getAmplifier() > 0 && e.getAmplifier() < 10 ? n + " " + I18n.format("enchantment.level." + (e.getAmplifier() + 1)) : n;
    }

    @Override
    public boolean visible() {
        return !effects(false).isEmpty();
    }

    @Override
    public float width(boolean preview) {
        float w = 0;
        for (PotionEffect e : effects(preview)) w = Math.max(w, textWidth(name(e), 6));
        return 6 + 18 + 5 + w + 6;
    }

    @Override
    public float height(boolean preview) {
        return Math.max(ROW, effects(preview).size() * ROW) + 2;
    }

    @Override
    public void draw(boolean preview) {
        List<PotionEffect> list = effects(preview);
        panel(width(preview), height(preview));
        for (int i = 0; i < list.size(); i++) {
            PotionEffect e = list.get(i);
            Potion p = Potion.potionTypes[e.getPotionID()];
            float y = 1 + i * ROW;
            if (p.hasStatusIcon()) {
                int idx = p.getStatusIconIndex();
                mc.getTextureManager().bindTexture(ICONS);
                GlStateManager.enableBlend();
                GlStateManager.color(1, 1, 1, 1);
                GlStateManager.pushMatrix();
                GlStateManager.translate(5, y + 1, 0);
                Gui.drawModalRectWithCustomSizedTexture(0, 0, idx % 8 * 18, 198 + idx / 8 * 18, 18, 18, 256, 256);
                GlStateManager.popMatrix();
            }
            Ui.semibold().draw(name(e), 29, y + 2.5f, 6, 0xFFF4F7FF);
            Ui.regular().draw(Potion.getDurationString(e), 29, y + 10.5f, 5, e.getDuration() < 200 ? Ui.DANGER : 0xFF9AA6C4);
        }
    }
}
