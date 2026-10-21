package com.playerpicked.playershop.guis.impl;

import com.playerpicked.playershop.PlayerShop;
import com.playerpicked.playershop.data.MenuController;
import com.playerpicked.playershop.guis.ShopBoundMenu;
import com.playerpicked.playershop.guis.Menu;
import com.playerpicked.playershop.identifiers.keys.ItemKeys;
import com.playerpicked.playershop.identifiers.keys.MenuKeys;
import com.playerpicked.playershop.identifiers.keys.MessageKeys;
import com.playerpicked.playershop.item.ItemFactory;
import com.playerpicked.playershop.processor.MessageProcessor;
import com.playerpicked.playershop.shop.models.PlayerShopData;
import com.playerpicked.playershop.shop.models.PlayerShopStorage;
import com.playerpicked.playershop.utils.CurrencyPermissionUtil;
import com.playerpicked.playershop.utils.SoundUtil;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@SuppressWarnings("deprecation")
public final class ShopCurrencyMenu extends Menu implements ShopBoundMenu {

    private final PlayerShopData shop;
    private final List<String> currencies = new ArrayList<>();
    private final PlayerShop plugin = PlayerShop.getInstance();

    private List<Integer> contentSlots = List.of();

    public ShopCurrencyMenu(@NotNull MenuController controller, @NotNull PlayerShopData shop) {
        super(controller);
        this.shop = shop;
    }

    @Override
    public void open() {
        super.open();
        SoundUtil.play(menuController.owner(), MenuKeys.SHOP_CURRENCY_SOUND_OPEN.getString());
    }

    @Override
    public void setMenuItems() {
        inventory.clear();
        currencies.clear();

        contentSlots = MenuKeys.SHOP_CURRENCY_SLOTS.getIntList();

        if (contentSlots.isEmpty()) {
            PlayerShop.getInstance().getLogger().warning("currency-slots is empty! Using fallback.");
            contentSlots = List.of(0,1,2,3,4,5,6,7,8);
        }

        ItemFactory.setItemsForMenu("shop-currency.items", inventory);

        var hooksConfig = plugin.getHooks();
        var currenciesSection = hooksConfig.getSection("hooks.currency.currencies");
        var templateSection = plugin.getGuis().getSection("shop-currency.items.currency-template");

        if (currenciesSection == null) return;

        int index = 0;

        for (String id : currenciesSection.getRoutesAsStrings(false)) {
            if (index >= contentSlots.size()) break;

            var currencySection = currenciesSection.getSection(id);
            if (currencySection == null) continue;

            currencies.add(id);

            String displayName = currencySection.getString("display-name", id);
            String prefix = currencySection.getString("prefix", "");

            boolean selected = shop.getCurrencyId().equalsIgnoreCase(id);
            boolean allowed = CurrencyPermissionUtil.has(menuController.owner(), id);

            ItemStack item = null;

            var itemSection = currencySection.getSection("item");
            if (itemSection != null) {
                item = ItemFactory.buildItem(itemSection, "hooks.currency.currencies." + id + ".item")
                        .orElse(null);
            }

            if (item == null && templateSection != null) {
                item = ItemFactory.buildItem(templateSection, "shop-currency.items.currency-template")
                        .orElse(null);
            }

            if (item == null) continue;

            ItemStack finalItem = item.clone();

            var gui = plugin.getGuis();

            String selectedText = MessageProcessor.process(gui.getString("shop-currency.states.selected", "Selected"));
            String notSelectedText = MessageProcessor.process(gui.getString("shop-currency.states.not-selected", "Not selected"));
            String noPermissionText = MessageProcessor.process(gui.getString("shop-currency.states.no-permission", "No permission"));

            String state;
            if (selected) state = selectedText;
            else if (allowed) state = notSelectedText;
            else state = noPermissionText;

            Map<String, String> replacements = Map.of(
                    "{currency}", MessageProcessor.process(displayName),
                    "{prefix}", prefix,
                    "{selected}", state
            );

            finalItem.editMeta(meta -> apply(meta, replacements));

            int slot = contentSlots.get(index++);
            inventory.setItem(slot, finalItem);
        }
    }

    @Override
    public void handleMenu(@NotNull InventoryClickEvent event) {
        event.setCancelled(true);

        int raw = event.getRawSlot();
        int top = inventory.getSize();

        if (raw >= top) return;

        if (ItemKeys.SHOP_CURRENCY_BACK.getSlots().contains(raw)) {
            SoundUtil.play(menuController.owner(), MenuKeys.SHOP_CURRENCY_SOUND_ACTION.getString());
            new ShopMainMenu(menuController, shop).open();
            return;
        }

        if (!contentSlots.contains(raw)) return;

        int index = contentSlots.indexOf(raw);
        if (index >= currencies.size()) return;

        String selected = currencies.get(index);

        if (!CurrencyPermissionUtil.has(menuController.owner(), selected)) {
            SoundUtil.play(menuController.owner(), MenuKeys.SHOP_CURRENCY_SOUND_ERROR.getString());
            menuController.owner().sendMessage(MessageKeys.SHOP_CURRENCY_NO_PERMISSION.getMessage().replace("{currency}", displayName(selected)));

            return;
        }

        plugin.getStorageManager()
                .getOrLoad(shop.getShopId())
                .thenAccept(storage -> {
                    if (!isEmpty(storage)) {
                        PlayerShop.getScheduler().runTask(() -> {
                            SoundUtil.play(menuController.owner(), MenuKeys.SHOP_CURRENCY_SOUND_ERROR.getString());
                            menuController.owner().sendMessage(MessageKeys.SHOP_STORAGE_NOT_EMPTY_SIMPLE.getMessage());
                        });
                        return;
                    }

                    PlayerShop.getScheduler().runTask(() -> {
                        shop.setCurrencyId(selected);
                        plugin.getShopService().update(shop);
                        plugin.getVisualService().update(shop);

                        SoundUtil.play(menuController.owner(), MenuKeys.SHOP_CURRENCY_SOUND_ACTION.getString());

                        menuController.owner().sendMessage(MessageKeys.SHOP_CURRENCY_UPDATED.getMessage()
                                .replace("{currency}", selected));

                        updateMenuItems();
                    });
                });
    }

    @NotNull
    private String displayName(@NotNull String currencyId) {
        String name = plugin.getHooks().getString("hooks.currency.currencies." + currencyId + ".display-name", currencyId);
        return MessageProcessor.process(name);
    }

    private boolean isEmpty(@NotNull PlayerShopStorage storage) {
        for (ItemStack item : storage.getContents()) {
            if (item != null && !item.getType().isAir()) return false;
        }
        return true;
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

        for (var entry : replacements.entrySet()) {
            out = out.replace(entry.getKey(), entry.getValue());
        }

        return out;
    }

    @Override
    public @NotNull String getMenuName() {
        return MenuKeys.SHOP_CURRENCY_TITLE.getString();
    }

    @Override
    public int getSlots() {
        return MenuKeys.SHOP_CURRENCY_SIZE.getInt();
    }

    @Override
    public int getMenuTick() {
        return MenuKeys.SHOP_CURRENCY_TICK.getInt();
    }

    @Override
    public @NotNull PlayerShopData getShop() {
        return shop;
    }
}