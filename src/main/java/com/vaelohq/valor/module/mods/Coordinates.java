package com.vaelohq.valor.module.mods;

import com.vaelohq.valor.module.HudModule;
import com.vaelohq.valor.module.Setting;
import com.vaelohq.valor.ui.Ui;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;

/** Your position, and optionally the way you're facing and the biome. */
public final class Coordinates extends HudModule {
    private static final float LINE = 9, SIZE = 6;
    private static final String[] FACING = {"South", "West", "North", "East"};
    private final Setting.Choice layout = add(new Setting.Choice("layout", "Layout", 0, "Lines", "One line"));
    private final Setting.Toggle facing = add(new Setting.Toggle("facing", "Facing", true));
    private final Setting.Toggle biome = add(new Setting.Toggle("biome", "Biome", false));

    public Coordinates() {
        super("coords", "Coordinates", "Where you are in the world.", Category.HUD, 0, 0.641f);
    }

    @Override
    public boolean visible() {
        return mc.thePlayer != null;
    }

    private String[] lines(boolean preview) {
        int x = 128, y = 64, z = -45;
        String face = "North", bio = "Plains";
        if (!preview && mc.thePlayer != null) {
            x = MathHelper.floor_double(mc.thePlayer.posX);
            y = MathHelper.floor_double(mc.thePlayer.getEntityBoundingBox().minY);
            z = MathHelper.floor_double(mc.thePlayer.posZ);
            face = FACING[MathHelper.floor_double(mc.thePlayer.rotationYaw * 4 / 360 + 0.5) & 3];
            bio = mc.theWorld.getBiomeGenForCoords(new BlockPos(x, y, z)).biomeName;
        }
        java.util.List<String> l = new java.util.ArrayList<>();
        if (layout.index == 1) l.add("XYZ  " + x + ", " + y + ", " + z);
        else {
            l.add("X  " + x);
            l.add("Y  " + y);
            l.add("Z  " + z);
        }
        if (facing.value) l.add(face);
        if (biome.value) l.add(bio);
        return l.toArray(new String[0]);
    }

    @Override
    public float width(boolean preview) {
        float w = 0;
        for (String s : lines(preview)) w = Math.max(w, textWidth(s, SIZE));
        return w + 10;
    }

    @Override
    public float height(boolean preview) {
        return lines(preview).length * LINE + 5;
    }

    @Override
    public void draw(boolean preview) {
        String[] l = lines(preview);
        panel(width(preview), height(preview));
        for (int i = 0; i < l.length; i++) Ui.semibold().draw(l[i], 5, 3 + i * LINE, SIZE, i >= (layout.index == 1 ? 1 : 3) ? 0xFF9AA6C4 : 0xFFF4F7FF);
    }
}
