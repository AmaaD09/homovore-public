package dev.leonetic.util.inventory.strategy;

import dev.leonetic.util.inventory.Result;

public interface SwapStrategy {
    boolean swap(Result result);

    boolean swapBack(int last, Result result);
}
