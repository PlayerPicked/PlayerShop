package com.playerpicked.playershop.identifiers.keys;

import com.playerpicked.playershop.PlayerShop;
import com.playerpicked.playershop.config.Config;
import com.playerpicked.playershop.processor.MessageProcessor;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public enum ConfigKeys {
    UPDATE_CHECKER_ENABLED("update-checker.enabled"),

    DATABASE_MYSQL("database.mysql"),
    DATABASE_POOL("database.pool"),

    SHOP_BLOCKS("shop.blocks"),

    STORAGE_MAX_ITEMS("storage.max-items"),

    SEARCH_ENABLED("search.enabled"),
    SEARCH_LISTED_BY_DEFAULT("search.listed-by-default"),
    SEARCH_MAX_RESULTS("search.max-results"),
    SEARCH_MIN_QUERY_LENGTH("search.min-query-length"),
    SEARCH_HIDE_UNAVAILABLE("search.hide-unavailable"),
    SEARCH_DEFAULT_FILTER("search.default-filter"),
    SEARCH_DEFAULT_SORT("search.default-sort"),

    SEARCH_VISIT_POINT_MAX_DISTANCE("search.visit-point.max-distance"),

    TELEPORT_ENABLED("search.teleport.enabled"),
    TELEPORT_WARMUP("search.teleport.warmup"),
    TELEPORT_COOLDOWN("search.teleport.cooldown"),
    TELEPORT_PROTECTION("search.teleport.protection"),
    TELEPORT_CANCEL_ON_MOVE("search.teleport.cancel-on-move"),
    TELEPORT_CANCEL_ON_DAMAGE("search.teleport.cancel-on-damage"),
    TELEPORT_BLACKLISTED_WORLDS("search.teleport.blacklisted-worlds"),

    TELEPORT_SAFETY_MAX_FALL("search.teleport.safety.max-fall"),
    TELEPORT_SAFETY_SEARCH_RADIUS("search.teleport.safety.search-radius"),
    TELEPORT_SAFETY_REQUIRE_ESCAPE("search.teleport.safety.require-escape"),
    TELEPORT_SAFETY_ALLOW_LIQUIDS("search.teleport.safety.allow-liquids"),
    TELEPORT_SAFETY_DANGEROUS_BLOCKS("search.teleport.safety.dangerous-blocks"),

    FORMATTING_AMOUNT_FORMAT("formatting.amount-format"),
    FORMATTING_TIME_FORMAT("formatting.time-format"),

    HOLOGRAM("hologram");

    private static final Config config = PlayerShop.getInstance().getConfiguration();
    private final String path;

    ConfigKeys(@NotNull String path) {
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

    public boolean getBoolean(boolean def) {
        return config.getBoolean(path, def);
    }

    public int getInt() {
        return config.getInt(path);
    }

    public int getInt(int def) {
        return config.getInt(path, def);
    }

    public long getLong() {
        return config.getLong(path);
    }

    public @NotNull String getRawString(@NotNull String def) {
        String value = config.getString(path, def);
        return value == null ? def : value;
    }

    public List<String> getList() {
        return config.getList(path);
    }

    public List<String> getList(List<String> def) {
        return config.getList(path, def);
    }

    public @NotNull Section getSection() {
        return config.getSection(path);
    }
}