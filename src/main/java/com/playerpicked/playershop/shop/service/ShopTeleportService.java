package com.playerpicked.playershop.shop.service;

import com.github.Anon8281.universalScheduler.scheduling.tasks.MyScheduledTask;
import com.playerpicked.playershop.PlayerShop;
import com.playerpicked.playershop.identifiers.IslandAccess;
import com.playerpicked.playershop.identifiers.keys.ConfigKeys;
import com.playerpicked.playershop.identifiers.keys.MenuKeys;
import com.playerpicked.playershop.identifiers.keys.MessageKeys;
import com.playerpicked.playershop.shop.models.PlayerShopData;
import com.playerpicked.playershop.utils.SafeLocationUtil;
import com.playerpicked.playershop.utils.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class ShopTeleportService {

    public static final String TELEPORT_PERMISSION = "playershop.search.teleport";
    public static final String BYPASS_PERMISSION = "playershop.teleport.bypass";

    private final PlayerShop plugin = PlayerShop.getInstance();

    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Long> protectedUntil = new ConcurrentHashMap<>();
    private final Map<UUID, MyScheduledTask> pending = new ConcurrentHashMap<>();

    public void request(@NotNull Player player, @NotNull PlayerShopData shop) {
        if (!ConfigKeys.TELEPORT_ENABLED.getBoolean(true)) {
            deny(player, MessageKeys.TELEPORT_DISABLED.getMessage());
            return;
        }

        if (!player.hasPermission(TELEPORT_PERMISSION)) {
            deny(player, MessageKeys.NO_PERMISSION.getMessage());
            return;
        }

        boolean bypass = player.hasPermission(BYPASS_PERMISSION);
        boolean own = shop.getOwnerUuid().equals(player.getUniqueId());

        if (plugin.getShopManager().get(shop.getLocation()).isEmpty()) {
            deny(player, MessageKeys.TELEPORT_SHOP_GONE.getMessage());
            return;
        }

        if (!shop.isListed() && !own && !bypass) {
            deny(player, MessageKeys.TELEPORT_SHOP_GONE.getMessage());
            return;
        }

        World world = shop.getLocation().getWorld();

        if (world == null) {
            deny(player, MessageKeys.TELEPORT_WORLD_NOT_LOADED.getMessage());
            return;
        }

        if (!bypass && isBlacklisted(world.getName())) {
            deny(player, MessageKeys.TELEPORT_WORLD_BLACKLISTED.getMessage());
            return;
        }

        long remaining = cooldownRemaining(player.getUniqueId());

        if (remaining > 0 && !bypass && !own) {
            deny(player, MessageKeys.TELEPORT_COOLDOWN.getMessage()
                    .replace("{seconds}", String.valueOf(remaining)));
            return;
        }

        if (!bypass) {
            IslandAccess access = plugin.getIslandManager().check(player, shop.getLocation());

            if (access == IslandAccess.BANNED) {
                deny(player, MessageKeys.ISLAND_BANNED.getMessage());
                return;
            }

            if (access == IslandAccess.LOCKED) {
                deny(player, MessageKeys.ISLAND_LOCKED.getMessage());
                return;
            }
        }

        int warmup = (own || bypass) ? 0 : Math.max(0, ConfigKeys.TELEPORT_WARMUP.getInt(3));

        cancel(player, false);

        if (warmup == 0) {
            resolveAndTeleport(player, shop);
            return;
        }

        player.sendMessage(MessageKeys.TELEPORT_WARMUP.getMessage()
                .replace("{seconds}", String.valueOf(warmup)));

        UUID uuid = player.getUniqueId();

        MyScheduledTask task = PlayerShop.getScheduler().runTaskLater(() -> {
            pending.remove(uuid);

            Player online = Bukkit.getPlayer(uuid);
            if (online == null || !online.isOnline()) return;

            resolveAndTeleport(online, shop);
        }, warmup * 20L);

        pending.put(uuid, task);
    }

    public boolean cancel(@NotNull Player player, boolean notify) {
        MyScheduledTask running = pending.remove(player.getUniqueId());
        if (running == null) return false;

        running.cancel();

        if (notify) {
            player.sendMessage(MessageKeys.TELEPORT_CANCELLED.getMessage());
            SoundUtil.play(player, MenuKeys.SHOP_SEARCH_SOUND_ERROR.getString());
        }

        return true;
    }

    public boolean hasPending(@NotNull UUID uuid) {
        return pending.containsKey(uuid);
    }

    public boolean isProtected(@NotNull UUID uuid) {
        Long until = protectedUntil.get(uuid);

        if (until == null) return false;

        if (until <= System.currentTimeMillis()) {
            protectedUntil.remove(uuid);
            return false;
        }

        return true;
    }

    public void clearProtection(@NotNull UUID uuid) {
        protectedUntil.remove(uuid);
    }

    public void forget(@NotNull UUID uuid) {
        MyScheduledTask running = pending.remove(uuid);
        if (running != null) running.cancel();

        protectedUntil.remove(uuid);
    }

    public boolean isCancelOnMove() {
        return ConfigKeys.TELEPORT_CANCEL_ON_MOVE.getBoolean(true);
    }

    public boolean isCancelOnDamage() {
        return ConfigKeys.TELEPORT_CANCEL_ON_DAMAGE.getBoolean(true);
    }

    private void resolveAndTeleport(@NotNull Player player, @NotNull PlayerShopData shop) {
        Location shopLocation = shop.getLocation();
        World world = shopLocation.getWorld();

        if (world == null) {
            deny(player, MessageKeys.TELEPORT_WORLD_NOT_LOADED.getMessage());
            return;
        }

        UUID uuid = player.getUniqueId();

        loadArea(world, shop)
                .thenRun(() -> PlayerShop.getScheduler().runTask(shopLocation, () -> {
                    Location destination = resolveDestination(shop);

                    Player online = Bukkit.getPlayer(uuid);
                    if (online == null || !online.isOnline()) return;

                    PlayerShop.getScheduler().runTask(online, () -> {
                        if (!online.isOnline()) return;

                        if (destination == null) {
                            deny(online, MessageKeys.TELEPORT_NO_SAFE_LOCATION.getMessage());
                            return;
                        }

                        teleport(online, shop, destination);
                    });
                }))
                .exceptionally(throwable -> {
                    Player online = Bukkit.getPlayer(uuid);
                    if (online == null || !online.isOnline()) return null;

                    PlayerShop.getScheduler().runTask(online, () -> deny(online, MessageKeys.TELEPORT_FAILED.getMessage()));

                    return null;
                });
    }

    @Nullable
    private Location resolveDestination(@NotNull PlayerShopData shop) {
        Location visit = shop.getVisitLocation();

        if (visit != null && visit.getWorld() != null && SafeLocationUtil.isSafe(visit)) return visit.clone();

        return SafeLocationUtil.findSafeSpot(shop.getLocation())
                .map(location -> SafeLocationUtil.facing(location, shop.getLocation()))
                .orElse(null);
    }

    private void teleport(@NotNull Player player, @NotNull PlayerShopData shop, @NotNull Location destination) {
        if (player.isInsideVehicle()) player.leaveVehicle();
        player.eject();

        player.teleportAsync(destination).thenAccept(success -> PlayerShop.getScheduler().runTask(player, () -> {
            if (!player.isOnline()) return;

            if (!Boolean.TRUE.equals(success)) {
                deny(player, MessageKeys.TELEPORT_FAILED.getMessage());
                return;
            }

            int cooldown = Math.max(0, ConfigKeys.TELEPORT_COOLDOWN.getInt(5));
            if (cooldown > 0) cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + cooldown * 1000L);

            player.sendMessage(MessageKeys.TELEPORT_SUCCESS.getMessage()
                    .replace("{owner}", shop.getOwnerName()));

            SoundUtil.play(player, MenuKeys.SHOP_SEARCH_SOUND_TELEPORT.getString());

            applyProtection(player);
        }));
    }

    private void applyProtection(@NotNull Player player) {
        int seconds = Math.max(0, ConfigKeys.TELEPORT_PROTECTION.getInt(5));
        if (seconds == 0) return;

        protectedUntil.put(player.getUniqueId(), System.currentTimeMillis() + seconds * 1000L);

        if (!MessageKeys.TELEPORT_PROTECTION.isEmpty()) {
            player.sendMessage(MessageKeys.TELEPORT_PROTECTION.getMessage()
                    .replace("{seconds}", String.valueOf(seconds)));
        }
    }

    @NotNull
    private CompletableFuture<Void> loadArea(@NotNull World world, @NotNull PlayerShopData shop) {
        Set<Long> chunks = new HashSet<>();

        collectChunks(chunks, shop.getX(), shop.getZ(), SafeLocationUtil.getSearchRadius() + 1);

        Location visit = shop.getVisitLocation();

        if (visit != null && world.equals(visit.getWorld())) {
            collectChunks(chunks, visit.getBlockX(), visit.getBlockZ(), 1);
        }

        List<CompletableFuture<Chunk>> futures = new ArrayList<>(chunks.size());

        for (long key : chunks) {
            futures.add(world.getChunkAtAsync((int) (key >> 32), (int) key, false));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    private void collectChunks(@NotNull Set<Long> chunks, int blockX, int blockZ, int radius) {
        int minChunkX = (blockX - radius) >> 4;
        int maxChunkX = (blockX + radius) >> 4;
        int minChunkZ = (blockZ - radius) >> 4;
        int maxChunkZ = (blockZ + radius) >> 4;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                chunks.add(((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL));
            }
        }
    }

    private long cooldownRemaining(@NotNull UUID uuid) {
        Long until = cooldowns.get(uuid);
        if (until == null) return 0;

        long remaining = until - System.currentTimeMillis();

        if (remaining <= 0) {
            cooldowns.remove(uuid);
            return 0;
        }

        return (remaining + 999) / 1000;
    }

    private boolean isBlacklisted(@NotNull String world) {
        List<String> blacklisted = ConfigKeys.TELEPORT_BLACKLISTED_WORLDS.getList(List.of());
        if (blacklisted == null || blacklisted.isEmpty()) return false;

        for (String entry : blacklisted) {
            if (entry == null) continue;
            if (entry.toLowerCase(Locale.ROOT).equals(world.toLowerCase(Locale.ROOT))) return true;
        }

        return false;
    }

    private void deny(@NotNull Player player, @NotNull String message) {
        player.sendMessage(message);
        SoundUtil.play(player, MenuKeys.SHOP_SEARCH_SOUND_ERROR.getString());
    }
}