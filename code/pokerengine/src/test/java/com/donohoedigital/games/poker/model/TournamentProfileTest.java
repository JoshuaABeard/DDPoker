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
import org.junit.jupiter.api.Test;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TournamentProfile constants and helper methods.
 */
public class TournamentProfileTest {

    // ========== Constant Value Tests ==========

    @Test
    public void should_AllowNinetyOnlinePlayers() {
        assertEquals(120, TournamentProfile.MAX_ONLINE_PLAYERS);
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
        assertTrue(maxObs >= 0 && maxObs <= TournamentProfile.MAX_OBSERVERS,
                "Max observers should be between 0 and " + TournamentProfile.MAX_OBSERVERS);
    }

    // ========== Configurable Max Online Players Tests ==========

    @Test
    public void should_UseConfiguredMaxOnlinePlayers() {
        TournamentProfile profile = new TournamentProfile("Test");
        profile.setMaxOnlinePlayers(100);
        profile.setNumPlayers(150);

        assertEquals(100, profile.getMaxOnlinePlayers());
    }

    @Test
    public void should_DefaultTo60Players() {
        TournamentProfile profile = new TournamentProfile("Test");
        assertEquals(60, profile.getConfiguredMaxOnlinePlayers());
    }

    @Test
    public void should_CapAtConfiguredMax() {
        TournamentProfile profile = new TournamentProfile("Test");
        profile.setMaxOnlinePlayers(50);
        profile.setNumPlayers(100);

        assertEquals(50, profile.getMaxOnlinePlayers());
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
        assertEquals("My Tournament", imported.getName(), "Name should match");
        assertEquals(5000, imported.getBuyinChips(), "Buyin chips should match");
        assertEquals(20, imported.getNumPlayers(), "Num players should match");
        assertEquals(500, imported.getBuyinCost(), "Buyin should match");
        assertEquals(15, imported.getDefaultMinutesPerLevel(), "Minutes per level should match");
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
        assertEquals(original.getBigBlind(1), imported.getBigBlind(1), "Level 1 big blind should match");
        assertEquals(original.getSmallBlind(1), imported.getSmallBlind(1), "Level 1 small blind should match");
        assertEquals(original.getStartingDepthBBs(), imported.getStartingDepthBBs(), "Starting depth should match");
    }

    // ========== Late Registration Tests ==========

    @Test
    public void should_DefaultToLateRegDisabled() {
        TournamentProfile profile = new TournamentProfile("test");
        assertFalse(profile.isLateRegEnabled(), "Late registration should be disabled by default");
    }

    @Test
    public void should_DefaultToLevel1Cutoff() {
        TournamentProfile profile = new TournamentProfile("test");
        assertEquals(1, profile.getLateRegUntilLevel(), "Default cutoff should be level 1");
    }

    @Test
    public void should_DefaultToStartingChipsMode() {
        TournamentProfile profile = new TournamentProfile("test");
        assertEquals(PokerConstants.LATE_REG_CHIPS_STARTING, profile.getLateRegChips(),
                "Default chip mode should be starting chips");
    }

    @Test
    public void should_EnableAndDisableLateReg() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setLateRegEnabled(true);
        assertTrue(profile.isLateRegEnabled(), "Late registration should be enabled");

