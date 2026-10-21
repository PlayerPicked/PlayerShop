package com.playerpicked.playershop.identifiers;

import org.jetbrains.annotations.NotNull;

public enum SearchSort {
    PRICE_LOW,
    PRICE_HIGH,
    STOCK,
    DISTANCE;

    @NotNull
    public SearchSort next() {
        return switch (this) {
            case PRICE_LOW -> PRICE_HIGH;
            case PRICE_HIGH -> STOCK;
            case STOCK -> DISTANCE;
            case DISTANCE -> PRICE_LOW;
        };
    }

    @NotNull
    public static SearchSort parse(String raw, @NotNull SearchSort fallback) {
        if (raw == null) return fallback;

        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }
}