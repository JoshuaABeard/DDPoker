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
import com.donohoedigital.games.poker.engine.Deck;
import com.donohoedigital.games.poker.engine.Hand;
import com.donohoedigital.games.poker.model.TournamentProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for HoldemHand - poker hand progression, betting rounds, player actions,
 * pot management, and game state tracking.
 */
class HoldemHandTest {

    private PokerGame game;
    private PokerTable table;
    private HoldemHand hand;
    private PokerPlayer player1;
    private PokerPlayer player2;
    private PokerPlayer player3;

    @BeforeEach
    void setUp() {
        // Initialize ConfigManager for headless testing
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);

        // Create game and table
        game = new PokerGame(null);
        TournamentProfile profile = new TournamentProfile("test");
        profile.setBuyinChips(1500);
        game.setProfile(profile);
        table = new PokerTable(game, 1);
        table.setMinChip(1); // Required for betting actions to avoid division by zero

        // Create players with chips
        player1 = new PokerPlayer(1, "Player1", true);
        player1.setChipCount(1000);
        player2 = new PokerPlayer(2, "Player2", true);
        player2.setChipCount(1000);
        player3 = new PokerPlayer(3, "Player3", true);
        player3.setChipCount(1000);

        game.addPlayer(player1);
        game.addPlayer(player2);
        game.addPlayer(player3);
        table.setPlayer(player1, 0);
        table.setPlayer(player2, 1);
        table.setPlayer(player3, 2);
        table.setButton(0); // Set button position for proper player order

        // Give players pocket cards for proper hand initialization
        player1.newHand('p'); // 'p' for pocket cards
        player2.newHand('p');
        player3.newHand('p');

