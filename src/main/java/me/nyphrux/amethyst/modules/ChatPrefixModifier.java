package me.nyphrux.amethyst.modules;

import me.nyphrux.amethyst.Main;
import me.nyphrux.amethyst.util.ModuleCreditsIntergration;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.lang.reflect.Field;

public class ChatPrefixModifier extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> prefix = sgGeneral.add(
        new StringSetting.Builder()
            .name("prefix")
            .description("Custom chat prefix. Use legacy [&] color codes.")
            .defaultValue("&7Amethyst &d»")
            .onChanged(this::updatePrefix)
            .build()
    );

    private Text oldPrefix;
    private Field prefixField;

    public ChatPrefixModifier() {
        super(Main.CATEGORY, "chat-prefix", "Customise the Meteor chat prefix to be whatever you want.");
        ((ModuleCreditsIntergration)this).setCredits("Felix (PREFIX accessor)");

        try {
            prefixField = ChatUtils.class.getDeclaredField("PREFIX");
            prefixField.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException("Failed to access ChatUtils.PREFIX", e);
        }
    }

    @Override
    public void onActivate() {
        try {
            oldPrefix = (Text) prefixField.get(null);
            updatePrefix(prefix.get() + " ");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDeactivate() {
        try {
            prefixField.set(null, oldPrefix);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updatePrefix(String value) {
        try {
            prefixField.set(null, Text.literal(translateColors(value)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String translateColors(String input) {
        for (Formatting f : Formatting.values()) {
            input = input.replace("&" + f.getCode(), "§" + f.getCode());
        }
        return input;
    }
}
