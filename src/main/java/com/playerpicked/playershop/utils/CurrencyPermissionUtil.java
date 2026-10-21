package com.playerpicked.playershop.utils;

import com.playerpicked.playershop.PlayerShop;
import com.playerpicked.playershop.shop.service.PlayerShopService;
import lombok.experimental.UtilityClass;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public final class CurrencyPermissionUtil {

    private static final String PREFIX = "playershop.currency.";
    private static final String BASE = "hooks.currency.currencies.";

    public @NotNull String getPermission(@NotNull String currencyId) {
        String permission = PlayerShop.getInstance().getHooks().getString(BASE + currencyId + ".permission", PREFIX + currencyId.toLowerCase());

        return permission == null ? "" : permission.trim();
    }

    public boolean has(@NotNull Player player, @NotNull String currencyId) {
        String permission = getPermission(currencyId);
        if (permission.isEmpty()) return true;

        return player.hasPermission(permission) || player.hasPermission(PlayerShopService.BYPASS_PERMISSION);
    }
}