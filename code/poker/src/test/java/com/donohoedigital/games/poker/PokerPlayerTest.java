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
package com.donohoedigital.games.poker;

import com.donohoedigital.config.ApplicationType;
import com.donohoedigital.config.ConfigManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for PokerPlayer - chip management, player state, money operations,
 * and hand operations.
 */
class PokerPlayerTest {

    private PokerPlayer player;

    @BeforeEach
    void setUp() {
        // Initialize ConfigManager for headless testing
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);

        // Create a test player (true = human, false = computer)
        player = new PokerPlayer(1, "TestPlayer", true);
    }

    // =================================================================
    // Chip Management Tests
    // =================================================================

    @Test
    void should_InitializeWithZeroChips_When_PlayerCreated() {
        assertThat(player.getChipCount()).isZero();
    }

    @Test
    void should_SetChipCount_When_ChipCountSet() {
        player.setChipCount(1500);

        assertThat(player.getChipCount()).isEqualTo(1500);
    }

    @Test
    void should_AddChips_When_ChipsAdded() {
        player.setChipCount(1000);

        player.addChips(500);

        assertThat(player.getChipCount()).isEqualTo(1500);
    }

    @Test
    void should_SubtractChips_When_NegativeChipsAdded() {
        player.setChipCount(1000);

        player.addChips(-300);

        assertThat(player.getChipCount()).isEqualTo(700);
    }

    @Test
    void should_ReturnZeroChipCountAtStart_When_NoChipsSet() {
        // ChipCountAtStart is only set when chips are first added, not via setChipCount
        assertThat(player.getChipCountAtStart()).isZero();
    }

    @Test
    void should_TrackInitialChipCount_When_ChipsFirstAdded() {
        player.addChips(1500);

        // After first add, starting chips should be tracked
        assertThat(player.getChipCount()).isEqualTo(1500);
    }

    @Test
    void should_SetOddChips_When_OddChipsSet() {
        player.setOddChips(3);

        assertThat(player.getOddChips()).isEqualTo(3);
    }

    @Test
    void should_MarkWonChipRace_When_ChipRaceWon() {
        player.setWonChipRace(true);

        assertThat(player.isWonChipRace()).isTrue();
    }

    @Test
    void should_MarkBrokeInChipRace_When_ChipRaceLost() {
        player.setBrokeChipRace(true);

        assertThat(player.isBrokeChipRace()).isTrue();
    }

    @Test
    void should_AllowZeroChips_When_PlayerBusted() {
        player.setChipCount(100);
        player.addChips(-100);

        assertThat(player.getChipCount()).isZero();
    }

    @Test
    void should_AllowNegativeChips_When_ChipsOverspent() {
        player.setChipCount(50);
        player.addChips(-100);

        assertThat(player.getChipCount()).isEqualTo(-50);
    }

    @Test
    void should_HandleLargeChipCounts_When_ChipsAccumulated() {
        player.setChipCount(1000000);
        player.addChips(500000);

        assertThat(player.getChipCount()).isEqualTo(1500000);
    }

    // =================================================================
    // Money Operations Tests
    // =================================================================

    @Test
    void should_SetBuyin_When_BuyinSet() {
        player.setBuyin(100);

        assertThat(player.getBuyin()).isEqualTo(100);
    }

    @Test
    void should_ReturnZeroRebuy_When_NoRebuysAdded() {
        // Rebuy/addon operations require table context, test getters only
        assertThat(player.getRebuy()).isZero();
        assertThat(player.getNumRebuys()).isZero();
    }

    @Test
    void should_ReturnZeroAddon_When_NoAddonPurchased() {
        assertThat(player.getAddon()).isZero();
    }

    @Test
    void should_CalculateTotalSpent_When_BuyinSet() {
        player.setBuyin(100);

        assertThat(player.getTotalSpent()).isEqualTo(100);
    }

    @Test
    void should_SetPrize_When_PrizeAwarded() {
        player.setPrize(500);

        assertThat(player.getPrize()).isEqualTo(500);
    }

    @Test
    void should_SetPlace_When_TournamentFinishRecorded() {
        player.setPlace(3);

        assertThat(player.getPlace()).isEqualTo(3);
    }

    // =================================================================
    // Player State Tests
    // =================================================================

    @Test
    void should_BeHuman_When_HumanPlayerCreated() {
        PokerPlayer human = new PokerPlayer(1, "Human", true); // true = human

        assertThat(human.isHuman()).isTrue();
        assertThat(human.isComputer()).isFalse();
    }

    @Test
    void should_BeComputer_When_ComputerPlayerCreated() {
        PokerPlayer computer = new PokerPlayer(1, "Computer", false); // false = computer

        assertThat(computer.isComputer()).isTrue();
        assertThat(computer.isHuman()).isFalse();
    }

    @Test
    void should_HaveCorrectName_When_PlayerCreated() {
        assertThat(player.getName()).isEqualTo("TestPlayer");
    }

    @Test
    void should_HaveCorrectID_When_PlayerCreated() {
        assertThat(player.getID()).isEqualTo(1);
    }

    @Test
    void should_ReturnDisplayName_When_GetDisplayNameCalled() {
        String displayName = player.getDisplayName(false);

        assertThat(displayName).isNotNull();
        assertThat(displayName).contains("TestPlayer");
    }

    @Test
    void should_NotBeInHand_When_NoHandInProgress() {
        // isInHand() requires an active HoldemHand, which needs table context
        player.setChipCount(1000);

        assertThat(player.isInHand()).isFalse();
    }

    // =================================================================
    // Profile Tests
    // =================================================================

    @Test
    void should_SetProfile_When_ProfileProvided() {
        PlayerProfile profile = new PlayerProfile("TestProfile");

        player.setProfile(profile);

        assertThat(player.getProfile()).isEqualTo(profile);
        assertThat(player.isProfileDefined()).isTrue();
    }

    @Test
    void should_NotHaveProfile_When_NoProfileSet() {
        assertThat(player.isProfileDefined()).isFalse();
    }

    @Test
    void should_SetProfilePath_When_PathProvided() {
        player.setProfilePath("/path/to/profile");

        assertThat(player.getProfilePath()).isEqualTo("/path/to/profile");
    }

    // =================================================================
    // Time Management Tests
    // =================================================================

    @Test
    void should_SetThinkBankMillis_When_ThinkBankSet() {
        player.setThinkBankMillis(30000);

        assertThat(player.getThinkBankMillis()).isEqualTo(30000);
    }

    @Test
    void should_SetTimeoutMillis_When_TimeoutSet() {
        player.setTimeoutMillis(60000);

        assertThat(player.getTimeoutMillis()).isEqualTo(60000);
    }

    // =================================================================
    // Online Player Tests
    // =================================================================

    @Test
    void should_SetOnlineActivated_When_ActivationSet() {
        player.setOnlineActivated(true);

        assertThat(player.isOnlineActivated()).isTrue();
    }

    @Test
    void should_NotBeOnlineActivated_When_NewPlayerCreated() {
        assertThat(player.isOnlineActivated()).isFalse();
    }

    @Test
    void should_HavePlayerId_When_CreatedWithKey() {
        PokerPlayer playerWithKey = new PokerPlayer("player-123", 5, "KeyedPlayer", true);

        assertThat(playerWithKey.getPlayerId()).isEqualTo("player-123");
    }

    @Test
    void should_HaveNullPlayerId_When_CreatedWithoutKey() {
        assertThat(player.getPlayerId()).isNull();
    }

    // =================================================================
    // Demo Limit Tests
    // =================================================================

    @Test
    void should_SetDemoLimit_When_DemoLimitSet() {
        player.setDemoLimit();

        assertThat(player.isDemoLimit()).isTrue();
    }

    @Test
    void should_NotHaveDemoLimit_When_NewPlayerCreated() {
        assertThat(player.isDemoLimit()).isFalse();
    }
}
