package me.nyphrux.amethyst.hud;

import me.nyphrux.amethyst.Main;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

public class CatHud extends HudElement {
    public static final HudElementInfo<CatHud> INFO = new HudElementInfo<>(
        Main.HUD, "cat-hud", "Displays a random cat image.", CatHud::new
    );

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> delay = sgGeneral.add(new DoubleSetting.Builder()
        .name("new-image-delay-s")
        .description("Seconds between image reloads.")
        .defaultValue(60)
        .min(1)
        .build()
    );

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Scale of the cat image.")
        .defaultValue(1.0)
        .min(0.1)
        .max(5.0)
        .sliderMin(0.1)
        .sliderMax(5.0)
        .build()
    );

    private final Setting<Boolean> message = sgGeneral.add(new BoolSetting.Builder()
        .name("message")
        .description("Sends a message in chat when a new cat image is loaded.")
        .defaultValue(true)
        .build()
    );

    private Identifier texture;
    private long lastReload;
    private boolean isLoading = false;

    public CatHud() {
        super(INFO);
        reload();
    }

    @Override
    public void tick(HudRenderer renderer) {
        if (!isLoading && System.currentTimeMillis() - lastReload >= delay.get() * 1000) {
            reload();
        }
    }

    @Override
    public void render(HudRenderer renderer) {
        if (texture == null) return;
        double baseSize = 100;
        double size = baseSize * scale.get();
        setSize(size, size);
        renderer.texture(texture, x, y, size, size, Color.WHITE);
    }

    public void reload() {
        if (isLoading) return;
        isLoading = true;

        CompletableFuture.runAsync(() -> {
            try {
                InputStream in = new URL("https://cataas.com/cat").openStream();
                NativeImage image = NativeImage.read(in);
                in.close();

                MinecraftClient.getInstance().execute(() -> {
                    try {
                        NativeImageBackedTexture tex = new NativeImageBackedTexture(image);
                        Identifier id = Identifier.of("amethyst", "cat_hud_" + System.currentTimeMillis());
                        MinecraftClient.getInstance().getTextureManager().registerTexture(id, tex);

                        if (texture != null) {
                            MinecraftClient.getInstance().getTextureManager().destroyTexture(texture);
                        }

                        texture = id;
                        lastReload = System.currentTimeMillis();
                        if (message.get()) {
                            ChatUtils.info("New cat image! Woaaa :3");
                        }
                        } catch (Exception e) {
                        image.close();
                    } finally {
                        isLoading = false;
                    }
                });
            } catch (Exception e) {
                isLoading = false;
            }
        });
    }
}
