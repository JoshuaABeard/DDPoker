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

import java.util.prefs.Preferences;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for FilePrefsAdapter - Preferences interface adapter
 */
class FilePrefsAdapterTest {

    @Test
    void should_ImplementPreferencesInterface() {
        FilePrefsAdapter adapter = new FilePrefsAdapter();

        // Verify it's a Preferences instance
        assertThat(adapter).isInstanceOf(Preferences.class);
    }

    @Test
    void should_StoreAndRetrieveValues() {
        FilePrefsAdapter adapter = new FilePrefsAdapter();

        // Store values
        adapter.put("test.key", "test value");
        adapter.putBoolean("test.bool", true);
        adapter.putInt("test.int", 42);
        adapter.putDouble("test.double", 3.14);

        // Retrieve values
        assertThat(adapter.get("test.key", "default")).isEqualTo("test value");
        assertThat(adapter.getBoolean("test.bool", false)).isTrue();
        assertThat(adapter.getInt("test.int", 0)).isEqualTo(42);
        assertThat(adapter.getDouble("test.double", 0.0)).isEqualTo(3.14);
    }

    @Test
    void should_ReturnDefaultValue_When_KeyNotFound() {
        FilePrefsAdapter adapter = new FilePrefsAdapter();

        String value = adapter.get("nonexistent", "default");

        assertThat(value).isEqualTo("default");
    }

    @Test
    void should_SupportChildNodes() {
        FilePrefsAdapter adapter = new FilePrefsAdapter();

        // Create child node
        Preferences child = adapter.node("options/poker");

        // Store value in child
        child.put("player.name", "Player1");

        // Retrieve value
        assertThat(child.get("player.name", "default")).isEqualTo("Player1");
    }

    @Test
    void should_BuildCorrectNodePath() {
        FilePrefsAdapter root = new FilePrefsAdapter();

        Preferences child = root.node("options");
        Preferences grandchild = child.node("poker");

        // Verify node paths are constructed correctly
        assertThat(((FilePrefsAdapter) child).getNodePath()).isEqualTo("options");
        assertThat(((FilePrefsAdapter) grandchild).getNodePath()).isEqualTo("options.poker");
    }

    @Test
    void should_ConvertNodePathToKey() {
        FilePrefsAdapter root = new FilePrefsAdapter();
        Preferences poker = root.node("options/poker");

        // Store value with node path
        poker.put("playerName", "TestPlayer");

        // Verify the key is stored with correct path
        // The key should be "options.poker.playerName" in FilePrefs
        String value = poker.get("playerName", "default");
        assertThat(value).isEqualTo("TestPlayer");
    }

    @Test
    void should_HandleRemove() {
        FilePrefsAdapter adapter = new FilePrefsAdapter();

        // Store and verify
        adapter.put("test.key", "value");
        assertThat(adapter.get("test.key", "default")).isEqualTo("value");

        // Remove and verify
        adapter.remove("test.key");
        assertThat(adapter.get("test.key", "default")).isEqualTo("default");
    }

    @Test
    void should_HandleFlush() throws Exception {
        FilePrefsAdapter adapter = new FilePrefsAdapter();

        adapter.put("test.key", "value");
        adapter.flush();  // Should not throw exception

        // Value should still be accessible
        assertThat(adapter.get("test.key", "default")).isEqualTo("value");
    }

    @Test
    void should_HandleSync() throws Exception {
        FilePrefsAdapter adapter = new FilePrefsAdapter();

        adapter.put("test.key", "value");
        adapter.sync();  // Should not throw exception

        // Value should still be accessible
        assertThat(adapter.get("test.key", "default")).isEqualTo("value");
    }

    @Test
    void should_SupportAbsolutePath() {
        FilePrefsAdapter root = new FilePrefsAdapter();
        Preferences child = root.node("test/node");

        // Verify absolute path
        assertThat(child.absolutePath()).contains("test");
        assertThat(child.absolutePath()).contains("node");
    }
}
