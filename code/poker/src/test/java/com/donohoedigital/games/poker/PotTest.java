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

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for Pot - pot management, chip tracking, side pots, and winner
 * distribution.
 */
class PotTest {

    private Pot pot;

    @BeforeEach
    void setUp() {
        // Initialize ConfigManager for headless testing
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);

        // Create a pot for preflop (round 0)
        pot = new Pot(0, 0);
    }

    // =================================================================
    // Pot Initialization Tests
    // =================================================================

    @Test
    void should_CreatePot_When_ConstructorCalled() {
        assertThat(pot).isNotNull();
        assertThat(pot.getRound()).isZero();
    }

    @Test
    void should_HaveZeroChips_When_PotCreated() {
        assertThat(pot.getChipCount()).isZero();
    }

    @Test
    void should_HaveNoPlayers_When_PotCreated() {
        assertThat(pot.getNumPlayers()).isZero();
        assertThat(pot.getPlayers()).isEmpty();
    }

    @Test
    void should_HaveNoSideBet_When_PotCreated() {
        assertThat(pot.getSideBet()).isEqualTo(Pot.NO_SIDE);
    }

    @Test
    void should_NotHaveBaseAllIn_When_PotCreated() {
        assertThat(pot.hasBaseAllIn()).isFalse();
    }

    // =================================================================
    // Chip Management Tests
    // =================================================================

    @Test
    void should_AddChips_When_PlayerBets() {
        PokerPlayer player = createTestPlayer("Player1");

        pot.addChips(player, 100);

        assertThat(pot.getChipCount()).isEqualTo(100);
    }

    @Test
    void should_AddMultipleChips_When_PlayerBetsMultipleTimes() {
        PokerPlayer player = createTestPlayer("Player1");

        pot.addChips(player, 100);
        pot.addChips(player, 50);
        pot.addChips(player, 25);

        assertThat(pot.getChipCount()).isEqualTo(175);
    }

    @Test
    void should_AddChipsFromMultiplePlayers_When_MultipleBets() {
        PokerPlayer player1 = createTestPlayer("Player1");
        PokerPlayer player2 = createTestPlayer("Player2");
        PokerPlayer player3 = createTestPlayer("Player3");

        pot.addChips(player1, 100);
        pot.addChips(player2, 100);
        pot.addChips(player3, 100);

        assertThat(pot.getChipCount()).isEqualTo(300);
    }

    @Test
    void should_TrackPlayers_When_ChipsAdded() {
        PokerPlayer player1 = createTestPlayer("Player1");
        PokerPlayer player2 = createTestPlayer("Player2");

        pot.addChips(player1, 100);
        pot.addChips(player2, 50);

        assertThat(pot.getNumPlayers()).isEqualTo(2);
        assertThat(pot.getPlayers()).containsExactly(player1, player2);
    }

    @Test
    void should_NotDuplicatePlayers_When_PlayerAddsChipsTwice() {
        PokerPlayer player = createTestPlayer("Player1");

        pot.addChips(player, 100);
        pot.addChips(player, 50);

        assertThat(pot.getNumPlayers()).isEqualTo(1);
    }

    @Test
    void should_GetPlayerByIndex_When_PlayerExists() {
        PokerPlayer player1 = createTestPlayer("Player1");
        PokerPlayer player2 = createTestPlayer("Player2");
        pot.addChips(player1, 100);
        pot.addChips(player2, 50);

        assertThat(pot.getPlayerAt(0)).isEqualTo(player1);
        assertThat(pot.getPlayerAt(1)).isEqualTo(player2);
    }

    @Test
    void should_CheckIfPlayerInPot_When_PlayerAdded() {
        PokerPlayer player1 = createTestPlayer("Player1");
        PokerPlayer player2 = createTestPlayer("Player2");
        pot.addChips(player1, 100);

        assertThat(pot.isInPot(player1)).isTrue();
        assertThat(pot.isInPot(player2)).isFalse();
    }

    // =================================================================
    // Round Advancement Tests
    // =================================================================

    @Test
    void should_AdvanceRound_When_RoundAdvances() {
        pot.addChips(createTestPlayer("Player1"), 100);

        pot.advanceRound();

        assertThat(pot.getRound()).isEqualTo(1); // Advanced from 0 to 1
    }

    @Test
    void should_PreserveChips_When_RoundAdvances() {
        pot.addChips(createTestPlayer("Player1"), 100);
        int chips = pot.getChipCount();

        pot.advanceRound();

        assertThat(pot.getChipCount()).isEqualTo(chips);
    }

    @Test
    void should_AddChipsAcrossRounds_When_RoundAdvances() {
        pot.addChips(createTestPlayer("Player1"), 100);
        pot.advanceRound();
        pot.addChips(createTestPlayer("Player1"), 50);

        assertThat(pot.getChipCount()).isEqualTo(150);
    }

    // =================================================================
    // Side Pot Tests
    // =================================================================

    @Test
    void should_SetSideBet_When_SidePotCreated() {
        pot.setSideBet(1);

        assertThat(pot.getSideBet()).isEqualTo(1);
    }

    @Test
    void should_HaveNoSideBet_When_MainPot() {
        assertThat(pot.getSideBet()).isEqualTo(Pot.NO_SIDE);
    }

    @Test
    void should_IndicateSidePot_When_SideBetSet() {
        pot.setSideBet(2);

        assertThat(pot.getSideBet()).isNotEqualTo(Pot.NO_SIDE);
    }

    // =================================================================
    // Winner Tests
    // =================================================================

    @Test
    void should_HaveNoWinners_When_PotCreated() {
        assertThat(pot.getWinners()).isEmpty();
    }

    @Test
    void should_SetWinners_When_WinnersAssigned() {
        PokerPlayer winner1 = createTestPlayer("Winner1");
        PokerPlayer winner2 = createTestPlayer("Winner2");
        List<PokerPlayer> winners = List.of(winner1, winner2);

        pot.setWinners(winners);

        assertThat(pot.getWinners()).containsExactly(winner1, winner2);
    }

    @Test
    void should_SetSingleWinner_When_OneWinner() {
        PokerPlayer winner = createTestPlayer("Winner");
        List<PokerPlayer> winners = List.of(winner);

        pot.setWinners(winners);

        assertThat(pot.getWinners()).hasSize(1);
        assertThat(pot.getWinners()).contains(winner);
    }

    // =================================================================
    // Reset Tests
    // =================================================================

    @Test
    void should_ClearChips_When_Reset() {
        pot.addChips(createTestPlayer("Player1"), 100);

        pot.reset();

        assertThat(pot.getChipCount()).isZero();
    }

    @Test
    void should_PreservePlayers_When_Reset() {
        pot.addChips(createTestPlayer("Player1"), 100);
        pot.addChips(createTestPlayer("Player2"), 50);

        pot.reset();

        // Players stay in pot when resetting (only chips are reset)
        assertThat(pot.getNumPlayers()).isEqualTo(2);
    }

    @Test
    void should_PreserveWinners_When_Reset() {
        PokerPlayer winner = createTestPlayer("Winner");
        pot.setWinners(List.of(winner));

        pot.reset();

        // Winners stay set when resetting (only chips are reset)
        assertThat(pot.getWinners()).hasSize(1);
    }

    // =================================================================
    // ToString Test
    // =================================================================

    @Test
    void should_ReturnString_When_ToStringCalled() {
        pot.addChips(createTestPlayer("Player1"), 100);

        String result = pot.toString();

        assertThat(result).isNotNull();
        assertThat(result).contains("100");
    }

    // =================================================================
    // Test Helper Methods
    // =================================================================

    private PokerPlayer createTestPlayer(String name) {
        return new PokerPlayer(0, name, true);
    }
}
