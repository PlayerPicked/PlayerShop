package com.mongenscave.mcplayershop.commands;

import com.mongenscave.mcplayershop.McPlayerShop;
import com.mongenscave.mcplayershop.data.MenuController;
import com.mongenscave.mcplayershop.guis.impl.ShopSearchMenu;
import com.mongenscave.mcplayershop.identifiers.SearchFilter;
import com.mongenscave.mcplayershop.identifiers.SearchSort;
import com.mongenscave.mcplayershop.identifiers.keys.ConfigKeys;
import com.mongenscave.mcplayershop.identifiers.keys.MenuKeys;
import com.mongenscave.mcplayershop.identifiers.keys.MessageKeys;
import com.mongenscave.mcplayershop.shop.models.PlayerShop;
import com.mongenscave.mcplayershop.shop.models.ShopSearchResponse;
import com.mongenscave.mcplayershop.shop.service.ShopDeleteConfirmService;
import com.mongenscave.mcplayershop.utils.AmountFormatUtil;
import com.mongenscave.mcplayershop.utils.SafeLocationUtil;
import com.mongenscave.mcplayershop.utils.ShopBlockUtil;
import com.mongenscave.mcplayershop.utils.SoundUtil;
import com.mongenscave.mcplayershop.utils.TimeFormatUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Optional;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import java.util.UUID;

@Command({"mcplayershop", "playershop", "mc-playershop"})
@SuppressWarnings("unused")
public class CommandPlayerShop {
    private static final int DELETE_TARGET_RANGE = 6;

    private final McPlayerShop plugin = McPlayerShop.getInstance();

    @Subcommand("reload")
    @CommandPermission("mcplayershop.admin")
    private void reload(@NotNull CommandSender sender) {
        plugin.getConfiguration().reload();
        plugin.getLanguage().reload();
        plugin.getGuis().reload();
        plugin.getHooks().reload();

        plugin.getHookManager().load();

        AmountFormatUtil.reload();
        TimeFormatUtil.reload();
        ShopBlockUtil.reload();
        SafeLocationUtil.reload();

        plugin.getShopManager().getAll().forEach(playerShop -> plugin.getVisualService().update(playerShop));

        sender.sendMessage(MessageKeys.RELOAD.getMessage());
    }

    @Subcommand("create")
    @CommandPermission("mcplayershop.create")
    private void create(@NotNull Player player) {
        boolean enabled = plugin.getShopService().toggleCreationMode(player.getUniqueId());
        
        if (enabled) {
            player.sendMessage(MessageKeys.SHOP_CREATION_MODE_ENABLED.getMessage());
        } else {
            player.sendMessage(MessageKeys.SHOP_CREATION_MODE_DISABLED.getMessage());
        }
    }

    @Subcommand("delete")
    @CommandPermission("mcplayershop.admin")
    private void delete(@NotNull Player player) {
        Block target = player.getTargetBlockExact(DELETE_TARGET_RANGE);

        if (target == null) {
            player.sendMessage(MessageKeys.SHOP_ADMIN_DELETE_NO_TARGET.getMessage());
            return;
        }

        var optional = plugin.getShopManager().get(target.getLocation());

        if (optional.isEmpty()) {
            player.sendMessage(MessageKeys.SHOP_ADMIN_DELETE_NO_TARGET.getMessage());
            return;
        }

        PlayerShop shop = optional.get();
        boolean owner = shop.getOwnerUuid().equals(player.getUniqueId());

        if (!plugin.getShopService().canManage(player, shop)) {
            player.sendMessage(MessageKeys.NO_PERMISSION.getMessage());
            return;
        }

        ShopDeleteConfirmService confirmService = plugin.getDeleteConfirmService();

        if (!owner && !confirmService.confirm(player, shop)) {
            player.sendMessage(MessageKeys.SHOP_ADMIN_DELETE_CONFIRM.getMessage()
                    .replace("{owner}", shop.getOwnerName())
                    .replace("{seconds}", String.valueOf(ShopDeleteConfirmService.TIMEOUT_SECONDS)));
            return;
        }

        Location location = shop.getLocation();
        String ownerName = shop.getOwnerName();

        plugin.getShopService().remove(player, shop);

        McPlayerShop.getScheduler().runTask(location, () -> location.getBlock().setType(Material.AIR, false));

        player.sendMessage(owner
                ? MessageKeys.SHOP_DELETED.getMessage()
                : MessageKeys.SHOP_ADMIN_DELETED.getMessage().replace("{owner}", ownerName));
    }

