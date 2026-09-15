package com.mongenscave.mcplayershop.hooks;

import com.mongenscave.mcplayershop.McPlayerShop;
import com.mongenscave.mcplayershop.hooks.impl.currency.CurrencyManager;
import com.mongenscave.mcplayershop.hooks.impl.currency.impl.PlayerPointsHook;
import com.mongenscave.mcplayershop.hooks.impl.currency.impl.VaultHook;
import com.mongenscave.mcplayershop.hooks.impl.island.impl.SuperiorSkyblockHook;
import com.mongenscave.mcplayershop.listener.WorldLoadListener;
import com.mongenscave.mcplayershop.utils.LoggerUtils;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public final class HookManager {

    private static final String SLIME_WORLD_EVENT = "com.infernalsuite.asp.api.events.LoadSlimeWorldEvent";

    private final McPlayerShop plugin = McPlayerShop.getInstance();
    private final CurrencyManager currencyManager = plugin.getCurrencyManager();

    @Getter private boolean slimeWorldHooked;

    public void load() {
        loadIntegrations();

        Section root = plugin.getHooks().getSection("hooks.currency");

        if (root == null) {
            LoggerUtils.warn("Hooks config missing 'hooks.currency' section.");
            return;
        }

        loadProviders(root.getSection("providers"));
        loadCurrencies(root.getSection("currencies"));
    }

    private void loadIntegrations() {
        Section integrations = plugin.getHooks().getSection("hooks.integrations");

        loadSlimeWorld(integrations);
        loadIslands(integrations);
    }

    private void loadIslands(Section integrations) {
        boolean enabled = integrations == null || integrations.getBoolean("superiorskyblock.enabled", true);

        if (!enabled) {
            plugin.getIslandManager().register(null);
            return;
        }

        if (!isPluginPresent("SuperiorSkyblock2")) {
            plugin.getIslandManager().register(null);
            LoggerUtils.warn("   [Hook] SuperiorSkyblock2 not found, skipping.");
            return;
        }

        boolean respectLocked = integrations == null || integrations.getBoolean("superiorskyblock.respect-locked-islands", true);
        boolean respectBans = integrations == null || integrations.getBoolean("superiorskyblock.respect-bans", true);

        plugin.getIslandManager().register(new SuperiorSkyblockHook(respectLocked, respectBans));
        LoggerUtils.info("\u001B[32m   [Hook] SuperiorSkyblock2 successfully enabled.\u001B[0m");
    }

    private void loadSlimeWorld(Section integrations) {
        if (slimeWorldHooked) return;

        boolean enabled = integrations == null || integrations.getBoolean("slimeworld.enabled", true);
        if (!enabled) return;

        if (!isClassPresent(SLIME_WORLD_EVENT)) {
            LoggerUtils.warn("   [Hook] SlimeWorld not found, skipping.");
            return;
        }

        plugin.getServer().getPluginManager().registerEvents(new WorldLoadListener(), plugin);
        slimeWorldHooked = true;
        LoggerUtils.info("\u001B[32m   [Hook] SlimeWorld successfully enabled.\u001B[0m");
    }

    private void loadProviders(Section section) {
        if (section == null) return;

        if (section.getBoolean("vault.enabled", false)) {
            if (isPluginPresent("Vault")) {
                var economy = plugin.getServer()
                        .getServicesManager()
                        .getRegistration(Economy.class);

                if (economy != null) {
                    currencyManager.register(new VaultHook(economy.getProvider()));
                    LoggerUtils.info("\u001B[32m   [Hook] Vault successfully enabled.\u001B[0m");
                } else {
                    LoggerUtils.warn("   [Hook] [Hook] Vault found but no Economy provider.");
                }
            } else {
                LoggerUtils.warn("   [Hook] [Hook] Vault not found, skipping.");
            }
        }

        if (section.getBoolean("playerpoints.enabled", false)) {
            if (isPluginPresent("PlayerPoints")) {
                LoggerUtils.info("\u001B[32m   [Hook] PlayerPoints successfully enabled.\u001B[0m");
                currencyManager.register(new PlayerPointsHook());
            } else {
                LoggerUtils.warn("   [Hook] PlayerPoints not found, skipping.");
            }
        }
    }

    private void loadCurrencies(Section section) {
        if (section == null) return;

        for (String key : section.getRoutesAsStrings(false)) {
            String base = "currencies." + key;

            String provider = plugin.getHooks().getString(base + ".provider");

            if (provider == null) continue;

            switch (provider.toLowerCase()) {

                case "vault" -> {
                    if (currencyManager.get("vault") == null) continue;
                    LoggerUtils.info("Currency registered: " + key + " (Vault)");
                }

                case "coinsengine" -> {
                    LoggerUtils.warn("CoinsEngine is no longer supported.");
                }

                case "playerpoints" -> {
                    if (currencyManager.get("playerpoints") == null) continue;
                    LoggerUtils.info("Currency registered: " + key + " (PlayerPoints)");
                }
            }
        }
    }

    private boolean isPluginPresent(String name) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(name);
        return plugin != null && plugin.isEnabled();
    }

    private boolean isClassPresent(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }
}