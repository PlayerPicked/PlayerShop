package com.playerpicked.playershop.guis.impl;

import com.playerpicked.playershop.PlayerShop;
import com.playerpicked.playershop.data.MenuController;
import com.playerpicked.playershop.guis.ShopBoundMenu;
import com.playerpicked.playershop.guis.Menu;
import com.playerpicked.playershop.identifiers.ShopMode;
import com.playerpicked.playershop.identifiers.ShopTransactionResult;
import com.playerpicked.playershop.identifiers.keys.ItemKeys;
import com.playerpicked.playershop.identifiers.keys.MenuKeys;
import com.playerpicked.playershop.identifiers.keys.MessageKeys;
import com.playerpicked.playershop.item.ItemFactory;
import com.playerpicked.playershop.processor.MessageProcessor;
import com.playerpicked.playershop.shop.models.PlayerShopData;
import com.playerpicked.playershop.shop.models.PlayerShopStorage;
import com.playerpicked.playershop.utils.AmountFormatUtil;
import com.playerpicked.playershop.utils.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("deprecation")
public final class ShopTradeMenu extends Menu implements ShopBoundMenu {

    private final PlayerShopData shop;
    private final Map<Integer, ItemKeys> slotMap = new ConcurrentHashMap<>();

    private PlayerShopStorage storage;

    private int amount = 1;

    public ShopTradeMenu(@NotNull MenuController controller, @NotNull PlayerShopData shop) {
        super(controller);
        this.shop = shop;
    }

    @Override
    public void open() {
        PlayerShop.getInstance().getStorageManager()
                .getOrLoad(shop.getShopId())
                .thenAccept(storage -> {
                    this.storage = storage;

                    PlayerShop.getScheduler().runTask(() -> {
                        super.open();
                        SoundUtil.play(menuController.owner(), MenuKeys.SHOP_TRADE_SOUND_OPEN.getString());
                    });
                });
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
                case SHOP_TRADE_ADD_1 -> modify(+getStep("add-1", 1));
                case SHOP_TRADE_ADD_10 -> modify(+getStep("add-10", 10));
                case SHOP_TRADE_ADD_64 -> modify(+getStep("add-64", 64));

                case SHOP_TRADE_REMOVE_1 -> modify(-getStep("remove-1", 1));
                case SHOP_TRADE_REMOVE_10 -> modify(-getStep("remove-10", 10));
                case SHOP_TRADE_REMOVE_64 -> modify(-getStep("remove-64", 64));

                case SHOP_TRADE_CONFIRM -> confirm();

                case SHOP_TRADE_CLOSE -> {
                    SoundUtil.play(menuController.owner(), MenuKeys.SHOP_TRADE_SOUND_ACTION.getString());
                    menuController.owner().closeInventory();
                }

                default -> {}
            }