    @Subcommand("search")
    @CommandPermission("mcplayershop.search")
    private void search(@NotNull Player player, @Optional String query) {
        if (!ConfigKeys.SEARCH_ENABLED.getBoolean(true)) {
            player.sendMessage(MessageKeys.SEARCH_DISABLED.getMessage());
            return;
        }

        String trimmed = query == null ? "" : query.trim();

        int minimum = plugin.getSearchService().minQueryLength();

        if (!trimmed.isEmpty() && trimmed.length() < minimum) {
            player.sendMessage(MessageKeys.SEARCH_TOO_SHORT.getMessage().replace("{min}", String.valueOf(minimum)));
            return;
        }

        SearchFilter filter = plugin.getSearchService().defaultFilter();
        SearchSort sort = plugin.getSearchService().defaultSort();

        if (!MessageKeys.SEARCH_SEARCHING.isEmpty()) player.sendMessage(MessageKeys.SEARCH_SEARCHING.getMessage().replace("{query}", trimmed.isEmpty() ? "*" : trimmed));

        UUID uuid = player.getUniqueId();

        plugin.getSearchService()
                .search(player, trimmed, filter, sort)
                .thenAccept(response -> {
                    Player online = Bukkit.getPlayer(uuid);
                    if (online == null || !online.isOnline()) return;

                    McPlayerShop.getScheduler().runTask(online, () -> openResults(online, trimmed, response, filter, sort));
                });
    }

    @Subcommand("hints")
    @CommandPermission("playershop.use")
    public void hints(Player player) {
        org.bukkit.persistence.PersistentDataContainer pdc = player.getPersistentDataContainer();
        if (pdc.has(com.mongenscave.mcplayershop.shop.service.PlayerShopService.HINTS_DISABLED, org.bukkit.persistence.PersistentDataType.BYTE)) {
            pdc.remove(com.mongenscave.mcplayershop.shop.service.PlayerShopService.HINTS_DISABLED);
            player.sendMessage(MessageKeys.SHOP_HINTS_ENABLED.getMessage());
        } else {
            pdc.set(com.mongenscave.mcplayershop.shop.service.PlayerShopService.HINTS_DISABLED, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
            player.sendMessage(MessageKeys.SHOP_HINTS_DISABLED.getMessage());
        }
    }

    private void openResults(@NotNull Player player, @NotNull String query, @NotNull ShopSearchResponse response, @NotNull SearchFilter filter, @NotNull SearchSort sort) {
        if (!player.isOnline()) return;

        String display = query.isEmpty() ? "*" : query;

        if (response.isEmpty()) {
            player.sendMessage(response.hidden() > 0
                    ? MessageKeys.SEARCH_ALL_UNAVAILABLE.getMessage()
                        .replace("{amount}", String.valueOf(response.hidden()))
                        .replace("{query}", display)
                    : MessageKeys.SEARCH_NO_RESULTS.getMessage()
                        .replace("{query}", display));

            SoundUtil.play(player, MenuKeys.SHOP_SEARCH_SOUND_ERROR.getString());
            return;
        }

        if (!MessageKeys.SEARCH_RESULTS.isEmpty()) {
            player.sendMessage(MessageKeys.SEARCH_RESULTS.getMessage()
                    .replace("{amount}", String.valueOf(response.results().size()))
                    .replace("{query}", display));
        }

        new ShopSearchMenu(MenuController.getMenuUtils(player), query, response.results(), filter, sort).open();
    }
}