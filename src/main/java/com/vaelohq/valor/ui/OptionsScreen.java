package com.vaelohq.valor.ui;

import com.vaelohq.valor.Config;
import com.vaelohq.valor.style.ValorPack;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.Language;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.gui.GuiScreenResourcePacks;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.EnumDifficulty;
import org.apache.commons.lang3.ArrayUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Valor settings: a sidebar of categories and a scrolling list of rows (switches, choices and sliders). Every row
 * changes the setting through GameSettings exactly as the vanilla screens do, so its side effects still happen.
 * Key bindings and languages work as in GuiControls and GuiLanguage. Options are saved when the screen closes.
 */
public class OptionsScreen extends ValorScreen {
    private enum Category {
        GENERAL("General"), VIDEO("Video"), CONTROLS("Controls"), SOUND("Music & Sounds"), CHAT("Chat"), SKIN("Skin"),
        LANGUAGE("Language"), PACKS("Resource Packs"), VALOR("Valor");
        final String label;

        Category(String label) {
            this.label = label;
        }
    }

    private static final GameSettings.Options[] VIDEO = {GameSettings.Options.GRAPHICS, GameSettings.Options.RENDER_DISTANCE,
            GameSettings.Options.AMBIENT_OCCLUSION, GameSettings.Options.FRAMERATE_LIMIT, GameSettings.Options.ANAGLYPH,
            GameSettings.Options.VIEW_BOBBING, GameSettings.Options.GUI_SCALE, GameSettings.Options.GAMMA, GameSettings.Options.RENDER_CLOUDS,
            GameSettings.Options.PARTICLES, GameSettings.Options.USE_FULLSCREEN, GameSettings.Options.ENABLE_VSYNC,
            GameSettings.Options.MIPMAP_LEVELS, GameSettings.Options.BLOCK_ALTERNATIVES, GameSettings.Options.USE_VBO,
            GameSettings.Options.ENTITY_SHADOWS};
    private static final GameSettings.Options[] CHAT = {GameSettings.Options.CHAT_VISIBILITY, GameSettings.Options.CHAT_COLOR,
            GameSettings.Options.CHAT_LINKS, GameSettings.Options.CHAT_OPACITY, GameSettings.Options.CHAT_LINKS_PROMPT,
            GameSettings.Options.CHAT_SCALE, GameSettings.Options.CHAT_HEIGHT_FOCUSED, GameSettings.Options.CHAT_HEIGHT_UNFOCUSED,
            GameSettings.Options.CHAT_WIDTH, GameSettings.Options.REDUCED_DEBUG_INFO};
    private static final int DONE = 100;
    private static final float ROW = 18, GAP = 4;
    private static Category last = Category.GENERAL;

    private final GuiScreen parent;
    private final ScrollList list = new ScrollList();
    private final List<Row> rows = new ArrayList<>();
    private Category category = last;
    private Row dragging;
    private KeyBinding listening;

    public OptionsScreen(GuiScreen parent) {
        this.parent = parent;
    }

    /** One line in the list. Draws itself at (x, y) with width w; most rows act on click, sliders on drag. */
    private abstract class Row {
        final String label;
        float hover;

        Row(String label) {
            this.label = label;
        }

        boolean enabled() {
            return true;
        }

        void draw(float x, float y, float w, float a) {
            float pad = Widgets.drawRow(x, y, w, ROW, label, hover, a * (enabled() ? 1 : 0.4f));
            control(x + w - pad, y, a * (enabled() ? 1 : 0.4f));
        }

        /** Draws the control whose right edge is at right. */
        abstract void control(float right, float y, float a);

        void click(int mx, float x, float w) {
        }

        void drag(int mx, float x, float w) {
        }
    }

    /** A switch. */
    private abstract class Toggle extends Row {
        private float knob = -1;

        Toggle(String label) {
            super(label);
        }

        abstract boolean on();

        abstract void flip();

