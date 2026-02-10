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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for PlayStyle name wrapper class.
 */
class PlayStyleTest
{
    // ========================================
    // Constructor Tests
    // ========================================

    @Test
    void should_StoreNameCorrectly_When_ConstructorCalled()
    {
        PlayStyle style = new PlayStyle("Aggressive");

        assertThat(style.getName()).isEqualTo("Aggressive");
    }

    @Test
    void should_AcceptNullName_When_ConstructorCalled()
    {
        PlayStyle style = new PlayStyle(null);

        assertThat(style.getName()).isNull();
    }

    @Test
    void should_AcceptEmptyString_When_ConstructorCalled()
    {
        PlayStyle style = new PlayStyle("");

        assertThat(style.getName()).isEmpty();
    }

    // ========================================
    // Getter/Setter Tests
    // ========================================

    @Test
    void should_ReturnCorrectName_When_GetNameCalled()
    {
        PlayStyle style = new PlayStyle("Tight-Passive");

        assertThat(style.getName()).isEqualTo("Tight-Passive");
    }

    @Test
    void should_UpdateName_When_SetNameCalled()
    {
        PlayStyle style = new PlayStyle("Conservative");

        style.setName("Aggressive");

        assertThat(style.getName()).isEqualTo("Aggressive");
    }

    @Test
    void should_AllowNullName_When_SetNameCalled()
    {
        PlayStyle style = new PlayStyle("Initial");

        style.setName(null);

        assertThat(style.getName()).isNull();
    }

    @Test
    void should_AllowEmptyString_When_SetNameCalled()
    {
        PlayStyle style = new PlayStyle("Initial");

        style.setName("");

        assertThat(style.getName()).isEmpty();
    }

    // ========================================
    // Common Play Style Names
    // ========================================

    @Test
    void should_StoreCommonPlayStyles_When_TypicalNamesProvided()
    {
        // Test common poker play style names
        PlayStyle tight = new PlayStyle("Tight");
        PlayStyle loose = new PlayStyle("Loose");
        PlayStyle aggressive = new PlayStyle("Aggressive");
        PlayStyle passive = new PlayStyle("Passive");
        PlayStyle tightAggressive = new PlayStyle("Tight-Aggressive");
        PlayStyle loosePassive = new PlayStyle("Loose-Passive");

        assertThat(tight.getName()).isEqualTo("Tight");
        assertThat(loose.getName()).isEqualTo("Loose");
        assertThat(aggressive.getName()).isEqualTo("Aggressive");
        assertThat(passive.getName()).isEqualTo("Passive");
        assertThat(tightAggressive.getName()).isEqualTo("Tight-Aggressive");
        assertThat(loosePassive.getName()).isEqualTo("Loose-Passive");
    }

    // ========================================
    // Multiple Changes Test
    // ========================================

    @Test
    void should_RetainLatestName_When_NameChangedMultipleTimes()
    {
        PlayStyle style = new PlayStyle("First");

        style.setName("Second");
        assertThat(style.getName()).isEqualTo("Second");

        style.setName("Third");
        assertThat(style.getName()).isEqualTo("Third");

        style.setName("Fourth");
        assertThat(style.getName()).isEqualTo("Fourth");
    }

    // ========================================
    // Edge Cases
    // ========================================

    @Test
    void should_HandleVeryLongName_When_SetNameCalled()
    {
        String longName = "Very-Long-Play-Style-Name-With-Many-Hyphens-And-Words-That-Describes-Complex-Playing-Strategy";
        PlayStyle style = new PlayStyle(longName);

        assertThat(style.getName()).isEqualTo(longName);
    }

    @Test
    void should_HandleSpecialCharacters_When_SetNameCalled()
    {
        PlayStyle style = new PlayStyle("Aggressive (Post-Flop)");

        assertThat(style.getName()).isEqualTo("Aggressive (Post-Flop)");
    }

    @Test
    void should_PreserveWhitespace_When_NameContainsSpaces()
    {
        PlayStyle style = new PlayStyle("  Tight  Aggressive  ");

        assertThat(style.getName()).isEqualTo("  Tight  Aggressive  ");
    }
}
