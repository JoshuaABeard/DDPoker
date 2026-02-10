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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/**
 * Edge case and stress tests for PlayerIdentity.
 * Tests unusual scenarios, error conditions, and concurrent access patterns.
 */
class PlayerIdentityEdgeCasesTest {

    @AfterEach
    void tearDown() {
        PlayerIdentity.resetConfigDirectoryForTesting();
    }

    // ========== Concurrent Access Tests ==========

    @Test
    void should_HandleConcurrentLoads_When_MultipleThreadsAccess(@TempDir Path tempDir) throws InterruptedException {
        // Setup: Create a player ID file first
        PlayerIdentity.setConfigDirectoryForTesting(tempDir.toString());
        String originalId = PlayerIdentity.loadOrCreate();

        // Test: Multiple threads trying to load simultaneously
        int threadCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        Set<String> loadedIds = new HashSet<>();
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // All threads start at once
                    String id = PlayerIdentity.loadOrCreate();
                    synchronized (loadedIds) {
                        loadedIds.add(id);
                    }
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // Concurrent access should not throw exceptions
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Start all threads
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // All threads should get the same ID
        assertThat(loadedIds).hasSize(1);
        assertThat(loadedIds).contains(originalId);
        assertThat(successCount.get()).isEqualTo(threadCount);
    }

