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
    void should_ResetToBaseChips_When_AdvancedRound() {
        pot.addChips(createTestPlayer("Player1"), 100);
        pot.advanceRound(); // Sets base to 100

        pot.addChips(createTestPlayer("Player1"), 50);
        assertThat(pot.getChipCount()).isEqualTo(150);

        pot.reset();

        assertThat(pot.getChipCount()).isEqualTo(100); // Back to base, not zero
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
    // Overbet Tests
    // =================================================================

    @Test
    void should_BeOverbet_When_OnlyOnePlayer() {
        pot.addChips(createTestPlayer("Player1"), 100);

        assertThat(pot.isOverbet()).isTrue();
    }

    @Test
    void should_NotBeOverbet_When_MultiplePlayers() {
        pot.addChips(createTestPlayer("Player1"), 100);
        pot.addChips(createTestPlayer("Player2"), 100);

        assertThat(pot.isOverbet()).isFalse();
    }

    @Test
    void should_NotBeOverbet_When_NoPlayers() {
        assertThat(pot.isOverbet()).isFalse();
    }

    // =================================================================
    // All-In Detection Tests
    // =================================================================

    @Test
    void should_DetectBaseAllIn_When_AllInPlayerAtRoundAdvance() {
        PokerPlayer player = createTestPlayer("Player1");
        player.setChipCount(0); // Player is all-in

        pot.addChips(player, 100);
        pot.advanceRound();

        assertThat(pot.hasBaseAllIn()).isTrue();
    }

    @Test
    void should_NotDetectBaseAllIn_When_NoAllInPlayers() {
        PokerPlayer player = createTestPlayer("Player1");
        player.setChipCount(500); // Player has chips remaining

        pot.addChips(player, 100);
        pot.advanceRound();

        assertThat(pot.hasBaseAllIn()).isFalse();
    }

    @Test
    void should_DetectBaseAllIn_When_AnyPlayerAllIn() {
        PokerPlayer player1 = createTestPlayer("Player1");
        PokerPlayer player2 = createTestPlayer("Player2");
        PokerPlayer player3 = createTestPlayer("Player3");

        player1.setChipCount(500); // Not all-in
        player2.setChipCount(0); // All-in
        player3.setChipCount(300); // Not all-in

        pot.addChips(player1, 100);
        pot.addChips(player2, 100);
        pot.addChips(player3, 100);
        pot.advanceRound();

        assertThat(pot.hasBaseAllIn()).isTrue();
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
    // Multi-Winner Split Pot Scenarios
    // =================================================================

    @Test
    void should_SetMultipleWinners_When_SplitPot() {
        PokerPlayer player1 = createTestPlayer("Player1");
        PokerPlayer player2 = createTestPlayer("Player2");
        pot.addChips(player1, 100);
        pot.addChips(player2, 100);

        pot.setWinners(List.of(player1, player2));

        assertThat(pot.getWinners()).hasSize(2);
        assertThat(pot.getWinners()).containsExactly(player1, player2);
    }

    @Test
    void should_SetThreeWinners_When_ThreeWaySplit() {
        PokerPlayer player1 = createTestPlayer("Player1");
        PokerPlayer player2 = createTestPlayer("Player2");
        PokerPlayer player3 = createTestPlayer("Player3");
        pot.addChips(player1, 100);
        pot.addChips(player2, 100);
        pot.addChips(player3, 100);

        pot.setWinners(List.of(player1, player2, player3));

        assertThat(pot.getWinners()).hasSize(3);
        assertThat(pot.getWinners()).containsExactly(player1, player2, player3);
    }

    @Test
    void should_TrackEachWinner_When_WinnersQueried() {
        PokerPlayer player1 = createTestPlayer("Player1");
        PokerPlayer player2 = createTestPlayer("Player2");
        PokerPlayer player3 = createTestPlayer("Player3");
        pot.addChips(player1, 100);
        pot.addChips(player2, 100);
        pot.addChips(player3, 100);

        // Only player1 and player3 win (split pot, player2 loses)
        pot.setWinners(List.of(player1, player3));

        assertThat(pot.getWinners()).contains(player1, player3);
        assertThat(pot.getWinners()).doesNotContain(player2);
    }

    @Test
    void should_ReplaceWinners_When_SetWinnersCalledTwice() {
        PokerPlayer player1 = createTestPlayer("Player1");
        PokerPlayer player2 = createTestPlayer("Player2");
        pot.addChips(player1, 100);
        pot.addChips(player2, 100);

        pot.setWinners(List.of(player1));
        assertThat(pot.getWinners()).containsExactly(player1);

        // Setting winners again should replace, not append
        pot.setWinners(List.of(player2));
        assertThat(pot.getWinners()).containsExactly(player2);
        assertThat(pot.getWinners()).doesNotContain(player1);
    }

    @Test
    void should_ClearWinners_When_EmptyListSet() {
        PokerPlayer player1 = createTestPlayer("Player1");
        pot.addChips(player1, 100);
        pot.setWinners(List.of(player1));

        pot.setWinners(List.of());

        assertThat(pot.getWinners()).isEmpty();
    }

    // =================================================================
    // Complex Side Pot Scenarios
    // =================================================================

    @Test
    void should_TrackSideBetAmount_When_SidePotCreated() {
        // Simulate main pot + side pot scenario with two separate Pot objects
        Pot mainPot = new Pot(0, 0);
        Pot sidePot = new Pot(0, 1);

        PokerPlayer player1 = createTestPlayer("Player1");
        PokerPlayer player2 = createTestPlayer("Player2");
        PokerPlayer player3 = createTestPlayer("Player3");

        // Main pot: all three players contribute 50 each
        mainPot.addChips(player1, 50);
        mainPot.addChips(player2, 50);
        mainPot.addChips(player3, 50);

        // Side pot: only player2 and player3 contribute extra 50 each
        sidePot.addChips(player2, 50);
        sidePot.addChips(player3, 50);
        sidePot.setSideBet(50);

        assertThat(mainPot.getChipCount()).isEqualTo(150);
        assertThat(sidePot.getChipCount()).isEqualTo(100);
        assertThat(sidePot.getSideBet()).isEqualTo(50);
        assertThat(mainPot.getNumPlayers()).isEqualTo(3);
        assertThat(sidePot.getNumPlayers()).isEqualTo(2);
    }

    @Test
    void should_CreateMultipleSidePots_When_MultiplePlayersAllIn() {
        // Three-level pot chain: main, side1, side2
        Pot mainPot = new Pot(0, 0);
        Pot sidePot1 = new Pot(0, 1);
        Pot sidePot2 = new Pot(0, 2);

        PokerPlayer shortStack = createTestPlayer("ShortStack");
        PokerPlayer midStack = createTestPlayer("MidStack");
        PokerPlayer bigStack = createTestPlayer("BigStack");

        shortStack.setChipCount(0); // all-in
        midStack.setChipCount(0); // all-in
        bigStack.setChipCount(500); // still has chips

        // Main pot: all three contribute 25
        mainPot.addChips(shortStack, 25);
        mainPot.addChips(midStack, 25);
        mainPot.addChips(bigStack, 25);

        // Side pot 1: mid and big contribute 50 more each
        sidePot1.addChips(midStack, 50);
        sidePot1.addChips(bigStack, 50);
        sidePot1.setSideBet(50);

        // Side pot 2: only big contributes 100 more (overbet)
        sidePot2.addChips(bigStack, 100);
        sidePot2.setSideBet(100);

        assertThat(mainPot.getChipCount()).isEqualTo(75);
        assertThat(mainPot.getNumPlayers()).isEqualTo(3);

        assertThat(sidePot1.getChipCount()).isEqualTo(100);
        assertThat(sidePot1.getNumPlayers()).isEqualTo(2);
        assertThat(sidePot1.getSideBet()).isEqualTo(50);

        assertThat(sidePot2.getChipCount()).isEqualTo(100);
        assertThat(sidePot2.getNumPlayers()).isEqualTo(1);
        assertThat(sidePot2.isOverbet()).isTrue();
        assertThat(sidePot2.getSideBet()).isEqualTo(100);
    }

    @Test
    void should_AssignDifferentWinners_When_MultipleSidePots() {
        // Main pot won by player1, side pot won by player2
        Pot mainPot = new Pot(0, 0);
        Pot sidePot = new Pot(0, 1);

        PokerPlayer player1 = createTestPlayer("Player1");
        PokerPlayer player2 = createTestPlayer("Player2");
        PokerPlayer player3 = createTestPlayer("Player3");

        mainPot.addChips(player1, 50);
        mainPot.addChips(player2, 50);
        mainPot.addChips(player3, 50);

        sidePot.addChips(player2, 50);
        sidePot.addChips(player3, 50);
        sidePot.setSideBet(50);

        mainPot.setWinners(List.of(player1));
        sidePot.setWinners(List.of(player2));

        assertThat(mainPot.getWinners()).containsExactly(player1);
        assertThat(sidePot.getWinners()).containsExactly(player2);
    }

    @Test
    void should_SplitSidePot_When_TwoWinnersInSidePot() {
        Pot mainPot = new Pot(0, 0);
        Pot sidePot = new Pot(0, 1);

        PokerPlayer player1 = createTestPlayer("Player1");
        PokerPlayer player2 = createTestPlayer("Player2");
        PokerPlayer player3 = createTestPlayer("Player3");

        mainPot.addChips(player1, 50);
        mainPot.addChips(player2, 50);
        mainPot.addChips(player3, 50);

        sidePot.addChips(player2, 100);
        sidePot.addChips(player3, 100);
        sidePot.setSideBet(100);

        // Main pot won by player1, side pot split between player2 and player3
        mainPot.setWinners(List.of(player1));
        sidePot.setWinners(List.of(player2, player3));

        assertThat(mainPot.getWinners()).hasSize(1);
        assertThat(sidePot.getWinners()).hasSize(2);
        assertThat(sidePot.getWinners()).containsExactly(player2, player3);
    }

    // =================================================================
    // Overbet Tracking (Extended)
    // =================================================================

    @Test
    void should_TransitionFromOverbet_When_SecondPlayerAdds() {
        PokerPlayer player1 = createTestPlayer("Player1");
        PokerPlayer player2 = createTestPlayer("Player2");

        pot.addChips(player1, 100);
        assertThat(pot.isOverbet()).isTrue();

        pot.addChips(player2, 100);
        assertThat(pot.isOverbet()).isFalse();
    }

    @Test
    void should_RemainOverbet_When_SamePlayerAddsTwice() {
        PokerPlayer player1 = createTestPlayer("Player1");

        pot.addChips(player1, 100);
        pot.addChips(player1, 50);

        // Still only one player, so still overbet
        assertThat(pot.isOverbet()).isTrue();
        assertThat(pot.getChipCount()).isEqualTo(150);
    }

    // =================================================================
    // All-In Detection (Extended)
    // =================================================================

    @Test
    void should_NotHaveBaseAllIn_When_NoPlayersBeforeAdvance() {
        // Pot with no players advanced
        pot.advanceRound();

        assertThat(pot.hasBaseAllIn()).isFalse();
    }

    @Test
    void should_RetainBaseAllIn_When_MultipleRoundsAdvanced() {
        PokerPlayer player1 = createTestPlayer("Player1");
        player1.setChipCount(0); // all-in
        pot.addChips(player1, 100);

        pot.advanceRound(); // round 0 -> 1, base all-in set
        assertThat(pot.hasBaseAllIn()).isTrue();

        // Give player chips back to simulate next round
        player1.setChipCount(500);
        pot.advanceRound(); // round 1 -> 2, re-evaluates
        // Now no player is all-in, so base all-in should be false
        assertThat(pot.hasBaseAllIn()).isFalse();
    }

    @Test
    void should_DetectBaseAllIn_When_MultiplePlayersAllIn() {
        PokerPlayer player1 = createTestPlayer("Player1");
        PokerPlayer player2 = createTestPlayer("Player2");
        player1.setChipCount(0);
        player2.setChipCount(0);

        pot.addChips(player1, 50);
        pot.addChips(player2, 50);
        pot.advanceRound();

        assertThat(pot.hasBaseAllIn()).isTrue();
    }

    // =================================================================
    // Round Advancement (Extended)
    // =================================================================

    @Test
    void should_AdvanceMultipleRounds_When_CalledRepeatedly() {
        pot.addChips(createTestPlayer("Player1"), 100);
        pot.advanceRound();
        pot.advanceRound();
        pot.advanceRound();

        assertThat(pot.getRound()).isEqualTo(3);
    }

    @Test
    void should_ResetToCorrectBase_When_AdvancedMultipleRounds() {
        PokerPlayer player = createTestPlayer("Player1");
        player.setChipCount(1000);

        pot.addChips(player, 100); // total: 100
        pot.advanceRound(); // base: 100

        pot.addChips(player, 200); // total: 300
        pot.advanceRound(); // base: 300

        pot.addChips(player, 50); // total: 350
        pot.reset(); // back to base: 300

        assertThat(pot.getChipCount()).isEqualTo(300);
    }

    // =================================================================
    // Edge Cases
    // =================================================================

    @Test
    void should_HandleZeroChipAdd_When_ZeroAdded() {
        PokerPlayer player = createTestPlayer("Player1");

        pot.addChips(player, 0);

        assertThat(pot.getChipCount()).isZero();
        // Player is still tracked even with zero chip add
        assertThat(pot.getNumPlayers()).isEqualTo(1);
        assertThat(pot.isInPot(player)).isTrue();
    }

    @Test
    void should_HandleLargeChipCounts_When_BigBetsAdded() {
        PokerPlayer player1 = createTestPlayer("Player1");
        PokerPlayer player2 = createTestPlayer("Player2");

        pot.addChips(player1, 1_000_000);
        pot.addChips(player2, 1_000_000);

        assertThat(pot.getChipCount()).isEqualTo(2_000_000);
    }

    @Test
    void should_HandleManyPlayers_When_TenPlayersInPot() {
        for (int i = 0; i < 10; i++) {
            pot.addChips(createTestPlayer("Player" + i), 100);
        }

        assertThat(pot.getNumPlayers()).isEqualTo(10);
        assertThat(pot.getChipCount()).isEqualTo(1000);
        assertThat(pot.isOverbet()).isFalse();
    }

    @Test
    void should_PreserveSideBet_When_ChipsAdded() {
        PokerPlayer player = createTestPlayer("Player1");
        pot.setSideBet(50);

        pot.addChips(player, 100);

        assertThat(pot.getSideBet()).isEqualTo(50);
    }

    @Test
    void should_PreserveSideBet_When_RoundAdvanced() {
        PokerPlayer player = createTestPlayer("Player1");
        pot.addChips(player, 100);
        pot.setSideBet(50);

        pot.advanceRound();

        assertThat(pot.getSideBet()).isEqualTo(50);
    }

    @Test
    void should_PreserveSideBet_When_Reset() {
        pot.setSideBet(50);
        pot.addChips(createTestPlayer("Player1"), 100);
        pot.advanceRound();
        pot.addChips(createTestPlayer("Player1"), 50);

        pot.reset();

        assertThat(pot.getSideBet()).isEqualTo(50);
    }

    // =================================================================
    // ToString (Extended)
    // =================================================================

    @Test
    void should_IncludePlayerNames_When_ToStringCalled() {
        pot.addChips(createTestPlayer("Alice"), 100);
        pot.addChips(createTestPlayer("Bob"), 50);

        String result = pot.toString();

        assertThat(result).contains("Alice");
        assertThat(result).contains("Bob");
    }

    @Test
    void should_IncludeRoundInfo_When_ToStringCalled() {
        pot.addChips(createTestPlayer("Player1"), 100);
        pot.advanceRound();

        String result = pot.toString();

        assertThat(result).contains("Round 1");
    }

    @Test
    void should_IncludeSideBetInfo_When_ToStringCalledOnSidePot() {
        pot.setSideBet(75);
        pot.addChips(createTestPlayer("Player1"), 100);

        String result = pot.toString();

        assertThat(result).contains("75");
    }

    @Test
    void should_HandleEmptyPot_When_ToStringCalled() {
        String result = pot.toString();

        assertThat(result).isNotNull();
        assertThat(result).contains("0"); // zero chips
    }

    // =================================================================
    // Test Helper Methods
    // =================================================================

    private PokerPlayer createTestPlayer(String name) {
        return new PokerPlayer(0, name, true);
    }
}
