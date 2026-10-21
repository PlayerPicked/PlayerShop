package com.playerpicked.playershop.hooks.impl.currency.impl;

import com.playerpicked.playershop.hooks.impl.currency.Currency;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellenteconomy.api.ExcellentEconomyAPI;

public final class ExcellentEconomyHook implements Currency {

    private final ExcellentEconomyAPI api;
    private final String id;
    private final String currencyId;

    public ExcellentEconomyHook(@NotNull ExcellentEconomyAPI api, @NotNull String id, @NotNull String currencyId) {
        this.api = api;
        this.id = id;
        this.currencyId = currencyId;
    }

    @Contract(pure = true)
    @Override
    public @NotNull String id() {
        return id;
    }

    @Override
    public boolean has(@NotNull OfflinePlayer player, double amount) {
        Player online = player.getPlayer();
        double balance = online != null
                ? api.getBalance(online, currencyId)
                : api.getBalanceAsync(player.getUniqueId(), currencyId).join();
        return balance >= amount;
    }

    @Override
    public boolean withdraw(@NotNull OfflinePlayer player, double amount) {
        Player online = player.getPlayer();
        if (online != null) return api.withdraw(online, currencyId, amount);
        return api.withdrawAsync(player.getUniqueId(), currencyId, amount).join().success();
    }

    @Override
    public void deposit(@NotNull OfflinePlayer player, double amount) {
        Player online = player.getPlayer();
        if (online != null) {
            api.deposit(online, currencyId, amount);
            return;
        }
        api.depositAsync(player.getUniqueId(), currencyId, amount);
    }

    @NotNull
    @Contract(pure = true)
    @Override
    public String format(double amount) {
        return String.valueOf(amount);
    }
}