        @Override
        void control(float right, float y, float a) {
            knob = knob < 0 ? (on() ? 1 : 0) : Ui.approach(knob, on() ? 1 : 0);
            Widgets.drawSwitch(right - 18, y + (ROW - 9) / 2, knob, a);
        }

        @Override
        void click(int mx, float x, float w) {
            flip();
        }
    }

    /** A value that changes when clicked. */
    private abstract class Choice extends Row {
        Choice(String label) {
            super(label);
        }

        abstract String value();

        @Override
        void control(float right, float y, float a) {
            Widgets.drawValue(value(), right, y, ROW, a);
        }
    }

    /** A 0..1 slider; its value text sits left of the track. */
    private abstract class Slider extends Row {
        static final float TRACK = 90;

        Slider(String label) {
            super(label);
        }

        abstract float get();

        abstract void set(float v);

        abstract String value();

        @Override
        void control(float right, float y, float a) {
            Widgets.drawSlider(right - TRACK, y + ROW / 2, TRACK, get(), hover, a);
            Widgets.drawValue(value(), right - TRACK - 7, y, ROW, a);
        }

        @Override
        void click(int mx, float x, float w) {
            dragging = this;
            drag(mx, x, w);
        }

        @Override
        void drag(int mx, float x, float w) {
            float right = x + w - ROW / 2 * 0.9f;
            set(Math.max(0, Math.min(1, (mx - (right - TRACK)) / TRACK)));
        }
    }

    /** Any vanilla option: a switch for on/off options, a slider for number options, otherwise a value to cycle. */
    private Row option(GameSettings.Options o) {
        GameSettings gs = mc.gameSettings;
        String label = I18n.format(o.getEnumString());
        if (o.getEnumBoolean()) return new Toggle(label) {
            boolean on() {
                return gs.getOptionOrdinalValue(o);
            }

            void flip() {
                gs.setOptionValue(o, 1);
            }
        };
        if (o.getEnumFloat()) return new Slider(label) {
            float get() {
                return o.normalizeValue(gs.getOptionFloatValue(o));
            }

            void set(float v) {
                gs.setOptionFloatValue(o, o.denormalizeValue(v));
            }

            String value() {
                return valueOf(o, label);
            }
        };
        return new Choice(label) {
            String value() {
                return valueOf(o, label);
            }

            void click(int mx, float x, float w) {
                gs.setOptionValue(o, 1);
            }
        };
    }

    /** GameSettings only gives "Label: value"; this keeps the value. */
    private String valueOf(GameSettings.Options o, String label) {
        String full = mc.gameSettings.getKeyBinding(o);
        return full.startsWith(label + ": ") ? full.substring(label.length() + 2) : full;
    }

    private Row action(String label, String hint, Runnable run) {
        return new Choice(label) {
            String value() {
                return hint;
            }

            void click(int mx, float x, float w) {
                run.run();
            }
        };
    }