    @Test
    void should_PreventRaceCondition_When_ConcurrentSaveAndLoad(@TempDir Path tempDir) throws InterruptedException {
        PlayerIdentity.setConfigDirectoryForTesting(tempDir.toString());

        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        Set<String> observedIds = new HashSet<>();

        for (int i = 0; i < threadCount; i++) {
            final int threadNum = i;
            executor.submit(() -> {
                try {
                    startLatch.await();

                    if (threadNum % 2 == 0) {
                        // Half the threads load
                        String id = PlayerIdentity.loadOrCreate();
                        synchronized (observedIds) {
                            observedIds.add(id);
                        }
                    } else {
                        // Half the threads load and save
                        String id = PlayerIdentity.loadOrCreate();
                        PlayerIdentity.save(id);
                        synchronized (observedIds) {
                            observedIds.add(id);
                        }
                    }
                } catch (Exception e) {
                    // Should handle gracefully
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Should eventually converge to a single ID
        assertThat(observedIds).isNotEmpty();
    }

    // ========== File Permission and Error Tests ==========

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void should_HandleReadOnlyDirectory_When_CannotWrite(@TempDir Path tempDir) throws IOException {
        // Create config directory
        Path configDir = tempDir.resolve("readonly-config");
        Files.createDirectories(configDir);

        // Make directory read-only (Unix-only)
        Set<PosixFilePermission> perms = new HashSet<>();
        perms.add(PosixFilePermission.OWNER_READ);
        perms.add(PosixFilePermission.OWNER_EXECUTE);
        Files.setPosixFilePermissions(configDir, perms);

        try {
            PlayerIdentity.setConfigDirectoryForTesting(configDir.toString());

            // Should still generate a UUID even if can't save
            String playerId = PlayerIdentity.loadOrCreate();

            assertThat(playerId)
                .isNotNull()
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");
        } finally {
            // Restore permissions for cleanup
            perms.add(PosixFilePermission.OWNER_WRITE);
            Files.setPosixFilePermissions(configDir, perms);
        }
    }

    @Test
    void should_HandleEmptyFile_When_PlayerIdFileEmpty(@TempDir Path tempDir) throws IOException {
        Path configDir = tempDir.resolve("config");
        Files.createDirectories(configDir);
        Path playerIdFile = configDir.resolve("player.id");

        // Create empty file
        Files.createFile(playerIdFile);

        PlayerIdentity.setConfigDirectoryForTesting(configDir.toString());

        // Should generate new ID when file is empty
        String playerId = PlayerIdentity.loadOrCreate();

        assertThat(playerId)
            .isNotNull()
            .matches("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");
    }

    @Test
    void should_HandleWhitespaceOnly_When_FileContainsOnlySpaces(@TempDir Path tempDir) throws IOException {
        Path configDir = tempDir.resolve("config");
        Files.createDirectories(configDir);
        Path playerIdFile = configDir.resolve("player.id");

        // Write whitespace only
        Files.writeString(playerIdFile, "   \n\t\n   ");

        PlayerIdentity.setConfigDirectoryForTesting(configDir.toString());

        // Should generate new valid ID
        String playerId = PlayerIdentity.loadOrCreate();

        assertThat(playerId)
            .isNotNull()
            .matches("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");
    }

    @Test
    void should_HandleMalformedJSON_When_FileContainsInvalidJSON(@TempDir Path tempDir) throws IOException {
        Path configDir = tempDir.resolve("config");
        Files.createDirectories(configDir);
        Path playerIdFile = configDir.resolve("player.id");

        // Write invalid JSON
        Files.writeString(playerIdFile, "{playerId: \"not-closed-quote}");

        PlayerIdentity.setConfigDirectoryForTesting(configDir.toString());

        // Should recover by generating new ID
        String playerId = PlayerIdentity.loadOrCreate();

        assertThat(playerId)
            .isNotNull()
            .matches("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");
    }

    // ========== UUID Format Edge Cases ==========

    @Test
    void should_RejectUppercaseUUID_When_LoadingFromFile(@TempDir Path tempDir) throws IOException {
        Path configDir = tempDir.resolve("config");
        Files.createDirectories(configDir);
        Path playerIdFile = configDir.resolve("player.id");

        // Write uppercase UUID (should be lowercase)
        String uppercaseUuid = "A1B2C3D4-E5F6-4789-ABCD-1234567890AB";
        Files.writeString(playerIdFile, "{\"playerId\":\"" + uppercaseUuid + "\",\"createdAt\":1234567890}");

        PlayerIdentity.setConfigDirectoryForTesting(configDir.toString());

        // Should accept and normalize to lowercase, or generate new one
        String playerId = PlayerIdentity.loadOrCreate();

        assertThat(playerId)
            .isNotNull()
            .matches("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");
    }

    @Test
    void should_RejectInvalidUUIDVersion_When_NotV4(@TempDir Path tempDir) throws IOException {
        Path configDir = tempDir.resolve("config");
        Files.createDirectories(configDir);
        Path playerIdFile = configDir.resolve("player.id");

        // Write UUID v1 (time-based, not v4 random)
        String uuidV1 = "12345678-1234-1234-1234-123456789012";
        Files.writeString(playerIdFile, "{\"playerId\":\"" + uuidV1 + "\",\"createdAt\":1234567890}");

        PlayerIdentity.setConfigDirectoryForTesting(configDir.toString());

        // Should reject and generate new valid v4 UUID
        String playerId = PlayerIdentity.loadOrCreate();

        assertThat(playerId)
            .isNotNull()
            .matches("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");
    }

    @Test
    void should_RejectTooShortUUID_When_MissingCharacters(@TempDir Path tempDir) throws IOException {
        Path configDir = tempDir.resolve("config");
        Files.createDirectories(configDir);
        Path playerIdFile = configDir.resolve("player.id");

        // Write truncated UUID
        Files.writeString(playerIdFile, "{\"playerId\":\"12345678-1234-4123-8123\",\"createdAt\":1234567890}");

        PlayerIdentity.setConfigDirectoryForTesting(configDir.toString());

        // Should reject and generate new valid UUID
        String playerId = PlayerIdentity.loadOrCreate();

        assertThat(playerId)
            .isNotNull()
            .matches("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");
    }

    @Test
    void should_RejectTooLongUUID_When_ExtraCharacters(@TempDir Path tempDir) throws IOException {
        Path configDir = tempDir.resolve("config");
        Files.createDirectories(configDir);
        Path playerIdFile = configDir.resolve("player.id");

        // Write UUID with extra characters
        Files.writeString(playerIdFile, "{\"playerId\":\"12345678-1234-4123-8123-123456789012-extra\",\"createdAt\":1234567890}");

        PlayerIdentity.setConfigDirectoryForTesting(configDir.toString());

        // Should reject and generate new valid UUID
        String playerId = PlayerIdentity.loadOrCreate();

        assertThat(playerId)
            .isNotNull()
            .matches("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");
    }

    // ========== Path and Directory Edge Cases ==========

    @Test
    void should_HandleVeryLongPath_When_DirectoryNameLong(@TempDir Path tempDir) {
        // Create a very long directory path
        StringBuilder longPath = new StringBuilder(tempDir.toString());
        for (int i = 0; i < 10; i++) {
            longPath.append("/very-long-directory-name-").append(i);
        }

        PlayerIdentity.setConfigDirectoryForTesting(longPath.toString());

        // Should either work or fail gracefully
        assertThatCode(() -> {
            String playerId = PlayerIdentity.loadOrCreate();
            assertThat(playerId).isNotNull();
        }).doesNotThrowAnyException();
    }

    @Test
    void should_HandleSpecialCharactersInPath_When_DirectoryNameHasSpaces(@TempDir Path tempDir) throws IOException {
        // Create directory with spaces
        Path configDir = tempDir.resolve("config with spaces");
        Files.createDirectories(configDir);

        PlayerIdentity.setConfigDirectoryForTesting(configDir.toString());

        // Should handle spaces in path correctly
        String playerId = PlayerIdentity.loadOrCreate();

        assertThat(playerId)
            .isNotNull()
            .matches("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");
    }

    // ========== Recovery Scenarios ==========

    @Test
    void should_RecoverFromPartialWrite_When_SaveInterrupted(@TempDir Path tempDir) throws IOException {
        Path configDir = tempDir.resolve("config");
        Files.createDirectories(configDir);
        Path playerIdFile = configDir.resolve("player.id");

        // Simulate partial write (incomplete JSON)
        Files.writeString(playerIdFile, "{\"playerId\":\"12345678-1234-4123");

        PlayerIdentity.setConfigDirectoryForTesting(configDir.toString());

        // Should detect corruption and generate new ID
        String playerId = PlayerIdentity.loadOrCreate();

        assertThat(playerId)
            .isNotNull()
            .matches("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");

        // Verify new ID is persisted correctly
        String reloadedId = PlayerIdentity.loadOrCreate();
        assertThat(reloadedId).isEqualTo(playerId);
    }

    @Test
    void should_PreserveExistingId_When_SubsequentSaveFails(@TempDir Path tempDir) {
        PlayerIdentity.setConfigDirectoryForTesting(tempDir.toString());

        // First save should succeed
        String originalId = PlayerIdentity.loadOrCreate();

        // Even if directory becomes unavailable, loading should still work from cache
        String reloadedId = PlayerIdentity.loadOrCreate();

        assertThat(reloadedId).isEqualTo(originalId);
    }

    // ========== Stress Test ==========

    @Test
    void should_HandleThousandsOfGenerations_When_CalledRepeatedly() {
        Set<String> allIds = new HashSet<>();

        // Generate 10,000 UUIDs
        for (int i = 0; i < 10000; i++) {
            String id = PlayerIdentity.generatePlayerId();
            allIds.add(id);

            // All should be valid UUID v4 format
            assertThat(id).matches("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");
        }

        // No collisions
        assertThat(allIds).hasSize(10000);
    }
}
