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
package com.donohoedigital.games.poker.logic;

import com.donohoedigital.config.ApplicationType;
import com.donohoedigital.config.ConfigManager;
import com.donohoedigital.games.poker.PokerGame;
import com.donohoedigital.games.poker.PokerPlayer;
import com.donohoedigital.games.poker.PokerTable;
import com.donohoedigital.games.poker.logic.GameOverChecker.GameOverResult;
import com.donohoedigital.games.poker.model.TournamentProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for GameOverChecker - game-over detection logic extracted from
 * CheckEndHand.java. Tests run in headless mode with no UI dependencies. Part
 * of Wave 2 testability refactoring.
 */
@Tag("unit")
class GameOverCheckerTest {

    private PokerGame game;
    private PokerTable table;
    private PokerPlayer human;
    private PokerPlayer ai1;
    private PokerPlayer ai2;

    @BeforeEach
    void setUp() {
        // Initialize ConfigManager for headless testing
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);

        // Create game and table with rebuy enabled
        game = new PokerGame(null);
        TournamentProfile profile = new TournamentProfile("test");
        profile.setBuyinChips(1500);
        profile.setRebuys(true);
        profile.setRebuyChipCount(1500); // Allow rebuy if chips <= 1500
        profile.setLastRebuyLevel(5); // Allow rebuys through level 5
        profile.fixAll(); // Ensure all settings are properly initialized
        game.setProfile(profile);
        table = new PokerTable(game, 1);
        table.setMinChip(5);

        // Create human player (first human added becomes the human player)
        human = new PokerPlayer(1, "Human", true);
        human.setChipCount(1000);
        game.addPlayer(human);
        table.setPlayer(human, 0);

        // Create AI players
        ai1 = new PokerPlayer(2, "AI1", false);
        ai1.setChipCount(1000);
        game.addPlayer(ai1);
        table.setPlayer(ai1, 1);

