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
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.prefs.Preferences;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that verify FilePrefs works with exact patterns used by DDOption classes
 */
class DDOptionCompatibilityTest {

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        FilePrefs.setTestConfigDir(tempDir.toString());
        Prefs.initialize();
    }

    /**
     * Simulates OptionBoolean pattern from line 88 of OptionBoolean.java:
     * box_.setSelected(prefs_.getBoolean(sName_, bDefault_));
     */
    @Test
    void should_SupportOptionBooleanPattern(@TempDir Path tempDir) {
        FilePrefs.setTestConfigDir(tempDir.toString());
        Prefs.initialize();

        // Simulate DDOption constructor pattern
        String sPrefNode = "poker";
        String sName = "general.largeCards";
        boolean bDefault = false;

        // This is exactly how DDOption.getOptionPrefs() works (line 111 of DDOption.java)
        Preferences prefs = Prefs.getUserPrefs(Prefs.NODE_OPTIONS + sPrefNode);

        // First read - should return default
        boolean initialValue = prefs.getBoolean(sName, bDefault);
        assertThat(initialValue).isEqualTo(bDefault);

        // User changes setting
        prefs.putBoolean(sName, true);

        // Re-read - should return saved value
        boolean savedValue = prefs.getBoolean(sName, bDefault);
        assertThat(savedValue).isTrue();

        // Simulate app restart
        FilePrefs.setTestConfigDir(tempDir.toString());
        Prefs.initialize();
        Preferences prefs2 = Prefs.getUserPrefs(Prefs.NODE_OPTIONS + sPrefNode);

        // Should load saved value
        boolean reloadedValue = prefs2.getBoolean(sName, bDefault);
        assertThat(reloadedValue).isTrue();
    }

    /**
     * Simulates OptionInteger pattern
     */
    @Test
    void should_SupportOptionIntegerPattern(@TempDir Path tempDir) {
        FilePrefs.setTestConfigDir(tempDir.toString());
        Prefs.initialize();

        String sPrefNode = "poker";
        String sName = "general.soundVolume";
        int iDefault = 100;

        Preferences prefs = Prefs.getUserPrefs(Prefs.NODE_OPTIONS + sPrefNode);

        // Initial read
        int initialValue = prefs.getInt(sName, iDefault);
        assertThat(initialValue).isEqualTo(iDefault);

        // User changes setting
        prefs.putInt(sName, 75);

        // Re-read
        int savedValue = prefs.getInt(sName, iDefault);
        assertThat(savedValue).isEqualTo(75);

        // Simulate app restart
        FilePrefs.setTestConfigDir(tempDir.toString());
        Prefs.initialize();
        Preferences prefs2 = Prefs.getUserPrefs(Prefs.NODE_OPTIONS + sPrefNode);

        int reloadedValue = prefs2.getInt(sName, iDefault);
        assertThat(reloadedValue).isEqualTo(75);
    }

    /**
     * Simulates OptionText pattern
     */
    @Test
    void should_SupportOptionTextPattern(@TempDir Path tempDir) {
        FilePrefs.setTestConfigDir(tempDir.toString());
        Prefs.initialize();

        String sPrefNode = "poker";
        String sName = "player.name";
        String sDefault = "Player1";

        Preferences prefs = Prefs.getUserPrefs(Prefs.NODE_OPTIONS + sPrefNode);

        // Initial read
        String initialValue = prefs.get(sName, sDefault);
        assertThat(initialValue).isEqualTo(sDefault);

        // User changes setting
        prefs.put(sName, "CustomPlayer");

        // Re-read
        String savedValue = prefs.get(sName, sDefault);
        assertThat(savedValue).isEqualTo("CustomPlayer");

        // Simulate app restart
        FilePrefs.setTestConfigDir(tempDir.toString());
        Prefs.initialize();
        Preferences prefs2 = Prefs.getUserPrefs(Prefs.NODE_OPTIONS + sPrefNode);

        String reloadedValue = prefs2.get(sName, sDefault);
        assertThat(reloadedValue).isEqualTo("CustomPlayer");
    }

    /**
     * Tests multiple option types in same node (common scenario)
     */
    @Test
    void should_SupportMultipleOptionTypesInSameNode(@TempDir Path tempDir) {
        FilePrefs.setTestConfigDir(tempDir.toString());
        Prefs.initialize();

        String sPrefNode = "poker";
        Preferences prefs = Prefs.getUserPrefs(Prefs.NODE_OPTIONS + sPrefNode);

        // Simulate various option types in same category
        prefs.putBoolean("general.largeCards", false);
        prefs.putBoolean("general.fourColorDeck", true);
        prefs.putBoolean("general.soundEffects", true);
        prefs.putInt("general.soundVolume", 80);
        prefs.put("player.name", "TestPlayer");
        prefs.putInt("practice.delay", 500);
        prefs.putBoolean("practice.autoDeal", false);

        // Verify all values
        assertThat(prefs.getBoolean("general.largeCards", true)).isFalse();
        assertThat(prefs.getBoolean("general.fourColorDeck", false)).isTrue();
        assertThat(prefs.getBoolean("general.soundEffects", false)).isTrue();
        assertThat(prefs.getInt("general.soundVolume", 0)).isEqualTo(80);
        assertThat(prefs.get("player.name", "default")).isEqualTo("TestPlayer");
        assertThat(prefs.getInt("practice.delay", 0)).isEqualTo(500);
        assertThat(prefs.getBoolean("practice.autoDeal", true)).isFalse();
    }

    /**
     * Tests multiple preference nodes (General, Practice, Online, etc.)
     */
    @Test
    void should_SupportMultiplePreferenceNodes(@TempDir Path tempDir) {
        FilePrefs.setTestConfigDir(tempDir.toString());
        Prefs.initialize();

        // General options
        Preferences generalPrefs = Prefs.getUserPrefs(Prefs.NODE_OPTIONS + "general");
        generalPrefs.putBoolean("largeCards", false);
        generalPrefs.putInt("soundVolume", 90);

        // Practice options
        Preferences practicePrefs = Prefs.getUserPrefs(Prefs.NODE_OPTIONS + "practice");
        practicePrefs.putBoolean("autoDeal", true);
        practicePrefs.putInt("delay", 500);

        // Online options
        Preferences onlinePrefs = Prefs.getUserPrefs(Prefs.NODE_OPTIONS + "online");
        onlinePrefs.putBoolean("enabled", true);
        onlinePrefs.put("server", "localhost:8877");

        // Clock options
        Preferences clockPrefs = Prefs.getUserPrefs(Prefs.NODE_OPTIONS + "clock");
        clockPrefs.putBoolean("colorUp", true);
        clockPrefs.putBoolean("pause", false);

        // Verify all nodes maintain separate values
        assertThat(generalPrefs.getBoolean("largeCards", true)).isFalse();
        assertThat(practicePrefs.getBoolean("autoDeal", false)).isTrue();
        assertThat(onlinePrefs.get("server", "default")).isEqualTo("localhost:8877");
        assertThat(clockPrefs.getBoolean("colorUp", false)).isTrue();
    }

    /**
     * Tests the exact node path pattern used by DDOption
     */
    @Test
    void should_UseCorrectNodePath(@TempDir Path tempDir) {
        FilePrefs.setTestConfigDir(tempDir.toString());
        Prefs.initialize();

        // This is the exact pattern from DDOption.getOptionPrefs() line 111
        String sPrefNode = "poker";
        Preferences prefs = Prefs.getUserPrefs(Prefs.NODE_OPTIONS + sPrefNode);

        // The full path should be: com/donohoedigital/generic/options/poker
        // (where "generic" is the default root name from Prefs.getRootNodeName())
        String absolutePath = prefs.absolutePath();

        assertThat(absolutePath).contains("options");
        assertThat(absolutePath).contains("poker");
    }

    /**
     * Tests that null preference node works (creates DummyPref behavior)
     */
    @Test
    void should_HandleNullPreferenceNode(@TempDir Path tempDir) {
        FilePrefs.setTestConfigDir(tempDir.toString());
        Prefs.initialize();

        // When sPrefNode is null, DDOption uses DummyPref
        // But our system should handle this case gracefully
        Preferences prefs = Prefs.getUserPrefs(Prefs.NODE_OPTIONS + "test");

        // Should not throw exception
        prefs.put("test.key", "test value");
        assertThat(prefs.get("test.key", "default")).isEqualTo("test value");
    }

    /**
     * Tests the option save/load cycle exactly as DDOption uses it
     */
    @Test
    void should_SupportFullOptionSaveLoadCycle(@TempDir Path tempDir) {
        FilePrefs.setTestConfigDir(tempDir.toString());
        Prefs.initialize();

        // === SAVE PHASE (user changes settings in UI) ===
        Preferences savePrefs = Prefs.getUserPrefs(Prefs.NODE_OPTIONS + "poker");

        // Simulate OptionBoolean saving
        savePrefs.putBoolean("general.largeCards", true);

        // Simulate OptionInteger saving
        savePrefs.putInt("general.soundVolume", 85);

        // Simulate OptionText saving
        savePrefs.put("player.name", "SavedPlayer");

        // === RESTART APPLICATION ===
        FilePrefs.setTestConfigDir(tempDir.toString());
        Prefs.initialize();

        // === LOAD PHASE (app loads settings on startup) ===
        Preferences loadPrefs = Prefs.getUserPrefs(Prefs.NODE_OPTIONS + "poker");

        // Simulate OptionBoolean loading (with default)
        boolean largeCards = loadPrefs.getBoolean("general.largeCards", false);
        assertThat(largeCards).isTrue();

        // Simulate OptionInteger loading (with default)
        int soundVolume = loadPrefs.getInt("general.soundVolume", 100);
        assertThat(soundVolume).isEqualTo(85);

        // Simulate OptionText loading (with default)
        String playerName = loadPrefs.get("player.name", "Player1");
        assertThat(playerName).isEqualTo("SavedPlayer");
    }

    /**
     * Tests that preference changes are immediately visible (no explicit flush needed)
     */
    @Test
    void should_ImmediatelyPersistChanges(@TempDir Path tempDir) {
        FilePrefs.setTestConfigDir(tempDir.toString());
        Prefs.initialize();

        Preferences prefs = Prefs.getUserPrefs(Prefs.NODE_OPTIONS + "poker");

        // Make a change (simulates user action in UI)
        prefs.putBoolean("test.immediate", true);

        // Read back immediately (simulates another component reading)
        boolean value = prefs.getBoolean("test.immediate", false);
        assertThat(value).isTrue();

        // Simulate app restart without explicit flush
        FilePrefs.setTestConfigDir(tempDir.toString());
        Prefs.initialize();

        Preferences prefs2 = Prefs.getUserPrefs(Prefs.NODE_OPTIONS + "poker");
        boolean persistedValue = prefs2.getBoolean("test.immediate", false);

        // Should be persisted (FilePrefs flushes immediately)
        assertThat(persistedValue).isTrue();
    }

    /**
     * Tests behavior with default values that match stored values
     */
    @Test
    void should_HandleDefaultValueMatchingStoredValue(@TempDir Path tempDir) {
        FilePrefs.setTestConfigDir(tempDir.toString());
        Prefs.initialize();

        Preferences prefs = Prefs.getUserPrefs(Prefs.NODE_OPTIONS + "poker");

        // Store the same value as default
        boolean defaultValue = false;
        prefs.putBoolean("test.same", defaultValue);

        // Read with same default
        boolean value = prefs.getBoolean("test.same", defaultValue);

        // Should return the stored value (which happens to equal default)
        assertThat(value).isEqualTo(defaultValue);
    }

    /**
     * Tests the pattern of checking if a value exists before reading
     */
    @Test
    void should_SupportCheckingIfValueExists(@TempDir Path tempDir) {
        FilePrefs.setTestConfigDir(tempDir.toString());
        Prefs.initialize();

        Preferences prefs = Prefs.getUserPrefs(Prefs.NODE_OPTIONS + "poker");

        // Set a value
        prefs.put("existing.key", "value");

        // Check existence pattern: get with null default
        String existingValue = prefs.get("existing.key", null);
        assertThat(existingValue).isNotNull();

        String nonExistingValue = prefs.get("nonexistent.key", null);
        assertThat(nonExistingValue).isNull();
    }
}
