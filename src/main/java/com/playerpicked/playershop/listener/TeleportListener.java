package com.playerpicked.playershop.listener;

import com.playerpicked.playershop.PlayerShop;
import com.playerpicked.playershop.shop.service.ShopTeleportService;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TeleportListener implements Listener {

    private final ShopTeleportService service = PlayerShop.getInstance().getTeleportService();

    @EventHandler(ignoreCancelled = true)
    public void onMove(@NotNull PlayerMoveEvent event) {
        if (!service.hasPending(event.getPlayer().getUniqueId())) return;
        if (!service.isCancelOnMove()) return;

        Location from = event.getFrom();
        Location to = event.getTo();

        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) return;

        service.cancel(event.getPlayer(), true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onTeleport(@NotNull PlayerTeleportEvent event) {
        if (!service.hasPending(event.getPlayer().getUniqueId())) return;

        service.cancel(event.getPlayer(), true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamage(@NotNull EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (service.hasPending(player.getUniqueId()) && service.isCancelOnDamage()) {
            service.cancel(player, true);
        }

        if (service.isProtected(player.getUniqueId())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamageByEntity(@NotNull EntityDamageByEntityEvent event) {
        Player attacker = resolveAttacker(event);
        if (attacker == null) return;

        service.clearProtection(attacker.getUniqueId());
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        service.forget(event.getPlayer().getUniqueId());
    }

    @Nullable
    private Player resolveAttacker(@NotNull EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) return player;

        if (event.getDamager() instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) return player;
        }

        return null;
    }
}