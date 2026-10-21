package com.playerpicked.playershop.database.impl;

import com.playerpicked.playershop.PlayerShop;
import com.playerpicked.playershop.database.AbstractDatabase;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.util.List;

public final class H2 extends AbstractDatabase {

    @Override
    protected HikariDataSource createDataSource() {
        HikariConfig hikari = new HikariConfig();

        String dbPath = PlayerShop.getInstance().getDataFolder().getAbsolutePath() + "/data";
        hikari.setJdbcUrl("jdbc:h2:file:" + dbPath + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;AUTO_SERVER=TRUE");
        hikari.setDriverClassName("org.h2.Driver");

        applyPoolSettings(hikari);
        hikari.setAutoCommit(true);

        return new HikariDataSource(hikari);
    }

    @Override
    protected List<String> getTableDefinitions() {
        return List.of(
                """
                CREATE TABLE IF NOT EXISTS playershops (
                    shop_id VARCHAR(36) NOT NULL,
                    owner_uuid VARCHAR(36) NOT NULL,
                    world VARCHAR(64) NOT NULL,
                    x INT NOT NULL,
                    y INT NOT NULL,
                    z INT NOT NULL,
                    item_id CLOB NOT NULL,
                    price DOUBLE NOT NULL,
                    mode VARCHAR(10) NOT NULL,
                    enabled BOOLEAN NOT NULL,
                    currency_id VARCHAR(32) NOT NULL DEFAULT 'vault',
                    created_at BIGINT NOT NULL,
                    updated_at BIGINT NOT NULL,
                    listed BOOLEAN NOT NULL DEFAULT TRUE,
                    visit_world VARCHAR(64) NULL,
                    visit_x DOUBLE NULL,
                    visit_y DOUBLE NULL,
                    visit_z DOUBLE NULL,
                    visit_yaw FLOAT NULL,
                    visit_pitch FLOAT NULL,
                    PRIMARY KEY (shop_id),
                    UNIQUE KEY unique_location (world, x, y, z),
                    INDEX idx_owner (owner_uuid),
                    INDEX idx_world (world)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS playershop_transactions (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    shop_id VARCHAR(36) NOT NULL,
                    player_uuid VARCHAR(36) NOT NULL,
                    type VARCHAR(10) NOT NULL,
                    amount INT NOT NULL,
                    price DOUBLE NOT NULL,
                    created_at BIGINT NOT NULL,
                    INDEX idx_shop (shop_id),
                    INDEX idx_player (player_uuid)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS playershop_storage (
                    shop_id VARCHAR(36) NOT NULL,
                    data CLOB NOT NULL,
                    updated_at BIGINT NOT NULL,
                    PRIMARY KEY (shop_id)
                )
                """
        );
    }

    @Override
    protected String getStorageUpsertSql() {
        return """
            MERGE INTO playershop_storage (shop_id, data, updated_at)
            KEY(shop_id)
            VALUES (?, ?, ?)
        """;
    }
}