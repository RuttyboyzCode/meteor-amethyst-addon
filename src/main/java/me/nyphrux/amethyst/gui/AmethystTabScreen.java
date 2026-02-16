package me.nyphrux.amethyst.gui;

import me.nyphrux.amethyst.Main;
import me.nyphrux.amethyst.api.OnlineApi;
import me.nyphrux.amethyst.hud.CatHud;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.WindowTabScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.network.PlayerListEntry;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class AmethystTabScreen extends WindowTabScreen {
    public AmethystTabScreen(GuiTheme theme, Tab tab) {
        super(theme, tab);
    }

    @Override
    public void initWidgets() {
        WSection playerListSection = add(theme.section("Online Players (Tablist)", true)).expandX().widget();

        WTable playerTable = playerListSection.add(theme.table()).expandX().widget();

        populatePlayerList(playerTable);

        WSection amethystApi = add(theme.section("Amethyst API", true)).expandX().widget();

        WTable apiTable = amethystApi.add(theme.table()).expandX().widget();

        apiTable.add(theme.label("Show active Amethyst users "));
        WCheckbox areYouAmethyst = apiTable.add(theme.checkbox(Main.showAmethystInTab)).widget();
        apiTable.row();

        areYouAmethyst.action = () -> {
            Main.showAmethystInTab = areYouAmethyst.checked;
            Config.get().save();
        };

        apiTable.add(theme.label("Hide myself from the API "));
        WCheckbox hideFromAmethyst = apiTable.add(theme.checkbox(Main.hiddenFromAPI)).widget();
        apiTable.row();

        hideFromAmethyst.action = () -> {
            Main.hiddenFromAPI = hideFromAmethyst.checked;
            Config.get().save();
        };

        WSection amethystHud = add(theme.section("Amethyst HUD", true)).expandX().widget();

        WTable hudTable = amethystHud.add(theme.table()).expandX().widget();

        WButton newCatPlease = hudTable.add(theme.button("New Cat Please [Broken]")).centerX().widget();
        hudTable.row();

        newCatPlease.action = () -> {
            for (HudElement element : Hud.get()) {
                if (element instanceof CatHud catHud && catHud.isActive()) {
                    catHud.reload();
                    break;
                }
                else {
                    ChatUtils.warning("CatHud is not enabled!");
                    break;
                }
            }
        };
    }

    private void populatePlayerList(WTable table) {
        table.clear();

        var client = mc.getInstance();
        if (client.getNetworkHandler() == null) {
            table.add(theme.label("No players to display.").color(Color.RED)).centerX();
            table.add(theme.label("Not connected to a server.").color(Color.RED)).centerX();
            return;
        }

        for (PlayerListEntry entry : client.getNetworkHandler().getPlayerList()) {
            String name = entry.getProfile().getName();
            String uuid = entry.getProfile().getId().toString();

            boolean isUsingAmethyst =
                Main.showAmethystInTab &&
                    !Main.hiddenFromAPI &&
                    OnlineApi.isPlayerOnline(uuid);

            table.add(theme.label(name).color(isUsingAmethyst
                    ? meteordevelopment.meteorclient.utils.render.color.Color.GREEN
                    : meteordevelopment.meteorclient.utils.render.color.Color.RED
            ));

            WButton testApi = table.add(theme.button("Status")).right().widget();
            testApi.action = () -> {
                ChatUtils.info(name + " " + uuid +
                    (isUsingAmethyst
                        ? " is using Amethyst"
                        : " isn't using Amethyst"));
            };

            table.row();
        }
    }
}
