package com.vaelohq.valor.ui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.storage.WorldInfo;
import org.lwjgl.input.Keyboard;

import java.util.Random;

/**
 * Valor create-world screen. Same rules as vanilla's GuiCreateWorld (game modes, cheats defaults, world types,
 * seeds, folder names). The Customize screens for flat and customised worlds only work with a GuiCreateWorld, so
 * they are given a hidden one and its settings are read back when they return to it (see {@link #returnFrom}).
 */
public class CreateWorldScreen extends ValorScreen {
    private static final int CREATE = 0, CANCEL = 1, TAB_GAME = 2, TAB_WORLD = 3, SURVIVAL = 4, HARDCORE = 5, CREATIVE = 6,
            TYPE = 7, CUSTOMIZE = 8, STRUCTURES = 9, CHEATS = 10, BONUS = 11;
    private static CreateWorldScreen waiting;

    private final GuiScreen parent;
    private final ValorTextField name = new ValorTextField("New World", false), seed = new ValorTextField("Random", false);
    private String gameMode = "survival", savedGameMode, saveDir = "World", generatorOptions = "";
    private boolean structures = true, cheats, cheatsSetByUser, bonusChest, hardcore, moreOptions, created;
    private int type;
    private GuiCreateWorld bridge;
    private Widgets.Pill create;
    private float cx, y0;

    public CreateWorldScreen(GuiScreen parent) {
        this.parent = parent;
        name.text = I18n.format("selectWorld.newWorld");
        name.max = seed.max = 32;
        name.focus(true);
    }

    /** Fills the form from an existing world, as vanilla's Re-create does. */
    public CreateWorldScreen recreate(WorldInfo original) {
        name.text = I18n.format("selectWorld.newWorld.copyOf", original.getWorldName());
        seed.text = Long.toString(original.getSeed());
        type = original.getTerrainType().getWorldTypeID();
        generatorOptions = original.getGeneratorOptions();
        structures = original.isMapFeaturesEnabled();
        cheats = original.areCommandsAllowed();
        if (original.isHardcoreModeEnabled()) gameMode = "hardcore";
        else if (original.getGameType().isSurvivalOrAdventure()) gameMode = "survival";
        else if (original.getGameType().isCreative()) gameMode = "creative";
        hardcore = gameMode.equals("hardcore");
        return this;
    }

    /** When a Customize screen goes back to the hidden GuiCreateWorld, returns the Valor screen that opened it. */
    public static CreateWorldScreen returnFrom(GuiCreateWorld gui) {
        CreateWorldScreen s = waiting;
        if (s == null || s.bridge != gui) return null;
        s.generatorOptions = gui.chunkProviderSettingsJson;
        waiting = null;
        return s;
    }

    private WorldType worldType() {
        return WorldType.worldTypes[type];
    }

    private boolean debug() {
        return worldType() == WorldType.DEBUG_WORLD;
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        applyWorldType();
        updateSaveDir();
        cx = width / 2f;
        y0 = 54;
        float cw = 220, x = cx - cw / 2;
        long t = opened + 80;
        buttonList.add(new Widgets.Pill(TAB_GAME, cx - 62, 30, 60, 14, 7, -1, "Game", !moreOptions, t));
        buttonList.add(new Widgets.Pill(TAB_WORLD, cx + 2, 30, 60, 14, 7, -1, "World", moreOptions, t));
        if (!moreOptions) {
            name.place(x, y0 + 8, cw, 16);
            if (debug()) {
                Widgets.Pill p = new Widgets.Pill(SURVIVAL, x, y0 + 50, cw, 16, 8, -1, "Spectator", true, t);
                p.enabled = false;
                buttonList.add(p);
            } else {
                float w3 = (cw - 8) / 3;
                String[] ids = {"survival", "hardcore", "creative"}, labels = {"Survival", "Hardcore", "Creative"};
                for (int i = 0; i < 3; i++)
                    buttonList.add(new Widgets.Pill(SURVIVAL + i, x + i * (w3 + 4), y0 + 50, w3, 16, 8, -1, labels[i], gameMode.equals(ids[i]), t));
            }
        } else {
            seed.place(x, y0 + 8, cw, 16);
            float ry = y0 + 40;
            boolean custom = worldType().isCustomizable() && !debug();
            Widgets.Setting typeRow = new Widgets.Setting(TYPE, x, ry, custom ? cw - 70 : cw, 18, "World type", t);
            typeRow.value = I18n.format(worldType().getTranslateName());
            buttonList.add(typeRow);
            if (custom) buttonList.add(new Widgets.Pill(CUSTOMIZE, x + cw - 64, ry + 1, 64, 16, 8, -1, "Customize", false, t));
            ry += worldType().showWorldInfoNotice() ? 33 : 22;
            if (!debug()) {
                if (worldType() != WorldType.CUSTOMIZED) {
                    buttonList.add(setting(STRUCTURES, x, ry, cw, "Generate structures", structures, true, t));
                    ry += 22;
                }
                buttonList.add(setting(CHEATS, x, ry, cw, "Allow cheats", cheats && !hardcore, !hardcore, t));
                buttonList.add(setting(BONUS, x, ry + 22, cw, "Bonus chest", bonusChest && !hardcore, !hardcore, t));
            }
        }
        float bw = (cw - 6) / 2;
        create = new Widgets.Pill(CREATE, x, height - 28, bw, 16, 8, -1, "Create world", true, t);
        create.enabled = !name.text.isEmpty();
        buttonList.add(create);
        buttonList.add(new Widgets.Pill(CANCEL, x + bw + 6, height - 28, bw, 16, 8, -1, "Cancel", false, t));
    }

