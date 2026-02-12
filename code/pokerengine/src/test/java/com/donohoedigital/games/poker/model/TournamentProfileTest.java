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

import java.io.*;

import static org.junit.Assert.*;

/**
 * Tests for TournamentProfile constants and helper methods.
 */
public class TournamentProfileTest {

    // ========== Constant Value Tests ==========

    @Test
    public void should_AllowNinetyOnlinePlayers() {
        assertEquals(90, TournamentProfile.MAX_ONLINE_PLAYERS);
    }

    @Test
    public void should_AllowOneMilMaxChips() {
        assertEquals(1000000, TournamentProfile.MAX_CHIPS);
    }

    @Test
    public void should_AllowOneMilMaxBuy() {
        assertEquals(1000000, TournamentProfile.MAX_BUY);
    }

    @Test
    public void should_AllowThirtyObservers() {
        assertEquals(30, TournamentProfile.MAX_OBSERVERS);
    }

    @Test
    public void should_AllowTwoMinuteTimeout() {
        assertEquals(120, TournamentProfile.MAX_TIMEOUT);
    }

    @Test
    public void should_AllowTwoMinuteThinkbank() {
        assertEquals(120, TournamentProfile.MAX_THINKBANK);
    }

    // ========== getMaxObservers Tests ==========

    @Test
    public void should_ReturnMaxObserversFromMap_WithoutTestingBranch() {
        // getMaxObservers should use same max regardless of testing mode
        TournamentProfile profile = new TournamentProfile("test");
        int maxObs = profile.getMaxObservers();
        assertTrue("Max observers should be between 0 and " + TournamentProfile.MAX_OBSERVERS,
                maxObs >= 0 && maxObs <= TournamentProfile.MAX_OBSERVERS);
    }

    // ========== getStartingDepthBBs Tests ==========

    @Test
    public void should_CalculateStartingDepth_ForDefaultProfile() {
        // Default profile: buyinChips=1000, level 1 big blind=2
        TournamentProfile profile = new TournamentProfile("test");
        assertEquals(500, profile.getStartingDepthBBs());
    }

    @Test
    public void should_CalculateStartingDepth_ForCustomValues() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setBuyinChips(10000);
        // Level 1 big blind is still 2 from fixAll()
        assertEquals(5000, profile.getStartingDepthBBs());
    }

    @Test
    public void should_ReturnNegativeOne_WhenBigBlindIsZero() {
        TournamentProfile profile = new TournamentProfile("test");
        // Clear level data to simulate no levels
        profile.clearLevel(1);
        // With no big blind data, getAmount returns 0
        assertEquals(-1, profile.getStartingDepthBBs());
    }

    // ========== Round-trip Import/Export Tests ==========

    @Test
    public void should_RoundTripProfile_WhenWrittenAndRead() throws IOException {
        // Given: a profile with custom settings
        TournamentProfile original = new TournamentProfile("My Tournament");
        original.setBuyinChips(5000);
        original.setNumPlayers(20);
        original.setBuyin(500);
        original.setMinutesPerLevel(15);

        // When: write to string and read back
        StringWriter sw = new StringWriter();
        original.write(sw);

        TournamentProfile imported = new TournamentProfile();
        imported.read(new StringReader(sw.toString()), false);

        // Then: settings should match
        assertEquals("Name should match", "My Tournament", imported.getName());
        assertEquals("Buyin chips should match", 5000, imported.getBuyinChips());
        assertEquals("Num players should match", 20, imported.getNumPlayers());
        assertEquals("Buyin should match", 500, imported.getBuyinCost());
        assertEquals("Minutes per level should match", 15, imported.getDefaultMinutesPerLevel());
    }

    @Test
    public void should_PreserveBlindStructure_WhenRoundTripped() throws IOException {
        // Given: a profile with custom blind levels
        TournamentProfile original = new TournamentProfile("Blind Test");
        original.setBuyinChips(10000);

        // When: round-trip through write/read
        StringWriter sw = new StringWriter();
        original.write(sw);

        TournamentProfile imported = new TournamentProfile();
        imported.read(new StringReader(sw.toString()), false);

        // Then: blind structure should match
        assertEquals("Level 1 big blind should match", original.getBigBlind(1), imported.getBigBlind(1));
        assertEquals("Level 1 small blind should match", original.getSmallBlind(1), imported.getSmallBlind(1));
        assertEquals("Starting depth should match", original.getStartingDepthBBs(), imported.getStartingDepthBBs());
    }
}
