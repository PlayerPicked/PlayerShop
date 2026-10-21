package com.playerpicked.playershop.shop.service;

import com.playerpicked.playershop.shop.models.PlayerShopData;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ShopDeleteConfirmService {
    public static final int TIMEOUT_SECONDS = 10;

    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();

    private record Pending(@NotNull UUID shopId, long expiresAt) {}

    public boolean confirm(@NotNull Player player, @NotNull PlayerShopData shop) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        Pending current = pending.get(uuid);

        if (current != null && current.shopId().equals(shop.getShopId()) && current.expiresAt() > now) {
            pending.remove(uuid);
            return true;
        }

        pending.entrySet().removeIf(entry -> entry.getValue().expiresAt() <= now);
        pending.put(uuid, new Pending(shop.getShopId(), now + TIMEOUT_SECONDS * 1000L));

        return false;
    }

    public void clear(@NotNull UUID uuid) {
        pending.remove(uuid);
    }
}