package com.playerpicked.playershop.guis;

import com.playerpicked.playershop.data.MenuController;
import com.playerpicked.playershop.guis.service.MenuService;
import com.playerpicked.playershop.processor.MenuProcessor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@SuppressWarnings("deprecation")
public abstract class Menu implements InventoryHolder {
    public MenuController menuController;
    protected Inventory inventory;

    private MenuProcessor updater;

    public Menu(@NotNull MenuController menuController) {
        this.menuController = menuController;
    }

    public abstract void handleMenu(final @NotNull InventoryClickEvent event);

    public abstract void setMenuItems();

    public abstract String getMenuName();

    public abstract int getSlots();

    public abstract int getMenuTick();

    @NotNull
    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        display();
    }

    protected boolean display() {
        Player player = menuController.owner();
        UUID uuid = player.getUniqueId();

        if (!MenuService.canOpen(uuid)) return false;
        if (!menuController.canOpen(getClass())) return false;

        inventory = Bukkit.createInventory(this, getSlots(), getMenuName());

        this.setMenuItems();

        InventoryView view = player.openInventory(inventory);
        if (view == null || view.getTopInventory() != inventory) {
            MenuController.remove(player);
            inventory = null;
            return false;
        }

        int tick = Math.max(0, getMenuTick());
        if (tick > 0) {
            if (updater == null) updater = new MenuProcessor(this);
            updater.start(tick);
        }

        return true;
    }

    public void close() {
        if (updater != null) {
            updater.stop();
            updater = null;
        }

        MenuController.remove(menuController.owner());
        inventory = null;
    }

    public void updateMenuItems() {
        if (inventory == null) return;

        setMenuItems();
        menuController.owner().updateInventory();
    }
}
