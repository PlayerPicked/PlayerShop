package com.playerpicked.playershop.guis.impl;

import com.playerpicked.playershop.PlayerShop;
import com.playerpicked.playershop.data.MenuController;
import com.playerpicked.playershop.guis.ShopBoundMenu;
import com.playerpicked.playershop.guis.PaginatedMenu;
import com.playerpicked.playershop.identifiers.keys.ItemKeys;
import com.playerpicked.playershop.identifiers.keys.MenuKeys;
import com.playerpicked.playershop.item.ItemFactory;
import com.playerpicked.playershop.shop.models.PlayerShopData;
import com.playerpicked.playershop.identifiers.ShopMode;
import com.playerpicked.playershop.shop.models.PlayerShopStorage;
import com.playerpicked.playershop.utils.SoundUtil;
import com.playerpicked.playershop.utils.StorageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class ShopStorageMenu extends PaginatedMenu implements ShopBoundMenu {

    private final PlayerShopData shop;
    private PlayerShopStorage storage;

    private int pageSize;
    private List<Integer> contentSlots = List.of();

    public ShopStorageMenu(@NotNull MenuController controller, @NotNull PlayerShopData shop) {
        super(controller);
        this.shop = shop;
    }

    @Override
    public void open() {
        contentSlots = MenuKeys.SHOP_STORAGE_SLOTS.getIntList();
        pageSize = contentSlots.size();

        PlayerShop.getInstance().getStorageManager()
                .getOrLoad(shop.getShopId())
                .thenAccept(storage -> {
                    this.storage = storage;

                    PlayerShop.getScheduler().runTask(() -> {
                        super.open();
                        SoundUtil.play(menuController.owner(), MenuKeys.SHOP_STORAGE_SOUND_OPEN.getString());
                    });
                });
    }

    @Override
    public void handleMenu(@NotNull InventoryClickEvent event) {
        event.setCancelled(true);

        int raw = event.getRawSlot();
        int topSize = inventory.getSize();

        Player player = (Player) event.getWhoClicked();

        if (raw >= topSize && event.getClickedInventory() == player.getInventory() && shop.getMode() == ShopMode.SELL) {
            int amount = event.isShiftClick()
                    ? countPlayerItems(player, shop.getItemStack())
                    : 1;

            deposit(player, amount);
            return;
        }

        if (raw >= topSize) return;

        if (ItemKeys.SHOP_STORAGE_BACK.getSlots().contains(raw)) {
            SoundUtil.play(player, MenuKeys.SHOP_STORAGE_SOUND_ACTION.getString());
            new ShopMainMenu(menuController, shop).open();
            return;
        }

        if (ItemKeys.SHOP_STORAGE_NEXT.getSlots().contains(raw)) {
            if ((page + 1) * pageSize < storage.getContents().size()) {
                page++;
                updateMenuItems();
                SoundUtil.play(player, MenuKeys.SHOP_STORAGE_SOUND_ACTION.getString());
            }
            return;
        }

        if (ItemKeys.SHOP_STORAGE_PREVIOUS.getSlots().contains(raw)) {
            if (page > 0) {
                page--;
                updateMenuItems();
                SoundUtil.play(player, MenuKeys.SHOP_STORAGE_SOUND_ACTION.getString());
            }
            return;
        }

        if (!contentSlots.contains(raw)) return;

        int localIndex = contentSlots.indexOf(raw);
        int realIndex = page * pageSize + localIndex;

        int amount = event.isShiftClick()
                ? getStackAmount(storage.getContents(), realIndex)
                : 1;

        withdraw(player, realIndex, amount);
    }

    private void deposit(@NotNull Player player, int amount) {
        if (amount <= 0) return;

        ItemStack shopItem = shop.getItemStack();

        int available = countPlayerItems(player, shopItem);
        if (available <= 0) return;

        int capacity = StorageUtil.getRemainingCapacity(storage);
        if (capacity <= 0) {
            SoundUtil.play(player, MenuKeys.SHOP_STORAGE_SOUND_ERROR.getString());
            return;
        }

        int remaining = Math.min(amount, Math.min(available, capacity));

        ItemStack toAdd = shopItem.clone();
        toAdd.setAmount(remaining);

        StorageUtil.add(storage, toAdd);

        removeExact(player, shopItem, remaining);

        PlayerShop.getInstance().getStorageManager().saveAsync(storage);
        PlayerShop.getInstance().getVisualService().update(shop);

        SoundUtil.play(player, MenuKeys.SHOP_STORAGE_SOUND_ACTION.getString());
        updateMenuItems();
    }

    private void withdraw(@NotNull Player player, int index, int amount) {
        List<ItemStack> contents = storage.getContents();

        if (index < 0 || index >= contents.size()) return;

        ItemStack item = contents.get(index);
        if (item == null) {
            SoundUtil.play(player, MenuKeys.SHOP_STORAGE_SOUND_ERROR.getString());
            return;
        }

        int take = Math.min(amount, item.getAmount());

        ItemStack give = item.clone();
        give.setAmount(take);

        if (!player.getInventory().addItem(give).isEmpty()) {
            SoundUtil.play(player, MenuKeys.SHOP_STORAGE_SOUND_ERROR.getString());
            return;
        }

        if (item.getAmount() <= take) {
            contents.remove(index);
        } else {
            item.setAmount(item.getAmount() - take);
        }

        PlayerShop.getInstance().getStorageManager().saveAsync(storage);
        PlayerShop.getInstance().getVisualService().update(shop);

        SoundUtil.play(player, MenuKeys.SHOP_STORAGE_SOUND_ACTION.getString());
        updateMenuItems();
    }

    private void removeExact(@NotNull Player player, ItemStack base, int amount) {
        int remaining = amount;

        for (ItemStack content : player.getInventory().getContents()) {
            if (content == null) continue;
            if (!content.isSimilar(base)) continue;

            int take = Math.min(remaining, content.getAmount());

            content.setAmount(content.getAmount() - take);
            remaining -= take;

            if (remaining <= 0) return;
        }
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

    private int getStackAmount(List<ItemStack> contents, int index) {
        if (index < 0 || index >= contents.size()) return 0;

        ItemStack item = contents.get(index);
        return item == null ? 0 : item.getAmount();
    }

    @Override
    public void setMenuItems() {
        inventory.clear();

        ItemFactory.setItemsForMenu("shop-storage.items", inventory);

        List<ItemStack> contents = storage.getContents();

        int maxPage = (int) Math.ceil((double) contents.size() / pageSize) - 1;
        if (maxPage < 0) maxPage = 0;

        if (page > maxPage) page = maxPage;
        if (page < 0) page = 0;

        int start = page * pageSize;

        for (int i = 0; i < pageSize; i++) {
            int index = start + i;
            if (index >= contents.size()) break;

            inventory.setItem(contentSlots.get(i), contents.get(index));
        }
    }

    @Override
    public @NotNull String getMenuName() {
        return MenuKeys.SHOP_STORAGE_TITLE.getString();
    }

    @Override
    public int getSlots() {
        return MenuKeys.SHOP_STORAGE_SIZE.getInt();
    }

    @Override
    public int getMenuTick() {
        return MenuKeys.SHOP_STORAGE_TICK.getInt();
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