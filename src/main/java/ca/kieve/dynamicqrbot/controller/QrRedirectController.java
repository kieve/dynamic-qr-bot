package ca.kieve.dynamicqrbot.controller;

import ca.kieve.dynamicqrbot.service.BotConfigService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
public class QrRedirectController {
    private final BotConfigService m_configService;

    public QrRedirectController(BotConfigService configService) {
        m_configService = configService;
    }

    @GetMapping("/{path}")
    public ResponseEntity<?> redirect(@PathVariable String path) {
        return m_configService.getDestinationForPath(path)
                .map(url -> ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create(url))
                        .build())
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
