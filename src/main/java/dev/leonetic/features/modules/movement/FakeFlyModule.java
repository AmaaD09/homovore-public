package dev.leonetic.features.modules.movement;

import dev.leonetic.Homovore;
import dev.leonetic.event.impl.entity.player.PreTickEvent;
import dev.leonetic.event.impl.entity.player.TickEvent;
import dev.leonetic.event.impl.network.PacketEvent;
import dev.leonetic.event.impl.render.Render2DEvent;
import dev.leonetic.event.system.Subscribe;
import dev.leonetic.features.modules.Module;
import dev.leonetic.features.settings.Setting;
import dev.leonetic.manager.RotationRequest;
import dev.leonetic.util.inventory.InventoryUtil;
import dev.leonetic.util.inventory.Result;
import dev.leonetic.util.inventory.ResultType;
import dev.leonetic.util.models.Timer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import static dev.leonetic.util.inventory.InventoryUtil.FULL_SCOPE;

public class FakeFlyModule extends Module {

    public static final String ID = "FakeFly";
    private static final int PRIORITY = 55;
    private static final int CHEST_MENU_SLOT = 6;
    private static final int OFFHAND_MENU_SLOT = 45;

    public enum Mode { Legit, GrimDurability }
    public enum FireworkMode { Auto, Delay, None }
    public enum HoverMode { Off, On, Freeze }

    private final Setting<Mode> mode = mode("Mode", Mode.GrimDurability);
    private final Setting<FireworkMode> fireworkMode = mode("FireworkMode", FireworkMode.Auto);
    private final Setting<Double> packetDelay = num("PacketDelay", 3.0, 0.0, 100.0);
    private final Setting<Boolean> unbreaking = bool("Unbreaking", true);
    private final Setting<Double> fakeDelay = num("FakeDelay", 800.0, 0.0, 3000.0);
    private final Setting<Boolean> releaseSneak = bool("ReleaseSneak", true);
    private final Setting<Double> releaseDelayMs = num("ReleaseDelay", 100.0, 0.0, 1000.0);
    private final Setting<Double> fireworkDelay = num("FireworkDelay", 1000.0, 0.0, 3000.0);
    private final Setting<Boolean> checkFirework = bool("CheckFirework", true);
    private final Setting<Boolean> inventorySwap = bool("InventorySwap", true);
    private final Setting<Boolean> control = bool("Control", true);
    private final Setting<Double> fallSpeed = num("FallSpeed", 0.02, 0.0, 3.0);
    private final Setting<HoverMode> hover = mode("Hover", HoverMode.Off);
    private final Setting<Integer> packetGap = num("PacketGap", 20, 1, 100);
    private final Timer fireworkTimer = new Timer();
    private final Timer swapTimer = new Timer();
    private int packetDelayCounter = 0;
    private double frozenX = Double.NaN;
    private double frozenY = Double.NaN;
    private double frozenZ = Double.NaN;
    private int ticksSincePacket = 0;

    public FakeFlyModule() {
        super("FakeFly", "Firework elytra flight with GrimAC durability bypass.", Category.MOVEMENT);
        fireworkDelay.setVisibility(v -> fireworkMode.getValue() == FireworkMode.Delay);
        fakeDelay.setVisibility(v -> mode.getValue() == Mode.Legit && unbreaking.getValue());
        releaseDelayMs.setVisibility(v -> releaseSneak.getValue());
        packetDelay.setVisibility(v -> mode.getValue() == Mode.GrimDurability);
        control.setVisibility(v -> mode.getValue() == Mode.GrimDurability);
        fallSpeed.setVisibility(v -> mode.getValue() == Mode.GrimDurability && control.getValue());
        packetGap.setVisibility(v -> hover.getValue() == HoverMode.Freeze);
    }

    @Override
    public void onEnable() {
        fireworkTimer.setMs(99999);
        swapTimer.setMs(99999);
        packetDelayCounter = 0;
        frozenX = frozenY = frozenZ = Double.NaN;
        ticksSincePacket = 0;
    }

    @Override
    public void onDisable() {
        Homovore.TIMER = 1f;
        Homovore.TIMER_MOVEMENT_ONLY = false;
        frozenX = frozenY = frozenZ = Double.NaN;
        if (mc.player == null) return;
        if (releaseSneak.getValue()) {
            long delay = releaseDelayMs.getValue().longValue();
            java.util.Timer timer = new java.util.Timer();
            timer.schedule(new java.util.TimerTask() {
                @Override
                public void run() {
                    mc.execute(() -> mc.options.keyShift.setDown(false));
                }
            }, delay);
        }
    }

