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
package com.donohoedigital.games.poker.model;

import com.donohoedigital.games.poker.engine.PokerConstants;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

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
public class LevelValidatorTest {

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
        assertEquals("Should have 3 levels", 3, levels.size());
        assertEquals("First level number should be 1", 1, levels.get(0).levelNum);
        assertEquals("Second level number should be 2", 2, levels.get(1).levelNum);
        assertEquals("Third level number should be 3", 3, levels.get(2).levelNum);
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
        assertEquals(5, levels.get(0).smallBlind);
        assertEquals(10, levels.get(0).bigBlind);
        assertEquals(25, levels.get(1).smallBlind);
        assertEquals(50, levels.get(1).bigBlind);
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
        assertEquals("Big blind should be 2x small", 20, levels.get(0).bigBlind);
    }

    @Test
    public void should_FillMissingSmallBlind_FromBigBlind() {
        // Given: level with big blind but no small blind
        Map<String, String> rawData = new HashMap<>();
        rawData.put("big1", "20");
        // small1 is missing

        LevelValidator validator = new LevelValidator();

        // When: validate
        List<LevelValidator.LevelData> levels = validator.validateAndNormalize(rawData, 10,
                PokerConstants.DE_NO_LIMIT_HOLDEM);

        // Then: small blind should be big / 2 (or adjusted)
        assertTrue("Small blind should be positive", levels.get(0).smallBlind > 0);
        assertTrue("Small blind should be <= big", levels.get(0).smallBlind <= levels.get(0).bigBlind);
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
        assertEquals("Level 2 small should match level 1", levels.get(0).smallBlind, levels.get(1).smallBlind);
        assertEquals("Level 2 big should match level 1", levels.get(0).bigBlind, levels.get(1).bigBlind);
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
        assertTrue("Level 2 small should be >= level 1 small", levels.get(1).smallBlind >= levels.get(0).smallBlind);
        assertTrue("Level 2 big should be >= level 1 big", levels.get(1).bigBlind >= levels.get(0).bigBlind);
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
        assertEquals("Ante can return to 0", 0, levels.get(1).ante);
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
        assertTrue("Ante should not decrease unless going to 0",
                levels.get(1).ante >= levels.get(0).ante || levels.get(1).ante == 0);
    }

    // ========== Ante Bounds Tests ==========

    @Test
    public void should_EnforceMinimumAnte_At5PercentOfSmallBlind() {
        // Given: ante is very small (less than 5% of small blind)
        Map<String, String> rawData = new HashMap<>();
        rawData.put("ante1", "1"); // Less than 5% of 100
        rawData.put("small1", "100");
        rawData.put("big1", "200");

        LevelValidator validator = new LevelValidator();

        // When: validate
        List<LevelValidator.LevelData> levels = validator.validateAndNormalize(rawData, 10,
                PokerConstants.DE_NO_LIMIT_HOLDEM);

        // Then: ante should be at least 5% of small blind
        int expectedMinAnte = (int) (100 * 0.05);
        assertTrue("Ante should be >= 5% of small blind", levels.get(0).ante >= expectedMinAnte);
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
        assertTrue("Ante should not exceed small blind", levels.get(0).ante <= levels.get(0).smallBlind);
    }

    // ========== Rounding Tests ==========

    @Test
    public void should_RoundBlinds_ToAppropriateIncrements() {
        // Given: blinds with odd values
        Map<String, String> rawData = new HashMap<>();
        rawData.put("small1", "47");
        rawData.put("big1", "93");

        LevelValidator validator = new LevelValidator();

        // When: validate
        List<LevelValidator.LevelData> levels = validator.validateAndNormalize(rawData, 10,
                PokerConstants.DE_NO_LIMIT_HOLDEM);

        // Then: blinds should be rounded up to next increment
        // At scale <= 100, increment is 1 (so rounds up: 47->47, 93->93, already valid)
        // At scale 101-500, increment is 5
        // Test that rounding occurred (values rounded up)
        int small = levels.get(0).smallBlind;
        int big = levels.get(0).bigBlind;

        assertTrue("Small blind should be >= original", small >= 47);
        assertTrue("Big blind should be >= original", big >= 93);

        // Test with larger values that should round to 5
        rawData.clear();
        rawData.put("small1", "103");
        rawData.put("big1", "207");

        levels = validator.validateAndNormalize(rawData, 10, PokerConstants.DE_NO_LIMIT_HOLDEM);

        // At scale 101-500, should round to multiples of 5
        assertTrue("Larger small blind should be rounded to 5", levels.get(0).smallBlind % 5 == 0);
        assertTrue("Larger big blind should be rounded to 5", levels.get(0).bigBlind % 5 == 0);
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
        assertTrue("Level should be marked as break", levels.get(0).isBreak);
        assertEquals("Break should have minutes", 10, levels.get(0).minutes);
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
        assertEquals("Break should have no small blind", 0, levels.get(1).smallBlind);
        assertEquals("Break should have no big blind", 0, levels.get(1).bigBlind);
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
        assertEquals("Should use default minutes (0 = use default)", 0, levels.get(0).minutes);
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
        assertEquals("Should preserve custom minutes", 20, levels.get(0).minutes);
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
        assertNull("Should use default game type (null = use default)", levels.get(0).gameType);
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
        assertEquals("Should have 1 default level", 1, levels.size());
        assertEquals("Level number should be 1", 1, levels.get(0).levelNum);
        assertTrue("Should have positive small blind", levels.get(0).smallBlind > 0);
        assertTrue("Should have positive big blind", levels.get(0).bigBlind > 0);
    }

    @Test
    public void should_HandleAllBreakLevels_ByAddingDefault() {
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
        boolean hasNonBreak = false;
        for (LevelValidator.LevelData level : levels) {
            if (!level.isBreak) {
                hasNonBreak = true;
                break;
            }
        }
        assertTrue("Should have at least one non-break level", hasNonBreak);
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
        assertTrue("Minutes should not exceed MAX_MINUTES", levels.get(0).minutes <= TournamentProfile.MAX_MINUTES);
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
        assertEquals("Should have 4 levels", 4, levels.size());

        // Verify consolidation
        assertEquals(1, levels.get(0).levelNum);
        assertEquals(2, levels.get(1).levelNum);
        assertEquals(3, levels.get(2).levelNum);
        assertEquals(4, levels.get(3).levelNum);

        // Verify break
        assertTrue("Level 3 should be break", levels.get(2).isBreak);

        // Verify monotonic blinds (excluding breaks)
        assertTrue("Level 4 big should be >= level 2 big", levels.get(3).bigBlind >= levels.get(1).bigBlind);
    }
}