            return;
        }

        event.setCancelled(true);
    }

    private void modify(int delta) {
        int max = getMaxAmount();
        int min = 1;

        amount += delta;

        if (amount < min) amount = min;
        if (amount > max) amount = max;

        SoundUtil.play(menuController.owner(), MenuKeys.SHOP_TRADE_SOUND_MODIFY.getString());
        updateMenuItems();
    }

    private void confirm() {
        Player player = menuController.owner();

        int max = getMaxAmount();
        if (amount > max) {
            SoundUtil.play(player, MenuKeys.SHOP_TRADE_SOUND_ERROR.getString());
            player.sendMessage(MessageKeys.SHOP_TRADE_INVALID_AMOUNT.getMessage());
            return;
        }

        ShopTransactionResult result;
        if (shop.getMode() == ShopMode.SELL) {
            result = PlayerShop.getInstance().getShopService().buy(shop, player, amount);
        } else {
            result = PlayerShop.getInstance().getShopService().sell(shop, player, amount);
        }

        handleResult(player, result);
    }

    private void handleResult(@NotNull Player player, @NotNull ShopTransactionResult result) {
        switch (result) {

            case SUCCESS -> {
                SoundUtil.play(player, MenuKeys.SHOP_TRADE_SOUND_ACTION.getString());
                player.sendMessage(formatSuccess());
                player.closeInventory();
            }

            case NOT_ENOUGH_MONEY -> {
                SoundUtil.play(player, MenuKeys.SHOP_TRADE_SOUND_ERROR.getString());
                player.sendMessage(MessageKeys.SHOP_ERROR_NOT_ENOUGH_MONEY.getMessage());
            }

            case NOT_ENOUGH_ITEMS -> {
                SoundUtil.play(player, MenuKeys.SHOP_TRADE_SOUND_ERROR.getString());
                player.sendMessage(MessageKeys.SHOP_ERROR_NOT_ENOUGH_ITEMS.getMessage());
            }

            case INVENTORY_FULL -> {
                SoundUtil.play(player, MenuKeys.SHOP_TRADE_SOUND_ERROR.getString());
                player.sendMessage(MessageKeys.SHOP_ERROR_INVENTORY_FULL.getMessage());
            }

            case STORAGE_FULL -> {
                SoundUtil.play(player, MenuKeys.SHOP_TRADE_SOUND_ERROR.getString());
                player.sendMessage(MessageKeys.SHOP_ERROR_STORAGE_FULL.getMessage());
            }

            case OWNER_NO_MONEY -> {
                SoundUtil.play(player, MenuKeys.SHOP_TRADE_SOUND_ERROR.getString());
                player.sendMessage(MessageKeys.SHOP_ERROR_OWNER_NO_MONEY.getMessage());
            }

            case SHOP_EMPTY -> {
                SoundUtil.play(player, MenuKeys.SHOP_TRADE_SOUND_ERROR.getString());
                player.sendMessage(MessageKeys.SHOP_ERROR_SHOP_EMPTY.getMessage());
            }

            default -> {
                SoundUtil.play(player, MenuKeys.SHOP_TRADE_SOUND_ERROR.getString());
                player.sendMessage(MessageKeys.SHOP_TRANSACTION_FAILED.getMessage());
            }
        }
    }

    private int getMaxAmount() {
        if (storage == null) return 1;

        int limit;
        if (shop.getMode() == ShopMode.SELL) {
            limit = countStorageItems();
        } else {
            limit = countPlayerItems(menuController.owner(), shop.getItemStack());
        }

        int configCap = MenuKeys.SHOP_TRADE_MAX_AMOUNT.getInt();
        if (configCap > 0) limit = Math.min(limit, configCap);

        return Math.max(limit, 1);
    }

    private int countStorageItems() {
        int total = 0;

        for (ItemStack item : storage.getContents()) {
            if (item == null) continue;
            if (!item.isSimilar(shop.getItemStack())) continue;

            total += item.getAmount();
        }

        return total;
    }

    private int countPlayerItems(@NotNull Player player, @NotNull ItemStack base) {
        int total = 0;

        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;
            if (!item.isSimilar(base)) continue;

            total += item.getAmount();
        }

        return total;
    }

    private String resolveItemName() {
        ItemStack item = shop.getItemStack();
        ItemMeta meta = item.getItemMeta();

        if (meta != null && !meta.getItemName().isEmpty()) {
            return meta.getItemName();
        } else {
            if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                return item.getItemMeta().getDisplayName();
            }

            String raw = item.getType().name().toLowerCase().replace("_", " ");
            String[] parts = raw.split(" ");

            StringBuilder builder = new StringBuilder();

            for (String part : parts) {
                builder.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1))
                        .append(" ");
            }

            return builder.toString().trim();
        }
    }

    private int getStep(@NotNull String key, int def) {
        return MenuKeys.SHOP_TRADE_STEPS.getSection().getInt(key, def);
    }

    @Override
    public void setMenuItems() {
        inventory.clear();
        slotMap.clear();

        ItemFactory.setItemsForMenu("shop-trade.items", inventory);

        int max = getMaxAmount();

        if (amount > max) amount = max;

        String itemName = resolveItemName();
        String currency = MessageProcessor.process(getCurrencyValue(shop));

        Map<String, String> replacements = Map.of(
                "{amount}", String.valueOf(amount),
                "{max}", String.valueOf(max),
                "{price_total}", AmountFormatUtil.format(amount * shop.getPrice()),
                "{price_each}", AmountFormatUtil.format(shop.getPrice()),
                "{mode}", resolveMode(),
                "{name}", itemName,
                "{currency}", currency
        );

        for (ItemKeys key : ItemKeys.values()) {
            if (!key.name().startsWith("SHOP_TRADE")) continue;

            for (int slot : key.getSlots()) {
                if (slot < 0 || slot >= inventory.getSize()) continue;

                ItemStack base = key.getItem();
                if (base == null) continue;

                ItemStack item;

                if (key == ItemKeys.SHOP_TRADE_CONFIRM) {
                    item = shop.getItemStack().clone();

                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        ItemMeta baseMeta = base.getItemMeta();

                        if (baseMeta != null) {
                            meta.setDisplayName(replace(baseMeta.getDisplayName(), replacements));

                            if (baseMeta.getLore() != null) {
                                meta.setLore(baseMeta.getLore().stream()
                                        .map(line -> replace(line, replacements))
                                        .toList());
                            }
                        }

                        item.setItemMeta(meta);
                    }
                } else {
                    item = base.clone();
                    item.editMeta(meta -> apply(meta, replacements));
                }

                inventory.setItem(slot, item);
                slotMap.put(slot, key);
            }
        }
    }

    private void apply(@NotNull ItemMeta meta, @NotNull Map<String, String> replacements) {
        String name = meta.getDisplayName();
        meta.setDisplayName(replace(name, replacements));

        if (meta.getLore() == null) return;

        meta.setLore(meta.getLore().stream()
                .map(line -> replace(line, replacements))
                .toList());
    }

    @NotNull
    private String replace(String input, @NotNull Map<String, String> replacements) {
        String out = input;

        for (var entry : replacements.entrySet()) {
            out = out.replace(entry.getKey(), entry.getValue());
        }

        return MessageProcessor.process(out);
    }

    @NotNull
    private String formatSuccess() {
        String owner = Bukkit.getOfflinePlayer(shop.getOwnerUuid()).getName();

        return MessageKeys.SHOP_TRANSACTION_SUCCESS.getMessage()
                .replace("{amount}", String.valueOf(amount))
                .replace("{item}", resolveItemName())
                .replace("{price}", AmountFormatUtil.format(amount * shop.getPrice()))
                .replace("{currency}", MessageProcessor.process(getCurrencyValue(shop)))
                .replace("{owner}", owner != null ? owner : "Unknown")
                .replace("{mode}", resolveMode());
    }

    @NotNull
    private String resolveMode() {
        return shop.getMode() == ShopMode.SELL
                ? MessageKeys.SHOP_MODE_BUY.getMessage()
                : MessageKeys.SHOP_MODE_SELL.getMessage();
    }

    @NotNull
    private String getCurrencyValue(@NotNull PlayerShopData shop) {
        String base = "hooks.currency.currencies." + shop.getCurrencyId();
        String value = PlayerShop.getInstance().getHooks().getString(base + "." + "prefix");

        return value != null ? value : "";
    }

    @Override
    public @NotNull String getMenuName() {
        return MenuKeys.SHOP_TRADE_TITLE.getString();
    }

    @Override
    public int getSlots() {
        return MenuKeys.SHOP_TRADE_SIZE.getInt();
    }

    @Override
    public int getMenuTick() {
        return MenuKeys.SHOP_TRADE_TICK.getInt();
    }

    @Override
    public @NotNull PlayerShopData getShop() {
        return shop;
    }

    @Override
    public boolean isStale() {
        return ShopBoundMenu.super.isStale() || storage != PlayerShop.getInstance().getStorageManager().getCached(shop.getShopId());
    }
}