package com.vaelohq.valor.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** A module option shown in its settings panel and saved with the module. */
public abstract class Setting {
    public final String id, name;

    Setting(String id, String name) {
        this.id = id;
        this.name = name;
    }

    abstract JsonElement save();

    abstract void load(JsonElement json);

    /** On / off. */
    public static final class Toggle extends Setting {
        public boolean value;

        public Toggle(String id, String name, boolean value) {
            super(id, name);
            this.value = value;
        }

        JsonElement save() {
            return new JsonPrimitive(value);
        }

        void load(JsonElement json) {
            value = json.getAsBoolean();
        }
    }

    /** A number between min and max, in steps; suffix is shown after the value. */
    public static final class Slider extends Setting {
        public final float min, max, step;
        public final String suffix;
        public float value;

        public Slider(String id, String name, float value, float min, float max, float step, String suffix) {
            super(id, name);
            this.value = value;
            this.min = min;
            this.max = max;
            this.step = step;
            this.suffix = suffix;
        }

        /** Sets from a 0..1 position, snapped to the step. */
        public void setFraction(float f) {
            value = Math.max(min, Math.min(max, min + Math.round(f * (max - min) / step) * step));
        }

        public float fraction() {
            return (value - min) / (max - min);
        }

        public String text() {
            return (step >= 1 ? String.valueOf(Math.round(value)) : String.format("%.1f", value)) + suffix;
        }

        JsonElement save() {
            return new JsonPrimitive(value);
        }

        void load(JsonElement json) {
            value = Math.max(min, Math.min(max, json.getAsFloat()));
        }
    }

    /** One of several named options. */
    public static final class Choice extends Setting {
        public final String[] options;
        public int index;

        public Choice(String id, String name, int index, String... options) {
            super(id, name);
            this.options = options;
            this.index = index;
        }

        public String value() {
            return options[index];
        }

        public void next() {
            index = (index + 1) % options.length;
        }

        JsonElement save() {
            return new JsonPrimitive(options[index]);
        }

        void load(JsonElement json) {
            for (int i = 0; i < options.length; i++) if (options[i].equals(json.getAsString())) index = i;
        }
    }

    /** A colour picked from Valor's palette. */
    public static final class Color extends Setting {
        public static final int[] PALETTE = {0xFFF4F7FF, 0xFF9AA6C4, 0xFF4C85FF, 0xFF00E5FF, 0xFF5BE38E, 0xFFFFD84A, 0xFFFF9F43, 0xFFFF5A6E,
                0xFFC77DFF, 0xFFFF7EB6};
        public int value;

        public Color(String id, String name, int value) {
            super(id, name);
            this.value = value;
        }

        JsonElement save() {
            return new JsonPrimitive(String.format("#%08X", value));
        }

        void load(JsonElement json) {
            value = (int) Long.parseLong(json.getAsString().substring(1), 16);
        }
    }
}
