package dev.leonetic.features.modules.combat;

import dev.leonetic.Homovore;
import dev.leonetic.event.impl.input.KeyInputEvent;
import dev.leonetic.event.system.Subscribe;
import dev.leonetic.features.modules.Module;
import dev.leonetic.features.settings.Setting;
import dev.leonetic.manager.SwapRequest;
import dev.leonetic.util.inventory.InventoryUtil;
import dev.leonetic.util.inventory.Result;
import net.minecraft.world.item.Items;

import static dev.leonetic.util.inventory.InventoryUtil.FULL_SCOPE;

public class KeyPotionModule extends Module {

    private final Setting<Boolean> onGround = bool("OnGround", false);

    public KeyPotionModule() {
        super("KeyPotion", "Throws a splash potion on keybind press.", Category.PLAYER);
    }

    @Subscribe
    public void onEnable() {
        if (onGround.getValue() && !mc.player.onGround()) return;

        Result potion = InventoryUtil.find(Items.SPLASH_POTION, FULL_SCOPE);
        if (potion.found()) {
            Homovore.swapManager.submit(new SwapRequest("KeyPotion", 40, potion,
                    () -> mc.gameMode.useItem(mc.player, potion.hand())));
        }
        disable();
    }
}
