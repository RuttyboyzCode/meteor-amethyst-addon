package me.nyphrux.amethyst.modules;

import me.nyphrux.amethyst.Main;
import meteordevelopment.meteorclient.events.game.SendMessageEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.ItemStack;

public class AntiDiscordSRV extends Module {

    public AntiDiscordSRV() {
        super(Main.CATEGORY, "AntiDiscordSRV", "Hides you're chat from discord SRV");
    }

    private String queuedMessage = null;
    private int messageDelayTicks = -1;
    private int renameDelayTicks = -1;
    private String renameBack = null;

    @EventHandler
    private void onTick(TickEvent.Post event) {
        ItemStack hand = mc.player.getMainHandStack();
        if (messageDelayTicks > 0) {
            messageDelayTicks--;
        } else if (messageDelayTicks == 0) {
            if (queuedMessage != null) {
                ChatUtils.sendPlayerMsg(queuedMessage);
                queuedMessage = null;
            }
            messageDelayTicks = -1;

            if (renameBack != null) {
                renameDelayTicks = 2;
            }
        }

        if (renameDelayTicks > 0) {
            renameDelayTicks--;
        } else if (renameDelayTicks == 0) {
            if (renameBack != null) {
                if (!hand.isEmpty()) {
                    ChatUtils.sendPlayerMsg("/rename " + renameBack);
                    renameBack = null;
                }
            }
            renameDelayTicks = -1;
        }
    }

    @EventHandler
    private void onChat(SendMessageEvent event) {
        if (event.message.startsWith("/")) return;
        if (mc.player == null || mc.world == null) return;
        if (event.message.endsWith(" [i]")) return;
        event.cancel();
        ItemStack hand = mc.player.getMainHandStack();
        String originalName = hand.getName().getString();
        boolean isEmptyName = originalName.isBlank() || originalName.equals("&7");

        if (isEmptyName) {
            ChatUtils.sendPlayerMsg(event.message + " [i]");
            queuedMessage = null;
            messageDelayTicks = -1;
            renameDelayTicks = -1;
            renameBack = null;
        } else {
            if (!hand.isEmpty()){
                ChatUtils.sendPlayerMsg("/rename &7");
            }

            queuedMessage = event.message + " [i]";
            renameBack = originalName;
            messageDelayTicks = 2;
        }
    }
}
