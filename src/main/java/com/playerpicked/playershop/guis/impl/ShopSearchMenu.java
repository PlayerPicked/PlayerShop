package com.playerpicked.playershop.guis.impl;

import com.playerpicked.playershop.PlayerShop;
import com.playerpicked.playershop.data.MenuController;
import com.playerpicked.playershop.guis.Menu;
import com.playerpicked.playershop.identifiers.SearchFilter;
import com.playerpicked.playershop.identifiers.SearchSort;
import com.playerpicked.playershop.identifiers.ShopMode;
import com.playerpicked.playershop.identifiers.keys.ItemKeys;
import com.playerpicked.playershop.identifiers.keys.MenuKeys;
import com.playerpicked.playershop.identifiers.keys.MessageKeys;
import com.playerpicked.playershop.item.ItemFactory;
import com.playerpicked.playershop.processor.MessageProcessor;
import com.playerpicked.playershop.shop.models.PlayerShopData;
import com.playerpicked.playershop.shop.models.ShopSearchResult;
import com.playerpicked.playershop.utils.AmountFormatUtil;
import com.playerpicked.playershop.utils.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("deprecation")
public final class ShopSearchMenu extends Menu {

    private final PlayerShop plugin = PlayerShop.getInstance();

    private final String query;

    private List<ShopSearchResult> results;
    private SearchFilter filter;
    private SearchSort sort;

    private int page = 0;
    private boolean loading = false;

    private List<Integer> contentSlots = List.of();

    public ShopSearchMenu(@NotNull MenuController controller, @Nullable String query, @NotNull List<ShopSearchResult> results, @NotNull SearchFilter filter, @NotNull SearchSort sort) {
        super(controller);

        this.query = query == null ? "" : query;
        this.results = new ArrayList<>(results);
        this.filter = filter;
        this.sort = sort;
    }

    @Override
    public void open() {
        contentSlots = MenuKeys.SHOP_SEARCH_SLOTS.getIntList();
        if (contentSlots.isEmpty()) contentSlots = defaultSlots();

        super.open();
        SoundUtil.play(menuController.owner(), MenuKeys.SHOP_SEARCH_SOUND_OPEN.getString());
    }

    @Override
    public void handleMenu(@NotNull InventoryClickEvent event) {
        event.setCancelled(true);

        int raw = event.getRawSlot();
        if (inventory == null || raw < 0 || raw >= inventory.getSize()) return;

        Player player = menuController.owner();

        if (ItemKeys.SHOP_SEARCH_CLOSE.getSlots().contains(raw)) {
            SoundUtil.play(player, MenuKeys.SHOP_SEARCH_SOUND_ACTION.getString());
            player.closeInventory();
            return;
        }

        if (ItemKeys.SHOP_SEARCH_NEXT.getSlots().contains(raw)) {
            flipPage(1);
            return;
        }

        if (ItemKeys.SHOP_SEARCH_PREVIOUS.getSlots().contains(raw)) {
            flipPage(-1);
            return;
        }

        if (ItemKeys.SHOP_SEARCH_FILTER.getSlots().contains(raw)) {
            cycleFilter();
            return;
        }

        if (ItemKeys.SHOP_SEARCH_SORT.getSlots().contains(raw)) {
            cycleSort();
            return;
        }

        int index = contentSlots.indexOf(raw);
        if (index < 0) return;

        int resultIndex = page * contentSlots.size() + index;
        if (resultIndex >= results.size()) return;

        PlayerShopData shop = results.get(resultIndex).shop();

        SoundUtil.play(player, MenuKeys.SHOP_SEARCH_SOUND_ACTION.getString());

        player.closeInventory();
        plugin.getTeleportService().request(player, shop);
    }

    private void flipPage(int direction) {
        int target = page + direction;

        if (target < 0 || target > getMaxPage()) {
            SoundUtil.play(menuController.owner(), MenuKeys.SHOP_SEARCH_SOUND_ERROR.getString());
            return;
        }

        page = target;

        SoundUtil.play(menuController.owner(), MenuKeys.SHOP_SEARCH_SOUND_PAGE.getString());
        setMenuItems();
    }

    private void cycleSort() {
        sort = sort.next();
        page = 0;

        plugin.getSearchService().sort(results, sort, menuController.owner().getLocation());

        SoundUtil.play(menuController.owner(), MenuKeys.SHOP_SEARCH_SOUND_ACTION.getString());
        setMenuItems();
    }

