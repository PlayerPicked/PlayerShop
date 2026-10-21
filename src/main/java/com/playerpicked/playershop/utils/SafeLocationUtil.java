package com.playerpicked.playershop.utils;

import com.playerpicked.playershop.identifiers.keys.ConfigKeys;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@UtilityClass
public final class SafeLocationUtil {

    private static final BlockFace[] HORIZONTAL = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};

    private static final List<String> DEFAULT_DANGEROUS = List.of(
            "LAVA", "FIRE", "SOUL_FIRE", "CAMPFIRE", "SOUL_CAMPFIRE", "MAGMA_BLOCK",
            "CACTUS", "SWEET_BERRY_BUSH", "POWDER_SNOW", "WITHER_ROSE",
            "END_PORTAL", "NETHER_PORTAL", "LAVA_CAULDRON"
    );

    private volatile Set<Material> dangerous = EnumSet.noneOf(Material.class);
    private volatile int maxFall = 3;
    @Getter
    private volatile int searchRadius = 4;
    private volatile boolean requireEscape = true;
    private volatile boolean allowLiquids = false;
    private volatile List<int[]> offsets = List.of();

    public void reload() {
        List<String> configured = ConfigKeys.TELEPORT_SAFETY_DANGEROUS_BLOCKS.getList(DEFAULT_DANGEROUS);
        if (configured == null || configured.isEmpty()) configured = DEFAULT_DANGEROUS;

        Set<Material> parsed = EnumSet.noneOf(Material.class);

        for (String raw : configured) {
            if (raw == null || raw.isBlank()) continue;

            Material material = Material.matchMaterial(raw.trim());

            if (material == null) {
                LoggerUtils.warn("Unknown dangerous block: " + raw + " -> skipping");
                continue;
            }

            parsed.add(material);
        }

        dangerous = parsed;

        maxFall = Math.max(0, ConfigKeys.TELEPORT_SAFETY_MAX_FALL.getInt(3));
        searchRadius = Math.max(1, Math.min(16, ConfigKeys.TELEPORT_SAFETY_SEARCH_RADIUS.getInt(4)));
        requireEscape = ConfigKeys.TELEPORT_SAFETY_REQUIRE_ESCAPE.getBoolean(true);
        allowLiquids = ConfigKeys.TELEPORT_SAFETY_ALLOW_LIQUIDS.getBoolean(false);

        offsets = buildOffsets(searchRadius, maxFall);
    }

    public boolean isSafe(@Nullable Location location) {
        if (location == null) return false;

        World world = location.getWorld();
        if (world == null) return false;

        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        if (y <= world.getMinHeight() || y + 1 >= world.getMaxHeight()) return false;
        if (!isChunkLoaded(world, x, z)) return false;

        Block feet = world.getBlockAt(x, y, z);
        Block head = world.getBlockAt(x, y + 1, z);

        if (!isFree(feet) || !isFree(head)) return false;
        if (!hasGround(world, x, y, z)) return false;
        if (hasDangerNearby(world, x, y, z)) return false;

        return !requireEscape || !isSealed(feet, head);
    }

    @NotNull
    public Optional<Location> sanitize(@Nullable Location location) {
        if (location == null) return Optional.empty();

        if (isSafe(location)) return Optional.of(location.clone());

        Location raised = location.clone();
        raised.setY(location.getBlockY() + 1);

        if (isSafe(raised)) return Optional.of(raised);

        return Optional.empty();
    }

    @NotNull
    public Optional<Location> findSafeSpot(@Nullable Location shopBlock) {
        if (shopBlock == null) return Optional.empty();

        World world = shopBlock.getWorld();
        if (world == null) return Optional.empty();

        int baseX = shopBlock.getBlockX();
        int baseY = shopBlock.getBlockY();
        int baseZ = shopBlock.getBlockZ();

        for (int[] offset : offsets) {
            Location candidate = new Location(
                    world,
                    baseX + offset[0] + 0.5,
                    baseY + offset[1],
                    baseZ + offset[2] + 0.5);

            if (isSafe(candidate)) return Optional.of(candidate);
        }

        return Optional.empty();
    }

    @NotNull
    public Location facing(@NotNull Location from, @NotNull Location target) {
        Location result = from.clone();

        double dx = (target.getBlockX() + 0.5) - result.getX();
        double dy = (target.getBlockY() + 0.5) - (result.getY() + 1.62);
        double dz = (target.getBlockZ() + 0.5) - result.getZ();

        double horizontal = Math.sqrt(dx * dx + dz * dz);

        if (horizontal < 0.001 && Math.abs(dy) < 0.001) return result;

        result.setYaw((float) Math.toDegrees(Math.atan2(-dx, dz)));
        result.setPitch((float) Math.toDegrees(Math.atan2(-dy, horizontal)));

        return result;
    }

    private boolean isChunkLoaded(@NotNull World world, int x, int z) {
        return world.isChunkLoaded(x >> 4, z >> 4);
    }

    private boolean isFree(@NotNull Block block) {
        Material type = block.getType();

        if (dangerous.contains(type)) return false;
        if (block.isLiquid() && !allowLiquids) return false;

        return block.isPassable();
    }

    private boolean hasGround(@NotNull World world, int x, int y, int z) {
        for (int drop = 1; drop <= maxFall + 1; drop++) {
            int checkY = y - drop;
            if (checkY <= world.getMinHeight()) return false;

            Block below = world.getBlockAt(x, checkY, z);
            Material type = below.getType();

            if (dangerous.contains(type)) return false;
            if (below.isLiquid()) return allowLiquids;

            if (type.isSolid()) return true;
        }

        return false;
    }

    private boolean hasDangerNearby(@NotNull World world, int x, int y, int z) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (!isChunkLoaded(world, x + dx, z + dz)) return true;

                for (int dy = -1; dy <= 2; dy++) {
                    int checkY = y + dy;
                    if (checkY <= world.getMinHeight() || checkY >= world.getMaxHeight()) continue;

                    if (dangerous.contains(world.getBlockAt(x + dx, checkY, z + dz).getType())) return true;
                }
            }
        }

        return false;
    }

    private boolean isSealed(@NotNull Block feet, @NotNull Block head) {
        for (BlockFace face : HORIZONTAL) {
            if (!feet.getRelative(face).getType().isSolid()) return false;
            if (!head.getRelative(face).getType().isSolid()) return false;
        }

        return head.getRelative(BlockFace.UP).getType().isSolid();
    }

    @NotNull
    private @UnmodifiableView List<int[]> buildOffsets(int radius, int fall) {
        List<int[]> result = new ArrayList<>();

        for (int dy = 2; dy >= -(fall + 1); dy--) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx == 0 && dz == 0 && dy == 0) continue;

                    result.add(new int[]{dx, dy, dz});
                }
            }
        }

        result.sort((first, second) -> {
            int firstScore = Math.abs(first[1]) * 1000 + (first[0] * first[0] + first[2] * first[2]);
            int secondScore = Math.abs(second[1]) * 1000 + (second[0] * second[0] + second[2] * second[2]);

            return Integer.compare(firstScore, secondScore);
        });

        return Collections.unmodifiableList(result);
    }
}