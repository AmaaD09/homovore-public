package dev.leonetic.util.player;

import dev.leonetic.Homovore;
import dev.leonetic.features.commands.Command;
import dev.leonetic.mixin.client.ChatComponentAccessor;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.leonetic.util.traits.Util.mc;

public class ChatUtil {
    private static final Map<String, MessageSignature> PERSISTENT = new HashMap<>();
    private static final SecureRandom RNG = new SecureRandom();

    public static volatile GuiMessage.Line noAnimateHead;

    public static void sendMessage(Component message) {
        sendSilentMessage(prefixed(message));
    }

    public static void sendSilentMessage(Component message) {
        if (Command.nullCheck()) {
            return;
        }

        mc.gui.getChat().addMessage(message);
    }

    public static void sendPersistent(String key, Component message) {
        addPersistent(key, prefixed(message));
    }

    private static void removeSilently(MessageSignature signature) {
        if (Command.nullCheck()) {
            return;
        }
        ChatComponent hud = mc.gui.getChat();
        ChatComponentAccessor accessor = (ChatComponentAccessor) hud;

        boolean removed = accessor.homovore$getAllMessages().removeIf(line -> signature.equals(line.signature()));
        if (removed) {
            accessor.homovore$refreshTrimmedMessages();
        }
    }

    private static final FontDescription LOGO_FONT = new FontDescription.Resource(
            Identifier.fromNamespaceAndPath("homovore", "logo"));

    public static Component getClientNameComponent() {
        return Component.literal("")
                .withStyle(Style.EMPTY.withFont(LOGO_FONT))
                .withColor(Homovore.colorManager.getAsInt("chat"));
    }

    public static Component getSkullComponent() {
        return Component.literal("☠").withColor(0xFF5555);
    }

    public static Component getInfoComponent() {
        return Component.literal("ℹ").withColor(0xFFFF55);
    }

    public static void sendPrefixed(Component icon, Component message) {
        sendSilentMessage(prefixedWith(icon, message));
    }

    public static void sendPersistentPrefixed(String key, Component icon, Component message) {
        addPersistent(key, prefixedWith(icon, message));
    }

    private static void addPersistent(String key, Component component) {
        if (Command.nullCheck()) {
            return;
        }
        MessageSignature previous = PERSISTENT.remove(key);
        if (previous != null) {
            removeSilently(previous);
        }
        byte[] data = new byte[256];
        RNG.nextBytes(data);
        MessageSignature signature = new MessageSignature(data);
        mc.gui.getChat().addMessage(component, signature, null);
        PERSISTENT.put(key, signature);

        List<GuiMessage.Line> trimmed = ((ChatComponentAccessor) mc.gui.getChat()).homovore$getTrimmedMessages();
        noAnimateHead = trimmed.isEmpty() ? null : trimmed.get(0);
    }

    private static Component prefixedWith(Component icon, Component message) {
        return Component.empty()
                .append(Component.literal("[").withColor(Homovore.colorManager.getAsInt("chatBracket")))
                .append(icon)
                .append(Component.literal("]").withColor(Homovore.colorManager.getAsInt("chatBracket")))
                .append(Component.literal(" ").withColor(Homovore.colorManager.getAsInt("chatBracket")))
                .append(message);
    }

    private static Component prefixed(Component message) {
        return Component.empty()
                .append(Component.literal("[").withColor(Homovore.colorManager.getAsInt("chatBracket")))
                .append(getClientNameComponent())
                .append(Component.literal("]").withColor(Homovore.colorManager.getAsInt("chatBracket")))
                .append(Component.literal(" ").withColor(Homovore.colorManager.getAsInt("chatBracket")))
                .append(message);
    }
}
