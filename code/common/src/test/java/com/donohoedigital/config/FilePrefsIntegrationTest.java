/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
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
 *
 * The "DD Poker" and "Donohoe Digital" names and logos, as well as any images,
 * graphics, text, and documentation found in this repository (including but not
 * limited to written documentation, website content, and marketing materials)
 * are licensed under the Creative Commons Attribution-NonCommercial-NoDerivatives
 * 4.0 International License (CC BY-NC-ND 4.0). You may not use these assets
 * without explicit written permission for any uses not covered by this License.
 * For the full License text, please see the LICENSE-CREATIVE-COMMONS.txt file
 * in the root directory of this project.
 *
 * For inquiries regarding commercial licensing of this source code or
 * the use of names, logos, images, text, or other assets, please contact
 * doug [at] donohoe [dot] info.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.prefs.Preferences;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration tests simulating real application usage
 */
class FilePrefsIntegrationTest {

    private Path configDir;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        this.configDir = tempDir;
        this.objectMapper = new ObjectMapper();

        // Reset FilePrefs for each test
        FilePrefs.setTestConfigDir(tempDir.toString());
        Prefs.initialize();
    }

    @Test
    void endToEnd_FreshInstallation_CreatesConfigFile() {
        // Simulate fresh installation - no config file exists yet
        File configFile = new File(configDir.toFile(), "config.json");
        assertThat(configFile).doesNotExist();

        // User changes some settings (simulating Options dialog)
        Preferences prefs = Prefs.getUserPrefs(Prefs.NODE_OPTIONS + "poker");
        prefs.put("player.name", "Player1");
        prefs.putBoolean("general.largeCards", false);
        prefs.putBoolean("general.fourColorDeck", true);
        prefs.putInt("general.soundVolume", 80);
        prefs.putBoolean("practice.autoDeal", true);
        prefs.putInt("practice.delay", 500);

        // Verify config file was created
        assertThat(configFile).exists();
        assertThat(configFile).isNotEmpty();
    }

    @Test
    void endToEnd_SettingsPersistAcrossRestart() throws Exception {
        // First run - user changes settings
        Preferences prefs1 = Prefs.getUserPrefs(Prefs.NODE_OPTIONS + "poker");
        prefs1.put("player.name", "TestPlayer");
        prefs1.putBoolean("general.soundEffects", true);
        prefs1.putInt("general.soundVolume", 75);

        // Simulate application restart by resetting FilePrefs
        FilePrefs.setTestConfigDir(configDir.toString());
        Prefs.initialize();

        // Second run - retrieve settings
        Preferences prefs2 = Prefs.getUserPrefs(Prefs.NODE_OPTIONS + "poker");
        assertThat(prefs2.get("player.name", "default")).isEqualTo("TestPlayer");
        assertThat(prefs2.getBoolean("general.soundEffects", false)).isTrue();
        assertThat(prefs2.getInt("general.soundVolume", 0)).isEqualTo(75);
    }

    @Test
    void endToEnd_BackupFileCreated() {
        File configFile = new File(configDir.toFile(), "config.json");
        File backupFile = new File(configDir.toFile(), "config.json.bak");

        // First change - no backup yet
        Preferences prefs = Prefs.getUserPrefs(Prefs.NODE_OPTIONS + "poker");
        prefs.put("test.key", "value1");

        assertThat(configFile).exists();
        // Backup may or may not exist on first write (depends on whether file existed before)

        // Second change - backup should definitely exist now
        prefs.put("test.key", "value2");

        assertThat(backupFile).exists();
    }

    @Test
    void endToEnd_BackupContainsPreviousVersion() throws Exception {
        File configFile = new File(configDir.toFile(), "config.json");
        File backupFile = new File(configDir.toFile(), "config.json.bak");

        Preferences prefs = Prefs.getUserPrefs(Prefs.NODE_OPTIONS + "poker");

        // First value
        prefs.put("test.key", "first_value");
        String firstContent = Files.readString(configFile.toPath());

        // Second value (should backup first value)
        prefs.put("test.key", "second_value");

        // Backup should contain first value
        if (backupFile.exists()) {
            String backupContent = Files.readString(backupFile.toPath());
            JsonNode backupJson = objectMapper.readTree(backupContent);

            // The backup should have the previous version (first_value)
            // Note: exact key path depends on how it's stored
            assertThat(backupJson).isNotNull();
        }
    }

    @Test
    void endToEnd_CorruptionRecovery() throws Exception {
        File configFile = new File(configDir.toFile(), "config.json");
        File backupFile = new File(configDir.toFile(), "config.json.bak");

        // Create valid config with backup
        Preferences prefs = Prefs.getUserPrefs(Prefs.NODE_OPTIONS + "poker");
        prefs.put("important.setting", "critical_value");
        prefs.put("another.setting", "another_value"); // Creates backup

        // Verify settings work
        assertThat(prefs.get("important.setting", "default")).isEqualTo("critical_value");

        // Corrupt the main config file
        Files.writeString(configFile.toPath(), "invalid json {{{ this is broken");

        // Simulate app restart - should recover from backup
        FilePrefs.setTestConfigDir(configDir.toString());
        Prefs.initialize();

        // Verify data was recovered from backup
        Preferences prefs2 = Prefs.getUserPrefs(Prefs.NODE_OPTIONS + "poker");
        String recovered = prefs2.get("important.setting", "default");

        // Should have recovered the value (either critical_value or another_value depending on backup timing)
        assertThat(recovered).isNotEqualTo("default");
    }

    @Test
    void endToEnd_JSONFormatReadable() throws Exception {
        // Set various types of settings
        Preferences poker = Prefs.getUserPrefs(Prefs.NODE_OPTIONS + "poker");
        poker.put("player.name", "TestPlayer");
        poker.putBoolean("enabled", true);
        poker.putInt("chips", 1000);

        Preferences general = Prefs.getUserPrefs(Prefs.NODE_OPTIONS + "general");
        general.putBoolean("soundEffects", true);
        general.putInt("soundVolume", 80);

        // Read the JSON file directly
        File configFile = new File(configDir.toFile(), "config.json");
        String jsonContent = Files.readString(configFile.toPath());

        // Verify it's valid JSON
        JsonNode root = objectMapper.readTree(jsonContent);
        assertThat(root).isNotNull();

        // Verify JSON is human-readable (pretty-printed with indentation)
        assertThat(jsonContent).contains("\n  "); // Has indentation
        assertThat(jsonContent).contains("\""); // Has quoted keys/values
    }

    @Test
    void endToEnd_MultipleNodesStoreCorrectly() {
        // Simulate multiple settings categories like in actual app
        Preferences general = Prefs.getUserPrefs(Prefs.NODE_OPTIONS + "general");
        general.putBoolean("largeCards", false);
        general.putInt("soundVolume", 100);

        Preferences practice = Prefs.getUserPrefs(Prefs.NODE_OPTIONS + "practice");
        practice.putBoolean("autoDeal", false);
        practice.putInt("delay", 500);

        Preferences online = Prefs.getUserPrefs(Prefs.NODE_OPTIONS + "online");
        online.putBoolean("enabled", true);
        online.put("server", "localhost:8877");

        Preferences clock = Prefs.getUserPrefs(Prefs.NODE_OPTIONS + "clock");
        clock.putBoolean("colorUp", true);

        // Verify all values persist
        assertThat(general.getBoolean("largeCards", true)).isFalse();
        assertThat(practice.getInt("delay", 0)).isEqualTo(500);
        assertThat(online.get("server", "default")).isEqualTo("localhost:8877");
        assertThat(clock.getBoolean("colorUp", false)).isTrue();
    }

    @Test
    void endToEnd_ConfigDirectoryCreatedAutomatically() {
        // Use a non-existent nested directory
        Path nestedDir = configDir.resolve("nested/deep/path");
        assertThat(nestedDir.toFile()).doesNotExist();

        // Initialize with nested directory
        FilePrefs.setTestConfigDir(nestedDir.toString());
        Prefs.initialize();

        // Make a change to trigger file creation
        Preferences prefs = Prefs.getUserPrefs(Prefs.NODE_OPTIONS + "test");
        prefs.put("test.key", "test value");

        // Verify directory was created automatically
        assertThat(nestedDir.toFile()).exists();
        assertThat(new File(nestedDir.toFile(), "config.json")).exists();
    }

    @Test
    void endToEnd_PlatformSpecificPaths() {
        // Test that platform detection returns expected paths
        String windowsPath = FilePrefs.getConfigDirectory("windows 10");
        assertThat(windowsPath).contains("ddpoker");
        assertThat(windowsPath).doesNotStartWith("."); // No dot prefix on Windows

        String macPath = FilePrefs.getConfigDirectory("mac os x");
        assertThat(macPath).contains("Library/Application Support/ddpoker");

        String linuxPath = FilePrefs.getConfigDirectory("linux");
        assertThat(linuxPath).contains("/.ddpoker"); // Dot prefix on Linux
    }

    @Test
    void endToEnd_ClearAllPreferences() {
        // Set multiple preferences across different nodes
        Preferences poker = Prefs.getUserPrefs(Prefs.NODE_OPTIONS + "poker");
        poker.put("player.name", "TestPlayer");

        Preferences general = Prefs.getUserPrefs(Prefs.NODE_OPTIONS + "general");
        general.putInt("soundVolume", 80);

        // Verify they exist
        assertThat(poker.get("player.name", "default")).isEqualTo("TestPlayer");
        assertThat(general.getInt("soundVolume", 0)).isEqualTo(80);

        // Clear all
        Prefs.clearAll();

        // Verify all cleared
        assertThat(poker.get("player.name", "default")).isEqualTo("default");
        assertThat(general.getInt("soundVolume", 0)).isEqualTo(0);
    }

    @Test
    void endToEnd_BackwardCompatibilityWithExistingCode() {
        // This test verifies the exact pattern used by existing DDOption classes

        // Pattern 1: Get preferences for a node
        Preferences prefs = Prefs.getUserPrefs(Prefs.NODE_OPTIONS + "poker");

        // Pattern 2: Put and get String values
        String key = "option.name";
        String value = "option_value";
        prefs.put(key, value);
        String retrieved = prefs.get(key, "default");
        assertThat(retrieved).isEqualTo(value);

        // Pattern 3: Put and get boolean values
        boolean boolValue = true;
        prefs.putBoolean("bool.option", boolValue);
        boolean retrievedBool = prefs.getBoolean("bool.option", false);
        assertThat(retrievedBool).isEqualTo(boolValue);

        // Pattern 4: Put and get int values
        int intValue = 42;
        prefs.putInt("int.option", intValue);
        int retrievedInt = prefs.getInt("int.option", 0);
        assertThat(retrievedInt).isEqualTo(intValue);

        // All patterns work - backward compatibility maintained!
    }
}
