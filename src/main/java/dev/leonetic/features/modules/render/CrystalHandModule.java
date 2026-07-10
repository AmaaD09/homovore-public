package dev.leonetic.features.modules.render;

import dev.leonetic.features.modules.Module;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class CrystalHandModule extends Module {
    public CrystalHandModule() {
        super("CrystalHand", "Renders your sword as an end crystal if you have crystals in your inventory", Category.RENDER);
    }

    public boolean hasCrystalsInInventory() {
        if (nullCheck()) return false;
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getItem(i).is(Items.END_CRYSTAL)) return true;
        }
        return false;
    }

    public ItemStack getDisplayStack(ItemStack original) {
        if (original.is(ItemTags.SWORDS) && hasCrystalsInInventory()) {
            return new ItemStack(Items.END_CRYSTAL);
        }
        return original;
    }
}
