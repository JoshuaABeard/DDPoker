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

import com.donohoedigital.games.poker.core.*;
import com.donohoedigital.games.poker.core.ai.StrategyProvider;
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

        ServerV2AIContext context = new ServerV2AIContext(table, hand, tournament, player, strategy);

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

        ServerV2AIContext context = new ServerV2AIContext(table, hand, tournament, player, strategy);

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

        ServerV2AIContext context = new ServerV2AIContext(table, hand, tournament, player, strategy);

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

        ServerV2AIContext context = new ServerV2AIContext(table, hand, tournament, player, strategy);

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

        ServerV2AIContext context = new ServerV2AIContext(table, hand, tournament, player, strategy);

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

        ServerV2AIContext context = new ServerV2AIContext(table, hand, tournament, player, strategy);

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

        ServerV2AIContext context = new ServerV2AIContext(table, hand, tournament, player, strategy);

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

        ServerV2AIContext context = new ServerV2AIContext(table, hand, tournament, player, strategy);

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

        ServerV2AIContext context = new ServerV2AIContext(table, hand, tournament, player, strategy);

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

        ServerV2AIContext context = new ServerV2AIContext(table, gameHand, tournament, aiPlayer, strategy);

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

        ServerV2AIContext context = new ServerV2AIContext(table, gameHand, tournament, aiPlayer, strategy);

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

        ServerV2AIContext context = new ServerV2AIContext(table, hand, tournament, aiPlayer, strategy);

        var model = context.getOpponentModel(opponent);

        assertThat(model).isNotNull();
        // Stub model returns default values
        assertThat(model.getPreFlopTightness(0, 0.5f)).isEqualTo(0.5f);
    }
}
