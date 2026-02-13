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

    // ========== Late Registration Tests ==========

    @Test
    public void should_DefaultToLateRegDisabled() {
        TournamentProfile profile = new TournamentProfile("test");
        assertFalse("Late registration should be disabled by default", profile.isLateRegEnabled());
    }

    @Test
    public void should_DefaultToLevel1Cutoff() {
        TournamentProfile profile = new TournamentProfile("test");
        assertEquals("Default cutoff should be level 1", 1, profile.getLateRegUntilLevel());
    }

    @Test
    public void should_DefaultToStartingChipsMode() {
        TournamentProfile profile = new TournamentProfile("test");
        assertEquals("Default chip mode should be starting chips", PokerConstants.LATE_REG_CHIPS_STARTING,
                profile.getLateRegChips());
    }

    @Test
    public void should_EnableAndDisableLateReg() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setLateRegEnabled(true);
        assertTrue("Late registration should be enabled", profile.isLateRegEnabled());

        profile.setLateRegEnabled(false);
        assertFalse("Late registration should be disabled", profile.isLateRegEnabled());
    }

    @Test
    public void should_SetLateRegCutoffLevel() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setLateRegUntilLevel(5);
        assertEquals("Cutoff should be level 5", 5, profile.getLateRegUntilLevel());
    }

    @Test
    public void should_EnforceLateRegCutoffMinimum() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setLateRegUntilLevel(0);
        assertEquals("Cutoff should be clamped to minimum 1", 1, profile.getLateRegUntilLevel());
    }

    @Test
    public void should_EnforceLateRegCutoffMaximum() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setLateRegUntilLevel(50); // Exceeds MAX_LEVELS (40)
        assertEquals("Cutoff should be clamped to MAX_LEVELS", TournamentProfile.MAX_LEVELS,
                profile.getLateRegUntilLevel());
    }

    @Test
    public void should_SetLateRegChipsToStarting() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setLateRegChips(PokerConstants.LATE_REG_CHIPS_STARTING);
        assertEquals("Chip mode should be starting", PokerConstants.LATE_REG_CHIPS_STARTING, profile.getLateRegChips());
    }

    @Test
    public void should_SetLateRegChipsToAverage() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setLateRegChips(PokerConstants.LATE_REG_CHIPS_AVERAGE);
        assertEquals("Chip mode should be average", PokerConstants.LATE_REG_CHIPS_AVERAGE, profile.getLateRegChips());
    }

    @Test
    public void should_RoundTripLateRegSettings() throws IOException {
        // Given: a profile with late registration enabled
        TournamentProfile original = new TournamentProfile("Late Reg Test");
        original.setLateRegEnabled(true);
        original.setLateRegUntilLevel(3);
        original.setLateRegChips(PokerConstants.LATE_REG_CHIPS_AVERAGE);

        // When: write to string and read back
        StringWriter sw = new StringWriter();
        original.write(sw);

        TournamentProfile imported = new TournamentProfile();
        imported.read(new StringReader(sw.toString()), false);

        // Then: late reg settings should match
        assertTrue("Late reg should be enabled", imported.isLateRegEnabled());
        assertEquals("Cutoff level should match", 3, imported.getLateRegUntilLevel());
        assertEquals("Chip mode should match", PokerConstants.LATE_REG_CHIPS_AVERAGE, imported.getLateRegChips());
    }

    // ========== Phase 4: Edge Cases and Integration Tests ==========

    // ========== Max Payout Constraints Tests ==========

    @Test
    public void should_CalculateMaxPayoutSpots_ForSmallTournament() {
        TournamentProfile profile = new TournamentProfile("test");
        // For 10 players, max payout spots should be limited
        int maxSpots = profile.getMaxPayoutSpots(10);
        assertTrue("Max spots should be positive", maxSpots > 0);
        assertTrue("Max spots should not exceed player count", maxSpots <= 10);
    }

    @Test
    public void should_CalculateMaxPayoutSpots_ForLargeTournament() {
        TournamentProfile profile = new TournamentProfile("test");
        // For 100 players, max payout spots should scale appropriately
        int maxSpots = profile.getMaxPayoutSpots(100);
        assertTrue("Max spots should be positive", maxSpots > 0);
        assertTrue("Max spots should not exceed player count", maxSpots <= 100);
        assertTrue("Max spots should be reasonable percentage", maxSpots >= 10);
    }

    @Test
    public void should_CalculateMaxPayoutPercent_ForSmallTournament() {
        TournamentProfile profile = new TournamentProfile("test");
        // For 10 players, max percent should be constrained
        int maxPercent = profile.getMaxPayoutPercent(10);
        assertTrue("Max percent should be positive", maxPercent > 0);
        assertTrue("Max percent should be reasonable", maxPercent <= 100);
    }

    @Test
    public void should_CalculateMaxPayoutPercent_ForLargeTournament() {
        TournamentProfile profile = new TournamentProfile("test");
        // For 100 players, max percent should be lower
        int maxPercent = profile.getMaxPayoutPercent(100);
        assertTrue("Max percent should be positive", maxPercent > 0);
        assertTrue("Max percent should be reasonable", maxPercent <= 100);
    }

    // ========== Edge Cases: Zero and Boundary Values ==========

    @Test
    public void should_HandleZeroPlayers_Gracefully() {
        TournamentProfile profile = new TournamentProfile("test");
        int maxSpots = profile.getMaxPayoutSpots(0);
        assertTrue("Should handle zero players without error", maxSpots >= 0);
    }

    @Test
    public void should_HandleSinglePlayer_Tournament() {
        TournamentProfile profile = new TournamentProfile("test");
        int maxSpots = profile.getMaxPayoutSpots(1);
        assertEquals("Single player should allow max 1 spot", 1, maxSpots);
    }

    // ========== Prize Pool and Payout Integration Tests ==========

    @Test
    public void should_HavePositivePrizePool_WithDefaultSettings() {
        TournamentProfile profile = new TournamentProfile("test");
        int prizePool = profile.getPrizePool();
        assertTrue("Default profile should have positive prize pool", prizePool > 0);
    }

    @Test
    public void should_HavePositiveTrueBuyin_WithDefaultSettings() {
        TournamentProfile profile = new TournamentProfile("test");
        int trueBuyin = profile.getTrueBuyin();
        assertTrue("True buyin should be positive", trueBuyin > 0);
    }

    @Test
    public void should_HaveAtLeastOnePayoutSpot_WithDefaultSettings() {
        TournamentProfile profile = new TournamentProfile("test");
        int spots = profile.getNumSpots();
        assertTrue("Should have at least 1 payout spot", spots >= 1);
    }

    // ========== Integration: Serialization of Payout Settings ==========

    @Test
    public void should_PreservePayoutCalculatorIntegration_WhenRoundTripped() throws IOException {
        // Given: a default profile (which uses PayoutCalculator internally)
        TournamentProfile original = new TournamentProfile("Payout Test");

        // When: write to string and read back
        StringWriter sw = new StringWriter();
        original.write(sw);

        TournamentProfile imported = new TournamentProfile();
        imported.read(new StringReader(sw.toString()), false);

        // Then: payout calculations should work identically
        assertEquals("Num spots should match", original.getNumSpots(), imported.getNumSpots());
        assertEquals("Prize pool should match", original.getPrizePool(), imported.getPrizePool());
        assertEquals("True buyin should match", original.getTrueBuyin(), imported.getTrueBuyin());
    }

    @Test
    public void should_IntegrateExtractedComponents_WithoutRegression() {
        // Integration test: verify that all three extracted components work together
        // PayoutCalculator, BlindStructure, and PayoutDistributionCalculator
        TournamentProfile profile = new TournamentProfile("Integration Test");

        // Verify PayoutCalculator integration (getNumSpots, getPrizePool)
        int spots = profile.getNumSpots();
        int prizePool = profile.getPrizePool();

        // Verify BlindStructure integration (getBigBlind, getSmallBlind)
        int bigBlind = profile.getBigBlind(1);
        int smallBlind = profile.getSmallBlind(1);

        // Verify starting depth calculation (uses BlindStructure)
        int startingDepth = profile.getStartingDepthBBs();

        // All should be positive and reasonable
        assertTrue("Spots should be positive", spots > 0);
        assertTrue("Prize pool should be positive", prizePool > 0);
        assertTrue("Big blind should be positive", bigBlind > 0);
        assertTrue("Small blind should be positive", smallBlind > 0);
        assertTrue("Starting depth should be positive", startingDepth > 0);

        // Blind relationship should hold
        assertEquals("Big blind should be 2x small blind", bigBlind, smallBlind * 2);
    }
}
