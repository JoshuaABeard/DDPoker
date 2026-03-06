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
package com.donohoedigital.games.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for GameButton - button definition parsing from colon-delimited
 * strings.
 */
class GameButtonTest {

    // ========== Parsing Tests ==========

    @Test
    void should_ParseNameOnly_When_NoColonPresent() {
        GameButton button = new GameButton("OK");

        assertThat(button.getName()).isEqualTo("OK");
        assertThat(button.getGotoPhase()).isNull();
        assertThat(button.getGenericParam()).isNull();
    }

    @Test
    void should_ParseNameAndPhase_When_OneColonPresent() {
        GameButton button = new GameButton("Next:GamePhase");

        assertThat(button.getName()).isEqualTo("Next");
        assertThat(button.getGotoPhase()).isEqualTo("GamePhase");
        assertThat(button.getGenericParam()).isNull();
    }

    @Test
    void should_ParseAllThreeParts_When_TwoColonsPresent() {
        GameButton button = new GameButton("Play:StartPhase:extraParam");

        assertThat(button.getName()).isEqualTo("Play");
        assertThat(button.getGotoPhase()).isEqualTo("StartPhase");
        assertThat(button.getGenericParam()).isEqualTo("extraParam");
    }

    // ========== isMatch Tests ==========

    @Test
    void should_ReturnTrue_When_DefinitionEqualsName() {
        assertThat(GameButton.isMatch("OK", "OK")).isTrue();
    }

    @Test
    void should_ReturnTrue_When_DefinitionStartsWithNameAndColon() {
        assertThat(GameButton.isMatch("Next:GamePhase", "Next")).isTrue();
    }

    @Test
    void should_ReturnFalse_When_NameDoesNotMatch() {
        assertThat(GameButton.isMatch("Cancel", "OK")).isFalse();
    }

    @Test
    void should_ReturnFalse_When_NameIsSubstringButNotMatch() {
        // "Next" starts with "Ne" but "Ne:" is not present
        assertThat(GameButton.isMatch("Next", "Ne")).isFalse();
    }

    // ========== toString Tests ==========

    @Test
    void should_IncludeNameAndPhase_When_ToStringCalled() {
        GameButton button = new GameButton("Play:StartPhase");

        String result = button.toString();

        assertThat(result).contains("Play");
        assertThat(result).contains("StartPhase");
    }

    @Test
    void should_ShowNullPhase_When_NameOnly() {
        GameButton button = new GameButton("OK");

        String result = button.toString();

        assertThat(result).contains("OK");
        assertThat(result).contains("null");
    }
}
