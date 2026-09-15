package com.mongenscave.mcplayershop.identifiers.keys;

import com.mongenscave.mcplayershop.McPlayerShop;
import com.mongenscave.mcplayershop.config.Config;
import com.mongenscave.mcplayershop.processor.MessageProcessor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

@Getter
public enum MessageKeys {
    RELOAD("messages.reload"),
    UPDATE_AVAILABLE("messages.update-available"),
    NO_PERMISSION("messages.no-permission"),
    PLAYER_REQUIRED("messages.player-required"),
    PLAYER_NOT_FOUND("messages.player-not-found"),
    MISSING_ARGUMENT("messages.missing-argument"),

    SHOP_STORAGE_NOT_EMPTY("messages.shop.storage-not-empty"),

    SHOP_ADMIN_DELETED("messages.shop.admin-deleted"),
    SHOP_ADMIN_DELETE_CONFIRM("messages.shop.admin-delete-confirm"),
    SHOP_ADMIN_DELETE_NO_TARGET("messages.shop.admin-delete-no-target"),
    SHOP_DELETED("messages.shop.deleted"),

    SHOP_TRANSACTION_SUCCESS("messages.shop.transaction-success"),
    SHOP_TRANSACTION_FAILED("messages.shop.transaction-failed"),
    SHOP_OWNER_NOTIFY_BUY("messages.shop.owner-notify.buy"),
    SHOP_OWNER_NOTIFY_SELL("messages.shop.owner-notify.sell"),
    SHOP_ERROR_NOT_ENOUGH_MONEY("messages.shop.errors.not-enough-money"),
    SHOP_ERROR_NOT_ENOUGH_ITEMS("messages.shop.errors.not-enough-items"),
    SHOP_ERROR_INVENTORY_FULL("messages.shop.errors.inventory-full"),
    SHOP_ERROR_STORAGE_FULL("messages.shop.errors.storage-full"),
    SHOP_ERROR_OWNER_NO_MONEY("messages.shop.errors.owner-no-money"),
    SHOP_ERROR_SHOP_EMPTY("messages.shop.errors.shop-empty"),
    SHOP_TRADE_INVALID_AMOUNT("messages.shop.errors.invalid-amount"),
    SHOP_CREATE_FAILED_BLOCK_CONTENT_NOT_EMPTY("messages.shop.errors.block-content-not-empty"),
    SHOP_CREATE_FAILED_DOUBLE_CHEST("messages.shop.errors.double-chest"),

    SHOP_STORAGE_NOT_EMPTY_SIMPLE("messages.shop.storage-not-empty-simple"),

    SHOP_CURRENCY_UPDATED("messages.shop.currency-updated"),
    SHOP_CURRENCY_NO_PERMISSION("messages.shop.currency-no-permission"),

    SHOP_PRICE_INVALID_NUMBER("messages.shop.price.invalid-number"),
    SHOP_PRICE_NEGATIVE("messages.shop.price.negative"),
    SHOP_PRICE_TOO_LARGE("messages.shop.price.too-large"),
    SHOP_PRICE_UPDATED("messages.shop.price.updated"),

    DIALOG_PRICE_TITLE("messages.dialog.price.title"),
    DIALOG_PRICE_BODY("messages.dialog.price.body"),
    DIALOG_PRICE_INPUT("messages.dialog.price.input"),

    DIALOG_CONFIRM("messages.dialog.confirm"),
    DIALOG_CONFIRM_LORE("messages.dialog.confirm-lore"),

    DIALOG_CANCEL("messages.dialog.cancel"),
    DIALOG_CANCEL_LORE("messages.dialog.cancel-lore"),

    SHOP_MODE_BUY("messages.shop.mode.buy"),
    SHOP_MODE_SELL("messages.shop.mode.sell"),

    SHOP_LIMIT_REACHED("messages.shop.limit-reached"),
    SHOP_CREATION_MODE_ENABLED("messages.shop.creation-mode-enabled"),
    SHOP_CREATION_MODE_DISABLED("messages.shop.creation-mode-disabled"),

    SEARCH_DISABLED("messages.search.disabled"),
    SEARCH_TOO_SHORT("messages.search.too-short"),
    SEARCH_SEARCHING("messages.search.searching"),
    SEARCH_NO_RESULTS("messages.search.no-results"),
    SEARCH_ALL_UNAVAILABLE("messages.search.all-unavailable"),
    SEARCH_RESULTS("messages.search.results"),

    LISTING_ENABLED("messages.shop.listing.enabled"),
    LISTING_DISABLED("messages.shop.listing.disabled"),

    VISIT_SET("messages.shop.visit.set"),
    VISIT_RESET("messages.shop.visit.reset"),
    VISIT_TOO_FAR("messages.shop.visit.too-far"),
    VISIT_WRONG_WORLD("messages.shop.visit.wrong-world"),
    VISIT_UNSAFE("messages.shop.visit.unsafe"),

    TELEPORT_DISABLED("messages.teleport.disabled"),
    TELEPORT_WARMUP("messages.teleport.warmup"),
    TELEPORT_CANCELLED("messages.teleport.cancelled"),
    TELEPORT_COOLDOWN("messages.teleport.cooldown"),
    TELEPORT_SUCCESS("messages.teleport.success"),
    TELEPORT_PROTECTION("messages.teleport.protection"),
    TELEPORT_NO_SAFE_LOCATION("messages.teleport.no-safe-location"),
    TELEPORT_SHOP_GONE("messages.teleport.shop-gone"),
    TELEPORT_WORLD_NOT_LOADED("messages.teleport.world-not-loaded"),
    TELEPORT_WORLD_BLACKLISTED("messages.teleport.world-blacklisted"),
    TELEPORT_FAILED("messages.teleport.failed"),

    ISLAND_LOCKED("messages.island.locked"),
    ISLAND_BANNED("messages.island.banned"),

    TIME_DAY("messages.time.day"),
    TIME_DAY_PLURAL("messages.time.day-plural"),
    TIME_HOUR("messages.time.hour"),
    TIME_HOUR_PLURAL("messages.time.hour-plural"),
    TIME_MINUTE("messages.time.minute"),
    TIME_MINUTE_PLURAL("messages.time.minute-plural"),
    TIME_SECOND("messages.time.second"),
    TIME_SECOND_PLURAL("messages.time.second-plural"),

    TIME_DAY_SHORT("messages.time.short.day"),
    TIME_HOUR_SHORT("messages.time.short.hour"),
    TIME_MINUTE_SHORT("messages.time.short.minute"),
    TIME_SECOND_SHORT("messages.time.short.second");

    private final String path;
    private static final Config config = McPlayerShop.getInstance().getLanguage();

    MessageKeys(@NotNull String path) {
        this.path = path;
    }

    public boolean isEmpty() {
        String raw = config.getString(path);
        return raw == null || raw.isEmpty();
    }

    public @NotNull String getMessage() {
        return MessageProcessor.process(config.getString(path))
                .replace("%prefix%", MessageProcessor.process(config.getString("prefix")));
    }

    @NotNull
    public @Unmodifiable List<String> getMessages() {
        return config.getStringList(path)
                .stream()
                .map(MessageProcessor::process)
                .toList();
    }
}