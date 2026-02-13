/*
    | Credits to Aholicknight (BananaForEveryone) for this code.
    | This file is a part of Aholicknight's BananaPlus addon.
    | > Not related to play.dupeanarchy.com
    | > Just fun things I want to add
 */

package me.nyphrux.amethyst.modules;

import me.nyphrux.amethyst.Main;
import me.nyphrux.amethyst.util.ModuleCreditsIntergration;
import me.nyphrux.amethyst.util.TimerUtil;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class Twerkin extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> twerkDelay = sgGeneral.add(new DoubleSetting.Builder()
        .name("Twerk Delay")
        .description("Time in milliseconds. (100ms = 1s)")
        .defaultValue(4)
        .min(2)
        .sliderRange(2,100)
        .build()
    );

    public Twerkin() {
        super(Main.CATEGORY, "twerkin", "Start twerking twin :3");
        ((ModuleCreditsIntergration)this).setCredits("Aholicknight (BananaForEveryone)");
    }

    private boolean twerkin = false;
    private final TimerUtil onTwerk = new TimerUtil();


    @EventHandler
    private void onTick(TickEvent.Pre event) {
        mc.options.sneakKey.setPressed(twerkin);

        if (onTwerk.passedMillis(twerkDelay.get().longValue()) && !twerkin) {
            onTwerk.reset();
            twerkin = true;
        }

        if (onTwerk.passedMillis(twerkDelay.get().longValue()) && twerkin) {
            onTwerk.reset();
            twerkin = false;
        }

    }

    @Override
    public void onDeactivate() {
        twerkin = false;
        mc.options.sneakKey.setPressed(false);
        onTwerk.reset();
    }
}
