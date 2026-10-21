package com.playerpicked.playershop.utils;

import com.playerpicked.playershop.processor.MessageProcessor;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public final class ComponentUtil {

    @NotNull
    @Contract("_ -> new")
    public static Component of(@NotNull String text) {
        return Component.text(MessageProcessor.process(text));
    }
}