    private void build() {
        rows.clear();
        GameSettings gs = mc.gameSettings;
        switch (category) {
            case GENERAL:
                rows.add(option(GameSettings.Options.FOV));
                if (mc.theWorld != null) rows.add(difficulty());
                if (mc.theWorld != null && mc.isSingleplayer() && !mc.theWorld.getWorldInfo().isHardcoreModeEnabled()
                        && !mc.theWorld.getWorldInfo().isDifficultyLocked()) rows.add(action("Lock difficulty", "Lock", this::lockDifficulty));
                if (mc.theWorld == null) rows.add(option(GameSettings.Options.REALMS_NOTIFICATIONS));
                rows.add(option(GameSettings.Options.SNOOPER_ENABLED));
                rows.add(action("Super Secret Settings", "Shuffle", () -> mc.entityRenderer.activateNextShader()));
                break;
            case VIDEO:
                for (GameSettings.Options o : VIDEO) if (o != GameSettings.Options.USE_VBO || OpenGlHelper.vboSupported) rows.add(option(o));
                break;
            case CONTROLS:
                rows.add(option(GameSettings.Options.INVERT_MOUSE));
                rows.add(option(GameSettings.Options.SENSITIVITY));
                rows.add(option(GameSettings.Options.TOUCHSCREEN));
                rows.add(action(I18n.format("controls.resetAll"), "Reset", () -> {
                    for (KeyBinding k : gs.keyBindings) k.setKeyCode(k.getKeyCodeDefault());
                    KeyBinding.resetKeyBindingArrayAndHash();
                }));
                KeyBinding[] keys = ArrayUtils.clone(gs.keyBindings);
                Arrays.sort(keys);
                String section = null;
                for (KeyBinding k : keys) {
                    if (!k.getKeyCategory().equals(section)) rows.add(new Header(I18n.format(section = k.getKeyCategory())));
                    rows.add(new KeyRow(k));
                }
                break;
            case SOUND:
                for (SoundCategory c : SoundCategory.values()) rows.add(new Slider(I18n.format("soundCategory." + c.getCategoryName())) {
                    float get() {
                        return gs.getSoundLevel(c);
                    }

                    void set(float v) {
                        gs.setSoundLevel(c, v);
                    }

                    String value() {
                        float v = get();
                        return v == 0 ? "OFF" : (int) (v * 100) + "%";
                    }
                });
                break;
            case CHAT:
                for (GameSettings.Options o : CHAT) rows.add(option(o));
                break;
            case SKIN:
                for (EnumPlayerModelParts p : EnumPlayerModelParts.values()) rows.add(new Toggle(p.func_179326_d().getUnformattedText()) {
                    boolean on() {
                        return gs.getModelParts().contains(p);
                    }

                    void flip() {
                        gs.switchModelPartEnabled(p);
                    }
                });
                break;
            case LANGUAGE:
                rows.add(option(GameSettings.Options.FORCE_UNICODE_FONT));
                rows.add(new Header("Languages"));
                for (Language l : mc.getLanguageManager().getLanguages()) rows.add(new Choice(l.toString()) {
                    String value() {
                        return l.getLanguageCode().equals(mc.getLanguageManager().getCurrentLanguage().getLanguageCode()) ? "Selected" : "";
                    }

                    void click(int mx, float x, float w) {
                        selectLanguage(l);
                    }
                });
                break;
            case PACKS:
                rows.add(action("Resource packs", "Open", () -> mc.displayGuiScreen(new GuiScreenResourcePacks(this))));
                break;
            case VALOR:
                rows.add(new Toggle("Valor style for the HUD, inventories and buttons") {
                    boolean on() {
                        return Config.valorStyle;
                    }

                    void flip() {
                        ValorPack.setEnabled(!Config.valorStyle);
                    }
                });
                break;
        }
    }

    /** Section title inside the list. */
    private final class Header extends Row {
        Header(String label) {
            super(label);
        }

        @Override
        boolean enabled() {
            return false;
        }

        @Override
        void draw(float x, float y, float w, float a) {
            Ui.semibold().draw(label, x + 4, y + ROW - 9, 5.5f, Ui.alpha(Ui.MUTED, a));
        }

        @Override
        void control(float right, float y, float a) {
        }
    }

    /** A key binding: click it, then press a key or mouse button (Escape clears it). Red when another binding uses the same key. */
    private final class KeyRow extends Row {
        static final float KEY = 64, RESET = 30;
        final KeyBinding key;

        KeyRow(KeyBinding key) {
            super(I18n.format(key.getKeyDescription()));
            this.key = key;
        }

