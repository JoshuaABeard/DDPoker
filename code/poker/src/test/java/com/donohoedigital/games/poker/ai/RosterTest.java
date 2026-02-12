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
package com.donohoedigital.games.poker.ai;

import com.donohoedigital.config.ApplicationType;
import com.donohoedigital.config.ConfigManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for Roster AI player name management.
 */
class RosterTest {
    @TempDir
    Path tempDir;

    private PlayerType playerType;
    private File rosterFile;

    @BeforeEach
    void setUp() {
        // Initialize ConfigManager for tests (only once)
        if (!com.donohoedigital.config.PropertyConfig.isInitialized()) {
            new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);
        }

        // Create PlayerType with a simple name
        playerType = new PlayerType("TestPlayer");

        // Mock the file by creating a temp .dat file and setting it directly
        File datFile = tempDir.resolve("test.dat").toFile();
        try {
            datFile.createNewFile();
            // Use reflection to set the file_ field
            java.lang.reflect.Field fileField = playerType.getClass().getSuperclass().getSuperclass()
                    .getDeclaredField("file_");
            fileField.setAccessible(true);
            fileField.set(playerType, datFile);

            // Set the fileName
            java.lang.reflect.Field fileNameField = playerType.getClass().getSuperclass()
                    .getDeclaredField("sFileName_");
            fileNameField.setAccessible(true);
            fileNameField.set(playerType, "test.dat");
        } catch (Exception e) {
            throw new RuntimeException("Failed to set up test", e);
        }

        // Get the roster file path (should be test.roster)
        rosterFile = new File(tempDir.toFile(), "test.roster");
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        // Force garbage collection to close file handles
        System.gc();
        Thread.sleep(100);

