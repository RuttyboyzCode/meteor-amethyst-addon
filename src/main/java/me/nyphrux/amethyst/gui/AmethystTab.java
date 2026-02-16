package me.nyphrux.amethyst.gui;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import net.minecraft.client.gui.screen.Screen;

public class AmethystTab extends Tab {
    public AmethystTab() {
        super("Amethyst");
    }

    @Override
    public TabScreen createScreen(GuiTheme theme) {
        return new AmethystTabScreen(theme, this);
    }

    @Override
    public boolean isScreen(Screen screen) {
        return screen instanceof AmethystTabScreen;
    }
}
