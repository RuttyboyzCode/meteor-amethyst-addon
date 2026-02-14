package me.nyphrux.amethyst.modules;

import me.nyphrux.amethyst.Main;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.block.BlockState;
import net.minecraft.util.Hand;
import net.minecraft.item.ItemStack;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.List;
import java.util.ArrayList;

public class ProScaffold extends Module {

    private final SettingGroup sgGeneral  = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("blocks")
        .description("The blocks to use")
        .defaultValue()
        .build()
    );

    private final Setting<Mode> yMode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("Mode")
        .description("The mode to use for Y level.")
        .defaultValue(Mode.FIRST_PLACE)
        .build()
    );

    private final Setting<Integer> level = sgGeneral.add(new IntSetting.Builder()
        .name("Y level")
        .description("The Y level to place blocks")
        .defaultValue(64)
        .sliderRange(-64, 319)
        .visible(() -> yMode.get() == Mode.STATIC)
        .build()
    );

    private final Setting<Integer> radius = sgGeneral.add(new IntSetting.Builder()
        .name("radius")
        .description("The radius of the block placing")
        .defaultValue(3)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("The delay in between places.")
        .defaultValue(1)
        .build()
    );

    private final Setting<Integer> bpt = sgGeneral.add(new IntSetting.Builder()
        .name("blocks pet tick")
        .description("The maximum amount of blocks to place each tick.")
        .defaultValue(2)
        .sliderRange(0, 20)
        .build()
    );

    private final Setting<Boolean> airPlace = sgGeneral.add(new BoolSetting.Builder()
        .name("air place")
        .description("Places blocks with no support.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Renders the blocks that will be placed.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .visible(render::get)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The side color of the target block rendering.")
        .defaultValue(new SettingColor(197, 137, 232, 10))
        .visible(render::get)
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color of the target block rendering.")
        .defaultValue(new SettingColor(197, 137, 232))
        .visible(render::get)
        .build()
    );

    private final Setting<Integer> fadeTime = sgRender.add(new IntSetting.Builder()
        .name("fade-time")
        .description("How long placed blocks stay rendered.")
        .defaultValue(10)
        .sliderRange(1, 40)
        .visible(render::get)
        .build()
    );

    private int timer = 0;
    private int firstPlace;
    private final List<FadeBlock> fadeBlocks = new ArrayList<>();

    public ProScaffold() {
        super(Main.CATEGORY, "pro-scaffold", "An improved scaffold that can place blocks in a radius around you.");
    }

    public void onActivate() {
        firstPlace = mc.player.getBlockPos().getY() - 1;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (timer >= delay.get()) {
            timer = 0;
        } else {
            timer++;
            return;
        }
        int blockCount = 0;
        for (Block block : blocks.get()) {
            FindItemResult result = InvUtils.find(block.asItem());
            blockCount = blockCount + result.count();
        }
        if (blockCount == 0) return;

        ItemStack handStack = mc.player.getMainHandStack();
        int blocksToPlace = 0;
        int slot = mc.player.getInventory().selectedSlot;

        if (blocks.get().contains(Block.getBlockFromItem(handStack.getItem()))) {
            blocksToPlace = handStack.getCount();
        } else {
            FindItemResult item;
            for (Block block : blocks.get()) {
                item = InvUtils.findInHotbar(block.asItem());
                if (item.found()) {
                    InvUtils.swap(item.slot(), true);
                    break;
                }
            }
            return;
        }

        int y = 0;

        switch (yMode.get()) {
            case UNDER -> y = mc.player.getBlockPos().getY() - 1;
            case FIRST_PLACE -> y = firstPlace;
            case STATIC -> y = level.get();
        }

        blocksToPlace = blocksToPlace > bpt.get() ? bpt.get() : blocksToPlace;

        for (BlockPos bp : bpProvider(mc.player.getBlockPos(), radius.get(), y)) {
            if (blocksToPlace <= 0) break;

            if (mc.world.getBlockState(bp).isReplaceable()) {
                if (placeBlock(bp, slot)) {
                    fadeBlocks.add(new FadeBlock(bp, fadeTime.get()));
                    blocksToPlace--;
                }
            }
        }

            fadeBlocks.removeIf(fadeBlock -> {
                fadeBlock.ticks--;
                return fadeBlock.ticks <= 0;
            });
        }

    private static class FadeBlock {
        public BlockPos pos;
        public int ticks;

        public FadeBlock(BlockPos pos, int ticks) {
            this.pos = pos;
            this.ticks = ticks;
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!render.get()) return;

        for (FadeBlock fadeBlock : fadeBlocks) {

            float progress = (float) fadeBlock.ticks / fadeTime.get();
            progress = progress * progress;
            int alpha = (int) (sideColor.get().a * progress);

            event.renderer.box(
                fadeBlock.pos,
                new SettingColor(
                    sideColor.get().r,
                    sideColor.get().g,
                    sideColor.get().b,
                    alpha
                ),
                lineColor.get(),
                shapeMode.get(),
                0
            );
        }
    }


    private boolean placeBlock(BlockPos blockpos, int slot) {
        return BlockUtils.place(blockpos, Hand.MAIN_HAND, slot, false, 50, false, true, false);
    }

    public List<BlockPos> bpProvider(BlockPos centerPos, int radius, int height) {
        List<BlockPos> blocks = new ArrayList<>();

        if (airPlace.get()) {
            blocks = getSphere(centerPos, radius, height);
        } else {
            for (BlockPos bp : getSphere(centerPos, radius, height)) {
                if (validPlace(bp)) {
                    blocks.add(bp);
                }
            }
        }

        if (yMode.get() == Mode.UNDER) {
            blocks.sort((pos1, pos2) -> {
                double dist1 = Math.sqrt(Math.pow(pos1.getX() - centerPos.getX(), 2) +
                    Math.pow(pos1.getZ() - centerPos.getZ(), 2));
                double dist2 = Math.sqrt(Math.pow(pos2.getX() - centerPos.getX(), 2) +
                    Math.pow(pos2.getZ() - centerPos.getZ(), 2));
                return Double.compare(dist1, dist2);
            });
        }

        return blocks;
    }

    public boolean validPlace(BlockPos pos) {
        boolean valid = false;
        BlockPos newPos = new BlockPos(pos.getX(), pos.getY() + 1, pos.getZ());
        if (pos.getY() < 319 && mc.world.getBlockState(newPos).getBlock() != Blocks.AIR) valid = true;
        newPos = new BlockPos(pos.getX(), pos.getY() - 1, pos.getZ());
        if (pos.getY() > -64 && mc.world.getBlockState(newPos).getBlock() != Blocks.AIR) valid = true;

        for (Direction dir : Direction.values()) {
            if (dir == Direction.UP || dir == Direction.DOWN) continue;
            BlockState state = mc.world.getBlockState(pos.offset(dir));
            if (state.getBlock() != Blocks.AIR) {
                valid = true;
                break;
            }
        }

        return valid;
    }

    public List<BlockPos> getSphere(BlockPos centerPos, int radius, int height) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int x = centerPos.getX() - radius; x <= centerPos.getX() + radius; x++) {
            for (int y = centerPos.getZ() - radius; y <= centerPos.getZ() + radius; y++) {
                BlockPos pos = new BlockPos(x, height, y);
                if (!mc.world.getBlockState(pos).isReplaceable()) continue;
                if (!blocks.contains(pos)) blocks.add(pos);
            }
        }
        return blocks;
    }

    public enum Mode {
        UNDER,
        FIRST_PLACE,
        STATIC
    }
}
