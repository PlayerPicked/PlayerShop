package com.playerpicked.playershop.utils;

import com.playerpicked.playershop.identifiers.keys.ConfigKeys;
import lombok.experimental.UtilityClass;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@UtilityClass
public final class ShopBlockUtil {

    private static final Material FALLBACK = Material.BARREL;

    private volatile Set<Material> blocks = Collections.unmodifiableSet(EnumSet.of(FALLBACK));

    public void reload() {
        List<String> configured = ConfigKeys.SHOP_BLOCKS.getList();

        if (configured == null || configured.isEmpty()) {
            LoggerUtils.warn("No 'shop.blocks' configured -> falling back to " + FALLBACK);
            blocks = Collections.unmodifiableSet(EnumSet.of(FALLBACK));
            return;
        }

        Set<Material> parsed = EnumSet.noneOf(Material.class);

        for (String raw : configured) {
            if (raw == null || raw.isBlank()) continue;

            Material material = Material.matchMaterial(raw.trim());

            if (material == null) {
                LoggerUtils.warn("Unknown shop block material: " + raw + " -> skipping");
                continue;
            }

            if (!material.isBlock()) {
                LoggerUtils.warn("Shop block material is not placeable: " + raw + " -> skipping");
                continue;
            }

            parsed.add(material);
        }

        if (parsed.isEmpty()) {
            LoggerUtils.warn("No valid entry in 'shop.blocks' -> falling back to " + FALLBACK);
            parsed.add(FALLBACK);
        }

        blocks = Collections.unmodifiableSet(parsed);
    }

    public boolean isShopBlock(@Nullable Material material) {
        return material != null && blocks.contains(material);
    }

    public boolean hasContents(@NotNull Block block) {
        return block.getState() instanceof Container container && !container.getInventory().isEmpty();
    }

    public boolean isDoubleChest(@NotNull Block block) {
        return block.getState() instanceof Chest chest && chest.getInventory().getHolder() instanceof DoubleChest;
    }

    public @NotNull Material getDropMaterial(@NotNull Block block) {
        Material type = block.getType();
        if (isShopBlock(type)) return type;

        return blocks.iterator().next();
    }
}
