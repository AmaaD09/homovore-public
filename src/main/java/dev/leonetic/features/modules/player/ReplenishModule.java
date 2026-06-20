package dev.leonetic.features.modules.player;

import dev.leonetic.event.impl.entity.player.TickEvent;
import dev.leonetic.event.system.Subscribe;
import dev.leonetic.features.modules.Module;
import dev.leonetic.features.settings.Setting;
import dev.leonetic.util.inventory.InventoryUtil;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class ReplenishModule extends Module {

    private final Setting<Integer> percentage = num("Percentage", 20, 10, 50);
    private final Setting<Boolean> alternative = bool("Alternative", false);

    private final Map<Integer, Integer> hotbarTicks = new HashMap<>();
    private final Map<Integer, Item> lastHotbarItems = new HashMap<>();

    public ReplenishModule() {
        super("Replenish", "Automatically refills items in the hotbar.", Category.PLAYER);
    }

    @Subscribe
    private void onTick(TickEvent event) {
        if (nullCheck()) return;
        if (mc.screen != null) return;

        if (!InventoryUtil.cursor().isEmpty()) return;

        for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++) {
            ItemStack stack = mc.player.getInventory().getItem(hotbarSlot);
            Item currentItem = stack.isEmpty() ? null : stack.getItem();

            if (lastHotbarItems.getOrDefault(hotbarSlot, null) != currentItem) {
                hotbarTicks.put(hotbarSlot, 0);
                lastHotbarItems.put(hotbarSlot, currentItem);
            } else {
                hotbarTicks.put(hotbarSlot, hotbarTicks.getOrDefault(hotbarSlot, 0) + 1);
            }

            if (hotbarTicks.getOrDefault(hotbarSlot, 0) < 10) continue;
            if (stack.isEmpty()) continue;

            int maxCount = stack.getMaxStackSize();
            int minCount = Math.max(1, (int) (maxCount * (percentage.getValue() / 100f)));

            if (stack.getCount() < minCount) {
                int invSlot = findInventorySlotToReplenish(stack);
                if (invSlot != -1) {
                    swap(hotbarSlot, invSlot);
                    return;
                }
            }
        }
    }

    private int findInventorySlotToReplenish(ItemStack target) {
        for (int i = 9; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            if (!ItemStack.isSameItemSameComponents(stack, target)) continue;
            return i;
        }
        return -1;
    }

    private void swap(int hotbarSlot, int invSlot) {

        int realInvSlot = invSlot;
        int realHotbarSlot = hotbarSlot + 36;

        if (alternative.getValue()) {

            InventoryUtil.click(realInvSlot, 0, ClickType.QUICK_MOVE);
        } else {

            InventoryUtil.click(realInvSlot, 0, ClickType.PICKUP);
            InventoryUtil.click(realHotbarSlot, 0, ClickType.PICKUP);
            InventoryUtil.click(realInvSlot, 0, ClickType.PICKUP);
        }
    }
}
