package me.nyphrux.amethyst.modules;

import me.nyphrux.amethyst.Main;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatSafety extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> disableChat = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-chat")
        .description("Turns off chat entirely.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> blockRacism = sgGeneral.add(new BoolSetting.Builder()
        .name("block-racism")
        .defaultValue(true)
        .visible(() -> !disableChat.get())
        .build()
    );

    private final Setting<Boolean> blockHomophobia = sgGeneral.add(new BoolSetting.Builder()
        .name("block-homophobia")
        .defaultValue(true)
        .visible(() -> !disableChat.get())
        .build()
    );

    private final Setting<Boolean> blockTransphobia = sgGeneral.add(new BoolSetting.Builder()
        .name("block-transphobia")
        .defaultValue(true)
        .visible(() -> !disableChat.get())
        .build()
    );

    private final Setting<Boolean> blockOtherSlurs = sgGeneral.add(new BoolSetting.Builder()
        .name("block-other-slurs")
        .defaultValue(true)
        .visible(() -> !disableChat.get())
        .build()
    );

    private final Setting<Boolean> blockGeneralProfanity = sgGeneral.add(new BoolSetting.Builder()
        .name("block-general-profanity")
        .defaultValue(true)
        .visible(() -> !disableChat.get())
        .build()
    );

    private final List<Pattern> racism = new ArrayList<>();
    private final List<Pattern> homophobia = new ArrayList<>();
    private final List<Pattern> transphobia = new ArrayList<>();
    private final List<Pattern> otherSlurs = new ArrayList<>();
    private final List<Pattern> profanity = new ArrayList<>();

    public ChatSafety() {
        super(Main.CATEGORY, "chat-safety", "Filters out offensive language using regex patterns.");

        profanity.add(Pattern.compile("(?i)\\b(f[u*1!@]+[c*]+[k*]|phu+[c*]+|f[u*]+k)\\b"));
        profanity.add(Pattern.compile("(?i)\\b(sh[i1!l]+t|shi+[t*]|s+h+i+t)\\b"));
        profanity.add(Pattern.compile("(?i)\\b(c[u*]+nt|c.?.?u.?n.?t)\\b"));
        profanity.add(Pattern.compile("(?i)\\b(b[i1!]+tch|b.?.?i.?t.?c.?h)\\b"));
        profanity.add(Pattern.compile("(?i)\\b(d[i1!]+ck|d[i1]+c)\\b"));
        profanity.add(Pattern.compile("(?i)\\b(p[u*]+ssy|p[u*]+s+y)\\b"));
        profanity.add(Pattern.compile("(?i)\\bm[o0]+th[ae]rf[u*]+cker\\b"));
        profanity.add(Pattern.compile("(?i)\\b(f[a@]+g|f[a@]g+[o0]+t)\\b"));

        racism.add(Pattern.compile("(?i)\\b(n[i1!]+gg[ae][ar]|n[i1]+g)\\b"));
        racism.add(Pattern.compile("(?i)\\b(ch[i1!]+nk|ch1nk)\\b"));
        racism.add(Pattern.compile("(?i)\\b(sp[i1!]+c)\\b"));
        racism.add(Pattern.compile("(?i)\\b(k[i1!]+ke)\\b"));

        homophobia.add(Pattern.compile("(?i)\\b(f[a@]+g|f[a@]g+[o0]+t|queer)\\b"));
        homophobia.add(Pattern.compile("(?i)\\b(d[i1!]+ke)\\b"));

        transphobia.add(Pattern.compile("(?i)\\b(tr[a@]+n+y|tr[a@]nn?y|troon)\\b"));

        otherSlurs.add(Pattern.compile("(?i)\\b(r[e3]+t[a@]rd|r[e3]t)\\b"));
        otherSlurs.add(Pattern.compile("(?i)\\b(p[e3]do)\\b"));
    }

    @EventHandler
    private void onReceiveMessage(ReceiveMessageEvent event) {
        if (disableChat.get()) {
            event.cancel();
            return;
        }

        Text original = event.getMessage();
        List<Pattern> active = new ArrayList<>();
        if (blockGeneralProfanity.get()) active.addAll(profanity);
        if (blockRacism.get()) active.addAll(racism);
        if (blockHomophobia.get()) active.addAll(homophobia);
        if (blockTransphobia.get()) active.addAll(transphobia);
        if (blockOtherSlurs.get()) active.addAll(otherSlurs);

        if (active.isEmpty()) return;

        StringBuilder regexBuilder = new StringBuilder();
        for (Pattern p : active) {
            if (regexBuilder.length() > 0) regexBuilder.append("|");
            regexBuilder.append(p.pattern());
        }

        Pattern combined = Pattern.compile(regexBuilder.toString());

        event.setMessage(censorText(original, combined));
    }

    private String censor(String word) {
        if (word.length() <= 2) return "*".repeat(word.length());
        return word.charAt(0) + "*".repeat(word.length() - 2) + word.charAt(word.length() - 1);
    }

    private MutableText censorText(Text original, Pattern combined) {
        if (original.getSiblings().isEmpty()) {
            return censorComponent(original, combined);
        }

        MutableText out = Text.empty();
        out.append(censorComponent(original, combined));

        for (Text sibling : original.getSiblings()) {
            out.append(censorText(sibling, combined));
        }

        return out.setStyle(original.getStyle());
    }

    private MutableText censorComponent(Text component, Pattern combined) {
        String content = component.getString();
        Matcher m = combined.matcher(content);
        MutableText result = Text.empty();
        int last = 0;

        while (m.find()) {
            if (m.start() > last) {
                result.append(Text.literal(content.substring(last, m.start())).setStyle(component.getStyle()));
            }

            String censored = censor(m.group());
            result.append(Text.literal(censored).setStyle(component.getStyle()));

            last = m.end();
        }

        if (last < content.length()) {
            result.append(Text.literal(content.substring(last)).setStyle(component.getStyle()));
        }

        return result;
    }
}
