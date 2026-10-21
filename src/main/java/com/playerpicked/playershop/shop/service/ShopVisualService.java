package com.playerpicked.playershop.shop.service;

import com.playerpicked.playershop.PlayerShop;
import com.playerpicked.playershop.identifiers.ShopMode;
import com.playerpicked.playershop.identifiers.keys.ConfigKeys;
import com.playerpicked.playershop.identifiers.keys.MessageKeys;
import com.playerpicked.playershop.processor.MessageProcessor;
import com.playerpicked.playershop.shop.models.PlayerShopData;
import com.playerpicked.playershop.shop.models.PlayerShopStorage;
import com.playerpicked.playershop.utils.AmountFormatUtil;
import com.playerpicked.playershop.utils.StorageUtil;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("deprecation")
public final class ShopVisualService {

    private final Map<UUID, List<Entity>> visuals = new ConcurrentHashMap<>();

    public void spawn(@NotNull PlayerShopData shop) {
        List<Entity> existing = visuals.get(shop.getShopId());
        if (existing != null) {
            boolean allValid = existing.stream().allMatch(entity -> entity != null && entity.isValid());
            if (allValid) {
                update(shop);
                return;
            }

            remove(shop.getShopId());
        }

        Section config = ConfigKeys.HOLOGRAM.getSection();

        double offsetY = config.getDouble("offset-y");
        Location base = shop.getLocation().clone().add(0.5, offsetY, 0.5);
        ItemStack stack = shop.getItemStack();

        TextDisplay text = spawnText(base, stack, shop, config);
        ItemDisplay item = spawnItem(base, stack, config);

        visuals.put(shop.getShopId(), List.of(item, text));

        refreshStorage(shop);
    }

    private void refreshStorage(@NotNull PlayerShopData shop) {
        var storageManager = PlayerShop.getInstance().getStorageManager();
        if (storageManager.getCached(shop.getShopId()) != null) return;

        storageManager.getOrLoad(shop.getShopId()).thenAccept(storage ->
                PlayerShop.getScheduler().runTask(() -> update(shop)));
    }

    @NotNull
    private TextDisplay spawnText(@NotNull Location base, ItemStack stack, PlayerShopData shop, @NotNull Section config) {
        Section textSec = config.getSection("text");
        TextDisplay text = base.getWorld().spawn(base, TextDisplay.class);

        text.setText(buildText(stack, shop, textSec));

        text.setShadowed(textSec.getBoolean("shadow"));
        text.setBillboard(Display.Billboard.valueOf(textSec.getString("billboard")));

        String color = textSec.getSection("background").getString("color");
        text.setBackgroundColor(parseColor(color));

        text.setPersistent(false);

        return text;
    }

    @NotNull
    private ItemDisplay spawnItem(@NotNull Location base, ItemStack stack, @NotNull Section config) {
        Section itemSec = config.getSection("item");
        double offsetY = itemSec.getDouble("offset-y");
        float scale = itemSec.getFloat("scale");

        Location itemLoc = base.clone().add(0, offsetY, 0);

        ItemDisplay item = itemLoc.getWorld().spawn(itemLoc, ItemDisplay.class);

        item.setItemStack(stack);
        item.setBillboard(Display.Billboard.VERTICAL);
        item.setPersistent(false);

        item.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                item.getTransformation().getLeftRotation(),
                new Vector3f(scale, scale, scale),
                item.getTransformation().getRightRotation()
        ));

        return item;
    }

    public void update(@NotNull PlayerShopData shop) {
        List<Entity> list = visuals.get(shop.getShopId());
        if (list == null) return;

        Section config = ConfigKeys.HOLOGRAM.getSection();
        Section textSec = config.getSection("text");

        ItemStack stack = shop.getItemStack();

        for (Entity entity : list) {
            if (entity instanceof TextDisplay text) {
                text.setText(buildText(stack, shop, textSec));
            }
        }
    }

    public void remove(UUID shopId) {
        List<Entity> list = visuals.remove(shopId);
        if (list == null) return;

        list.forEach(Entity::remove);
    }

    @NotNull
    private String buildText(@NotNull ItemStack item, @NotNull PlayerShopData shop, @NotNull Section textSec) {
        List<String> lines = textSec.getStringList("lines");

        String itemName = shop.getItemDisplayName();
        String currencyId = shop.getCurrencyId();
        String currencyName = getCurrencyValue(shop, "display-name");
        String currencyPrefix = getCurrencyValue(shop, "prefix");

        final String ownerName = shop.getOwnerName();
        final int stock = resolveStock(shop, item);

        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            String parsed = line
                    .replace("{owner}", ownerName)
                    .replace("{item}", itemName)
                    .replace("{price}", AmountFormatUtil.format(shop.getPrice()))
                    .replace("{currency}", currencyName)
                    .replace("{currency_id}", currencyId)
                    .replace("{shop_mode}", resolveMode(shop))
                    .replace("{currency_prefix}", currencyPrefix)
                    .replace("{storage}", AmountFormatUtil.format(stock));

            parsed = MessageProcessor.process(parsed);

            builder.append(parsed).append("\n");
        }

        return builder.toString().trim();
    }

    @NotNull
    private Color parseColor(@NotNull String input) {
        if (input.equalsIgnoreCase("TRANSPARENT")) {
            return Color.fromARGB(0, 0, 0, 0);
        }

        if (input.startsWith("#")) {
            try {
                int rgb = Integer.parseInt(input.substring(1), 16);

                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                return Color.fromARGB(255, r, g, b);
            } catch (Exception ignored) {
                return Color.fromARGB(0, 0, 0, 0);
            }
        }

        return Color.fromARGB(0, 0, 0, 0);
    }

    private int resolveStock(@NotNull PlayerShopData shop, @NotNull ItemStack item) {
        PlayerShopStorage storage = PlayerShop.getInstance().getStorageManager().getCached(shop.getShopId());
        if (storage == null) return 0;

        return StorageUtil.count(storage, item);
    }

    @NotNull
    private String getCurrencyValue(@NotNull PlayerShopData shop, @NotNull String key) {
        String base = "hooks.currency.currencies." + shop.getCurrencyId();
        String value = PlayerShop.getInstance().getHooks().getString(base + "." + key);

        return value != null ? value : "";
    }

    @NotNull
    private String resolveMode(@NotNull PlayerShopData shop) {
        return shop.getMode() == ShopMode.SELL
                ? MessageKeys.SHOP_MODE_SELL.getMessage()
                : MessageKeys.SHOP_MODE_BUY.getMessage();
    }

    public void removeAll() {
        visuals.values().forEach(list -> list.forEach(Entity::remove));
        visuals.clear();
    }
}