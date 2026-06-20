package dev.leonetic.features.modules.render;

import dev.leonetic.event.impl.render.Render3DEvent;
import dev.leonetic.features.modules.Module;
import dev.leonetic.features.settings.Setting;
import dev.leonetic.util.render.RenderUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BreadcrumbsModule extends Module {

    private static final int PEARL_DELAY = 2;
    private static final double Y_OFFSET = 0.1;
    private static final double MIN_SEGMENT_SQ = 1.0e-4;

    public Setting<Boolean> self      = bool("Self", true);
    public Setting<Boolean> pearls    = bool("Pearls", true);
    public Setting<Integer> length    = num("Length", 40, 5, 200);
    public Setting<Float>   lineWidth = num("LineWidth", 2.0f, 0.5f, 5.0f);
    public Setting<Color>   color     = color("Color", 255, 255, 255, 200);
    public Setting<Boolean> fade      = bool("Fade", true);
    public Setting<Float>   fadeStart = num("FadeStart", 0.5f, 0f, 1f);

    private final List<Breadcrumb> selfCrumbs = new ArrayList<>();
    private final List<Breadcrumb> pearlCrumbs = new ArrayList<>();
    private final Map<Integer, Vec3> lastPearlSample = new HashMap<>();

    private Vec3 lastPlayerSample;

    public BreadcrumbsModule() {
        super("Breadcrumbs", "Shows movement trails for self and pearls.", Category.RENDER);
        fadeStart.setVisibility(v -> fade.getValue());
    }

    @Override
    public void onEnable() {
        reset();
    }

    @Override
    public void onDisable() {
        reset();
    }

    @Override
    public void reset() {
        selfCrumbs.clear();
        pearlCrumbs.clear();
        lastPearlSample.clear();
        lastPlayerSample = null;
    }

    @Override
    public void onTick() {
        if (nullCheck()) return;

        fade(selfCrumbs);
        fade(pearlCrumbs);

        if (self.getValue()) {
            Vec3 pos = new Vec3(mc.player.xOld, mc.player.yOld + Y_OFFSET, mc.player.zOld);
            if (lastPlayerSample != null && lastPlayerSample.distanceToSqr(pos) > MIN_SEGMENT_SQ)
                selfCrumbs.add(new Breadcrumb(lastPlayerSample, pos, length.getValue()));
            lastPlayerSample = pos;
        } else {
            lastPlayerSample = null;
        }

        if (pearls.getValue()) {
            Set<Integer> alive = new HashSet<>();
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (!(entity instanceof ThrownEnderpearl pearl)) continue;
                int id = pearl.getId();
                alive.add(id);
                Vec3 pos = new Vec3(pearl.xOld, pearl.yOld, pearl.zOld);
                Vec3 last = lastPearlSample.get(id);
                if (last != null && pearl.tickCount > PEARL_DELAY && last.distanceToSqr(pos) > MIN_SEGMENT_SQ)
                    pearlCrumbs.add(new Breadcrumb(last, pos, length.getValue()));
                lastPearlSample.put(id, pos);
            }
            lastPearlSample.keySet().retainAll(alive);
        } else {
            lastPearlSample.clear();
        }
    }

    @Override
    public void onRender3D(Render3DEvent event) {
        if (nullCheck()) return;
        Color base = color.getValue();
        float partial = event.getDelta();
        float width = lineWidth.getValue();

        for (Breadcrumb crumb : selfCrumbs)
            RenderUtil.drawLine(crumb.from, crumb.to, fade(base, crumb, partial), width);
        if (self.getValue() && lastPlayerSample != null) {
            Vec3 head = interpolate(mc.player, partial).add(0, Y_OFFSET, 0);
            RenderUtil.drawLine(lastPlayerSample, head, base, width);
        }

        for (Breadcrumb crumb : pearlCrumbs)
            RenderUtil.drawLine(crumb.from, crumb.to, fade(base, crumb, partial), width);
        if (pearls.getValue()) {
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (!(entity instanceof ThrownEnderpearl pearl)) continue;
                if (pearl.tickCount <= PEARL_DELAY) continue;
                Vec3 last = lastPearlSample.get(pearl.getId());
                if (last == null) continue;
                RenderUtil.drawLine(last, interpolate(pearl, partial), base, width);
            }
        }
    }

    private static Vec3 interpolate(Entity e, float partial) {
        return new Vec3(
                Mth.lerp(partial, e.xOld, e.getX()),
                Mth.lerp(partial, e.yOld, e.getY()),
                Mth.lerp(partial, e.zOld, e.getZ())
        );
    }

    private Color fade(Color base, Breadcrumb b, float partial) {
        if (!fade.getValue()) return base;
        float life = (b.age + partial) / b.lifeTime;

        float start = fadeStart.getValue();
        float progress = (life - start) / (1f - start);
        if (progress < 0f) progress = 0f;
        if (progress > 1f) progress = 1f;
        int a = Math.round(base.getAlpha() * (1f - progress));
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), a);
    }

    private void fade(List<Breadcrumb> list) {
        Iterator<Breadcrumb> it = list.iterator();
        while (it.hasNext()) {
            Breadcrumb b = it.next();
            if (++b.age >= b.lifeTime) it.remove();
        }
    }

    private static class Breadcrumb {
        final Vec3 from;
        final Vec3 to;
        final int lifeTime;
        int age;
        Breadcrumb(Vec3 from, Vec3 to, int lifeTime) {
            this.from = from;
            this.to = to;
            this.lifeTime = lifeTime;
        }
    }
}
