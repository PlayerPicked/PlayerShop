package com.playerpicked.playershop.utils;

import lombok.experimental.UtilityClass;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.Base64;
import java.util.Locale;
import java.util.regex.Pattern;

@UtilityClass
@SuppressWarnings("deprecation")
public final class ItemUtil {

    private final Pattern WHITESPACE = Pattern.compile("\\s+");

    @NotNull
    public String searchText(@NotNull ItemStack item) {
        StringBuilder builder = new StringBuilder();

        builder.append(item.getType().name().toLowerCase(Locale.ROOT).replace('_', ' '));

        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String itemName = meta.getItemName();
            if (!itemName.isEmpty()) builder.append(' ').append(normalize(itemName));

            if (meta.hasDisplayName()) builder.append(' ').append(normalize(meta.getDisplayName()));
        }

        return builder.toString();
    }

    @NotNull
    @SuppressWarnings("deprecation")
    public String displayName(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String itemName = meta.getItemName();
            if (!itemName.isEmpty()) return itemName;

            if (meta.hasDisplayName()) return meta.getDisplayName();
        }

        String lower = item.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
        StringBuilder builder = new StringBuilder(lower.length());

        for (String part : lower.split(" ")) {
            if (part.isEmpty()) continue;

            builder.append(Character.toUpperCase(part.charAt(0)))
                    .append(part, 1, part.length())
                    .append(' ');
        }

        return builder.toString().trim();
    }

    @NotNull
    @SuppressWarnings("deprecation")
    public String normalize(@Nullable String input) {
        if (input == null) return "";

        String stripped = ChatColor.stripColor(input);

        return WHITESPACE.matcher(stripped.toLowerCase(Locale.ROOT).replace('_', ' '))
                .replaceAll(" ")
                .trim();
    }

    public String serialize(ItemStack item) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (BukkitObjectOutputStream data = new BukkitObjectOutputStream(output)) {
            data.writeObject(item);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return Base64.getEncoder().encodeToString(output.toByteArray());
    }

    public ItemStack deserialize(String data) {
        ByteArrayInputStream input = new ByteArrayInputStream(Base64.getDecoder().decode(data));

        try (BukkitObjectInputStream stream = new BukkitObjectInputStream(input)) {
            return (ItemStack) stream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public String serializeInventory(@NotNull ItemStack[] contents) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (BukkitObjectOutputStream data = new BukkitObjectOutputStream(output)) {
            data.writeInt(contents.length);

            for (ItemStack item : contents) {
                data.writeObject(item);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return Base64.getEncoder().encodeToString(output.toByteArray());
    }

    @NotNull
    public ItemStack[] deserializeInventory(String data) {
        ByteArrayInputStream input = new ByteArrayInputStream(Base64.getDecoder().decode(data));

        try (BukkitObjectInputStream stream = new BukkitObjectInputStream(input)) {
            int size = stream.readInt();
            ItemStack[] contents = new ItemStack[size];

            for (int i = 0; i < size; i++) {
                contents[i] = (ItemStack) stream.readObject();
            }

            return contents;
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}