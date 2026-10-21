package com.playerpicked.playershop.shop.models;

import lombok.Getter;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public final class PlayerShopStorage {

    private final UUID shopId;
    private final List<ItemStack> contents = new ArrayList<>();

    private volatile boolean dirty;

    public PlayerShopStorage(UUID shopId) {
        this.shopId = shopId;
    }

    public void add(ItemStack item) {
        contents.add(item);
        dirty = true;
    }

    public void remove(int index) {
        contents.remove(index);
        dirty = true;
    }

    public void set(int index, ItemStack item) {
        contents.set(index, item);
        dirty = true;
    }
}