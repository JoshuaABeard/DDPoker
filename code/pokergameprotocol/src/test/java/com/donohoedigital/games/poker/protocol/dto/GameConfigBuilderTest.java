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
package com.donohoedigital.games.poker.protocol.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class GameConfigBuilderTest {

    /**
     * Create a fully-featured profile with all optional features enabled.
     */
    private TournamentProfileData fullProfile() {
        List<BlindLevelData> blinds = List.of(new BlindLevelData(1, 25, 50, 0, 20, false, "NOLIMIT_HOLDEM"),
                new BlindLevelData(2, 50, 100, 10, 20, false, "POTLIMIT_HOLDEM"),
                new BlindLevelData(3, 0, 0, 0, 10, true, null),
                new BlindLevelData(4, 100, 200, 25, 15, false, "LIMIT_HOLDEM"));

        return new TournamentProfileData("Friday Night Poker", "Weekly tournament", "Welcome ${name}!", 90, 90, 10,
                true, 1000, 5000, blinds, true, "NOLIMIT_HOLDEM", "TIME", 10, 20,
                // Rebuys enabled
                true, 500, 2500, 5000, 3, 5, "LESS_THAN",
                // Addons enabled
                true, 500, 2500, 6,
                // Payout
                "SPOTS", 10.0, 50000, 9, List.of(30.0, 20.0, 15.0), "AUTO",
                // House
                "PERCENT", 5.0, 0,
                // Bounty enabled
                true, 50,
                // Timeouts
                30, 25, 20, 15, 10, 60,
                // Boot
                true, 25, true, 10,
                // Online
                true, true, 100,
                // Betting
                4, true,
                // Late registration enabled
                true, 3, "STARTING",
                // Scheduled start enabled
                true, Instant.parse("2026-03-15T19:00:00Z"), 10,
                // Invite enabled
                true, List.of("player1", "player2"), false);
    }

    /**
     * Create a minimal profile with all optional features disabled.
     */
    private TournamentProfileData minimalProfile() {
        List<BlindLevelData> blinds = List.of(new BlindLevelData(1, 10, 20, 0, 15, false, "NOLIMIT_HOLDEM"));

        return new TournamentProfileData("Simple Game", "Basic", null, 10, 10, 10, true, 100, 1000, blinds, false,
                "NOLIMIT_HOLDEM", "TIME", 10, 15,
                // Rebuys disabled
                false, 0, 0, 0, 0, 0, null,
                // Addons disabled
                false, 0, 0, 0,
                // Payout
                "SPOTS", 0.0, 0, 1, List.of(100.0), "AUTO",
                // House (both zero => null)
                "PERCENT", 0.0, 0,
                // Bounty disabled
                false, 0,
                // Timeouts
                30, 0, 0, 0, 0, 15,
                // Boot
                false, 0, false, 0,
                // Online
                false, false, 0,
                // Betting
                4, true,
                // Late registration disabled
                false, 0, null,
                // Scheduled start disabled
                false, null, 0,
                // Invite disabled
                false, null, false);
    }

    @Test
    void fromProfile_basicFields() {
        GameConfig config = GameConfigBuilder.fromProfile(fullProfile());

        assertEquals("Friday Night Poker", config.name());
        assertEquals("Weekly tournament", config.description());
        assertEquals("Welcome ${name}!", config.greeting());
        assertEquals(90, config.maxPlayers());
        assertEquals(90, config.maxOnlinePlayers());
        assertTrue(config.fillComputer());
        assertEquals(1000, config.buyIn());
        assertEquals(5000, config.startingChips());
        assertTrue(config.doubleAfterLastLevel());
        assertEquals("NOLIMIT_HOLDEM", config.defaultGameType());
        assertEquals(GameConfig.LevelAdvanceMode.TIME, config.levelAdvanceMode());
        assertEquals(10, config.handsPerLevel());
        assertEquals(20, config.defaultMinutesPerLevel());
        assertTrue(config.allowDash());
        assertTrue(config.allowAdvisor());
        assertNull(config.aiPlayers());
        assertNull(config.humanDisplayName());
        assertNull(config.practiceConfig());
    }

    @Test
    void fromProfile_blindStructureMapping() {
        GameConfig config = GameConfigBuilder.fromProfile(fullProfile());
        List<GameConfig.BlindLevel> blinds = config.blindStructure();

        assertEquals(4, blinds.size());

        // Level 1: normal
        GameConfig.BlindLevel bl1 = blinds.get(0);
        assertEquals(25, bl1.smallBlind());
        assertEquals(50, bl1.bigBlind());
        assertEquals(0, bl1.ante());
        assertEquals(20, bl1.minutes());
        assertFalse(bl1.isBreak());
        assertEquals("NOLIMIT_HOLDEM", bl1.gameType());

        // Level 2: normal with ante and different game type
        GameConfig.BlindLevel bl2 = blinds.get(1);
        assertEquals(50, bl2.smallBlind());
        assertEquals(100, bl2.bigBlind());
        assertEquals(10, bl2.ante());
        assertFalse(bl2.isBreak());
        assertEquals("POTLIMIT_HOLDEM", bl2.gameType());

        // Level 3: break
        GameConfig.BlindLevel bl3 = blinds.get(2);
        assertEquals(0, bl3.smallBlind());
        assertEquals(0, bl3.bigBlind());
        assertEquals(0, bl3.ante());
        assertEquals(10, bl3.minutes());
        assertTrue(bl3.isBreak());
        assertNull(bl3.gameType());

        // Level 4: normal with limit game type
        GameConfig.BlindLevel bl4 = blinds.get(3);
        assertEquals(100, bl4.smallBlind());
        assertEquals(200, bl4.bigBlind());
        assertEquals(25, bl4.ante());
        assertFalse(bl4.isBreak());
        assertEquals("LIMIT_HOLDEM", bl4.gameType());
    }

    @Test
    void fromProfile_handsAdvanceMode() {
        TournamentProfileData profile = new TournamentProfileData("Test", "Test", null, 10, 10, 10, true, 100, 1000,
                List.of(new BlindLevelData(1, 10, 20, 0, 15, false, "NOLIMIT_HOLDEM")), false, "NOLIMIT_HOLDEM",
                "HANDS", 25, 15, false, 0, 0, 0, 0, 0, null, false, 0, 0, 0, "SPOTS", 0.0, 0, 1, List.of(100.0), "AUTO",
                "PERCENT", 0.0, 0, false, 0, 30, 0, 0, 0, 0, 15, false, 0, false, 0, false, false, 0, 4, true, false, 0,
                null, false, null, 0, false, null, false);

        GameConfig config = GameConfigBuilder.fromProfile(profile);
        assertEquals(GameConfig.LevelAdvanceMode.HANDS, config.levelAdvanceMode());
    }

    @Test
    void fromProfile_rebuysEnabled() {
        GameConfig config = GameConfigBuilder.fromProfile(fullProfile());
        GameConfig.RebuyConfig rebuys = config.rebuys();

        assertNotNull(rebuys);
        assertTrue(rebuys.enabled());
        assertEquals(500, rebuys.cost());
        assertEquals(2500, rebuys.chips());
        assertEquals(5000, rebuys.chipCount());
        assertEquals(3, rebuys.maxRebuys());
        assertEquals(5, rebuys.lastLevel());
        assertEquals("LESS_THAN", rebuys.expressionType());
    }

    @Test
    void fromProfile_rebuysDisabled() {
        GameConfig config = GameConfigBuilder.fromProfile(minimalProfile());
        assertNull(config.rebuys());
    }

    @Test
    void fromProfile_addonsEnabled() {
        GameConfig config = GameConfigBuilder.fromProfile(fullProfile());
        GameConfig.AddonConfig addons = config.addons();

        assertNotNull(addons);
        assertTrue(addons.enabled());
        assertEquals(500, addons.cost());
        assertEquals(2500, addons.chips());
        assertEquals(6, addons.level());
    }

    @Test
    void fromProfile_addonsDisabled() {
        GameConfig config = GameConfigBuilder.fromProfile(minimalProfile());
        assertNull(config.addons());
    }

    @Test
    void fromProfile_payoutConfig() {
        GameConfig config = GameConfigBuilder.fromProfile(fullProfile());
        GameConfig.PayoutConfig payout = config.payout();

        assertNotNull(payout);
        assertEquals("SPOTS", payout.type());
        assertEquals(9, payout.spots());
        assertEquals(10, payout.percent());
        assertEquals(50000, payout.prizePool());
        assertEquals("AUTO", payout.allocationType());
        assertEquals(List.of(30.0, 20.0, 15.0), payout.spotAllocations());
    }

    @Test
    void fromProfile_houseEnabled() {
        GameConfig config = GameConfigBuilder.fromProfile(fullProfile());
        GameConfig.HouseConfig house = config.house();

        assertNotNull(house);
        assertEquals("PERCENT", house.cutType());
        assertEquals(5, house.percent());
        assertEquals(0, house.amount());
    }

    @Test
    void fromProfile_houseDisabledWhenBothZero() {
        GameConfig config = GameConfigBuilder.fromProfile(minimalProfile());
        assertNull(config.house());
    }

    @Test
    void fromProfile_houseEnabledWithAmountOnly() {
        TournamentProfileData profile = new TournamentProfileData("Test", "Test", null, 10, 10, 10, true, 100, 1000,
                List.of(new BlindLevelData(1, 10, 20, 0, 15, false, "NOLIMIT_HOLDEM")), false, "NOLIMIT_HOLDEM", "TIME",
                10, 15, false, 0, 0, 0, 0, 0, null, false, 0, 0, 0, "SPOTS", 0.0, 0, 1, List.of(100.0), "AUTO",
                "AMOUNT", 0.0, 100, false, 0, 30, 0, 0, 0, 0, 15, false, 0, false, 0, false, false, 0, 4, true, false,
                0, null, false, null, 0, false, null, false);

        GameConfig config = GameConfigBuilder.fromProfile(profile);
        assertNotNull(config.house());
        assertEquals("AMOUNT", config.house().cutType());
        assertEquals(0, config.house().percent());
        assertEquals(100, config.house().amount());
    }

    @Test
    void fromProfile_bountyEnabled() {
        GameConfig config = GameConfigBuilder.fromProfile(fullProfile());
        GameConfig.BountyConfig bounty = config.bounty();

        assertNotNull(bounty);
        assertTrue(bounty.enabled());
        assertEquals(50, bounty.amount());
    }

    @Test
    void fromProfile_bountyDisabled() {
        GameConfig config = GameConfigBuilder.fromProfile(minimalProfile());
        assertNull(config.bounty());
    }

    @Test
    void fromProfile_timeoutConfig() {
        GameConfig config = GameConfigBuilder.fromProfile(fullProfile());
        GameConfig.TimeoutConfig timeouts = config.timeouts();

        assertNotNull(timeouts);
        assertEquals(30, timeouts.defaultSeconds());
        assertEquals(25, timeouts.preflopSeconds());
        assertEquals(20, timeouts.flopSeconds());
        assertEquals(15, timeouts.turnSeconds());
        assertEquals(10, timeouts.riverSeconds());
        assertEquals(60, timeouts.thinkBankSeconds());
    }

    @Test
    void fromProfile_bootConfig() {
        GameConfig config = GameConfigBuilder.fromProfile(fullProfile());
        GameConfig.BootConfig boot = config.boot();

        assertNotNull(boot);
        assertTrue(boot.bootSitout());
        assertEquals(25, boot.bootSitoutCount());
        assertTrue(boot.bootDisconnect());
        assertEquals(10, boot.bootDisconnectCount());
    }

    @Test
    void fromProfile_lateRegistrationEnabled() {
        GameConfig config = GameConfigBuilder.fromProfile(fullProfile());
        GameConfig.LateRegistrationConfig lateReg = config.lateRegistration();

        assertNotNull(lateReg);
        assertTrue(lateReg.enabled());
        assertEquals(3, lateReg.untilLevel());
        assertEquals("STARTING", lateReg.chipMode());
    }

    @Test
    void fromProfile_lateRegistrationDisabled() {
        GameConfig config = GameConfigBuilder.fromProfile(minimalProfile());
        assertNull(config.lateRegistration());
    }

    @Test
    void fromProfile_scheduledStartEnabled() {
        GameConfig config = GameConfigBuilder.fromProfile(fullProfile());
        GameConfig.ScheduledStartConfig scheduled = config.scheduledStart();

        assertNotNull(scheduled);
        assertTrue(scheduled.enabled());
        assertEquals(Instant.parse("2026-03-15T19:00:00Z"), scheduled.startTime());
        assertEquals(10, scheduled.minPlayers());
    }

    @Test
    void fromProfile_scheduledStartDisabled() {
        GameConfig config = GameConfigBuilder.fromProfile(minimalProfile());
        assertNull(config.scheduledStart());
    }

    @Test
    void fromProfile_inviteEnabled() {
        GameConfig config = GameConfigBuilder.fromProfile(fullProfile());
        GameConfig.InviteConfig invite = config.invite();

        assertNotNull(invite);
        assertTrue(invite.inviteOnly());
        assertEquals(List.of("player1", "player2"), invite.invitees());
        assertFalse(invite.observersPublic());
    }

    @Test
    void fromProfile_inviteDisabled() {
        GameConfig config = GameConfigBuilder.fromProfile(minimalProfile());
        assertNull(config.invite());
    }

    @Test
    void fromProfile_bettingConfig() {
        GameConfig config = GameConfigBuilder.fromProfile(fullProfile());
        GameConfig.BettingConfig betting = config.betting();

        assertNotNull(betting);
        assertEquals(4, betting.maxRaises());
        assertTrue(betting.raiseCapIgnoredHeadsUp());
    }

    @Test
    void fromProfile_greetingNormalization() {
        // Null greeting
        GameConfig config1 = GameConfigBuilder.fromProfile(minimalProfile());
        assertNull(config1.greeting());

        // Whitespace-only greeting
        TournamentProfileData whitespaceGreeting = new TournamentProfileData("Test", "Test", "   ", 10, 10, 10, true,
                100, 1000, List.of(new BlindLevelData(1, 10, 20, 0, 15, false, "NOLIMIT_HOLDEM")), false,
                "NOLIMIT_HOLDEM", "TIME", 10, 15, false, 0, 0, 0, 0, 0, null, false, 0, 0, 0, "SPOTS", 0.0, 0, 1,
                List.of(100.0), "AUTO", "PERCENT", 0.0, 0, false, 0, 30, 0, 0, 0, 0, 15, false, 0, false, 0, false,
                false, 0, 4, true, false, 0, null, false, null, 0, false, null, false);
        GameConfig config2 = GameConfigBuilder.fromProfile(whitespaceGreeting);
        assertNull(config2.greeting());
    }

    @Test
    void fromProfile_nullBlindLevelsProducesEmptyList() {
        TournamentProfileData profile = new TournamentProfileData("Test", "Test", null, 10, 10, 10, true, 100, 1000,
                null, false, "NOLIMIT_HOLDEM", "TIME", 10, 15, false, 0, 0, 0, 0, 0, null, false, 0, 0, 0, "SPOTS", 0.0,
                0, 1, null, "AUTO", "PERCENT", 0.0, 0, false, 0, 30, 0, 0, 0, 0, 15, false, 0, false, 0, false, false,
                0, 4, true, false, 0, null, false, null, 0, false, null, false);

        GameConfig config = GameConfigBuilder.fromProfile(profile);
        assertNotNull(config.blindStructure());
        assertTrue(config.blindStructure().isEmpty());
    }

    @Test
    void buildAiPlayers_createsCorrectList() {
        TournamentProfileData profile = minimalProfile();
        List<String> names = List.of("Bot Alice", "Bot Bob", "Bot Charlie");

        List<GameConfig.AIPlayerConfig> aiPlayers = GameConfigBuilder.buildAiPlayers(profile, names, 5);

        assertEquals(3, aiPlayers.size());
        assertEquals("Bot Alice", aiPlayers.get(0).name());
        assertEquals(5, aiPlayers.get(0).skillLevel());
        assertEquals("Bot Bob", aiPlayers.get(1).name());
        assertEquals(5, aiPlayers.get(1).skillLevel());
        assertEquals("Bot Charlie", aiPlayers.get(2).name());
        assertEquals(5, aiPlayers.get(2).skillLevel());
    }

    @Test
    void buildAiPlayers_emptyList() {
        List<GameConfig.AIPlayerConfig> aiPlayers = GameConfigBuilder.buildAiPlayers(minimalProfile(), List.of(), 4);
        assertTrue(aiPlayers.isEmpty());
    }
}
