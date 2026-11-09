package com.vaelohq.valor.module;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayDeque;

/** Mouse clicks of the last second in game, for Keystrokes and the CPS counter. Always registered. */
public final class Clicks {
    private static final ArrayDeque<Long> LEFT = new ArrayDeque<>(), RIGHT = new ArrayDeque<>();

    @SubscribeEvent
    public void onMouse(MouseEvent e) {
        if (!e.buttonstate) return;
        if (e.button == 0) LEFT.add(Minecraft.getSystemTime());
        else if (e.button == 1) RIGHT.add(Minecraft.getSystemTime());
    }

    public static int left() {
        return count(LEFT);
    }

    public static int right() {
        return count(RIGHT);
    }

    private static int count(ArrayDeque<Long> clicks) {
        long cutoff = Minecraft.getSystemTime() - 1000;
        while (!clicks.isEmpty() && clicks.peekFirst() < cutoff) clicks.pollFirst();
        return clicks.size();
    }
}
