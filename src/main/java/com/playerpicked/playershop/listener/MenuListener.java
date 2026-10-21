package com.playerpicked.playershop.listener;

import com.playerpicked.playershop.PlayerShop;
import com.playerpicked.playershop.data.MenuController;
import com.playerpicked.playershop.guis.Menu;
import com.playerpicked.playershop.guis.ShopBoundMenu;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class MenuListener implements Listener {
    @EventHandler
    public void onInventoryClick(final @NotNull InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof ShopBoundMenu bound && bound.isStale()) {
            event.setCancelled(true);

            Player player = (Player) event.getWhoClicked();
            PlayerShop.getScheduler().runTask(player::closeInventory);
            return;
        }

        if (holder instanceof Menu menu) menu.handleMenu(event);
    }

    @EventHandler
    public void onInventoryClose(final @NotNull InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof Menu menu) menu.close();
    }


    @EventHandler
    public void onPlayerQuit(final @NotNull PlayerQuitEvent event) {
        Player player = event.getPlayer();
        MenuController.remove(player);
    }
}