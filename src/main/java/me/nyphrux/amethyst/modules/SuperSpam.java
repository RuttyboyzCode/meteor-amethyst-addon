package me.nyphrux.amethyst.modules;

import me.nyphrux.amethyst.Main;
import me.nyphrux.amethyst.util.ModuleCreditsIntergration;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.DisconnectedScreen;

import java.util.List;

public class SuperSpam extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSpeed = settings.createGroup("Speed Settings");

    private final Setting<List<String>> messages = sgGeneral.add(new StringListSetting.Builder()
        .name("messages")
        .defaultValue(List.of("# - > **__` [ Amethyst Beats Dupers ] `__**"))
        .build()
    );

    private final Setting<Boolean> fullPotential = sgSpeed.add(new BoolSetting.Builder()
        .name("full-potential")
        .description("Sends messages as fast as possible, ignoring all delays. Overrides all other settings in this category.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> reckless = sgSpeed.add(new BoolSetting.Builder()
        .name("reckless")
        .description("Sends messages at maximum speed (100/t - 2000/s)")
        .defaultValue(false)
        .visible(() -> fullPotential.get())
        .build()
    );

    private final Setting<Integer> messagesPerTick = sgSpeed.add(new IntSetting.Builder()
        .name("messages-per-tick")
        .description("Number of messages to send each tick.")
        .defaultValue(1)
        .min(1)
        .sliderMax(20)
        .visible(() -> !fullPotential.get())
        .build()
    );

    private final Setting<Boolean> noDelay = sgSpeed.add(new BoolSetting.Builder()
        .name("no-delay")
        .description("Ignores the delay between messages. Overrides the delay setting.")
        .defaultValue(false)
        .visible(() -> !fullPotential.get())
        .build()
    );

    private final Setting<Integer> delay = sgSpeed.add(new IntSetting.Builder()
        .name("delay")
        .description("Delay between messages in ticks. 20 ticks = 1 second.")
        .defaultValue(20)
        .min(0)
        .sliderMax(200)
        .visible(() -> !noDelay.get() && !fullPotential.get())
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
    private static final int MAX_PACKETS_PER_SECOND = 1500;
    private static final int SAFE_MARGIN = 250;
    private static final int TICKS_PER_SECOND = 20;

    private int tokens;

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

        int safePackets = MAX_PACKETS_PER_SECOND - SAFE_MARGIN;
        tokens = safePackets / TICKS_PER_SECOND;
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

        if (!fullPotential.get()) {
            if (!noDelay.get() && timer > 0) {
                timer--;
                return;
            }

            for (int i = 0; i < messagesPerTick.get(); i++) {
                if (text == null) {
                    if (messageI >= messages.get().size()) messageI = 0;
                    text = messages.get().get(messageI++);
                }

                if (autoSplitMessages.get() && text.length() > splitLength.get()) {
                    int start = splitNum * splitLength.get();
                    int end = Math.min(start + splitLength.get(), text.length());

                    ChatUtils.sendPlayerMsg(text.substring(start, end));

                    int splits = (int) Math.ceil((double) text.length() / splitLength.get());
                    splitNum = ++splitNum % splits;

                    if (splitNum == 0) text = null;
                    timer = autoSplitDelay.get();
                    break;
                } else {
                    if (text.length() > 256) text = text.substring(0, 256);
                    ChatUtils.sendPlayerMsg(text);
                    text = null;
                }
            }

            if (!noDelay.get()) timer = delay.get();
            return;
        }

        int packetsPerSecond = reckless.get()
            ? 2000
            : (MAX_PACKETS_PER_SECOND - SAFE_MARGIN);

        int sendsThisTick = packetsPerSecond / TICKS_PER_SECOND;

        for (int i = 0; i < sendsThisTick; i++) {
            if (text == null) {
                if (messageI >= messages.get().size()) messageI = 0;
                text = messages.get().get(messageI++);
            }

            if (autoSplitMessages.get() && text.length() > splitLength.get()) {
                int start = splitNum * splitLength.get();
                int end = Math.min(start + splitLength.get(), text.length());

                ChatUtils.sendPlayerMsg(text.substring(start, end));

                int splits = (int) Math.ceil((double) text.length() / splitLength.get());
                splitNum = ++splitNum % splits;

                if (splitNum == 0) text = null;
            } else {
                if (text.length() > 256) text = text.substring(0, 256);
                ChatUtils.sendPlayerMsg(text);
                text = null;
            }
        }
    }

    private WVerticalList infoList;

    @Override
    public WWidget getWidget(GuiTheme theme) {
        if (infoList == null) infoList = theme.verticalList();

        infoList.clear();

        infoList.add(theme.label("[ Reopen module config to see warnings ]").color(Color.CYAN));

        if (fullPotential.get()) {
            if (reckless.get()) {
                infoList.add(theme.label("WARNING: Reckless mode enabled (2000/s)").color(Color.RED));
                infoList.add(theme.label("Use with extreme caution to avoid kicks.").color(Color.RED));
                infoList.add(theme.label("Enable Auto-Reconnect for safety.").color(Color.RED));
            } else {
                infoList.add(theme.label("CAUTION: Full Potential mode enabled (max safe speed)").color(Color.ORANGE));
                infoList.add(theme.label("May cause instability or kicks.").color(Color.ORANGE));
            }
        } else {
            infoList.add(theme.label("Module running in normal mode").color(Color.GREEN));
        }

        return infoList;
    }
}
