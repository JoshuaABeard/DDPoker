/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2026 Joshua Beard and contributors
 *
 * This file is part of DD Poker, originally created by Doug Donohoe.
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
package com.donohoedigital.games.poker.model;

import com.donohoedigital.games.poker.engine.PokerConstants;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for LevelValidator - validates and normalizes tournament blind level
 * structure.
 *
 * <p>
 * The validator performs several critical operations:
 * <ol>
 * <li>Gap consolidation: Levels 1,3,5 become 1,2,3
 * <li>Missing blind fill-in: If big=0, uses small*2
 * <li>Monotonic enforcement: Blinds must increase (or stay same) each level
 * <li>Ante bounds: 5% minimum, 100% maximum of small blind
 * <li>Rounding: Amounts rounded to appropriate increments
 * <li>Break level handling: Special ante value (-1) indicates break
 * <li>Default propagation: Missing game types and minutes use defaults
 * </ol>
 */
class LevelValidatorTest {

    /**
     * Minimum ante as a percentage of small blind (5%), derived from
     * LevelValidator's normalizeLevels() which uses
     * {@code (int)(smallBlind * 0.05f)}.
     */
    private static final float ANTE_MIN_PERCENT = 0.05f;

    // ========== Gap Consolidation Tests ==========

    @Test
    public void should_ConsolidateGaps_WhenLevelsAreNonSequential() {
        // Given: levels defined at positions 1, 3, 5 (gaps at 2, 4)
        Map<String, String> rawData = new HashMap<>();
        rawData.put("ante1", "0");
        rawData.put("small1", "5");
        rawData.put("big1", "10");

        rawData.put("ante3", "5");
        rawData.put("small3", "10");
        rawData.put("big3", "20");

        rawData.put("ante5", "10");
        rawData.put("small5", "20");
        rawData.put("big5", "40");

        LevelValidator validator = new LevelValidator();

        // When: validate and normalize
        List<LevelValidator.LevelData> levels = validator.validateAndNormalize(rawData, 10,
                PokerConstants.DE_NO_LIMIT_HOLDEM);

        // Then: should have 3 consecutive levels
        assertThat(levels).hasSize(3);
        assertThat(levels.get(0).levelNum).isEqualTo(1);
        assertThat(levels.get(1).levelNum).isEqualTo(2);
        assertThat(levels.get(2).levelNum).isEqualTo(3);
    }

    @Test
    public void should_PreserveBlindValues_AfterConsolidation() {
        // Given: levels with gaps
        Map<String, String> rawData = new HashMap<>();
        rawData.put("small1", "5");
        rawData.put("big1", "10");
        rawData.put("small5", "25");
        rawData.put("big5", "50");

        LevelValidator validator = new LevelValidator();

        // When: consolidate
        List<LevelValidator.LevelData> levels = validator.validateAndNormalize(rawData, 10,
                PokerConstants.DE_NO_LIMIT_HOLDEM);

        // Then: blind values should be preserved (just renumbered)
        assertThat(levels.get(0).smallBlind).isEqualTo(5);
        assertThat(levels.get(0).bigBlind).isEqualTo(10);
        assertThat(levels.get(1).smallBlind).isEqualTo(25);
        assertThat(levels.get(1).bigBlind).isEqualTo(50);
    }

    // ========== Missing Blind Fill-In Tests ==========

    @Test
    public void should_FillMissingBigBlind_FromSmallBlind() {
        // Given: level with small blind but no big blind
        Map<String, String> rawData = new HashMap<>();
        rawData.put("small1", "10");
        // big1 is missing

        LevelValidator validator = new LevelValidator();

        // When: validate
        List<LevelValidator.LevelData> levels = validator.validateAndNormalize(rawData, 10,
                PokerConstants.DE_NO_LIMIT_HOLDEM);

        // Then: big blind should be small * 2
        assertThat(levels.get(0).bigBlind).isEqualTo(20);
    }

    @Test
    public void should_FillMissingSmallBlind_FromBigBlind() {
        // Given: level with big blind (20) but no small blind
        // LevelValidator sets smallBlind = bigBlind / 2 when bigBlind > 2
        Map<String, String> rawData = new HashMap<>();
        rawData.put("big1", "20");
        // small1 is missing

        LevelValidator validator = new LevelValidator();

        // When: validate
        List<LevelValidator.LevelData> levels = validator.validateAndNormalize(rawData, 10,
                PokerConstants.DE_NO_LIMIT_HOLDEM);

        // Then: small blind should be exactly big / 2 = 10
        assertThat(levels.get(0).smallBlind).isEqualTo(10);
    }