        // Clean up roster file if it exists
        if (rosterFile != null && rosterFile.exists()) {
            // Try multiple times in case file is locked
            for (int i = 0; i < 3; i++) {
                if (rosterFile.delete()) {
                    break;
                }
                Thread.sleep(50);
            }
        }
    }

    // ========================================
    // getRoster Tests
    // ========================================

    @Test
    void should_ReturnEmptyString_When_RosterFileDoesNotExist() {
        String roster = Roster.getRoster(playerType);

        assertThat(roster).isEmpty();
    }

    @Test
    void should_ReturnRosterContent_When_RosterFileExists() throws IOException {
        Files.write(rosterFile.toPath(), "Alice,Bob,Charlie".getBytes());

        String roster = Roster.getRoster(playerType);

        assertThat(roster).isEqualTo("Alice,Bob,Charlie");
    }

    @Test
    void should_TrimWhitespace_When_RosterFileHasTrailingNewlines() throws IOException {
        Files.write(rosterFile.toPath(), "Alice,Bob\n\n".getBytes());

        String roster = Roster.getRoster(playerType);

        assertThat(roster).isEqualTo("Alice,Bob");
    }

    @Test
    void should_HandleMultilineRoster_When_FileHasMultipleLines() throws IOException {
        Files.write(rosterFile.toPath(), "Alice,Bob\nCharlie,Dave".getBytes());

        String roster = Roster.getRoster(playerType);

        assertThat(roster).isEqualTo("Alice,Bob\nCharlie,Dave");
    }

    // ========================================
    // setRoster Tests
    // ========================================

    @Test
    void should_CreateRosterFile_When_SettingRoster() {
        Roster.setRoster(playerType, "Alice,Bob,Charlie");

        assertThat(rosterFile).exists();
    }

    @Test
    void should_WriteRosterContent_When_SettingRoster() throws IOException {
        Roster.setRoster(playerType, "Alice,Bob,Charlie");

        String content = new String(Files.readAllBytes(rosterFile.toPath()));
        assertThat(content).isEqualTo("Alice,Bob,Charlie");
    }

    @Test
    void should_OverwriteExistingRoster_When_SettingNewRoster() throws IOException {
        Files.write(rosterFile.toPath(), "OldNames".getBytes());

        Roster.setRoster(playerType, "NewNames");

        String content = new String(Files.readAllBytes(rosterFile.toPath()));
        assertThat(content).isEqualTo("NewNames");
    }

    @Test
    void should_WriteEmptyString_When_SettingEmptyRoster() throws IOException {
        Roster.setRoster(playerType, "");

        String content = new String(Files.readAllBytes(rosterFile.toPath()));
        assertThat(content).isEmpty();
    }

    // ========================================
    // getRosterNameList Tests
    // ========================================

    @Test
    void should_ReturnEmptyList_When_RosterFileDoesNotExist() {
        List<String> names = Roster.getRosterNameList(playerType);

        assertThat(names).isEmpty();
    }

    @Test
    void should_ParseNames_When_CommaSeparatedRoster() throws IOException {
        Files.write(rosterFile.toPath(), "Alice,Bob,Charlie".getBytes());

        List<String> names = Roster.getRosterNameList(playerType);

        assertThat(names).containsExactly("Alice", "Bob", "Charlie");
    }

    @Test
    void should_TrimWhitespace_When_NamesHaveSpaces() throws IOException {
        Files.write(rosterFile.toPath(), " Alice , Bob , Charlie ".getBytes());

        List<String> names = Roster.getRosterNameList(playerType);

        assertThat(names).containsExactly("Alice", "Bob", "Charlie");
    }

    @Test
    void should_HandleMultipleCommas_When_RosterHasExtraCommas() throws IOException {
        Files.write(rosterFile.toPath(), "Alice,,,Bob,,Charlie".getBytes());

        List<String> names = Roster.getRosterNameList(playerType);

        assertThat(names).containsExactly("Alice", "Bob", "Charlie");
    }

    @Test
    void should_HandleLeadingComma_When_RosterStartsWithComma() throws IOException {
        Files.write(rosterFile.toPath(), ",Alice,Bob".getBytes());

        List<String> names = Roster.getRosterNameList(playerType);

        assertThat(names).containsExactly("Alice", "Bob");
    }

    @Test
    void should_HandleTrailingComma_When_RosterEndsWithComma() throws IOException {
        Files.write(rosterFile.toPath(), "Alice,Bob,".getBytes());

        List<String> names = Roster.getRosterNameList(playerType);

        assertThat(names).containsExactly("Alice", "Bob");
    }

    @Test
    void should_RemoveDuplicates_When_RosterHasDuplicateNames() throws IOException {
        Files.write(rosterFile.toPath(), "Alice,Bob,Alice,Charlie,Bob".getBytes());

        List<String> names = Roster.getRosterNameList(playerType);

        assertThat(names).containsExactly("Alice", "Bob", "Charlie");
    }

    @Test
    void should_ReturnSingleName_When_RosterHasOneName() throws IOException {
        Files.write(rosterFile.toPath(), "Alice".getBytes());

        List<String> names = Roster.getRosterNameList(playerType);

        assertThat(names).containsExactly("Alice");
    }

    @Test
    void should_ReturnEmptyList_When_RosterIsOnlyCommas() throws IOException {
        Files.write(rosterFile.toPath(), ",,,".getBytes());

        List<String> names = Roster.getRosterNameList(playerType);

        assertThat(names).isEmpty();
    }

    @Test
    void should_ReturnEmptyList_When_RosterIsOnlyWhitespace() throws IOException {
        Files.write(rosterFile.toPath(), "   ".getBytes());

        List<String> names = Roster.getRosterNameList(playerType);

        assertThat(names).isEmpty();
    }

    // ========================================
    // Integration Tests
    // ========================================

    @Test
    void should_RoundTripSuccessfully_When_SettingAndGettingRoster() {
        String original = "Alice,Bob,Charlie";

        Roster.setRoster(playerType, original);
        String retrieved = Roster.getRoster(playerType);

        assertThat(retrieved).isEqualTo(original);
    }

    @Test
    void should_PreserveNameList_When_RoundTripThroughFileSystem() {
        Roster.setRoster(playerType, "Alice,Bob,Charlie");

        List<String> names = Roster.getRosterNameList(playerType);

        assertThat(names).containsExactly("Alice", "Bob", "Charlie");
    }

    @Test
    void should_HandleComplexNames_When_NamesHaveSpecialCharacters() throws IOException {
        Files.write(rosterFile.toPath(), "O'Brien,José,François".getBytes());

        List<String> names = Roster.getRosterNameList(playerType);

        assertThat(names).containsExactly("O'Brien", "José", "François");
    }
}
