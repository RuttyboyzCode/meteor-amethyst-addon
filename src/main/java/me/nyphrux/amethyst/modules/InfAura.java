package me.nyphrux.amethyst.modules;

import me.nyphrux.amethyst.Main;
import me.nyphrux.amethyst.util.ModuleCreditsIntergration;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.render.WireframeEntityRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.renderer.ShapeMode;

import java.util.Set;

public class InfAura extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTargeting = settings.createGroup("Targeting");
    private final SettingGroup sgWeapon = settings.createGroup("Weapon");
    private final SettingGroup sgRender = settings.createGroup("Render");
    private Vec3d lastTargetPos = null;

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("Attack range.")
        .defaultValue(100)
        .min(6)
        .max(200)
        .build()
    );

    private final Setting<Integer> cooldown = sgGeneral.add(new IntSetting.Builder()
        .name("cooldown")
        .description("Ticks between attacks.")
        .defaultValue(20)
        .min(10)
        .max(40)
        .build()
    );

    private final Setting<Boolean> criticals = sgGeneral.add(new BoolSetting.Builder()
        .name("criticals")
        .description("Attempt to perform critical hits.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Set<EntityType<?>>> entities = sgTargeting.add(new EntityTypeListSetting.Builder()
        .name("entities")
        .description("Entities to attack.")
        .onlyAttackable()
        .defaultValue(EntityType.PLAYER)
        .build()
    );

    private final Setting<Boolean> ignoreTamed = sgTargeting.add(new BoolSetting.Builder()
        .name("ignore-tamed")
        .description("Don't attack tamed mobs.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("Color of the line to the target.")
        .defaultValue(new SettingColor(255, 50, 50, 180))
        .build()
    );

    private final Setting<Boolean> targetEsp = sgRender.add(new BoolSetting.Builder()
        .name("target-esp")
        .description("Render ESP on target.")
        .defaultValue(true)
        .build()
    );

    private final Setting<TargetEspMode> espMode = sgRender.add(new EnumSetting.Builder<TargetEspMode>()
        .name("esp-mode")
        .description("How to render ESP.")
        .defaultValue(TargetEspMode.Wireframe)
        .visible(targetEsp::get)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("ESP shape mode.")
        .defaultValue(ShapeMode.Lines)
        .visible(() -> targetEsp.get() && espMode.get() != TargetEspMode.Glow)
        .build()
    );

    private final Setting<Double> fillOpacity = sgRender.add(new DoubleSetting.Builder()
        .name("fill-opacity")
        .description("Opacity for box fill.")
        .defaultValue(0.25)
        .range(0, 1)
        .sliderMax(1)
        .visible(() -> espMode.get() == TargetEspMode.Box && shapeMode.get() != ShapeMode.Lines)
        .build()
    );

    private final Setting<SettingColor> targetEspColor = sgRender.add(new ColorSetting.Builder()
        .name("esp-color")
        .description("Target ESP color.")
        .defaultValue(new SettingColor(255, 0, 0, 255))
        .visible(targetEsp::get)
        .build()
    );

    private final Setting<Boolean> silentSwap = sgWeapon.add(new BoolSetting.Builder()
        .name("silent-swap")
        .description("Silently swaps to the best weapon when attacking.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> swordOnly = sgWeapon.add(new BoolSetting.Builder()
        .name("sword-only")
        .description("Only uses swords for attacking.")
        .defaultValue(false)
        .visible(silentSwap::get)
        .build()
    );

    private final Setting<Boolean> swapBack = sgWeapon.add(new BoolSetting.Builder()
        .name("swap-back")
        .description("Swaps back to previous slot after attacking.")
        .defaultValue(true)
        .visible(silentSwap::get)
        .build()
    );

    private int attackTimer;
    private Vec3d originalPos;
    private boolean isAttacking;
    private static final double STEP_DISTANCE = 5.0;
    private int prevSlot = -1;
    private long lastAttackTime = 0;
    private static final long LINE_DISPLAY_TIME_MS = 1500;
    private Entity lastTargetEntity = null;

    public InfAura() {
        super(Main.CATEGORY, "inf-aura", "Attacks entities from any distance.");
        ((ModuleCreditsIntergration)this).setCredits("Ruttyboyz (Making the module)");
    }

    @Override
    public void onDeactivate() {
        if (isAttacking && mc.player != null && originalPos != null) {
            safeReturnToPos();
        }
        isAttacking = false;
        if (prevSlot != -1 && swapBack.get()) {
            mc.player.getInventory().selectedSlot = prevSlot;
            prevSlot = -1;
        }
    }

    private int findBestWeapon() {
        int bestSlot = mc.player.getInventory().selectedSlot;
        float bestDamage = 0;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;

            if (swordOnly.get() && !stack.isIn(ItemTags.SWORDS)) continue;

            float damage = 0;
            if (stack.isIn(ItemTags.SWORDS)) damage = 7;
            else if (stack.getItem() instanceof AxeItem) damage = 9;

            if (damage > bestDamage) {
                bestDamage = damage;
                bestSlot = i;
            }
        }

        return bestSlot;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null || isAttacking) return;

        if (attackTimer > 0) {
            attackTimer--;
            return;
        }

        if (mc.player.getAttackCooldownProgress(0) < 1.0f) return;

        Entity target = findTarget();
        if (target != null) {
            originalPos = mc.player.getPos();
            attackEntity(target);
            attackTimer = cooldown.get();
        }
    }

    private Entity findTarget() {
        Entity closest = null;
        double closestDist = range.get();

        for (Entity entity : mc.world.getEntities()) {
            if (!isValidTarget(entity)) continue;

            double dist = mc.player.getPos().distanceTo(entity.getPos());
            if (dist < closestDist) {
                closestDist = dist;
                closest = entity;
            }
        }

        return closest;
    }

    private boolean isValidTarget(Entity entity) {
        if (entity == null || entity == mc.player || !entity.isAlive()) return false;
        if (!entities.get().contains(entity.getType())) return false;
        if (mc.player.getPos().distanceTo(entity.getPos()) > range.get()) return false;
        if (entity instanceof PlayerEntity && shouldSkipPlayer((PlayerEntity) entity)) return false;
        if (ignoreTamed.get() && entity instanceof net.minecraft.entity.passive.TameableEntity tameable && tameable.isTamed()) {
            return false;
        }
        return true;
    }

    private boolean shouldSkipPlayer(PlayerEntity player) {
        return !Friends.get().shouldAttack(player);
    }

    private void attackEntity(Entity target) {
        isAttacking = true;
        Vec3d targetPos = target.getPos();
        lastTargetPos = target.getPos();
        lastAttackTime = System.currentTimeMillis();
        lastTargetEntity = target;


        Vec3d currentPos = mc.player.getPos();
        double horizontalDist = Math.sqrt(
            Math.pow(targetPos.x - currentPos.x, 2) +
                Math.pow(targetPos.z - currentPos.z, 2)
        );

        if (horizontalDist > STEP_DISTANCE) {
            int steps = (int) Math.ceil(horizontalDist / STEP_DISTANCE);
            for (int i = 1; i <= steps; i++) {
                double progress = (double) i / steps;
                double x = MathHelper.lerp(progress, currentPos.x, targetPos.x);
                double z = MathHelper.lerp(progress, currentPos.z, targetPos.z);

                if (!safeTeleportTo(x, currentPos.y, z)) {
                    isAttacking = false;
                    return;
                }
            }
        }

        if (safeTeleportTo(targetPos.x, targetPos.y, targetPos.z)) {
            if (criticals.get()) {
                safeTeleportTo(targetPos.x, targetPos.y + 0.11, targetPos.z);
                safeTeleportTo(targetPos.x, targetPos.y, targetPos.z);
            }

            if (silentSwap.get()) {
                prevSlot = mc.player.getInventory().selectedSlot;
                int weaponSlot = findBestWeapon();
                mc.player.getInventory().selectedSlot = weaponSlot;
            }

            mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
            mc.player.swingHand(Hand.MAIN_HAND);
            if (silentSwap.get() && swapBack.get() && prevSlot != -1) {
                mc.player.getInventory().selectedSlot = prevSlot;
                prevSlot = -1;
            }

            Vec3d returnPos = originalPos;
            horizontalDist = Math.sqrt(
                Math.pow(returnPos.x - targetPos.x, 2) +
                    Math.pow(returnPos.z - targetPos.z, 2)
            );

            if (horizontalDist > STEP_DISTANCE) {
                int steps = (int) Math.ceil(horizontalDist / STEP_DISTANCE);
                for (int i = 1; i <= steps; i++) {
                    double progress = (double) i / steps;
                    double x = MathHelper.lerp(progress, targetPos.x, returnPos.x);
                    double z = MathHelper.lerp(progress, targetPos.z, returnPos.z);

                    if (!safeTeleportTo(x, returnPos.y, z)) break;
                }
            }

            safeTeleportTo(returnPos.x, returnPos.y, returnPos.z);
        }

        isAttacking = false;
    }

    private boolean safeTeleportTo(double x, double y, double z) {
        try {
            sendPositionPacket(x, y, z, true);
            mc.player.setPos(x, y, z);
            mc.player.setVelocity(0, 0, 0);
            Thread.sleep(1);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void safeReturnToPos() {
        if (originalPos != null) {
            sendPositionPacket(originalPos.x, originalPos.y, originalPos.z, true);
            mc.player.setPos(originalPos.x, originalPos.y, originalPos.z);
            mc.player.setVelocity(0, 0, 0);
            mc.player.setOnGround(true);
        }
    }
    @EventHandler
    private void onRender(Render3DEvent event) {
        if (lastTargetEntity == null || !lastTargetEntity.isAlive()) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAttackTime > LINE_DISPLAY_TIME_MS) {
            lastTargetEntity = null;
            return;
        }

        Vec3d playerPos = mc.player.getPos().subtract(0, 0.1, 0);
        Vec3d targetPos = lastTargetEntity.getPos();
        int start = event.renderer.lines.vec3(playerPos.x, playerPos.y, playerPos.z).color(lineColor.get()).next();
        int end = event.renderer.lines.vec3(targetPos.x, targetPos.y, targetPos.z).color(lineColor.get()).next();
        event.renderer.lines.line(start, end);

        if (!targetEsp.get()) return;

        SettingColor sc = targetEspColor.get();
        Color color = new Color(sc.r, sc.g, sc.b, sc.a);
        Color sideColor = new Color(sc.r, sc.g, sc.b, (int) (sc.a * fillOpacity.get()));

        switch (espMode.get()) {
            case Wireframe -> {
                WireframeEntityRenderer.render(event, lastTargetEntity, 1.0, color, color, shapeMode.get());
            }
            case Box -> {
                Box box = lastTargetEntity.getBoundingBox();
                double x = lastTargetEntity.getX() - lastTargetEntity.getX();
                double y = lastTargetEntity.getY() - lastTargetEntity.getY();
                double z = lastTargetEntity.getZ() - lastTargetEntity.getZ();
                event.renderer.box(
                    box.minX + x, box.minY + y, box.minZ + z,
                    box.maxX + x, box.maxY + y, box.maxZ + z,
                    sideColor, color, shapeMode.get(), 0
                );
            }
            case Glow -> {
                lastTargetEntity.setGlowing(true);
            }
        }
    }

    private void sendPositionPacket(double x, double y, double z, boolean onGround) {
        try {
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, onGround, false));
        } catch (Throwable t) {
            System.out.println("[InfAura] Failed to send packet: " + t);
        }
    }

    public enum TargetEspMode {
        Wireframe,
        Box,
        Glow
    }
}