    @Test
    public void should_FillMissingBlinds_FromPreviousLevel() {
        // Given: level 2 has no blinds defined
        Map<String, String> rawData = new HashMap<>();
        rawData.put("small1", "10");
        rawData.put("big1", "20");
        rawData.put("ante2", "5");
        // small2 and big2 are missing

        LevelValidator validator = new LevelValidator();

        // When: validate
        List<LevelValidator.LevelData> levels = validator.validateAndNormalize(rawData, 10,
                PokerConstants.DE_NO_LIMIT_HOLDEM);

        // Then: level 2 should inherit blinds from level 1
        assertThat(levels.get(1).smallBlind).isEqualTo(levels.get(0).smallBlind);
        assertThat(levels.get(1).bigBlind).isEqualTo(levels.get(0).bigBlind);
    }

    // ========== Monotonic Increasing Enforcement Tests ==========

    @Test
    public void should_EnforceMonotonicIncrease_ForBlinds() {
        // Given: level 2 has smaller blinds than level 1 (should be corrected)
        Map<String, String> rawData = new HashMap<>();
        rawData.put("small1", "20");
        rawData.put("big1", "40");
        rawData.put("small2", "10"); // Smaller than level 1
        rawData.put("big2", "20");

        LevelValidator validator = new LevelValidator();

        // When: validate
        List<LevelValidator.LevelData> levels = validator.validateAndNormalize(rawData, 10,
                PokerConstants.DE_NO_LIMIT_HOLDEM);

        // Then: level 2 blinds should be >= level 1 blinds
        assertThat(levels.get(1).smallBlind).isGreaterThanOrEqualTo(levels.get(0).smallBlind);
        assertThat(levels.get(1).bigBlind).isGreaterThanOrEqualTo(levels.get(0).bigBlind);
    }

    @Test
    public void should_AllowAnteToReturnToZero() {
        // Given: level 2 has ante=0 after level 1 had ante=5
        Map<String, String> rawData = new HashMap<>();
        rawData.put("ante1", "5");
        rawData.put("small1", "10");
        rawData.put("big1", "20");
        rawData.put("ante2", "0");
        rawData.put("small2", "15");
        rawData.put("big2", "30");

        LevelValidator validator = new LevelValidator();

        // When: validate
        List<LevelValidator.LevelData> levels = validator.validateAndNormalize(rawData, 10,
                PokerConstants.DE_NO_LIMIT_HOLDEM);

        // Then: ante can go back to 0
        assertThat(levels.get(1).ante).isEqualTo(0);
    }

    @Test
    public void should_PreventAnteFromDecreasing_UnlessZero() {
        // Given: level 2 has lower ante than level 1 (but not zero)
        Map<String, String> rawData = new HashMap<>();
        rawData.put("ante1", "10");
        rawData.put("small1", "20");
        rawData.put("big1", "40");
        rawData.put("ante2", "5"); // Lower than level 1, but not zero
        rawData.put("small2", "25");
        rawData.put("big2", "50");

        LevelValidator validator = new LevelValidator();

        // When: validate
        List<LevelValidator.LevelData> levels = validator.validateAndNormalize(rawData, 10,
                PokerConstants.DE_NO_LIMIT_HOLDEM);

        // Then: ante should be enforced to >= previous (if not zero)
        assertThat(levels.get(1).ante).satisfiesAnyOf(
                ante -> assertThat(ante).isGreaterThanOrEqualTo(levels.get(0).ante),
                ante -> assertThat(ante).isEqualTo(0));
    }

    // ========== Ante Bounds Tests ==========

    @Test
    public void should_EnforceMinimumAnte_At5PercentOfSmallBlind() {
        // Given: ante is very small (less than 5% of small blind = 5)
        Map<String, String> rawData = new HashMap<>();
        rawData.put("ante1", "1"); // Less than 5% of 100
        rawData.put("small1", "100");
        rawData.put("big1", "200");

        LevelValidator validator = new LevelValidator();

        // When: validate
        List<LevelValidator.LevelData> levels = validator.validateAndNormalize(rawData, 10,
                PokerConstants.DE_NO_LIMIT_HOLDEM);

        // Then: ante should be at least 5% of small blind
        int expectedMinAnte = (int) (100 * ANTE_MIN_PERCENT);
        assertThat(levels.get(0).ante).isGreaterThanOrEqualTo(expectedMinAnte);
    }