    private void cycleFilter() {
        if (loading) return;

        loading = true;
        filter = filter.next();

        SoundUtil.play(menuController.owner(), MenuKeys.SHOP_SEARCH_SOUND_ACTION.getString());

        Player player = menuController.owner();
        UUID uuid = player.getUniqueId();

        plugin.getSearchService()
                .search(player, query, filter, sort)
                .thenAccept(response -> {
                    Player online = Bukkit.getPlayer(uuid);

                    if (online == null || !online.isOnline()) {
                        loading = false;
                        return;
                    }

                    PlayerShop.getScheduler().runTask(online, () -> {
                        loading = false;

                        if (inventory == null) return;

                        results = new ArrayList<>(response.results());
                        page = 0;

                        setMenuItems();
                        online.updateInventory();
                    });
                })
                .exceptionally(throwable -> {
                    loading = false;
                    return null;
                });
    }

    @Override
    public void setMenuItems() {
        if (inventory == null) return;

        inventory.clear();

        ItemFactory.setItemsForMenu("shop-search.items", inventory);

        if (contentSlots.isEmpty()) contentSlots = defaultSlots();

        Map<String, String> controls = new HashMap<>();
        controls.put("{page}", String.valueOf(page + 1));
        controls.put("{max}", String.valueOf(getMaxPage() + 1));
        controls.put("{query}", displayQuery());
        controls.put("{results}", String.valueOf(results.size()));
        controls.put("{filter}", label("shop-search.filters." + filter.name(), filter.name()));
        controls.put("{sort}", label("shop-search.sorts." + sort.name(), sort.name()));

        applyToSlots(ItemKeys.SHOP_SEARCH_PAGE_INFO, controls);
        applyToSlots(ItemKeys.SHOP_SEARCH_FILTER, controls);
        applyToSlots(ItemKeys.SHOP_SEARCH_SORT, controls);

        if (results.isEmpty()) {
            applyToSlots(ItemKeys.SHOP_SEARCH_EMPTY, controls);
            return;
        }

        for (int slot : ItemKeys.SHOP_SEARCH_EMPTY.getSlots()) {
            inventory.setItem(slot, null);
        }

        page = Math.max(0, Math.min(page, getMaxPage()));

        int start = page * contentSlots.size();
        int end = Math.min(start + contentSlots.size(), results.size());

        RenderContext context = createContext();

        for (int index = start; index < end; index++) {
            int slot = contentSlots.get(index - start);
            if (slot < 0 || slot >= inventory.getSize()) continue;

            inventory.setItem(slot, buildResultItem(results.get(index), context));
        }
    }

    private record RenderContext(@NotNull String name, @NotNull List<String> lore,
                                 @NotNull String sellMode, @NotNull String buyMode,
                                 @NotNull String available, @NotNull String outOfStock,
                                 @NotNull String storageFull, @NotNull String islandLocked,
                                 @NotNull String otherWorld, @NotNull String unknownDistance,
                                 @NotNull Location origin) {}

    @NotNull
    private RenderContext createContext() {
        String name = "";
        List<String> lore = List.of();

        ItemStack template = ItemKeys.SHOP_SEARCH_RESULT.getItem();

        if (template != null && template.getType() != Material.AIR) {
            ItemMeta meta = template.getItemMeta();

            if (meta != null) {
                name = meta.getDisplayName();
                if (meta.getLore() != null) lore = meta.getLore();
            }
        }

        return new RenderContext(name, lore,
                MessageKeys.SHOP_MODE_SELL.getMessage(),
                MessageKeys.SHOP_MODE_BUY.getMessage(),
                state("available"),
                state("out-of-stock"),
                state("storage-full"),
                state("island-locked"),
                state("other-world"),
                MessageProcessor.process(plugin.getGuis().getString("shop-search.unknown-distance", "???")),
                menuController.owner().getLocation());
    }

    @NotNull
    private ItemStack buildResultItem(@NotNull ShopSearchResult result, @NotNull RenderContext context) {
        ItemStack icon = result.shop().getItemStack();
        icon.setAmount(1);

        Map<String, String> replacements = buildReplacements(result, context);

        icon.editMeta(meta -> {
            if (!context.name().isEmpty()) meta.setDisplayName(replace(context.name(), replacements));

            meta.setLore(context.lore().stream()
                    .map(line -> replace(line, replacements))
                    .toList());
        });

        return icon;
    }

