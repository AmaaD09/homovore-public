package dev.leonetic.features.modules.player;

import dev.leonetic.features.modules.Module;
import dev.leonetic.features.settings.Setting;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class FastuseModule extends Module {
    public final Setting<Boolean> all         = bool("All",         false);
    public final Setting<Boolean> experience  = bool("Experience",  true);
    public final Setting<Boolean> blocks      = bool("Blocks",      false);
    public final Setting<Boolean> enderChests = bool("EnderChests", true);

    public FastuseModule() {
        super("Fastuse", "Use items and interact with blocks faster", Category.PLAYER);
    }

    @Override
    public void onTick() {
        if (nullCheck()) return;
        if (matches(mc.player.getMainHandItem()) || matches(mc.player.getOffhandItem())) {
            mc.rightClickDelay = 0;
        }
    }

    private boolean matches(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (all.getValue()) return true;
        if (experience.getValue()  && stack.is(Items.EXPERIENCE_BOTTLE)) return true;
        if (enderChests.getValue() && stack.is(Items.ENDER_CHEST)) return true;
        if (blocks.getValue()      && stack.getItem() instanceof BlockItem) return true;
        return false;
    }
}