    @Test
    public void should_EnforceMaximumAnte_At100PercentOfSmallBlind() {
        // Given: ante is larger than small blind
        Map<String, String> rawData = new HashMap<>();
        rawData.put("ante1", "150"); // Greater than small blind
        rawData.put("small1", "100");
        rawData.put("big1", "200");

        LevelValidator validator = new LevelValidator();

        // When: validate
        List<LevelValidator.LevelData> levels = validator.validateAndNormalize(rawData, 10,
                PokerConstants.DE_NO_LIMIT_HOLDEM);

        // Then: ante should not exceed small blind
        assertThat(levels.get(0).ante).isLessThanOrEqualTo(levels.get(0).smallBlind);
    }

    // ========== Rounding Tests ==========

    @Test
    public void should_NotRoundBlinds_When_ValuesAreUnder100() {
        // Given: blinds with odd values at or below 100 (increment = 1, no rounding)
        Map<String, String> rawData = new HashMap<>();
        rawData.put("small1", "47");
        rawData.put("big1", "93");

        LevelValidator validator = new LevelValidator();

        // When: validate
        List<LevelValidator.LevelData> levels = validator.validateAndNormalize(rawData, 10,
                PokerConstants.DE_NO_LIMIT_HOLDEM);

        // Then: at scale <= 100, increment is 1 so values are unchanged
        assertThat(levels.get(0).smallBlind).isEqualTo(47);
        assertThat(levels.get(0).bigBlind).isEqualTo(93);
    }

    @Test
    public void should_RoundBlinds_ToNearestFive_WhenValuesAreInHundreds() {
        // Given: blinds with odd values in the 101-500 range (increment = 5)
        // Rounding uses integer division: remainder >= (increment/2) rounds up.
        // 103: remainder=3, 3 >= (5/2=2) -> round up -> 105
        // 207: remainder=2, 2 >= (5/2=2) -> round up -> 210
        Map<String, String> rawData = new HashMap<>();
        rawData.put("small1", "103");
        rawData.put("big1", "207");

        LevelValidator validator = new LevelValidator();

        // When: validate
        List<LevelValidator.LevelData> levels = validator.validateAndNormalize(rawData, 10,
                PokerConstants.DE_NO_LIMIT_HOLDEM);

        // Then: values should be rounded to multiples of 5
        assertThat(levels.get(0).smallBlind).isEqualTo(105);
        assertThat(levels.get(0).bigBlind).isEqualTo(210);
    }

    // ========== Break Level Tests ==========

    @Test
    public void should_MarkBreakLevels_WithSpecialAnteValue() {
        // Given: level with break ante value (-1)
        Map<String, String> rawData = new HashMap<>();
        rawData.put("ante1", String.valueOf(TournamentProfile.BREAK_ANTE_VALUE));
        rawData.put("minutes1", "10");

        LevelValidator validator = new LevelValidator();

        // When: validate
        List<LevelValidator.LevelData> levels = validator.validateAndNormalize(rawData, 10,
                PokerConstants.DE_NO_LIMIT_HOLDEM);

        // Then: should be marked as break
        assertThat(levels.get(0).isBreak).isTrue();
        assertThat(levels.get(0).minutes).isEqualTo(10);
    }

    @Test
    public void should_ExcludeBreakLevels_FromBlindValidation() {
        // Given: break level with no blinds
        Map<String, String> rawData = new HashMap<>();
        rawData.put("small1", "10");
        rawData.put("big1", "20");
        rawData.put("ante2", String.valueOf(TournamentProfile.BREAK_ANTE_VALUE));
        rawData.put("minutes2", "10");
        rawData.put("small3", "15");
        rawData.put("big3", "30");

        LevelValidator validator = new LevelValidator();

        // When: validate
        List<LevelValidator.LevelData> levels = validator.validateAndNormalize(rawData, 10,
                PokerConstants.DE_NO_LIMIT_HOLDEM);

        // Then: break should not have blinds
        assertThat(levels.get(1).smallBlind).isEqualTo(0);
        assertThat(levels.get(1).bigBlind).isEqualTo(0);
    }

