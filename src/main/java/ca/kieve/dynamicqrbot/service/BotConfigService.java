package ca.kieve.dynamicqrbot.service;

import ca.kieve.dynamicqrbot.config.QrBotProperties;
import ca.kieve.dynamicqrbot.model.BotConfigFile;
import ca.kieve.dynamicqrbot.model.QrMapping;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BotConfigService {
    private static final Logger LOG = LoggerFactory.getLogger(BotConfigService.class);

    private final ObjectMapper m_yamlMapper = new ObjectMapper(new YAMLFactory());
    private final File m_configFile;

    private BotConfigFile m_configData = new BotConfigFile();
    private final ConcurrentHashMap<String, QrMapping> m_byPath = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, QrMapping> m_byNickname = new ConcurrentHashMap<>();
    private volatile Set<Long> m_adminWhitelist = Set.of();

    public BotConfigService(QrBotProperties properties) {
        m_configFile = new File(properties.configFile());
    }

    @PostConstruct
    public void load() {
        if (!m_configFile.exists()) {
            LOG.info("Config file not found at {}, starting with empty config",
                    m_configFile.getAbsolutePath());
            return;
        }
        try {
            m_configData = m_yamlMapper.readValue(m_configFile, BotConfigFile.class);
            rebuildIndexes();
            LOG.info("Loaded {} QR mapping(s) and {} admin(s) from {}",
                    m_configData.mappings().size(),
                    m_configData.adminWhitelist().size(),
                    m_configFile.getAbsolutePath());
        } catch (IOException e) {
            LOG.error("Failed to load config file", e);
        }
    }

    public Optional<String> getDestinationForPath(String path) {
        QrMapping mapping = m_byPath.get(normalizeStaticPath(path));
        return mapping == null ? Optional.empty() : Optional.ofNullable(mapping.destinationUrl());
    }

    public synchronized boolean updateDestination(String nickname, String newUrl) {
        QrMapping existing = m_byNickname.get(nickname.toLowerCase());
        if (existing == null) {
            return false;
        }
        QrMapping updated = existing.withDestinationUrl(newUrl);
        List<QrMapping> newList = new ArrayList<>();
        for (QrMapping m : m_configData.mappings()) {
            newList.add(m == existing ? updated : m);
        }
        m_configData = new BotConfigFile(newList, m_configData.adminWhitelist());
        rebuildIndexes();
        save();
        return true;
    }

    public List<QrMapping> getMappings() {
        return List.copyOf(m_configData.mappings());
    }

    public synchronized boolean addMapping(String nickname, String staticPath, String destinationUrl) {
        if (m_byNickname.containsKey(nickname.toLowerCase())
                || m_byPath.containsKey(normalizeStaticPath(staticPath))) {
            return false;
        }
        QrMapping newMapping = new QrMapping(nickname, staticPath, destinationUrl);
        List<QrMapping> newList = new ArrayList<>(m_configData.mappings());
        newList.add(newMapping);
        m_configData = new BotConfigFile(newList, m_configData.adminWhitelist());
        rebuildIndexes();
        save();
        return true;
    }

    public synchronized boolean deleteMapping(String nickname) {
        QrMapping existing = m_byNickname.get(nickname.toLowerCase());
        if (existing == null) {
            return false;
        }
        List<QrMapping> newList = new ArrayList<>(m_configData.mappings());
        newList.remove(existing);
        m_configData = new BotConfigFile(newList, m_configData.adminWhitelist());
        rebuildIndexes();
        save();
        return true;
    }

    public List<String> getNicknames() {
        return m_configData.mappings().stream()
                .map(QrMapping::nickname)
                .toList();
    }

    public boolean isAdmin(long userId) {
        return m_adminWhitelist.contains(userId);
    }

    public synchronized boolean addAdmin(long userId) {
        if (m_adminWhitelist.contains(userId)) {
            return false;
        }
        List<Long> newAdminList = new ArrayList<>(m_configData.adminWhitelist());
        newAdminList.add(userId);
        m_configData = new BotConfigFile(m_configData.mappings(), newAdminList);
        rebuildIndexes();
        save();
        return true;
    }

    private void rebuildIndexes() {
        m_byPath.clear();
        m_byNickname.clear();
        for (QrMapping mapping : m_configData.mappings()) {
            m_byPath.put(normalizeStaticPath(mapping.staticPath()), mapping);
            m_byNickname.put(mapping.nickname().toLowerCase(), mapping);
        }
        m_adminWhitelist = Collections.unmodifiableSet(new HashSet<>(m_configData.adminWhitelist()));
    }

    private void save() {
        try {
            m_yamlMapper.writeValue(m_configFile, m_configData);
        } catch (IOException e) {
            LOG.error("Failed to save config file", e);
        }
    }

    private static String normalizeStaticPath(String path) {
        if (path == null) {
            return "";
        }
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        return normalized.toLowerCase();
    }
}
