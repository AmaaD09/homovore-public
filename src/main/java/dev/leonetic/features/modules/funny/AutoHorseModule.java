package dev.leonetic.features.modules.funny;

import dev.leonetic.Homovore;
import dev.leonetic.event.impl.render.Render2DEvent;
import dev.leonetic.features.modules.Module;
import dev.leonetic.features.settings.Setting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class AutoHorseModule extends Module {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("homovore", "textures/autohorse.png");
    private static final int TEXTURE_WIDTH = 1024;
    private static final int TEXTURE_HEIGHT = 1536;
    private static final long MIN_TRIGGER_INTERVAL_MS = 200L;

    private final Setting<Integer> duration = num("Duration", 25, 5, 100);
    private final Setting<Float> scale = num("Scale", 0.16f, 0.05f, 0.35f);
    private final Setting<Float> volume = num("Volume", 1.0f, 0.1f, 1.0f);

    private final List<HorseSprite> horses = new ArrayList<>();

    private int activeTicks;
    private boolean wasHurtLastTick;
    private long lastTriggerAt;

    public AutoHorseModule() {
        super("AutoHorse", "Shows horses and plays a sound when you take damage.", Category.FUNNY);
    }

    @Override
    public void onEnable() {
        resetState();
    }

    @Override
    public void onDisable() {
        resetState();
    }

    @Override
    public void onTick() {
        if (nullCheck()) {
            resetState();
            return;
        }

        if (activeTicks > 0) {
            activeTicks--;
            if (activeTicks == 0) {
                horses.clear();
            }
        }

        boolean hurt = mc.player.hurtTime > 0;
        if (hurt && !wasHurtLastTick) {
            triggerHorseRain();
        }

        wasHurtLastTick = hurt;
    }

    @Override
    public void onRender2D(Render2DEvent event) {
        if (horses.isEmpty()) return;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        if (screenWidth <= 0 || screenHeight <= 0) {
            return;
        }

        GuiGraphics context = event.getContext();
        for (HorseSprite horse : horses) {
            int width = Math.min(horse.width(), screenWidth);
            int height = Math.min(horse.height(), screenHeight);
            if (width <= 0 || height <= 0) {
                continue;
            }

            context.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                Math.max(0, Math.min(horse.x(), screenWidth - width)),
                Math.max(0, Math.min(horse.y(), screenHeight - height)),
                0.0f,
                0.0f,
                width,
                height,
                TEXTURE_WIDTH,
                TEXTURE_HEIGHT,
                TEXTURE_WIDTH,
                TEXTURE_HEIGHT
            );
        }
    }

    private void triggerHorseRain() {
        if (mc.getWindow() == null) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastTriggerAt < MIN_TRIGGER_INTERVAL_MS) {
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        if (screenWidth <= 0 || screenHeight <= 0) {
            return;
        }

        int imageWidth = Math.max(72, Math.round(screenWidth * scale.getValue()));
        int imageHeight = Math.max(108, Math.round(imageWidth * (TEXTURE_HEIGHT / (float) TEXTURE_WIDTH)));

        if (imageHeight > screenHeight) {
            float shrink = screenHeight / (float) imageHeight;
            imageHeight = screenHeight;
            imageWidth = Math.max(1, Math.round(imageWidth * shrink));
        }
        if (imageWidth > screenWidth) {
            imageWidth = screenWidth;
        }
        if (imageWidth <= 0 || imageHeight <= 0) {
            return;
        }

        horses.clear();
        int count = random.nextInt(3, 6);
        int maxX = Math.max(0, screenWidth - imageWidth);
        int maxY = Math.max(0, screenHeight - imageHeight);

        for (int i = 0; i < count; i++) {
            horses.add(new HorseSprite(
                random.nextInt(maxX + 1),
                random.nextInt(maxY + 1),
                imageWidth,
                imageHeight
            ));
        }

        activeTicks = duration.getValue();
        lastTriggerAt = now;
        playHorseSound();
    }

    private void resetState() {
        horses.clear();
        activeTicks = 0;
        wasHurtLastTick = false;
        lastTriggerAt = 0L;
    }

    private void playHorseSound() {
        try {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(Homovore.AUTOHORSE_SOUND, volume.getValue()));
        } catch (RuntimeException e) {
            Homovore.LOGGER.warn("Failed to play AutoHorse sound, falling back to UI click", e);
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, Math.min(volume.getValue(), 1.0f)));
        }
    }

    private record HorseSprite(int x, int y, int width, int height) {}
}
