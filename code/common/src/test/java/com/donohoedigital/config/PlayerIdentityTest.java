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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD tests for PlayerIdentity - UUID-based player identification system.
 * Tests written BEFORE implementation (Red-Green-Refactor cycle).
 */
class PlayerIdentityTest {

    @AfterEach
    void tearDown() {
        // Reset config directory override after each test to prevent cross-test pollution
        PlayerIdentity.resetConfigDirectoryForTesting();
    }

    // ========== Cycle 1: UUID Generation ==========

    @Test
    void should_GenerateValidUUIDv4_When_GeneratePlayerIdCalled() {
        // RED: This test will fail - PlayerIdentity doesn't exist yet
        String playerId = PlayerIdentity.generatePlayerId();

        // UUID v4 format: xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx
        // where y is 8, 9, a, or b
        assertThat(playerId)
            .matches("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");
    }

    @Test
    void should_GenerateUniqueIds_When_CalledMultipleTimes() {
        // Ensure no collisions in 1000 generated UUIDs
        Set<String> ids = new HashSet<>();

        for (int i = 0; i < 1000; i++) {
            ids.add(PlayerIdentity.generatePlayerId());
        }

        assertThat(ids).hasSize(1000);
    }

    @Test
    void should_NotBeNull_When_GeneratePlayerIdCalled() {
        String playerId = PlayerIdentity.generatePlayerId();

        assertThat(playerId).isNotNull();
    }

    // ========== Cycle 2: Platform Detection ==========

    @ParameterizedTest
    @ValueSource(strings = {"windows 10", "windows 11", "windows server 2022", "Windows NT"})
    void should_DetectWindowsConfigDirectory_When_RunningOnWindows(String osName) {
        // Test with mocked OS property
        String originalOs = System.getProperty("os.name");
        try {
            System.setProperty("os.name", osName);

            String configDir = PlayerIdentity.getConfigDirectory();

            // Windows: %APPDATA%\ddpoker
            assertThat(configDir)
                .contains("ddpoker")
                .doesNotStartWith(".");  // Not hidden directory
        } finally {
            System.setProperty("os.name", originalOs);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"Mac OS X", "Darwin", "mac os x"})
    void should_UseMacOSPath_When_RunningOnMacOS(String osName) {
        String originalOs = System.getProperty("os.name");
        try {
            System.setProperty("os.name", osName);

            String configDir = PlayerIdentity.getConfigDirectory();

            // macOS: ~/Library/Application Support/ddpoker
            assertThat(configDir)
                .contains("/Library/Application Support/ddpoker");
        } finally {
            System.setProperty("os.name", originalOs);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"Linux", "linux", "FreeBSD", "unix"})
    void should_UseLinuxHiddenPath_When_RunningOnLinux(String osName) {
        String originalOs = System.getProperty("os.name");
        try {
            System.setProperty("os.name", osName);

            String configDir = PlayerIdentity.getConfigDirectory();

            // Linux: ~/.ddpoker (hidden directory)
            assertThat(configDir)
                .endsWith("/.ddpoker");
        } finally {
            System.setProperty("os.name", originalOs);
        }
    }

    // ========== Cycle 3: Save/Load ==========

    @Test
    void should_SavePlayerIdToFile_When_SaveCalled(@TempDir Path tempDir) throws IOException {
        // Override config directory for testing
        PlayerIdentity.setConfigDirectoryForTesting(tempDir.toString());

        String playerId = "550e8400-e29b-41d4-a716-446655440000";

        PlayerIdentity.save(playerId);

        // Verify file exists
        Path playerFile = tempDir.resolve("player.json");
        assertThat(playerFile).exists();

        // Verify JSON content
        String content = Files.readString(playerFile);
        assertThat(content)
            .contains("\"playerId\"")
            .contains("550e8400-e29b-41d4-a716-446655440000")
            .contains("\"createdAt\"");
    }

    @Test
    void should_LoadPlayerIdFromFile_When_FileExists(@TempDir Path tempDir) {
        PlayerIdentity.setConfigDirectoryForTesting(tempDir.toString());

        // Save a player ID
        String originalId = "550e8400-e29b-41d4-a716-446655440000";
        PlayerIdentity.save(originalId);

        // Load it back
        String loadedId = PlayerIdentity.loadOrCreate();

        assertThat(loadedId).isEqualTo(originalId);
    }

    @Test
    void should_GenerateNewId_When_FileDoesNotExist(@TempDir Path tempDir) {
        PlayerIdentity.setConfigDirectoryForTesting(tempDir.toString());

        // No file exists, should generate new UUID
        String playerId = PlayerIdentity.loadOrCreate();

        assertThat(playerId).isNotNull();
        assertThat(playerId).matches("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");

        // Verify file was created
        Path playerFile = tempDir.resolve("player.json");
        assertThat(playerFile).exists();
    }

