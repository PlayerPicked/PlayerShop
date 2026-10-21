package com.playerpicked.playershop.shop.service;

import com.playerpicked.playershop.PlayerShop;
import com.playerpicked.playershop.identifiers.keys.MessageKeys;
import com.playerpicked.playershop.processor.MessageProcessor;
import com.playerpicked.playershop.shop.models.PlayerShopData;
import com.playerpicked.playershop.utils.AmountFormatUtil;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ShopOwnerNotifyService {

    private static final long FLUSH_DELAY_TICKS = 60L;

    private final Map<String, PendingNotification> pending = new ConcurrentHashMap<>();

    public void notifyBuy(@NotNull PlayerShopData shop, @NotNull Player actor, int amount, double total) {
        enqueue(shop, actor, MessageKeys.SHOP_OWNER_NOTIFY_BUY, "BUY", amount, total);
    }

    public void notifySell(@NotNull PlayerShopData shop, @NotNull Player actor, int amount, double total) {
        enqueue(shop, actor, MessageKeys.SHOP_OWNER_NOTIFY_SELL, "SELL", amount, total);
    }

    private void enqueue(@NotNull PlayerShopData shop, @NotNull Player actor, @NotNull MessageKeys key, @NotNull String type, int amount, double total) {
        if (key.isEmpty()) return;
        if (shop.getOwnerUuid().equals(actor.getUniqueId())) return;

        String mapKey = shop.getShopId() + ":" + actor.getUniqueId() + ":" + type;

        PendingNotification notification = pending.compute(mapKey, (k, existing) -> {
            if (existing == null) return new PendingNotification(shop, actor.getName(), key, amount, total);

            existing.amount += amount;
            existing.total += total;
            return existing;
        });

        if (notification.scheduled.compareAndSet(false, true)) {
            PlayerShop.getScheduler().runTaskLater(() -> flush(mapKey), FLUSH_DELAY_TICKS);
        }
    }

    private void flush(@NotNull String mapKey) {
        PendingNotification notification = pending.remove(mapKey);
        if (notification == null) return;

        Player owner = Bukkit.getPlayer(notification.shop.getOwnerUuid());
        if (owner == null) return;

        String message = notification.key.getMessage()
                .replace("{player}", notification.actorName)
                .replace("{amount}", String.valueOf(notification.amount))
                .replace("{item}", resolveItemName(notification.shop.getItemStack()))
                .replace("{price}", AmountFormatUtil.format(notification.total))
                .replace("{currency}", MessageProcessor.process(getCurrencyPrefix(notification.shop)));

        owner.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(message));
    }

    @NotNull
    private String resolveItemName(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();

        if (meta != null && !meta.getItemName().isEmpty()) {
            return meta.getItemName();
        }

        if (meta != null && meta.hasDisplayName()) {
            return meta.getDisplayName();
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

    @NotNull
    private String getCurrencyPrefix(@NotNull PlayerShopData shop) {
        String base = "hooks.currency.currencies." + shop.getCurrencyId();
        String value = PlayerShop.getInstance().getHooks().getString(base + ".prefix");

        return value != null ? value : "";
    }

    private static final class PendingNotification {
        private final PlayerShopData shop;
        private final String actorName;
        private final MessageKeys key;
        private final AtomicBoolean scheduled = new AtomicBoolean(false);

        private int amount;
        private double total;

        private PendingNotification(PlayerShopData shop, String actorName, MessageKeys key, int amount, double total) {
            this.shop = shop;
            this.actorName = actorName;
            this.key = key;
            this.amount = amount;
            this.total = total;
        }
    }
}
