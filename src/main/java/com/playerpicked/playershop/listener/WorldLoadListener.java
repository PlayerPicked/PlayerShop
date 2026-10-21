package com.playerpicked.playershop.listener;

import com.infernalsuite.asp.api.events.LoadSlimeWorldEvent;
import com.infernalsuite.asp.api.world.SlimeWorld;
import com.playerpicked.playershop.PlayerShop;
import com.playerpicked.playershop.shop.service.ShopLoader;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public class WorldLoadListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSlimeWorldLoad(@NotNull LoadSlimeWorldEvent event) {
        SlimeWorld slimeWorld = event.getSlimeWorld();
        String worldName = slimeWorld.getName();

        PlayerShop.getScheduler().runTaskLater(() -> ShopLoader.loadByWorld(PlayerShop.getInstance().getShopManager(), worldName), 20L);
    }
}