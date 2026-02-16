package ca.kieve.dynamicqrbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "qr-bot")
public record QrBotProperties(
        String discordToken,
        String configFile,
        Long globalAdmin
) {
}
