package com.playerpicked.playershop.hooks.impl.island.impl;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.island.IslandPrivilege;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import com.playerpicked.playershop.hooks.impl.island.IslandHook;
import com.playerpicked.playershop.identifiers.IslandAccess;
import com.playerpicked.playershop.utils.LoggerUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SuperiorSkyblockHook implements IslandHook {

    private static final String CLOSE_BYPASS = "CLOSE_BYPASS";

    private final boolean respectLocked;
    private final boolean respectBans;

    public SuperiorSkyblockHook(boolean respectLocked, boolean respectBans) {
        this.respectLocked = respectLocked;
        this.respectBans = respectBans;
    }

    @Override
    public @NotNull IslandAccess check(@NotNull Player player, @NotNull Location location) {
        Island island = islandAt(location);
        if (island == null || island.isSpawn()) return IslandAccess.ALLOWED;

        SuperiorPlayer superiorPlayer;

        try {
            superiorPlayer = SuperiorSkyblockAPI.getPlayer(player);
        } catch (Throwable throwable) {
            return IslandAccess.ALLOWED;
        }

        if (superiorPlayer == null) return IslandAccess.ALLOWED;

        try {
            if (superiorPlayer.hasBypassModeEnabled()) return IslandAccess.ALLOWED;
            if (island.isMember(superiorPlayer) || island.isCoop(superiorPlayer)) return IslandAccess.ALLOWED;

            if (respectBans && island.isBanned(superiorPlayer)) return IslandAccess.BANNED;

            if (respectLocked && island.isLocked()) {
                IslandPrivilege bypass = closeBypass();

                if (bypass != null && island.hasPermission(superiorPlayer, bypass)) return IslandAccess.ALLOWED;

                return IslandAccess.LOCKED;
            }
        } catch (Throwable throwable) {
            LoggerUtils.warn("SuperiorSkyblock access check failed: " + throwable.getMessage());
            return IslandAccess.ALLOWED;
        }

        return IslandAccess.ALLOWED;
    }

    @Override
    public boolean isLocked(@NotNull Location location) {
        if (!respectLocked) return false;

        Island island = islandAt(location);
        if (island == null || island.isSpawn()) return false;

        try {
            return island.isLocked();
        } catch (Throwable throwable) {
            return false;
        }
    }

    @Nullable
    private Island islandAt(@NotNull Location location) {
        try {
            return SuperiorSkyblockAPI.getIslandAt(location);
        } catch (Throwable throwable) {
            return null;
        }
    }

    @Nullable
    private IslandPrivilege closeBypass() {
        try {
            return IslandPrivilege.getByName(CLOSE_BYPASS);
        } catch (Throwable throwable) {
            return null;
        }
    }
}