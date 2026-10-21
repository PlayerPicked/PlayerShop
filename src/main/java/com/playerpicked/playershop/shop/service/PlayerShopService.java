package com.playerpicked.playershop.shop.service;

import com.playerpicked.playershop.PlayerShop;
import com.playerpicked.playershop.database.DatabaseManager;
import com.playerpicked.playershop.guis.ShopBoundMenu;
import com.playerpicked.playershop.hooks.impl.currency.Currency;
import com.playerpicked.playershop.identifiers.ShopTransactionResult;
import com.playerpicked.playershop.identifiers.keys.ConfigKeys;
import com.playerpicked.playershop.shop.manager.PlayerShopManager;
import com.playerpicked.playershop.shop.models.PlayerShopData;
import com.playerpicked.playershop.identifiers.ShopMode;
import com.playerpicked.playershop.shop.manager.PlayerShopStorageManager;
import com.playerpicked.playershop.shop.models.PlayerShopStorage;
import com.playerpicked.playershop.utils.ItemUtil;
import com.playerpicked.playershop.utils.ShopBlockUtil;
import com.playerpicked.playershop.utils.StorageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Barrel;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PlayerShopService {

    public static final String BYPASS_PERMISSION = "playershop.admin.bypass";

    private final PlayerShop plugin = PlayerShop.getInstance();
    private final PlayerShopManager manager = plugin.getShopManager();
    private final ShopVisualService visuals = plugin.getVisualService();
    private final PlayerShopStorageManager storageManager = plugin.getStorageManager();
    private final ShopOwnerNotifyService ownerNotify = new ShopOwnerNotifyService();

    private final java.util.Set<UUID> creationModePlayers = new java.util.concurrent.ConcurrentHashMap<>().newKeySet();
    public static final org.bukkit.NamespacedKey HINTS_DISABLED = new org.bukkit.NamespacedKey(PlayerShop.getInstance(), "hints_disabled");

    public boolean toggleCreationMode(@NotNull UUID uuid) {
        if (creationModePlayers.contains(uuid)) {
            creationModePlayers.remove(uuid);
            return false;
        } else {
            creationModePlayers.add(uuid);
            return true;
        }
    }

    public boolean hasCreationMode(@NotNull UUID uuid) {
        return creationModePlayers.contains(uuid);
    }

    public void removeCreationMode(@NotNull UUID uuid) {
        creationModePlayers.remove(uuid);
    }

    public void create(@NotNull Player player, @NotNull Location location, @NotNull ItemStack item) {
        ItemStack base = item.clone();
        base.setAmount(1);

        String defaultCurrency = plugin.getHooks().getString("hooks.currency.default", "vault");

        PlayerShopData shop = new PlayerShopData(
                UUID.randomUUID(),
                player.getUniqueId(),
                location,
                ItemUtil.serialize(base),
                0,
                ShopMode.SELL,
                false,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                defaultCurrency,
                ConfigKeys.SEARCH_LISTED_BY_DEFAULT.getBoolean(true),
                null
        );

        manager.register(shop);
        DatabaseManager.getDatabase().insertShop(shop);

        setupBlock(location);
        visuals.spawn(shop);
    }


    public void setupBlock(@NotNull Location location) {
        Block block = location.getBlock();

        if (!(block.getBlockData() instanceof Barrel barrel)) return;

        barrel.setFacing(BlockFace.UP);
        barrel.setOpen(true);

        block.setBlockData(barrel, true);
    }

    public boolean canManage(@NotNull Player player, @NotNull PlayerShopData shop) {
        return shop.getOwnerUuid().equals(player.getUniqueId()) || player.hasPermission(BYPASS_PERMISSION);
    }

    public void remove(@NotNull Player player, @NotNull PlayerShopData shop) {
        if (!canManage(player, shop)) return;

        UUID shopId = shop.getShopId();

        Material blockMaterial = ShopBlockUtil.getDropMaterial(shop.getLocation().getBlock());

        manager.unregister(shop.getLocation());
        visuals.remove(shopId);
        closeMenus(shopId);

        storageManager.getOrLoad(shopId).thenAccept(storage -> {
            if (Bukkit.isPrimaryThread()) {
                finishRemoval(player, shopId, storage, blockMaterial);
                return;
            }

            PlayerShop.getScheduler().runTask(() -> finishRemoval(player, shopId, storage, blockMaterial));
        });
    }

    private void finishRemoval(@NotNull Player player, @NotNull UUID shopId, @NotNull PlayerShopStorage storage, @NotNull Material blockMaterial) {
        closeMenus(shopId);

        List<ItemStack> contents = new ArrayList<>(storage.getContents());
        storage.getContents().clear();

        storageManager.remove(shopId);
        DatabaseManager.getDatabase().deleteShop(shopId);

        player.getInventory().addItem(new ItemStack(blockMaterial));

        for (ItemStack item : contents) {
            if (item != null) player.getInventory().addItem(item);
        }
    }

    private void closeMenus(@NotNull UUID shopId) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!(online.getOpenInventory().getTopInventory().getHolder() instanceof ShopBoundMenu menu)) continue;
            if (!menu.getShop().getShopId().equals(shopId)) continue;

            online.closeInventory();
        }
    }

    public void update(@NotNull PlayerShopData shop) {
        shop.setUpdatedAt(System.currentTimeMillis());
        DatabaseManager.getDatabase().updateShop(shop);
        plugin.getVisualService().update(shop);
    }

    public @NotNull ShopTransactionResult buy(@NotNull PlayerShopData shop, Player buyer, int amount) {
        PlayerShopStorage storage = storageManager.getOrLoadSync(shop.getShopId());

        ItemStack shopItem = shop.getItemStack();
        int available = StorageUtil.count(storage, shopItem);

        if (available < amount) return ShopTransactionResult.SHOP_EMPTY;

        Currency currency = plugin.getCurrencyManager().get(shop.getCurrencyId());
        if (currency == null) return ShopTransactionResult.CURRENCY_ERROR;

        double total = shop.getPrice() * amount;

        if (!currency.has(buyer, total)) return ShopTransactionResult.NOT_ENOUGH_MONEY;

        ItemStack give = shopItem.clone();
        give.setAmount(amount);

        if (!currency.withdraw(buyer, total)) {
            return ShopTransactionResult.NOT_ENOUGH_MONEY;
        }

        var leftover = buyer.getInventory().addItem(give);
        if (!leftover.isEmpty()) {
            int notAdded = leftover.values().stream().mapToInt(ItemStack::getAmount).sum();
            int added = amount - notAdded;

            if (added > 0) {
                ItemStack addedStack = shopItem.clone();
                addedStack.setAmount(added);
                buyer.getInventory().removeItem(addedStack);
            }

            currency.deposit(buyer, total);
            return ShopTransactionResult.INVENTORY_FULL;
        }

        currency.deposit(Bukkit.getOfflinePlayer(shop.getOwnerUuid()), total);

        StorageUtil.remove(storage, shopItem, amount);
        storageManager.saveAsync(storage);

        visuals.update(shop);

        DatabaseManager.getDatabase().insertTransaction(
                shop.getShopId(),
                buyer.getUniqueId(),
                "BUY",
                amount,
                total
        );

        ownerNotify.notifyBuy(shop, buyer, amount, total);

        return ShopTransactionResult.SUCCESS;
    }

    public @NotNull ShopTransactionResult sell(@NotNull PlayerShopData shop, Player player, int amount) {
        PlayerShopStorage storage = storageManager.getOrLoadSync(shop.getShopId());

        int capacity = StorageUtil.getRemainingCapacity(storage);
        if (capacity < amount) return ShopTransactionResult.STORAGE_FULL;

        Currency currency = plugin.getCurrencyManager().get(shop.getCurrencyId());
        if (currency == null) return ShopTransactionResult.CURRENCY_ERROR;

        double total = shop.getPrice() * amount;

        OfflinePlayer owner = Bukkit.getOfflinePlayer(shop.getOwnerUuid());
        if (!currency.has(owner, total)) return ShopTransactionResult.OWNER_NO_MONEY;

        ItemStack base = shop.getItemStack();
        if (!player.getInventory().containsAtLeast(base, amount)) return ShopTransactionResult.NOT_ENOUGH_ITEMS;

        ItemStack toStore = base.clone();
        toStore.setAmount(amount);

        if (!StorageUtil.add(storage, toStore)) return ShopTransactionResult.STORAGE_FULL;

        if (!removeExact(player, base, amount)) {
            StorageUtil.remove(storage, base, amount);
            return ShopTransactionResult.NOT_ENOUGH_ITEMS;
        }

        if (!currency.withdraw(owner, total)) {
            StorageUtil.remove(storage, base, amount);
            player.getInventory().addItem(toStore);
            return ShopTransactionResult.OWNER_NO_MONEY;
        }

        currency.deposit(player, total);

        storageManager.saveAsync(storage);

        visuals.update(shop);

        DatabaseManager.getDatabase().insertTransaction(
                shop.getShopId(),
                player.getUniqueId(),
                "SELL",
                amount,
                total
        );

        ownerNotify.notifySell(shop, player, amount, total);

        return ShopTransactionResult.SUCCESS;
    }

    private boolean removeExact(@NotNull Player player, ItemStack base, int amount) {
        int remaining = amount;

        for (ItemStack content : player.getInventory().getContents()) {
            if (content == null) continue;

            if (!content.isSimilar(base)) continue;

            int take = Math.min(remaining, content.getAmount());

            content.setAmount(content.getAmount() - take);
            remaining -= take;

            if (remaining <= 0) break;
        }

        return remaining <= 0;
    }
}
