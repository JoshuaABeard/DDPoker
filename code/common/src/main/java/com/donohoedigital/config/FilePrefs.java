/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 Joshua Beard and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * For the full License text, please see the LICENSE.txt file
 * in the root directory of this project.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

/**
 * File-based JSON configuration manager.
 * Replaces Java Preferences API with portable JSON configuration file.
 *
 * Features:
 * - Platform-specific config directories (Windows: %APPDATA%, macOS: ~/Library/Application Support, Linux: ~/.ddpoker)
 * - Simple backup strategy (config.json.bak before each write)
 * - Automatic corruption recovery from backup
 * - Thread-safe operations
 * - Immediate flush on every change
 */
public class FilePrefs {
    private static final Logger logger = LogManager.getLogger(FilePrefs.class);

    private static final String CONFIG_FILE = "config.json";
    private static final String BACKUP_FILE = "config.json.bak";

    private static FilePrefs instance;
    private static String testConfigDir = null;  // For testing only

    private final String configDir;
    private final ObjectMapper objectMapper;
    private Map<String, Object> config;

    /**
     * Get singleton instance with default platform-specific config directory
     */
    public static FilePrefs getInstance() {
        if (instance == null) {
            synchronized (FilePrefs.class) {
                if (instance == null) {
                    String dir = testConfigDir != null ? testConfigDir : getConfigDirectory();
                    instance = new FilePrefs(dir);
                }
            }
        }
        return instance;
    }

    /**
     * Set config directory for testing. Must be called before getInstance().
     * Package-visible for testing.
     */
    static void setTestConfigDir(String dir) {
        testConfigDir = dir;
        instance = null;  // Reset instance to use new directory
    }

    /**
     * Create FilePrefs with custom config directory (private - use getInstance())
     */
    private FilePrefs(String configDir) {
        this.configDir = configDir;
        this.objectMapper = new ObjectMapper();
        this.config = new HashMap<>();
        load();
    }

    /**
     * Get platform-specific config directory.
     * Windows: %APPDATA%\ddpoker (no dot prefix)
     * macOS: ~/Library/Application Support/ddpoker
     * Linux: ~/.ddpoker (with dot prefix)
     */
    public static String getConfigDirectory() {
        return getConfigDirectory(System.getProperty("os.name"));
    }

    /**
     * Get platform-specific config directory for given OS name.
     * Package-visible for testing.
     */
    static String getConfigDirectory(String osName) {
        String os = osName.toLowerCase();

        // Check for macOS first (more specific)
        if (os.contains("mac") || os.contains("darwin")) {
            // macOS: ~/Library/Application Support/ddpoker
            return System.getProperty("user.home") + "/Library/Application Support/ddpoker";
        } else if (os.contains("win")) {
            // Windows: %APPDATA%\ddpoker (no dot)
            return System.getenv("APPDATA") + File.separator + "ddpoker";
        } else {
            // Linux/Unix: ~/.ddpoker (with dot)
            return System.getProperty("user.home") + "/.ddpoker";
        }
    }

    /**
     * Get string value for key, or default if not found
     */
    public synchronized String get(String key, String defaultValue) {
        Object value = config.get(key);
        if (value == null) {
            return defaultValue;
        }
        return value.toString();
    }

    /**
     * Get boolean value for key, or default if not found
     */
    public synchronized boolean getBoolean(String key, boolean defaultValue) {
        Object value = config.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(value.toString());
    }

    /**
     * Get integer value for key, or default if not found
     */
    public synchronized int getInt(String key, int defaultValue) {
        Object value = config.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            logger.warn("Invalid integer value for key {}: {}", key, value);
            return defaultValue;
        }
    }

    /**
     * Get double value for key, or default if not found
     */
    public synchronized double getDouble(String key, double defaultValue) {
        Object value = config.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            logger.warn("Invalid double value for key {}: {}", key, value);
            return defaultValue;
        }
    }

    /**
     * Put string value for key. Flushes immediately.
     */
    public synchronized void put(String key, String value) {
        config.put(key, value);
        flush();
    }

    /**
     * Put boolean value for key. Flushes immediately.
     */
    public synchronized void putBoolean(String key, boolean value) {
        config.put(key, value);
        flush();
    }

    /**
     * Put integer value for key. Flushes immediately.
     */
    public synchronized void putInt(String key, int value) {
        config.put(key, value);
        flush();
    }

    /**
     * Put double value for key. Flushes immediately.
     */
    public synchronized void putDouble(String key, double value) {
        config.put(key, value);
        flush();
    }

    /**
     * Remove value for key. Flushes immediately.
     */
    public synchronized void remove(String key) {
        config.remove(key);
        flush();
    }

    /**
     * Flush configuration to disk with backup.
     * Creates backup of existing config before writing new one.
     * Thread-safe.
     */
    public synchronized void flush() {
        try {
            File configDirectory = new File(configDir);
            if (!configDirectory.exists()) {
                configDirectory.mkdirs();
            }

            File configFile = new File(configDirectory, CONFIG_FILE);
            File backupFile = new File(configDirectory, BACKUP_FILE);

            // Create backup of existing config before writing
            if (configFile.exists()) {
                Files.copy(configFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                logger.debug("Created backup: {}", backupFile.getAbsolutePath());
            }

            // Write new config with pretty printing
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(configFile, config);
            logger.debug("Flushed config to: {}", configFile.getAbsolutePath());

        } catch (IOException e) {
            logger.error("Failed to flush configuration", e);
        }
    }

    /**
     * Load configuration from disk.
     * Attempts to recover from backup if main config is corrupted.
     * Thread-safe.
     */
    public synchronized void load() {
        File configDirectory = new File(configDir);
        File configFile = new File(configDirectory, CONFIG_FILE);
        File backupFile = new File(configDirectory, BACKUP_FILE);

        // Try to load main config
        if (configFile.exists()) {
            try {
                config = objectMapper.readValue(configFile, new TypeReference<Map<String, Object>>() {});
                logger.info("Loaded configuration from: {}", configFile.getAbsolutePath());
                return;
            } catch (IOException e) {
                logger.warn("Config file corrupted, attempting to recover from backup: {}", e.getMessage());
            }
        }

        // Try backup if main failed or doesn't exist
        if (backupFile.exists()) {
            try {
                config = objectMapper.readValue(backupFile, new TypeReference<Map<String, Object>>() {});
                logger.info("Recovered configuration from backup: {}", backupFile.getAbsolutePath());
                // Restore from backup
                flush();
                return;
            } catch (IOException e) {
                logger.warn("Backup file also corrupted: {}", e.getMessage());
            }
        }

        // No valid config found, start fresh with defaults
        config = new HashMap<>();
        logger.info("Starting with fresh configuration (no existing config found)");
    }

    /**
     * Clear all configuration. Flushes immediately.
     */
    public synchronized void clear() {
        config.clear();
        flush();
    }

    /**
     * Get the config directory path
     */
    public String getConfigDir() {
        return configDir;
    }
}
