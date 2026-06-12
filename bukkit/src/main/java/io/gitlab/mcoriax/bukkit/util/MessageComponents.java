package io.gitlab.mcoriax.bukkit.util;

import net.kyori.adventure.text.Component;

public final class MessageComponents {
    private MessageComponents() {}

    public static Component value(String prefixKey, String prefixFallback, Object value, String suffixKey, String suffixFallback) {
        return value(prefixKey, prefixFallback, Component.text(String.valueOf(value)), suffixKey, suffixFallback);
    }

    public static Component value(String prefixKey, String prefixFallback, Component value, String suffixKey, String suffixFallback) {
        return Component.translatable(prefixKey, prefixFallback)
                .append(value)
                .append(Component.translatable(suffixKey, suffixFallback));
    }

    public static Component values(
            String prefixKey,
            String prefixFallback,
            Object firstValue,
            String middleKey,
            String middleFallback,
            Object secondValue,
            String suffixKey,
            String suffixFallback
    ) {
        return Component.translatable(prefixKey, prefixFallback)
                .append(Component.text(String.valueOf(firstValue)))
                .append(Component.translatable(middleKey, middleFallback))
                .append(Component.text(String.valueOf(secondValue)))
                .append(Component.translatable(suffixKey, suffixFallback));
    }
}
