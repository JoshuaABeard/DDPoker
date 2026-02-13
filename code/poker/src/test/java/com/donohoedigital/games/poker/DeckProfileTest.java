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
package com.donohoedigital.games.poker;

import com.donohoedigital.config.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.*;

import java.io.*;
import java.nio.file.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for DeckProfile - card deck profile management for custom deck images.
 */
class DeckProfileTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);
    }

    // ========== Constructor Tests ==========

    @Test
    void should_CreateEmptyDeckProfile() {
        DeckProfile profile = new DeckProfile();

        assertThat(profile).isNotNull();
    }

    @Test
    void should_CreateDeckProfile_FromFile() throws IOException {
        File testFile = tempDir.resolve("card-test.png").toFile();
        testFile.createNewFile();

        DeckProfile profile = new DeckProfile(testFile, false);

        assertThat(profile.getFile()).isEqualTo(testFile);
    }

    @Test
    void should_CreateDeckProfile_FromCopy() throws IOException {
        File testFile = tempDir.resolve("card-original.gif").toFile();
        testFile.createNewFile();
        DeckProfile original = new DeckProfile(testFile, false);

        DeckProfile copy = new DeckProfile(original);

        assertThat(copy.getFile()).isEqualTo(original.getFile());
        assertThat(copy.getName()).isEqualTo(original.getName());
        assertThat(copy.getFileName()).isEqualTo(original.getFileName());
    }

    // ========== setFile() Name Parsing Tests ==========

    @Test
    void should_ParseName_FromFileName() throws IOException {
        File testFile = tempDir.resolve("mydeck.png").toFile();
        testFile.createNewFile();
        DeckProfile profile = new DeckProfile();

        profile.setFile(testFile);

        assertThat(profile.getName()).isEqualTo("mydeck");
        assertThat(profile.getFileName()).isEqualTo("mydeck.png");
    }

    @Test
    void should_RemoveCardPrefix_WhenParsing() throws IOException {
        File testFile = tempDir.resolve("card-default.jpg").toFile();
        testFile.createNewFile();
        DeckProfile profile = new DeckProfile();

        profile.setFile(testFile);

        assertThat(profile.getName()).isEqualTo("default");
        assertThat(profile.getFileName()).isEqualTo("card-default.jpg");
    }

    @Test
    void should_HandleMultipleDots_InFileName() throws IOException {
        File testFile = tempDir.resolve("my.custom.deck.png").toFile();
        testFile.createNewFile();
        DeckProfile profile = new DeckProfile();

        profile.setFile(testFile);

        assertThat(profile.getName()).isEqualTo("my.custom.deck");
    }

    @Test
    void should_RemoveCardPrefix_WithMultipleDots() throws IOException {
        File testFile = tempDir.resolve("card-my.deck.gif").toFile();
        testFile.createNewFile();
        DeckProfile profile = new DeckProfile();

        profile.setFile(testFile);

        assertThat(profile.getName()).isEqualTo("my.deck");
    }

    // ========== canDelete() Tests ==========

    @Test
    void should_AllowDelete_ForCustomDeck() throws IOException {
        File testFile = tempDir.resolve("mycustom.png").toFile();
        testFile.createNewFile();
        DeckProfile profile = new DeckProfile(testFile, false);

        boolean canDelete = profile.canDelete();

        assertThat(canDelete).isTrue();
    }

    @Test
    void should_PreventDelete_ForBuiltInDeck() throws IOException {
        File testFile = tempDir.resolve("card-default.png").toFile();
        testFile.createNewFile();
        DeckProfile profile = new DeckProfile(testFile, false);

        boolean canDelete = profile.canDelete();

        assertThat(canDelete).isFalse();
    }

    @Test
    void should_PreventDelete_ForAnyCardPrefixedDeck() throws IOException {
        File testFile = tempDir.resolve("card-something.jpg").toFile();
        testFile.createNewFile();
        DeckProfile profile = new DeckProfile(testFile, false);

        assertThat(profile.canDelete()).isFalse();
    }

    // ========== save() Tests ==========

    @Test
    void should_NotSave_WhenCalled() throws IOException {
        File testFile = tempDir.resolve("test.png").toFile();
        testFile.createNewFile();
        DeckProfile profile = new DeckProfile(testFile, false);

        // save() is a no-op for DeckProfile
        assertThatCode(() -> profile.save()).doesNotThrowAnyException();
    }

    // ========== getBegin() Tests ==========

    @Test
    void should_ReturnDeckBegin() {
        DeckProfile profile = new DeckProfile();

        assertThat(profile.getBegin()).isEqualTo("deck");
    }

    // ========== getProfileDirName() Tests ==========

    @Test
    void should_ReturnDecksDirName() {
        DeckProfile profile = new DeckProfile();

        assertThat(profile.getProfileDirName()).isEqualTo("decks");
    }

    // ========== DeckFilter (Swing FileFilter) Tests ==========

    @Test
    void should_AcceptDirectory_WithSwingFilter() {
        DeckProfile.DeckFilter filter = new DeckProfile.DeckFilter();
        File dir = tempDir.toFile();

        boolean accepted = filter.accept(dir);

        assertThat(accepted).isTrue();
    }

    @Test
    void should_AcceptValidImageFile_WithSwingFilter() throws IOException {
        File testFile = tempDir.resolve("deck.png").toFile();
        testFile.createNewFile();
        DeckProfile.DeckFilter filter = new DeckProfile.DeckFilter();

        boolean accepted = filter.accept(testFile);

        assertThat(accepted).isTrue();
    }

    @Test
    void should_RejectInvalidFile_WithSwingFilter() throws IOException {
        File testFile = tempDir.resolve("test.txt").toFile();
        testFile.createNewFile();
        DeckProfile.DeckFilter filter = new DeckProfile.DeckFilter();

        boolean accepted = filter.accept(testFile);

        assertThat(accepted).isFalse();
    }

    @Test
    void should_ProvideDescription_ForSwingFilter() {
        DeckProfile.DeckFilter filter = new DeckProfile.DeckFilter();

        String description = filter.getDescription();

        assertThat(description).isNotNull();
    }

    @Test
    void should_RejectTooLargeFile_WithSwingFilter() throws IOException {
        File testFile = tempDir.resolve("toobig.png").toFile();
        testFile.createNewFile();

        // Write 36KB of data (over the 35KB limit)
        try (FileOutputStream fos = new FileOutputStream(testFile)) {
            byte[] data = new byte[36000];
            fos.write(data);
        }

        DeckProfile.DeckFilter filter = new DeckProfile.DeckFilter();
        boolean accepted = filter.accept(testFile);

        assertThat(accepted).isFalse();
    }

    @Test
    void should_AcceptFileUnderSizeLimit_WithSwingFilter() throws IOException {
        File testFile = tempDir.resolve("justright.gif").toFile();
        testFile.createNewFile();

        // Write 34KB of data (under the 35KB limit)
        try (FileOutputStream fos = new FileOutputStream(testFile)) {
            byte[] data = new byte[34000];
            fos.write(data);
        }

        DeckProfile.DeckFilter filter = new DeckProfile.DeckFilter();
        boolean accepted = filter.accept(testFile);

        assertThat(accepted).isTrue();
    }

    // ========== Edge Case Tests ==========

    @Test
    void should_HandleShortFileName() throws IOException {
        File testFile = tempDir.resolve("a.png").toFile();
        testFile.createNewFile();
        DeckProfile profile = new DeckProfile();

        profile.setFile(testFile);

        assertThat(profile.getName()).isEqualTo("a");
    }

    @Test
    void should_HandleLongFileName() throws IOException {
        String longName = "verylongfilenamewithlotsofcharactersinit.png";
        File testFile = tempDir.resolve(longName).toFile();
        testFile.createNewFile();
        DeckProfile profile = new DeckProfile();

        profile.setFile(testFile);

        assertThat(profile.getName()).isEqualTo("verylongfilenamewithlotsofcharactersinit");
    }

    @Test
    void should_HandleCardPrefixWithNoSuffix() throws IOException {
        File testFile = tempDir.resolve("card-.png").toFile();
        testFile.createNewFile();
        DeckProfile profile = new DeckProfile();

        profile.setFile(testFile);

        assertThat(profile.getName()).isEmpty();
    }

    @Test
    void should_HandleMultipleExtensions() throws IOException {
        File testFile = tempDir.resolve("deck.tar.gz.png").toFile();
        testFile.createNewFile();
        DeckProfile profile = new DeckProfile();

        profile.setFile(testFile);

        assertThat(profile.getName()).isEqualTo("deck.tar.gz");
    }
}
