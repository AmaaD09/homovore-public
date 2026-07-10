package dev.leonetic.util.inventory.strategy;

import dev.leonetic.util.inventory.InventoryUtil;
import dev.leonetic.util.inventory.Result;
import dev.leonetic.util.inventory.ResultType;
import net.minecraft.world.inventory.ClickType;

public final class InventoryStrategy implements SwapStrategy {
    public static final InventoryStrategy INSTANCE = new InventoryStrategy();

    private InventoryStrategy() {
    }

    @Override
    public boolean swap(Result result) {
        if (result.type() != ResultType.INVENTORY && result.type() != ResultType.HOTBAR)
            return false;
        int slot = inventorySlot(result);
        InventoryUtil.click(slot, InventoryUtil.selected(), ClickType.SWAP);
        return true;
    }

    @Override
    public boolean swapBack(int last, Result result) {
        return swap(result);
    }

    private static int inventorySlot(Result result) {
        return result.type() == ResultType.HOTBAR ? result.slot() + 36 : result.slot();
    }
}
