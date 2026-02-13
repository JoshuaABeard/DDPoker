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

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for BlindTemplate - predefined blind structure templates.
 */
public class BlindTemplateTest {

    @Test
    public void should_HaveCorrectDisplayNames() {
        assertEquals("Slow (x1.5, 20min)", BlindTemplate.SLOW.getDisplayName());
        assertEquals("Standard (x2.0, 15min)", BlindTemplate.STANDARD.getDisplayName());
        assertEquals("Turbo (x2.0, 10min)", BlindTemplate.TURBO.getDisplayName());
        assertEquals("Hyper (x2.0, 5min)", BlindTemplate.HYPER.getDisplayName());
    }

    @Test
    public void should_HaveCorrectProgression() {
        assertEquals(1.5, BlindTemplate.SLOW.getProgression(), 0.01);
        assertEquals(2.0, BlindTemplate.STANDARD.getProgression(), 0.01);
        assertEquals(2.0, BlindTemplate.TURBO.getProgression(), 0.01);
        assertEquals(2.0, BlindTemplate.HYPER.getProgression(), 0.01);
    }

    @Test
    public void should_HaveCorrectMinutesPerLevel() {
        assertEquals(20, BlindTemplate.SLOW.getMinutesPerLevel());
        assertEquals(15, BlindTemplate.STANDARD.getMinutesPerLevel());
        assertEquals(10, BlindTemplate.TURBO.getMinutesPerLevel());
        assertEquals(5, BlindTemplate.HYPER.getMinutesPerLevel());
    }

    @Test
    public void should_HaveCorrectStartingBlinds() {
        assertArrayEquals(new int[]{25, 50}, BlindTemplate.SLOW.getStartingBlinds());
        assertArrayEquals(new int[]{25, 50}, BlindTemplate.STANDARD.getStartingBlinds());
        assertArrayEquals(new int[]{25, 50}, BlindTemplate.TURBO.getStartingBlinds());
        assertArrayEquals(new int[]{25, 50}, BlindTemplate.HYPER.getStartingBlinds());
    }

    @Test
    public void should_GenerateLevels_WithStandardTemplate() {
        TournamentProfile profile = new TournamentProfile("Test");

        // Generate 5 levels with Standard template, no breaks
        BlindTemplate.STANDARD.generateLevels(profile, 5, false, 0);

        // Verify level 1: 25/50
        assertEquals(0, profile.getAnte(1));
        assertEquals(25, profile.getSmallBlind(1));
        assertEquals(50, profile.getBigBlind(1));
        assertEquals(15, profile.getMinutes(1));

        // Verify level 2: 50/100 (2x progression)
        assertEquals(0, profile.getAnte(2));
        assertEquals(50, profile.getSmallBlind(2));
        assertEquals(100, profile.getBigBlind(2));

        // Verify level 3: 100/200
        assertEquals(0, profile.getAnte(3));
        assertEquals(100, profile.getSmallBlind(3));
        assertEquals(200, profile.getBigBlind(3));

        // Verify level 4: 200/400
        assertEquals(0, profile.getAnte(4));
        assertEquals(200, profile.getSmallBlind(4));
        assertEquals(400, profile.getBigBlind(4));

        // Verify level 5: 400/800 with ante starting
        assertTrue("Antes should start by level 5", profile.getAnte(5) > 0);
        assertEquals(400, profile.getSmallBlind(5));
        assertEquals(800, profile.getBigBlind(5));
    }

    @Test
    public void should_GenerateLevels_WithSlowTemplate() {
        TournamentProfile profile = new TournamentProfile("Test");

        // Generate 3 levels with Slow template (1.5x progression)
        BlindTemplate.SLOW.generateLevels(profile, 3, false, 0);

        // Verify level 1: 25/50
        assertEquals(25, profile.getSmallBlind(1));
        assertEquals(50, profile.getBigBlind(1));
        assertEquals(20, profile.getMinutes(1)); // Slow = 20 min

        // Verify level 2: 35/70 (1.5x, rounded)
        assertEquals(35, profile.getSmallBlind(2));
        assertEquals(70, profile.getBigBlind(2));

        // Verify level 3: 50/100 (1.5x again, rounded)
        assertEquals(50, profile.getSmallBlind(3));
        assertEquals(100, profile.getBigBlind(3));
    }

    @Test
    public void should_InsertBreaks_WhenRequested() {
        TournamentProfile profile = new TournamentProfile("Test");

        // Generate 9 levels with breaks every 3 levels
        BlindTemplate.STANDARD.generateLevels(profile, 9, true, 3);

        // Verify first 3 blind levels are regular
        assertFalse("Level 1 should not be a break", profile.isBreak(1));
        assertFalse("Level 2 should not be a break", profile.isBreak(2));
        assertFalse("Level 3 should not be a break", profile.isBreak(3));

        // Verify break is inserted after 3 blind levels
        assertTrue("Level 4 should be a break", profile.isBreak(4));

        // Verify next 3 blind levels are regular
        assertFalse("Level 5 should not be a break", profile.isBreak(5));
        assertFalse("Level 6 should not be a break", profile.isBreak(6));
        assertFalse("Level 7 should not be a break", profile.isBreak(7));

        // Verify another break after 3 more blind levels
        assertTrue("Level 8 should be a break", profile.isBreak(8));

        // Verify final 3 blind levels are regular (no break after last level)
        assertFalse("Level 9 should not be a break", profile.isBreak(9));
        assertFalse("Level 10 should not be a break", profile.isBreak(10));
        assertFalse("Level 11 should not be a break", profile.isBreak(11));
    }

