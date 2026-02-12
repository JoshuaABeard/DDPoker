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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD tests for FilePrefs - File-based JSON configuration
 */
class FilePrefsTest {

    // RED: Test 1 - Platform directory detection for Windows
    @Test
    void should_DetectWindowsConfigDirectory_When_OsIsWindows() {
        // This test will fail because FilePrefs doesn't exist yet
        String configDir = FilePrefs.getConfigDirectory("windows 10");

        String expectedPath = System.getenv("APPDATA") + File.separator + "ddpoker";
        assertThat(configDir).isEqualTo(expectedPath);
        assertThat(configDir).doesNotContain("."); // Windows: no dot prefix
    }

    // RED: Test 2 - Platform directory detection variations for Windows
    @ParameterizedTest
    @ValueSource(strings = {"windows 10", "windows 11", "Windows NT"})
    void should_DetectWindowsDirectory_When_OsNameContainsWindows(String osName) {
        String configDir = FilePrefs.getConfigDirectory(osName);

        assertThat(configDir).contains("ddpoker");
        assertThat(configDir).contains(File.separator);
    }

    // RED: Test 3 - Platform directory detection for macOS
    @ParameterizedTest
    @ValueSource(strings = {"mac os x", "Mac OS X", "darwin"})
    void should_DetectMacConfigDirectory_When_OsIsMac(String osName) {
        String configDir = FilePrefs.getConfigDirectory(osName);

        String expectedPath = System.getProperty("user.home") + "/Library/Application Support/ddpoker";
        assertThat(configDir).isEqualTo(expectedPath);
        assertThat(configDir).contains("Library/Application Support");
    }

    // RED: Test 4 - Platform directory detection for Linux
    @ParameterizedTest
    @ValueSource(strings = {"linux", "Linux", "unix"})
    void should_DetectLinuxConfigDirectory_When_OsIsLinux(String osName) {
        String configDir = FilePrefs.getConfigDirectory(osName);

        String expectedPath = System.getProperty("user.home") + "/.ddpoker";
        assertThat(configDir).isEqualTo(expectedPath);
        assertThat(configDir).startsWith(System.getProperty("user.home"));
        assertThat(configDir).contains("/.ddpoker"); // Linux: with dot prefix
    }

    // RED: Test 5 - Config file creation with temp directory
    @Test
    void should_CreateConfigFile_When_FirstSaved(@TempDir Path tempDir) {
        // Override config directory for testing
        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs = FilePrefs.getInstance();

        prefs.put("test.key", "test value");
        prefs.flush();

        File configFile = new File(tempDir.toFile(), "config.json");
        assertThat(configFile).exists();
    }

    // RED: Test 6 - Load configuration from file
    @Test
    void should_LoadConfiguration_When_FileExists(@TempDir Path tempDir) {
        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs = FilePrefs.getInstance();

        // Save a value
        prefs.put("test.key", "test value");
        prefs.flush();

        // Reset and reload (simulates app restart)
        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs2 = FilePrefs.getInstance();

        // Verify value loaded
        String value = prefs2.get("test.key", "default");
        assertThat(value).isEqualTo("test value");
    }

    // RED: Test 7 - Get with default value when key not found
    @Test
    void should_ReturnDefaultValue_When_KeyNotFound(@TempDir Path tempDir) {
        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs = FilePrefs.getInstance();

        String value = prefs.get("nonexistent.key", "default value");

        assertThat(value).isEqualTo("default value");
    }

    // RED: Test 8 - Store and retrieve boolean values
    @Test
    void should_StoreAndRetrieveBooleanValues(@TempDir Path tempDir) {
        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs = FilePrefs.getInstance();

        prefs.putBoolean("test.bool", true);
        prefs.flush();

        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs2 = FilePrefs.getInstance();
        boolean value = prefs2.getBoolean("test.bool", false);

        assertThat(value).isTrue();
    }

    // RED: Test 9 - Store and retrieve integer values
    @Test
    void should_StoreAndRetrieveIntegerValues(@TempDir Path tempDir) {
        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs = FilePrefs.getInstance();

        prefs.putInt("test.int", 42);
        prefs.flush();

        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs2 = FilePrefs.getInstance();
        int value = prefs2.getInt("test.int", 0);

        assertThat(value).isEqualTo(42);
    }

    // RED: Test 10 - Store and retrieve double values
    @Test
    void should_StoreAndRetrieveDoubleValues(@TempDir Path tempDir) {
        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs = FilePrefs.getInstance();

        prefs.putDouble("test.double", 3.14159);
        prefs.flush();

        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs2 = FilePrefs.getInstance();
        double value = prefs2.getDouble("test.double", 0.0);

        assertThat(value).isEqualTo(3.14159);
    }

    // RED: Test 11 - Backup file creation
    @Test
    void should_CreateBackup_When_ConfigExists(@TempDir Path tempDir) {
        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs = FilePrefs.getInstance();

        // Create initial config
        prefs.put("test.key", "value1");
        prefs.flush();

        // Modify and flush again (should create backup)
        prefs.put("test.key", "value2");
        prefs.flush();

        // Verify backup exists
        File backupFile = new File(tempDir.toFile(), "config.json.bak");
        assertThat(backupFile).exists();
    }

    // RED: Test 12 - Recovery from corrupted config
    @Test
    void should_RecoverFromBackup_When_ConfigCorrupted(@TempDir Path tempDir) throws Exception {
        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs = FilePrefs.getInstance();

        // Create valid config
        prefs.put("test.key", "valid value");
        prefs.flush();

        // Modify to trigger backup
        prefs.put("test.key", "updated value");
        prefs.flush();

        // Corrupt main config file
        File configFile = new File(tempDir.toFile(), "config.json");
        java.nio.file.Files.writeString(configFile.toPath(), "invalid json {{{");

        // Reset and reload - should recover from backup
        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs2 = FilePrefs.getInstance();
        String value = prefs2.get("test.key", "default");

        // Should have recovered the "updated value" from backup
        assertThat(value).isEqualTo("updated value");
    }

    // RED: Test 13 - Thread safety with synchronized methods
    @Test
    void should_HandleConcurrentAccess_When_MultipleThreads(@TempDir Path tempDir) throws Exception {
        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs = FilePrefs.getInstance();

        // Create multiple threads that write different values
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                prefs.put("thread1.key", "value" + i);
                prefs.flush();
            }
        });

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                prefs.put("thread2.key", "value" + i);
                prefs.flush();
            }
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        // Verify both keys exist and have valid values
        String val1 = prefs.get("thread1.key", null);
        String val2 = prefs.get("thread2.key", null);

        assertThat(val1).isNotNull();
        assertThat(val2).isNotNull();
        assertThat(val1).startsWith("value");
        assertThat(val2).startsWith("value");
    }
}