    // ========== Default Propagation Tests ==========

    @Test
    public void should_UseDefaultMinutes_WhenNotSpecified() {
        // Given: level with no minutes specified
        Map<String, String> rawData = new HashMap<>();
        rawData.put("small1", "10");
        rawData.put("big1", "20");
        // minutes1 is missing

        LevelValidator validator = new LevelValidator();

        // When: validate with default minutes = 15
        List<LevelValidator.LevelData> levels = validator.validateAndNormalize(rawData, 15,
                PokerConstants.DE_NO_LIMIT_HOLDEM);

        // Then: should use default minutes (stored as 0, meaning use default)
        assertThat(levels.get(0).minutes).isEqualTo(0);
    }

    @Test
    public void should_PreserveMinutes_WhenDifferentFromDefault() {
        // Given: level with custom minutes
        Map<String, String> rawData = new HashMap<>();
        rawData.put("small1", "10");
        rawData.put("big1", "20");
        rawData.put("minutes1", "20"); // Different from default

        LevelValidator validator = new LevelValidator();

        // When: validate with default minutes = 10
        List<LevelValidator.LevelData> levels = validator.validateAndNormalize(rawData, 10,
                PokerConstants.DE_NO_LIMIT_HOLDEM);

        // Then: should preserve custom minutes
        assertThat(levels.get(0).minutes).isEqualTo(20);
    }

    @Test
    public void should_UseDefaultGameType_WhenNotSpecified() {
        // Given: level with no game type
        Map<String, String> rawData = new HashMap<>();
        rawData.put("small1", "10");
        rawData.put("big1", "20");

        LevelValidator validator = new LevelValidator();

        // When: validate
        List<LevelValidator.LevelData> levels = validator.validateAndNormalize(rawData, 10,
                PokerConstants.DE_NO_LIMIT_HOLDEM);

        // Then: should use default game type (stored as null, meaning use default)
        assertThat(levels.get(0).gameType).isNull();
    }

    // ========== Edge Cases ==========

    @Test
    public void should_CreateDefaultLevel_WhenNoLevelsProvided() {
        // Given: empty level data
        Map<String, String> rawData = new HashMap<>();

        LevelValidator validator = new LevelValidator();

        // When: validate
        List<LevelValidator.LevelData> levels = validator.validateAndNormalize(rawData, 10,
                PokerConstants.DE_NO_LIMIT_HOLDEM);

        // Then: should create a default level 1
        assertThat(levels).hasSize(1);
        assertThat(levels.get(0).levelNum).isEqualTo(1);
        assertThat(levels.get(0).smallBlind).isGreaterThan(0);
        assertThat(levels.get(0).bigBlind).isGreaterThan(0);
    }

    @Test
    public void should_AddDefaultNonBreakLevel_When_AllLevelsAreBreaks() {
        // Given: only break levels defined
        Map<String, String> rawData = new HashMap<>();
        rawData.put("ante1", String.valueOf(TournamentProfile.BREAK_ANTE_VALUE));
        rawData.put("minutes1", "10");
        rawData.put("ante2", String.valueOf(TournamentProfile.BREAK_ANTE_VALUE));
        rawData.put("minutes2", "10");

        LevelValidator validator = new LevelValidator();

        // When: validate
        List<LevelValidator.LevelData> levels = validator.validateAndNormalize(rawData, 10,
                PokerConstants.DE_NO_LIMIT_HOLDEM);

        // Then: should add a non-break level
        assertThat(levels).anySatisfy(level -> assertThat(level.isBreak).isFalse());
    }

    @Test
    public void should_IgnoreLevels_When_LevelNumberExceedsMaximum() {
        // Given: levels defined beyond MAX_LEVELS (40)
        Map<String, String> rawData = new HashMap<>();
        rawData.put("small1", "10");
        rawData.put("big1", "20");
        // Level 41 exceeds MAX_LEVELS and should be silently ignored
        rawData.put("small41", "100");
        rawData.put("big41", "200");

        LevelValidator validator = new LevelValidator();

        // When: validate
        List<LevelValidator.LevelData> levels = validator.validateAndNormalize(rawData, 10,
                PokerConstants.DE_NO_LIMIT_HOLDEM);

        // Then: only the valid level within range should appear (level 41 is dropped)
        assertThat(levels).hasSize(1);
        assertThat(levels.get(0).smallBlind).isEqualTo(10);
        assertThat(levels.get(0).bigBlind).isEqualTo(20);
    }

