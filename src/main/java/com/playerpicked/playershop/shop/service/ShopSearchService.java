package com.playerpicked.playershop.shop.service;

import com.playerpicked.playershop.PlayerShop;
import com.playerpicked.playershop.identifiers.SearchFilter;
import com.playerpicked.playershop.identifiers.SearchSort;
import com.playerpicked.playershop.identifiers.ShopMode;
import com.playerpicked.playershop.identifiers.keys.ConfigKeys;
import com.playerpicked.playershop.shop.models.PlayerShopData;
import com.playerpicked.playershop.shop.models.PlayerShopStorage;
import com.playerpicked.playershop.shop.models.ShopSearchResponse;
import com.playerpicked.playershop.shop.models.ShopSearchResult;
import com.playerpicked.playershop.utils.ItemUtil;
import com.playerpicked.playershop.utils.StorageUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class ShopSearchService {

    private static final int CANDIDATE_CAP = 1000;
    private final PlayerShop plugin = PlayerShop.getInstance();

    @NotNull
    public CompletableFuture<ShopSearchResponse> search(@Nullable Player viewer, @Nullable String query, @NotNull SearchFilter filter, @NotNull SearchSort sort) {
        String normalized = ItemUtil.normalize(query);
        String[] tokens = normalized.isEmpty() ? new String[0] : normalized.split(" ");

        Location origin = viewer == null ? null : viewer.getLocation().clone();
        List<String> blacklisted = blacklistedWorlds();

        List<PlayerShopData> candidates = new ArrayList<>();
        List<Integer> scores = new ArrayList<>();

        for (PlayerShopData shop : plugin.getShopManager().getAll()) {
            if (candidates.size() >= CANDIDATE_CAP) break;

            if (!shop.isListed()) continue;
            if (!filter.matches(shop.getMode())) continue;
            if (blacklisted.contains(shop.getWorld().toLowerCase(Locale.ROOT))) continue;

            int score = score(shop, normalized, tokens);
            if (score < 0) continue;

            candidates.add(shop);
            scores.add(score);
        }

        if (candidates.isEmpty()) return CompletableFuture.completedFuture(ShopSearchResponse.empty());

        boolean hideUnavailable = ConfigKeys.SEARCH_HIDE_UNAVAILABLE.getBoolean(true);
        int maxResults = Math.max(1, ConfigKeys.SEARCH_MAX_RESULTS.getInt(200));

        var storageManager = plugin.getStorageManager();

        List<CompletableFuture<ShopSearchResult>> futures = new ArrayList<>(candidates.size());

        for (int index = 0; index < candidates.size(); index++) {
            PlayerShopData shop = candidates.get(index);
            int score = scores.get(index);

            futures.add(storageManager.getOrLoad(shop.getShopId()).thenApply(storage -> toResult(shop, storage, score)));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(ignored -> {
                    List<ShopSearchResult> results = new ArrayList<>(futures.size());
                    int hidden = 0;

                    for (CompletableFuture<ShopSearchResult> future : futures) {
                        ShopSearchResult result = future.join();

                        if (hideUnavailable && !result.isAvailable()) {
                            hidden++;
                            continue;
                        }

                        results.add(result);
                    }

                    sort(results, sort, origin);

                    if (results.size() > maxResults) results = new ArrayList<>(results.subList(0, maxResults));

                    return new ShopSearchResponse(results, hidden);
                });
    }

    @NotNull
    private ShopSearchResult toResult(@NotNull PlayerShopData shop, @NotNull PlayerShopStorage storage, int score) {
        boolean selling = shop.getMode() == ShopMode.SELL;

        int stock = selling ? StorageUtil.count(storage, shop.getItemStack()) : 0;
        int space = selling ? 0 : StorageUtil.getRemainingCapacity(storage);

        return new ShopSearchResult(shop, stock, space, score);
    }

    public void sort(@NotNull List<ShopSearchResult> results, @NotNull SearchSort sort, @Nullable Location origin) {
        Comparator<ShopSearchResult> comparator = switch (sort) {
            case PRICE_LOW -> Comparator.comparingDouble(result -> result.shop().getPrice());
            case PRICE_HIGH -> Comparator.<ShopSearchResult>comparingDouble(result -> result.shop().getPrice()).reversed();
            case STOCK -> Comparator.<ShopSearchResult>comparingInt(result -> result.shop().getMode() == ShopMode.SELL ? result.stock() : result.space()).reversed();
            case DISTANCE -> Comparator.comparingDouble(result -> distance(origin, result.shop()));
        };

        Comparator<ShopSearchResult> byScore = Comparator.comparingInt(ShopSearchResult::score);

        results.sort(comparator.thenComparing(byScore.reversed()));
    }

    public double distance(@Nullable Location origin, @NotNull PlayerShopData shop) {
        if (origin == null || origin.getWorld() == null) return Double.MAX_VALUE;
        if (!origin.getWorld().getName().equals(shop.getWorld())) return Double.MAX_VALUE;

        double dx = origin.getX() - shop.getX();
        double dy = origin.getY() - shop.getY();
        double dz = origin.getZ() - shop.getZ();

        return dx * dx + dy * dy + dz * dz;
    }

    private int score(@NotNull PlayerShopData shop, @NotNull String query, @NotNull String[] tokens) {
        if (tokens.length == 0) return 0;

        String itemText = shop.getSearchText();

        if (itemText.equals(query)) return 100;
        if (itemText.startsWith(query)) return 80;
        if (itemText.contains(query)) return 60;

        if (containsAll(itemText, tokens)) return 40;

        String owner = shop.getOwnerName().toLowerCase(Locale.ROOT);

        return containsAll(owner, tokens) ? 20 : -1;
    }

    private boolean containsAll(@NotNull String text, @NotNull String[] tokens) {
        for (String token : tokens) {
            if (!text.contains(token)) return false;
        }

        return true;
    }


    @NotNull
    private List<String> blacklistedWorlds() {
        List<String> configured = ConfigKeys.TELEPORT_BLACKLISTED_WORLDS.getList(List.of());
        if (configured == null || configured.isEmpty()) return List.of();

        return configured.stream()
                .filter(world -> world != null && !world.isBlank())
                .map(world -> world.toLowerCase(Locale.ROOT))
                .toList();
    }

    @NotNull
    public SearchFilter defaultFilter() {
        return SearchFilter.parse(ConfigKeys.SEARCH_DEFAULT_FILTER.getRawString("SELL"), SearchFilter.SELL);
    }

    @NotNull
    public SearchSort defaultSort() {
        return SearchSort.parse(ConfigKeys.SEARCH_DEFAULT_SORT.getRawString("PRICE_LOW"), SearchSort.PRICE_LOW);
    }

    public int minQueryLength() {
        return Math.max(0, ConfigKeys.SEARCH_MIN_QUERY_LENGTH.getInt(2));
    }
}