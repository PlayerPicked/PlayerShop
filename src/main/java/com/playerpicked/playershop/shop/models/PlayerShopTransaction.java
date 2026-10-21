package com.playerpicked.playershop.shop.models;

import java.util.UUID;

public record PlayerShopTransaction(UUID shopId, UUID playerUuid, String type, int amount, double price, long createdAt) {}