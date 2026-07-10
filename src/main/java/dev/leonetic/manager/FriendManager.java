package dev.leonetic.manager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.leonetic.Homovore;
import dev.leonetic.util.traits.Jsonable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FriendManager implements Jsonable {

    public static final long PERMANENT = -1L;

    private final Map<String, Long> friends = new LinkedHashMap<>();

    public void init() {
        Homovore.configManager.addConfig(this);
    }

    public boolean isFriend(String name) {
        purgeExpired();
        return friends.keySet().stream().anyMatch(friend -> friend.equalsIgnoreCase(name));
    }

    public boolean isFriend(Player player) {
        return this.isFriend(player.getGameProfile().name());
    }

    public void addFriend(String name) {
        this.addFriend(name, PERMANENT);
    }

    public void addFriend(String name, long expiry) {

        boolean alreadyFriend = friends.keySet().stream().anyMatch(friend -> friend.equalsIgnoreCase(name));

        friends.keySet().removeIf(friend -> friend.equalsIgnoreCase(name));
        friends.put(name, expiry);

        Homovore.enemyManager.removeEnemy(name);

        if (!alreadyFriend) {
            whisperFriendAdded(name);
        }
    }

    private void whisperFriendAdded(String name) {
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener connection = mc.getConnection();
        if (connection == null) return;
        connection.sendCommand("w " + name + " Added as a friend on client");
    }

    public void removeFriend(String name) {
        friends.keySet().removeIf(s -> s.equalsIgnoreCase(name));
    }

    public void clearFriends() {
        friends.clear();
    }

    public long getExpiry(String name) {
        for (Map.Entry<String, Long> entry : friends.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return PERMANENT;
    }

    public List<String> getFriends() {
        purgeExpired();
        return new ArrayList<>(friends.keySet());
    }

    private void purgeExpired() {
        long now = System.currentTimeMillis();
        friends.values().removeIf(expiry -> expiry != PERMANENT && expiry <= now);
    }

    @Override
    public JsonElement toJson() {
        purgeExpired();
        JsonObject object = new JsonObject();
        JsonArray array = new JsonArray();
        for (Map.Entry<String, Long> entry : friends.entrySet()) {
            JsonObject element = new JsonObject();
            element.addProperty("name", entry.getKey());
            element.addProperty("expiry", entry.getValue());
            array.add(element);
        }
        object.add("friends", array);
        return object;
    }

    @Override
    public void fromJson(JsonElement element) {
        for (JsonElement e : element.getAsJsonObject().get("friends").getAsJsonArray()) {
            if (e.isJsonObject()) {
                JsonObject obj = e.getAsJsonObject();
                long expiry = obj.has("expiry") ? obj.get("expiry").getAsLong() : PERMANENT;
                friends.put(obj.get("name").getAsString(), expiry);
            } else {

                friends.put(e.getAsString(), PERMANENT);
            }
        }
        purgeExpired();
    }

    @Override
    public String getFileName() {
        return "friends.json";
    }
}
