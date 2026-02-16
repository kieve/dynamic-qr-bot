package ca.kieve.dynamicqrbot;

import ca.kieve.dynamicqrbot.config.QrBotProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(QrBotProperties.class)
public class DynamicQrBotApplication {
    static void main(String[] args) {
        SpringApplication.run(DynamicQrBotApplication.class, args);
    }
}
