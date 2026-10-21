package com.playerpicked.playershop.hooks.impl.currency;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CurrencyManager {

    private final Map<String, Currency> providers = new ConcurrentHashMap<>();

    public void register(Currency currency) {
        providers.put(currency.id().toLowerCase(), currency);
    }

    public Currency get(@NotNull String id) {
        return providers.get(id.toLowerCase());
    }

    @NotNull
    @Contract(pure = true)
    public Collection<Currency> getAll() {
        return providers.values();
    }
}