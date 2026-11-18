package com.playerpicked.playershop.shop.service;

import com.playerpicked.playershop.PlayerShop;
import com.playerpicked.playershop.database.DatabaseManager;
import com.playerpicked.playershop.shop.manager.PlayerShopManager;
import com.playerpicked.playershop.shop.models.PlayerShopData;
import com.playerpicked.playershop.utils.LoggerUtils;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class ShopLoader {

    public static void load(PlayerShopManager manager, ShopVisualService visuals) {
        apply(DatabaseManager.getDatabase().findAllShops(), manager, visuals, "all worlds");
    }

    public static void loadByWorld(PlayerShopManager manager, String world) {
        apply(DatabaseManager.getDatabase().findShopsByWorld(world), manager, PlayerShop.getInstance().getVisualService(), world);
    }

    private static void apply(@NotNull CompletableFuture<List<PlayerShopData>> future, PlayerShopManager manager, ShopVisualService visuals, String scope) {
        future.thenAccept(list -> PlayerShop.getScheduler().runTask(() -> {
            list.forEach(manager::register);

            PlayerShopService service = PlayerShop.getInstance().getShopService();

            for (PlayerShopData shop : list) {
                World world = shop.getLocation().getWorld();
                if (world == null || !world.isChunkLoaded(shop.getX() >> 4, shop.getZ() >> 4)) continue;

                try {
                    service.setupBlock(shop.getLocation());
                    visuals.spawn(shop);
                } catch (Exception exception) {
                    LoggerUtils.error("Failed to spawn visuals for shop " + shop.getShopId() + ": " + exception);
                }
            }

            LoggerUtils.info("Loaded " + list.size() + " shop(s) from " + scope);
        })).exceptionally(throwable -> {
            LoggerUtils.error("Loading shops from " + scope + " failed: " + throwable);
            return null;
        });
    }
}