    private static Widgets.Setting setting(int id, float x, float y, float w, String label, boolean on, boolean enabled, long t) {
        Widgets.Setting s = new Widgets.Setting(id, x, y, w, 18, label, t);
        s.on = on;
        s.enabled = enabled;
        return s;
    }

    /** The debug world forces spectator mode; leaving it restores the previous mode (vanilla's showMoreWorldOptions). */
    private void applyWorldType() {
        if (debug()) {
            if (savedGameMode == null) savedGameMode = gameMode;
            gameMode = "spectator";
        } else if (savedGameMode != null) {
            gameMode = savedGameMode;
            savedGameMode = null;
        }
    }

    /** Folder the world will be saved in: vanilla's cleaned-up name, made unique. */
    private void updateSaveDir() {
        String dir = name.text.trim();
        for (char c : ChatAllowedCharacters.allowedCharactersArray) dir = dir.replace(c, '_');
        if (dir.isEmpty()) dir = "World";
        saveDir = GuiCreateWorld.getUncollidingSaveDirName(mc.getSaveLoader(), dir);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    protected void drawContent(int mx, int my) {
        float a = Ui.ease(time() / 0.3f), x = cx - 110;
        Ui.bold().drawCentered("Create new world", cx, 10 + (1 - a) * 4, 12, Ui.alpha(Ui.TEXT, a));
        ValorFont r = Ui.regular();
        int label = Ui.alpha(Ui.MUTED, a), hint = Ui.alpha(0xFF6B7591, a);
        if (!moreOptions) {
            r.draw("World name", x + 1, y0, 5, label);
            name.draw(a);
            r.draw(r.fit("Saved in folder: " + saveDir, 5, 220), x + 1, y0 + 28, 5, hint);
            r.draw("Game mode", x + 1, y0 + 42, 5, label);
            r.draw(I18n.format("selectWorld.gameMode." + gameMode + ".line1"), x + 1, y0 + 72, 5.2f, label);
            r.draw(I18n.format("selectWorld.gameMode." + gameMode + ".line2"), x + 1, y0 + 80, 5.2f, label);
        } else {
            r.draw("Seed", x + 1, y0, 5, label);
            seed.draw(a);
            r.draw("Leave blank for a random seed", x + 1, y0 + 28, 5, hint);
            if (worldType().showWorldInfoNotice())
                r.draw(r.fit(I18n.format(worldType().getTranslatedInfo()), 5, 220), x + 1, y0 + 62, 5, Ui.alpha(0xFFFFC84A, a));
        }
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws java.io.IOException {
        ValorTextField f = moreOptions ? seed : name;
        f.focus(f.inside(mx, my));
        super.mouseClicked(mx, my, button);
    }

    @Override
    protected void keyTyped(char c, int key) {
        if (key == Keyboard.KEY_ESCAPE) mc.displayGuiScreen(parent);
        else if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
            if (create.enabled) createWorld();
        } else {
            ValorTextField f = moreOptions ? seed : name;
            if (f.focused) f.key(c, key);
            create.enabled = !name.text.isEmpty();
            updateSaveDir();
        }
    }

    @Override
    protected void actionPerformed(GuiButton b) {
        switch (b.id) {
            case CREATE: createWorld(); return;
            case CANCEL: mc.displayGuiScreen(parent); return;
            case TAB_GAME:
            case TAB_WORLD:
                moreOptions = b.id == TAB_WORLD;
                (moreOptions ? seed : name).focus(true);
                (moreOptions ? name : seed).focus(false);
                break;
            case SURVIVAL:
            case HARDCORE:
            case CREATIVE:
                // Cheats follow the mode until the player sets them (vanilla: off for survival/hardcore, on for creative).
                gameMode = b.id == SURVIVAL ? "survival" : b.id == HARDCORE ? "hardcore" : "creative";
                hardcore = b.id == HARDCORE;
                if (!cheatsSetByUser) cheats = b.id == CREATIVE;
                break;
            case TYPE:
                do type = (type + 1) % WorldType.worldTypes.length;
                while (!selectable(WorldType.worldTypes[type]));
                generatorOptions = "";
                break;
            case CUSTOMIZE:
                bridge = new GuiCreateWorld(this);
                bridge.chunkProviderSettingsJson = generatorOptions;
                waiting = this;
                worldType().onCustomizeButton(mc, bridge);
                return;
            case STRUCTURES: structures = !structures; break;
            case CHEATS:
                cheatsSetByUser = true;
                cheats = !cheats;
                break;
            case BONUS: bonusChest = !bonusChest; break;
        }
        initGui();
    }

    /** Vanilla's rule: types that can be created; the debug world only while Shift is held. */
    private static boolean selectable(WorldType t) {
        return t != null && t.getCanBeCreated() && (t != WorldType.DEBUG_WORLD || isShiftKeyDown());
    }

    private void createWorld() {
        mc.displayGuiScreen(null);
        if (created) return;
        created = true;
        long s = new Random().nextLong();
        if (!seed.text.isEmpty()) {
            try {
                long j = Long.parseLong(seed.text);
                if (j != 0) s = j;
            } catch (NumberFormatException e) {
                s = seed.text.hashCode();
            }
        }
        worldType().onGUICreateWorldPress();
        WorldSettings settings = new WorldSettings(s, WorldSettings.GameType.getByName(gameMode), structures, hardcore, worldType());
        settings.setWorldName(generatorOptions);   // the generator options, despite the name
        if (bonusChest && !hardcore) settings.enableBonusChest();
        if (cheats && !hardcore) settings.enableCommands();
        mc.launchIntegratedServer(saveDir, name.text.trim(), settings);
    }
}
