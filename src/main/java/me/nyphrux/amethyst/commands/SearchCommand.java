package me.nyphrux.amethyst.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class SearchCommand extends Command {

    public SearchCommand() {
        super("search", "Counts how many of an entity are in render distance.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("entity", StringArgumentType.word())
            .suggests((ctx, suggestions) -> {
                String remaining = suggestions.getRemaining().toLowerCase();

                Registries.ENTITY_TYPE.getIds().forEach(id -> {
                    if (!id.getNamespace().equals("minecraft")) return;

                    String name = id.getPath();
                    if (remaining.isEmpty() || name.startsWith(remaining)) {
                        suggestions.suggest(name);
                    }
                });

                return suggestions.buildFuture();
            })

            .executes(ctx -> {
                if (mc.world == null) {
                    ChatUtils.error("You are not in a world.");
                    return 0;
                }

                String input = StringArgumentType.getString(ctx, "entity").toLowerCase();
                Identifier id = Identifier.tryParse(input.contains(":") ? input : "minecraft:" + input);

                if (id == null || !Registries.ENTITY_TYPE.containsId(id)) {
                    ChatUtils.error("Unknown entity: " + input);
                    return 0;
                }

                var type = Registries.ENTITY_TYPE.get(id);
                int count = 0;

                for (Entity entity : mc.world.getEntities()) {
                    if (entity.getType() == type) count++;
                }

                ChatUtils.info("There are %d %s%s in your render distance.",
                    count,
                    input,
                    count == 1 ? "" : "s"
                );

                return 1;
            })
        );
    }

    private static com.mojang.brigadier.suggestion.Suggestions suggestEntities(SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase();

        Registries.ENTITY_TYPE.getIds().forEach(id -> {
            if (!id.getNamespace().equals("minecraft")) return;

            String name = id.getPath();
            if (name.startsWith(remaining)) {
                builder.suggest(name);
            }

            if (remaining.isEmpty() || name.startsWith(remaining)) {
                builder.suggest(name);
            }
        });

        return builder.build();
    }
}
