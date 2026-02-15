package me.nyphrux.amethyst.modules;

import me.nyphrux.amethyst.Main;
import me.nyphrux.amethyst.util.InvUtil;
import me.nyphrux.amethyst.util.ModuleCreditsIntergration;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class ProAutoTotem extends Module {
    private final SettingGroup sg_general = settings.getDefaultGroup();

    private final Setting<Versions> cfg_version = sg_general.add(new EnumSetting.Builder<Versions>()
        .name("mode")
        .defaultValue(Versions.one_dot_17)
        .build()
    );

    private final Setting<Boolean> cfg_close_screen = sg_general.add(new BoolSetting.Builder()
        .name("close-screen-on-pop")
        .description("Closes any screen handler while putting totem in offhand.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> cfg_action_delay = sg_general.add(new IntSetting.Builder()
        .name("action-delay")
        .defaultValue(0)
        .build()
    );

    private final Setting<Boolean> cfg_anti_gap_disease = sg_general.add(new BoolSetting.Builder()
        .name("stop-eating-on-action")
        .visible(() -> cfg_version.get() == Versions.one_dot_12)
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> cfg_try_hard = sg_general.add(new BoolSetting.Builder()
        .name("totem-spam")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> cfg_try_hard_delay = sg_general.add(new IntSetting.Builder()
        .name("spam-delay")
        .visible(cfg_try_hard::get)
        .defaultValue(0)
        .build()
    );

    public static ProAutoTotem instance;

    public ProAutoTotem() {
        super(Main.CATEGORY, "pro-auto-totem", "A decent auto totem with the weirdest configs.");
        ((ModuleCreditsIntergration)this).setCredits("So skidded the original author is unknown atp.");
        instance = this;
    }

    @EventHandler
    private void tick(TickEvent.Pre event) {
        if (delay_ticks_left > 0) {
            --delay_ticks_left;
            return;
        }

        if (mc.player.currentScreenHandler instanceof CreativeInventoryScreen.CreativeScreenHandler) return;

        ItemStack
            offhand_stack = mc.player.getInventory().getStack(40),
            cursor_stack = mc.player.currentScreenHandler.getCursorStack();

        final boolean
            is_holding_totem = cursor_stack.getItem() == Items.TOTEM_OF_UNDYING,
            is_totem_in_offhand = offhand_stack.getItem() == Items.TOTEM_OF_UNDYING,
            can_click_offhand = mc.player.currentScreenHandler instanceof PlayerScreenHandler;

        if (cfg_try_hard.get() && !(isClassic() && mc.currentScreen instanceof HandledScreen)) {
            if (try_hard_ticks_left > 0)
                --try_hard_ticks_left;
            else {
                try_hard_ticks_left = cfg_try_hard_delay.get();
                should_override_totem = true;
            }
        }

        if (is_totem_in_offhand && !should_override_totem) return;

        final int totem_id = GetTotemId();
        if (totem_id == -1 && !is_holding_totem) return;

        if (is_holding_totem && can_click_offhand) {
            InvUtil.Click(45);
            return;
        }

        if (!can_click_offhand) {
            if (isClassic()) {
                ItemStack mainhand_stack = mc.player.getInventory().getStack(selected_slot);
                if (mainhand_stack.getItem() == Items.TOTEM_OF_UNDYING) {
                    mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket
                        (PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
                    mc.player.setStackInHand(Hand.OFF_HAND, mc.player.getInventory().getStack(selected_slot));
                    mc.player.getInventory().setStack(selected_slot, offhand_stack);
                    mc.player.clearActiveItem();
                    return;
                }

                if (is_holding_totem)
                    InvUtil.Click(InvUtil.GetFirstHotbarSlotId() + selected_slot);

                should_override_totem = false;
            }
            else {
                if (totem_id == -1) {
                    for (Slot slot : mc.player.currentScreenHandler.slots) {
                        if (!slot.getStack().isEmpty()) continue;
                        InvUtil.Click(slot.id);
                        return;
                    }

                    InvUtil.Click(InvUtil.GetFirstHotbarSlotId() + selected_slot);
                    return;
                }
            }
        }

        InvUtil.Move(totem_id);

        should_override_totem = !is_totem_in_offhand && ShouldOverrideTotem();
    }

    @EventHandler
    private void onPacketSent(PacketEvent.Sent event) {
        if (event.packet instanceof ClickSlotC2SPacket) {
            delay_ticks_left = cfg_action_delay.get();

            if (isClassic() && cfg_anti_gap_disease.get())
                mc.interactionManager.stopUsingItem(mc.player);
        }
        else if (event.packet instanceof PlayerActionC2SPacket packet) {
            if (packet.getAction() == PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND)
                delay_ticks_left = cfg_action_delay.get();
        }
        else if (event.packet instanceof UpdateSelectedSlotC2SPacket packet) {
            selected_slot = packet.getSelectedSlot();
        }
    }

    @EventHandler
    private void onPacketReceived(PacketEvent.Receive event) {
        if (event.packet instanceof EntityStatusS2CPacket packet) {
            if (packet.getStatus() != 35 || packet.getEntity(mc.world).getId() != mc.player.getId()) return;

            if (mc.player.currentScreenHandler instanceof PlayerScreenHandler) return;

            if (cfg_close_screen.get())
                mc.player.closeHandledScreen();

            ItemStack mainhand_stack = mc.player.getInventory().getStack(selected_slot);
            if (mainhand_stack.getItem() == Items.TOTEM_OF_UNDYING)
            {
                mainhand_stack.decrement(1);
                return;
            }

            ItemStack offhand_stack = mc.player.getOffHandStack();
            if (offhand_stack.getItem() == Items.TOTEM_OF_UNDYING)
                offhand_stack.decrement(1);
        }
        else if (event.packet instanceof UpdateSelectedSlotS2CPacket packet) {
            selected_slot = packet.slot();
        }
    }

    @Override
    public void onActivate() {
        should_override_totem = true;
        selected_slot = mc.player.getInventory().selectedSlot;
    }

    public static boolean isClassic() {
        return instance.cfg_version.get() == Versions.one_dot_12;
    }

    private int GetTotemId() {
        final int hotbar_start = InvUtil.GetFirstHotbarSlotId();
        for (int i = hotbar_start; i < hotbar_start + 9; ++i) {
            if (mc.player.currentScreenHandler.getSlot(i).getStack().getItem() != Items.TOTEM_OF_UNDYING) continue;
            return i;
        }

        for (int i = 0; i < hotbar_start; ++i) {
            if (mc.player.currentScreenHandler.getSlot(i).getStack().getItem() != Items.TOTEM_OF_UNDYING) continue;
            return i;
        }

        return -1;
    }

    private boolean ShouldOverrideTotem() {
        return cfg_version.get() != Versions.one_dot_17 ||
            (!(mc.player.currentScreenHandler instanceof PlayerScreenHandler) && cfg_version.get() == Versions.one_dot_17);
    }

    private boolean should_override_totem;
    private int selected_slot = 0;
    private int delay_ticks_left = 0, try_hard_ticks_left = 0;

    public enum Versions {
        one_dot_12("classic"),
        one_dot_16("1.16.5"),
        one_dot_17("1.17+");

        Versions(String name) {this.name = name;}

        String name;

        @Override public String toString() {return name;}
    }
}
