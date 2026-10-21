package com.playerpicked.playershop.utils;

import com.playerpicked.playershop.PlayerShop;
import com.playerpicked.playershop.commands.CommandPlayerShop;
import com.playerpicked.playershop.listener.ChunkListener;
import com.playerpicked.playershop.listener.MenuListener;
import com.playerpicked.playershop.listener.ShopListener;
import com.playerpicked.playershop.listener.TeleportListener;
import lombok.experimental.UtilityClass;
import revxrsal.commands.bukkit.BukkitLamp;

@UtilityClass
public class RegisterUtils {

    private final PlayerShop plugin = PlayerShop.getInstance();

    public void registerListeners() {
        plugin.getServer().getPluginManager().registerEvents(new ShopListener(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new MenuListener(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new ChunkListener(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new TeleportListener(), plugin);
    }

    public void registerCommands() {
        var lamp = BukkitLamp.builder(plugin).build();

        lamp.register(new CommandPlayerShop());
    }
}