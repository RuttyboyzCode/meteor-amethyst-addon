package me.nyphrux.amethyst.api;

import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.utils.network.Http;
import me.nyphrux.amethyst.Main;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class OnlineApi implements ClientModInitializer {
    private static final String API_URL = "https://amethyst.nyphrux.workers.dev/api";
    private static final Set<String> onlinePlayers = new HashSet<>();
    private static Timer updateTimer;
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public void onInitializeClient() {
        LogUtils.getLogger().info("Online API initialized.");

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> startOnlineTracking());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> stopOnlineTracking());
        ClientLifecycleEvents.CLIENT_STOPPING.register(this::onShutdown);
    }

    private void startOnlineTracking() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        String uuid = client.player.getUuidAsString();
        String username = client.player.getName().getString();

        if (!Main.hiddenFromAPI) {
            setOnline(uuid, username);
        }

        if (updateTimer != null) updateTimer.cancel();
        updateTimer = new Timer();
        updateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                fetchOnlinePlayers();
            }
        }, 0, 30_000);
    }

    private void stopOnlineTracking() {
        MinecraftClient client = MinecraftClient.getInstance();

        if (updateTimer != null) {
            updateTimer.cancel();
            updateTimer = null;
        }

        if (client.player != null && !Main.hiddenFromAPI) {
            setOffline(client.player.getUuidAsString());
        }
    }

    private void setOnline(String uuid, String username) {
        try {
            String json = "{ \"uuid\": \"" + uuid + "\", \"username\": \"" + username + "\", \"status\": \"online\" }";

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "/status"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> LogUtils.getLogger().info("Online status set. Response: {}", response.statusCode()));
        } catch (Exception e) {
            LogUtils.getLogger().error("Failed to set online status: {}", e.getMessage());
        }
    }

    private void setOffline(String uuid) {
        try {
            String json = "{ \"uuid\": \"" + uuid + "\", \"status\": \"offline\" }";

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "/status"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> LogUtils.getLogger().info("Offline status set. Response: {}", response.statusCode()));
        } catch (Exception e) {
            LogUtils.getLogger().error("Failed to set offline status: {}", e.getMessage());
        }
    }

    private void fetchOnlinePlayers() {
        try {
            String response = Http.get(API_URL + "/online").sendString();
            if (response == null || response.isEmpty()) return;

            JsonArray jsonArray = JsonParser.parseString(response).getAsJsonArray();

            synchronized (onlinePlayers) {
                onlinePlayers.clear();
                jsonArray.forEach(element -> onlinePlayers.add(element.getAsJsonObject().get("uuid").getAsString()));
            }
        } catch (Exception e) {
            LogUtils.getLogger().error("Failed to fetch online players: {}", e.getMessage(), e);
        }
    }

    private void onShutdown(MinecraftClient client) {
        stopOnlineTracking();
    }

    public static boolean isPlayerOnline(String uuid) {
        synchronized (onlinePlayers) {
            return onlinePlayers.contains(uuid);
        }
    }
}
