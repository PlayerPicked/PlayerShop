package com.playerpicked.playershop.listener;

import com.playerpicked.playershop.PlayerShop;
import com.playerpicked.playershop.shop.manager.PlayerShopManager;
import com.playerpicked.playershop.shop.models.PlayerShopData;
import com.playerpicked.playershop.shop.service.PlayerShopService;
import com.playerpicked.playershop.shop.service.ShopVisualService;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class ChunkListener implements Listener {

    private final PlayerShop plugin = PlayerShop.getInstance();

    @EventHandler
    public void onChunkLoad(@NotNull ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        PlayerShopManager manager = plugin.getShopManager();

        List<PlayerShopData> shops = manager.getByChunk(chunk);
        if (shops.isEmpty()) return;

        PlayerShopService service = plugin.getShopService();
        ShopVisualService visuals = plugin.getVisualService();

        PlayerShop.getScheduler().runTask(() -> {
            for (PlayerShopData shop : shops) {
                service.setupBlock(shop.getLocation());
                visuals.spawn(shop);
            }
        });
    }

    @EventHandler
    public void onChunkUnload(@NotNull ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        PlayerShopManager manager = plugin.getShopManager();

        List<PlayerShopData> shops = manager.getByChunk(chunk);
        if (shops.isEmpty()) return;

        ShopVisualService visuals = plugin.getVisualService();
        for (PlayerShopData shop : shops) {
            visuals.remove(shop.getShopId());
        }
    }
}