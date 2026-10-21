package com.playerpicked.playershop.listener;

import com.playerpicked.playershop.PlayerShop;
import com.playerpicked.playershop.data.MenuController;
import com.playerpicked.playershop.guis.impl.ShopMainMenu;
import com.playerpicked.playershop.guis.impl.ShopTradeMenu;
import com.playerpicked.playershop.identifiers.keys.MessageKeys;
import com.playerpicked.playershop.shop.models.PlayerShopData;
import com.playerpicked.playershop.shop.service.PlayerShopService;
import com.playerpicked.playershop.shop.service.ShopDeleteConfirmService;
import com.playerpicked.playershop.utils.ShopBlockUtil;
import com.playerpicked.playershop.utils.ShopLimitUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@SuppressWarnings("deprecation")
public final class ShopListener implements Listener {

    private static final BlockFace[] HORIZONTAL_FACES = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};

    private final PlayerShopService service = PlayerShop.getInstance().getShopService();
    private final ShopDeleteConfirmService deleteConfirm = PlayerShop.getInstance().getDeleteConfirmService();

    private final java.util.Map<UUID, Long> hintCooldowns = new java.util.HashMap<>();
    private static final long HINT_COOLDOWN_MS = 30_000L;

    @EventHandler
    public void onInteract(@NotNull PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || !ShopBlockUtil.isShopBlock(block.getType())) return;

        Player player = event.getPlayer();
        var manager = PlayerShop.getInstance().getShopManager();

        var optional = manager.get(block.getLocation());

        if (optional.isPresent()) {
            if (player.isSneaking() && event.getItem() != null && event.getItem().getType().isBlock()) {
                return;
            }

            event.setUseInteractedBlock(Event.Result.DENY);
            event.setUseItemInHand(Event.Result.DENY);
            event.setCancelled(true);

            PlayerShopData shop = optional.get();

            if (shop.getOwnerUuid().equals(player.getUniqueId())) {
                openOwnerMenu(player, shop);
                return;
            }

            handleCustomerInteract(player, shop);
            return;
        }

        if (event.isCancelled()) return;

        if (!service.hasCreationMode(player.getUniqueId())) {
            if (player.isSneaking() && event.getItem() != null && event.getItem().getType() != Material.AIR) {
                if (!player.getPersistentDataContainer().has(PlayerShopService.HINTS_DISABLED, org.bukkit.persistence.PersistentDataType.BYTE)) {
                    long now = System.currentTimeMillis();
                    long last = hintCooldowns.getOrDefault(player.getUniqueId(), 0L);
                    if (now - last > HINT_COOLDOWN_MS) {
                        hintCooldowns.put(player.getUniqueId(), now);
                        player.sendMessage(MessageKeys.SHOP_HINT_CREATION.getMessage());
                    }
                }
            }
            return;
        }

        int limit = ShopLimitUtil.getLimit(player);
        int current = manager.getShopCount(player.getUniqueId());

        if (limit > 0 && current >= limit) {
            player.sendMessage(MessageKeys.SHOP_LIMIT_REACHED.getMessage()
                    .replace("{limit}", String.valueOf(limit))
                    .replace("{current}", String.valueOf(current)));

            event.setCancelled(true);
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) return;

        if (ShopBlockUtil.isDoubleChest(block)) {
            player.sendMessage(MessageKeys.SHOP_CREATE_FAILED_DOUBLE_CHEST.getMessage());
            event.setCancelled(true);
            return;
        }

        if (ShopBlockUtil.hasContents(block)) {
            player.sendMessage(MessageKeys.SHOP_CREATE_FAILED_BLOCK_CONTENT_NOT_EMPTY.getMessage());
            event.setCancelled(true);
            return;
        }

        service.create(player, block.getLocation(), item);
        service.removeCreationMode(player.getUniqueId());

        event.setCancelled(true);
    }

    private void openOwnerMenu(@NotNull Player player, @NotNull PlayerShopData shop) {
        new ShopMainMenu(MenuController.getMenuUtils(player), shop).open();
    }

    private void handleCustomerInteract(@NotNull Player player, @NotNull PlayerShopData shop) {
        new ShopTradeMenu(MenuController.getMenuUtils(player), shop).open();
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(@NotNull BlockPlaceEvent event) {
        Block placed = event.getBlockPlaced();
        if (placed.getType() != Material.CHEST && placed.getType() != Material.TRAPPED_CHEST) return;

        var manager = PlayerShop.getInstance().getShopManager();

        for (BlockFace face : HORIZONTAL_FACES) {
            Block relative = placed.getRelative(face);

            if (relative.getType() != placed.getType()) continue;
            if (manager.get(relative.getLocation()).isEmpty()) continue;

            event.setCancelled(true);
            event.getPlayer().sendMessage(MessageKeys.SHOP_CREATE_FAILED_DOUBLE_CHEST.getMessage());
            return;
        }
    }

    @EventHandler
    public void onBreak(@NotNull BlockBreakEvent event) {
        if (event.isCancelled()) return;

        Block block = event.getBlock();
        if (!ShopBlockUtil.isShopBlock(block.getType())) return;

        Player player = event.getPlayer();

        var manager = PlayerShop.getInstance().getShopManager();
        var optional = manager.get(block.getLocation());

        if (optional.isEmpty()) return;

        PlayerShopData shop = optional.get();
        boolean owner = shop.getOwnerUuid().equals(player.getUniqueId());

        if (!owner && !player.hasPermission(PlayerShopService.BYPASS_PERMISSION)) {
            event.setCancelled(true);
            return;
        }

        if (!player.isSneaking()) {
            event.setCancelled(true);
            return;
        }

        if (!owner && !deleteConfirm.confirm(player, shop)) {
            event.setCancelled(true);

            player.sendMessage(MessageKeys.SHOP_ADMIN_DELETE_CONFIRM.getMessage()
                    .replace("{owner}", resolveOwnerName(shop.getOwnerUuid()))
                    .replace("{seconds}", String.valueOf(ShopDeleteConfirmService.TIMEOUT_SECONDS)));

            return;
        }

        if (!service.remove(player, shop)) {
            event.setCancelled(true);
            player.sendMessage(MessageKeys.SHOP_DELETE_FAILED.getMessage());
            return;
        }

        if (!owner) {
            player.sendMessage(MessageKeys.SHOP_ADMIN_DELETED.getMessage()
                    .replace("{owner}", resolveOwnerName(shop.getOwnerUuid())));
        }

        event.setDropItems(false);
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemMove(@NotNull InventoryMoveItemEvent event) {
        if (isShopInventory(event.getDestination()) || isShopInventory(event.getSource())) {
            event.setCancelled(true);
        }
    }

    private boolean isShopInventory(@NotNull Inventory inventory) {
        if (!(inventory.getHolder(false) instanceof BlockState state)) return false;
        if (!ShopBlockUtil.isShopBlock(state.getType())) return false;

        return PlayerShop.getInstance().getShopManager().get(state.getLocation()).isPresent();
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        deleteConfirm.clear(event.getPlayer().getUniqueId());
    }

    private @NotNull String resolveOwnerName(@NotNull UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) return online.getName();

        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return name != null ? name : "Unknown";
    }
}