package me.nyphrux.amethyst.modules;

import me.nyphrux.amethyst.Main;
import me.nyphrux.amethyst.util.ModuleCreditsIntergration;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class ScaffoldPro extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlace = settings.createGroup("Placing");
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<ListMode> listMode = sgGeneral.add(new EnumSetting.Builder<ListMode>()
        .name("list-mode")
        .description("How to use the block list.")
        .defaultValue(ListMode.Whitelist)
        .build()
    );

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("Placement mode.")
        .defaultValue(Mode.Simple)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Rotate to block placement position.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> airPlace = sgGeneral.add(new BoolSetting.Builder()
        .name("air-place")
        .description("Place blocks in air without adjacent blocks.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> onlyOnMove = sgGeneral.add(new BoolSetting.Builder()
        .name("only-on-move")
        .description("Only place blocks when moving.")
        .defaultValue(false)
        .build()
    );

    private final Setting<List<Block>> blocks = sgPlace.add(new BlockListSetting.Builder()
        .name("blocks")
        .description("Blocks to whitelist or blacklist.")
        .defaultValue(Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN)
        .build()
    );

    private final Setting<Integer> delay = sgPlace.add(new IntSetting.Builder()
        .name("delay-ticks")
        .description("Delay between placements in ticks.")
        .defaultValue(0)
        .min(0)
        .sliderMax(10)
        .build()
    );

    private final Setting<Integer> blocksPerTick = sgPlace.add(new IntSetting.Builder()
        .name("blocks-per-tick")
        .description("Maximum blocks to place per tick.")
        .defaultValue(3)
        .min(1)
        .sliderMax(10)
        .build()
    );

    private final Setting<Double> placeRange = sgPlace.add(new DoubleSetting.Builder()
        .name("place-range")
        .description("Maximum range for block placement.")
        .defaultValue(4.5)
        .min(0)
        .sliderMax(6)
        .build()
    );

    private final Setting<Boolean> autoSwitch = sgPlace.add(new BoolSetting.Builder()
        .name("auto-switch")
        .description("Automatically switch to scaffold blocks.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How to render placement positions.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("Color of the outline.")
        .defaultValue(new SettingColor(255, 0, 0, 255))
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("Color of the fill.")
        .defaultValue(new SettingColor(255, 0, 0, 40))
        .build()
    );

    private int placeCooldown;
    private final List<BlockPos> renderPositions = new ArrayList<>();

    public ScaffoldPro() {
        super(Main.CATEGORY, "scaffold-pro", "Place blocks below you when walking in air.");
        ((ModuleCreditsIntergration)this).setCredits("None.");
    }

    @Override
    public void onDeactivate() {
        placeCooldown = 0;
        renderPositions.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        renderPositions.clear();

        if (placeCooldown > 0) {
            placeCooldown--;
            return;
        }

        if (onlyOnMove.get()) {
            boolean isMoving = mc.player.input.movementForward != 0 || mc.player.input.movementSideways != 0;
            if (!isMoving) return;
        }

        FindItemResult blockItem = findValidBlock();
        if (!blockItem.found()) return;

        List<BlockPos> positions = getPlacementPositions();
        if (positions.isEmpty()) return;

        int placed = 0;
        for (BlockPos pos : positions) {
            if (placed >= blocksPerTick.get()) break;
            if (mc.player.getPos().distanceTo(Vec3d.ofCenter(pos)) > placeRange.get()) continue;

            if (!isValidPlacementPosition(pos)) continue;
            Direction side = getPlacementSide(pos);
            if (side == null && !airPlace.get()) continue;

            if (placeBlock(pos, side, blockItem)) {
                placed++;
                renderPositions.add(pos);
                placeCooldown = delay.get();
            }
        }
    }

    private FindItemResult findValidBlock() {
        return InvUtils.findInHotbar(i -> {
            if (!(i.getItem() instanceof BlockItem)) return false;
            Block block = ((BlockItem) i.getItem()).getBlock();

            if (listMode.get() == ListMode.Whitelist) {
                return blocks.get().contains(block);
            } else {
                return !blocks.get().contains(block);
            }
        });
    }

    private List<BlockPos> getPlacementPositions() {
        List<BlockPos> positions = new ArrayList<>();

        switch (mode.get()) {
            case Simple -> {
                positions.add(mc.player.getBlockPos().down());
            }
            case Extended -> {
                BlockPos center = mc.player.getBlockPos().down();
                positions.add(center);

                Vec3d velocity = mc.player.getVelocity();
                if (velocity.horizontalLength() > 0.1) {
                    double yaw = Math.toRadians(mc.player.getYaw());
                    int offsetX = (int) Math.round(-Math.sin(yaw));
                    int offsetZ = (int) Math.round(Math.cos(yaw));
                    positions.add(center.add(offsetX, 0, offsetZ));
                }
            }
            case Tower -> {
                BlockPos center = mc.player.getBlockPos().down();
                for (int i = 0; i < 3; i++) {
                    positions.add(center.down(i));
                }
            }
        }

        positions.removeIf(pos -> !isValidPlacementPosition(pos));

        return positions;
    }

    private boolean isValidPlacementPosition(BlockPos pos) {
        if (mc.world == null || mc.player == null) return false;

        if (!mc.world.getBlockState(pos).isReplaceable()) return false;

        Box playerBox = mc.player.getBoundingBox();
        Box blockBox = new Box(pos);
        return !playerBox.intersects(blockBox);
    }

    private Direction getPlacementSide(BlockPos pos) {
        Direction[] priorities = {Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP};

        for (Direction dir : priorities) {
            BlockPos neighbor = pos.offset(dir);
            if (mc.world.getBlockState(neighbor).isSolidBlock(mc.world, neighbor)) {
                return dir.getOpposite();
            }
        }
        return null;
    }

    private boolean placeBlock(BlockPos pos, Direction side, FindItemResult blockItem) {
        if (mc.player == null || mc.world == null) return false;

        int previousSlot = mc.player.getInventory().selectedSlot;
        if (autoSwitch.get()) {
            InvUtils.swap(blockItem.slot(), false);
        } else if (mc.player.getInventory().selectedSlot != blockItem.slot()) {
            return false;
        }

        final boolean[] placed = {false};

        if (side != null) {
            BlockPos neighbor = pos.offset(side.getOpposite());
            Vec3d hitVec = Vec3d.ofCenter(neighbor).add(Vec3d.of(side.getVector()).multiply(0.5));

            if (rotate.get()) {
                double yaw = Rotations.getYaw(hitVec);
                double pitch = Rotations.getPitch(hitVec);
                Rotations.rotate(yaw, pitch, () -> {
                    placed[0] = BlockUtils.place(neighbor, blockItem, false, 0, true);
                });
            } else {
                placed[0] = BlockUtils.place(neighbor, blockItem, false, 0, true);
            }
        } else if (airPlace.get()) {
            Vec3d hitVec = Vec3d.ofCenter(pos);

            if (rotate.get()) {
                double yaw = Rotations.getYaw(hitVec);
                double pitch = Rotations.getPitch(hitVec);
                Rotations.rotate(yaw, pitch, () -> {
                    placed[0] = BlockUtils.place(pos, blockItem, false, 0, true);
                });
            } else {
                placed[0] = BlockUtils.place(pos, blockItem, false, 0, true);
            }
        }

        if (autoSwitch.get()) {
            InvUtils.swap(previousSlot, false);
        }

        return placed[0];
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.player == null || mc.world == null) return;

        for (BlockPos pos : renderPositions) {
            event.renderer.box(
                pos,
                sideColor.get(),
                lineColor.get(),
                shapeMode.get(),
                0
            );
        }

        if (renderPositions.isEmpty()) {
            List<BlockPos> positions = getPlacementPositions();
            for (BlockPos pos : positions) {
                if (isValidPlacementPosition(pos)) {
                    event.renderer.box(
                        pos,
                        sideColor.get(),
                        lineColor.get(),
                        shapeMode.get(),
                        0
                    );
                }
            }
        }
    }

    public enum Mode {
        Simple,
        Extended,
        Tower
    }

    public enum ListMode {
        Whitelist,
        Blacklist
    }
}
