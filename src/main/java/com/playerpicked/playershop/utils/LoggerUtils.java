package com.playerpicked.playershop.utils;

import com.playerpicked.playershop.PlayerShop;
import com.playerpicked.playershop.database.DatabaseManager;
import lombok.experimental.UtilityClass;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class LoggerUtils {
    private final Logger logger = LogManager.getLogger("PlayerShop");

    public void info(@NotNull String msg, @NotNull Object... objs) {
        logger.info(msg, objs);
    }

    public void warn(@NotNull String msg, @NotNull Object... objs) {
        logger.warn(msg, objs);
    }

    public void error(@NotNull String msg, @NotNull Object... objs) {
        logger.error(msg, objs);
    }

    public void printStartup() {
        String main = "\u001B[38;2;255;138;101m";
        String reset = "\u001B[0m";
        String software = PlayerShop.getInstance().getServer().getName();
        String version = PlayerShop.getInstance().getServer().getVersion();

        info("");
        info("{}    ____  _        _ __   _______ ____  ____  _   _  ___  ____  {}", main, reset);
        info("{}   |  _ \\| |      / \\\\ \\ /  / ____|  _ \\/ ___|| | | |/ _ \\|  _ \\ {}", main, reset);
        info("{}   | |_) | |     / _ \\\\ V / |  _| | |_) \\___ \\| |_| | | | | |_) |{}", main, reset);
        info("{}   |  __/| |___ / ___ \\| | | |___|  _ < ___) |  _  | |_| |  __/ {}", main, reset);
        info("{}   |_|   |_____/_/   \\_\\_| |_____|_| \\_\\____/|_| |_|\\___/|_|    {}", main, reset);
        info("");
        info("{}   The plugin successfully started.{}", main, reset);
        info("{}   PlayerShop {} {}{}", main, software, version, reset);
        info("{}   Discord @ dc.playerpicked.com{}", main, reset);
        info("");
        info("\u001B[33m   [Database] Selected database type: {}\u001B[0m", DatabaseManager.getDatabaseType());
    }
}