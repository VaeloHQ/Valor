package com.vaelohq.valor.ui;

import com.vaelohq.valor.mixin.GuiConfirmOpenLinkAccessor;
import com.vaelohq.valor.mixin.GuiCreateWorldAccessor;
import com.vaelohq.valor.mixin.GuiErrorScreenAccessor;
import com.vaelohq.valor.mixin.GuiMultiplayerAccessor;
import com.vaelohq.valor.mixin.GuiOptionsAccessor;
import com.vaelohq.valor.mixin.GuiSelectWorldAccessor;
import com.vaelohq.valor.mixin.GuiYesNoAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;

import java.lang.reflect.Field;

/** Decides which Valor screen replaces a vanilla screen that is about to open. */
public final class ScreenSwap {
    private static boolean introShown;

    private ScreenSwap() {
    }

    /** The Valor replacement for gui, or gui itself when Valor leaves it alone. */
    public static GuiScreen replace(GuiScreen gui) {
        Class<?> type = gui.getClass();
        if (gui instanceof GuiMainMenu) {
            // First title screen of the launch: intro, then the account panel, then the menu.
            GuiScreen s = introShown ? new MainMenuScreen() : new IntroScreen();
            introShown = true;
            return s;
        }
        if (gui instanceof GuiIngameMenu) return new PauseScreen();
        if (gui instanceof GuiGameOver) return new DeathScreen();
        if (gui instanceof GuiMultiplayer) return new MultiplayerScreen(((GuiMultiplayerAccessor) gui).valor$parent());
        if (gui instanceof GuiSelectWorld) {
            // The world list hands FML a GuiSelectWorld wrapping itself; showing that again means going back to it.
            GuiScreen parent = ((GuiSelectWorldAccessor) gui).valor$parent();
            return parent instanceof SingleplayerScreen ? parent : new SingleplayerScreen(parent);
        }
        if (gui instanceof GuiCreateWorld) {
            CreateWorldScreen back = CreateWorldScreen.returnFrom((GuiCreateWorld) gui);
            return back != null ? back : new CreateWorldScreen(((GuiCreateWorldAccessor) gui).valor$parent());
        }
        if (gui instanceof GuiOptions) return new OptionsScreen(((GuiOptionsAccessor) gui).valor$parent());
        if (gui instanceof GuiVideoSettings) return new OptionsScreen(parentOf(gui), OptionsScreen.Category.VIDEO);
        if (gui instanceof GuiControls) return new OptionsScreen(parentOf(gui), OptionsScreen.Category.CONTROLS);
        if (gui instanceof GuiScreenOptionsSounds) return new OptionsScreen(parentOf(gui), OptionsScreen.Category.SOUND);
        if (gui instanceof ScreenChatOptions) return new OptionsScreen(parentOf(gui), OptionsScreen.Category.CHAT);
        if (gui instanceof GuiCustomizeSkin) return new OptionsScreen(parentOf(gui), OptionsScreen.Category.SKIN);
        if (gui instanceof GuiLanguage) return new OptionsScreen(parentOf(gui), OptionsScreen.Category.LANGUAGE);
        if (gui instanceof GuiSnooper) return new OptionsScreen(parentOf(gui), OptionsScreen.Category.GENERAL);
        if (gui instanceof GuiShareToLan) return new LanScreen(parentOf(gui));
        if (gui instanceof GuiScreenResourcePacks) return new ResourcePacksScreen(parentOf(gui));
        // Only these exact prompts: subclasses such as Forge's old-save warning draw their own text.
        if (type == GuiYesNo.class || type == GuiConfirmOpenLink.class) return confirm((GuiYesNo) gui);
        if (gui instanceof GuiErrorScreen) {
            GuiErrorScreenAccessor e = (GuiErrorScreenAccessor) gui;
            return new ConfirmScreen(null, e.valor$title(), e.valor$message(), "Back", null, false,
                    ok -> Minecraft.getMinecraft().displayGuiScreen(null));
        }
        return gui;
    }

    /** A vanilla yes/no prompt as a Valor dialog that answers the same callback with the same id. */
    private static GuiScreen confirm(GuiYesNo gui) {
        GuiYesNoAccessor y = (GuiYesNoAccessor) gui;
        GuiYesNoCallback callback = y.valor$callback();
        int id = y.valor$id();
        String message = y.valor$message() == null ? "" : y.valor$message();
        GuiScreen behind = callback instanceof GuiScreen ? (GuiScreen) callback : null;
        ConfirmScreen c;
        if (gui instanceof GuiConfirmOpenLink) {
            GuiConfirmOpenLinkAccessor l = (GuiConfirmOpenLinkAccessor) gui;
            if (l.valor$showWarning()) message += "\n" + l.valor$warning();
            c = new ConfirmScreen(behind, y.valor$title(), message, y.valor$yes(), y.valor$no(), false, ok -> callback.confirmClicked(ok, id))
                    .extra(l.valor$copy(), ((GuiConfirmOpenLink) gui)::copyLinkToClipboard);
        } else {
            c = new ConfirmScreen(behind, y.valor$title(), message, y.valor$yes(), y.valor$no(), false, ok -> callback.confirmClicked(ok, id));
        }
        return c.delay(y.valor$delay());
    }

    /** The screen a simple vanilla screen returns to: its only field of a GuiScreen type. */
    static GuiScreen parentOf(GuiScreen gui) {
        for (Class<?> c = gui.getClass(); c != GuiScreen.class; c = c.getSuperclass())
            for (Field f : c.getDeclaredFields())
                if (GuiScreen.class.isAssignableFrom(f.getType())) {
                    try {
                        f.setAccessible(true);
                        return (GuiScreen) f.get(gui);
                    } catch (IllegalAccessException e) {
                        throw new IllegalStateException(e);
                    }
                }
        return null;
    }
}
