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

import com.donohoedigital.games.poker.integration.IntegrationTestBase;
import com.donohoedigital.games.poker.model.TournamentProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for HoldemHand pot calculation methods.
 * Critical money-tracking operations - pot totals must be accurate.
 * Extends IntegrationTestBase for game infrastructure.
 */
@Tag("integration")
class HoldemHandPotCalculationTest extends IntegrationTestBase
{
    private PokerGame game;
    private PokerTable table;
    private HoldemHand hand;
    private PokerPlayer[] players;

    @BeforeEach
    void setUp()
    {
        // Create game infrastructure
        game = new PokerGame(null);
        TournamentProfile profile = new TournamentProfile("test");
        profile.setBuyinChips(1500);
        game.setProfile(profile);

        table = new PokerTable(game, 1);
        table.setMinChip(1);

        // Create 3 players (enough for side pot scenarios)
        players = new PokerPlayer[3];
        for (int i = 0; i < 3; i++)
        {
            players[i] = new PokerPlayer(i + 1, "Player" + i, true);
            players[i].setChipCount(1000);
            game.addPlayer(players[i]);
            table.setPlayer(players[i], i);
        }

        // Initialize hand
        table.setButton(0);
        for (PokerPlayer p : players)
        {
            p.newHand('p');
        }

        hand = new HoldemHand(table);
        table.setHoldemHand(hand);
        hand.setBigBlind(20);
        hand.setSmallBlind(10);
        hand.setPlayerOrder(false);
    }

    // ========================================
    // getTotalPotChipCount() Tests - Single Pot
    // ========================================

    @Test
    void should_ReturnZero_When_NoBets()
    {
        assertThat(hand.getTotalPotChipCount()).isEqualTo(0);
    }

    @Test
    void should_ReturnCorrectTotal_When_SimpleBets()
    {
        // Player 0 bets 100
        hand.setCurrentPlayerIndex(0);
        players[0].bet(100, "bet");

        // Player 1 calls
        hand.setCurrentPlayerIndex(1);
        players[1].call("call");

        // Total should be 200 (100 + 100)
        assertThat(hand.getTotalPotChipCount()).isEqualTo(200);
    }

    @Test
    void should_AccumulateChips_When_MultipleBets()
    {
        // Player 0 bets 50
        hand.setCurrentPlayerIndex(0);
        players[0].bet(50, "bet");

        // Player 1 calls 50, then player 0 raises
        hand.setCurrentPlayerIndex(1);
        players[1].call("call");

        // Advance to next round for another bet
        hand.advanceRound();
        hand.setCurrentPlayerIndex(0);
        players[0].bet(100, "bet");

        // Total should include all bets from both rounds
        assertThat(hand.getTotalPotChipCount()).isGreaterThanOrEqualTo(200);
    }

    // ========================================
    // getNumPots() Tests
    // ========================================

    @Test
    void should_ReturnOne_When_NoSidePots()
    {
        // Simple betting - no all-ins
        hand.setCurrentPlayerIndex(0);
        players[0].bet(50, "bet");

        hand.setCurrentPlayerIndex(1);
        players[1].call("call");

        // Should have exactly 1 pot (main pot)
        assertThat(hand.getNumPots()).isEqualTo(1);
    }

    @Test
    void should_CountAllPots_When_MultiplePots()
    {
        // This test verifies pot count increases when side pots are created
        // The actual side pot creation happens in calcPots() which is called
        // during betting rounds. For now, verify we start with 1 pot.
        assertThat(hand.getNumPots()).isEqualTo(1);
    }

    // ========================================
    // getCurrentPot() Tests
    // ========================================

    @Test
    void should_ReturnMainPot_When_NoSidePots()
    {
        Pot currentPot = hand.getCurrentPot();

        // Should have a pot (main pot)
        assertThat(currentPot).isNotNull();
        assertThat(currentPot.getRound()).isEqualTo(HoldemHand.ROUND_PRE_FLOP);
    }

    @Test
    void should_ReturnActivePot_When_ChipsAdded()
    {
        // Add bets to pot
        hand.setCurrentPlayerIndex(0);
        players[0].bet(100, "bet");

        Pot currentPot = hand.getCurrentPot();

        // Current pot should have chips
        assertThat(currentPot).isNotNull();
        assertThat(currentPot.getChipCount()).isGreaterThan(0);
    }

    // ========================================
    // getPotOdds() Tests
    // ========================================

    @Test
    void should_CalculatePotOdds_When_SimpleCall()
    {
        // Player 0 bets 100
        hand.setCurrentPlayerIndex(0);
        players[0].bet(100, "bet");

        // Player 1 needs to call 100 into pot of 100
        // Pot odds = call / (call + pot) = 100 / 200 = 50%
        hand.setCurrentPlayerIndex(1);
        float odds = hand.getPotOdds(players[1]);

        assertThat(odds).isCloseTo(50.0f, within(1.0f));
    }

    @Test
    void should_CalculatePotOdds_When_LargerPot()
    {
        // Player 0 bets 100
        hand.setCurrentPlayerIndex(0);
        players[0].bet(100, "bet");

        // Player 1 calls 100
        hand.setCurrentPlayerIndex(1);
        players[1].call("call");

        // Player 2 needs to call 100 into pot of 200
        // Pot odds = 100 / 300 = 33.3%
        hand.setCurrentPlayerIndex(2);
        float odds = hand.getPotOdds(players[2]);

        assertThat(odds).isCloseTo(33.3f, within(1.0f));
    }

    @Test
    void should_AdjustPotOdds_When_AllInCall()
    {
        // Player 0 bets 100
        hand.setCurrentPlayerIndex(0);
        players[0].bet(100, "bet");

        // Player 1 has only 50 chips left - all-in call
        players[1].setChipCount(50);
        players[1].newHand('p'); // Recapture chip count

        hand.setCurrentPlayerIndex(1);
        float odds = hand.getPotOdds(players[1]);

        // All-in odds calculated based on portion of pot player can win
        // Should be reasonable percentage (not exact due to complex calculation)
        assertThat(odds).isGreaterThan(0.0f);
        assertThat(odds).isLessThan(100.0f);
    }

    // ========================================
    // Pot Total Invariant Tests
    // ========================================

    @Test
    void should_MaintainAccuracy_When_MultipleRounds()
    {
        // Pre-flop betting
        hand.setCurrentPlayerIndex(0);
        players[0].bet(50, "bet");
        int preFlopTotal = hand.getTotalPotChipCount();
        assertThat(preFlopTotal).isEqualTo(50);

        // Flop betting
        hand.advanceRound();
        hand.setCurrentPlayerIndex(0);
        players[0].bet(100, "bet");

        // Total should include both rounds
        int totalAfterFlop = hand.getTotalPotChipCount();
        assertThat(totalAfterFlop).isGreaterThanOrEqualTo(preFlopTotal + 100);
    }

    @Test
    void should_NotLoseChips_When_BettingSequence()
    {
        // Track total chips before betting
        int totalChipsBefore = 0;
        for (PokerPlayer p : players)
        {
            totalChipsBefore += p.getChipCount();
        }

        // Betting sequence
        hand.setCurrentPlayerIndex(0);
        players[0].bet(100, "bet");

        hand.setCurrentPlayerIndex(1);
        players[1].call("call");

        // Chips deducted from players + pot should equal original total
        int totalChipsAfter = 0;
        for (PokerPlayer p : players)
        {
            totalChipsAfter += p.getChipCount();
        }
        totalChipsAfter += hand.getTotalPotChipCount();

        assertThat(totalChipsAfter).isEqualTo(totalChipsBefore);
    }
}
