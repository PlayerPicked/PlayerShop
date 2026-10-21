package com.playerpicked.playershop.shop.models;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public record ShopSearchResponse(@NotNull List<ShopSearchResult> results, int hidden) {

    @NotNull
    public static ShopSearchResponse empty() {
        return new ShopSearchResponse(List.of(), 0);
    }

    public boolean isEmpty() {
        return results.isEmpty();
    }
}