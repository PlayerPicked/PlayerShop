package com.playerpicked.playershop.hooks.impl.currency.impl;

import com.playerpicked.playershop.hooks.impl.currency.Currency;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public record VaultHook(Economy economy) implements Currency {

    @NotNull
    @Contract(pure = true)
    @Override
    public String id() {
        return "vault";
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return economy.has(player, amount);
    }

    @Override
    public boolean withdraw(OfflinePlayer player, double amount) {
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    @Override
    public void deposit(OfflinePlayer player, double amount) {
        economy.depositPlayer(player, amount);
    }

    @Override
    public String format(double amount) {
        return economy.format(amount);
    }
}