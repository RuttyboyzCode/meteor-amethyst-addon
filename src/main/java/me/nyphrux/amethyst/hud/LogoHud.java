package me.nyphrux.amethyst.hud;

import me.nyphrux.amethyst.Main;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.util.Identifier;

public class LogoHud extends HudElement {
    public static final HudElementInfo<LogoHud> INFO = new HudElementInfo<>(
        Main.HUD, "icon-text-hud", "Displays the mod icon with text using a custom font.", LogoHud::new
    );

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Scale of the logo and banner.")
        .defaultValue(1.0)
        .min(0.1)
        .max(5.0)
        .sliderMin(0.1)
        .sliderMax(5.0)
        .build()
    );

    private static final Identifier LOGO = Identifier.of("amethyst", "icon.png");
    private static final Identifier BANNER = Identifier.of("amethyst", "banner.png");

    public LogoHud() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        double baseLogoSize = 64;
        double baseBannerHeight = baseLogoSize * 0.75;
        double baseBannerWidth = baseBannerHeight * 4;
        double basePadding = 6;

        double scaledLogoSize = baseLogoSize * scale.get();
        double scaledBannerHeight = baseBannerHeight * scale.get();
        double scaledBannerWidth = baseBannerWidth * scale.get();
        double scaledPadding = basePadding * scale.get();

        setSize(scaledLogoSize + scaledPadding + scaledBannerWidth, Math.max(scaledLogoSize, scaledBannerHeight));
        renderer.texture(LOGO, x, y, scaledLogoSize, scaledLogoSize, Color.WHITE);
        renderer.texture(
            BANNER,
            x + scaledLogoSize + scaledPadding,
            y + (scaledLogoSize - scaledBannerHeight) / 2,
            scaledBannerWidth,
            scaledBannerHeight,
            Color.WHITE
        );
    }
}