    @NotNull
    private Map<String, String> buildReplacements(@NotNull ShopSearchResult result, @NotNull RenderContext context) {
        PlayerShopData shop = result.shop();
        boolean selling = shop.getMode() == ShopMode.SELL;

        Map<String, String> replacements = new HashMap<>();

        replacements.put("{item}", shop.getItemDisplayName());
        replacements.put("{owner}", shop.getOwnerName());
        replacements.put("{mode}", selling ? context.sellMode() : context.buyMode());

        replacements.put("{price}", AmountFormatUtil.format(shop.getPrice()));
        replacements.put("{currency}", MessageProcessor.process(currencyValue(shop, "display-name")));
        replacements.put("{currency_prefix}", MessageProcessor.process(currencyValue(shop, "prefix")));

        replacements.put("{stock}", AmountFormatUtil.format(selling ? result.stock() : result.space()));

        replacements.put("{world}", shop.getWorld());
        replacements.put("{x}", String.valueOf(shop.getX()));
        replacements.put("{y}", String.valueOf(shop.getY()));
        replacements.put("{z}", String.valueOf(shop.getZ()));

        replacements.put("{distance}", distanceText(shop, context));
        replacements.put("{status}", statusText(result, context));

        return replacements;
    }

    @NotNull
    private String statusText(@NotNull ShopSearchResult result, @NotNull RenderContext context) {
        PlayerShopData shop = result.shop();

        if (plugin.getIslandManager().isLocked(shop.getLocation())) return context.islandLocked();

        if (!result.isAvailable()) {
            return shop.getMode() == ShopMode.SELL ? context.outOfStock() : context.storageFull();
        }

        World world = context.origin().getWorld();

        if (world == null || !world.getName().equals(shop.getWorld())) return context.otherWorld();

        return context.available();
    }

    @NotNull
    private String distanceText(@NotNull PlayerShopData shop, @NotNull RenderContext context) {
        double squared = plugin.getSearchService().distance(context.origin(), shop);
        if (squared == Double.MAX_VALUE) return context.unknownDistance();

        return AmountFormatUtil.format(Math.round(Math.sqrt(squared))) + "m";
    }

    @NotNull
    private String state(@NotNull String key) {
        return MessageProcessor.process(plugin.getGuis().getString("shop-search.states." + key, ""));
    }

    @NotNull
    private String label(@NotNull String path, @NotNull String fallback) {
        return MessageProcessor.process(plugin.getGuis().getString(path, fallback));
    }

    @NotNull
    private String displayQuery() {
        if (!query.isBlank()) return query;

        return MessageProcessor.process(plugin.getGuis().getString("shop-search.browse-label", "Every shop"));
    }

    @NotNull
    private String currencyValue(@NotNull PlayerShopData shop, @NotNull String key) {
        String value = plugin.getHooks().getString("hooks.currency.currencies." + shop.getCurrencyId() + "." + key);
        return value == null ? "" : value;
    }

    private void applyToSlots(@NotNull ItemKeys key, @NotNull Map<String, String> replacements) {
        ItemStack base = key.getItem();
        if (base == null || base.getType() == Material.AIR) return;

        for (int slot : key.getSlots()) {
            if (slot < 0 || slot >= inventory.getSize()) continue;

            ItemStack clone = base.clone();
            clone.editMeta(meta -> apply(meta, replacements));

            inventory.setItem(slot, clone);
        }
    }

    private void apply(@NotNull ItemMeta meta, @NotNull Map<String, String> replacements) {
        String name = meta.getDisplayName();
        if (!name.isEmpty()) meta.setDisplayName(replace(name, replacements));

        List<String> lore = meta.getLore();
        if (lore == null || lore.isEmpty()) return;

        meta.setLore(lore.stream()
                .map(line -> replace(line, replacements))
                .toList());
    }

    @NotNull
    private String replace(@NotNull String input, @NotNull Map<String, String> replacements) {
        String out = input;

        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            out = out.replace(entry.getKey(), entry.getValue());
        }

        return out;
    }

    private int getMaxPage() {
        if (results.isEmpty() || contentSlots.isEmpty()) return 0;
        return (int) Math.ceil((double) results.size() / contentSlots.size()) - 1;
    }

    @NotNull
    private List<Integer> defaultSlots() {
        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < 45; slot++) slots.add(slot);

        return slots;
    }

    @Override
    public @NotNull String getMenuName() {
        return MenuKeys.SHOP_SEARCH_TITLE.getString().replace("{query}", displayQuery());
    }

    @Override
    public int getSlots() {
        int size = MenuKeys.SHOP_SEARCH_SIZE.getInt();
        return size <= 0 ? 54 : size;
    }

    @Override
    public int getMenuTick() {
        return MenuKeys.SHOP_SEARCH_TICK.getInt();
    }
}