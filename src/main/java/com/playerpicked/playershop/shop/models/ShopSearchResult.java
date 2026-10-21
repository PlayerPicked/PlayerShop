package com.playerpicked.playershop.shop.models;

import com.playerpicked.playershop.identifiers.ShopMode;
import org.jetbrains.annotations.NotNull;

public record ShopSearchResult(@NotNull PlayerShopData shop, int stock, int space, int score) {
    public boolean isAvailable() {
        return shop.getMode() == ShopMode.SELL ? stock > 0 : space > 0;
    }
}