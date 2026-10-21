package com.playerpicked.playershop;

import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import com.playerpicked.pickly.paper.PaperMetrics;
import com.playerpicked.playershop.config.Config;
import com.playerpicked.playershop.database.DatabaseManager;
import com.playerpicked.playershop.hooks.HookManager;
import com.playerpicked.playershop.hooks.impl.currency.CurrencyManager;
import com.playerpicked.playershop.hooks.impl.island.IslandManager;
import com.playerpicked.playershop.shop.manager.PlayerShopManager;
import com.playerpicked.playershop.shop.service.PlayerShopService;
import com.playerpicked.playershop.shop.service.ShopLoader;
import com.playerpicked.playershop.shop.service.ShopDeleteConfirmService;
import com.playerpicked.playershop.shop.service.ShopPriceService;
import com.playerpicked.playershop.shop.service.ShopSearchService;
import com.playerpicked.playershop.shop.service.ShopTeleportService;
import com.playerpicked.playershop.shop.service.ShopVisualService;
import com.playerpicked.playershop.shop.manager.PlayerShopStorageManager;
import com.playerpicked.playershop.shop.models.PlayerShopStorage;
import com.playerpicked.playershop.update.UpdateChecker;
import com.playerpicked.playershop.utils.AmountFormatUtil;
import com.playerpicked.playershop.utils.LegacyMigrationUtil;
import com.playerpicked.playershop.utils.LoggerUtils;
import com.playerpicked.playershop.utils.RegisterUtils;
import com.playerpicked.playershop.utils.SafeLocationUtil;
import com.playerpicked.playershop.utils.ShopBlockUtil;
import com.playerpicked.playershop.utils.TimeFormatUtil;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import lombok.Getter;
import org.bstats.bukkit.Metrics;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import revxrsal.zapper.ZapperJavaPlugin;

import java.io.File;

public final class PlayerShop extends ZapperJavaPlugin {

    @Getter private static PlayerShop instance;
    @Getter private static TaskScheduler scheduler;

    @Getter private Config language;
    @Getter private Config guis;
    @Getter private Config hooks;
    Config config;

    @Getter private PlayerShopManager shopManager;
    @Getter private ShopVisualService visualService;
    @Getter private PlayerShopService shopService;
    @Getter private PlayerShopStorageManager storageManager;
    @Getter private CurrencyManager currencyManager;
    @Getter private IslandManager islandManager;
    @Getter private HookManager hookManager;
    @Getter private ShopPriceService shopPriceService;
    @Getter private ShopSearchService searchService;
    @Getter private ShopTeleportService teleportService;
    @Getter private ShopDeleteConfirmService deleteConfirmService;

    private boolean migrationFailed;

    private PaperMetrics metrics;

    @Override
    public void onLoad() {
        instance = this;
        scheduler = UniversalScheduler.getScheduler(this);
        migrationFailed = !LegacyMigrationUtil.migrate(getDataFolder().toPath());
    }

    @Override
    public void onEnable() {
        if (migrationFailed) {
            LoggerUtils.error("Disabling plugin to protect legacy data. Move the mc-PlayerShop folder contents into the PlayerShop folder manually.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        initializeComponents();

        DatabaseManager.initialize();

        shopManager = new PlayerShopManager();
        visualService = new ShopVisualService();
        storageManager = new PlayerShopStorageManager();
        shopService = new PlayerShopService();
        currencyManager = new CurrencyManager();
        islandManager = new IslandManager();
        hookManager = new HookManager();
        shopPriceService = new ShopPriceService();
        searchService = new ShopSearchService();
        teleportService = new ShopTeleportService();
        deleteConfirmService = new ShopDeleteConfirmService();

        ShopLoader.load(shopManager, visualService);

        AmountFormatUtil.reload();
        TimeFormatUtil.reload();
        ShopBlockUtil.reload();
        SafeLocationUtil.reload();

        RegisterUtils.registerCommands();
        RegisterUtils.registerListeners();

        LoggerUtils.printStartup();
        hookManager.load();

        metrics = new PaperMetrics(this, "pck_6fHEJiYKJbo4YUqc3uTB9HRD");
        new UpdateChecker(136274).start();
    }

    @Override
    public void onDisable() {
        if (migrationFailed) return;
        if (metrics != null) metrics.shutdown();
        if (visualService != null) visualService.removeAll();

        for (PlayerShopStorage storage : storageManager.getAll()) {
            storageManager.saveAsync(storage);
        }

        DatabaseManager.getDatabase().shutdown();
    }

    public Config getConfiguration() {
        return config;
    }

    private void initializeComponents() {
        final GeneralSettings generalSettings = GeneralSettings.builder()
                .setUseDefaults(false)
                .build();

        final LoaderSettings loaderSettings = LoaderSettings.builder()
                .setAutoUpdate(true)
                .build();

        final UpdaterSettings updaterSettings = UpdaterSettings.builder()
                .setKeepAll(true)
                .setVersioning(new BasicVersioning("version"))
                .build();

        config = loadConfig("config.yml", generalSettings, loaderSettings, updaterSettings);
        language = loadConfig("messages.yml", generalSettings, loaderSettings, updaterSettings);
        guis = loadConfig("guis.yml", generalSettings, loaderSettings, updaterSettings);
        hooks = loadConfig("hooks.yml", generalSettings, loaderSettings, updaterSettings);
    }

    @NotNull
    @Contract("_, _, _, _ -> new")
    private Config loadConfig(@NotNull String fileName, @NotNull GeneralSettings generalSettings, @NotNull LoaderSettings loaderSettings, @NotNull UpdaterSettings updaterSettings) {
        return new Config(
                new File(getDataFolder(), fileName),
                getResource(fileName),
                generalSettings,
                loaderSettings,
                DumperSettings.DEFAULT,
                updaterSettings
        );
    }
}