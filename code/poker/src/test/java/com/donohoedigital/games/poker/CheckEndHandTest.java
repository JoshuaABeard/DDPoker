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
import com.donohoedigital.games.poker.model.TournamentProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class CheckEndHandTest {

    private PokerGame game;
    private TournamentProfile profile;

    @BeforeEach
    void setUp() {
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);

        game = new PokerGame(null);
        profile = new TournamentProfile("test");
        profile.setBuyinChips(1500);
        game.setProfile(profile);
    }

    // =================================================================
    // isHumanBroke tests
    // =================================================================

    @Test
    void should_ReturnTrue_When_PlayerHasZeroChipsAndNotObserver() {
        PokerPlayer player = new PokerPlayer(0, "Human", true);
        player.setChipCount(0);

        assertThat(CheckEndHand.isHumanBroke(player)).isTrue();
    }

    @Test
    void should_ReturnFalse_When_PlayerHasChips() {
        PokerPlayer player = new PokerPlayer(0, "Human", true);
        player.setChipCount(500);

        assertThat(CheckEndHand.isHumanBroke(player)).isFalse();
    }

    @Test
    void should_ReturnFalse_When_PlayerIsObserver() {
        PokerPlayer player = new PokerPlayer(0, "Human", true);
        player.setChipCount(0);
        player.setObserver(true);

        assertThat(CheckEndHand.isHumanBroke(player)).isFalse();
    }

    // =================================================================
    // shouldOfferRebuy tests
    // =================================================================

    @Test
    void should_ReturnTrue_When_BrokeAndRebuyAllowed() {
        profile.setRebuys(true);
        profile.setLastRebuyLevel(5);

        PokerTable table = new PokerTable(game, 1);
        game.addTable(table);

        PokerPlayer human = new PokerPlayer(0, "Human", true);
        human.setChipCount(0);
        game.addPlayer(human);

        assertThat(CheckEndHand.shouldOfferRebuy(human, table)).isTrue();
    }

    @Test
    void should_ReturnFalse_When_HasChips() {
        profile.setRebuys(true);
        profile.setLastRebuyLevel(5);

        PokerTable table = new PokerTable(game, 1);
        game.addTable(table);

        PokerPlayer human = new PokerPlayer(0, "Human", true);
        human.setChipCount(500);
        game.addPlayer(human);

        assertThat(CheckEndHand.shouldOfferRebuy(human, table)).isFalse();
    }

    @Test
    void should_ReturnFalse_When_RebuyNotAllowed() {
        // Rebuys disabled (default)
        PokerTable table = new PokerTable(game, 1);
        game.addTable(table);

        PokerPlayer human = new PokerPlayer(0, "Human", true);
        human.setChipCount(0);
        game.addPlayer(human);

        assertThat(CheckEndHand.shouldOfferRebuy(human, table)).isFalse();
    }

    // =================================================================
    // calculateNeverBrokeTransfer tests
    // =================================================================

    @Test
    void should_ReturnHalfOfChipLeader_When_EvenlyDivisible() {
        // 1000 / 2 = 500; 500 % 25 = 0; result = 500
        assertThat(CheckEndHand.calculateNeverBrokeTransfer(1000, 25)).isEqualTo(500);
    }

    @Test
    void should_RoundDownToMinChip_When_NotEvenlyDivisible() {
        // 1000 / 2 = 500; 500 % 30 = 20; result = 500 - 20 = 480
        assertThat(CheckEndHand.calculateNeverBrokeTransfer(1000, 30)).isEqualTo(480);
    }

    @Test
    void should_ReturnZero_When_ChipLeaderHasMinimal() {
        // 1 / 2 = 0; 0 % 25 = 0; result = 0
        assertThat(CheckEndHand.calculateNeverBrokeTransfer(1, 25)).isEqualTo(0);
    }

    // =================================================================
    // checkGameOverStatus tests
    // =================================================================

    @Test
    void should_ReturnContinue_When_PlayerHasChips() {
        PokerTable table = new PokerTable(game, 1);
        game.addTable(table);

        PokerPlayer human = new PokerPlayer(0, "Human", true);
        human.setChipCount(1000);
        game.addPlayer(human);

        PokerPlayer ai = new PokerPlayer(1, "AI", false);
        ai.setChipCount(1000);
        game.addPlayer(ai);

        CheckEndHand.GameOverResult result = CheckEndHand.checkGameOverStatus(game, human, table, false);

        assertThat(result).isEqualTo(CheckEndHand.GameOverResult.CONTINUE);
    }

    @Test
    void should_ReturnRebuyOffered_When_BrokeAndRebuyAllowed() {
        profile.setRebuys(true);
        profile.setLastRebuyLevel(5);

        PokerTable table = new PokerTable(game, 1);
        game.addTable(table);

        PokerPlayer human = new PokerPlayer(0, "Human", true);
        human.setChipCount(0);
        game.addPlayer(human);

        PokerPlayer ai = new PokerPlayer(1, "AI", false);
        ai.setChipCount(1000);
        game.addPlayer(ai);

        CheckEndHand.GameOverResult result = CheckEndHand.checkGameOverStatus(game, human, table, false);

        assertThat(result).isEqualTo(CheckEndHand.GameOverResult.REBUY_OFFERED);
    }

    @Test
    void should_ReturnGameOver_When_BrokeOfflineNoCheat() {
        PokerTable table = new PokerTable(game, 1);
        game.addTable(table);

        PokerPlayer human = new PokerPlayer(0, "Human", true);
        human.setChipCount(0);
        game.addPlayer(human);

        PokerPlayer ai = new PokerPlayer(1, "AI", false);
        ai.setChipCount(1000);
        game.addPlayer(ai);

        CheckEndHand.GameOverResult result = CheckEndHand.checkGameOverStatus(game, human, table, false);

        assertThat(result).isEqualTo(CheckEndHand.GameOverResult.GAME_OVER);
    }

    @Test
    void should_ReturnNeverBrokeActive_When_BrokeOfflineWithCheat() {
        PokerTable table = new PokerTable(game, 1);
        game.addTable(table);

        PokerPlayer human = new PokerPlayer(0, "Human", true);
        human.setChipCount(0);
        game.addPlayer(human);

        PokerPlayer ai = new PokerPlayer(1, "AI", false);
        ai.setChipCount(1000);
        game.addPlayer(ai);

        CheckEndHand.GameOverResult result = CheckEndHand.checkGameOverStatus(game, human, table, true);

        assertThat(result).isEqualTo(CheckEndHand.GameOverResult.NEVER_BROKE_ACTIVE);
    }

    @Test
    void should_ReturnTournamentWon_When_OnePlayerLeft() {
        PokerTable table = new PokerTable(game, 1);
        game.addTable(table);

        PokerPlayer human = new PokerPlayer(0, "Human", true);
        human.setChipCount(3000);
        game.addPlayer(human);

        // Add an eliminated player so the game has more than one player total
        // but only one with chips
        PokerPlayer ai = new PokerPlayer(1, "AI", false);
        ai.setChipCount(0);
        ai.setEliminated(true);
        game.addPlayer(ai);

        CheckEndHand.GameOverResult result = CheckEndHand.checkGameOverStatus(game, human, table, false);

        assertThat(result).isEqualTo(CheckEndHand.GameOverResult.TOURNAMENT_WON);
    }
}
