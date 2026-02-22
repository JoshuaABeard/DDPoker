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

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages player identity using UUID v4. Replaces legacy license key system
 * with modern UUID-based identification.
 *
 * <p>
 * Player IDs are auto-generated on first run and stored in a JSON file at
 * platform-specific locations:
 * <ul>
 * <li>Windows: {@code %APPDATA%\ddpoker\player.json}</li>
 * <li>macOS: {@code ~/Library/Application Support/ddpoker/player.json}</li>
 * <li>Linux: {@code ~/.ddpoker/player.json}</li>
 * </ul>
 *
 * <p>
 * Example player.json:
 *
 * <pre>
 * {
 *   "playerId": "550e8400-e29b-41d4-a716-446655440000",
 *   "createdAt": 1707523200000
 * }
 * </pre>
 *
 * @since 3.4.0-community
 */
public class PlayerIdentity {

    private static final String PLAYER_FILE = "player.json";
    private static final ObjectMapper mapper = new ObjectMapper();

    // For testing - allows override of config directory
    private static String configDirectoryOverride = null;

    /**
     * Generate a new UUID v4 player ID.
     *
     * @return A new UUID v4 string in format xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx
     */
    public static String generatePlayerId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Load existing player ID from file, or create a new one if file doesn't exist.
     * Handles corruption gracefully by generating a new ID if file is invalid.
     *
     * @return The player's UUID
     */
    public static String loadOrCreate() {
        File playerFile = getPlayerFile();

        // Try to load existing ID
        if (playerFile.exists()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = mapper.readValue(playerFile, Map.class);
                String playerId = (String) data.get("playerId");

                if (playerId != null && isValidUUID(playerId)) {
                    return playerId;
                }
                // Fall through to generate new ID if invalid
            } catch (IOException e) {
                // File corrupted, generate new ID
            }
        }

        // Generate new UUID and save it
        String newId = generatePlayerId();
        save(newId);
        return newId;
    }

    /**
     * Save player ID to disk. Creates parent directories if they don't exist.
     *
     * @param playerId
     *            The UUID to save
     * @throws RuntimeException
     *             if save fails
     */
    public static void save(String playerId) {
        try {
            File playerFile = getPlayerFile();

            // Create parent directories if needed
            File parentDir = playerFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    throw new IOException("Failed to create directory: " + parentDir);
                }
            }

            // Create JSON data
            Map<String, Object> data = new HashMap<>();
            data.put("playerId", playerId);
            data.put("createdAt", System.currentTimeMillis());

            // Write to file with pretty printing
            mapper.writerWithDefaultPrettyPrinter().writeValue(playerFile, data);

        } catch (IOException e) {
            throw new RuntimeException("Failed to save player ID", e);
        }
    }

    /**
     * Get player config file location.
     *
     * @return File object for player.json
     */
    private static File getPlayerFile() {
        String configDir = getConfigDirectory();
        return new File(configDir, PLAYER_FILE);
    }

    /**
     * Get platform-specific config directory.
     *
     * <p>
     * Detects OS and returns appropriate path:
     * <ul>
     * <li>Windows: {@code %APPDATA%\ddpoker}</li>
     * <li>macOS: {@code ~/Library/Application Support/ddpoker}</li>
     * <li>Linux: {@code ~/.ddpoker}</li>
     * </ul>
     *
     * @return Config directory path
     */
    static String getConfigDirectory() {
        // Allow test override
        if (configDirectoryOverride != null) {
            return configDirectoryOverride;
        }
        return FilePrefs.getConfigDirectory();
    }

    /**
     * Validate UUID format.
     *
     * @param uuid
     *            String to validate
     * @return true if valid UUID, false otherwise
     */
    private static boolean isValidUUID(String uuid) {
        if (uuid == null) {
            return false;
        }
        try {
            UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Override config directory for testing. Package-private for test access.
     *
     * @param directory
     *            Directory to use for testing, or null to reset
     */
    static void setConfigDirectoryForTesting(String directory) {
        configDirectoryOverride = directory;
    }

    /**
     * Reset config directory override for testing. Package-private for test access.
     */
    static void resetConfigDirectoryForTesting() {
        configDirectoryOverride = null;
    }
}
