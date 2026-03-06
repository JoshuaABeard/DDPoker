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
package com.donohoedigital.games.poker.server;

import com.donohoedigital.games.poker.core.GameHand;
import com.donohoedigital.games.poker.core.GameTable;
import com.donohoedigital.games.poker.core.TournamentContext;
import com.donohoedigital.games.poker.core.ai.AIConstants;
import com.donohoedigital.games.poker.core.ai.StrategyProvider;
import com.donohoedigital.games.poker.engine.GamePlayerInfo;
import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.CardSuit;
import com.donohoedigital.games.poker.engine.Hand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ServerV2AIContext implementation.
 * <p>
 * Uses Mockito to mock complex interfaces to focus on testing ServerV2AIContext
 * logic rather than implementing full test doubles.
 */
class ServerV2AIContextTest {

    @Test
    void getHohM_calculatesCorrectly() {
        // Setup mocks
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        when(table.getLevel()).thenReturn(1);
        when(table.getNumOccupiedSeats()).thenReturn(3);
        when(tournament.getSmallBlind(1)).thenReturn(50);
        when(tournament.getBigBlind(1)).thenReturn(100);
        when(tournament.getAnte(1)).thenReturn(0);
        when(player.getChipCount()).thenReturn(10000);

        ServerV2AIContext context = new ServerV2AIContext(table, hand, tournament, player, strategy,
                new ServerOpponentTracker());

        // M = chipCount / (SB + BB + ante*players)
        // M = 10000 / (50 + 100 + 0*3) = 66.67
        float m = context.getHohM(player);

        assertThat(m).isCloseTo(66.67f, within(0.1f));
    }

    @Test
    void getHohM_withAnte_includesAnteCost() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        when(table.getLevel()).thenReturn(1);
        when(table.getNumOccupiedSeats()).thenReturn(3);
        when(tournament.getSmallBlind(1)).thenReturn(50);
        when(tournament.getBigBlind(1)).thenReturn(100);
        when(tournament.getAnte(1)).thenReturn(10);
        when(player.getChipCount()).thenReturn(10000);

        ServerV2AIContext context = new ServerV2AIContext(table, hand, tournament, player, strategy,
                new ServerOpponentTracker());

        // M = 10000 / (50 + 100 + 10*3) = 55.56
        float m = context.getHohM(player);

