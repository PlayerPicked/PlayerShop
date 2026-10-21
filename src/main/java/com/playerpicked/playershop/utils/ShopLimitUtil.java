package com.playerpicked.playershop.utils;

import lombok.experimental.UtilityClass;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public final class ShopLimitUtil {

    private static final String PREFIX = "playershop.limit.";

    public int getLimit(@NotNull Player player) {
        int max = 0;

        for (PermissionAttachmentInfo perm : player.getEffectivePermissions()) {
            String permission = perm.getPermission();

            if (!permission.startsWith(PREFIX)) continue;

            String value = permission.substring(PREFIX.length());

            try {
                int parsed = Integer.parseInt(value);
                if (parsed > max) max = parsed;
            } catch (NumberFormatException ignored) {}
        }

        return max;
    }
}