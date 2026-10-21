package com.playerpicked.playershop.utils;

import com.playerpicked.playershop.identifiers.keys.ConfigKeys;
import com.playerpicked.playershop.shop.models.PlayerShopStorage;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class StorageUtil {

    public static int count(@NotNull PlayerShopStorage storage, ItemStack target) {
        int total = 0;

        for (ItemStack item : storage.getContents()) {
            if (item == null) continue;
            if (!item.isSimilar(target)) continue;

            total += item.getAmount();
        }

        return total;
    }

    public static void remove(@NotNull PlayerShopStorage storage, ItemStack target, int amount) {
        var contents = storage.getContents();

        for (int i = 0; i < contents.size(); i++) {
            ItemStack item = contents.get(i);

            if (item == null) continue;
            if (!item.isSimilar(target)) continue;

            int remove = Math.min(item.getAmount(), amount);

            item.setAmount(item.getAmount() - remove);

            if (item.getAmount() <= 0) {
                contents.remove(i);
                i--;
            }

            amount -= remove;
            if (amount <= 0) return;
        }
    }

    public static boolean add(@NotNull PlayerShopStorage storage, @NotNull ItemStack stack) {
        int remaining = stack.getAmount();

        if (remaining > getRemainingCapacity(storage)) return false;

        for (ItemStack existing : storage.getContents()) {
            if (!existing.isSimilar(stack)) continue;

            int space = existing.getMaxStackSize() - existing.getAmount();
            if (space <= 0) continue;

            int move = Math.min(space, remaining);
            existing.setAmount(existing.getAmount() + move);

            remaining -= move;
            if (remaining == 0) return true;
        }

        while (remaining > 0) {
            int move = Math.min(stack.getMaxStackSize(), remaining);

            ItemStack newStack = stack.clone();
            newStack.setAmount(move);

            storage.getContents().add(newStack);

            remaining -= move;
        }

        return true;
    }

    public static int countAll(@NotNull PlayerShopStorage storage) {
        int total = 0;

        for (ItemStack item : storage.getContents()) {
            if (item == null) continue;
            total += item.getAmount();
        }

        return total;
    }

    public static int getRemainingCapacity(@NotNull PlayerShopStorage storage) {
        int max = ConfigKeys.STORAGE_MAX_ITEMS.getInt();
        return Math.max(0, max - countAll(storage));
    }
}