    @Test
    void should_GenerateNewId_When_FileCorrupted(@TempDir Path tempDir) throws IOException {
        PlayerIdentity.setConfigDirectoryForTesting(tempDir.toString());

        // Create corrupted player.json
        Path playerFile = tempDir.resolve("player.json");
        Files.writeString(playerFile, "invalid json {{{");

        // Should recover by generating new UUID
        String playerId = PlayerIdentity.loadOrCreate();

        assertThat(playerId).isNotNull();
        assertThat(playerId).matches("[0-9a-f]{8}-.*");

        // File should now have valid JSON
        String content = Files.readString(playerFile);
        assertThat(content).contains("\"playerId\"");
    }

    @Test
    void should_GenerateNewId_When_PlayerIdMissing(@TempDir Path tempDir) throws IOException {
        PlayerIdentity.setConfigDirectoryForTesting(tempDir.toString());

        // Create JSON file without playerId field
        Path playerFile = tempDir.resolve("player.json");
        Files.writeString(playerFile, "{\"someOtherField\": \"value\"}");

        // Should generate new UUID
        String playerId = PlayerIdentity.loadOrCreate();

        assertThat(playerId).isNotNull();
        assertThat(playerId).matches("[0-9a-f]{8}-.*");
    }

    @Test
    void should_GenerateNewId_When_PlayerIdInvalid(@TempDir Path tempDir) throws IOException {
        PlayerIdentity.setConfigDirectoryForTesting(tempDir.toString());

        // Create JSON with invalid UUID
        Path playerFile = tempDir.resolve("player.json");
        Files.writeString(playerFile, "{\"playerId\": \"not-a-valid-uuid\"}");

        // Should generate new valid UUID
        String playerId = PlayerIdentity.loadOrCreate();

        assertThat(playerId).isNotNull();
        assertThat(playerId).matches("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");
    }

    @Test
    void should_CreateDirectoryAutomatically_When_Saving(@TempDir Path tempDir) throws IOException {
        // Use a subdirectory that doesn't exist yet
        Path configDir = tempDir.resolve("subdir/nested");
        PlayerIdentity.setConfigDirectoryForTesting(configDir.toString());

        String playerId = "550e8400-e29b-41d4-a716-446655440000";
        PlayerIdentity.save(playerId);

        // Directory should be created
        assertThat(configDir).exists();
        assertThat(configDir.resolve("player.json")).exists();
    }

    @Test
    void should_ReturnSameId_When_LoadedMultipleTimes(@TempDir Path tempDir) {
        PlayerIdentity.setConfigDirectoryForTesting(tempDir.toString());

        // Save once
        String originalId = "550e8400-e29b-41d4-a716-446655440000";
        PlayerIdentity.save(originalId);

        // Load multiple times
        String id1 = PlayerIdentity.loadOrCreate();
        String id2 = PlayerIdentity.loadOrCreate();
        String id3 = PlayerIdentity.loadOrCreate();

        assertThat(id1).isEqualTo(originalId);
        assertThat(id2).isEqualTo(originalId);
        assertThat(id3).isEqualTo(originalId);
    }

    // ========== UUID Validation ==========

    @Test
    void should_AcceptValidUUID_When_Loading(@TempDir Path tempDir) {
        PlayerIdentity.setConfigDirectoryForTesting(tempDir.toString());

        // Various valid UUID v4 formats
        String[] validUuids = {
            "550e8400-e29b-41d4-a716-446655440000",
            "7f3d8b92-4c21-4d88-9e3a-1b5c6d7e8f90",
            "a1b2c3d4-e5f6-4789-abcd-ef0123456789"
        };

        for (String uuid : validUuids) {
            PlayerIdentity.save(uuid);
            String loaded = PlayerIdentity.loadOrCreate();
            assertThat(loaded).isEqualTo(uuid);
        }
    }

    @Test
    void should_IncludeCreatedAtTimestamp_When_Saving(@TempDir Path tempDir) throws IOException {
        PlayerIdentity.setConfigDirectoryForTesting(tempDir.toString());

        String playerId = "550e8400-e29b-41d4-a716-446655440000";
        long beforeSave = System.currentTimeMillis();

        PlayerIdentity.save(playerId);

        long afterSave = System.currentTimeMillis();

        // Verify createdAt is present and reasonable
        Path playerFile = tempDir.resolve("player.json");
        String content = Files.readString(playerFile);
        assertThat(content).contains("\"createdAt\"");

        // Could parse and validate timestamp is between beforeSave and afterSave
        // but just checking it exists is sufficient for this test
    }
}
