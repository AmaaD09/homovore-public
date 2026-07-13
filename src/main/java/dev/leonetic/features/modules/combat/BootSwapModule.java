package dev.leonetic.features.modules.combat;

import dev.leonetic.event.impl.entity.player.PreTickEvent;
import dev.leonetic.event.system.Subscribe;
import dev.leonetic.features.modules.Module;
import dev.leonetic.util.EnchantmentUtil;
import dev.leonetic.util.inventory.InventoryUtil;
import dev.leonetic.util.inventory.Result;
import dev.leonetic.util.inventory.ResultType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.state.BlockState;

import static dev.leonetic.util.inventory.InventoryUtil.FULL_SCOPE;

public class BootSwapModule extends Module {

    private static final int BOOTS_MENU_SLOT = 8;
    private static final int OFFHAND_MENU_SLOT = 45;

    public BootSwapModule() {
        super("BootSwap", "Equips Protection boots while phased and Blast Protection boots otherwise.", Category.COMBAT);
    }

    @Subscribe
    private void onPreTick(PreTickEvent event) {
        if (nullCheck()) return;

        ResourceKey<Enchantment> desired = isPhased()
                ? Enchantments.PROTECTION
                : Enchantments.BLAST_PROTECTION;
        if (EnchantmentUtil.has(desired, mc.player.getItemBySlot(EquipmentSlot.FEET))) return;
        if (mc.gameMode == null || mc.player.containerMenu != mc.player.inventoryMenu) return;
        if (!InventoryUtil.cursor().isEmpty()) return;

        Result boots = InventoryUtil.find(
                stack -> isBoots(stack) && EnchantmentUtil.has(desired, stack),
                FULL_SCOPE
        );
        if (!boots.found()) return;

        int sourceSlot = containerSlotOf(boots);
        InventoryUtil.click(sourceSlot, 0, ClickType.PICKUP);
        InventoryUtil.click(BOOTS_MENU_SLOT, 0, ClickType.PICKUP);
        InventoryUtil.click(sourceSlot, 0, ClickType.PICKUP);
    }

    private boolean isPhased() {
        BlockState state = mc.level.getBlockState(mc.player.blockPosition());
        return !state.isAir() && !state.canBeReplaced();
    }

    private static boolean isBoots(ItemStack stack) {
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null && equippable.slot() == EquipmentSlot.FEET;
    }

    private static int containerSlotOf(Result result) {
        if (result.type() == ResultType.OFFHAND) return OFFHAND_MENU_SLOT;
        return result.slot() < 9 ? result.slot() + 36 : result.slot();
    }
}
