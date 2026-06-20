package dev.leonetic.features;

import dev.leonetic.event.impl.input.KeyInputEvent;
import dev.leonetic.event.impl.input.MouseInputEvent;
import dev.leonetic.event.system.Subscribe;
import dev.leonetic.features.gui.HomovoreGui;
import dev.leonetic.util.traits.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractCommandBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.StructureBlockEditScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import org.lwjgl.glfw.GLFW;

public class GuiMove implements Util {

    public void init() {
        EVENT_BUS.register(this);
    }

    @Subscribe
    private void onKey(KeyInputEvent event) {
        if (event.getAction() != GLFW.GLFW_PRESS && event.getAction() != GLFW.GLFW_RELEASE) return;
        if (!shouldRun()) return;

        if (mc.screen instanceof HomovoreGui && isArrowKey(event.getKey())) return;
        boolean pressed = event.getAction() == GLFW.GLFW_PRESS;
        KeyEvent ke = new KeyEvent(event.getKey(), 0, 0);
        pass(mc.options.keyUp,     ke, null, pressed);
        pass(mc.options.keyDown,   ke, null, pressed);
        pass(mc.options.keyLeft,   ke, null, pressed);
        pass(mc.options.keyRight,  ke, null, pressed);
        pass(mc.options.keyJump,   ke, null, pressed);
        pass(mc.options.keyShift,  ke, null, pressed);
        pass(mc.options.keySprint, ke, null, pressed);
    }

    @Subscribe
    private void onMouse(MouseInputEvent event) {
        if (event.getAction() != GLFW.GLFW_PRESS && event.getAction() != GLFW.GLFW_RELEASE) return;
        if (!shouldRun()) return;
        boolean pressed = event.getAction() == GLFW.GLFW_PRESS;
        MouseButtonEvent me = new MouseButtonEvent(0, 0, new MouseButtonInfo(event.getButton(), 0));
        pass(mc.options.keyUp,     null, me, pressed);
        pass(mc.options.keyDown,   null, me, pressed);
        pass(mc.options.keyLeft,   null, me, pressed);
        pass(mc.options.keyRight,  null, me, pressed);
        pass(mc.options.keyJump,   null, me, pressed);
        pass(mc.options.keyShift,  null, me, pressed);
        pass(mc.options.keySprint, null, me, pressed);
    }

    private void pass(KeyMapping mapping, KeyEvent ke, MouseButtonEvent me, boolean pressed) {
        boolean matched = ke != null ? mapping.matches(ke) : mapping.matchesMouse(me);
        if (!matched) return;
        mapping.setDown(pressed);
    }

    private static boolean isArrowKey(int key) {
        return key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_RIGHT
                || key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_DOWN;
    }

    private boolean shouldRun() {
        Screen s = mc.screen;
        if (s == null) return false;

        if (s instanceof ChatScreen) return false;
        if (s instanceof AbstractSignEditScreen) return false;
        if (s instanceof AnvilScreen) return false;
        if (s instanceof AbstractCommandBlockEditScreen) return false;
        if (s instanceof StructureBlockEditScreen) return false;
        return true;
    }

}
