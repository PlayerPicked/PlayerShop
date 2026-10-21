package com.playerpicked.playershop.hooks.impl.island;

import com.playerpicked.playershop.identifiers.IslandAccess;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface IslandHook {
    @NotNull IslandAccess check(@NotNull Player player, @NotNull Location location);

    boolean isLocked(@NotNull Location location);
}