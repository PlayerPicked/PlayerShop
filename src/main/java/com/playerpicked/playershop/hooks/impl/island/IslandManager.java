package com.playerpicked.playershop.hooks.impl.island;

import com.playerpicked.playershop.identifiers.IslandAccess;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class IslandManager {

    @Nullable private volatile IslandHook hook;

    public void register(@Nullable IslandHook hook) {
        this.hook = hook;
    }

    @NotNull
    public IslandAccess check(@NotNull Player player, @NotNull Location location) {
        IslandHook current = hook;
        if (current == null) return IslandAccess.ALLOWED;

        return current.check(player, location);
    }

    public boolean isLocked(@NotNull Location location) {
        IslandHook current = hook;
        if (current == null) return false;

        return current.isLocked(location);
    }
}