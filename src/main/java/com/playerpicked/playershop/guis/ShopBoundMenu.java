package com.playerpicked.playershop.guis;

import com.playerpicked.playershop.PlayerShop;
import com.playerpicked.playershop.shop.models.PlayerShopData;
import org.jetbrains.annotations.NotNull;

public interface ShopBoundMenu {

    @NotNull PlayerShopData getShop();

    default boolean isStale() {
        PlayerShopData shop = getShop();

        return PlayerShop.getInstance().getShopManager()
                .get(shop.getLocation())
                .map(current -> !current.getShopId().equals(shop.getShopId()))
                .orElse(true);
    }
}