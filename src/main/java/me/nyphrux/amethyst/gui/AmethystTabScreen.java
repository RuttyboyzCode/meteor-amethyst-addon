package me.nyphrux.amethyst.gui;

import me.nyphrux.amethyst.Main;
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

public class AmethystTabScreen extends WindowTabScreen {
    public AmethystTabScreen(GuiTheme theme, Tab tab) {
        super(theme, tab);
    }

    @Override
    public void initWidgets() {
        WSection amethystApi = add(theme.section("Amethyst API", true)).expandX().widget();

        WTable apiTable = amethystApi.add(theme.table()).expandX().widget();

        apiTable.add(theme.label("Show Amethyst Users: "));
        WCheckbox areYouAmethyst = apiTable.add(theme.checkbox(Main.showAmethystInTab)).widget();
        apiTable.row();

        areYouAmethyst.action = () -> {
            Main.showAmethystInTab = areYouAmethyst.checked;
            Config.get().save();
        };

        apiTable.add(theme.label("Hide me: "));
        WCheckbox hideFromAmethyst = apiTable.add(theme.checkbox(Main.hiddenFromAPI)).widget();
        apiTable.row();

        hideFromAmethyst.action = () -> {
            Main.hiddenFromAPI = hideFromAmethyst.checked;
            Config.get().save();
        };

        WSection amethystHud = add(theme.section("Amethyst HUD", true)).expandX().widget();

        WTable hudTable = amethystHud.add(theme.table()).expandX().widget();

        WButton newCatPlease = hudTable.add(theme.button("New Cat Please")).centerX().widget();
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
}
