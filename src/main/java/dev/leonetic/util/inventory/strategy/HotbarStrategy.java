package dev.leonetic.util.inventory.strategy;

import dev.leonetic.util.inventory.InventoryUtil;
import dev.leonetic.util.inventory.Result;
import dev.leonetic.util.inventory.ResultType;

public final class HotbarStrategy implements SwapStrategy {
    public static final HotbarStrategy INSTANCE = new HotbarStrategy();

    private HotbarStrategy() {
    }

    @Override
    public boolean swap(Result result) {
        if (result.type() == ResultType.HOTBAR) {
            InventoryUtil.swap(result.slot());
            return true;
        }
        return false;
    }

    @Override
    public boolean swapBack(int last, Result result) {
        if (result.type() == ResultType.HOTBAR) {
            InventoryUtil.swap(last);
            return true;
        }
        return false;
    }
}
