package com.playerpicked.playershop.identifiers;

import org.jetbrains.annotations.NotNull;

public enum SearchFilter {
    SELL,
    BUY,
    ALL;

    @NotNull
    public SearchFilter next() {
        return switch (this) {
            case SELL -> BUY;
            case BUY -> ALL;
            case ALL -> SELL;
        };
    }

    public boolean matches(@NotNull ShopMode mode) {
        return switch (this) {
            case SELL -> mode == ShopMode.SELL;
            case BUY -> mode == ShopMode.BUY;
            case ALL -> true;
        };
    }

    @NotNull
    public static SearchFilter parse(String raw, @NotNull SearchFilter fallback) {
        if (raw == null) return fallback;

        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }
}