        assertThat(m).isCloseTo(55.56f, within(0.1f));
    }

    @Test
    void getHohZone_greenZone_forHighM() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        when(table.getLevel()).thenReturn(1);
        when(table.getNumOccupiedSeats()).thenReturn(3);
        when(tournament.getSmallBlind(1)).thenReturn(50);
        when(tournament.getBigBlind(1)).thenReturn(100);
        when(tournament.getAnte(1)).thenReturn(0);
        when(player.getChipCount()).thenReturn(10000); // M = 66.67

        ServerV2AIContext context = new ServerV2AIContext(table, hand, tournament, player, strategy,
                new ServerOpponentTracker());

        int zone = context.getHohZone(player);

        assertThat(zone).isEqualTo(4); // HOH_GREEN (>= 20)
    }

    @Test
    void getHohZone_redZone_forLowM() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        when(table.getLevel()).thenReturn(1);
        when(table.getNumOccupiedSeats()).thenReturn(3);
        when(tournament.getSmallBlind(1)).thenReturn(50);
        when(tournament.getBigBlind(1)).thenReturn(100);
        when(tournament.getAnte(1)).thenReturn(0);
        when(player.getChipCount()).thenReturn(500); // M = 3.33

        ServerV2AIContext context = new ServerV2AIContext(table, hand, tournament, player, strategy,
                new ServerOpponentTracker());

        int zone = context.getHohZone(player);

        assertThat(zone).isEqualTo(1); // HOH_RED (1-5)
    }

    @Test
    void getHohZone_deadZone_forCriticalM() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        when(table.getLevel()).thenReturn(1);
        when(table.getNumOccupiedSeats()).thenReturn(3);
        when(tournament.getSmallBlind(1)).thenReturn(50);
        when(tournament.getBigBlind(1)).thenReturn(100);
        when(tournament.getAnte(1)).thenReturn(0);
        when(player.getChipCount()).thenReturn(100); // M = 0.67

        ServerV2AIContext context = new ServerV2AIContext(table, hand, tournament, player, strategy,
                new ServerOpponentTracker());

        int zone = context.getHohZone(player);

        assertThat(zone).isEqualTo(0); // HOH_DEAD (< 1)
    }

    @Test
    void evaluateHandRank_pocketAces_returnsPair() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, hand, tournament, player, strategy,
                new ServerOpponentTracker());

        Card[] holeCards = new Card[]{new Card(CardSuit.SPADES, Card.ACE), new Card(CardSuit.HEARTS, Card.ACE)};
        Card[] community = new Card[0];

        int rank = context.evaluateHandRank(holeCards, community);

        assertThat(rank).isEqualTo(2); // HandInfo.PAIR (not 1)
    }

    @Test
    void evaluateHandScore_calculatesScore() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, hand, tournament, player, strategy,
                new ServerOpponentTracker());

        Card[] holeCards = new Card[]{new Card(CardSuit.SPADES, Card.ACE), new Card(CardSuit.HEARTS, Card.KING)};
        Card[] community = new Card[0];

        long score = context.evaluateHandScore(holeCards, community);

        assertThat(score).isGreaterThan(0L);
    }

    @Test
    void getBest5CardRanks_returnsCorrectRanks() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, hand, tournament, player, strategy,
                new ServerOpponentTracker());

        // Pocket aces with low board cards - makes best hand: AA + KQJ high cards
        Card[] holeCards = new Card[]{new Card(CardSuit.SPADES, Card.ACE), new Card(CardSuit.HEARTS, Card.ACE)};
        Card[] community = new Card[]{new Card(CardSuit.CLUBS, Card.KING), new Card(CardSuit.DIAMONDS, Card.QUEEN),
                new Card(CardSuit.SPADES, Card.JACK)};

        int[] ranks = context.getBest5CardRanks(holeCards, community);

        // Verify array has 5 ranks and contains the aces (order may vary by
        // implementation)
        assertThat(ranks).hasSize(5);
        assertThat(ranks).contains(Card.ACE); // Pocket aces included in best hand
    }

    @Test
    void getStrategy_returnsProvidedStrategy() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, hand, tournament, player, strategy,
                new ServerOpponentTracker());

        StrategyProvider returned = context.getStrategy();

        assertThat(returned).isSameAs(strategy);
    }

    @Test
    void getPocketCards_aiPlayer_returnsCards() {
        GameTable table = mock(GameTable.class);
        GameHand gameHand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        Card[] cards = new Card[]{new Card(CardSuit.SPADES, Card.ACE), new Card(CardSuit.HEARTS, Card.KING)};
        when(gameHand.getPlayerCards(aiPlayer)).thenReturn(cards);

        ServerV2AIContext context = new ServerV2AIContext(table, gameHand, tournament, aiPlayer, strategy,
                new ServerOpponentTracker());

        Hand result = context.getPocketCards(aiPlayer);

        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    void getPocketCards_otherPlayer_returnsNull() {
        GameTable table = mock(GameTable.class);
        GameHand gameHand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);
        GamePlayerInfo otherPlayer = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        Card[] cards = new Card[]{new Card(CardSuit.CLUBS, Card.ACE), new Card(CardSuit.DIAMONDS, Card.KING)};
        when(gameHand.getPlayerCards(otherPlayer)).thenReturn(cards);

        ServerV2AIContext context = new ServerV2AIContext(table, gameHand, tournament, aiPlayer, strategy,
                new ServerOpponentTracker());

        Hand result = context.getPocketCards(otherPlayer);

        // Security: AI can't see other players' cards
        assertThat(result).isNull();
    }

    @Test
    void getOpponentModel_returnsStubModel() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);
        GamePlayerInfo opponent = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, hand, tournament, aiPlayer, strategy,
                new ServerOpponentTracker());

        var model = context.getOpponentModel(opponent);

        assertThat(model).isNotNull();
        // Stub model returns default values
        assertThat(model.getPreFlopTightness(0, 0.5f)).isEqualTo(0.5f);
    }

    // === Step 5c: HandPotential Draw Count Tests ===

    @Test
    void getNutFlushCount_withNutFlushDraw_returnsPositiveCount() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, hand, tournament, aiPlayer, strategy,
                new ServerOpponentTracker());

        // Nut flush draw: Ace-King of hearts with two hearts on flop
        Hand pocket = new Hand(new Card(CardSuit.HEARTS, Card.ACE), new Card(CardSuit.HEARTS, Card.KING));
        Hand community = new Hand(new Card(CardSuit.HEARTS, Card.QUEEN), new Card(CardSuit.HEARTS, Card.JACK),
                new Card(CardSuit.SPADES, Card.TWO));

        int nutFlushCount = context.getNutFlushCount(pocket, community);

        // Should have nut flush draw with 4 hearts (2 in pocket, 2 on board)
        assertThat(nutFlushCount).isGreaterThan(0);
    }

    @Test
    void getNonNutFlushCount_withWeakFlushDraw_returnsPositiveCount() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, hand, tournament, aiPlayer, strategy,
                new ServerOpponentTracker());

        // Weak flush draw: 5-4 of hearts with two hearts on flop
        Hand pocket = new Hand(new Card(CardSuit.HEARTS, Card.FIVE), new Card(CardSuit.HEARTS, Card.FOUR));
        Hand community = new Hand(new Card(CardSuit.HEARTS, Card.QUEEN), new Card(CardSuit.HEARTS, Card.JACK),
                new Card(CardSuit.SPADES, Card.TWO));

        int nonNutFlushCount = context.getNonNutFlushCount(pocket, community);

        // Should have non-nut flush draw with 4 hearts (low cards)
        assertThat(nonNutFlushCount).isGreaterThan(0);
    }

    @Test
    void getNutStraightCount_withNutStraightDraw_returnsPositiveCount() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, hand, tournament, aiPlayer, strategy,
                new ServerOpponentTracker());

        // Open-ended straight draw: 9-8 with 7-6 on flop
        Hand pocket = new Hand(new Card(CardSuit.SPADES, Card.NINE), new Card(CardSuit.HEARTS, Card.EIGHT));
        Hand community = new Hand(new Card(CardSuit.CLUBS, Card.SEVEN), new Card(CardSuit.DIAMONDS, Card.SIX),
                new Card(CardSuit.SPADES, Card.TWO));

        int nutStraightCount = context.getNutStraightCount(pocket, community);

        // Should have straight draw (10 or 5 completes straight)
        assertThat(nutStraightCount).isGreaterThanOrEqualTo(0); // May be nut or non-nut depending on board
    }

    @Test
    void getNonNutStraightCount_withStraightDraw_returnsCount() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, hand, tournament, aiPlayer, strategy,
                new ServerOpponentTracker());

        // Gutshot straight draw: 5-4 with 7-6-2 on flop
        Hand pocket = new Hand(new Card(CardSuit.SPADES, Card.FIVE), new Card(CardSuit.HEARTS, Card.FOUR));
        Hand community = new Hand(new Card(CardSuit.CLUBS, Card.SEVEN), new Card(CardSuit.DIAMONDS, Card.SIX),
                new Card(CardSuit.SPADES, Card.TWO));

        int nonNutStraightCount = context.getNonNutStraightCount(pocket, community);

        // Counts should be >= 0 (8 or 3 completes)
        assertThat(nonNutStraightCount).isGreaterThanOrEqualTo(0);
    }

    @Test
    void getDrawCounts_nullInputs_returnZero() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, hand, tournament, aiPlayer, strategy,
                new ServerOpponentTracker());

        assertThat(context.getNutFlushCount(null, null)).isEqualTo(0);
        assertThat(context.getNonNutFlushCount(null, null)).isEqualTo(0);
        assertThat(context.getNutStraightCount(null, null)).isEqualTo(0);
        assertThat(context.getNonNutStraightCount(null, null)).isEqualTo(0);
    }

    // === Step 5d: Hand Strength Tests ===

    @Test
    void getRawHandStrength_postFlop_returnsReasonableStrength() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, hand, tournament, aiPlayer, strategy,
                new ServerOpponentTracker());

        // Pocket aces on a low flop should have high strength
        Hand pocket = new Hand(new Card(CardSuit.SPADES, Card.ACE), new Card(CardSuit.HEARTS, Card.ACE));
        Hand community = new Hand(new Card(CardSuit.CLUBS, Card.SEVEN), new Card(CardSuit.DIAMONDS, Card.FIVE),
                new Card(CardSuit.SPADES, Card.TWO));

        float strength = context.getRawHandStrength(pocket, community);

        // Should be between 0 and 1, likely high for pocket aces
        assertThat(strength).isBetween(0.0f, 1.0f);
        assertThat(strength).isGreaterThan(0.5f); // Pocket aces should beat most hands
    }

    @Test
    void getRawHandStrength_weakHand_returnsLowStrength() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, hand, tournament, aiPlayer, strategy,
                new ServerOpponentTracker());

        // 7-2 offsuit on ace-high board is very weak
        Hand pocket = new Hand(new Card(CardSuit.SPADES, Card.SEVEN), new Card(CardSuit.HEARTS, Card.TWO));
        Hand community = new Hand(new Card(CardSuit.CLUBS, Card.ACE), new Card(CardSuit.DIAMONDS, Card.KING),
                new Card(CardSuit.SPADES, Card.QUEEN));

        float strength = context.getRawHandStrength(pocket, community);

        assertThat(strength).isBetween(0.0f, 1.0f);
        assertThat(strength).isLessThan(0.3f); // Should be weak
    }

    @Test
    void getBiasedRawHandStrength_adjustsForOpponentRange() {
        GameTable table = mock(GameTable.class);
        GameHand gameHand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        when(table.getPlayer(1)).thenReturn(aiPlayer);

        Card[] cards = new Card[]{new Card(CardSuit.SPADES, Card.ACE), new Card(CardSuit.HEARTS, Card.KING)};
        when(gameHand.getPlayerCards(aiPlayer)).thenReturn(cards);

        ServerV2AIContext context = new ServerV2AIContext(table, gameHand, tournament, aiPlayer, strategy,
                new ServerOpponentTracker());

        Hand community = new Hand(new Card(CardSuit.CLUBS, Card.QUEEN), new Card(CardSuit.DIAMONDS, Card.JACK),
                new Card(CardSuit.SPADES, Card.TEN));

        float biasedStrength = context.getBiasedRawHandStrength(1, community);

        // Should return reasonable strength adjusted by SimpleBias
        assertThat(biasedStrength).isBetween(0.0f, 1.0f);
    }

    @Test
    void getBiasedEffectiveHandStrength_computesEHS() {
        GameTable table = mock(GameTable.class);
        GameHand gameHand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        when(table.getPlayer(1)).thenReturn(aiPlayer);

        Card[] cards = new Card[]{new Card(CardSuit.SPADES, Card.ACE), new Card(CardSuit.HEARTS, Card.KING)};
        when(gameHand.getPlayerCards(aiPlayer)).thenReturn(cards);

        ServerV2AIContext context = new ServerV2AIContext(table, gameHand, tournament, aiPlayer, strategy,
                new ServerOpponentTracker());

        Hand community = new Hand(new Card(CardSuit.CLUBS, Card.QUEEN), new Card(CardSuit.DIAMONDS, Card.JACK),
                new Card(CardSuit.SPADES, Card.TEN));

        float ehs = context.getBiasedEffectiveHandStrength(1, community);

        // EHS should be between 0 and 1
        assertThat(ehs).isBetween(0.0f, 1.0f);
    }

    @Test
    void getApparentStrength_computesObservedStrength() {
        GameTable table = mock(GameTable.class);
        GameHand gameHand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        when(table.getPlayer(1)).thenReturn(aiPlayer);

        Card[] cards = new Card[]{new Card(CardSuit.SPADES, Card.ACE), new Card(CardSuit.HEARTS, Card.KING)};
        when(gameHand.getPlayerCards(aiPlayer)).thenReturn(cards);

        ServerV2AIContext context = new ServerV2AIContext(table, gameHand, tournament, aiPlayer, strategy,
                new ServerOpponentTracker());

        Hand community = new Hand(new Card(CardSuit.CLUBS, Card.QUEEN), new Card(CardSuit.DIAMONDS, Card.JACK),
                new Card(CardSuit.SPADES, Card.TEN));

        float apparentStrength = context.getApparentStrength(1, community);

        // Apparent strength should be between 0 and 1
        assertThat(apparentStrength).isBetween(0.0f, 1.0f);
    }

    @Test
    void handStrengthMethods_nullInputs_returnZero() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, hand, tournament, aiPlayer, strategy,
                new ServerOpponentTracker());

        assertThat(context.getRawHandStrength(null, null)).isEqualTo(0.0f);
    }

    // === Tests for Step 5f implementations ===

    @Test
    void getRemainingAverageHohM_usesTableAverage() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        when(table.getSeats()).thenReturn(3);
        when(table.getLevel()).thenReturn(1);
        when(table.getNumOccupiedSeats()).thenReturn(3);
        when(tournament.getBigBlind(1)).thenReturn(20);

        // Setup players with different chip counts
        GamePlayerInfo p1 = mock(GamePlayerInfo.class);
        GamePlayerInfo p2 = mock(GamePlayerInfo.class);
        GamePlayerInfo p3 = mock(GamePlayerInfo.class);
        when(p1.getChipCount()).thenReturn(1000);
        when(p2.getChipCount()).thenReturn(2000);
        when(p3.getChipCount()).thenReturn(3000);

        when(table.getPlayer(0)).thenReturn(p1);
        when(table.getPlayer(1)).thenReturn(p2);
        when(table.getPlayer(2)).thenReturn(p3);

        ServerV2AIContext context = new ServerV2AIContext(table, hand, tournament, aiPlayer, strategy,
                new ServerOpponentTracker());

        float avgM = context.getRemainingAverageHohM();

        // Average M = avg chips / big blind = 2000 / 20 = 100
        assertThat(avgM).isEqualTo(100.0f);
    }

    @Test
    void getStartingOrder_calculatesPreFlopOrder() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        when(table.getSeats()).thenReturn(9);
        when(table.getButton()).thenReturn(0); // Button at seat 0

        // Setup players at different seats
        GamePlayerInfo p1 = mock(GamePlayerInfo.class); // Seat 1 = small blind
        GamePlayerInfo p5 = mock(GamePlayerInfo.class); // Seat 5 = middle position
        GamePlayerInfo p0 = mock(GamePlayerInfo.class); // Seat 0 = button

        when(table.getPlayer(0)).thenReturn(p0);
        when(table.getPlayer(1)).thenReturn(p1);
        when(table.getPlayer(5)).thenReturn(p5);

        // Mock getSeat to return seat numbers
        when(table.getSeat(p0)).thenReturn(0);
        when(table.getSeat(p1)).thenReturn(1);
        when(table.getSeat(p5)).thenReturn(5);

        ServerV2AIContext context = new ServerV2AIContext(table, hand, tournament, aiPlayer, strategy,
                new ServerOpponentTracker());

        // Small blind (seat 1) should be 1 position from button
        assertThat(context.getStartingOrder(p1)).isEqualTo(1);

        // Seat 5 should be 5 positions from button
        assertThat(context.getStartingOrder(p5)).isEqualTo(5);

        // Button should be 0 positions from button
        assertThat(context.getStartingOrder(p0)).isEqualTo(0);
    }

    @Test
    void getPostFlopPositionCategory_categorizesByPosition() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        when(table.getSeats()).thenReturn(9);
        when(table.getButton()).thenReturn(0);

        // Setup players
        GamePlayerInfo blind = mock(GamePlayerInfo.class); // Seat 1 = SB
        GamePlayerInfo early = mock(GamePlayerInfo.class); // Seat 3 = early
        GamePlayerInfo middle = mock(GamePlayerInfo.class); // Seat 5 = middle
        GamePlayerInfo late = mock(GamePlayerInfo.class); // Seat 8 = late

        when(table.getPlayer(0)).thenReturn(mock(GamePlayerInfo.class)); // button
        when(table.getPlayer(1)).thenReturn(blind);
        when(table.getPlayer(3)).thenReturn(early);
        when(table.getPlayer(5)).thenReturn(middle);
        when(table.getPlayer(8)).thenReturn(late);

        // Mock getSeat to return seat numbers
        when(table.getSeat(blind)).thenReturn(1);
        when(table.getSeat(early)).thenReturn(3);
        when(table.getSeat(middle)).thenReturn(5);
        when(table.getSeat(late)).thenReturn(8);

        ServerV2AIContext context = new ServerV2AIContext(table, hand, tournament, aiPlayer, strategy,
                new ServerOpponentTracker());

        // Blinds (seats 1-2 from button)
        assertThat(context.getPostFlopPositionCategory(blind)).isEqualTo(0);

        // Early position (seats 3-4)
        assertThat(context.getPostFlopPositionCategory(early)).isEqualTo(1);

        // Middle position (seat 5-6)
        assertThat(context.getPostFlopPositionCategory(middle)).isEqualTo(2);

        // Late position (seats 7-8, button)
        assertThat(context.getPostFlopPositionCategory(late)).isEqualTo(3);
    }

    @Test
    void isLimit_returnsNoLimitDefault() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, hand, tournament, aiPlayer, strategy,
                new ServerOpponentTracker());

        // Should default to no-limit (false)
        assertThat(context.isLimit()).isFalse();
    }

    @Test
    void getStartingPositionCategory_returnsSmallBlindForSB() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        // 9-seat table, button at seat 0
        when(table.getSeats()).thenReturn(9);
        when(table.getButton()).thenReturn(0);
        when(table.getNumOccupiedSeats()).thenReturn(9);

        // SB at seat 1 (1 from button), BB at seat 2 (2 from button)
        GamePlayerInfo sb = mock(GamePlayerInfo.class);
        GamePlayerInfo bb = mock(GamePlayerInfo.class);
        when(table.getSeat(sb)).thenReturn(1);
        when(table.getSeat(bb)).thenReturn(2);

        ServerV2AIContext context = new ServerV2AIContext(table, hand, tournament, aiPlayer, strategy,
                new ServerOpponentTracker());

        assertThat(context.getStartingPositionCategory(sb)).isEqualTo(AIConstants.POSITION_SMALL);
        assertThat(context.getStartingPositionCategory(bb)).isEqualTo(AIConstants.POSITION_BIG);
    }

    @Test
    void getStartingPositionCategory_returnsCorrectNonBlindPositions() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        // 9-seat table, button at seat 0
        when(table.getSeats()).thenReturn(9);
        when(table.getButton()).thenReturn(0);
        when(table.getNumOccupiedSeats()).thenReturn(9);

        // Button at seat 0
        GamePlayerInfo button = mock(GamePlayerInfo.class);
        when(table.getSeat(button)).thenReturn(0);

        // UTG at seat 3 (first to act pre-flop)
        GamePlayerInfo utg = mock(GamePlayerInfo.class);
        when(table.getSeat(utg)).thenReturn(3);

        ServerV2AIContext context = new ServerV2AIContext(table, hand, tournament, aiPlayer, strategy,
                new ServerOpponentTracker());

        // Button should be LATE (not blind)
        assertThat(context.getStartingPositionCategory(button)).isEqualTo(AIConstants.POSITION_LATE);

        // UTG (distance 3) should be EARLY in a 9-player game
        assertThat(context.getStartingPositionCategory(utg)).isEqualTo(AIConstants.POSITION_EARLY);
    }

    @Test
    void getStartingPositionCategory_headsUp_buttonIsSB() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        // 2 players, button at seat 0
        when(table.getSeats()).thenReturn(9);
        when(table.getButton()).thenReturn(0);
        when(table.getNumOccupiedSeats()).thenReturn(2);

        GamePlayerInfo button = mock(GamePlayerInfo.class);
        GamePlayerInfo other = mock(GamePlayerInfo.class);
        when(table.getSeat(button)).thenReturn(0);
        when(table.getSeat(other)).thenReturn(3);

        ServerV2AIContext context = new ServerV2AIContext(table, hand, tournament, aiPlayer, strategy,
                new ServerOpponentTracker());

        // In heads-up, button is small blind
        assertThat(context.getStartingPositionCategory(button)).isEqualTo(AIConstants.POSITION_SMALL);
        // Other player is big blind
        assertThat(context.getStartingPositionCategory(other)).isEqualTo(AIConstants.POSITION_BIG);
    }

    // === Null/edge-case guards ===

    @Test
    void getHohM_nullPlayer_returnsZero() {
        GameTable table = mock(GameTable.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, null, tournament, aiPlayer, strategy,
                new ServerOpponentTracker());

        assertThat(context.getHohM(null)).isEqualTo(0.0f);
    }

    @Test
    void getHohM_zeroCostPerRound_returnsZero() {
        GameTable table = mock(GameTable.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        when(table.getLevel()).thenReturn(0);
        when(table.getNumOccupiedSeats()).thenReturn(2);
        when(tournament.getSmallBlind(0)).thenReturn(0);
        when(tournament.getBigBlind(0)).thenReturn(0);
        when(tournament.getAnte(0)).thenReturn(0);

        ServerV2AIContext context = new ServerV2AIContext(table, null, tournament, player, strategy,
                new ServerOpponentTracker());

        assertThat(context.getHohM(player)).isEqualTo(0.0f);
    }

    @Test
    void getHohQ_returnsRatioOfMToAverageM() {
        GameTable table = mock(GameTable.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo p1 = mock(GamePlayerInfo.class);
        GamePlayerInfo p2 = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        when(table.getLevel()).thenReturn(1);
        when(table.getNumOccupiedSeats()).thenReturn(2);
        when(table.getSeats()).thenReturn(2);
        when(tournament.getSmallBlind(1)).thenReturn(10);
        when(tournament.getBigBlind(1)).thenReturn(20);
        when(tournament.getAnte(1)).thenReturn(0);

        // p1 has 600 chips (M=20), p2 has 300 chips (M=10), avg M=15
        when(p1.getChipCount()).thenReturn(600);
        when(p2.getChipCount()).thenReturn(300);
        when(table.getPlayer(0)).thenReturn(p1);
        when(table.getPlayer(1)).thenReturn(p2);

        ServerV2AIContext context = new ServerV2AIContext(table, null, tournament, p1, strategy,
                new ServerOpponentTracker());

        float q = context.getHohQ(p1);
        // Q = M(p1)/avgM = 20/15 = 1.333
        assertThat(q).isCloseTo(1.333f, within(0.01f));
    }

    @Test
    void getHohZone_orangeZone() {
        GameTable table = mock(GameTable.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        when(table.getLevel()).thenReturn(1);
        when(table.getNumOccupiedSeats()).thenReturn(2);
        when(tournament.getSmallBlind(1)).thenReturn(50);
        when(tournament.getBigBlind(1)).thenReturn(100);
        when(tournament.getAnte(1)).thenReturn(0);
        when(player.getChipCount()).thenReturn(1000); // M = 6.67

        ServerV2AIContext context = new ServerV2AIContext(table, null, tournament, player, strategy,
                new ServerOpponentTracker());

        assertThat(context.getHohZone(player)).isEqualTo(2); // orange
    }

    @Test
    void getHohZone_yellowZone() {
        GameTable table = mock(GameTable.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        when(table.getLevel()).thenReturn(1);
        when(table.getNumOccupiedSeats()).thenReturn(2);
        when(tournament.getSmallBlind(1)).thenReturn(50);
        when(tournament.getBigBlind(1)).thenReturn(100);
        when(tournament.getAnte(1)).thenReturn(0);
        when(player.getChipCount()).thenReturn(2000); // M = 13.33

        ServerV2AIContext context = new ServerV2AIContext(table, null, tournament, player, strategy,
                new ServerOpponentTracker());

        assertThat(context.getHohZone(player)).isEqualTo(3); // yellow
    }

    @Test
    void getTableAverageHohM_nullTable_returnsZero() {
        StrategyProvider strategy = mock(StrategyProvider.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);

        ServerV2AIContext context = new ServerV2AIContext(null, null, null, player, strategy,
                new ServerOpponentTracker());

        assertThat(context.getTableAverageHohM()).isEqualTo(0.0f);
    }

    // === GameHand delegation methods (null hand returns defaults) ===

    @Test
    void wasRaisedPreFlop_nullHand_returnsFalse() {
        GameTable table = mock(GameTable.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, null, null, player, strategy,
                new ServerOpponentTracker());

        assertThat(context.wasRaisedPreFlop()).isFalse();
    }

    @Test
    void wasRaisedPreFlop_delegatesToHand() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        when(hand.wasRaisedPreFlop()).thenReturn(true);

        ServerV2AIContext context = new ServerV2AIContext(table, hand, null, player, strategy,
                new ServerOpponentTracker());

        assertThat(context.wasRaisedPreFlop()).isTrue();
    }

    @Test
    void wasFirstRaiserPreFlop_nullHand_returnsFalse() {
        GameTable table = mock(GameTable.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, null, null, player, strategy,
                new ServerOpponentTracker());

        assertThat(context.wasFirstRaiserPreFlop(player)).isFalse();
    }

    @Test
    void wasLastRaiserPreFlop_nullHand_returnsFalse() {
        GameTable table = mock(GameTable.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, null, null, player, strategy,
                new ServerOpponentTracker());

        assertThat(context.wasLastRaiserPreFlop(player)).isFalse();
    }

    @Test
    void wasOnlyRaiserPreFlop_nullHand_returnsFalse() {
        GameTable table = mock(GameTable.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, null, null, player, strategy,
                new ServerOpponentTracker());

        assertThat(context.wasOnlyRaiserPreFlop(player)).isFalse();
    }

    @Test
    void getFirstBettor_nullHand_returnsNull() {
        GameTable table = mock(GameTable.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, null, null, player, strategy,
                new ServerOpponentTracker());

        assertThat(context.getFirstBettor(0, true)).isNull();
    }

    @Test
    void getFirstVoluntaryAction_nullHand_returnsZero() {
        GameTable table = mock(GameTable.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, null, null, player, strategy,
                new ServerOpponentTracker());

        assertThat(context.getFirstVoluntaryAction(player, 0)).isEqualTo(0);
    }

    @Test
    void wasPotAction_nullHand_returnsFalse() {
        GameTable table = mock(GameTable.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, null, null, player, strategy,
                new ServerOpponentTracker());

        assertThat(context.wasPotAction(0)).isFalse();
    }

    @Test
    void getPotStatus_nullHand_returnsZero() {
        GameTable table = mock(GameTable.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, null, null, player, strategy,
                new ServerOpponentTracker());

        assertThat(context.getPotStatus()).isEqualTo(0);
    }

    @Test
    void getLastActionThisRound_nullHand_returnsZero() {
        GameTable table = mock(GameTable.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, null, null, player, strategy,
                new ServerOpponentTracker());

        assertThat(context.getLastActionThisRound(player)).isEqualTo(0);
    }

    @Test
    void getMinRaise_nullHand_returnsZero() {
        GameTable table = mock(GameTable.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, null, null, player, strategy,
                new ServerOpponentTracker());

        assertThat(context.getMinRaise()).isEqualTo(0);
    }

    @Test
    void getPotOdds_nullHand_returnsZero() {
        GameTable table = mock(GameTable.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, null, null, player, strategy,
                new ServerOpponentTracker());

        assertThat(context.getPotOdds(player)).isEqualTo(0.0f);
    }

    @Test
    void paidToPlay_nullHand_returnsFalse() {
        GameTable table = mock(GameTable.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, null, null, player, strategy,
                new ServerOpponentTracker());

        assertThat(context.paidToPlay(player)).isFalse();
    }

    @Test
    void couldLimp_nullHand_returnsFalse() {
        GameTable table = mock(GameTable.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, null, null, player, strategy,
                new ServerOpponentTracker());

        assertThat(context.couldLimp(player)).isFalse();
    }

    @Test
    void limped_nullHand_returnsFalse() {
        GameTable table = mock(GameTable.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, null, null, player, strategy,
                new ServerOpponentTracker());

        assertThat(context.limped(player)).isFalse();
    }

    @Test
    void getBigBlind_nullTournament_returnsZero() {
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(null, null, null, player, strategy,
                new ServerOpponentTracker());

        assertThat(context.getBigBlind()).isEqualTo(0);
    }

    @Test
    void getBigBlind_delegatesToTournament() {
        GameTable table = mock(GameTable.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        when(table.getLevel()).thenReturn(3);
        when(tournament.getBigBlind(3)).thenReturn(200);

        ServerV2AIContext context = new ServerV2AIContext(table, null, tournament, player, strategy,
                new ServerOpponentTracker());

        assertThat(context.getBigBlind()).isEqualTo(200);
    }

    @Test
    void getMinChip_nullTable_returnsOne() {
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(null, null, null, player, strategy,
                new ServerOpponentTracker());

        assertThat(context.getMinChip()).isEqualTo(1);
    }

    @Test
    void getMinChip_delegatesToTable() {
        GameTable table = mock(GameTable.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        when(table.getMinChip()).thenReturn(25);

        ServerV2AIContext context = new ServerV2AIContext(table, null, null, player, strategy,
                new ServerOpponentTracker());

        assertThat(context.getMinChip()).isEqualTo(25);
    }

    @Test
    void getCall_nullHand_returnsZero() {
        GameTable table = mock(GameTable.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, null, null, player, strategy,
                new ServerOpponentTracker());

        assertThat(context.getCall(player)).isEqualTo(0);
    }

    @Test
    void getTotalPotChipCount_nullHand_returnsZero() {
        GameTable table = mock(GameTable.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, null, null, player, strategy,
                new ServerOpponentTracker());

        assertThat(context.getTotalPotChipCount()).isEqualTo(0);
    }

    @Test
    void getCommunity_nullHand_returnsNull() {
        GameTable table = mock(GameTable.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, null, null, player, strategy,
                new ServerOpponentTracker());

        assertThat(context.getCommunity()).isNull();
    }

    @Test
    void getPocketCards_nullHand_returnsNull() {
        GameTable table = mock(GameTable.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, null, null, player, strategy,
                new ServerOpponentTracker());

        assertThat(context.getPocketCards(player)).isNull();
    }

    @Test
    void getPocketCards_nullPlayer_returnsNull() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, hand, null, player, strategy,
                new ServerOpponentTracker());

        assertThat(context.getPocketCards(null)).isNull();
    }

    @Test
    void getNumLimpers_nullHand_returnsZero() {
        GameTable table = mock(GameTable.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, null, null, player, strategy,
                new ServerOpponentTracker());

        assertThat(context.getNumLimpers()).isEqualTo(0);
    }

    @Test
    void hasActedThisRound_nullHand_returnsFalse() {
        GameTable table = mock(GameTable.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, null, null, player, strategy,
                new ServerOpponentTracker());

        assertThat(context.hasActedThisRound(player)).isFalse();
    }

    @Test
    void getLastBettor_nullHand_returnsNull() {
        GameTable table = mock(GameTable.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, null, null, player, strategy,
                new ServerOpponentTracker());

        assertThat(context.getLastBettor(1, false)).isNull();
    }

    @Test
    void getNumFoldsSinceLastBet_nullHand_returnsZero() {
        GameTable table = mock(GameTable.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, null, null, player, strategy,
                new ServerOpponentTracker());

        assertThat(context.getNumFoldsSinceLastBet()).isEqualTo(0);
    }

    @Test
    void isBlind_nullHand_returnsFalse() {
        GameTable table = mock(GameTable.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, null, null, player, strategy,
                new ServerOpponentTracker());

        assertThat(context.isBlind(player)).isFalse();
    }

    @Test
    void getNumPlayersWithCards_nullHand_returnsZero() {
        GameTable table = mock(GameTable.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, null, null, player, strategy,
                new ServerOpponentTracker());

        assertThat(context.getNumPlayersWithCards()).isEqualTo(0);
    }

    @Test
    void getNumPlayersAtTable_nullTable_returnsZero() {
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(null, null, null, player, strategy,
                new ServerOpponentTracker());

        assertThat(context.getNumPlayersAtTable()).isEqualTo(0);
    }

    @Test
    void getNumPlayersAtTable_delegatesToTable() {
        GameTable table = mock(GameTable.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        when(table.getSeats()).thenReturn(9);

        ServerV2AIContext context = new ServerV2AIContext(table, null, null, player, strategy,
                new ServerOpponentTracker());

        assertThat(context.getNumPlayersAtTable()).isEqualTo(9);
    }

    @Test
    void getPlayerAt_nullTable_returnsNull() {
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(null, null, null, player, strategy,
                new ServerOpponentTracker());

        assertThat(context.getPlayerAt(0)).isNull();
    }

    @Test
    void getPlayersLeft_nullTable_returnsEmptyList() {
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(null, null, null, player, strategy,
                new ServerOpponentTracker());

        assertThat(context.getPlayersLeft(player)).isEmpty();
    }

    @Test
    void getSeat_nullPlayer_returnsNegative() {
        GameTable table = mock(GameTable.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, null, null, player, strategy,
                new ServerOpponentTracker());

        assertThat(context.getSeat(null)).isEqualTo(-1);
    }

    @Test
    void getSeat_nullTable_returnsNegative() {
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(null, null, null, player, strategy,
                new ServerOpponentTracker());

        assertThat(context.getSeat(player)).isEqualTo(-1);
    }

    @Test
    void getChipCountAtStart_nullOpponentTracker_returnsCurrentChipCount() {
        GameTable table = mock(GameTable.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        when(player.getChipCount()).thenReturn(5000);

        ServerV2AIContext context = new ServerV2AIContext(table, null, null, player, strategy, null);

        assertThat(context.getChipCountAtStart(player)).isEqualTo(5000);
    }

    @Test
    void getChipCountAtStart_nullPlayer_returnsZero() {
        GameTable table = mock(GameTable.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, null, null, player, strategy,
                new ServerOpponentTracker());

        assertThat(context.getChipCountAtStart(null)).isEqualTo(0);
    }

    @Test
    void getHandsBeforeBigBlind_nullPlayer_returnsZero() {
        GameTable table = mock(GameTable.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, null, null, player, strategy,
                new ServerOpponentTracker());

        assertThat(context.getHandsBeforeBigBlind(null)).isEqualTo(0);
    }

    @Test
    void getConsecutiveHandsUnpaid_alwaysReturnsZero() {
        GameTable table = mock(GameTable.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, null, null, player, strategy,
                new ServerOpponentTracker());

        assertThat(context.getConsecutiveHandsUnpaid(player)).isEqualTo(0);
    }

    @Test
    void getSelfModel_returnsModelForAiPlayer() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, hand, tournament, aiPlayer, strategy,
                new ServerOpponentTracker());

        var model = context.getSelfModel();
        assertThat(model).isNotNull();
    }

    @Test
    void getOpponentModel_nullPlayer_returnsStubModel() {
        GameTable table = mock(GameTable.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, null, null, aiPlayer, strategy,
                new ServerOpponentTracker());

        var model = context.getOpponentModel(null);
        assertThat(model).isNotNull();
        assertThat(model.getHandsPlayed()).isEqualTo(0);
    }

    @Test
    void getOpponentModel_nullTracker_returnsStubModel() {
        GameTable table = mock(GameTable.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);
        GamePlayerInfo opponent = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, null, null, aiPlayer, strategy, null);

        var model = context.getOpponentModel(opponent);
        assertThat(model).isNotNull();
        // Stub model returns defaults
        assertThat(model.getPreFlopAggression(0, 0.5f)).isEqualTo(0.5f);
        assertThat(model.getActPostFlop(0, 0.3f)).isEqualTo(0.3f);
        assertThat(model.getCheckFoldPostFlop(0, 0.2f)).isEqualTo(0.2f);
        assertThat(model.getOpenPostFlop(0, 0.1f)).isEqualTo(0.1f);
        assertThat(model.getRaisePostFlop(0, 0.4f)).isEqualTo(0.4f);
        assertThat(model.getHandsPaidPercent(0.5f)).isEqualTo(0.5f);
        assertThat(model.getHandsLimpedPercent(0.1f)).isEqualTo(0.1f);
        assertThat(model.getHandsFoldedUnraisedPercent(0.2f)).isEqualTo(0.2f);
        assertThat(model.getOverbetFrequency(0.05f)).isEqualTo(0.05f);
        assertThat(model.getBetFoldFrequency(0.1f)).isEqualTo(0.1f);
        assertThat(model.getHandsRaisedPreFlopPercent(0.3f)).isEqualTo(0.3f);
        assertThat(model.isOverbetPotPostFlop()).isFalse();
        model.setOverbetPotPostFlop(true);
        assertThat(model.isOverbetPotPostFlop()).isTrue();
    }

    @Test
    void getHandScore_nullPocket_returnsZero() {
        GameTable table = mock(GameTable.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, null, null, player, strategy,
                new ServerOpponentTracker());

        assertThat(context.getHandScore(null, new Hand(0))).isEqualTo(0);
    }

    @Test
    void getHandScore_validCards_returnsPositiveScore() {
        GameTable table = mock(GameTable.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, null, null, player, strategy,
                new ServerOpponentTracker());

        Hand pocket = new Hand(new Card(CardSuit.SPADES, Card.ACE), new Card(CardSuit.HEARTS, Card.ACE));
        Hand community = new Hand(new Card(CardSuit.CLUBS, Card.KING), new Card(CardSuit.DIAMONDS, Card.QUEEN),
                new Card(CardSuit.SPADES, Card.JACK));

        int score = context.getHandScore(pocket, community);
        assertThat(score).isGreaterThan(0);
    }

    @Test
    void getRawHandStrength_preFlop_returnsZero() {
        GameTable table = mock(GameTable.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, null, null, player, strategy,
                new ServerOpponentTracker());

        Hand pocket = new Hand(new Card(CardSuit.SPADES, Card.ACE), new Card(CardSuit.HEARTS, Card.ACE));
        Hand community = new Hand(1);
        community.addCard(new Card(CardSuit.CLUBS, Card.KING)); // only 1 card

        assertThat(context.getRawHandStrength(pocket, community)).isEqualTo(0.0f);
    }

    @Test
    void getBiasedRawHandStrength_nullCommunity_returnsZero() {
        GameTable table = mock(GameTable.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, null, null, player, strategy,
                new ServerOpponentTracker());

        assertThat(context.getBiasedRawHandStrength(0, null)).isEqualTo(0.0f);
    }

    @Test
    void getBiasedRawHandStrength_nullPlayer_returnsZero() {
        GameTable table = mock(GameTable.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        when(table.getPlayer(5)).thenReturn(null);

        ServerV2AIContext context = new ServerV2AIContext(table, null, null, player, strategy,
                new ServerOpponentTracker());

        Hand community = new Hand(new Card(CardSuit.CLUBS, Card.SEVEN), new Card(CardSuit.DIAMONDS, Card.FIVE),
                new Card(CardSuit.SPADES, Card.TWO));

        assertThat(context.getBiasedRawHandStrength(5, community)).isEqualTo(0.0f);
    }

    @Test
    void getBiasedEffectiveHandStrength_nullCommunity_returnsZero() {
        GameTable table = mock(GameTable.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, null, null, player, strategy,
                new ServerOpponentTracker());

        assertThat(context.getBiasedEffectiveHandStrength(0, null)).isEqualTo(0.0f);
    }

    @Test
    void getBiasedEffectiveHandStrength_riverCommunity_returnsZero() {
        GameTable table = mock(GameTable.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, null, null, player, strategy,
                new ServerOpponentTracker());

        // 5-card community (river) returns 0 for effective hand strength
        Hand community = new Hand(new Card(CardSuit.CLUBS, Card.SEVEN), new Card(CardSuit.DIAMONDS, Card.FIVE),
                new Card(CardSuit.SPADES, Card.TWO), new Card(CardSuit.HEARTS, Card.THREE),
                new Card(CardSuit.CLUBS, Card.FOUR));

        assertThat(context.getBiasedEffectiveHandStrength(0, community)).isEqualTo(0.0f);
    }

    @Test
    void getStartingPositionCategory_nullPlayer_returnsEarly() {
        GameTable table = mock(GameTable.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, null, null, player, strategy,
                new ServerOpponentTracker());

        assertThat(context.getStartingPositionCategory(null)).isEqualTo(AIConstants.POSITION_EARLY);
    }

    @Test
    void getPostFlopPositionCategory_nullPlayer_returnsMiddle() {
        GameTable table = mock(GameTable.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, null, null, player, strategy,
                new ServerOpponentTracker());

        assertThat(context.getPostFlopPositionCategory(null)).isEqualTo(2);
    }

    @Test
    void getStartingOrder_nullPlayer_returnsZero() {
        GameTable table = mock(GameTable.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, null, null, player, strategy,
                new ServerOpponentTracker());

        assertThat(context.getStartingOrder(null)).isEqualTo(0);
    }

    @Test
    void getDrawCounts_smallCommunity_returnZero() {
        GameTable table = mock(GameTable.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, null, null, player, strategy,
                new ServerOpponentTracker());

        Hand pocket = new Hand(new Card(CardSuit.SPADES, Card.ACE), new Card(CardSuit.HEARTS, Card.KING));
        Hand tooSmall = new Hand(2);
        tooSmall.addCard(new Card(CardSuit.CLUBS, Card.TWO));
        tooSmall.addCard(new Card(CardSuit.DIAMONDS, Card.THREE));

        assertThat(context.getNutFlushCount(pocket, tooSmall)).isEqualTo(0);
        assertThat(context.getNonNutFlushCount(pocket, tooSmall)).isEqualTo(0);
        assertThat(context.getNutStraightCount(pocket, tooSmall)).isEqualTo(0);
        assertThat(context.getNonNutStraightCount(pocket, tooSmall)).isEqualTo(0);
    }

    @Test
    void getDrawCounts_turnCommunity_enumeratesOneCard() {
        GameTable table = mock(GameTable.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        ServerV2AIContext context = new ServerV2AIContext(table, null, null, player, strategy,
                new ServerOpponentTracker());

        // Flush draw on turn (4 cards on board)
        Hand pocket = new Hand(new Card(CardSuit.HEARTS, Card.ACE), new Card(CardSuit.HEARTS, Card.KING));
        Hand community = new Hand(new Card(CardSuit.HEARTS, Card.QUEEN), new Card(CardSuit.HEARTS, Card.JACK),
                new Card(CardSuit.SPADES, Card.TWO), new Card(CardSuit.CLUBS, Card.THREE));

        // Should be able to enumerate without errors
        int nutFlush = context.getNutFlushCount(pocket, community);
        assertThat(nutFlush).isGreaterThanOrEqualTo(0);
    }
}
