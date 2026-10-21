package com.playerpicked.playershop.database;

import com.playerpicked.playershop.shop.models.PlayerShopData;
import com.playerpicked.playershop.shop.models.PlayerShopTransaction;
import com.playerpicked.playershop.shop.models.PlayerShopStorage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface Database {
    CompletableFuture<Void> initialize();

    CompletableFuture<Void> insertShop(PlayerShopData shop);
    CompletableFuture<Void> updateShop(PlayerShopData shop);
    CompletableFuture<Void> deleteShop(UUID shopId);

    CompletableFuture<List<PlayerShopData>> findAllShops();
    CompletableFuture<List<PlayerShopData>> findShopsByWorld(String world);

    CompletableFuture<Void> insertTransaction(UUID shopId, UUID playerUuid, String type, int amount, double price);
    CompletableFuture<List<PlayerShopTransaction>> getTransactions(UUID shopId, int limit);

    CompletableFuture<Optional<PlayerShopStorage>> loadStorage(UUID shopId);
    CompletableFuture<Void> saveStorage(PlayerShopStorage storage);
    CompletableFuture<Void> deleteStorage(UUID shopId);

    void shutdown();
}