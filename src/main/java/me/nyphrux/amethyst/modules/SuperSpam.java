package me.nyphrux.amethyst.modules;

import me.nyphrux.amethyst.Main;
import me.nyphrux.amethyst.util.ModuleCreditsIntergration;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.List;

public class SuperSpam extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<String>> messages = sgGeneral.add(new StringListSetting.Builder()
        .name("messages")
        .defaultValue(List.of("# > || Amethyst beats play.dupeanarchy.com ||"))
        .build()
    );

    private final Setting<Integer> messagesPerTick = sgGeneral.add(new IntSetting.Builder()
        .name("messages-per-tick")
        .defaultValue(1)
        .min(1)
        .sliderMax(20)
        .build()
    );

    private final Setting<Boolean> noDelay = sgGeneral.add(new BoolSetting.Builder()
        .name("no-delay")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .defaultValue(20)
        .min(0)
        .sliderMax(200)
        .visible(() -> !noDelay.get())
        .build()
    );

    private final Setting<Boolean> disableOnLeave = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-on-leave")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> disableOnDisconnect = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-on-disconnect")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> autoSplitMessages = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-split-messages")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> splitLength = sgGeneral.add(new IntSetting.Builder()
        .name("split-length")
        .visible(autoSplitMessages::get)
        .defaultValue(256)
        .min(1)
        .sliderMax(256)
        .build()
    );

    private final Setting<Integer> autoSplitDelay = sgGeneral.add(new IntSetting.Builder()
        .name("split-delay")
        .visible(autoSplitMessages::get)
        .defaultValue(20)
        .min(0)
        .sliderMax(200)
        .build()
    );

    private int messageI, timer, splitNum;
    private String text;

    public SuperSpam() {
        super(Main.CATEGORY, "super-spam", "Spams messages aggressively, with no punishment.");
        ((ModuleCreditsIntergration)this).setCredits("MeteorClient Modules - SuperSpam");
    }

    @Override
    public void onActivate() {
        timer = 0;
        messageI = 0;
        splitNum = 0;
        text = null;
    }

    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {
        if (disableOnDisconnect.get() && event.screen instanceof DisconnectedScreen) toggle();
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        if (disableOnLeave.get()) toggle();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (messages.get().isEmpty()) return;

        if (!noDelay.get() && timer > 0) {
            timer--;
            return;
        }

        for (int c = 0; c < messagesPerTick.get(); c++) {
            if (text == null) {
                int i;
                if (messageI >= messages.get().size()) messageI = 0;
                i = messageI++;

                text = messages.get().get(i);
            }

            if (autoSplitMessages.get() && text.length() > splitLength.get()) {
                double l = text.length();
                int splits = (int) Math.ceil(l / splitLength.get());

                int start = splitNum * splitLength.get();
                int end = Math.min(start + splitLength.get(), text.length());
                ChatUtils.sendPlayerMsg(text.substring(start, end));

                splitNum = ++splitNum % splits;
                timer = autoSplitDelay.get();
                if (splitNum == 0) {
                    timer = delay.get();
                    text = null;
                }
                break;
            } else {
                if (text.length() > 256) text = text.substring(0, 256);
                ChatUtils.sendPlayerMsg(text);
                text = null;
            }
        }

        if (!noDelay.get()) timer = delay.get();
    }
}
