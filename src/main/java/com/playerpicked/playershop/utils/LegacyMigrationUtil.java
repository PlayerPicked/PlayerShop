package com.playerpicked.playershop.utils;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@UtilityClass
public class LegacyMigrationUtil {
    private final String LEGACY_FOLDER = "mc-PlayerShop";
    private final String LEGACY_PERMISSION_PREFIX = "mcplayershop.";
    private final String PERMISSION_PREFIX = "playershop.";
    private final Set<String> SKIPPED_ROOT_ENTRIES = Set.of("libs");

    public boolean migrate(@NotNull Path dataFolder) {
        Path legacyFolder = dataFolder.resolveSibling(LEGACY_FOLDER);
        if (!Files.isDirectory(legacyFolder)) return true;

        LoggerUtils.info("Found legacy {} folder, migrating to {}...", LEGACY_FOLDER, dataFolder.getFileName());

        try {
            Files.createDirectories(dataFolder);

            List<Path> entries;
            try (Stream<Path> stream = Files.list(legacyFolder)) {
                entries = stream.toList();
            }

            for (Path entry : entries) {
                String name = entry.getFileName().toString();
                if (SKIPPED_ROOT_ENTRIES.contains(name) || name.endsWith(".lock.db")) continue;
                moveRecursively(entry, dataFolder.resolve(name));
            }

            deleteRecursively(legacyFolder);
            LoggerUtils.info("Legacy data migrated successfully.");
            return true;
        } catch (IOException exception) {
            LoggerUtils.error("Failed to migrate legacy {} folder: {}", LEGACY_FOLDER, exception.toString());
            return false;
        }
    }

    private void moveRecursively(@NotNull Path source, @NotNull Path target) throws IOException {
        if (Files.isDirectory(source)) {
            Files.createDirectories(target);

            List<Path> children;
            try (Stream<Path> stream = Files.list(source)) {
                children = stream.toList();
            }

            for (Path child : children) {
                moveRecursively(child, target.resolve(child.getFileName().toString()));
            }
            return;
        }

        if (Files.exists(target)) {
            Path backup = target.resolveSibling(target.getFileName() + ".pre-migration");
            Files.move(target, backup, StandardCopyOption.REPLACE_EXISTING);
            LoggerUtils.warn("Existing {} was replaced by legacy data, backup saved as {}", target.getFileName(), backup.getFileName());
        }

        Files.move(source, target);

        if (target.getFileName().toString().endsWith(".yml")) rewritePermissions(target);
    }

    private void rewritePermissions(@NotNull Path file) throws IOException {
        String content = Files.readString(file, StandardCharsets.UTF_8);
        if (!content.contains(LEGACY_PERMISSION_PREFIX)) return;

        Files.writeString(file, content.replace(LEGACY_PERMISSION_PREFIX, PERMISSION_PREFIX), StandardCharsets.UTF_8);
    }

    private void deleteRecursively(@NotNull Path path) throws IOException {
        try (Stream<Path> stream = Files.walk(path)) {
            for (Path entry : stream.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(entry);
            }
        }
    }
}
