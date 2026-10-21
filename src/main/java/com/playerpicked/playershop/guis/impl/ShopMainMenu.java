package com.playerpicked.playershop.guis.impl;

import com.playerpicked.playershop.PlayerShop;
import com.playerpicked.playershop.data.MenuController;
import com.playerpicked.playershop.guis.ShopBoundMenu;
import com.playerpicked.playershop.guis.Menu;
import com.playerpicked.playershop.identifiers.keys.ConfigKeys;
import com.playerpicked.playershop.identifiers.keys.ItemKeys;
import com.playerpicked.playershop.identifiers.keys.MenuKeys;
import com.playerpicked.playershop.identifiers.keys.MessageKeys;
import com.playerpicked.playershop.item.ItemFactory;
import com.playerpicked.playershop.processor.MessageProcessor;
import com.playerpicked.playershop.shop.models.PlayerShopData;
import com.playerpicked.playershop.identifiers.ShopMode;
import com.playerpicked.playershop.shop.models.PlayerShopStorage;
import com.playerpicked.playershop.utils.AmountFormatUtil;
import com.playerpicked.playershop.utils.SafeLocationUtil;
import com.playerpicked.playershop.utils.SoundUtil;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("deprecation")
public final class ShopMainMenu extends Menu implements ShopBoundMenu {

    private final PlayerShopData shop;
    private final Map<Integer, ItemKeys> slotMap = new ConcurrentHashMap<>();

    public ShopMainMenu(@NotNull MenuController controller, @NotNull PlayerShopData shop) {
        super(controller);
        this.shop = shop;
    }

    @Override
    public void open() {
        super.open();
        SoundUtil.play(menuController.owner(), MenuKeys.SHOP_MAIN_SOUND_OPEN.getString());
    }

    @Override
    public void handleMenu(@NotNull InventoryClickEvent event) {
        if (event.getClickedInventory() == null) return;

        int raw = event.getRawSlot();
        int top = inventory.getSize();

        if (raw < top) {
            event.setCancelled(true);

            ItemKeys key = slotMap.get(raw);
            if (key == null) return;

            switch (key) {
                case SHOP_MAIN_SELL_MODE, SHOP_MAIN_BUY_MODE -> toggleMode();

                case SHOP_MAIN_LISTED, SHOP_MAIN_UNLISTED -> toggleListing();
                case SHOP_MAIN_VISIT_POINT -> handleVisitPoint(event.isRightClick());

                case SHOP_MAIN_STORAGE -> {
                    SoundUtil.play(menuController.owner(), MenuKeys.SHOP_MAIN_SOUND_ACTION.getString());
                    new ShopStorageMenu(menuController, shop).open();
                }

                case SHOP_MAIN_CLOSE -> {
                    SoundUtil.play(menuController.owner(), MenuKeys.SHOP_MAIN_SOUND_ACTION.getString());
                    menuController.owner().closeInventory();
                }

                case SHOP_MAIN_TRANSACTIONS -> {
                    SoundUtil.play(menuController.owner(), MenuKeys.SHOP_MAIN_SOUND_ACTION.getString());
                    new ShopTransactionsMenu(menuController, shop).open();
                }

                case SHOP_MAIN_CURRENCY -> {
                    SoundUtil.play(menuController.owner(), MenuKeys.SHOP_MAIN_SOUND_ACTION.getString());
                    new ShopCurrencyMenu(menuController, shop).open();
                }

                case SHOP_MAIN_PRICE_CHANGE -> {
                    SoundUtil.play(menuController.owner(), MenuKeys.SHOP_MAIN_SOUND_ACTION.getString());
                    PlayerShop.getInstance().getShopPriceService().openEditor(menuController.owner(), shop);
                }

                default -> {}
            }

            return;
        }

        event.setCancelled(true);
    }

    private void toggleMode() {
        PlayerShop.getInstance().getStorageManager()
                .getOrLoad(shop.getShopId())
                .thenAccept(storage -> {
                    if (!isEmpty(storage)) {
                        PlayerShop.getScheduler().runTask(() -> {
                            SoundUtil.play(menuController.owner(), MenuKeys.SHOP_MAIN_SOUND_ERROR.getString());
                            menuController.owner().sendMessage(MessageKeys.SHOP_STORAGE_NOT_EMPTY.getMessage());
                        });
                        return;
                    }

                    PlayerShop.getScheduler().runTask(() -> {
                        shop.setMode(shop.getMode() == ShopMode.SELL ? ShopMode.BUY : ShopMode.SELL);
                        PlayerShop.getInstance().getShopService().update(shop);

                        SoundUtil.play(menuController.owner(), MenuKeys.SHOP_MAIN_SOUND_ACTION.getString());

                        updateMenuItems();
                    });
                });
    }

