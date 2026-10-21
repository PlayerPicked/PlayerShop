package com.playerpicked.playershop.shop.service;

import com.playerpicked.playershop.PlayerShop;
import com.playerpicked.playershop.identifiers.keys.MessageKeys;
import com.playerpicked.playershop.shop.models.PlayerShopData;
import com.playerpicked.playershop.utils.AmountFormatUtil;
import com.playerpicked.playershop.utils.DialogUtil;
import io.papermc.paper.dialog.DialogResponseView;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public final class ShopPriceService {

    private static final String INPUT_KEY = "price";

    private final PlayerShop plugin = PlayerShop.getInstance();
    private final PlayerShopService shopService = plugin.getShopService();

    public void openEditor(@NotNull Player player, @NotNull PlayerShopData shop) {
        if (!shop.getOwnerUuid().equals(player.getUniqueId())) return;
        player.showDialog(DialogUtil.createPriceDialog((view, audience) -> handleInput(view, audience, shop)));
    }

    private void handleInput(@NotNull DialogResponseView view, Object audience, @NotNull PlayerShopData shop) {
        String input = view.getText(INPUT_KEY);
        if (input == null || input.isEmpty()) return;

        double price;

        try {
            price = Double.parseDouble(input);
        } catch (Exception ex) {
            if (audience instanceof Player player) {
                player.sendMessage(MessageKeys.SHOP_PRICE_INVALID_NUMBER.getMessage());
                openEditor(player, shop);
            }

            return;
        }

        if (price < 0) {
            if (audience instanceof Player player) {
                player.sendMessage(MessageKeys.SHOP_PRICE_NEGATIVE.getMessage());
                openEditor(player, shop);
            }

            return;
        }

        if (price > 1_000_000_000) {
            if (audience instanceof Player player) {
                player.sendMessage(MessageKeys.SHOP_PRICE_TOO_LARGE.getMessage());
                openEditor(player, shop);
            }

            return;
        }

        shop.setPrice(price);
        shopService.update(shop);
        plugin.getVisualService().update(shop);

        if (audience instanceof Player player) {
            player.sendMessage(MessageKeys.SHOP_PRICE_UPDATED.getMessage()
                    .replace("{price}", AmountFormatUtil.format(price)));
        }
    }
}