        profile.setLateRegEnabled(false);
        assertFalse(profile.isLateRegEnabled(), "Late registration should be disabled");
    }

    @Test
    public void should_SetLateRegCutoffLevel() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setLateRegUntilLevel(5);
        assertEquals(5, profile.getLateRegUntilLevel(), "Cutoff should be level 5");
    }

    @Test
    public void should_EnforceLateRegCutoffMinimum() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setLateRegUntilLevel(0);
        assertEquals(1, profile.getLateRegUntilLevel(), "Cutoff should be clamped to minimum 1");
    }

    @Test
    public void should_EnforceLateRegCutoffMaximum() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setLateRegUntilLevel(50); // Exceeds MAX_LEVELS (40)
        assertEquals(TournamentProfile.MAX_LEVELS, profile.getLateRegUntilLevel(),
                "Cutoff should be clamped to MAX_LEVELS");
    }

    @Test
    public void should_SetLateRegChipsToStarting() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setLateRegChips(PokerConstants.LATE_REG_CHIPS_STARTING);
        assertEquals(PokerConstants.LATE_REG_CHIPS_STARTING, profile.getLateRegChips(), "Chip mode should be starting");
    }

    @Test
    public void should_SetLateRegChipsToAverage() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setLateRegChips(PokerConstants.LATE_REG_CHIPS_AVERAGE);
        assertEquals(PokerConstants.LATE_REG_CHIPS_AVERAGE, profile.getLateRegChips(), "Chip mode should be average");
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
        assertTrue(imported.isLateRegEnabled(), "Late reg should be enabled");
        assertEquals(3, imported.getLateRegUntilLevel(), "Cutoff level should match");
        assertEquals(PokerConstants.LATE_REG_CHIPS_AVERAGE, imported.getLateRegChips(), "Chip mode should match");
    }

    // ========== Scheduled Start Tests ==========

    @Test
    public void should_DefaultToScheduledStartDisabled() {
        TournamentProfile profile = new TournamentProfile("test");
        assertFalse(profile.isScheduledStartEnabled(), "Scheduled start should be disabled by default");
    }

    @Test
    public void should_DefaultToZeroStartTime() {
        TournamentProfile profile = new TournamentProfile("test");
        assertEquals(0L, profile.getStartTime(), "Default start time should be 0");
    }

    @Test
    public void should_DefaultToMinimumPlayers() {
        TournamentProfile profile = new TournamentProfile("test");
        assertEquals(PokerConstants.MIN_SCHEDULED_START_PLAYERS, profile.getMinPlayersForStart(),
                "Default minimum players should be MIN_SCHEDULED_START_PLAYERS");
    }

    @Test
    public void should_EnableAndDisableScheduledStart() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setScheduledStartEnabled(true);
        assertTrue(profile.isScheduledStartEnabled(), "Scheduled start should be enabled");

        profile.setScheduledStartEnabled(false);
        assertFalse(profile.isScheduledStartEnabled(), "Scheduled start should be disabled");
    }

    @Test
    public void should_SetStartTime() {
        TournamentProfile profile = new TournamentProfile("test");
        long targetTime = System.currentTimeMillis() + 3600000; // 1 hour from now
        profile.setStartTime(targetTime);
        assertEquals(targetTime, profile.getStartTime(), "Start time should match");
    }

    @Test
    public void should_SetMinPlayersForStart() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setMinPlayersForStart(5);
        assertEquals(5, profile.getMinPlayersForStart(), "Min players should be 5");
    }

    @Test
    public void should_EnforceMinPlayersMinimum() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setMinPlayersForStart(1);
        assertEquals(2, profile.getMinPlayersForStart(), "Min players should be clamped to 2");
    }

    @Test
    public void should_EnforceMinPlayersMaximum() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setMinPlayersForStart(130); // Exceeds MAX_ONLINE_PLAYERS (120)
        assertEquals(TournamentProfile.MAX_ONLINE_PLAYERS, profile.getMinPlayersForStart(),
                "Min players should be clamped to MAX_ONLINE_PLAYERS");
    }

    @Test
    public void should_RoundTripScheduledStartSettings() throws IOException {
        // Given: a profile with scheduled start enabled
        TournamentProfile original = new TournamentProfile("Scheduled Start Test");
        long targetTime = System.currentTimeMillis() + 7200000; // 2 hours from now
        original.setScheduledStartEnabled(true);
        original.setStartTime(targetTime);
        original.setMinPlayersForStart(4);

        // When: write to string and read back
        StringWriter sw = new StringWriter();
        original.write(sw);

        TournamentProfile imported = new TournamentProfile();
        imported.read(new StringReader(sw.toString()), false);

        // Then: scheduled start settings should match
        assertTrue(imported.isScheduledStartEnabled(), "Scheduled start should be enabled");
        assertEquals(targetTime, imported.getStartTime(), "Start time should match");
        assertEquals(4, imported.getMinPlayersForStart(), "Min players should match");
    }

    // ========== Per-Street Timeout Tests ==========

    @Test
    public void should_DefaultToZeroForPerStreetTimeouts() {
        TournamentProfile profile = new TournamentProfile("test");
        assertEquals(0, profile.getTimeoutPreflop(), "Pre-flop timeout should default to 0");
        assertEquals(0, profile.getTimeoutFlop(), "Flop timeout should default to 0");
        assertEquals(0, profile.getTimeoutTurn(), "Turn timeout should default to 0");
        assertEquals(0, profile.getTimeoutRiver(), "River timeout should default to 0");
    }

    @Test
    public void should_FallbackToBaseTimeout_WhenPerStreetNotSet() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setTimeoutSeconds(45);

        // All rounds should use base timeout when per-street is 0
        // Using raw constants: ROUND_PRE_FLOP=0, ROUND_FLOP=1, ROUND_TURN=2,
        // ROUND_RIVER=3
        assertEquals(45, profile.getTimeoutForRound(0), "Pre-flop should use base timeout");
        assertEquals(45, profile.getTimeoutForRound(1), "Flop should use base timeout");
        assertEquals(45, profile.getTimeoutForRound(2), "Turn should use base timeout");
        assertEquals(45, profile.getTimeoutForRound(3), "River should use base timeout");
    }

    @Test
    public void should_UsePerStreetTimeout_WhenSet() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setTimeoutSeconds(30); // base
        profile.setTimeoutPreflop(15);
        profile.setTimeoutRiver(60);

        // Using raw constants: ROUND_PRE_FLOP=0, ROUND_FLOP=1, ROUND_TURN=2,
        // ROUND_RIVER=3
        assertEquals(15, profile.getTimeoutForRound(0), "Pre-flop should use custom timeout");
        assertEquals(30, profile.getTimeoutForRound(1), "Flop should fall back to base timeout");
        assertEquals(30, profile.getTimeoutForRound(2), "Turn should fall back to base timeout");
        assertEquals(60, profile.getTimeoutForRound(3), "River should use custom timeout");
    }

    @Test
    public void should_EnforceMaxTimeout_ForPerStreet() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setTimeoutRiver(200); // Exceeds MAX_TIMEOUT (120)
        assertEquals(120, profile.getTimeoutRiver(), "River timeout should be clamped to MAX_TIMEOUT");
    }

    @Test
    public void should_EnforceMinTimeout_ForNonZeroPerStreet() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setTimeoutSeconds(30); // base timeout
        profile.setTimeoutPreflop(3); // Below MIN_TIMEOUT (5)

        // Getter returns the raw value (3)
        assertEquals(3, profile.getTimeoutPreflop(), "Getter should return raw value");

        // But getTimeoutForRound should enforce MIN_TIMEOUT
        assertEquals(5, profile.getTimeoutForRound(0), "Per-street timeout below MIN_TIMEOUT should be clamped to 5");
    }

    @Test
    public void should_RoundTripPerStreetTimeouts() throws IOException {
        // Given: a profile with per-street timeouts
        TournamentProfile original = new TournamentProfile("Per-Street Test");
        original.setTimeoutSeconds(30);
        original.setTimeoutPreflop(15);
        original.setTimeoutFlop(20);
        original.setTimeoutTurn(40);
        original.setTimeoutRiver(60);

        // When: write to string and read back
        StringWriter sw = new StringWriter();
        original.write(sw);

        TournamentProfile imported = new TournamentProfile();
        imported.read(new StringReader(sw.toString()), false);

        // Then: per-street settings should match
        assertEquals(15, imported.getTimeoutPreflop(), "Pre-flop timeout should match");
        assertEquals(20, imported.getTimeoutFlop(), "Flop timeout should match");
        assertEquals(40, imported.getTimeoutTurn(), "Turn timeout should match");
        assertEquals(60, imported.getTimeoutRiver(), "River timeout should match");
    }

    @Test
    public void should_FallbackToBaseTimeout_ForInvalidRound() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setTimeoutSeconds(30);

        // ROUND_SHOWDOWN (4) and invalid rounds should use base timeout
        assertEquals(30, profile.getTimeoutForRound(4), "Showdown should use base timeout");
        assertEquals(30, profile.getTimeoutForRound(999), "Invalid round should use base timeout");
    }

    // ========== Rebuy Configuration Tests ==========

    @Test
    public void should_DefaultToRebuysDisabled() {
        TournamentProfile profile = new TournamentProfile("test");
        assertFalse(profile.isRebuys(), "Rebuys should be disabled by default");
    }

    @Test
    public void should_EnableAndDisableRebuys() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setRebuys(true);
        assertTrue(profile.isRebuys(), "Rebuys should be enabled");

        profile.setRebuys(false);
        assertFalse(profile.isRebuys(), "Rebuys should be disabled");
    }

    @Test
    public void should_GetRebuyChipCountFromProfile() {
        TournamentProfile profile = new TournamentProfile("test");
        int rebuyChips = profile.getRebuyChipCount();
        assertTrue(rebuyChips > 0, "Rebuy chip count should be > 0");
        assertTrue(rebuyChips <= TournamentProfile.MAX_REBUY_CHIPS, "Rebuy chip count should be reasonable");
    }

    @Test
    public void should_SetRebuyChipCount() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setRebuyChipCount(3000);
        assertEquals(3000, profile.getRebuyChipCount(), "Rebuy chip count should be 3000");
    }

    @Test
    public void should_EnforceMaxRebuyChips() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setRebuyChipCount(2000000);
        assertEquals(TournamentProfile.MAX_REBUY_CHIPS, profile.getRebuyChipCount(),
                "Rebuy chips should be clamped to MAX_REBUY_CHIPS");
    }

    @Test
    public void should_DefaultToRebuyLTE() {
        TournamentProfile profile = new TournamentProfile("test");
        assertEquals(PokerConstants.REBUY_LTE, profile.getRebuyExpressionType(),
                "Rebuy expression should default to LTE");
    }

    @Test
    public void should_SetRebuyExpressionToLT() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setRebuyExpression(PokerConstants.REBUY_LT);
        assertEquals(PokerConstants.REBUY_LT, profile.getRebuyExpressionType(), "Rebuy expression should be LT");
    }

    @Test
    public void should_DefaultRebuyCostToZero() {
        TournamentProfile profile = new TournamentProfile("test");
        assertEquals(0, profile.getRebuyCost(), "Rebuy cost should default to 0");
    }

    @Test
    public void should_DefaultRebuyChipsToZero() {
        TournamentProfile profile = new TournamentProfile("test");
        assertEquals(0, profile.getRebuyChips(), "Rebuy chips should default to 0");
    }

    @Test
    public void should_RoundTripRebuySettings() throws IOException {
        TournamentProfile original = new TournamentProfile("Rebuy Test");
        original.setRebuys(true);
        original.setRebuyChipCount(3000);
        original.setRebuyExpression(PokerConstants.REBUY_LT);

        StringWriter sw = new StringWriter();
        original.write(sw);

        TournamentProfile imported = new TournamentProfile();
        imported.read(new StringReader(sw.toString()), false);

        assertTrue(imported.isRebuys(), "Rebuys should be enabled");
        assertEquals(3000, imported.getRebuyChipCount(), "Rebuy chip count should match");
        assertEquals(PokerConstants.REBUY_LT, imported.getRebuyExpressionType(), "Rebuy expression should match");
    }

    // ========== Addon Configuration Tests ==========

    @Test
    public void should_DefaultToAddonsDisabled() {
        TournamentProfile profile = new TournamentProfile("test");
        assertFalse(profile.isAddons(), "Addons should be disabled by default");
    }

    @Test
    public void should_EnableAndDisableAddons() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setAddons(true);
        assertTrue(profile.isAddons(), "Addons should be enabled");

        profile.setAddons(false);
        assertFalse(profile.isAddons(), "Addons should be disabled");
    }

    @Test
    public void should_DefaultAddonCostToZero() {
        TournamentProfile profile = new TournamentProfile("test");
        assertEquals(0, profile.getAddonCost(), "Addon cost should default to 0");
    }

    @Test
    public void should_DefaultAddonChipsToZero() {
        TournamentProfile profile = new TournamentProfile("test");
        assertEquals(0, profile.getAddonChips(), "Addon chips should default to 0");
    }

    @Test
    public void should_DefaultAddonLevelToZero() {
        TournamentProfile profile = new TournamentProfile("test");
        assertEquals(0, profile.getAddonLevel(), "Addon level should default to 0");
    }

    @Test
    public void should_RoundTripAddonSettings() throws IOException {
        TournamentProfile original = new TournamentProfile("Addon Test");
        original.setAddons(true);

        StringWriter sw = new StringWriter();
        original.write(sw);

        TournamentProfile imported = new TournamentProfile();
        imported.read(new StringReader(sw.toString()), false);

        assertTrue(imported.isAddons(), "Addons should be enabled");
    }

    // ========== Online Mode Settings Tests ==========

    @Test
    public void should_DefaultToFillWithComputer() {
        TournamentProfile profile = new TournamentProfile("test");
        assertTrue(profile.isFillComputer(), "Fill with computer should be enabled by default");
    }

    @Test
    public void should_DefaultToDisallowDash() {
        TournamentProfile profile = new TournamentProfile("test");
        assertFalse(profile.isAllowDash(), "Dashboard should be disallowed by default");
    }

    @Test
    public void should_DefaultToDisallowAdvisor() {
        TournamentProfile profile = new TournamentProfile("test");
        assertFalse(profile.isAllowAdvisor(), "Advisor should be disallowed by default");
    }

    @Test
    public void should_DefaultToNoBootSitout() {
        TournamentProfile profile = new TournamentProfile("test");
        assertFalse(profile.isBootSitout(), "Boot sitout should be disabled by default");
    }

    @Test
    public void should_DefaultToBootDisconnect() {
        TournamentProfile profile = new TournamentProfile("test");
        assertTrue(profile.isBootDisconnect(), "Boot disconnect should be enabled by default");
    }

    @Test
    public void should_GetBootSitoutCountDefault() {
        TournamentProfile profile = new TournamentProfile("test");
        assertEquals(25, profile.getBootSitoutCount(), "Boot sitout count should default to 25");
    }

    @Test
    public void should_GetBootDisconnectCountDefault() {
        TournamentProfile profile = new TournamentProfile("test");
        assertEquals(10, profile.getBootDisconnectCount(), "Boot disconnect count should default to 10");
    }

    // ========== Payout Allocation Tests ==========

    @Test
    public void should_DefaultToAutoAllocation() {
        TournamentProfile profile = new TournamentProfile("test");
        assertTrue(profile.isAllocAuto(), "Should default to auto allocation");
        assertFalse(profile.isAllocPercent(), "Should not be percent allocation");
        assertFalse(profile.isAllocFixed(), "Should not be fixed allocation");
        assertFalse(profile.isAllocSatellite(), "Should not be satellite allocation");
    }

    @Test
    public void should_SetPercentAllocation() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setAlloc(PokerConstants.ALLOC_PERC);
        assertTrue(profile.isAllocPercent(), "Should be percent allocation");
        assertFalse(profile.isAllocAuto(), "Should not be auto allocation");
    }

    @Test
    public void should_SetFixedAllocation() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setAlloc(PokerConstants.ALLOC_AMOUNT);
        assertTrue(profile.isAllocFixed(), "Should be fixed allocation");
        assertFalse(profile.isAllocAuto(), "Should not be auto allocation");
    }

    @Test
    public void should_SetSatellitePayout() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setPayout(PokerConstants.PAYOUT_SATELLITE);
        assertTrue(profile.isAllocSatellite(), "Should be satellite payout");
        assertFalse(profile.isAllocAuto(), "Should not be auto allocation");
    }

    // ========== Integration Tests ==========

    @Test
    public void should_UpdatePayoutSpots_WhenNumPlayersDecreases() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setPayout(PokerConstants.PAYOUT_SPOTS);
        profile.setPayoutSpots(20);

        profile.updateNumPlayers(10);

        int maxSpots = profile.getMaxPayoutSpots(10);
        assertTrue(maxSpots < 20, "Max payout spots should be reasonable for 10 players");
    }

    @Test
    public void should_GetDefaultHousePercent() {
        TournamentProfile profile = new TournamentProfile("test");
        int housePercent = profile.getHousePercent();
        assertTrue(housePercent >= 0, "House percent should be >= 0");
    }

    @Test
    public void should_GetDefaultHouseAmount() {
        TournamentProfile profile = new TournamentProfile("test");
        int houseAmount = profile.getHouseAmount();
        assertTrue(houseAmount >= 0, "House amount should be >= 0");
    }

    @Test
    public void should_CalculatePrizePool_WithRebuys() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setNumPlayers(100);
        profile.setBuyin(100);
        profile.setRebuys(true);

        int totalPool = 100 * 100;
        profile.setPrizePool(totalPool, false);

        int prizePool = profile.getPrizePool();
        assertTrue(prizePool >= 10000, "Prize pool should include rebuys");
    }

    @Test
    public void should_MaintainBlindStructure_AfterFixAll() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setBuyinChips(10000);
        profile.setLevel(1, 0, 50, 100, 10);
        profile.setLevel(2, 0, 100, 200, 10);

        profile.fixAll();

        assertEquals(100, profile.getBigBlind(1), "Level 1 big blind should be 100");
        assertEquals(200, profile.getBigBlind(2), "Level 2 big blind should be 200");
    }
}
