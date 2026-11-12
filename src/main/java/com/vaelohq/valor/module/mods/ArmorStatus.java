package com.vaelohq.valor.module.mods;

import com.vaelohq.valor.module.HudModule;
import com.vaelohq.valor.module.Setting;
import com.vaelohq.valor.ui.Ui;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Your armour (helmet at the top) and held item, with how much durability each has left. */
public final class ArmorStatus extends HudModule {
    private static final float ROW = 18;
    private final Setting.Choice show = add(new Setting.Choice("show", "Durability as", 0, "Uses left", "Percent", "Nothing"));
    private final Setting.Toggle held = add(new Setting.Toggle("held", "Held item", true));

    public ArmorStatus() {
        super("armor", "Armor Status", "Your armour and held item with durability.", Category.PVP, 1, 0.279f);
    }

    private List<ItemStack> items(boolean preview) {
        List<ItemStack> list = new ArrayList<>();
        if (preview || mc.thePlayer == null) {
            list.add(damaged(new ItemStack(Items.diamond_helmet), 0.1f));
            list.add(damaged(new ItemStack(Items.diamond_chestplate), 0.35f));
            list.add(damaged(new ItemStack(Items.diamond_leggings), 0.6f));
            list.add(damaged(new ItemStack(Items.diamond_boots), 0.85f));
            if (held.value) list.add(new ItemStack(Items.diamond_sword));
            return list;
        }
        for (int i = 3; i >= 0; i--) if (mc.thePlayer.inventory.armorInventory[i] != null) list.add(mc.thePlayer.inventory.armorInventory[i]);
        if (held.value && mc.thePlayer.getHeldItem() != null) list.add(mc.thePlayer.getHeldItem());
        return list;
    }

    private static ItemStack damaged(ItemStack s, float used) {
        s.setItemDamage(Math.round(s.getMaxDamage() * used));
        return s;
    }

    private String label(ItemStack s) {
        if (show.index == 2) return "";
        if (!s.isItemStackDamageable()) return s.stackSize > 1 ? String.valueOf(s.stackSize) : "";
        int left = s.getMaxDamage() - s.getItemDamage();
        return show.index == 0 ? String.valueOf(left) : left * 100 / s.getMaxDamage() + "%";
    }

    @Override
    public boolean visible() {
        return !items(false).isEmpty();
    }

    @Override
    public float width(boolean preview) {
        float w = 0;
        for (ItemStack s : items(preview)) w = Math.max(w, textWidth(label(s), 6));
        return 16 + 8 + (w > 0 ? w + 4 : 0);
    }

    @Override
    public float height(boolean preview) {
        return Math.max(ROW, items(preview).size() * ROW) + 2;
    }

    @Override
    public void draw(boolean preview) {
        List<ItemStack> list = items(preview);
        panel(width(preview), height(preview));
        for (int i = 0; i < list.size(); i++) {
            ItemStack s = list.get(i);
            float y = 1 + i * ROW;
            Ui.item(s, 4, y + 1, 1);
            String l = label(s);
            if (l.isEmpty()) continue;
            int color = 0xFFF4F7FF;
            if (s.isItemStackDamageable()) {
                float left = 1 - (float) s.getItemDamage() / s.getMaxDamage();
                color = left > 0.5f ? Ui.SUCCESS : left > 0.2f ? 0xFFFFD84A : Ui.DANGER;
            }
            Ui.semibold().draw(l, 24, y + (ROW - 7.2f) / 2, 6, color);
        }
    }
}
