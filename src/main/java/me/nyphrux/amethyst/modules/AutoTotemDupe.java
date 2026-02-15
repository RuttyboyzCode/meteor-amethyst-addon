package me.nyphrux.amethyst.modules;

import me.nyphrux.amethyst.Main;
import me.nyphrux.amethyst.util.ModuleCreditsIntergration;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

public class AutoTotemDupe extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> alwaysDupe = sgGeneral.add(new BoolSetting.Builder()
        .name("always-dupe")
        .description("Always dupes, ignores inventory amount.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> dupeWhen = sgGeneral.add(new IntSetting.Builder()
        .name("dupe-when")
        .description("Only dupes when you have this many totems or less.")
        .defaultValue(10)
        .min(1)
        .max(27)
        .sliderMin(0)
        .sliderMax(27)
        .visible(() -> !alwaysDupe.get())
        .build()
    );

    private final Setting<Integer> dupeAmount = sgGeneral.add(new IntSetting.Builder()
        .name("dupe-amount")
        .description("/dupe {num} - Dumbass :p")
        .defaultValue(24)
        .sliderMin(1)
        .sliderMax(64)
        .min(1)
        .max(64)
        .build()
    );

    private final Setting<Integer> dupeDelay = sgGeneral.add(new IntSetting.Builder()
        .name("dupe-delay")
        .description("Delay between actions (In ticks) [Ideal: 1-4]")
        .defaultValue(1)
        .sliderMin(0)
        .sliderMax(20)
        .min(0)
        .max(20)
        .build()
    );

    public AutoTotemDupe() {
        super(Main.CATEGORY, "auto-totem-dupe", "Automatically dupes totems making you invincible.");
        ((ModuleCreditsIntergration)this).setCredits("Wim (Making it)");
        // Wim made the module in an obfuscated meteor addon. I "blind skidded" it and made this shit.
    }

    private boolean inUse; // lowk made this so it doesnt kill itself but its useless
    private DupeStep step = DupeStep.DONE;
    private int delayTicks;
    private int cachedTotemSlot;
    private int cachedHotbarSlot;

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        if (step != DupeStep.DONE) {
            if (delayTicks > 0) {
                delayTicks--;
                return;
            }

            int screenHandlerSlot = 36 + cachedHotbarSlot;

            switch (step) {
                case MOVE_IN -> {
                    click(cachedTotemSlot);
                    click(screenHandlerSlot);
                    click(cachedTotemSlot);

                    step = DupeStep.DUPE;
                    delayTicks = dupeDelay.get();
                }

                case DUPE -> {
                    mc.player.networkHandler.sendChatCommand("dupe " + dupeAmount.get());

                    step = DupeStep.MOVE_OUT;
                    delayTicks = dupeDelay.get();
                }

                case SECOND_DUPE -> {
                    mc.player.networkHandler.sendChatCommand("dupe " + dupeAmount.get());

                    step = DupeStep.MOVE_OUT;
                    delayTicks = dupeDelay.get();
                }

                case MOVE_OUT -> {
                    click(cachedTotemSlot);
                    click(screenHandlerSlot);
                    click(cachedTotemSlot);

                    mc.player.getInventory().selectedSlot = cachedHotbarSlot;
                    step = DupeStep.DONE;
                    inUse = false;
                }
            }

            return;
        }

        if (inUse) return;

        int totems = countTotemsInInv();
        if (!alwaysDupe.get() && totems > dupeWhen.get()) return;

        FindItemResult totem = InvUtils.find(Items.TOTEM_OF_UNDYING);
        if (!totem.found()) return;

        int selectedHotbarSlot = mc.player.getInventory().selectedSlot;
        int screenHandlerSlot = 36 + selectedHotbarSlot;

        if (totem.slot() == screenHandlerSlot) {
            mc.player.networkHandler.sendChatCommand("dupe " + dupeAmount.get());
            return;
        }

        inUse = true;
        cachedTotemSlot = totem.slot();
        cachedHotbarSlot = selectedHotbarSlot;
        step = DupeStep.MOVE_IN;
        delayTicks = 0;
    }

    private int countTotemsInInv() {
        int count = 0;
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.TOTEM_OF_UNDYING) {
                count++;
            }
        }
        return count;
    }

    private void click(int slot) {
        mc.interactionManager.clickSlot(
            mc.player.currentScreenHandler.syncId,
            slot,
            0,
            SlotActionType.PICKUP,
            mc.player
        );
    }

    private enum DupeStep {
        MOVE_IN,
        DUPE,
        SECOND_DUPE, // Safety dupe in case initial fails.
        MOVE_OUT,
        DONE
    }
}