    private boolean isEmpty(@NotNull PlayerShopStorage storage) {
        for (ItemStack item : storage.getContents()) {
            if (item != null && !item.getType().isAir()) return false;
        }

        return true;
    }

    private void toggleListing() {
        shop.setListed(!shop.isListed());
        PlayerShop.getInstance().getShopService().update(shop);

        SoundUtil.play(menuController.owner(), MenuKeys.SHOP_MAIN_SOUND_ACTION.getString());

        menuController.owner().sendMessage(shop.isListed()
                ? MessageKeys.LISTING_ENABLED.getMessage()
                : MessageKeys.LISTING_DISABLED.getMessage());

        updateMenuItems();
    }

    private void handleVisitPoint(boolean reset) {
        Player player = menuController.owner();

        if (reset) {
            shop.setVisitLocation(null);
            PlayerShop.getInstance().getShopService().update(shop);

            SoundUtil.play(player, MenuKeys.SHOP_MAIN_SOUND_ACTION.getString());
            player.sendMessage(MessageKeys.VISIT_RESET.getMessage());

            updateMenuItems();
            return;
        }

        Location location = player.getLocation();

        if (location.getWorld() == null || !location.getWorld().getName().equals(shop.getWorld())) {
            reject(player, MessageKeys.VISIT_WRONG_WORLD.getMessage());
            return;
        }

        int maxDistance = Math.max(1, ConfigKeys.SEARCH_VISIT_POINT_MAX_DISTANCE.getInt(10));

        double dx = location.getX() - (shop.getX() + 0.5);
        double dy = location.getY() - (shop.getY() + 0.5);
        double dz = location.getZ() - (shop.getZ() + 0.5);

        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (distance > maxDistance) {
            reject(player, MessageKeys.VISIT_TOO_FAR.getMessage()
                    .replace("{distance}", String.valueOf(maxDistance))
                    .replace("{current}", String.valueOf(Math.round(distance))));
            return;
        }

        Optional<Location> safe = SafeLocationUtil.sanitize(location);

        if (safe.isEmpty()) {
            reject(player, MessageKeys.VISIT_UNSAFE.getMessage());
            return;
        }

        shop.setVisitLocation(safe.get());
        PlayerShop.getInstance().getShopService().update(shop);

        SoundUtil.play(player, MenuKeys.SHOP_MAIN_SOUND_ACTION.getString());

        player.sendMessage(MessageKeys.VISIT_SET.getMessage()
                .replace("{x}", String.valueOf(safe.get().getBlockX()))
                .replace("{y}", String.valueOf(safe.get().getBlockY()))
                .replace("{z}", String.valueOf(safe.get().getBlockZ())));

        updateMenuItems();
    }

    private void reject(@NotNull Player player, @NotNull String message) {
        SoundUtil.play(player, MenuKeys.SHOP_MAIN_SOUND_ERROR.getString());
        player.sendMessage(message);
    }

    @NotNull
    private String visitValue() {
        var guis = PlayerShop.getInstance().getGuis();

        if (!shop.hasVisitLocation()) {
            return MessageProcessor.process(guis.getString("shop-main.states.visit-automatic", "Automatic"));
        }

        Location visit = shop.getVisitLocation();

        return MessageProcessor.process(guis.getString("shop-main.states.visit-custom", "{x}, {y}, {z}"))
                .replace("{x}", String.valueOf(visit.getBlockX()))
                .replace("{y}", String.valueOf(visit.getBlockY()))
                .replace("{z}", String.valueOf(visit.getBlockZ()));
    }

