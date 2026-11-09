package com.vaelohq.valor.module.mods;

import com.vaelohq.valor.module.HudModule;
import com.vaelohq.valor.module.Setting;
import com.vaelohq.valor.ui.Ui;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

/** How many arrows you carry. */
public final class ArrowCounter extends HudModule {
    private static final ItemStack ARROW = new ItemStack(Items.arrow);
    private final Setting.Toggle onlyBow = add(new Setting.Toggle("onlyBow", "Only while holding a bow", true));

    public ArrowCounter() {
        super("arrows", "Arrow Counter", "How many arrows you have.", Category.PVP, 0.5f, 0.8f);
    }

    private int count(boolean preview) {
        if (preview || mc.thePlayer == null) return 64;
        int n = 0;
        for (ItemStack s : mc.thePlayer.inventory.mainInventory) if (s != null && s.getItem() == Items.arrow) n += s.stackSize;
        return n;
    }

    @Override
    public boolean visible() {
        if (mc.thePlayer == null) return false;
        ItemStack held = mc.thePlayer.getHeldItem();
        return !onlyBow.value || held != null && held.getItem() == Items.bow;
    }

    @Override
    public float width(boolean preview) {
        return 4 + 16 + 4 + textWidth(String.valueOf(count(preview)), 6) + 6;
    }

    @Override
    public float height(boolean preview) {
        return 20;
    }

    @Override
    public void draw(boolean preview) {
        int n = count(preview);
        panel(width(preview), 20);
        Ui.item(ARROW, 4, 2, 1);
        Ui.semibold().draw(String.valueOf(n), 24, (20 - 7.2f) / 2, 6, n == 0 ? Ui.DANGER : 0xFFF4F7FF);
    }
}
