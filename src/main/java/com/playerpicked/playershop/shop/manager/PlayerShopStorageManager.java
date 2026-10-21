package com.playerpicked.playershop.shop.manager;

import com.playerpicked.playershop.database.Database;
import com.playerpicked.playershop.database.DatabaseManager;
import com.playerpicked.playershop.shop.models.PlayerShopStorage;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerShopStorageManager {

    private final Map<UUID, PlayerShopStorage> cache = new ConcurrentHashMap<>();
    private final Database repository = DatabaseManager.getDatabase();

    public CompletableFuture<PlayerShopStorage> getOrLoad(UUID shopId) {
        PlayerShopStorage cached = cache.get(shopId);
        if (cached != null) return CompletableFuture.completedFuture(cached);

        return repository.loadStorage(shopId).thenApply(optional ->
                cache.computeIfAbsent(shopId, id -> optional.orElseGet(() -> new PlayerShopStorage(id))));
    }

    public PlayerShopStorage getOrLoadSync(UUID shopId) {
        return cache.computeIfAbsent(shopId, id -> repository.loadStorage(id).join()
                .orElse(new PlayerShopStorage(id)));
    }

    public PlayerShopStorage getCached(UUID shopId) {
        return cache.get(shopId);
    }

    public void saveAsync(PlayerShopStorage storage) {
        repository.saveStorage(storage);
    }

    public void remove(UUID shopId) {
        cache.remove(shopId);
        repository.deleteStorage(shopId);
    }

    @NotNull
    @Contract(pure = true)
    public Collection<PlayerShopStorage> getAll() {
        return cache.values();
    }
}