    @Subscribe
    private void onPreTick(PreTickEvent event) {
        if (nullCheck()) return;

        packetDelayCounter++;

        float realYaw = mc.player.getYRot();
        float yaw = computeYaw(realYaw);
        float pitch = computePitch(mc.player.getXRot());

        if (mode.getValue() == Mode.GrimDurability) {
            Homovore.rotationManager.submit(new RotationRequest(
                    ID, PRIORITY, yaw, Mth.clamp(pitch, -90f, 90f), RotationRequest.Mode.MOTION, false, true));
        }

        boolean hasFirework = false;
        if (checkFirework.getValue()) {
            for (var entity : mc.level.getEntities(null, mc.player.getBoundingBox().inflate(64))) {
                if (entity instanceof FireworkRocketEntity fw && fw.getOwner() == mc.player) {
                    hasFirework = true;
                    break;
                }
            }
        }

        boolean wearingElytra = mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA);

        if (wearingElytra && !mc.player.isFallFlying() && !mc.player.onGround() && mc.getConnection() != null) {
            mc.getConnection().send(new ServerboundPlayerCommandPacket(
                    mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
            mc.player.startFallFlying();
        }

        boolean isHovering = hover.getValue() == HoverMode.On && !wantToMove() && mc.player.isFallFlying();

        if (mode.getValue() == Mode.Legit) {
            tickLegit(wearingElytra, hasFirework, isHovering);
        } else {
            tickGrimDurability(hasFirework, isHovering);
        }

        if (hover.getValue() == HoverMode.Freeze) {
            if (wantToMove() || mc.player.onGround()) {
                frozenX = frozenY = frozenZ = Double.NaN;
            } else if (Double.isNaN(frozenY)) {
                frozenX = mc.player.getX();
                frozenY = mc.player.getY();
                frozenZ = mc.player.getZ();
            }

            if (!Double.isNaN(frozenY)) {
                mc.player.setDeltaMovement(0, 0, 0);
                mc.player.setPos(frozenX, frozenY, frozenZ);
            }
        } else {
            frozenX = frozenY = frozenZ = Double.NaN;
        }
    }

    @Subscribe
    private void onTick(TickEvent event) {
        if (nullCheck()) return;

        if (!mc.player.isFallFlying()) {
            if (hover.getValue() == HoverMode.On) {
                Homovore.TIMER = 1f;
                Homovore.TIMER_MOVEMENT_ONLY = false;
            }
            return;
        }

        boolean moving = wantToMove();

        Homovore.TIMER_MOVEMENT_ONLY = false;
        if (hover.getValue() == HoverMode.On && !moving) {
            Homovore.TIMER = 0.05f;
        } else {
            Homovore.TIMER = 1f;
        }

        if (mode.getValue() != Mode.GrimDurability || !control.getValue()) return;
        if (mc.screen != null) return;
        if (!moving) {
            mc.player.setDeltaMovement(0, -fallSpeed.getValue(), 0);
        }
    }

    @Subscribe
    private void onRender2DHover(Render2DEvent event) {
        if (nullCheck()) return;
        if (hover.getValue() != HoverMode.On) return;
        if (!mc.player.isFallFlying()) return;
        if (wantToMove()) {
            Homovore.TIMER = 1f;
            Homovore.TIMER_MOVEMENT_ONLY = false;
        }
    }

    @Subscribe
    private void onPacketSend(PacketEvent.Send event) {
        if (nullCheck() || hover.getValue() != HoverMode.Freeze || Double.isNaN(frozenY)) return;
        if (!(event.getPacket() instanceof ServerboundMovePlayerPacket pkt)) return;
        if (!pkt.hasPosition()) return;

        ticksSincePacket++;
        event.cancel();

        if (ticksSincePacket >= packetGap.getValue()) {
            ticksSincePacket = 0;
            boolean onGround = mc.player.onGround();
            boolean hc = mc.player.horizontalCollision;
            mc.player.connection.send(new ServerboundMovePlayerPacket.PosRot(
                    frozenX,
                    frozenY,
                    frozenZ,
                    pkt.getYRot(mc.player.getYRot()),
                    pkt.getXRot(mc.player.getXRot()),
                    onGround,
                    hc
            ));
        }
    }

    private void tickLegit(boolean wearingElytra, boolean hasFirework, boolean isHovering) {
        if (wearingElytra && mc.player.isFallFlying() && unbreaking.getValue()
                && swapTimer.passedMs(fakeDelay.getValue().longValue()) && canClickInventory()) {
            InventoryUtil.click(CHEST_MENU_SLOT, 0, ClickType.PICKUP);
            InventoryUtil.click(CHEST_MENU_SLOT, 0, ClickType.PICKUP);
            if (mc.getConnection() != null) {
                mc.getConnection().send(new ServerboundPlayerCommandPacket(
                        mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
                mc.player.startFallFlying();
            }
            swapTimer.reset();
        }

        if (wearingElytra && mc.player.isFallFlying() && !isHovering) {
            if (fireworkMode.getValue() == FireworkMode.Auto && !hasFirework) {
                useFirework();
            } else if (fireworkMode.getValue() == FireworkMode.Delay && wantToMove()
                    && (!checkFirework.getValue() || !hasFirework)) {
                useFirework();
            }
        }
    }

    private void tickGrimDurability(boolean hasFirework, boolean isHovering) {
        if (!canClickInventory()) return;
        Result elytra = InventoryUtil.find(Items.ELYTRA, FULL_SCOPE);
        if (!elytra.found() || packetDelayCounter <= packetDelay.getValue().intValue()) return;

        int elytraSlot = containerSlot(elytra);

        InventoryUtil.click(elytraSlot, 0, ClickType.PICKUP);
        InventoryUtil.click(CHEST_MENU_SLOT, 0, ClickType.PICKUP);
        InventoryUtil.click(elytraSlot, 0, ClickType.PICKUP);

        if (!mc.player.onGround() && mc.getConnection() != null) {
            mc.getConnection().send(new ServerboundPlayerCommandPacket(
                    mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
            mc.player.startFallFlying();
        }

        if (!isHovering) {
            if (fireworkMode.getValue() == FireworkMode.Auto && !hasFirework) {
                useFirework();
            } else if (fireworkMode.getValue() == FireworkMode.Delay && wantToMove()
                    && (!checkFirework.getValue() || !hasFirework)) {
                useFirework();
            }
        }

        InventoryUtil.click(elytraSlot, 0, ClickType.PICKUP);
        InventoryUtil.click(CHEST_MENU_SLOT, 0, ClickType.PICKUP);
        InventoryUtil.click(elytraSlot, 0, ClickType.PICKUP);

        packetDelayCounter = 0;
    }

    private void useFirework() {
        if (fireworkMode.getValue() == FireworkMode.Delay
                && !fireworkTimer.passedMs(fireworkDelay.getValue().longValue())) return;

        if (mc.player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.FIREWORK_ROCKET)) {
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            fireworkTimer.reset();
        } else if (mc.player.getItemInHand(InteractionHand.OFF_HAND).is(Items.FIREWORK_ROCKET)) {
            mc.gameMode.useItem(mc.player, InteractionHand.OFF_HAND);
            fireworkTimer.reset();
        } else if (inventorySwap.getValue()) {
            Result fw = InventoryUtil.find(Items.FIREWORK_ROCKET, FULL_SCOPE);
            if (!fw.found() || !canClickInventory()) return;
            int fwSlot = containerSlot(fw);
            int selected = InventoryUtil.selected();
            InventoryUtil.click(fwSlot, selected, ClickType.SWAP);
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            InventoryUtil.click(fwSlot, selected, ClickType.SWAP);
            fireworkTimer.reset();
        }
    }

    private float computeYaw(float yaw) {
        boolean forward = mc.options.keyUp.isDown();
        boolean back = mc.options.keyDown.isDown();
        boolean left = mc.options.keyLeft.isDown();
        boolean right = mc.options.keyRight.isDown();

        if (forward && !back) {
            if (left && !right) yaw -= 45f;
            else if (right && !left) yaw += 45f;
        } else if (back && !forward) {
            yaw += 180f;
            if (left && !right) yaw += 45f;
            else if (right && !left) yaw -= 45f;
        } else if (left && !right) {
            yaw -= 90f;
        } else if (right && !left) {
            yaw += 90f;
        }
        return yaw;
    }

    private float computePitch(float pitch) {
        boolean up = mc.options.keyJump.isDown();
        boolean down = mc.options.keyShift.isDown();
        boolean moving = mc.options.keyUp.isDown() || mc.options.keyDown.isDown()
                || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown();

        if (up && down) return -3f;
        if (up)   return moving ? -45f : -90f;
        if (down) return moving ? 45f  :  90f;
        if (moving) return -1.9f;
        return pitch;
    }

    private boolean wantToMove() {
        return mc.options.keyUp.isDown() || mc.options.keyDown.isDown()
                || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown()
                || mc.options.keyJump.isDown() || mc.options.keyShift.isDown();
    }

    private boolean canClickInventory() {
        return mc.gameMode != null && mc.player.containerMenu == mc.player.inventoryMenu;
    }

    private int containerSlot(Result result) {
        if (result.type() == ResultType.OFFHAND) return OFFHAND_MENU_SLOT;
        int slot = result.slot();
        return slot < 9 ? slot + 36 : slot;
    }

    @Override
    public String getDisplayInfo() {
        if (mc.player == null) return null;
        int count = 0;
        if (inventorySwap.getValue()) {
            if (mc.player.getOffhandItem().is(Items.FIREWORK_ROCKET))
                count += mc.player.getOffhandItem().getCount();
            for (int i = 0; i < 36; i++) {
                if (mc.player.getInventory().getItem(i).is(Items.FIREWORK_ROCKET))
                    count += mc.player.getInventory().getItem(i).getCount();
            }
        } else {
            for (int i = 0; i < 9; i++) {
                if (mc.player.getInventory().getItem(i).is(Items.FIREWORK_ROCKET))
                    count += mc.player.getInventory().getItem(i).getCount();
            }
        }
        return "F:" + count;
    }
}
