package com.playerpicked.playershop.update;

import com.playerpicked.playershop.PlayerShop;
import com.playerpicked.playershop.identifiers.keys.ConfigKeys;
import com.playerpicked.playershop.identifiers.keys.MessageKeys;
import com.playerpicked.playershop.utils.LoggerUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class UpdateChecker {

    public static final String UPDATE_PERMISSION = "playershop.update-notify";

    private static final String API_URL = "https://api.spigotmc.org/legacy/update.php?resource=";
    private static final String RESOURCE_URL = "https://www.spigotmc.org/resources/";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final long CHECK_INTERVAL_TICKS = 20L * 60L * 30L;

    private final PlayerShop plugin = PlayerShop.getInstance();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();
    private final int resourceId;

    public UpdateChecker(int resourceId) {
        this.resourceId = resourceId;
    }

    public void start() {
        if (!ConfigKeys.UPDATE_CHECKER_ENABLED.getBoolean()) return;

        PlayerShop.getScheduler().runTaskTimerAsynchronously(this::check, 1L, CHECK_INTERVAL_TICKS);
    }

    private void check() {
        String latest = fetchLatestVersion();
        if (latest == null) return;

        String current = plugin.getDescription().getVersion();
        if (!isNewer(latest, current)) return;

        notifyUpdate(current, latest);
    }

    private static boolean isNewer(String latest, String current) {
        int[] latestParts = parseVersion(latest);
        int[] currentParts = parseVersion(current);
        int length = Math.max(latestParts.length, currentParts.length);

        for (int i = 0; i < length; i++) {
            int latestPart = i < latestParts.length ? latestParts[i] : 0;
            int currentPart = i < currentParts.length ? currentParts[i] : 0;

            if (latestPart != currentPart) return latestPart > currentPart;
        }

        return false;
    }

    @NotNull
    private static int[] parseVersion(@NotNull String version) {
        String core = version.replaceFirst("^[^0-9]+", "").split("[^0-9.]", 2)[0];
        if (core.isEmpty()) return new int[0];

        String[] parts = core.split("\\.");
        int[] numbers = new int[parts.length];

        for (int i = 0; i < parts.length; i++) {
            try {
                numbers[i] = parts[i].isEmpty() ? 0 : Integer.parseInt(parts[i]);
            } catch (NumberFormatException exception) {
                numbers[i] = 0;
            }
        }

        return numbers;
    }

    private @Nullable String fetchLatestVersion() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + resourceId))
                .timeout(TIMEOUT)
                .header("User-Agent", "PlayerShop-UpdateChecker")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                LoggerUtils.warn("Unable to check for updates: HTTP {}", response.statusCode());
                return null;
            }

            String version = response.body().trim();
            return version.isEmpty() ? null : version;
        } catch (IOException exception) {
            LoggerUtils.warn("Unable to check for updates: {}", exception.getMessage());
            return null;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private void notifyUpdate(String current, String latest) {
        LoggerUtils.warn("A new update is available! Current: {}, Latest: {}", current, latest);
        LoggerUtils.warn("Download it at: {}{}", RESOURCE_URL, resourceId);

        String message = MessageKeys.UPDATE_AVAILABLE.getMessage()
                .replace("{current}", current)
                .replace("{new}", latest);

        PlayerShop.getScheduler().runTask(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission(UPDATE_PERMISSION)) player.sendMessage(message);
            }
        });
    }
}