        @Override
        void control(float right, float y, float a) {
            boolean clash = false;
            if (key.getKeyCode() != 0) for (KeyBinding k : mc.gameSettings.keyBindings)
                if (k != key && k.getKeyCode() == key.getKeyCode()) clash = true;
            boolean wait = listening == key;
            String name = wait ? "Press a key" : GameSettings.getKeyDisplayString(key.getKeyCode());
            Shapes.draw(right - KEY, y + 3, KEY, ROW - 6, (ROW - 6) / 2, Ui.alpha(wait ? 0xFF22357A : 0xFF1A2140, a),
                    Ui.alpha(wait ? 0xFF1A2A66 : 0xFF141A33, a), Ui.alpha(wait ? Ui.CYAN : clash ? Ui.DANGER : 0x30FFFFFF, a), wait || clash ? 0.8f : 0.5f, 0, 0, 0);
            ValorFont f = Ui.semibold();
            float size = ROW * 0.29f;
            f.drawCentered(f.fit(name, size, KEY - 8), right - KEY / 2, y + (ROW - size * 1.2f) / 2, size,
                    Ui.alpha(wait ? Ui.TEXT : clash ? Ui.DANGER : 0xFFC9D2E8, a));
            if (key.getKeyCode() != key.getKeyCodeDefault())
                Ui.regular().draw("Reset", right - KEY - RESET, y + (ROW - size * 1.2f) / 2, size, Ui.alpha(0xFF8FB4FF, a));
        }

        @Override
        void click(int mx, float x, float w) {
            float right = x + w - ROW / 2 * 0.9f;
            if (key.getKeyCode() != key.getKeyCodeDefault() && mx >= right - KEY - RESET - 2 && mx < right - KEY - 2) {
                mc.gameSettings.setOptionKeyBinding(key, key.getKeyCodeDefault());
                KeyBinding.resetKeyBindingArrayAndHash();
            } else listening = key;
        }
    }

    /** Same steps as vanilla's GuiLanguage. */
    private void selectLanguage(Language l) {
        GameSettings gs = mc.gameSettings;
        mc.getLanguageManager().setCurrentLanguage(l);
        gs.language = l.getLanguageCode();
        mc.refreshResources();
        mc.fontRendererObj.setUnicodeFlag(mc.getLanguageManager().isCurrentLocaleUnicode() || gs.forceUnicodeFont);
        mc.fontRendererObj.setBidiFlag(mc.getLanguageManager().isCurrentLanguageBidirectional());
        gs.saveOptions();
        initGui();
    }

    /** Vanilla's difficulty button: cycles in singleplayer unless hardcore or locked, shown read-only otherwise. */
    private Row difficulty() {
        return new Choice(I18n.format("options.difficulty")) {
            boolean enabled() {
                return mc.isSingleplayer() && !mc.theWorld.getWorldInfo().isHardcoreModeEnabled() && !mc.theWorld.getWorldInfo().isDifficultyLocked();
            }

            String value() {
                return I18n.format(mc.theWorld.getDifficulty().getDifficultyResourceKey());
            }

            void click(int mx, float x, float w) {
                mc.theWorld.getWorldInfo().setDifficulty(EnumDifficulty.getDifficultyEnum(mc.theWorld.getDifficulty().getDifficultyId() + 1));
            }
        };
    }

    private void lockDifficulty() {
        String level = I18n.format(mc.theWorld.getWorldInfo().getDifficulty().getDifficultyResourceKey());
        mc.displayGuiScreen(new ConfirmScreen(this, I18n.format("difficulty.lock.title"), I18n.format("difficulty.lock.question", level),
                "Lock", "Cancel", true, yes -> {
            if (yes && mc.theWorld != null) mc.theWorld.getWorldInfo().setDifficultyLocked(true);
            mc.displayGuiScreen(this);
        }));
    }

    @Override
    public void initGui() {
        super.initGui();
        float total = Math.min(400, width - 24), x0 = (width - total) / 2, side = 92, top = 44;
        list.place(x0 + side + 10, top, total - side - 10 - 8, height - top - 36);
        long t = opened + 60;
        Category[] cats = Category.values();
        for (int i = 0; i < cats.length; i++)
            buttonList.add(new Widgets.Pill(i, x0, top + i * 19, side, 16, 8, -1, cats[i].label, cats[i] == category, t + i * 15));
        buttonList.add(new Widgets.Pill(DONE, (width - 120) / 2f, height - 26, 120, 16, 8, -1, "Done", false, t));
        build();
    }

