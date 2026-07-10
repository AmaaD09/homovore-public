package dev.leonetic.features.modules.render;

import dev.leonetic.features.modules.Module;
import dev.leonetic.features.settings.Setting;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.projectile.Projectile;

public class SeeThroughModule extends Module {

    public Setting<Boolean> players      = bool("Players",     true).setPage("Entities");
    public Setting<Float>   playersRange = num("Players Range", 64.0f, 4.0f, 256.0f).setPage("Entities").setVisibility(v -> players.getValue());

    public Setting<Boolean> monsters      = bool("Monsters",    false).setPage("Entities");
    public Setting<Float>   monstersRange = num("Monsters Range", 64.0f, 4.0f, 256.0f).setPage("Entities").setVisibility(v -> monsters.getValue());

    public Setting<Boolean> animals      = bool("Animals",     false).setPage("Entities");
    public Setting<Float>   animalsRange = num("Animals Range", 64.0f, 4.0f, 256.0f).setPage("Entities").setVisibility(v -> animals.getValue());

    public Setting<Boolean> items      = bool("Items",        true).setPage("Entities");
    public Setting<Float>   itemsRange = num("Items Range", 64.0f, 4.0f, 256.0f).setPage("Entities").setVisibility(v -> items.getValue());

    public Setting<Boolean> crystals      = bool("Crystals",     false).setPage("Entities");
    public Setting<Float>   crystalsRange = num("Crystals Range", 64.0f, 4.0f, 256.0f).setPage("Entities").setVisibility(v -> crystals.getValue());

    public Setting<Boolean> projectiles      = bool("Projectiles",  false).setPage("Entities");
    public Setting<Float>   projectilesRange = num("Projectiles Range", 64.0f, 4.0f, 256.0f).setPage("Entities").setVisibility(v -> projectiles.getValue());

    public SeeThroughModule() {
        super("SeeThrough", "Render selected entities through walls", Category.RENDER);
    }

    public boolean shouldSeeThrough(EntityRenderState state) {
        if (state == null) return false;

        EntityType<?> type = state.entityType;
        if (type == null) return false;

        if (type == EntityType.PLAYER)      return inRange(state, players, playersRange);
        if (type == EntityType.ITEM)        return inRange(state, items, itemsRange);
        if (type == EntityType.END_CRYSTAL) return inRange(state, crystals, crystalsRange);
        if (Projectile.class.isAssignableFrom(type.getBaseClass())) return inRange(state, projectiles, projectilesRange);
        if (type.getCategory() == MobCategory.MONSTER) return inRange(state, monsters, monstersRange);
        if (type.getCategory() != MobCategory.MISC)    return inRange(state, animals, animalsRange);
        return false;
    }

    private boolean inRange(EntityRenderState state, Setting<Boolean> enabled, Setting<Float> range) {
        if (!enabled.getValue()) return false;
        float r = range.getValue();
        return state.distanceToCameraSq <= (double) r * r;
    }
}
