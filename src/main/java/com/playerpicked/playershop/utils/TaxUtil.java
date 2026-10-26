package com.playerpicked.playershop.utils;

import com.playerpicked.playershop.identifiers.keys.ConfigKeys;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public final class TaxUtil {

    public double calculate(@NotNull String currencyId, double total) {
        if (!ConfigKeys.TAX_ENABLED.getBoolean(false)) return 0;

        double rate = Math.min(getRate(currencyId), 100);
        if (rate <= 0) return 0;

        return Math.round(total * rate) / 100.0;
    }

    private double getRate(@NotNull String currencyId) {
        double rate = ConfigKeys.TAX_RATE.getDouble(0);

        Section overrides = ConfigKeys.TAX_CURRENCIES.getSection();
        if (overrides == null) return rate;

        return overrides.getDouble(currencyId, rate);
    }
}