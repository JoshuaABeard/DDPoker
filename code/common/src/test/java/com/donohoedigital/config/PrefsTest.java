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
 * Tests for Prefs facade - verifies FilePrefs integration
 */
class PrefsTest {

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        // Reset FilePrefs for each test
        FilePrefs.setTestConfigDir(tempDir.toString());
        Prefs.initialize();
    }

    @Test
    void should_InitializeFilePrefs() {
        // Initialization should succeed without errors
        Prefs.initialize();
    }

    @Test
    void should_ReturnUserRootPrefs() {
        Preferences prefs = Prefs.getUserRootPrefs();

        assertThat(prefs).isNotNull();
        assertThat(prefs).isInstanceOf(Preferences.class);
    }

    @Test
    void should_ReturnUserPrefsForNode() {
        Preferences prefs = Prefs.getUserPrefs(Prefs.NODE_OPTIONS + "poker");

        assertThat(prefs).isNotNull();
        assertThat(prefs).isInstanceOf(Preferences.class);
    }

    @Test
    void should_StoreAndRetrieveValues() {
        Preferences prefs = Prefs.getUserPrefs(Prefs.NODE_OPTIONS + "poker");

        // Store values
        prefs.put("player.name", "TestPlayer");
        prefs.putBoolean("enabled", true);
        prefs.putInt("chips", 1000);

        // Retrieve values
        assertThat(prefs.get("player.name", "default")).isEqualTo("TestPlayer");
        assertThat(prefs.getBoolean("enabled", false)).isTrue();
        assertThat(prefs.getInt("chips", 0)).isEqualTo(1000);
    }

    @Test
    void should_ClearAllPreferences() {
        Preferences prefs = Prefs.getUserPrefs(Prefs.NODE_OPTIONS + "poker");

        // Store a value
        prefs.put("test.key", "test value");
        assertThat(prefs.get("test.key", "default")).isEqualTo("test value");

        // Clear all
        Prefs.clearAll();

        // Value should be gone
        assertThat(prefs.get("test.key", "default")).isEqualTo("default");
    }

    @Test
    void should_AutoInitialize_When_NotExplicitlyInitialized(@TempDir Path tempDir) {
        // Reset without explicit initialization
        FilePrefs.setTestConfigDir(tempDir.toString());

        // Should auto-initialize on first use
        Preferences prefs = Prefs.getUserPrefs(Prefs.NODE_OPTIONS + "test");

        prefs.put("auto.key", "auto value");
        assertThat(prefs.get("auto.key", "default")).isEqualTo("auto value");
    }

    @Test
    void should_SupportNestedNodes() {
        Preferences poker = Prefs.getUserPrefs(Prefs.NODE_OPTIONS + "poker");
        Preferences general = Prefs.getUserPrefs(Prefs.NODE_OPTIONS + "general");

        // Store in different nodes
        poker.put("game.type", "tournament");
        general.put("sound.volume", "80");

        // Retrieve from different nodes
        assertThat(poker.get("game.type", "default")).isEqualTo("tournament");
        assertThat(general.get("sound.volume", "default")).isEqualTo("80");
    }

    @Test
    void should_MaintainBackwardCompatibility() {
        // Test the exact API that existing code uses
        Preferences prefs = Prefs.getUserPrefs(Prefs.NODE_OPTIONS + "poker");

        // This is how DDOption classes use Prefs
        String key = "test.option";
        String value = "test value";

        prefs.put(key, value);
        String retrieved = prefs.get(key, null);

        assertThat(retrieved).isEqualTo(value);
    }
}
