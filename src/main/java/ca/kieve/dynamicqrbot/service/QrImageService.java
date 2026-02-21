package ca.kieve.dynamicqrbot.service;

import ca.kieve.dynamicqrbot.config.QrBotProperties;
import ca.kieve.dynamicqrbot.model.QrMapping;
import io.nayuki.qrcodegen.QrCode;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class QrImageService {
    private static final Logger LOG = LoggerFactory.getLogger(QrImageService.class);

    private final BotConfigService m_configService;
    private final QrBotProperties m_properties;
    private final Path m_imageDir;

    private final ConcurrentHashMap<String, String> m_nicknameToFilename = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> m_filenameToContent = new ConcurrentHashMap<>();

    public QrImageService(BotConfigService configService, QrBotProperties properties) {
        m_configService = configService;
        m_properties = properties;
        m_imageDir = Path.of(properties.qrImageDir());
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(m_imageDir);
        } catch (IOException e) {
            LOG.error("Failed to create QR image directory: {}", m_imageDir, e);
            return;
        }

        for (QrMapping mapping : m_configService.getMappings()) {
            String filename = buildFilename(mapping);
            Path filePath = m_imageDir.resolve(filename);

            if (Files.exists(filePath)) {
                try {
                    String content = Files.readString(filePath);
                    m_nicknameToFilename.put(mapping.nickname(), filename);
                    m_filenameToContent.put(filename, content);
                    LOG.debug("Loaded cached QR image for '{}'", mapping.nickname());
                } catch (IOException e) {
                    LOG.warn("Failed to read cached QR image for '{}', regenerating",
                            mapping.nickname(), e);
                    generateQrImage(mapping);
                }
            } else {
                generateQrImage(mapping);
            }
        }

        LOG.info("QR image cache initialized with {} images", m_nicknameToFilename.size());
    }

    public void generateQrImage(QrMapping mapping) {
        String redirectUrl = buildRedirectUrl(mapping.staticPath());
        String svg = generateSvg(redirectUrl);
        String filename = buildFilename(mapping);
        Path filePath = m_imageDir.resolve(filename);

        try {
            Files.writeString(filePath, svg);
            m_nicknameToFilename.put(mapping.nickname(), filename);
            m_filenameToContent.put(filename, svg);
            LOG.info("Generated QR image for '{}': {}", mapping.nickname(), filename);
        } catch (IOException e) {
            LOG.error("Failed to write QR image for '{}'", mapping.nickname(), e);
        }
    }

    public void deleteQrImage(String nickname) {
        String filename = m_nicknameToFilename.remove(nickname);
        if (filename != null) {
            m_filenameToContent.remove(filename);
            try {
                Files.deleteIfExists(m_imageDir.resolve(filename));
                LOG.info("Deleted QR image for '{}'", nickname);
            } catch (IOException e) {
                LOG.error("Failed to delete QR image file for '{}'", nickname, e);
            }
        }
    }

    public String getImageContent(String filename) {
        return m_filenameToContent.get(filename);
    }

    public String getImageFilename(String nickname) {
        return m_nicknameToFilename.get(nickname);
    }

    public Map<String, String> getAllCachedImages() {
        return Collections.unmodifiableMap(m_nicknameToFilename);
    }

    private String buildRedirectUrl(String staticPath) {
        String base = m_properties.baseUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String path = staticPath.startsWith("/") ? staticPath : "/" + staticPath;
        return base + path;
    }

    private String buildFilename(QrMapping mapping) {
        String hash = md5(mapping.staticPath());
        return sanitize(mapping.nickname()) + "_" + hash + ".svg";
    }

    private String generateSvg(String text) {
        QrCode qr = QrCode.encodeText(text, QrCode.Ecc.MEDIUM);
        int size = qr.size;
        int border = 2;
        int total = size + border * 2;

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" ");
        sb.append("viewBox=\"0 0 ").append(total).append(' ').append(total).append("\" ");
        sb.append("stroke=\"none\">\n");
        sb.append("<rect width=\"100%\" height=\"100%\" fill=\"#FFFFFF\"/>\n");
        sb.append("<path d=\"");

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if (qr.getModule(x, y)) {
                    sb.append("M").append(x + border).append(',').append(y + border)
                            .append("h1v1h-1z ");
                }
            }
        }

        sb.append("\" fill=\"#000000\"/>\n");
        sb.append("</svg>\n");
        return sb.toString();
    }

    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not available", e);
        }
    }

    private static String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9_-]", "_");
    }
}
