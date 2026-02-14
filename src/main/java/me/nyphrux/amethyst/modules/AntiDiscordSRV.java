package me.nyphrux.amethyst.modules;

import me.nyphrux.amethyst.Main;
import me.nyphrux.amethyst.util.ModuleCreditsIntergration;
import meteordevelopment.meteorclient.events.game.SendMessageEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class AntiDiscordSRV extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private String storedName = null;
    private boolean restoring = false;
    private int tickDelay = 0;

    public enum Style {
        New,
        Full,
        Minimal
    }

    private final Setting<Style> style = sgGeneral.add(
        new EnumSetting.Builder<Style>()
            .name("style")
            .description("Suffix style to append to messages.")
            .defaultValue(Style.Minimal)
            .build()
    );

    public AntiDiscordSRV() {
        super(Main.CATEGORY, "anti-discordSRV", "Hides your messages from DiscordSRV.");
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WVerticalList list = theme.verticalList();
        list.add(theme.label("This module abuses the show-item-in-chat system.")).widget();
        list.add(theme.label("WARNING: All your messages will show your held item at the end.")).widget();
        return list;
    }

    @EventHandler
    private void onSendMessage(SendMessageEvent event) {
        String message = event.message;

        if (message.startsWith("/") || message.startsWith(".")) return;

        String suffix = getSuffix();

        if (message.endsWith(suffix)) return;

        event.message = message + " " + suffix;
    }

    private String getSuffix() {
        return switch (style.get()) {
            case New -> "[hand]";
            case Full -> "[item]";
            case Minimal -> "[i]";
        };
    }
}