    @Test
    public void should_NotInsertBreak_BeforeFirstLevel() {
        TournamentProfile profile = new TournamentProfile("Test");

        // Generate 3 levels with breaks every 1 level
        BlindTemplate.STANDARD.generateLevels(profile, 3, true, 1);

        // First level should NOT be a break
        assertFalse("Level 1 should not be a break", profile.isBreak(1));
    }

    @Test
    public void should_SetDefaultMinutesPerLevel() {
        TournamentProfile profile = new TournamentProfile("Test");

        // Generate levels with Turbo template
        BlindTemplate.TURBO.generateLevels(profile, 3, false, 0);

        // Verify default minutes is set to 10 (Turbo)
        assertEquals(10, profile.getDefaultMinutesPerLevel());
    }

    @Test
    public void should_ClearExistingLevels_BeforeGenerating() {
        TournamentProfile profile = new TournamentProfile("Test");

        // Set some existing levels
        profile.setLevel(1, 10, 100, 200, 10);
        profile.setLevel(2, 20, 200, 400, 10);

        // Generate new levels
        BlindTemplate.STANDARD.generateLevels(profile, 3, false, 0);

        // Verify old levels are replaced
        assertEquals(25, profile.getSmallBlind(1)); // Not 100
        assertEquals(50, profile.getBigBlind(1)); // Not 200
    }

    @Test(expected = IllegalArgumentException.class)
    public void should_ThrowException_WhenNumLevelsTooLow() {
        TournamentProfile profile = new TournamentProfile("Test");
        BlindTemplate.STANDARD.generateLevels(profile, 0, false, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void should_ThrowException_WhenNumLevelsTooHigh() {
        TournamentProfile profile = new TournamentProfile("Test");
        BlindTemplate.STANDARD.generateLevels(profile, 41, false, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void should_ThrowException_WhenBreakFrequencyInvalid() {
        TournamentProfile profile = new TournamentProfile("Test");
        BlindTemplate.STANDARD.generateLevels(profile, 5, true, 0);
    }

    @Test
    public void should_GenerateSingleLevel() {
        TournamentProfile profile = new TournamentProfile("Test");

        // Generate just 1 level
        BlindTemplate.STANDARD.generateLevels(profile, 1, false, 0);

        assertEquals(25, profile.getSmallBlind(1));
        assertEquals(50, profile.getBigBlind(1));
    }

    @Test
    public void should_GenerateManyLevels() {
        TournamentProfile profile = new TournamentProfile("Test");

        // Generate 20 levels (won't hit MAX_BLINDANTE cap)
        BlindTemplate.STANDARD.generateLevels(profile, 20, false, 0);

        // Verify first level
        assertEquals(25, profile.getSmallBlind(1));
        assertEquals(50, profile.getBigBlind(1));

        // Verify a middle level exists and progresses
        assertTrue("Level 10 should have blinds set", profile.getBigBlind(10) > 0);
        assertTrue("Level 10 should have higher blinds than level 1", profile.getBigBlind(10) > profile.getBigBlind(1));

        // Verify last level exists
        assertTrue("Level 20 should have blinds set", profile.getBigBlind(20) > 0);
        assertTrue("Level 20 should have higher blinds than level 10",
                profile.getBigBlind(20) > profile.getBigBlind(10));
    }

    @Test
    public void should_UseDisplayName_ForToString() {
        assertEquals("Slow (x1.5, 20min)", BlindTemplate.SLOW.toString());
        assertEquals("Standard (x2.0, 15min)", BlindTemplate.STANDARD.toString());
        assertEquals("Turbo (x2.0, 10min)", BlindTemplate.TURBO.toString());
        assertEquals("Hyper (x2.0, 5min)", BlindTemplate.HYPER.toString());
    }

    @Test
    public void should_RoundBlinds_ToNiceNumbers() {
        TournamentProfile profile = new TournamentProfile("Test");

        // Generate many levels to see rounding in action
        BlindTemplate.SLOW.generateLevels(profile, 15, false, 0);

        // Check that all blinds are "nice" numbers (end in 0, 5, 25, 50, etc.)
        for (int i = 1; i <= 15; i++) {
            int small = profile.getSmallBlind(i);
            int big = profile.getBigBlind(i);

            // Should be divisible by 5 at minimum
            assertTrue("Small blind should be a nice number: " + small, small % 5 == 0 || small < 10);
            assertTrue("Big blind should be a nice number: " + big, big % 5 == 0 || big < 10);
        }
    }
}
