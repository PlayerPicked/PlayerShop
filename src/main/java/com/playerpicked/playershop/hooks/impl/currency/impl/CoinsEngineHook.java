package com.playerpicked.playershop.hooks.impl.currency.impl;

import com.playerpicked.playershop.hooks.impl.currency.Currency;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.coinsengine.api.CoinsEngineAPI;

public final class CoinsEngineHook implements Currency {

    private final String currencyId;

    public CoinsEngineHook(String currencyId) {
        this.currencyId = currencyId;
    }

    @Contract(pure = true)
    @Override
    public @NotNull String id() {
        return "coinsengine:" + currencyId;
    }

    @Override
    public boolean has(@NotNull OfflinePlayer player, double amount) {
        return CoinsEngineAPI.getBalance(player.getUniqueId(), currencyId) >= amount;
    }

    @Override
    public boolean withdraw(@NotNull OfflinePlayer player, double amount) {
        return CoinsEngineAPI.removeBalance(player.getUniqueId(), currencyId, amount);
    }

    @Override
    public void deposit(@NotNull OfflinePlayer player, double amount) {
        CoinsEngineAPI.addBalance(player.getUniqueId(), currencyId, amount);
    }

    @NotNull
    @Contract(pure = true)
    @Override
    public String format(double amount) {
        return String.valueOf(amount);
    }
}