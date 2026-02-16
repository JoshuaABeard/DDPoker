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
import com.donohoedigital.games.poker.core.state.BettingRound;
import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.CardSuit;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ServerAIContext implementation. Tests all newly implemented
 * methods for V1/V2 AI support.
 */
class ServerAIContextTest {

    // ========== Hand Lifecycle Tests ==========

    @Test
    void setCurrentHand_updatesHandReference() {
        GameTable table = mock(GameTable.class);
        GameHand initialHand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);

        ServerAIContext context = new ServerAIContext(table, initialHand, tournament, aiPlayer,
                new ServerOpponentTracker());
        assertThat(context.getCurrentHand()).isSameAs(initialHand);

        GameHand newHand = mock(GameHand.class);
        context.setCurrentHand(newHand);

        assertThat(context.getCurrentHand()).isSameAs(newHand);
    }

    // ========== Betting Round Tests ==========

    @Test
    void getBettingRound_returnsHandRound() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);

        when(hand.getRound()).thenReturn(BettingRound.TURN);

        ServerAIContext context = new ServerAIContext(table, hand, tournament, aiPlayer, new ServerOpponentTracker());

        assertThat(context.getBettingRound()).isEqualTo(2); // TURN = 2
    }

    @Test
    void getBettingRound_nullHand_returnsZero() {
        GameTable table = mock(GameTable.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);

        ServerAIContext context = new ServerAIContext(table, null, tournament, aiPlayer, new ServerOpponentTracker());

        assertThat(context.getBettingRound()).isEqualTo(0); // PRE_FLOP = 0
    }

    // ========== Card Access Tests ==========

    @Test
    void getHoleCards_ownPlayer_returnsCards() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);

        Card[] cards = new Card[]{new Card(CardSuit.SPADES, Card.ACE), new Card(CardSuit.HEARTS, Card.KING)};
        when(hand.getPlayerCards(aiPlayer)).thenReturn(cards);

        ServerAIContext context = new ServerAIContext(table, hand, tournament, aiPlayer, new ServerOpponentTracker());

        Card[] result = context.getHoleCards(aiPlayer);

        assertThat(result).isEqualTo(cards);
    }

    @Test
    void getHoleCards_otherPlayer_returnsNull_security() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);
        GamePlayerInfo opponent = mock(GamePlayerInfo.class);

        Card[] opponentCards = new Card[]{new Card(CardSuit.CLUBS, Card.ACE), new Card(CardSuit.DIAMONDS, Card.KING)};
        when(hand.getPlayerCards(opponent)).thenReturn(opponentCards);

        ServerAIContext context = new ServerAIContext(table, hand, tournament, aiPlayer, new ServerOpponentTracker());

        // Security: AI should not see opponent's cards
        Card[] result = context.getHoleCards(opponent);

        assertThat(result).isNull();
    }

    @Test
    void getCommunityCards_returnsCards() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);

        Card[] community = new Card[]{new Card(CardSuit.HEARTS, Card.TEN), new Card(CardSuit.DIAMONDS, Card.JACK),
                new Card(CardSuit.CLUBS, Card.QUEEN)};
        when(hand.getCommunityCards()).thenReturn(community);

        ServerAIContext context = new ServerAIContext(table, hand, tournament, aiPlayer, new ServerOpponentTracker());

        Card[] result = context.getCommunityCards();

        assertThat(result).isEqualTo(community);
    }

    // ========== Pot and Betting Tests ==========

    @Test
    void getPotSize_returnsPot() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);

        when(hand.getPotSize()).thenReturn(450);

        ServerAIContext context = new ServerAIContext(table, hand, tournament, aiPlayer, new ServerOpponentTracker());

        assertThat(context.getPotSize()).isEqualTo(450);
    }

    @Test
    void getAmountToCall_returnsCallAmount() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);

        when(hand.getAmountToCall(player)).thenReturn(100);

        ServerAIContext context = new ServerAIContext(table, hand, tournament, aiPlayer, new ServerOpponentTracker());

        assertThat(context.getAmountToCall(player)).isEqualTo(100);
    }

    @Test
    void getNumActivePlayers_returnsPlayersWithCards() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);

        when(hand.getNumWithCards()).thenReturn(4);

        ServerAIContext context = new ServerAIContext(table, hand, tournament, aiPlayer, new ServerOpponentTracker());

        assertThat(context.getNumActivePlayers()).isEqualTo(4);
    }

    @Test
    void hasBeenBet_returnsTrue_whenPotAction() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);

        when(hand.getRound()).thenReturn(BettingRound.FLOP);
        when(hand.wasPotAction(1)).thenReturn(true);

        ServerAIContext context = new ServerAIContext(table, hand, tournament, aiPlayer, new ServerOpponentTracker());

        assertThat(context.hasBeenBet()).isTrue();
    }

    @Test
    void hasBeenRaised_preFlop_returnsTrue_whenRaised() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);

        when(hand.getRound()).thenReturn(BettingRound.PRE_FLOP);
        when(hand.wasRaisedPreFlop()).thenReturn(true);

        ServerAIContext context = new ServerAIContext(table, hand, tournament, aiPlayer, new ServerOpponentTracker());

        assertThat(context.hasBeenRaised()).isTrue();
    }

    @Test
    void hasBeenRaised_postFlop_returnsTrue_whenRaiserExists() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);
        GamePlayerInfo raiser = mock(GamePlayerInfo.class);

        when(hand.getRound()).thenReturn(BettingRound.FLOP);
        when(hand.getLastBettor(1, false)).thenReturn(raiser); // false = raises only

        ServerAIContext context = new ServerAIContext(table, hand, tournament, aiPlayer, new ServerOpponentTracker());

        assertThat(context.hasBeenRaised()).isTrue();
    }

    @Test
    void getLastBettor_returnsBettor() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);
        GamePlayerInfo bettor = mock(GamePlayerInfo.class);

        when(hand.getRound()).thenReturn(BettingRound.TURN);
        when(hand.getLastBettor(2, true)).thenReturn(bettor);

        ServerAIContext context = new ServerAIContext(table, hand, tournament, aiPlayer, new ServerOpponentTracker());

        assertThat(context.getLastBettor()).isSameAs(bettor);
    }

    @Test
    void getLastRaiser_returnsRaiser() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);
        GamePlayerInfo raiser = mock(GamePlayerInfo.class);

        when(hand.getRound()).thenReturn(BettingRound.FLOP);
        when(hand.getLastBettor(1, false)).thenReturn(raiser);

        ServerAIContext context = new ServerAIContext(table, hand, tournament, aiPlayer, new ServerOpponentTracker());

        assertThat(context.getLastRaiser()).isSameAs(raiser);
    }

    @Test
    void getNumCallers_returnsLimpers() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);

        when(hand.getNumLimpers()).thenReturn(2);

        ServerAIContext context = new ServerAIContext(table, hand, tournament, aiPlayer, new ServerOpponentTracker());

        assertThat(context.getNumCallers()).isEqualTo(2);
    }

    // ========== Position Tests ==========

    @Test
    void isButton_returnsTrue_whenPlayerIsButton() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);

        when(table.getButton()).thenReturn(3);
        when(table.getSeat(player)).thenReturn(3);

        ServerAIContext context = new ServerAIContext(table, hand, tournament, aiPlayer, new ServerOpponentTracker());

        assertThat(context.isButton(player)).isTrue();
    }

    @Test
    void isButton_returnsFalse_whenPlayerNotButton() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);

        when(table.getButton()).thenReturn(3);
        when(table.getSeat(player)).thenReturn(5);

        ServerAIContext context = new ServerAIContext(table, hand, tournament, aiPlayer, new ServerOpponentTracker());

        assertThat(context.isButton(player)).isFalse();
    }

    @Test
    void isSmallBlind_headsUp_buttonIsSmallBlind() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);

        when(table.getButton()).thenReturn(2);
        when(table.getSeats()).thenReturn(6);
        when(table.getNumOccupiedSeats()).thenReturn(2); // Heads-up
        when(table.getSeat(player)).thenReturn(2);

        ServerAIContext context = new ServerAIContext(table, hand, tournament, aiPlayer, new ServerOpponentTracker());

        assertThat(context.isSmallBlind(player)).isTrue();
    }

    @Test
    void isSmallBlind_multiPlayer_oneAfterButton() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);

        when(table.getButton()).thenReturn(2);
        when(table.getSeats()).thenReturn(6);
        when(table.getNumOccupiedSeats()).thenReturn(4); // Not heads-up
        when(table.getSeat(player)).thenReturn(3); // 1 after button

        ServerAIContext context = new ServerAIContext(table, hand, tournament, aiPlayer, new ServerOpponentTracker());

        assertThat(context.isSmallBlind(player)).isTrue();
    }

    @Test
    void isBigBlind_headsUp_nonButtonIsBigBlind() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        GamePlayerInfo otherPlayer = mock(GamePlayerInfo.class);

        when(table.getButton()).thenReturn(2);
        when(table.getSeats()).thenReturn(6);
        when(table.getNumOccupiedSeats()).thenReturn(2); // Heads-up
        when(table.getSeat(player)).thenReturn(4);
        when(table.getPlayer(4)).thenReturn(otherPlayer);

        ServerAIContext context = new ServerAIContext(table, hand, tournament, aiPlayer, new ServerOpponentTracker());

        assertThat(context.isBigBlind(player)).isTrue();
    }

    @Test
    void isBigBlind_multiPlayer_twoAfterButton() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);

        when(table.getButton()).thenReturn(2);
        when(table.getSeats()).thenReturn(6);
        when(table.getNumOccupiedSeats()).thenReturn(4); // Not heads-up
        when(table.getSeat(player)).thenReturn(4); // 2 after button

        ServerAIContext context = new ServerAIContext(table, hand, tournament, aiPlayer, new ServerOpponentTracker());

        assertThat(context.isBigBlind(player)).isTrue();
    }

    @Test
    void getPosition_returnsDistanceFromButton() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);

        when(table.getButton()).thenReturn(2);
        when(table.getSeats()).thenReturn(6);
        when(table.getSeat(player)).thenReturn(4); // 2 seats after button

        ServerAIContext context = new ServerAIContext(table, hand, tournament, aiPlayer, new ServerOpponentTracker());

        // Position 0 = button, position increases clockwise
        assertThat(context.getPosition(player)).isEqualTo(2);
    }

    @Test
    void getPosition_wrapsAround() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);

        when(table.getButton()).thenReturn(5);
        when(table.getSeats()).thenReturn(6);
        when(table.getSeat(player)).thenReturn(1); // Wraps around

        ServerAIContext context = new ServerAIContext(table, hand, tournament, aiPlayer, new ServerOpponentTracker());

        // (1 - 5 + 6) % 6 = 2
        assertThat(context.getPosition(player)).isEqualTo(2);
    }

    // ========== Player Action Tracking Tests ==========

    @Test
    void getNumPlayersYetToAct_countsCorrectly() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);

        GamePlayerInfo p1 = mock(GamePlayerInfo.class);
        GamePlayerInfo p2 = mock(GamePlayerInfo.class);
        GamePlayerInfo p3 = mock(GamePlayerInfo.class);

        when(table.getSeats()).thenReturn(6);
        when(table.getSeat(player)).thenReturn(2);
        when(table.getPlayer(3)).thenReturn(p1);
        when(table.getPlayer(4)).thenReturn(p2);
        when(table.getPlayer(5)).thenReturn(p3);

        when(hand.hasActedThisRound(p1)).thenReturn(false);
        when(hand.hasActedThisRound(p2)).thenReturn(false);
        when(hand.hasActedThisRound(p3)).thenReturn(true); // Already acted

        ServerAIContext context = new ServerAIContext(table, hand, tournament, aiPlayer, new ServerOpponentTracker());

        // 2 players after player haven't acted yet
        assertThat(context.getNumPlayersYetToAct(player)).isEqualTo(2);
    }

    @Test
    void getNumPlayersWhoActed_countsCorrectly() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);

        GamePlayerInfo p1 = mock(GamePlayerInfo.class);
        GamePlayerInfo p2 = mock(GamePlayerInfo.class);
        GamePlayerInfo p3 = mock(GamePlayerInfo.class);

        when(table.getSeats()).thenReturn(6);
        when(table.getPlayer(0)).thenReturn(p1);
        when(table.getPlayer(1)).thenReturn(p2);
        when(table.getPlayer(2)).thenReturn(p3);

        when(hand.hasActedThisRound(p1)).thenReturn(true);
        when(hand.hasActedThisRound(p2)).thenReturn(false);
        when(hand.hasActedThisRound(p3)).thenReturn(true);

        ServerAIContext context = new ServerAIContext(table, hand, tournament, aiPlayer, new ServerOpponentTracker());

        // 2 players have acted this round
        assertThat(context.getNumPlayersWhoActed(player)).isEqualTo(2);
    }

    // ========== Null Safety Tests ==========

    @Test
    void nullHand_methodsReturnSafeDefaults() {
        GameTable table = mock(GameTable.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);

        ServerAIContext context = new ServerAIContext(table, null, tournament, aiPlayer, new ServerOpponentTracker());

        assertThat(context.getBettingRound()).isEqualTo(0);
        assertThat(context.getHoleCards(player)).isNull();
        assertThat(context.getCommunityCards()).isNull();
        assertThat(context.getPotSize()).isEqualTo(0);
        assertThat(context.getAmountToCall(player)).isEqualTo(0);
        assertThat(context.getNumActivePlayers()).isEqualTo(0);
        assertThat(context.hasBeenBet()).isFalse();
        assertThat(context.hasBeenRaised()).isFalse();
        assertThat(context.getLastBettor()).isNull();
        assertThat(context.getLastRaiser()).isNull();
        assertThat(context.getNumCallers()).isEqualTo(0);
    }

    @Test
    void nullTable_positionMethodsReturnSafeDefaults() {
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);

        ServerAIContext context = new ServerAIContext(null, hand, tournament, aiPlayer, new ServerOpponentTracker());

        assertThat(context.isButton(player)).isFalse();
        assertThat(context.isSmallBlind(player)).isFalse();
        assertThat(context.isBigBlind(player)).isFalse();
        assertThat(context.getPosition(player)).isEqualTo(0);
    }

    // ========== Action Tracking Tests (Step 5a) ==========

    @Test
    void onPlayerAction_tracksAction() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);

        when(player.getID()).thenReturn(1);

        ServerAIContext context = new ServerAIContext(table, hand, tournament, aiPlayer, new ServerOpponentTracker());

        context.onPlayerAction(player, com.donohoedigital.games.poker.HandAction.ACTION_BET, 100, 1);

        int lastAction = context.getLastActionInRound(player, 1);
        assertThat(lastAction).isEqualTo(com.donohoedigital.games.poker.HandAction.ACTION_BET);
    }

    @Test
    void getAmountBetThisRound_returnsTrackedAmount() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);

        when(player.getID()).thenReturn(1);
        when(hand.getRound()).thenReturn(com.donohoedigital.games.poker.core.state.BettingRound.FLOP);

        ServerAIContext context = new ServerAIContext(table, hand, tournament, aiPlayer, new ServerOpponentTracker());

        context.onPlayerAction(player, com.donohoedigital.games.poker.HandAction.ACTION_BET, 100, 1);
        context.onPlayerAction(player, com.donohoedigital.games.poker.HandAction.ACTION_RAISE, 200, 1);

        int amount = context.getAmountBetThisRound(player);
        assertThat(amount).isEqualTo(300); // 100 + 200
    }

    @Test
    void getLastBetAmount_returnsLastBet() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);
        GamePlayerInfo player1 = mock(GamePlayerInfo.class);
        GamePlayerInfo player2 = mock(GamePlayerInfo.class);

        when(player1.getID()).thenReturn(1);
        when(player2.getID()).thenReturn(2);
        when(hand.getRound()).thenReturn(com.donohoedigital.games.poker.core.state.BettingRound.FLOP);

        ServerAIContext context = new ServerAIContext(table, hand, tournament, aiPlayer, new ServerOpponentTracker());

        context.onPlayerAction(player1, com.donohoedigital.games.poker.HandAction.ACTION_BET, 100, 1);
        context.onPlayerAction(player2, com.donohoedigital.games.poker.HandAction.ACTION_RAISE, 250, 1);

        int lastBet = context.getLastBetAmount();
        assertThat(lastBet).isEqualTo(250); // Last raise amount
    }

    @Test
    void setCurrentHand_clearsActionTracking() {
        GameTable table = mock(GameTable.class);
        GameHand hand1 = mock(GameHand.class);
        GameHand hand2 = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);

        when(player.getID()).thenReturn(1);
        when(hand1.getRound()).thenReturn(com.donohoedigital.games.poker.core.state.BettingRound.FLOP);
        when(hand2.getRound()).thenReturn(com.donohoedigital.games.poker.core.state.BettingRound.FLOP);

        ServerAIContext context = new ServerAIContext(table, hand1, tournament, aiPlayer, new ServerOpponentTracker());

        context.onPlayerAction(player, com.donohoedigital.games.poker.HandAction.ACTION_BET, 100, 1);
        assertThat(context.getAmountBetThisRound(player)).isEqualTo(100);

        // New hand should clear tracking
        context.setCurrentHand(hand2);

        assertThat(context.getAmountBetThisRound(player)).isEqualTo(0);
        assertThat(context.getLastBetAmount()).isEqualTo(0);
    }

    @Test
    void actionTracking_separatesByRound() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);
        GamePlayerInfo player = mock(GamePlayerInfo.class);

        when(player.getID()).thenReturn(1);
        when(hand.getRound()).thenReturn(com.donohoedigital.games.poker.core.state.BettingRound.FLOP);

        ServerAIContext context = new ServerAIContext(table, hand, tournament, aiPlayer, new ServerOpponentTracker());

        // Bet on flop
        context.onPlayerAction(player, com.donohoedigital.games.poker.HandAction.ACTION_BET, 100, 1);
        // Raise on turn
        context.onPlayerAction(player, com.donohoedigital.games.poker.HandAction.ACTION_RAISE, 200, 2);

        int flopAction = context.getLastActionInRound(player, 1);
        int turnAction = context.getLastActionInRound(player, 2);

        assertThat(flopAction).isEqualTo(com.donohoedigital.games.poker.HandAction.ACTION_BET);
        assertThat(turnAction).isEqualTo(com.donohoedigital.games.poker.HandAction.ACTION_RAISE);
    }

    @Test
    void opponentFrequency_usesTrackerData() {
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        GamePlayerInfo aiPlayer = mock(GamePlayerInfo.class);
        GamePlayerInfo opponent = mock(GamePlayerInfo.class);

        when(opponent.getID()).thenReturn(2);

        ServerOpponentTracker tracker = new ServerOpponentTracker();
        ServerAIContext context = new ServerAIContext(table, hand, tournament, aiPlayer, tracker);

        // Simulate opponent playing hands
        for (int i = 0; i < 5; i++) {
            tracker.onHandStart(opponent, 1000);
            tracker.onPlayerAction(opponent, com.donohoedigital.games.poker.HandAction.ACTION_RAISE, 100, 0, 2);
            tracker.onHandEnd(opponent);
        }

        int raiseFreq = context.getOpponentRaiseFrequency(opponent, 0);
        // 5 raises out of 5 hands = 100%
        assertThat(raiseFreq).isEqualTo(100);
    }
}