    @Override
    public void setMenuItems() {
        inventory.clear();
        slotMap.clear();

        ItemFactory.setItemsForMenu("shop-main.items", inventory);

        Map<String, String> replacements = Map.of(
                "{mode}", resolveMode(shop),
                "{currency}", MessageProcessor.process(getCurrencyValue(shop)),
                "{price}", AmountFormatUtil.format(shop.getPrice()),
                "{visit}", visitValue()
        );

        setItem(ItemKeys.SHOP_MAIN_CURRENCY, replacements);
        setItem(ItemKeys.SHOP_MAIN_STORAGE, replacements);
        setItem(ItemKeys.SHOP_MAIN_TRANSACTIONS, replacements);
        setItem(ItemKeys.SHOP_MAIN_PRICE_CHANGE, replacements);
        setItem(ItemKeys.SHOP_MAIN_VISIT_POINT, replacements);
        setItem(ItemKeys.SHOP_MAIN_CLOSE, replacements);

        setModeItem(replacements);
        setListingItem(replacements);
    }

    private void setModeItem(@NotNull Map<String, String> replacements) {
        if (shop.getMode() == ShopMode.SELL) {
            setSingleItem("shop-main.sell-mode", ItemKeys.SHOP_MAIN_SELL_MODE, replacements);
        } else {
            setSingleItem("shop-main.buy-mode", ItemKeys.SHOP_MAIN_BUY_MODE, replacements);
        }
    }

    private void setListingItem(@NotNull Map<String, String> replacements) {
        if (shop.isListed()) {
            setSingleItem("shop-main.listed", ItemKeys.SHOP_MAIN_LISTED, replacements);
        } else {
            setSingleItem("shop-main.unlisted", ItemKeys.SHOP_MAIN_UNLISTED, replacements);
        }
    }

    private void setSingleItem(@NotNull String path, @NotNull ItemKeys key, @NotNull Map<String, String> replacements) {
        Section section = PlayerShop.getInstance().getGuis().getSection(path);
        if (section == null) return;

        Optional<ItemStack> item = ItemFactory.buildItem(section, path);

        if (item.isEmpty() || item.get().getType() == Material.AIR) return;

        ItemStack clone = item.get().clone();
        clone.editMeta(meta -> apply(meta, replacements));

        for (int slot : PlayerShop.getInstance().getGuis().getIntList(path + ".slot")) {
            if (slot < 0 || slot >= inventory.getSize()) continue;

            inventory.setItem(slot, clone);
            slotMap.put(slot, key);
        }
    }

    @NotNull
    private String getCurrencyValue(@NotNull PlayerShopData shop) {
        String base = "hooks.currency.currencies." + shop.getCurrencyId();
        String value = PlayerShop.getInstance().getHooks().getString(base + "." + "display-name");

        return value != null ? value : "";
    }

    @NotNull
    private String resolveMode(@NotNull PlayerShopData shop) {
        return shop.getMode() == ShopMode.SELL
                ? MessageKeys.SHOP_MODE_SELL.getMessage()
                : MessageKeys.SHOP_MODE_BUY.getMessage();
    }

    private void setItem(@NotNull ItemKeys key, @NotNull Map<String, String> replacements) {
        for (int slot : key.getSlots()) {
            if (slot < 0 || slot >= inventory.getSize()) continue;

            ItemStack item = key.getItem();
            if (item == null || item.getType() == Material.AIR) continue;

            ItemStack clone = item.clone();
            clone.editMeta(meta -> apply(meta, replacements));

            inventory.setItem(slot, clone);
            slotMap.put(slot, key);
        }
    }

    private void apply(@NotNull org.bukkit.inventory.meta.ItemMeta meta, Map<String, String> replacements) {
        String name = meta.getDisplayName();
        meta.setDisplayName(replace(name, replacements));

        if (meta.getLore() == null) return;

        meta.setLore(meta.getLore().stream()
                .map(line -> replace(line, replacements))
                .toList());
    }

    private String replace(String input, @NotNull Map<String, String> replacements) {
        String out = input;
        for (var e : replacements.entrySet()) out = out.replace(e.getKey(), e.getValue());

        return out;
    }

    @Override
    public @NotNull String getMenuName() {
        return MenuKeys.SHOP_MAIN_TITLE.getString();
    }

    @Override
    public int getSlots() {
        return MenuKeys.SHOP_MAIN_SIZE.getInt();
    }

    @Override
    public int getMenuTick() {
        return MenuKeys.SHOP_MAIN_TICK.getInt();
    }

    @Override
    public @NotNull PlayerShopData getShop() {
        return shop;
    }
}