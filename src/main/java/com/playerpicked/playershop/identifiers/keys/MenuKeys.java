package com.playerpicked.playershop.identifiers.keys;

import com.playerpicked.playershop.PlayerShop;
import com.playerpicked.playershop.config.Config;
import com.playerpicked.playershop.processor.MessageProcessor;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public enum MenuKeys {

    SHOP_MAIN_TITLE("shop-main.title"),
    SHOP_MAIN_SIZE("shop-main.size"),
    SHOP_MAIN_TICK("shop-main.tick"),
    SHOP_MAIN_SOUND_OPEN("shop-main.sounds.open"),
    SHOP_MAIN_SOUND_ERROR("shop-main.sounds.error"),
    SHOP_MAIN_SOUND_ACTION("shop-main.sounds.action"),

    SHOP_TRANSACTIONS_TITLE("shop-transactions.title"),
    SHOP_TRANSACTIONS_SIZE("shop-transactions.size"),
    SHOP_TRANSACTIONS_TICK("shop-transactions.tick"),
    SHOP_TRANSACTIONS_SLOTS("shop-transactions.transaction-slots"),
    SHOP_TRANSACTIONS_SOUND_OPEN("shop-transactions.sounds.open"),
    SHOP_TRANSACTIONS_SOUND_ERROR("shop-transactions.sounds.error"),
    SHOP_TRANSACTIONS_SOUND_ACTION("shop-transactions.sounds.action"),
    SHOP_TRANSACTIONS_SOUND_PAGE("shop-transactions.sounds.page"),

    SHOP_STORAGE_TITLE("shop-storage.title"),
    SHOP_STORAGE_SIZE("shop-storage.size"),
    SHOP_STORAGE_TICK("shop-storage.tick"),
    SHOP_STORAGE_SLOTS("shop-storage.storage-slots"),
    SHOP_STORAGE_SOUND_OPEN("shop-storage.sounds.open"),
    SHOP_STORAGE_SOUND_ERROR("shop-storage.sounds.error"),
    SHOP_STORAGE_SOUND_ACTION("shop-storage.sounds.action"),

    SHOP_CURRENCY_TITLE("shop-currency.title"),
    SHOP_CURRENCY_SIZE("shop-currency.size"),
    SHOP_CURRENCY_TICK("shop-currency.tick"),
    SHOP_CURRENCY_SLOTS("shop-currency.currency-slots"),
    SHOP_CURRENCY_SOUND_OPEN("shop-currency.sounds.open"),
    SHOP_CURRENCY_SOUND_ERROR("shop-currency.sounds.error"),
    SHOP_CURRENCY_SOUND_ACTION("shop-currency.sounds.action"),

    SHOP_TRADE_TITLE("shop-trade.title"),
    SHOP_TRADE_SIZE("shop-trade.size"),
    SHOP_TRADE_TICK("shop-trade.tick"),
    SHOP_TRADE_MAX_AMOUNT("shop-trade.max-amount"),
    SHOP_TRADE_STEPS("shop-trade.steps"),
    SHOP_TRADE_SOUND_OPEN("shop-trade.sounds.open"),
    SHOP_TRADE_SOUND_ERROR("shop-trade.sounds.error"),
    SHOP_TRADE_SOUND_ACTION("shop-trade.sounds.action"),
    SHOP_TRADE_SOUND_MODIFY("shop-trade.sounds.modify"),

    SHOP_SEARCH_TITLE("shop-search.title"),
    SHOP_SEARCH_SIZE("shop-search.size"),
    SHOP_SEARCH_TICK("shop-search.tick"),
    SHOP_SEARCH_SLOTS("shop-search.result-slots"),
    SHOP_SEARCH_SOUND_OPEN("shop-search.sounds.open"),
    SHOP_SEARCH_SOUND_ERROR("shop-search.sounds.error"),
    SHOP_SEARCH_SOUND_ACTION("shop-search.sounds.action"),
    SHOP_SEARCH_SOUND_PAGE("shop-search.sounds.page"),
    SHOP_SEARCH_SOUND_TELEPORT("shop-search.sounds.teleport");

    private static final Config config = PlayerShop.getInstance().getGuis();
    private final String path;

    MenuKeys(@NotNull String path) {
        this.path = path;
    }

    public static @NotNull String getString(@NotNull String path) {
        return config.getString(path);
    }

    public @NotNull String getString() {
        return MessageProcessor.process(config.getString(path));
    }

    public boolean getBoolean() {
        return config.getBoolean(path);
    }

    public int getInt() {
        return config.getInt(path);
    }

    public List<Integer> getIntList() {
        return config.getIntList(path);
    }

    public List<String> getList() {
        return config.getList(path);
    }

    public @NotNull Section getSection() {
        return config.getSection(path);
    }
}