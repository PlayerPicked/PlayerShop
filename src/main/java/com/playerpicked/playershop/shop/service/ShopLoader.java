package com.playerpicked.playershop.shop.service;

import com.playerpicked.playershop.PlayerShop;
import com.playerpicked.playershop.database.DatabaseManager;
import com.playerpicked.playershop.shop.manager.PlayerShopManager;

public final class ShopLoader {

    public static void load(PlayerShopManager manager, ShopVisualService visuals) {
        DatabaseManager.getDatabase().findAllShops().thenAccept(list -> {
            PlayerShop.getScheduler().runTask(() -> {
                list.forEach(shop -> {
                    manager.register(shop);
                    visuals.spawn(shop);
                });
            });
        });
    }

    public static void loadByWorld(PlayerShopManager manager, String world) {
        DatabaseManager.getDatabase().findShopsByWorld(world).thenAccept(list -> {
            PlayerShop.getScheduler().runTask(() -> {
                list.forEach(shop -> {
                    manager.register(shop);
                    PlayerShop.getInstance().getVisualService().spawn(shop);
                });

            });
        });
    }
}