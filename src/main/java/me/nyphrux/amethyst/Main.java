package me.nyphrux.amethyst;

import me.nyphrux.amethyst.commands.*;
import me.nyphrux.amethyst.modules.*;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.item.Items;

public class Main extends MeteorAddon {
    public static final Category CATEGORY = new Category("Amethyst", Items.AMETHYST_SHARD.getDefaultStack());

    @Override
    public void onInitialize() {
        Modules.get().add(new AutoTotemDupe());
        Modules.get().add(new Twerkin());
        Modules.get().add(new ChatPrefixModifier());
        Modules.get().add(new ProFlight());
        Modules.get().add(new ProScaffold());
        Modules.get().add(new InfAura());
        Modules.get().add(new AntiDiscordSRV());

        Commands.add(new SearchCommand());
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "me.nyphrux.amethyst";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("nyphrux", "meteor-amethyst-addon");
    }
}
