package ca.kieve.dynamicqrbot.model;

import java.util.List;

public record BotConfigFile(
        List<QrMapping> mappings,
        List<Long> adminWhitelist
) {
    public BotConfigFile {
        mappings = mappings == null ? List.of() : List.copyOf(mappings);
        adminWhitelist = adminWhitelist == null ? List.of() : List.copyOf(adminWhitelist);
    }

    public BotConfigFile() {
        this(List.of(), List.of());
    }
}
