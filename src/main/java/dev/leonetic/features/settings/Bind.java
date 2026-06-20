package dev.leonetic.features.settings;

import com.google.common.base.Converter;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.blaze3d.platform.InputConstants;
import dev.leonetic.util.traits.Util;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

public class Bind implements Util {
    public static final int MOUSE_BUTTON_OFFSET = 1000;

    private int key;

    public Bind(int key) {
        this.key = key;
    }

    public static Bind none() {
        return new Bind(-1);
    }

    public static Bind fromMouseButton(int button) {
        return new Bind(MOUSE_BUTTON_OFFSET + button);
    }

    public static boolean isMouseButton(int key) {
        return key >= MOUSE_BUTTON_OFFSET;
    }

    public int getKey() {
        return this.key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public boolean isEmpty() {
        return this.key < 0;
    }

    public String toString() {
        if (this.isEmpty()) return "None";
        if (isMouseButton(this.key)) {
            return "M" + (this.key - MOUSE_BUTTON_OFFSET + 1);
        }
        String raw = InputConstants.getKey(new KeyEvent(this.key, 0, 0)).getName();
        String cleaned = raw.replace("key.keyboard.", "").replace("key.mouse.", "").replace('.', ' ').trim();
        StringBuilder out = new StringBuilder(cleaned.length());
        for (String part : cleaned.split(" ")) {
            if (part.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(this.capitalise(part));
        }
        return out.toString();
    }

    public boolean isDown() {
        if (this.isEmpty()) return false;
        if (isMouseButton(this.key)) {
            return GLFW.glfwGetMouseButton(mc.getWindow().handle(), this.key - MOUSE_BUTTON_OFFSET) == GLFW.GLFW_PRESS;
        }
        return GLFW.glfwGetKey(mc.getWindow().handle(), this.getKey()) == 1;
    }

    private String capitalise(String str) {
        if (str.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(str.charAt(0)) + (str.length() != 1 ? str.substring(1).toLowerCase() : "");
    }

    public static class BindConverter
            extends Converter<Bind, JsonElement> {
        public JsonElement doForward(Bind bind) {
            return new JsonPrimitive(bind.toString());
        }

        public Bind doBackward(JsonElement jsonElement) {
            String s = jsonElement.getAsString();
            if (s.equalsIgnoreCase("None")) {
                return Bind.none();
            }
            if (s.toUpperCase().startsWith("M") && s.length() >= 2) {
                try {
                    int button = Integer.parseInt(s.substring(1)) - 1;
                    if (button >= 0) return Bind.fromMouseButton(button);
                } catch (NumberFormatException ignored) {

                }
            }
            int key = -1;
            try {
                key = InputConstants.getKey(s.toUpperCase()).getValue();
            } catch (Exception exception) {

            }
            if (key == 0) {
                return Bind.none();
            }
            return new Bind(key);
        }
    }
}