    @Test
    public void should_EnforceMaxMinutes() {
        // Given: level with excessive minutes
        Map<String, String> rawData = new HashMap<>();
        rawData.put("small1", "10");
        rawData.put("big1", "20");
        rawData.put("minutes1", "500"); // Exceeds MAX_MINUTES (120)

        LevelValidator validator = new LevelValidator();

        // When: validate
        List<LevelValidator.LevelData> levels = validator.validateAndNormalize(rawData, 10,
                PokerConstants.DE_NO_LIMIT_HOLDEM);

        // Then: minutes should be capped at MAX_MINUTES
        assertThat(levels.get(0).minutes).isLessThanOrEqualTo(TournamentProfile.MAX_MINUTES);
    }

    // ========== Integration Tests ==========

    @Test
    public void should_ValidateComplexLevelStructure() {
        // Given: complex structure with gaps, breaks, and missing data
        Map<String, String> rawData = new HashMap<>();
        // Level 1: basic level
        rawData.put("small1", "5");
        rawData.put("big1", "10");

        // Level 3: gap from 1, missing small blind
        rawData.put("big3", "20");

        // Level 5: break
        rawData.put("ante5", String.valueOf(TournamentProfile.BREAK_ANTE_VALUE));
        rawData.put("minutes5", "15");

        // Level 7: after break
        rawData.put("small7", "15");
        rawData.put("big7", "30");
        rawData.put("ante7", "3");

        LevelValidator validator = new LevelValidator();

        // When: validate
        List<LevelValidator.LevelData> levels = validator.validateAndNormalize(rawData, 10,
                PokerConstants.DE_NO_LIMIT_HOLDEM);

        // Then: should have 4 levels (consolidated from 1,3,5,7)
        assertThat(levels).hasSize(4);

        // Verify consolidation
        assertThat(levels.get(0).levelNum).isEqualTo(1);
        assertThat(levels.get(1).levelNum).isEqualTo(2);
        assertThat(levels.get(2).levelNum).isEqualTo(3);
        assertThat(levels.get(3).levelNum).isEqualTo(4);

        // Verify break
        assertThat(levels.get(2).isBreak).isTrue();

        // Verify monotonic blinds (excluding breaks)
        assertThat(levels.get(3).bigBlind).isGreaterThanOrEqualTo(levels.get(1).bigBlind);
    }

    // ========== LevelData toString() Tests ==========

    @Test
    public void should_IncludeBreakKeyword_When_LevelIsBreak() {
        // Given: a break level
        Map<String, String> rawData = new HashMap<>();
        rawData.put("ante1", String.valueOf(TournamentProfile.BREAK_ANTE_VALUE));
        rawData.put("minutes1", "15");

        LevelValidator validator = new LevelValidator();

        // When: validate and get the level
        List<LevelValidator.LevelData> levels = validator.validateAndNormalize(rawData, 10,
                PokerConstants.DE_NO_LIMIT_HOLDEM);

        // Then: toString should show BREAK format
        String result = levels.get(0).toString();
        assertThat(result).isNotNull();
        assertThat(result).contains("BREAK");
        assertThat(result).contains("Level");
        assertThat(result).contains("15");
    }

    @Test
    public void should_ShowBlindsAndAnte_When_LevelIsNormal() {
        // Given: a normal level with all fields
        Map<String, String> rawData = new HashMap<>();
        rawData.put("ante1", "5");
        rawData.put("small1", "10");
        rawData.put("big1", "20");
        rawData.put("minutes1", "15");
        rawData.put("gametype1", PokerConstants.DE_NO_LIMIT_HOLDEM);

        LevelValidator validator = new LevelValidator();

        // When: validate and get the level
        List<LevelValidator.LevelData> levels = validator.validateAndNormalize(rawData, 10,
                PokerConstants.DE_POT_LIMIT_HOLDEM);

        // Then: toString should show full level details
        String result = levels.get(0).toString();
        assertThat(result).isNotNull();
        assertThat(result).contains("Level");
        assertThat(result).contains("Ante");
        assertThat(result).contains("Small");
        assertThat(result).contains("Big");
        assertThat(result).contains("Minutes");
        assertThat(result).contains("Type");
    }
}