        ai2 = new PokerPlayer(3, "AI2", false);
        ai2.setChipCount(1000);
        game.addPlayer(ai2);
        table.setPlayer(ai2, 2);
    }

    // =================================================================
    // shouldOfferRebuy() Tests
    // =================================================================

    @Test
    void should_ReturnTrue_When_PlayerBrokeNotObserverAndRebuyAllowed() {
        human.setChipCount(0);

        boolean result = GameOverChecker.shouldOfferRebuy(human, table);

        assertThat(result).isTrue();
    }

    @Test
    void should_ReturnFalse_When_PlayerHasChips() {
        human.setChipCount(500);

        boolean result = GameOverChecker.shouldOfferRebuy(human, table);

        assertThat(result).isFalse();
    }

    @Test
    void should_ReturnFalse_When_PlayerIsObserver() {
        human.setChipCount(0);
        human.setObserver(true);

        boolean result = GameOverChecker.shouldOfferRebuy(human, table);

        assertThat(result).isFalse();
    }

    @Test
    void should_ReturnFalse_When_RebuyNotAllowed() {
        human.setChipCount(0);
        // Disable rebuys for this test
        game.getProfile().setRebuys(false);

        boolean result = GameOverChecker.shouldOfferRebuy(human, table);

        assertThat(result).isFalse();
    }

    // =================================================================
    // isHumanBroke() Tests
    // =================================================================

    @Test
    void should_ReturnTrue_When_PlayerHasNoChipsAndNotObserver() {
        human.setChipCount(0);

        boolean result = GameOverChecker.isHumanBroke(human);

        assertThat(result).isTrue();
    }

    @Test
    void isHumanBroke_should_ReturnFalse_When_PlayerHasChips() {
        human.setChipCount(100);

        boolean result = GameOverChecker.isHumanBroke(human);

        assertThat(result).isFalse();
    }

    @Test
    void isHumanBroke_should_ReturnFalse_When_PlayerIsObserver() {
        human.setChipCount(0);
        human.setObserver(true);

        boolean result = GameOverChecker.isHumanBroke(human);

        assertThat(result).isFalse();
    }

    // =================================================================
    // checkGameOverStatus() Tests
    // =================================================================

    @Test
    void should_ReturnRebuyOffered_When_HumanBrokeAndCanRebuy() {
        human.setChipCount(0);
        // Rebuy is already enabled in setUp

        GameOverResult result = GameOverChecker.checkGameOverStatus(game, human, table, false);

        assertThat(result).isEqualTo(GameOverResult.REBUY_OFFERED);
    }

    @Test
    void should_ReturnGameOver_When_HumanBrokeCannotRebuyNotOnline() {
        human.setChipCount(0);
        game.getProfile().setRebuys(false);
        // game is offline by default

        GameOverResult result = GameOverChecker.checkGameOverStatus(game, human, table, false);

        assertThat(result).isEqualTo(GameOverResult.GAME_OVER);
    }

    @Test
    void should_ReturnNeverBrokeActive_When_HumanBrokeAndCheatEnabled() {
        human.setChipCount(0);
        game.getProfile().setRebuys(false);

        GameOverResult result = GameOverChecker.checkGameOverStatus(game, human, table, true);

        assertThat(result).isEqualTo(GameOverResult.NEVER_BROKE_ACTIVE);
    }

    @Test
    void should_ReturnTournamentWon_When_OnePlayerLeft() {
        // Eliminate AI players
        ai1.setChipCount(0);
        ai2.setChipCount(0);

        GameOverResult result = GameOverChecker.checkGameOverStatus(game, human, table, false);

        assertThat(result).isEqualTo(GameOverResult.TOURNAMENT_WON);
    }

    @Test
    void should_ReturnContinue_When_GameInProgress() {
        // All players have chips - game continues
        human.setChipCount(1000);
        ai1.setChipCount(1000);
        ai2.setChipCount(1000);

        GameOverResult result = GameOverChecker.checkGameOverStatus(game, human, table, false);

        assertThat(result).isEqualTo(GameOverResult.CONTINUE);
    }

    @Test
    void should_ReturnContinue_When_HumanBrokeButIsObserver() {
        human.setChipCount(0);
        human.setObserver(true);

        GameOverResult result = GameOverChecker.checkGameOverStatus(game, human, table, false);

        assertThat(result).isEqualTo(GameOverResult.CONTINUE);
    }

    // =================================================================
    // calculateNeverBrokeTransfer() Tests
    // =================================================================

    @Test
    void should_CalculateHalfOfLeaderChips_When_ExactlyDivisible() {
        int transfer = GameOverChecker.calculateNeverBrokeTransfer(1000, 5);

        // Half of 1000 = 500, which is divisible by 5
        assertThat(transfer).isEqualTo(500);
    }

    @Test
    void should_RoundDownToMinChip_When_NotExactlyDivisible() {
        int transfer = GameOverChecker.calculateNeverBrokeTransfer(1003, 5);

        // Half of 1003 = 501.5 → 501
        // 501 % 5 = 1, so round down: 501 - 1 = 500
        assertThat(transfer).isEqualTo(500);
    }

    @Test
    void should_HandleLargeChipAmounts() {
        int transfer = GameOverChecker.calculateNeverBrokeTransfer(10000, 25);

        // Half of 10000 = 5000, which is divisible by 25
        assertThat(transfer).isEqualTo(5000);
    }

    @Test
    void should_HandleSmallChipAmounts() {
        int transfer = GameOverChecker.calculateNeverBrokeTransfer(100, 10);

        // Half of 100 = 50, which is divisible by 10
        assertThat(transfer).isEqualTo(50);
    }

    @Test
    void should_RoundDownWithMinChipOf100() {
        int transfer = GameOverChecker.calculateNeverBrokeTransfer(5550, 100);

        // Half of 5550 = 2775
        // 2775 % 100 = 75, so round down: 2775 - 75 = 2700
        assertThat(transfer).isEqualTo(2700);
    }

    @Test
    void should_HandleOddChipLeaderAmount() {
        int transfer = GameOverChecker.calculateNeverBrokeTransfer(999, 5);

        // Half of 999 = 499.5 → 499 (integer division)
        // 499 % 5 = 4, so round down: 499 - 4 = 495
        assertThat(transfer).isEqualTo(495);
    }

    @Test
    void should_ReturnZero_When_ChipLeaderHasVeryFewChips() {
        int transfer = GameOverChecker.calculateNeverBrokeTransfer(7, 10);

        // Half of 7 = 3 (integer division)
        // 3 % 10 = 3, so round down: 3 - 3 = 0
        assertThat(transfer).isEqualTo(0);
    }

    // =================================================================
    // Edge Cases
    // =================================================================

    @Test
    void should_HandleMinChipOfOne() {
        int transfer = GameOverChecker.calculateNeverBrokeTransfer(1000, 1);

        // Half of 1000 = 500
        // Any amount is divisible by 1
        assertThat(transfer).isEqualTo(500);
    }
}
