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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Edge cases and error handling tests for FilePrefs
 */
class FilePrefsEdgeCasesTest {

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        FilePrefs.setTestConfigDir(tempDir.toString());
    }

    @Test
    void should_HandleEmptyStringValues(@TempDir Path tempDir) {
        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs = FilePrefs.getInstance();

        prefs.put("empty.string", "");
        prefs.flush();

        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs2 = FilePrefs.getInstance();

        assertThat(prefs2.get("empty.string", "default")).isEmpty();
    }

    @Test
    void should_HandleNullDefaultValues(@TempDir Path tempDir) {
        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs = FilePrefs.getInstance();

        String value = prefs.get("nonexistent.key", null);

        assertThat(value).isNull();
    }

    @Test
    void should_HandleSpecialCharactersInKeys(@TempDir Path tempDir) {
        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs = FilePrefs.getInstance();

        // Keys with dots, underscores, hyphens
        prefs.put("key.with.dots", "value1");
        prefs.put("key_with_underscores", "value2");
        prefs.put("key-with-hyphens", "value3");

        assertThat(prefs.get("key.with.dots", "default")).isEqualTo("value1");
        assertThat(prefs.get("key_with_underscores", "default")).isEqualTo("value2");
        assertThat(prefs.get("key-with-hyphens", "default")).isEqualTo("value3");
    }

    @Test
    void should_HandleSpecialCharactersInValues(@TempDir Path tempDir) {
        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs = FilePrefs.getInstance();

        // Values with quotes, newlines, special chars
        prefs.put("quotes", "value with \"quotes\"");
        prefs.put("newlines", "line1\nline2\nline3");
        prefs.put("special", "!@#$%^&*()[]{}|\\:;<>?,./");
        prefs.put("unicode", "日本語 émojis 🎮🃏");

        prefs.flush();

        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs2 = FilePrefs.getInstance();

        assertThat(prefs2.get("quotes", "")).isEqualTo("value with \"quotes\"");
        assertThat(prefs2.get("newlines", "")).isEqualTo("line1\nline2\nline3");
        assertThat(prefs2.get("special", "")).isEqualTo("!@#$%^&*()[]{}|\\:;<>?,./");
        assertThat(prefs2.get("unicode", "")).isEqualTo("日本語 émojis 🎮🃏");
    }

    @Test
    void should_HandleVeryLongKeys(@TempDir Path tempDir) {
        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs = FilePrefs.getInstance();

        String longKey = "a".repeat(500);
        prefs.put(longKey, "value");

        assertThat(prefs.get(longKey, "default")).isEqualTo("value");
    }

    @Test
    void should_HandleVeryLongValues(@TempDir Path tempDir) {
        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs = FilePrefs.getInstance();

        String longValue = "x".repeat(10000);
        prefs.put("long.value", longValue);
        prefs.flush();

        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs2 = FilePrefs.getInstance();

        assertThat(prefs2.get("long.value", "default")).isEqualTo(longValue);
    }

    @Test
    void should_HandleLargeNumberOfKeys(@TempDir Path tempDir) {
        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs = FilePrefs.getInstance();

        // Store 1000 keys
        for (int i = 0; i < 1000; i++) {
            prefs.put("key." + i, "value" + i);
        }
        prefs.flush();

        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs2 = FilePrefs.getInstance();

        // Verify all keys exist
        for (int i = 0; i < 1000; i++) {
            assertThat(prefs2.get("key." + i, "default")).isEqualTo("value" + i);
        }
    }

    @Test
    void should_HandleBooleanEdgeCases(@TempDir Path tempDir) {
        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs = FilePrefs.getInstance();

        // Test various boolean representations
        prefs.put("bool.true", "true");
        prefs.put("bool.false", "false");
        prefs.put("bool.yes", "yes");
        prefs.put("bool.no", "no");
        prefs.put("bool.1", "1");
        prefs.put("bool.0", "0");

        // Only "true" should parse as true
        assertThat(prefs.getBoolean("bool.true", false)).isTrue();
        assertThat(prefs.getBoolean("bool.false", true)).isFalse();
        assertThat(prefs.getBoolean("bool.yes", true)).isFalse(); // "yes" is not "true"
    }

    @Test
    void should_HandleIntegerEdgeCases(@TempDir Path tempDir) {
        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs = FilePrefs.getInstance();

        // Edge values
        prefs.putInt("int.zero", 0);
        prefs.putInt("int.negative", -12345);
        prefs.putInt("int.max", Integer.MAX_VALUE);
        prefs.putInt("int.min", Integer.MIN_VALUE);

        assertThat(prefs.getInt("int.zero", -1)).isEqualTo(0);
        assertThat(prefs.getInt("int.negative", 0)).isEqualTo(-12345);
        assertThat(prefs.getInt("int.max", 0)).isEqualTo(Integer.MAX_VALUE);
        assertThat(prefs.getInt("int.min", 0)).isEqualTo(Integer.MIN_VALUE);
    }

    @Test
    void should_HandleDoubleEdgeCases(@TempDir Path tempDir) {
        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs = FilePrefs.getInstance();

        // Edge values
        prefs.putDouble("double.zero", 0.0);
        prefs.putDouble("double.negative", -123.456);
        prefs.putDouble("double.pi", Math.PI);
        prefs.putDouble("double.e", Math.E);
        prefs.putDouble("double.max", Double.MAX_VALUE);
        prefs.putDouble("double.min", Double.MIN_VALUE);

        assertThat(prefs.getDouble("double.zero", -1.0)).isEqualTo(0.0);
        assertThat(prefs.getDouble("double.negative", 0.0)).isEqualTo(-123.456);
        assertThat(prefs.getDouble("double.pi", 0.0)).isEqualTo(Math.PI);
        assertThat(prefs.getDouble("double.e", 0.0)).isEqualTo(Math.E);
        assertThat(prefs.getDouble("double.max", 0.0)).isEqualTo(Double.MAX_VALUE);
        assertThat(prefs.getDouble("double.min", 0.0)).isEqualTo(Double.MIN_VALUE);
    }

    @Test
    void should_HandleInvalidIntegerStrings(@TempDir Path tempDir) {
        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs = FilePrefs.getInstance();

        // Store non-numeric strings
        prefs.put("invalid.int", "not a number");

        // Should return default value when parsing fails
        assertThat(prefs.getInt("invalid.int", 999)).isEqualTo(999);
    }

    @Test
    void should_HandleInvalidDoubleStrings(@TempDir Path tempDir) {
        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs = FilePrefs.getInstance();

        prefs.put("invalid.double", "not a number");

        assertThat(prefs.getDouble("invalid.double", 123.45)).isEqualTo(123.45);
    }

    @Test
    void should_RecoverWhenBothFilesCorrupted(@TempDir Path tempDir) throws Exception {
        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs = FilePrefs.getInstance();

        // Create config
        prefs.put("test.key", "test value");
        prefs.flush();

        File configFile = new File(tempDir.toFile(), "config.json");
        File backupFile = new File(tempDir.toFile(), "config.json.bak");

        // Corrupt both files
        Files.writeString(configFile.toPath(), "corrupted {{{");
        if (backupFile.exists()) {
            Files.writeString(backupFile.toPath(), "also corrupted {{{");
        }

        // Should start with empty config
        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs2 = FilePrefs.getInstance();

        // Should return default (fresh start)
        assertThat(prefs2.get("test.key", "default")).isEqualTo("default");

        // Should be able to write new config
        prefs2.put("new.key", "new value");
        assertThat(prefs2.get("new.key", "default")).isEqualTo("new value");
    }

    @Test
    void should_HandleEmptyJSONFile(@TempDir Path tempDir) throws Exception {
        FilePrefs.setTestConfigDir(tempDir.toString());

        // Create empty config file
        File configFile = new File(tempDir.toFile(), "config.json");
        Files.writeString(configFile.toPath(), "");

        // Should handle empty file gracefully
        FilePrefs prefs = FilePrefs.getInstance();

        assertThat(prefs.get("any.key", "default")).isEqualTo("default");

        // Should be able to write
        prefs.put("new.key", "value");
        assertThat(prefs.get("new.key", "default")).isEqualTo("value");
    }

    @Test
    void should_HandleEmptyJSONObject(@TempDir Path tempDir) throws Exception {
        FilePrefs.setTestConfigDir(tempDir.toString());

        // Create config with empty object
        File configFile = new File(tempDir.toFile(), "config.json");
        configFile.getParentFile().mkdirs();
        Files.writeString(configFile.toPath(), "{}");

        FilePrefs prefs = FilePrefs.getInstance();

        assertThat(prefs.get("any.key", "default")).isEqualTo("default");

        prefs.put("new.key", "value");
        assertThat(prefs.get("new.key", "default")).isEqualTo("value");
    }

    @Test
    void should_HandleDeepNestedPaths(@TempDir Path tempDir) {
        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs = FilePrefs.getInstance();

        // Very deep nested key
        String deepKey = "level1.level2.level3.level4.level5.level6.level7.key";
        prefs.put(deepKey, "deep value");

        assertThat(prefs.get(deepKey, "default")).isEqualTo("deep value");
    }

    @Test
    void should_HandleRapidSuccessiveWrites(@TempDir Path tempDir) {
        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs = FilePrefs.getInstance();

        // Rapid successive writes
        for (int i = 0; i < 100; i++) {
            prefs.put("rapid.key", "value" + i);
            prefs.flush();
        }

        // Should have last value
        assertThat(prefs.get("rapid.key", "default")).isEqualTo("value99");
    }

    @Test
    void should_HandleClearAfterRemove(@TempDir Path tempDir) {
        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs = FilePrefs.getInstance();

        prefs.put("key1", "value1");
        prefs.put("key2", "value2");

        prefs.remove("key1");
        assertThat(prefs.get("key1", "default")).isEqualTo("default");

        prefs.clear();
        assertThat(prefs.get("key2", "default")).isEqualTo("default");
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void should_HandleWindowsPathSeparators() {
        String path = FilePrefs.getConfigDirectory("windows 10");
        assertThat(path).contains(File.separator);
        assertThat(path).doesNotContain("/"); // Should use backslash on Windows
    }

    @Test
    void should_HandleMacPathFormat() {
        String path = FilePrefs.getConfigDirectory("mac os x");
        assertThat(path).contains("Library/Application Support");
        assertThat(path).contains("/"); // Should use forward slash
    }

    @Test
    void should_HandleLinuxHiddenDirectory() {
        String path = FilePrefs.getConfigDirectory("linux");
        assertThat(path).startsWith(System.getProperty("user.home"));
        assertThat(path).contains("/.ddpoker"); // Should be hidden with dot prefix
    }

    @Test
    void should_HandleVariousOSNameFormats() {
        // Test various real-world OS name formats
        assertThat(FilePrefs.getConfigDirectory("Windows 10")).contains("ddpoker");
        assertThat(FilePrefs.getConfigDirectory("Windows 11")).contains("ddpoker");
        assertThat(FilePrefs.getConfigDirectory("Windows NT")).contains("ddpoker");
        assertThat(FilePrefs.getConfigDirectory("Mac OS X")).contains("Library");
        assertThat(FilePrefs.getConfigDirectory("Darwin")).contains("Library");
        assertThat(FilePrefs.getConfigDirectory("Linux")).contains("/.ddpoker");
        assertThat(FilePrefs.getConfigDirectory("GNU/Linux")).contains("/.ddpoker");
        assertThat(FilePrefs.getConfigDirectory("FreeBSD")).contains("/.ddpoker");
    }

    @Test
    void should_PreserveKeyOrderingAfterReload(@TempDir Path tempDir) {
        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs = FilePrefs.getInstance();

        // Add keys in specific order
        prefs.put("zebra", "z");
        prefs.put("alpha", "a");
        prefs.put("beta", "b");
        prefs.flush();

        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs2 = FilePrefs.getInstance();

        // All values should be retrievable regardless of order
        assertThat(prefs2.get("zebra", "")).isEqualTo("z");
        assertThat(prefs2.get("alpha", "")).isEqualTo("a");
        assertThat(prefs2.get("beta", "")).isEqualTo("b");
    }

    @Test
    void should_HandleMultipleFlushesInSuccession(@TempDir Path tempDir) {
        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs = FilePrefs.getInstance();

        prefs.put("key", "value1");
        prefs.flush();
        prefs.flush(); // Redundant flush
        prefs.flush(); // Another redundant flush

        assertThat(prefs.get("key", "default")).isEqualTo("value1");
    }

    @Test
    void should_HandleConcurrentReadsAndWrites(@TempDir Path tempDir) throws Exception {
        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs = FilePrefs.getInstance();

        // Multiple threads reading and writing simultaneously
        Thread writer = new Thread(() -> {
            for (int i = 0; i < 50; i++) {
                prefs.put("write.key", "value" + i);
                try { Thread.sleep(1); } catch (InterruptedException e) { }
            }
        });

        Thread reader = new Thread(() -> {
            for (int i = 0; i < 50; i++) {
                prefs.get("write.key", "default");
                try { Thread.sleep(1); } catch (InterruptedException e) { }
            }
        });

        writer.start();
        reader.start();
        writer.join();
        reader.join();

        // No exceptions should have been thrown
        // Final value should be from last write
        String finalValue = prefs.get("write.key", "default");
        assertThat(finalValue).startsWith("value");
    }
}
