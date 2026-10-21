package com.playerpicked.playershop.shop.models;

import com.playerpicked.playershop.identifiers.ShopMode;
import com.playerpicked.playershop.utils.ItemUtil;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Getter
public final class PlayerShopData {

    private final UUID shopId;
    private final UUID ownerUuid;
    private final Location location;

    private final String world;
    private final int x;
    private final int y;
    private final int z;

    private String itemId;
    @Setter private double price;
    @Setter private String currencyId;
    @Setter private ShopMode mode;

    @Setter private boolean enabled;

    @Setter private boolean listed;
    @Setter @Nullable private Location visitLocation;

    private final long createdAt;
    @Setter private long updatedAt;

    @Getter(AccessLevel.NONE) private transient ItemStack cachedItem;
    @Getter(AccessLevel.NONE) private transient String cachedSearchText;
    @Getter(AccessLevel.NONE) private transient String cachedItemName;
    @Getter(AccessLevel.NONE) private transient String cachedOwnerName;

    public PlayerShopData(UUID shopId, UUID ownerUuid, @NotNull Location location, String itemId,
                          double price, ShopMode mode, boolean enabled, long createdAt, long updatedAt,
                          String currencyId, boolean listed, @Nullable Location visitLocation) {
        this.shopId = shopId;
        this.ownerUuid = ownerUuid;
        this.location = location;

        this.world = location.getWorld().getName();
        this.x = location.getBlockX();
        this.y = location.getBlockY();
        this.z = location.getBlockZ();

        this.itemId = itemId;
        this.price = price;
        this.currencyId = currencyId;
        this.mode = mode;
        this.enabled = enabled;

        this.listed = listed;
        this.visitLocation = visitLocation;

        this.createdAt = createdAt;
        this.updatedAt = updatedAt;

        this.cachedItem = ItemUtil.deserialize(itemId);
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;

        this.cachedItem = null;
        this.cachedSearchText = null;
        this.cachedItemName = null;
    }

    @NotNull
    public ItemStack getItemStack() {
        return item().clone();
    }

    @NotNull
    public String getSearchText() {
        if (cachedSearchText == null) cachedSearchText = ItemUtil.searchText(item());
        return cachedSearchText;
    }

    @NotNull
    public String getItemDisplayName() {
        if (cachedItemName == null) cachedItemName = ItemUtil.displayName(item());
        return cachedItemName;
    }

    @NotNull
    public String getOwnerName() {
        Player online = Bukkit.getPlayer(ownerUuid);

        if (online != null) {
            cachedOwnerName = online.getName();
            return cachedOwnerName;
        }

        if (cachedOwnerName != null) return cachedOwnerName;

        String name = Bukkit.getOfflinePlayer(ownerUuid).getName();
        cachedOwnerName = name == null ? "Unknown" : name;

        return cachedOwnerName;
    }

    public boolean hasVisitLocation() {
        return visitLocation != null && visitLocation.getWorld() != null;
    }

    @NotNull
    private ItemStack item() {
        if (cachedItem == null) cachedItem = ItemUtil.deserialize(itemId);
        return cachedItem;
    }
}