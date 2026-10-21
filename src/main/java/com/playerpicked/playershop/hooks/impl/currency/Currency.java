package com.playerpicked.playershop.hooks.impl.currency;

import org.bukkit.OfflinePlayer;

public interface Currency {
    String id();
    boolean has(OfflinePlayer player, double amount);
    boolean withdraw(OfflinePlayer player, double amount);
    void deposit(OfflinePlayer player, double amount);
    String format(double amount);
}