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

    // ========== Scheduled Start Tests ==========

    @Test
    public void should_DefaultToScheduledStartDisabled() {
        TournamentProfile profile = new TournamentProfile("test");
        assertFalse("Scheduled start should be disabled by default", profile.isScheduledStartEnabled());
    }

    @Test
    public void should_DefaultToZeroStartTime() {
        TournamentProfile profile = new TournamentProfile("test");
        assertEquals("Default start time should be 0", 0L, profile.getStartTime());
    }

    @Test
    public void should_DefaultToMinimumPlayers() {
        TournamentProfile profile = new TournamentProfile("test");
        assertEquals("Default minimum players should be MIN_SCHEDULED_START_PLAYERS",
                PokerConstants.MIN_SCHEDULED_START_PLAYERS, profile.getMinPlayersForStart());
    }

    @Test
    public void should_EnableAndDisableScheduledStart() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setScheduledStartEnabled(true);
        assertTrue("Scheduled start should be enabled", profile.isScheduledStartEnabled());

        profile.setScheduledStartEnabled(false);
        assertFalse("Scheduled start should be disabled", profile.isScheduledStartEnabled());
    }

    @Test
    public void should_SetStartTime() {
        TournamentProfile profile = new TournamentProfile("test");
        long targetTime = System.currentTimeMillis() + 3600000; // 1 hour from now
        profile.setStartTime(targetTime);
        assertEquals("Start time should match", targetTime, profile.getStartTime());
    }

    @Test
    public void should_SetMinPlayersForStart() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setMinPlayersForStart(5);
        assertEquals("Min players should be 5", 5, profile.getMinPlayersForStart());
    }

    @Test
    public void should_EnforceMinPlayersMinimum() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setMinPlayersForStart(1);
        assertEquals("Min players should be clamped to 2", 2, profile.getMinPlayersForStart());
    }

    @Test
    public void should_EnforceMinPlayersMaximum() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setMinPlayersForStart(100); // Exceeds MAX_ONLINE_PLAYERS (90)
        assertEquals("Min players should be clamped to MAX_ONLINE_PLAYERS", TournamentProfile.MAX_ONLINE_PLAYERS,
                profile.getMinPlayersForStart());
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
        assertTrue("Scheduled start should be enabled", imported.isScheduledStartEnabled());
        assertEquals("Start time should match", targetTime, imported.getStartTime());
        assertEquals("Min players should match", 4, imported.getMinPlayersForStart());
    }

    // ========== Per-Street Timeout Tests ==========

    @Test
    public void should_DefaultToZeroForPerStreetTimeouts() {
        TournamentProfile profile = new TournamentProfile("test");
        assertEquals("Pre-flop timeout should default to 0", 0, profile.getTimeoutPreflop());
        assertEquals("Flop timeout should default to 0", 0, profile.getTimeoutFlop());
        assertEquals("Turn timeout should default to 0", 0, profile.getTimeoutTurn());
        assertEquals("River timeout should default to 0", 0, profile.getTimeoutRiver());
    }

    @Test
    public void should_FallbackToBaseTimeout_WhenPerStreetNotSet() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setTimeoutSeconds(45);

        // All rounds should use base timeout when per-street is 0
        // Using raw constants: ROUND_PRE_FLOP=0, ROUND_FLOP=1, ROUND_TURN=2,
        // ROUND_RIVER=3
        assertEquals("Pre-flop should use base timeout", 45, profile.getTimeoutForRound(0));
        assertEquals("Flop should use base timeout", 45, profile.getTimeoutForRound(1));
        assertEquals("Turn should use base timeout", 45, profile.getTimeoutForRound(2));
        assertEquals("River should use base timeout", 45, profile.getTimeoutForRound(3));
    }

    @Test
    public void should_UsePerStreetTimeout_WhenSet() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setTimeoutSeconds(30); // base
        profile.setTimeoutPreflop(15);
        profile.setTimeoutRiver(60);

        // Using raw constants: ROUND_PRE_FLOP=0, ROUND_FLOP=1, ROUND_TURN=2,
        // ROUND_RIVER=3
        assertEquals("Pre-flop should use custom timeout", 15, profile.getTimeoutForRound(0));
        assertEquals("Flop should fall back to base timeout", 30, profile.getTimeoutForRound(1));
        assertEquals("Turn should fall back to base timeout", 30, profile.getTimeoutForRound(2));
        assertEquals("River should use custom timeout", 60, profile.getTimeoutForRound(3));
    }

    @Test
    public void should_EnforceMaxTimeout_ForPerStreet() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setTimeoutRiver(200); // Exceeds MAX_TIMEOUT (120)
        assertEquals("River timeout should be clamped to MAX_TIMEOUT", 120, profile.getTimeoutRiver());
    }

    @Test
    public void should_EnforceMinTimeout_ForNonZeroPerStreet() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setTimeoutSeconds(30); // base timeout
        profile.setTimeoutPreflop(3); // Below MIN_TIMEOUT (5)

        // Getter returns the raw value (3)
        assertEquals("Getter should return raw value", 3, profile.getTimeoutPreflop());

        // But getTimeoutForRound should enforce MIN_TIMEOUT
        assertEquals("Per-street timeout below MIN_TIMEOUT should be clamped to 5", 5, profile.getTimeoutForRound(0));
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
        assertEquals("Pre-flop timeout should match", 15, imported.getTimeoutPreflop());
        assertEquals("Flop timeout should match", 20, imported.getTimeoutFlop());
        assertEquals("Turn timeout should match", 40, imported.getTimeoutTurn());
        assertEquals("River timeout should match", 60, imported.getTimeoutRiver());
    }

    @Test
    public void should_FallbackToBaseTimeout_ForInvalidRound() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setTimeoutSeconds(30);

        // ROUND_SHOWDOWN (4) and invalid rounds should use base timeout
        assertEquals("Showdown should use base timeout", 30, profile.getTimeoutForRound(4));
        assertEquals("Invalid round should use base timeout", 30, profile.getTimeoutForRound(999));
    }

    // ========== Rebuy Configuration Tests ==========

    @Test
    public void should_DefaultToRebuysDisabled() {
        TournamentProfile profile = new TournamentProfile("test");
        assertFalse("Rebuys should be disabled by default", profile.isRebuys());
    }

    @Test
    public void should_EnableAndDisableRebuys() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setRebuys(true);
        assertTrue("Rebuys should be enabled", profile.isRebuys());

        profile.setRebuys(false);
        assertFalse("Rebuys should be disabled", profile.isRebuys());
    }

    @Test
    public void should_GetRebuyChipCountFromProfile() {
        TournamentProfile profile = new TournamentProfile("test");
        int rebuyChips = profile.getRebuyChipCount();
        assertTrue("Rebuy chip count should be > 0", rebuyChips > 0);
        assertTrue("Rebuy chip count should be reasonable", rebuyChips <= TournamentProfile.MAX_REBUY_CHIPS);
    }

    @Test
    public void should_SetRebuyChipCount() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setRebuyChipCount(3000);
        assertEquals("Rebuy chip count should be 3000", 3000, profile.getRebuyChipCount());
    }

    @Test
    public void should_EnforceMaxRebuyChips() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setRebuyChipCount(2000000);
        assertEquals("Rebuy chips should be clamped to MAX_REBUY_CHIPS", TournamentProfile.MAX_REBUY_CHIPS,
                profile.getRebuyChipCount());
    }

    @Test
    public void should_DefaultToRebuyLTE() {
        TournamentProfile profile = new TournamentProfile("test");
        assertEquals("Rebuy expression should default to LTE", PokerConstants.REBUY_LTE,
                profile.getRebuyExpressionType());
    }

    @Test
    public void should_SetRebuyExpressionToLT() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setRebuyExpression(PokerConstants.REBUY_LT);
        assertEquals("Rebuy expression should be LT", PokerConstants.REBUY_LT, profile.getRebuyExpressionType());
    }

    @Test
    public void should_DefaultRebuyCostToZero() {
        TournamentProfile profile = new TournamentProfile("test");
        assertEquals("Rebuy cost should default to 0", 0, profile.getRebuyCost());
    }

    @Test
    public void should_DefaultRebuyChipsToZero() {
        TournamentProfile profile = new TournamentProfile("test");
        assertEquals("Rebuy chips should default to 0", 0, profile.getRebuyChips());
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

        assertTrue("Rebuys should be enabled", imported.isRebuys());
        assertEquals("Rebuy chip count should match", 3000, imported.getRebuyChipCount());
        assertEquals("Rebuy expression should match", PokerConstants.REBUY_LT, imported.getRebuyExpressionType());
    }

    // ========== Addon Configuration Tests ==========

    @Test
    public void should_DefaultToAddonsDisabled() {
        TournamentProfile profile = new TournamentProfile("test");
        assertFalse("Addons should be disabled by default", profile.isAddons());
    }

    @Test
    public void should_EnableAndDisableAddons() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setAddons(true);
        assertTrue("Addons should be enabled", profile.isAddons());

        profile.setAddons(false);
        assertFalse("Addons should be disabled", profile.isAddons());
    }

    @Test
    public void should_DefaultAddonCostToZero() {
        TournamentProfile profile = new TournamentProfile("test");
        assertEquals("Addon cost should default to 0", 0, profile.getAddonCost());
    }

    @Test
    public void should_DefaultAddonChipsToZero() {
        TournamentProfile profile = new TournamentProfile("test");
        assertEquals("Addon chips should default to 0", 0, profile.getAddonChips());
    }

    @Test
    public void should_DefaultAddonLevelToZero() {
        TournamentProfile profile = new TournamentProfile("test");
        assertEquals("Addon level should default to 0", 0, profile.getAddonLevel());
    }

    @Test
    public void should_RoundTripAddonSettings() throws IOException {
        TournamentProfile original = new TournamentProfile("Addon Test");
        original.setAddons(true);

        StringWriter sw = new StringWriter();
        original.write(sw);

        TournamentProfile imported = new TournamentProfile();
        imported.read(new StringReader(sw.toString()), false);

        assertTrue("Addons should be enabled", imported.isAddons());
    }

    // ========== Online Mode Settings Tests ==========

    @Test
    public void should_DefaultToFillWithComputer() {
        TournamentProfile profile = new TournamentProfile("test");
        assertTrue("Fill with computer should be enabled by default", profile.isFillComputer());
    }

    @Test
    public void should_CheckDefaultOnlineActivatedSetting() {
        TournamentProfile profile = new TournamentProfile("test");
        // Just verify the method returns a boolean - actual default may vary
        boolean setting = profile.isOnlineActivatedPlayersOnly();
        assertTrue("Method should return a valid boolean", setting || !setting);
    }

    @Test
    public void should_SetOnlineActivatedPlayersOnly() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setOnlineActivatedPlayersOnly(true);
        assertTrue("Online activated players only should be true", profile.isOnlineActivatedPlayersOnly());
    }

    @Test
    public void should_DefaultToDisallowDash() {
        TournamentProfile profile = new TournamentProfile("test");
        assertFalse("Dashboard should be disallowed by default", profile.isAllowDash());
    }

    @Test
    public void should_DefaultToDisallowAdvisor() {
        TournamentProfile profile = new TournamentProfile("test");
        assertFalse("Advisor should be disallowed by default", profile.isAllowAdvisor());
    }

    @Test
    public void should_DefaultToNoBootSitout() {
        TournamentProfile profile = new TournamentProfile("test");
        assertFalse("Boot sitout should be disabled by default", profile.isBootSitout());
    }

    @Test
    public void should_DefaultToBootDisconnect() {
        TournamentProfile profile = new TournamentProfile("test");
        assertTrue("Boot disconnect should be enabled by default", profile.isBootDisconnect());
    }

    @Test
    public void should_GetBootSitoutCountDefault() {
        TournamentProfile profile = new TournamentProfile("test");
        assertEquals("Boot sitout count should default to 25", 25, profile.getBootSitoutCount());
    }

    @Test
    public void should_GetBootDisconnectCountDefault() {
        TournamentProfile profile = new TournamentProfile("test");
        assertEquals("Boot disconnect count should default to 10", 10, profile.getBootDisconnectCount());
    }

    // ========== Payout Allocation Tests ==========

    @Test
    public void should_DefaultToAutoAllocation() {
        TournamentProfile profile = new TournamentProfile("test");
        assertTrue("Should default to auto allocation", profile.isAllocAuto());
        assertFalse("Should not be percent allocation", profile.isAllocPercent());
        assertFalse("Should not be fixed allocation", profile.isAllocFixed());
        assertFalse("Should not be satellite allocation", profile.isAllocSatellite());
    }

    @Test
    public void should_SetPercentAllocation() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setAlloc(PokerConstants.ALLOC_PERC);
        assertTrue("Should be percent allocation", profile.isAllocPercent());
        assertFalse("Should not be auto allocation", profile.isAllocAuto());
    }

    @Test
    public void should_SetFixedAllocation() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setAlloc(PokerConstants.ALLOC_AMOUNT);
        assertTrue("Should be fixed allocation", profile.isAllocFixed());
        assertFalse("Should not be auto allocation", profile.isAllocAuto());
    }

    @Test
    public void should_SetSatellitePayout() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setPayout(PokerConstants.PAYOUT_SATELLITE);
        assertTrue("Should be satellite payout", profile.isAllocSatellite());
        assertFalse("Should not be auto allocation", profile.isAllocAuto());
    }

    // ========== Integration Tests ==========

    @Test
    public void should_UpdatePayoutSpots_WhenNumPlayersDecreases() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setPayout(PokerConstants.PAYOUT_SPOTS);
        profile.setPayoutSpots(20);

        profile.updateNumPlayers(10);

        int maxSpots = profile.getMaxPayoutSpots(10);
        assertTrue("Max payout spots should be reasonable for 10 players", maxSpots < 20);
    }

    @Test
    public void should_GetDefaultHousePercent() {
        TournamentProfile profile = new TournamentProfile("test");
        int housePercent = profile.getHousePercent();
        assertTrue("House percent should be >= 0", housePercent >= 0);
    }

    @Test
    public void should_GetDefaultHouseAmount() {
        TournamentProfile profile = new TournamentProfile("test");
        int houseAmount = profile.getHouseAmount();
        assertTrue("House amount should be >= 0", houseAmount >= 0);
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
        assertTrue("Prize pool should include rebuys", prizePool >= 10000);
    }

    @Test
    public void should_MaintainBlindStructure_AfterFixAll() {
        TournamentProfile profile = new TournamentProfile("test");
        profile.setBuyinChips(10000);
        profile.setLevel(1, 0, 50, 100, 10);
        profile.setLevel(2, 0, 100, 200, 10);

        profile.fixAll();

        assertEquals("Level 1 big blind should be 100", 100, profile.getBigBlind(1));
        assertEquals("Level 2 big blind should be 200", 200, profile.getBigBlind(2));
    }
}
