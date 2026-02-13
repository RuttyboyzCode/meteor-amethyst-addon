package me.nyphrux.amethyst.modules;

import me.nyphrux.amethyst.Main;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.CollisionShapeEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.PlayerMoveC2SPacketAccessor;
import meteordevelopment.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.RaycastContext;

public class ProFlight extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAntiKick = settings.createGroup("Anti Kick");

    private final Setting<Double> horizontalSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("speed")
        .description("Horizontal speed in blocks per second.")
        .defaultValue(1.0)
        .min(0)
        .sliderMin(0.15)
        .sliderMax(1.75)
        .build()
    );

    private final Setting<Double> verticalSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("vertical-speed")
        .description("Vertical speed in blocks per second.")
        .defaultValue(0.75)
        .min(0)
        .sliderMin(0.25)
        .sliderMax(2.5)
        .build()
    );

    private final Setting<Double> fallSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("fall-speed")
        .description("How fast you fall in blocks per second.")
        .defaultValue(0.1)
        .min(0)
        .sliderMin(0)
        .sliderMax(1)
        .build()
    );

    private final Setting<NoFallMode> noFallMode = sgGeneral.add(new EnumSetting.Builder<NoFallMode>()
        .name("no-fall-mode")
        .description("Prevents you from getting fall damage.")
        .defaultValue(NoFallMode.Bypass)
        .build()
    );

    private final Setting<Double> bypassHeight = sgGeneral.add(new DoubleSetting.Builder()
        .name("bypass-height")
        .description("How high to teleport you before disabling the module.")
        .defaultValue(1.0)
        .sliderMin(0.1)
        .sliderMax(5)
        .visible(() -> noFallMode.get() == NoFallMode.Bypass || noFallMode.get() == NoFallMode.Extra)
        .build()
    );

    private final Setting<Boolean> noUnloadedChunks = sgGeneral.add(new BoolSetting.Builder()
        .name("no-unloaded-chunks")
        .description("Stops you from flying into unloaded chunks.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> antiKick = sgAntiKick.add(new BoolSetting.Builder()
        .name("anti-kick")
        .description("Prevents you from being kicked by the server.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> onGround = sgAntiKick.add(new BoolSetting.Builder()
        .name("on-ground")
        .description("Tells the server you're on ground when sending the anti-kick packets.")
        .defaultValue(true)
        .visible(antiKick::get)
        .build()
    );

    private final Setting<Double> antiKickDistance = sgAntiKick.add(new DoubleSetting.Builder()
        .name("anti-kick-distance")
        .description("Distance to teleport down for anti-kick (in blocks).")
        .defaultValue(0.1)
        .min(0)
        .sliderMin(0.01)
        .sliderMax(2.0)
        .visible(antiKick::get)
        .build()
    );

    private final Setting<Integer> antiKickDelay = sgAntiKick.add(new IntSetting.Builder()
        .name("anti-kick-delay")
        .description("Delay between anti-kick packets (in ticks).")
        .defaultValue(20)
        .min(1)
        .sliderMin(1)
        .sliderMax(100)
        .visible(antiKick::get)
        .build()
    );

    private final Setting<Boolean> stopOnDisable = sgAntiKick.add(new BoolSetting.Builder()
        .name("stop-on-disable")
        .description("Resets your velocity once you disable the module.")
        .defaultValue(true)
        .build()
    );

    private int antiKickTimer;

    public ProFlight() {
        super(Main.CATEGORY, "pro-flight", "Allows you to fly in survival mode.");
    }

    @Override
    public void onActivate() {
        antiKickTimer = 0;
    }

    @Override
    public void onDeactivate() {
        switch (noFallMode.get()) {
            case Bypass -> {
                if (mc.getNetworkHandler() != null && mc.player != null) {
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                        mc.player.getX(),
                        mc.player.getY() + bypassHeight.get(),
                        mc.player.getZ(),
                        onGround.get(),
                        false
                    ));
                }
            }

            case Extra -> {
                if (mc.world != null && mc.player != null && mc.getNetworkHandler() != null) {
                    BlockHitResult result = mc.world.raycast(new RaycastContext(
                        mc.player.getPos().add(0, 0.05, 0),
                        mc.player.getPos().add(0, bypassHeight.get(), 0),
                        RaycastContext.ShapeType.OUTLINE,
                        RaycastContext.FluidHandling.NONE,
                        mc.player
                    ));

                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                        mc.player.getX(),
                        result.getPos().y - (mc.player.getBoundingBox().getLengthY() / 2),
                        mc.player.getZ(),
                        onGround.get(),
                        false
                    ));
                }
            }
        }

        if (stopOnDisable.get() && mc.player != null) {
            mc.player.setVelocity(0, 0, 0);
        }
    }

    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;

        if (antiKick.get()) {
            antiKickTimer++;

            if (antiKickTimer >= antiKickDelay.get()) {
                antiKickTimer = 0;

                if (mc.getNetworkHandler() != null) {
                    BlockHitResult result = mc.world.raycast(new RaycastContext(
                        mc.player.getPos().add(0, 0.05, 0),
                        mc.player.getPos().add(0, -antiKickDistance.get(), 0),
                        RaycastContext.ShapeType.OUTLINE,
                        RaycastContext.FluidHandling.NONE,
                        mc.player
                    ));

                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                        mc.player.getX(),
                        result.getPos().y,
                        mc.player.getZ(),
                        onGround.get(),
                        false
                    ));
                }
            }
        }

        Vec3d velocity = getHorizontalVelocity(horizontalSpeed.get()*25);

        double velocityX = velocity.getX();
        double velocityZ = velocity.getZ();

        if (noUnloadedChunks.get()) {
            int currentChunkX = (int) (mc.player.getX() / 16);
            int currentChunkZ = (int) (mc.player.getZ() / 16);
            int newChunkX = (int) ((mc.player.getX() + velocityX) / 16);
            int newChunkZ = (int) ((mc.player.getZ() + velocityZ) / 16);

            if (!mc.world.getChunkManager().isChunkLoaded(currentChunkX, newChunkZ)) {
                velocityZ = 0;
            }

            if (!mc.world.getChunkManager().isChunkLoaded(newChunkX, currentChunkZ)) {
                velocityX = 0;
            }
        }

        double velocityY;
        if (mc.player.input.playerInput.jump()) {
            velocityY = verticalSpeed.get();
        } else if (mc.player.input.playerInput.sneak()) {
            velocityY = -verticalSpeed.get();
        } else {
            velocityY = -fallSpeed.get();
        }

        mc.player.setVelocity(velocityX, velocityY, velocityZ);
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (mc.player == null) return;

        if (event.packet instanceof PlayerMoveC2SPacket packet) {
            if (((IPlayerMoveC2SPacket) packet).meteor$getTag() != 1337
                && !mc.player.getAbilities().creativeMode
                && noFallMode.get() != NoFallMode.None
                && mc.player.getVelocity().getY() < 0.1) {
                ((PlayerMoveC2SPacketAccessor) packet).setOnGround(true);
            }
        }
    }

    @EventHandler
    private void onFluidCollisionShape(CollisionShapeEvent event) {
        if (event.state != null && !event.state.getFluidState().isEmpty()) {
            event.shape = VoxelShapes.empty();
        }
    }

    private Vec3d getHorizontalVelocity(double speed) {
        if (mc.player == null) return Vec3d.ZERO;

        float yaw = mc.player.getYaw();
        double diagonal = 1.0 / Math.sqrt(2);

        Vec3d forward = Vec3d.fromPolar(0, yaw);
        Vec3d right = Vec3d.fromPolar(0, yaw + 90);

        double dx = 0;
        double dz = 0;

        boolean straight = false;
        boolean sideways = false;

        if (mc.player.input.playerInput.forward()) {
            dx += forward.x / 20 * speed;
            dz += forward.z / 20 * speed;
            straight = true;
        }

        if (mc.player.input.playerInput.backward()) {
            dx -= forward.x / 20 * speed;
            dz -= forward.z / 20 * speed;
            straight = true;
        }

        if (mc.player.input.playerInput.right()) {
            dx += right.x / 20 * speed;
            dz += right.z / 20 * speed;
            sideways = true;
        }

        if (mc.player.input.playerInput.left()) {
            dx -= right.x / 20 * speed;
            dz -= right.z / 20 * speed;
            sideways = true;
        }

        if (straight && sideways) {
            dx *= diagonal;
            dz *= diagonal;
        }

        return new Vec3d(dx, 0, dz);
    }

    public enum NoFallMode {
        None,
        Normal,
        Bypass,
        Extra
    }
}