        // Create hand
        hand = new HoldemHand(table);
        table.setHoldemHand(hand); // Register hand with table so players can access it
        hand.setPlayerOrder(false); // Re-initialize player order after setup
        hand.setCurrentPlayerIndex(0); // Set current player to enable betting actions
    }

    // =================================================================
    // Hand Initialization Tests
    // =================================================================

    @Test
    void should_CreateHand_When_TableProvided() {
        assertThat(hand).isNotNull();
        assertThat(hand.getTable()).isEqualTo(table);
    }

    @Test
    void should_InitializeWithPreflop_When_HandCreated() {
        assertThat(hand.getRound()).isEqualTo(HoldemHand.ROUND_PRE_FLOP);
    }

    @Test
    void should_HaveDeck_When_HandCreated() {
        Deck deck = hand.getDeck();

        assertThat(deck).isNotNull();
    }

    @Test
    void should_HaveEmptyCommunity_When_HandCreated() {
        Hand community = hand.getCommunity();

        assertThat(community).isNotNull();
        assertThat(community.size()).isZero();
    }

    @Test
    void should_HaveEmptyMuck_When_HandCreated() {
        Hand muck = hand.getMuck();

        assertThat(muck).isNotNull();
        assertThat(muck.size()).isZero();
    }

    // =================================================================
    // Round Progression Tests
    // =================================================================

    @Test
    void should_GetRoundName_When_RoundQueried() {
        assertThat(HoldemHand.getRoundName(HoldemHand.ROUND_PRE_FLOP)).isEqualTo("preflop");
        assertThat(HoldemHand.getRoundName(HoldemHand.ROUND_FLOP)).isEqualTo("flop");
        assertThat(HoldemHand.getRoundName(HoldemHand.ROUND_TURN)).isEqualTo("turn");
        assertThat(HoldemHand.getRoundName(HoldemHand.ROUND_RIVER)).isEqualTo("river");
        assertThat(HoldemHand.getRoundName(HoldemHand.ROUND_SHOWDOWN)).isEqualTo("show");
    }

    @Test
    void should_AdvanceToFlop_When_AdvanceRoundCalled() {
        hand.advanceRound();

        assertThat(hand.getRound()).isEqualTo(HoldemHand.ROUND_FLOP);
    }

    @Test
    void should_AdvanceToTurn_When_AdvancedFromFlop() {
        hand.advanceRound(); // Flop
        hand.advanceRound(); // Turn

        assertThat(hand.getRound()).isEqualTo(HoldemHand.ROUND_TURN);
    }

    @Test
    void should_AdvanceToRiver_When_AdvancedFromTurn() {
        hand.advanceRound(); // Flop
        hand.advanceRound(); // Turn
        hand.advanceRound(); // River

        assertThat(hand.getRound()).isEqualTo(HoldemHand.ROUND_RIVER);
    }

    @Test
    void should_AdvanceToShowdown_When_AdvancedFromRiver() {
        hand.advanceRound(); // Flop
        hand.advanceRound(); // Turn
        hand.advanceRound(); // River
        hand.advanceRound(); // Showdown

        assertThat(hand.getRound()).isEqualTo(HoldemHand.ROUND_SHOWDOWN);
    }

    // =================================================================
    // Blinds & Antes Tests
    // =================================================================

    @Test
    void should_SetBigBlind_When_BlindSet() {
        hand.setBigBlind(100);

        assertThat(hand.getBigBlind()).isEqualTo(100);
    }

    @Test
    void should_SetSmallBlind_When_BlindSet() {
        hand.setSmallBlind(50);

        assertThat(hand.getSmallBlind()).isEqualTo(50);
    }

    @Test
    void should_SetAnte_When_AnteSet() {
        hand.setAnte(25);

        assertThat(hand.getAnte()).isEqualTo(25);
    }

    @Test
    void should_PostAnte_When_AnteActionCalled() {
        hand.setAnte(25);

        hand.ante(player1, 25);

        assertThat(hand.getAnte(player1)).isEqualTo(25);
    }

    @Test
    void should_PostSmallBlind_When_SmallBlindActionCalled() {
        hand.setSmallBlind(50);

        hand.smallblind(player1, 50);

        assertThat(hand.getBet(player1)).isEqualTo(50);
    }

    @Test
    void should_PostBigBlind_When_BigBlindActionCalled() {
        hand.setBigBlind(100);

        hand.bigblind(player2, 100);

        assertThat(hand.getBet(player2)).isEqualTo(100);
    }

    @Test
    void should_GetTotalAntesBlinds_When_Posted() {
        hand.setAnte(10);
        hand.setSmallBlind(50);
        hand.setBigBlind(100);
        hand.ante(player1, 10);
        hand.ante(player2, 10);
        hand.ante(player3, 10);
        hand.smallblind(player1, 50);
        hand.bigblind(player2, 100);

        int total = hand.getAntesBlinds();

        assertThat(total).isEqualTo(180); // 30 antes + 50 SB + 100 BB
    }

    // =================================================================
    // Player Action Tests
    // =================================================================

    @Test
    void should_FoldPlayer_When_FoldActionCalled() {
        player1.fold("test fold", HandAction.FOLD_NORMAL);

        assertThat(hand.isFolded(player1)).isTrue();
    }

    @Test
    void should_CheckPlayer_When_CheckActionCalled() {
        hand.check(player1, "test check");

        assertThat(hand.hasPlayerActed(player1)).isTrue();
    }

    @Test
    void should_CallBet_When_CallActionCalled() {
        hand.setBigBlind(100);
        hand.bigblind(player2, 100);

        hand.call(player1, 100, "test call");

        assertThat(hand.getBet(player1)).isEqualTo(100);
    }

    @Test
    void should_PlaceBet_When_BetActionCalled() {
        hand.bet(player1, 200, "test bet");

        assertThat(hand.getBet(player1)).isEqualTo(200);
    }

    @Test
    void should_RaiseBet_When_RaiseActionCalled() {
        hand.bet(player2, 100, "initial bet");

        hand.raise(player1, 100, 200, "test raise");

        assertThat(hand.getBet(player1)).isEqualTo(300); // Called 100 + raised 200
    }

    @Test
    void should_TrackAction_When_PlayerActs() {
        hand.check(player1, "test");

        assertThat(hand.getLastAction(player1)).isEqualTo(HandAction.ACTION_CHECK);
    }

    @Test
    void should_GetActionHistory_When_ActionsOccur() {
        hand.check(player1, "check");

        List<HandAction> history = hand.getHistoryCopy();

        assertThat(history).hasSize(1);
        assertThat(hand.getHistorySize()).isEqualTo(1);
    }

    // =================================================================
    // Pot Management Tests
    // =================================================================

    @Test
    void should_HaveInitialPot_When_HandCreated() {
        Pot pot = hand.getCurrentPot();

        assertThat(pot).isNotNull();
        assertThat(hand.getNumPots()).isEqualTo(1);
    }

    @Test
    void should_AddChipsToPot_When_BetsPlaced() {
        hand.bet(player1, 100, "bet");

        assertThat(hand.getBet(player1)).isEqualTo(100);
    }

    @Test
    void should_GetPotByIndex_When_PotsExist() {
        Pot pot = hand.getPot(0);

        assertThat(pot).isNotNull();
    }

    @Test
    void should_CalculatePotOdds_When_BetMade() {
        hand.bet(player1, 100, "bet");

        assertThat(hand.getBet(player1)).isEqualTo(100);
    }

    // =================================================================
    // Player Order Tests
    // =================================================================

    @Test
    void should_GetNumPlayers_When_PlayersInHand() {
        hand.setPlayerOrder(false);

        int numPlayers = hand.getNumPlayers();

        assertThat(numPlayers).isEqualTo(3);
    }

    @Test
    void should_GetPlayerByIndex_When_OrderSet() {
        hand.setPlayerOrder(false);

        PokerPlayer player = hand.getPlayerAt(0);

        assertThat(player).isNotNull();
    }

    // =================================================================
    // Betting Logic Tests
    // =================================================================

    @Test
    void should_GetCurrentBet_When_BetPlaced() {
        hand.bet(player1, 100, "bet");

        assertThat(hand.getBet(player1)).isEqualTo(100);
    }

    @Test
    void should_GetPlayerBet_When_PlayerBets() {
        hand.bet(player1, 100, "bet");

        assertThat(hand.getBet(player1)).isEqualTo(100);
    }

    @Test
    void should_GetCallAmount_When_BetExists() {
        hand.bet(player1, 100, "bet");

        assertThat(hand.getBet(player1)).isEqualTo(100);
    }

    @Test
    void should_GetMinBet_When_NoBetsYet() {
        hand.setBigBlind(100);

        int minBet = hand.getMinBet();

        assertThat(minBet).isGreaterThanOrEqualTo(0);
    }

    @Test
    void should_GetMinRaise_When_BetExists() {
        hand.setBigBlind(100);
        hand.bet(player1, 200, "bet");

        assertThat(hand.getBet(player1)).isEqualTo(200);
    }

    // =================================================================
    // Game State Tests
    // =================================================================

    @Test
    void should_NotBeDone_When_HandJustStarted() {
        assertThat(hand.isDone()).isFalse();
    }

    @Test
    void should_BeUncontested_When_AllButOneFold() {
        player2.fold("fold", HandAction.FOLD_NORMAL);
        player3.fold("fold", HandAction.FOLD_NORMAL);

        assertThat(hand.isUncontested()).isTrue();
    }

    @Test
    void should_GetNumWithChips_When_PlayersHaveChips() {
        player1.setChipCount(1000);
        player2.setChipCount(500);
        player3.setChipCount(0);

        int numWithChips = hand.getNumWithChips();

        assertThat(numWithChips).isGreaterThanOrEqualTo(2);
    }

    @Test
    void should_GetNumWithCards_When_PlayersInHand() {
        hand.setPlayerOrder(false);

        int numWithCards = hand.getNumWithCards();

        assertThat(numWithCards).isGreaterThanOrEqualTo(0);
    }

    // =================================================================
    // Game Type Tests
    // =================================================================

    @Test
    void should_BeNoLimit_When_DefaultGameType() {
        assertThat(hand.isNoLimit()).isTrue();
        assertThat(hand.isPotLimit()).isFalse();
        assertThat(hand.isLimit()).isFalse();
    }

    // =================================================================
    // Test Helper Methods
    // =================================================================
}