    @Override
    protected void drawContent(int mx, int my) {
        float a = Ui.ease(time() / 0.3f);
        Ui.bold().drawCentered("Options", width / 2f, 12 + (1 - a) * 4, 13, Ui.alpha(Ui.TEXT, a));
        boolean in = list.contains(mx, my);
        list.begin(rows.size() * (ROW + GAP) - GAP);
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            float y = list.y + i * (ROW + GAP) - list.scroll;
            r.hover = Ui.approach(r.hover, r.enabled() && (r == dragging || in && my >= y && my < y + ROW) ? 1 : 0);
            if (y + ROW >= list.y && y <= list.y + list.h) r.draw(list.x, y, list.w, a * Ui.ease((time() - 0.03f * Math.min(i, 10)) / 0.3f));
        }
        list.end(mx, my, a);
    }

    private Row rowAt(int mx, int my) {
        if (!list.contains(mx, my)) return null;
        int i = (int) Math.floor((my - list.y + list.scroll) / (ROW + GAP));
        return i >= 0 && i < rows.size() && my < list.y + i * (ROW + GAP) - list.scroll + ROW ? rows.get(i) : null;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        list.wheel(Mouse.getEventDWheel(), ROW + GAP);
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        if (listening != null) {   // binding a mouse button, as GuiControls does
            mc.gameSettings.setOptionKeyBinding(listening, -100 + button);
            listening = null;
            KeyBinding.resetKeyBindingArrayAndHash();
            return;
        }
        super.mouseClicked(mx, my, button);
        if (button != 0 || list.press(mx, my)) return;
        Row r = rowAt(mx, my);
        if (r == null || !r.enabled()) return;
        int scale = mc.gameSettings.guiScale;
        r.click(mx, list.x, list.w);
        if (dragging == null) mc.getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
        relayoutIfScaleChanged(scale);
    }

    @Override
    protected void mouseClickMove(int mx, int my, int button, long held) {
        list.drag(my);
        if (dragging != null) dragging.drag(mx, list.x, list.w);
    }

    @Override
    protected void mouseReleased(int mx, int my, int state) {
        super.mouseReleased(mx, my, state);
        list.release();
        if (dragging != null) mc.getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
        dragging = null;
    }

    /** GUI scale changes the screen's size in GUI units, so lay out again (as GuiVideoSettings does). */
    private void relayoutIfScaleChanged(int before) {
        if (mc.gameSettings.guiScale == before) return;
        ScaledResolution sr = new ScaledResolution(mc);
        setWorldAndResolution(mc, sr.getScaledWidth(), sr.getScaledHeight());
    }

    @Override
    protected void actionPerformed(GuiButton b) {
        if (b.id == DONE) {
            mc.displayGuiScreen(parent);
            return;
        }
        last = category = Category.values()[b.id];
        listening = null;
        initGui();
    }

    @Override
    protected void keyTyped(char c, int key) {
        if (listening != null) {   // GuiControls: Escape clears, otherwise the key (or the character for keys LWJGL doesn't know)
            if (key == Keyboard.KEY_ESCAPE) mc.gameSettings.setOptionKeyBinding(listening, 0);
            else if (key != 0) mc.gameSettings.setOptionKeyBinding(listening, key);
            else if (c > 0) mc.gameSettings.setOptionKeyBinding(listening, c + 256);
            listening = null;
            KeyBinding.resetKeyBindingArrayAndHash();
        } else if (key == Keyboard.KEY_ESCAPE) mc.displayGuiScreen(parent);
    }

    @Override
    public void onGuiClosed() {
        mc.gameSettings.saveOptions();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return parent == null || parent.doesGuiPauseGame();
    }
}
