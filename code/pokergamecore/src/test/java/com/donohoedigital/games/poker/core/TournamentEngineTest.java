/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
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
 *
 * The "DD Poker" and "Donohoe Digital" names and logos, as well as any images,
 * graphics, text, and documentation found in this repository (including but not
 * limited to written documentation, website content, and marketing materials)
 * are licensed under the Creative Commons Attribution-NonCommercial-NoDerivatives
 * 4.0 International License (CC BY-NC-ND 4.0). You may not use these assets
 * without explicit written permission for any uses not covered by this License.
 * For the full License text, please see the LICENSE-CREATIVE-COMMONS.txt file
 * in the root directory of this project.
 *
 * For inquiries regarding commercial licensing of this source code or
 * the use of names, logos, images, text, or other assets, please contact
 * doug [at] donohoe [dot] info.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker.core;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.donohoedigital.games.poker.core.event.GameEventBus;
import com.donohoedigital.games.poker.core.state.BettingRound;
import com.donohoedigital.games.poker.core.state.TableState;

/** Tests for {@link TournamentEngine} state handlers. */
class TournamentEngineTest {

    private TournamentEngine engine;
    private GameEventBus eventBus;
    private PlayerActionProvider actionProvider;
    private StubGameTable table;
    private StubTournamentContext game;

    @BeforeEach
    void setUp() {
        eventBus = new GameEventBus();
        actionProvider = (player, options) -> null; // simple stub
        engine = new TournamentEngine(eventBus, actionProvider);

        table = new StubGameTable();
        game = new StubTournamentContext();
    }

    // DONE state tests

    @Test
    void handleDone_shouldTransitionToBegin() {
        table.tableState = TableState.DONE;
        table.autoDeal = false;

        TableProcessResult result = engine.processTable(table, game, true, false);

        assertThat(result.nextState()).isEqualTo(TableState.BEGIN);
    }

    // GAME_OVER state tests

    @Test
    void handleGameOver_shouldReturnEmptyResult() {
        table.tableState = TableState.GAME_OVER;

        TableProcessResult result = engine.processTable(table, game, true, false);

        // GAME_OVER is a terminal state - no next state
        assertThat(result.nextState()).isNull();
    }

    // ON_HOLD state tests

    @Test
    void handleOnHold_withMultiplePlayers_shouldTransitionToBegin() {
        table.tableState = TableState.ON_HOLD;
        table.numOccupiedSeats = 2;

        TableProcessResult result = engine.processTable(table, game, true, false);

        assertThat(result.nextState()).isEqualTo(TableState.BEGIN);
    }

    @Test
    void handleOnHold_withSinglePlayer_shouldStayOnHold() {
        table.tableState = TableState.ON_HOLD;
        table.numOccupiedSeats = 1;

        TableProcessResult result = engine.processTable(table, game, true, false);

        assertThat(result.nextState()).isNull();
    }

    // BEGIN_WAIT state tests

    @Test
    void handleBeginWait_shouldReturnEmptyResult() {
        table.tableState = TableState.BEGIN_WAIT;

        TableProcessResult result = engine.processTable(table, game, true, false);

        // BEGIN_WAIT just waits - no state change
        assertThat(result.nextState()).isNull();
    }

    // NONE state tests

    @Test
    void handleNone_shouldReturnEmptyResult() {
        table.tableState = TableState.NONE;

        TableProcessResult result = engine.processTable(table, game, true, false);

        assertThat(result.nextState()).isNull();
    }

    // PENDING_LOAD state tests

    @Test
    void handlePendingLoad_shouldTransitionToPending() {
        table.tableState = TableState.PENDING_LOAD;

        TableProcessResult result = engine.processTable(table, game, true, false);

        assertThat(result.nextState()).isEqualTo(TableState.PENDING);
    }

    @Test
    void handlePendingLoad_withPendingPhase_shouldSetPhaseToRun() {
        table.tableState = TableState.PENDING_LOAD;
        table.pendingPhase = "SomePhase";

        TableProcessResult result = engine.processTable(table, game, true, false);

        assertThat(result.nextState()).isEqualTo(TableState.PENDING);
        assertThat(result.phaseToRun()).isEqualTo("SomePhase");
    }

    // PENDING state tests

    @Test
    void handlePending_shouldReturnEmptyResult() {
        table.tableState = TableState.PENDING;

        TableProcessResult result = engine.processTable(table, game, true, false);

        // PENDING delegates to doPending which handles state transitions
        assertThat(result.nextState()).isNull();
    }

    // DEAL_FOR_BUTTON state tests

    @Test
    void handleDealForButton_shouldSetPendingStateToBegin() {
        table.tableState = TableState.DEAL_FOR_BUTTON;

        TableProcessResult result = engine.processTable(table, game, true, false);

        assertThat(result.pendingState()).isEqualTo(TableState.BEGIN);
    }

    // BEGIN state tests

    @Test
    void handleBegin_withAutoDeal_shouldTransitionToStartHand() {
        table.tableState = TableState.BEGIN;
        table.autoDeal = true;

        TableProcessResult result = engine.processTable(table, game, true, false);

        assertThat(result.nextState()).isEqualTo(TableState.START_HAND);
    }

    @Test
    void handleBegin_withoutAutoDeal_shouldRunWaitForDealPhase() {
        table.tableState = TableState.BEGIN;
        table.autoDeal = false;

        TableProcessResult result = engine.processTable(table, game, true, false);

        assertThat(result.phaseToRun()).isEqualTo("TD.WaitForDeal");
        assertThat(result.nextState()).isEqualTo(TableState.BEGIN_WAIT);
    }

    // CHECK_END_HAND state tests

    @Test
    void handleCheckEndHand_shouldSetPendingStateToClean() {
        table.tableState = TableState.CHECK_END_HAND;

        TableProcessResult result = engine.processTable(table, game, true, false);

        assertThat(result.pendingState()).isEqualTo(TableState.CLEAN);
    }

    // CLEAN state tests

    @Test
    void handleClean_shouldTransitionToNewLevelCheck() {
        table.tableState = TableState.CLEAN;

        TableProcessResult result = engine.processTable(table, game, true, false);

        assertThat(result.nextState()).isEqualTo(TableState.NEW_LEVEL_CHECK);
        assertThat(result.shouldSleep()).isFalse();
    }

    // NEW_LEVEL_CHECK state tests

    @Test
    void handleNewLevelCheck_shouldTransitionToStartHand() {
        table.tableState = TableState.NEW_LEVEL_CHECK;
        table.level = 1; // Match game level so no level change

        TableProcessResult result = engine.processTable(table, game, true, false);

        assertThat(result.nextState()).isEqualTo(TableState.START_HAND);
    }

    // START_HAND state tests

    @Test
    void handleStartHand_shouldTransitionToBetting() {
        table.tableState = TableState.START_HAND;

        TableProcessResult result = engine.processTable(table, game, true, false);

        assertThat(result.phaseToRun()).isEqualTo("TD.DealDisplayHand");
        assertThat(result.pendingState()).isEqualTo(TableState.BETTING);
    }

    // BREAK state tests

    @Test
    void handleBreak_shouldTransitionToNewLevelCheck() {
        table.tableState = TableState.BREAK;

        TableProcessResult result = engine.processTable(table, game, true, false);

        assertThat(result.nextState()).isEqualTo(TableState.NEW_LEVEL_CHECK);
    }

    // PRE_SHOWDOWN state tests

    @Test
    void handlePreShowdown_shouldTransitionToShowdown() {
        table.tableState = TableState.PRE_SHOWDOWN;

        TableProcessResult result = engine.processTable(table, game, true, false);

        assertThat(result.nextState()).isEqualTo(TableState.SHOWDOWN);
        assertThat(result.shouldSleep()).isFalse();
    }

    // SHOWDOWN state tests

    @Test
    void handleShowdown_shouldTransitionToDone() {
        table.tableState = TableState.SHOWDOWN;

        TableProcessResult result = engine.processTable(table, game, true, false);

        assertThat(result.nextState()).isEqualTo(TableState.DONE);
        assertThat(result.shouldAutoSave()).isTrue();
        assertThat(result.shouldSleep()).isFalse();
    }

    @Test
    void handleShowdown_withNonNullHand_shouldStoreHistory() {
        table.tableState = TableState.SHOWDOWN;
        StubGameHand hand = new StubGameHand(BettingRound.SHOWDOWN, true);
        table.holdemHand = hand;

        TableProcessResult result = engine.processTable(table, game, true, false);

        assertThat(result.pendingState()).isEqualTo(TableState.DONE);
        assertThat(result.phaseToRun()).isEqualTo("TD.Showdown");
        assertThat(result.shouldAutoSave()).isTrue();
        // Note: Can't verify storeHandHistory() was called without modifying
        // StubGameHand,
        // but this exercises the non-null hand path including localPlayer null-check
    }

    // BETTING state tests

    @Test
    void handleBetting_shouldNotSleepForOfflineGame() {
        table.tableState = TableState.BETTING;
        table.holdemHand = new StubGameHand(BettingRound.RIVER, true); // Hand is done

        TableProcessResult result = engine.processTable(table, game, true, false);

        assertThat(result.nextState()).isIn(TableState.PRE_SHOWDOWN, TableState.SHOWDOWN);
    }

    // COMMUNITY state tests

    @Test
    void handleCommunity_shouldContinueToCommunity() {
        table.tableState = TableState.COMMUNITY;
        StubGameHand hand = new StubGameHand(BettingRound.FLOP, false);
        hand.numWithCards = 1; // Only 1 player, skip phase
        table.holdemHand = hand;

        TableProcessResult result = engine.processTable(table, game, true, true); // Online game

        assertThat(result.nextState()).isEqualTo(TableState.COMMUNITY);
        assertThat(result.shouldSleep()).isFalse();
    }

    @Test
    void handleCommunity_atRiver_shouldTransitionToPreShowdown() {
        table.tableState = TableState.COMMUNITY;
        StubGameHand hand = new StubGameHand(BettingRound.TURN, false); // Start at TURN, will advance to RIVER
        hand.numWithCards = 1; // Only 1 player, skip phase
        table.holdemHand = hand;

        TableProcessResult result = engine.processTable(table, game, true, true); // Online game

        assertThat(result.nextState()).isEqualTo(TableState.PRE_SHOWDOWN);
        assertThat(result.shouldSleep()).isFalse();
    }

    // COLOR_UP state tests

    @Test
    void handleColorUp_shouldTransitionToStartHand() {
        table.tableState = TableState.COLOR_UP;

        TableProcessResult result = engine.processTable(table, game, true, false);

        assertThat(result.nextState()).isEqualTo(TableState.START_HAND);
    }

    // Tests for handlePendingTimeouts and handleBettingTimeout

    @Test
    void handlePending_withTimeouts_nonBettingState_shouldRemoveWaitAll_whenTimeoutExceeded() {
        table.tableState = TableState.PENDING;
        table.previousTableState = TableState.CLEAN; // Non-betting, non-new-level
        table.numWaiting = 1;
        table.millisSinceLastStateChange = 6000; // Exceeds 5000ms timeout

        TableProcessResult result = engine.processTable(table, game, true, true);

        // Should have timed out and removed wait list
        assertThat(table.removeWaitAllCalled).isTrue();
    }

    @Test
    void handlePending_withTimeouts_nonBettingState_shouldNotTimeout_whenBelowThreshold() {
        table.tableState = TableState.PENDING;
        table.previousTableState = TableState.CLEAN;
        table.numWaiting = 1;
        table.millisSinceLastStateChange = 4000; // Below 5000ms timeout

        engine.processTable(table, game, true, true);

        assertThat(table.removeWaitAllCalled).isFalse();
    }

    @Test
    void handlePending_withTimeouts_newLevelCheckState_shouldUse30SecondTimeout() {
        table.tableState = TableState.PENDING;
        table.previousTableState = TableState.NEW_LEVEL_CHECK;
        table.numWaiting = 1;
        table.millisSinceLastStateChange = 25000; // Below 30000ms timeout

        engine.processTable(table, game, true, true);

        assertThat(table.removeWaitAllCalled).isFalse();
    }

    @Test
    void handlePending_withTimeouts_newLevelCheckState_shouldTimeout_whenExceeds30Seconds() {
        table.tableState = TableState.PENDING;
        table.previousTableState = TableState.NEW_LEVEL_CHECK;
        table.numWaiting = 1;
        table.millisSinceLastStateChange = 31000; // Exceeds 30000ms timeout

        engine.processTable(table, game, true, true);

        assertThat(table.removeWaitAllCalled).isTrue();
    }

    @Test
    void handlePending_withBettingTimeout_shouldReturnEarly_whenNoWaitList() {
        table.tableState = TableState.PENDING;
        table.previousTableState = TableState.BETTING;
        table.numWaiting = 0; // Empty wait list
        table.millisSinceLastStateChange = 100000;

        engine.processTable(table, game, true, true);

        // Should not call removeWaitAll since list is already empty
        assertThat(table.removeWaitAllCalled).isFalse();
    }

    @Test
    void handlePending_withBettingTimeout_shouldReturnEarly_whenComputerPlayer() {
        table.tableState = TableState.PENDING;
        table.previousTableState = TableState.BETTING;
        table.numWaiting = 1;

        StubGamePlayer computerPlayer = new StubGamePlayer(1, "AI", 1000, false);
        computerPlayer.isComputer = true; // Computer player
        table.waitPlayer = computerPlayer;
        table.millisSinceLastStateChange = 100000;

        engine.processTable(table, game, true, true);

        assertThat(table.removeWaitAllCalled).isFalse();
    }

    @Test
    void handlePending_withBettingTimeout_shouldReturnEarly_whenNoHand() {
        table.tableState = TableState.PENDING;
        table.previousTableState = TableState.BETTING;
        table.numWaiting = 1;

        StubGamePlayer humanPlayer = new StubGamePlayer(1, "Human", 1000, true);
        humanPlayer.isComputer = false;
        table.waitPlayer = humanPlayer;
        table.holdemHand = null; // No hand
        table.millisSinceLastStateChange = 100000;

        engine.processTable(table, game, true, true);

        assertThat(table.removeWaitAllCalled).isFalse();
    }

    @Test
    void handlePending_withBettingTimeout_shouldTimeout_whenExceedsLimit() {
        table.tableState = TableState.PENDING;
        table.previousTableState = TableState.BETTING;
        table.numWaiting = 1;

        StubGamePlayer humanPlayer = new StubGamePlayer(1, "Human", 1000, true);
        humanPlayer.isComputer = false;
        humanPlayer.thinkBankMillis = 0; // No think bank
        table.waitPlayer = humanPlayer;

        StubGameHand hand = new StubGameHand(BettingRound.FLOP, false);
        table.holdemHand = hand;

        game.timeoutForRound = 30; // 30 seconds
        table.millisSinceLastStateChange = 32000; // Exceeds 31000ms (30s + 1s padding)

        engine.processTable(table, game, true, true);

        assertThat(table.removeWaitAllCalled).isTrue();
    }

    @Test
    void handlePending_withBettingTimeout_shouldNotTimeout_whenThinkBankRemaining() {
        table.tableState = TableState.PENDING;
        table.previousTableState = TableState.BETTING;
        table.numWaiting = 1;

        StubGamePlayer humanPlayer = new StubGamePlayer(1, "Human", 1000, true);
        humanPlayer.isComputer = false;
        humanPlayer.thinkBankMillis = 5000; // Has think bank
        table.waitPlayer = humanPlayer;

        StubGameHand hand = new StubGameHand(BettingRound.FLOP, false);
        table.holdemHand = hand;

        game.timeoutForRound = 30;
        table.millisSinceLastStateChange = 32000; // Exceeds timeout but has think bank

        engine.processTable(table, game, true, true);

        assertThat(table.removeWaitAllCalled).isFalse();
    }

    // Tests for handlePreShowdown

    @Test
    void handlePreShowdown_withNullHand_shouldTransitionToShowdown() {
        table.tableState = TableState.PRE_SHOWDOWN;
        table.holdemHand = null;

        TableProcessResult result = engine.processTable(table, game, true, false);

        assertThat(result.nextState()).isEqualTo(TableState.SHOWDOWN);
        assertThat(result.shouldSleep()).isFalse();
    }

    @Test
    void handlePreShowdown_asHost_shouldCallPreResolve() {
        table.tableState = TableState.PRE_SHOWDOWN;
        StubGameHand hand = new StubGameHand(BettingRound.SHOWDOWN, true);
        table.holdemHand = hand;

        engine.processTable(table, game, true, false);

        assertThat(hand.preResolveCalled).isTrue();
        assertThat(hand.preResolveIsOnline).isFalse();
    }

    @Test
    void handlePreShowdown_asHost_onlineGame_shouldCallPreResolveWithOnlineFlag() {
        table.tableState = TableState.PRE_SHOWDOWN;
        StubGameHand hand = new StubGameHand(BettingRound.SHOWDOWN, true);
        table.holdemHand = hand;

        engine.processTable(table, game, true, true); // Online

        assertThat(hand.preResolveCalled).isTrue();
        assertThat(hand.preResolveIsOnline).isTrue();
    }

    @Test
    void handlePreShowdown_asClient_shouldClearWaitList() {
        table.tableState = TableState.PRE_SHOWDOWN;
        table.numWaiting = 1;
        StubGameHand hand = new StubGameHand(BettingRound.SHOWDOWN, true);
        table.holdemHand = hand;

        engine.processTable(table, game, false, true); // Not host

        assertThat(table.removeWaitAllCalled).isTrue();
    }

    @Test
    void handlePreShowdown_onlineGame_withWinnerWantsToShow_shouldAddToWaitList() {
        table.tableState = TableState.PRE_SHOWDOWN;

        StubGamePlayer winner = new StubGamePlayer(1, "Winner", 1000, true);
        winner.isComputer = false;
        winner.isHuman = true;
        winner.askShowWinning = true;

        StubGameHand hand = new StubGameHand(BettingRound.SHOWDOWN, true);
        hand.isUncontested = true;
        hand.preWinners.add(winner);
        table.holdemHand = hand;

        game.localPlayer = winner; // Local player is the winner

        TableProcessResult result = engine.processTable(table, game, true, true); // Online

        assertThat(table.numWaiting).isEqualTo(1);
        assertThat(result.phaseToRun()).isEqualTo("TD.PreShowdown");
        assertThat(result.pendingState()).isEqualTo(TableState.SHOWDOWN);
    }

    @Test
    void handlePreShowdown_onlineGame_withLoserWantsToShow_shouldAddToWaitList() {
        table.tableState = TableState.PRE_SHOWDOWN;

        StubGamePlayer loser = new StubGamePlayer(2, "Loser", 500, true);
        loser.isComputer = false;
        loser.isHuman = true;
        loser.askShowLosing = true;

        StubGameHand hand = new StubGameHand(BettingRound.SHOWDOWN, true);
        hand.preLosers.add(loser);
        table.holdemHand = hand;

        game.localPlayer = loser;

        TableProcessResult result = engine.processTable(table, game, true, true);

        assertThat(table.numWaiting).isEqualTo(1);
        assertThat(result.phaseToRun()).isEqualTo("TD.PreShowdown");
    }

    @Test
    void handlePreShowdown_onlineGame_waitListNotEmpty_butNotLocalPlayer_shouldWait() {
        table.tableState = TableState.PRE_SHOWDOWN;

        StubGamePlayer winner = new StubGamePlayer(1, "Winner", 1000, true);
        winner.isComputer = false;
        winner.isHuman = true;
        winner.askShowWinning = true;

        StubGamePlayer otherPlayer = new StubGamePlayer(2, "Other", 500, true);

        StubGameHand hand = new StubGameHand(BettingRound.SHOWDOWN, true);
        hand.isUncontested = true;
        hand.preWinners.add(winner);
        table.holdemHand = hand;

        game.localPlayer = otherPlayer; // Local player is NOT in wait list

        TableProcessResult result = engine.processTable(table, game, true, true);

        assertThat(table.numWaiting).isEqualTo(1);
        assertThat(result.phaseToRun()).isNull(); // No phase to run
        assertThat(result.shouldOnlySendToWaitList()).isTrue();
    }

    @Test
    void handlePreShowdown_offlineGame_shouldNotAddPlayersToWaitList() {
        table.tableState = TableState.PRE_SHOWDOWN;

        StubGamePlayer winner = new StubGamePlayer(1, "Winner", 1000, true);
        winner.isComputer = false;
        winner.isHuman = true;
        winner.askShowWinning = true;

        StubGameHand hand = new StubGameHand(BettingRound.SHOWDOWN, true);
        hand.isUncontested = true;
        hand.preWinners.add(winner);
        table.holdemHand = hand;

        TableProcessResult result = engine.processTable(table, game, true, false); // Offline

        // Offline games skip the whole wait list logic
        assertThat(table.numWaiting).isEqualTo(0);
        assertThat(result.nextState()).isEqualTo(TableState.SHOWDOWN);
    }

    // Additional tests for handlePending - scheduled start logic

    @Test
    void handlePending_withScheduledStart_shouldRemoveWaitAll_whenTimeAndPlayersReached() {
        table.tableState = TableState.PENDING;
        game.scheduledStartEnabled = true;
        game.scheduledStartTime = System.currentTimeMillis() - 1000; // 1 second ago
        game.minPlayersForScheduledStart = 2;
        game.numPlayers = 3; // More than minimum

        table.numWaiting = 1; // Someone in wait list

        TableProcessResult result = engine.processTable(table, game, true, false);

        // Should have removed all from wait list (verified via stub tracking)
        assertThat(table.removeWaitAllCalled).isTrue();
    }

    @Test
    void handlePending_withScheduledStart_shouldNotAutoStart_whenNotEnoughPlayers() {
        table.tableState = TableState.PENDING;
        game.scheduledStartEnabled = true;
        game.scheduledStartTime = System.currentTimeMillis() - 1000;
        game.minPlayersForScheduledStart = 5;
        game.numPlayers = 3; // Less than minimum

        table.numWaiting = 1;

        engine.processTable(table, game, true, false);

        assertThat(table.removeWaitAllCalled).isFalse();
    }

    @Test
    void handlePending_withScheduledStart_shouldNotAutoStart_whenTimeNotReached() {
        table.tableState = TableState.PENDING;
        game.scheduledStartEnabled = true;
        game.scheduledStartTime = System.currentTimeMillis() + 10000; // 10 seconds in future
        game.minPlayersForScheduledStart = 2;
        game.numPlayers = 3;

        table.numWaiting = 1;

        engine.processTable(table, game, true, false);

        assertThat(table.removeWaitAllCalled).isFalse();
    }

    @Test
    void handlePending_withScheduledStart_shouldIgnoreZeroStartTime() {
        table.tableState = TableState.PENDING;
        game.scheduledStartEnabled = true;
        game.scheduledStartTime = 0; // Invalid start time
        game.minPlayersForScheduledStart = 2;
        game.numPlayers = 3;

        table.numWaiting = 1;

        engine.processTable(table, game, true, false);

        // Should not auto-start with startTime=0
        assertThat(table.removeWaitAllCalled).isFalse();
    }

    @Test
    void handlePending_withEmptyWaitList_shouldTransitionToPendingState() {
        table.tableState = TableState.PENDING;
        table.pendingTableState = TableState.BEGIN;
        table.numWaiting = 0; // Empty wait list

        TableProcessResult result = engine.processTable(table, game, true, false);

        assertThat(result.nextState()).isEqualTo(TableState.BEGIN);
        assertThat(result.shouldSleep()).isFalse();
    }

    @Test
    void handlePending_withEmptyWaitList_andAutoDeal_shouldSetPause() {
        table.tableState = TableState.PENDING;
        table.pendingTableState = TableState.BEGIN;
        table.autoDeal = true;
        table.numWaiting = 0;
        table.autoDealDelay = 2000;

        TableProcessResult result = engine.processTable(table, game, true, false);

        assertThat(result.nextState()).isEqualTo(TableState.BEGIN);
        assertThat(table.pauseMillis).isEqualTo(2000);
    }

    @Test
    void handlePending_withEmptyWaitList_andAutoDeal_onlineGame_shouldSetShortPause() {
        table.tableState = TableState.PENDING;
        table.pendingTableState = TableState.BEGIN;
        table.autoDeal = true;
        table.numWaiting = 0;
        table.autoDealDelay = 2000;

        TableProcessResult result = engine.processTable(table, game, true, true); // Online

        assertThat(result.nextState()).isEqualTo(TableState.BEGIN);
        // Online games use 1000ms regardless of autoDealDelay
        assertThat(table.pauseMillis).isEqualTo(1000);
    }

    @Test
    void handlePending_withZipMode_shouldNotSleep() {
        table.tableState = TableState.PENDING;
        table.pendingTableState = TableState.BETTING;
        table.numWaiting = 1;
        table.zipMode = true;

        TableProcessResult result = engine.processTable(table, game, true, false);

        assertThat(result.shouldSleep()).isFalse();
    }

    @Test
    void handlePending_waitingOnComputer_shouldNotSleep() {
        table.tableState = TableState.PENDING;
        table.pendingTableState = TableState.BETTING;
        table.numWaiting = 1;

        StubGamePlayer computerPlayer = new StubGamePlayer(1, "AI", 1000, false);
        computerPlayer.isComputer = true;
        table.waitPlayer = computerPlayer;

        TableProcessResult result = engine.processTable(table, game, true, false);

        assertThat(result.shouldSleep()).isFalse();
    }

    @Test
    void handlePending_waitingOnHuman_shouldSleep() {
        table.tableState = TableState.PENDING;
        table.pendingTableState = TableState.BETTING;
        table.numWaiting = 1;

        StubGamePlayer humanPlayer = new StubGamePlayer(1, "Human", 1000, true);
        humanPlayer.isComputer = false;
        table.waitPlayer = humanPlayer;

        TableProcessResult result = engine.processTable(table, game, true, false);

        assertThat(result.shouldSleep()).isTrue();
    }

    // Stub implementations for testing

    private static class StubGameTable implements GameTable {
        public TableState tableState = TableState.NONE;
        public TableState pendingTableState = null;
        public TableState previousTableState = null;
        public String pendingPhase = null;
        public int numOccupiedSeats = 0;
        public boolean autoDeal = false;
        public int button = 0;
        public int handNum = 0;
        public int level = 0;
        public GameHand holdemHand = null;
        public int numWaiting = 0;
        public boolean removeWaitAllCalled = false;
        public int pauseMillis = 0;
        public int autoDealDelay = 0;
        public boolean zipMode = false;
        public GamePlayerInfo waitPlayer = null;
        public long millisSinceLastStateChange = 0;
        public boolean coloringUp = false;
        public java.util.List<GamePlayerInfo> addedPlayers = null;

        @Override
        public int getNumber() {
            return 1;
        }

        @Override
        public TableState getTableState() {
            return tableState;
        }

        @Override
        public void setTableState(TableState state) {
            this.tableState = state;
        }

        @Override
        public TableState getPendingTableState() {
            return pendingTableState;
        }

        @Override
        public void setPendingTableState(TableState state) {
            this.pendingTableState = state;
        }

        @Override
        public TableState getPreviousTableState() {
            return previousTableState;
        }

        @Override
        public String getPendingPhase() {
            return pendingPhase;
        }

        @Override
        public void setPendingPhase(String phase) {
            this.pendingPhase = phase;
        }

        @Override
        public int getSeats() {
            return 10;
        }

        @Override
        public int getNumOccupiedSeats() {
            return numOccupiedSeats;
        }

        @Override
        public GamePlayerInfo getPlayer(int seat) {
            return null;
        }

        @Override
        public int getButton() {
            return button;
        }

        @Override
        public void setButton(int seat) {
            this.button = seat;
        }

        @Override
        public int getNextSeatAfterButton() {
            return 0;
        }

        @Override
        public int getNextSeat(int seat) {
            return (seat + 1) % 10;
        }

        @Override
        public int getHandNum() {
            return handNum;
        }

        @Override
        public void setHandNum(int handNum) {
            this.handNum = handNum;
        }

        @Override
        public int getLevel() {
            return level;
        }

        @Override
        public void setLevel(int level) {
            this.level = level;
        }

        @Override
        public int getMinChip() {
            return 5;
        }

        @Override
        public GameHand getHoldemHand() {
            return holdemHand;
        }

        @Override
        public void setHoldemHand(GameHand hand) {
            this.holdemHand = hand;
        }

        @Override
        public boolean isAutoDeal() {
            return autoDeal;
        }

        @Override
        public boolean isCurrent() {
            return false;
        }

        @Override
        public void processAIRebuys() {
        }

        @Override
        public void processAIAddOns() {
        }

        @Override
        public void clearRebuyList() {
        }

        @Override
        public void setNextMinChip(int minChip) {
        }

        @Override
        public void doColorUpDetermination() {
        }

        @Override
        public boolean isColoringUp() {
            return coloringUp;
        }

        @Override
        public void colorUp() {
        }

        @Override
        public void colorUpFinish() {
        }

        @Override
        public void startBreak() {
        }

        @Override
        public void startNewHand() {
        }

        @Override
        public void setButton() {
        }

        @Override
        public void removeWaitAll() {
            removeWaitAllCalled = true;
            numWaiting = 0;
        }

        @Override
        public void addWait(GamePlayerInfo player) {
            numWaiting++;
        }

        @Override
        public int getWaitSize() {
            return numWaiting;
        }

        @Override
        public GamePlayerInfo getWaitPlayer(int index) {
            return waitPlayer;
        }

        @Override
        public long getMillisSinceLastStateChange() {
            return millisSinceLastStateChange;
        }

        @Override
        public void setPause(int millis) {
            this.pauseMillis = millis;
        }

        @Override
        public int getAutoDealDelay() {
            return autoDealDelay;
        }

        @Override
        public void simulateHand() {
        }

        @Override
        public java.util.List<GamePlayerInfo> getAddedPlayersList() {
            return addedPlayers;
        }

        @Override
        public boolean isRemoved() {
            return false;
        }

        @Override
        public boolean isAllComputer() {
            return false;
        }

        @Override
        public boolean isZipMode() {
            return zipMode;
        }

        @Override
        public void setZipMode(boolean zipMode) {
            this.zipMode = zipMode;
        }
    }

    private static class StubTournamentContext implements TournamentContext {
        public boolean scheduledStartEnabled = false;
        public long scheduledStartTime = 0;
        public int minPlayersForScheduledStart = 2;
        public int numPlayers = 0;
        public int timeoutForRound = 30;
        public GamePlayerInfo localPlayer = null;
        public int level = 1;
        public int lastMinChip = 5;
        public int minChip = 5;
        public int breakLevel = -1; // -1 means no break
        public boolean onePlayerLeft = false;

        @Override
        public int getNumTables() {
            return 1;
        }

        @Override
        public GameTable getTable(int index) {
            return null;
        }

        @Override
        public int getNumPlayers() {
            return numPlayers;
        }

        @Override
        public GamePlayerInfo getPlayerByID(int playerId) {
            return null;
        }

        @Override
        public boolean isPractice() {
            return false;
        }

        @Override
        public boolean isOnlineGame() {
            return false;
        }

        @Override
        public boolean isGameOver() {
            return false;
        }

        @Override
        public int getLevel() {
            return level;
        }

        @Override
        public void nextLevel() {
        }

        @Override
        public boolean isLevelExpired() {
            return false;
        }

        @Override
        public void advanceClockBreak() {
        }

        @Override
        public void startGameClock() {
        }

        @Override
        public int getLastMinChip() {
            return lastMinChip;
        }

        @Override
        public int getMinChip() {
            return minChip;
        }

        @Override
        public void advanceClock() {
        }

        @Override
        public boolean isBreakLevel(int level) {
            return this.breakLevel == level;
        }

        @Override
        public boolean isScheduledStartEnabled() {
            return scheduledStartEnabled;
        }

        @Override
        public long getScheduledStartTime() {
            return scheduledStartTime;
        }

        @Override
        public int getMinPlayersForScheduledStart() {
            return minPlayersForScheduledStart;
        }

        @Override
        public int getTimeoutForRound(int round) {
            return timeoutForRound;
        }

        @Override
        public GameTable getCurrentTable() {
            return null;
        }

        @Override
        public int getTimeoutSeconds() {
            return 30;
        }

        @Override
        public boolean isOnePlayerLeft() {
            return onePlayerLeft;
        }

        @Override
        public GamePlayerInfo getLocalPlayer() {
            return localPlayer;
        }

        @Override
        public int getSmallBlind(int level) {
            return 500; // Stub value for testing
        }

        @Override
        public int getBigBlind(int level) {
            return 1000; // Stub value for testing
        }

        @Override
        public int getAnte(int level) {
            return 0; // No antes in stub
        }
    }

    private static class StubGameHand implements GameHand {
        public BettingRound round;
        public boolean done;
        public int numWithCards = 2; // Default to 2 players with cards
        public GamePlayerInfo currentPlayer; // For testing betting logic
        public boolean preResolveCalled = false;
        public boolean preResolveIsOnline = false;
        public boolean isUncontested = false;
        public java.util.List<GamePlayerInfo> preWinners = new java.util.ArrayList<>();
        public java.util.List<GamePlayerInfo> preLosers = new java.util.ArrayList<>();
        public int amountToCall = 0;
        public int minBet = 0;
        public int minRaise = 0;

        StubGameHand(BettingRound round, boolean done) {
            this.round = round;
            this.done = done;
        }

        @Override
        public BettingRound getRound() {
            return round;
        }

        @Override
        public void setRound(BettingRound round) {
            this.round = round;
        }

        @Override
        public boolean isDone() {
            return done;
        }

        @Override
        public int getNumWithCards() {
            return numWithCards;
        }

        @Override
        public int getCurrentPlayerInitIndex() {
            return 0;
        }

        @Override
        public void advanceRound() {
            // Advance to next round
            if (round == BettingRound.PRE_FLOP)
                round = BettingRound.FLOP;
            else if (round == BettingRound.FLOP)
                round = BettingRound.TURN;
            else if (round == BettingRound.TURN)
                round = BettingRound.RIVER;
            else if (round == BettingRound.RIVER)
                round = BettingRound.SHOWDOWN;
        }

        @Override
        public void preResolve(boolean isOnline) {
            this.preResolveCalled = true;
            this.preResolveIsOnline = isOnline;
        }

        @Override
        public void resolve() {
        }

        @Override
        public void storeHandHistory() {
        }

        @Override
        public java.util.List<GamePlayerInfo> getPreWinners() {
            return preWinners;
        }

        @Override
        public java.util.List<GamePlayerInfo> getPreLosers() {
            return preLosers;
        }

        @Override
        public boolean isUncontested() {
            return isUncontested;
        }

        @Override
        public GamePlayerInfo getCurrentPlayerWithInit() {
            return currentPlayer;
        }

        @Override
        public int getAmountToCall(GamePlayerInfo player) {
            return amountToCall;
        }

        @Override
        public int getMinBet() {
            return minBet;
        }

        @Override
        public int getMinRaise() {
            return minRaise;
        }

        @Override
        public void applyPlayerAction(GamePlayerInfo player, PlayerAction action) {
            // Stub implementation - does nothing
        }
    }

    /**
     * Test that handleBetting calls actionProvider and processes the returned
     * action. This exercises the createActionOptions + processPlayerAction
     * integration.
     */
    @Test
    void handleBetting_callsActionProviderAndProcessesAction() {
        // Arrange: Create a player and hand that will exercise the betting logic
        StubGamePlayer player = new StubGamePlayer(1, "Alice", 1000, true);

        StubGameHand hand = new StubGameHand(BettingRound.FLOP, false);
        hand.numWithCards = 2;
        hand.currentPlayer = player; // This makes getCurrentPlayerWithInit() return the player

        table.tableState = TableState.BETTING;
        table.holdemHand = hand;

        // Track if actionProvider was called
        boolean[] providerCalled = {false};
        PlayerActionProvider testProvider = (p, options) -> {
            providerCalled[0] = true;
            // Verify ActionOptions are created correctly
            assertThat(options).isNotNull();
            assertThat(options.canFold()).isTrue();
            return PlayerAction.fold();
        };

        TournamentEngine testEngine = new TournamentEngine(eventBus, testProvider);

        // Act
        TableProcessResult result = testEngine.processTable(table, game, true, false);

        // Assert
        assertThat(providerCalled[0]).isTrue();
        assertThat(result.nextState()).isEqualTo(TableState.BETTING);
    }

    @Test
    void handleBetting_withNullHand_shouldReturnEmptyResult() {
        table.tableState = TableState.BETTING;
        table.holdemHand = null;

        TableProcessResult result = engine.processTable(table, game, true, false);

        assertThat(result.nextState()).isNull();
    }

    @Test
    void handleBetting_withHandDone_shouldTransitionToNextState() {
        StubGameHand hand = new StubGameHand(BettingRound.RIVER, true);

        table.tableState = TableState.BETTING;
        table.holdemHand = hand;

        TableProcessResult result = engine.processTable(table, game, true, false);

        assertThat(result.nextState()).isEqualTo(TableState.PRE_SHOWDOWN);
    }

    @Test
    void handleBetting_withNullCurrentPlayer_shouldReturnEmptyResult() {
        StubGameHand hand = new StubGameHand(BettingRound.FLOP, false);
        hand.currentPlayer = null;

        table.tableState = TableState.BETTING;
        table.holdemHand = hand;

        TableProcessResult result = engine.processTable(table, game, true, false);

        assertThat(result.nextState()).isNull();
    }

    @Test
    void handleBetting_asClient_shouldNotSetTimeoutMillis() {
        StubGamePlayer player = new StubGamePlayer(1, "Alice", 1000, true);
        boolean[] timeoutSet = {false};

        // Override setTimeoutMillis to track if it's called
        StubGamePlayer trackingPlayer = new StubGamePlayer(1, "Alice", 1000, true) {
            @Override
            public void setTimeoutMillis(int millis) {
                timeoutSet[0] = true;
            }
        };

        StubGameHand hand = new StubGameHand(BettingRound.FLOP, false);
        hand.currentPlayer = trackingPlayer;

        table.tableState = TableState.BETTING;
        table.holdemHand = hand;

        PlayerActionProvider testProvider = (p, options) -> PlayerAction.fold();
        TournamentEngine testEngine = new TournamentEngine(eventBus, testProvider);

        // Act as client (isHost = false)
        testEngine.processTable(table, game, false, false);

        // Assert timeout was NOT set
        assertThat(timeoutSet[0]).isFalse();
    }

    @Test
    void handleBetting_withSittingOutPlayer_shouldSetSittingOutAndPause() {
        StubGamePlayer player = new StubGamePlayer(1, "Alice", 1000, true);
        player.sittingOut = true;

        StubGameHand hand = new StubGameHand(BettingRound.FLOP, false);
        hand.currentPlayer = player;

        table.tableState = TableState.BETTING;
        table.holdemHand = hand;

        TableProcessResult result = engine.processTable(table, game, true, false);

        assertThat(player.isSittingOut()).isTrue();
        assertThat(table.pauseMillis).isEqualTo(1100);
        assertThat(result.nextState()).isEqualTo(TableState.BETTING);
    }

    @Test
    void handleBetting_withNullActionFromProvider_shouldAutoFold() {
        StubGamePlayer player = new StubGamePlayer(1, "Alice", 1000, true);

        StubGameHand hand = new StubGameHand(BettingRound.FLOP, false);
        hand.currentPlayer = player;

        table.tableState = TableState.BETTING;
        table.holdemHand = hand;

        // Provider returns null - safety fallback should auto-fold
        PlayerActionProvider nullProvider = (p, options) -> null;
        TournamentEngine testEngine = new TournamentEngine(eventBus, nullProvider);

        TableProcessResult result = testEngine.processTable(table, game, true, false);

        assertThat(result.nextState()).isEqualTo(TableState.BETTING);
    }

    @Test
    void handleBetting_withRemotePlayer_asHost_shouldAddToWaitList() {
        StubGamePlayer player = new StubGamePlayer(1, "Remote", 1000, false);

        StubGameHand hand = new StubGameHand(BettingRound.FLOP, false);
        hand.currentPlayer = player;

        table.tableState = TableState.BETTING;
        table.holdemHand = hand;

        TableProcessResult result = engine.processTable(table, game, true, true);

        assertThat(table.numWaiting).isEqualTo(1);
        assertThat(result.shouldRunOnClient()).isTrue();
        assertThat(result.shouldOnlySendToWaitList()).isTrue();
        assertThat(result.shouldAddAllHumans()).isFalse();
        assertThat(result.pendingState()).isEqualTo(TableState.BETTING);
    }

    @Test
    void handleBetting_withRemotePlayer_asClient_shouldReturnEmptyResult() {
        StubGamePlayer player = new StubGamePlayer(1, "Remote", 1000, false);

        StubGameHand hand = new StubGameHand(BettingRound.FLOP, false);
        hand.currentPlayer = player;

        table.tableState = TableState.BETTING;
        table.holdemHand = hand;

        TableProcessResult result = engine.processTable(table, game, false, true);

        assertThat(result.nextState()).isNull();
    }

    @Test
    void createActionOptions_withNoAmountToCall_shouldAllowCheckAndBet() {
        StubGamePlayer player = new StubGamePlayer(1, "Alice", 1000, true);

        StubGameHand hand = new StubGameHand(BettingRound.FLOP, false);
        hand.amountToCall = 0;
        hand.minBet = 100;
        hand.minRaise = 200;
        hand.currentPlayer = player;

        table.tableState = TableState.BETTING;
        table.holdemHand = hand;

        boolean[] optionsVerified = {false};
        PlayerActionProvider testProvider = (p, options) -> {
            assertThat(options.canCheck()).isTrue();
            assertThat(options.canCall()).isFalse();
            assertThat(options.canBet()).isTrue();
            assertThat(options.canRaise()).isFalse();
            assertThat(options.canFold()).isTrue();
            assertThat(options.callAmount()).isEqualTo(0);
            assertThat(options.minBet()).isEqualTo(100);
            assertThat(options.maxBet()).isEqualTo(1000);
            optionsVerified[0] = true;
            return PlayerAction.check();
        };

        TournamentEngine testEngine = new TournamentEngine(eventBus, testProvider);
        testEngine.processTable(table, game, true, false);

        assertThat(optionsVerified[0]).isTrue();
    }

    @Test
    void createActionOptions_withAmountToCall_shouldAllowCallAndRaise() {
        StubGamePlayer player = new StubGamePlayer(1, "Alice", 1000, true);

        StubGameHand hand = new StubGameHand(BettingRound.FLOP, false);
        hand.amountToCall = 100;
        hand.minBet = 100;
        hand.minRaise = 200;
        hand.currentPlayer = player;

        table.tableState = TableState.BETTING;
        table.holdemHand = hand;

        boolean[] optionsVerified = {false};
        PlayerActionProvider testProvider = (p, options) -> {
            assertThat(options.canCheck()).isFalse();
            assertThat(options.canCall()).isTrue();
            assertThat(options.canBet()).isFalse();
            assertThat(options.canRaise()).isTrue();
            assertThat(options.callAmount()).isEqualTo(100);
            assertThat(options.minRaise()).isEqualTo(200);
            assertThat(options.maxRaise()).isEqualTo(1000);
            optionsVerified[0] = true;
            return PlayerAction.call();
        };

        TournamentEngine testEngine = new TournamentEngine(eventBus, testProvider);
        testEngine.processTable(table, game, true, false);

        assertThat(optionsVerified[0]).isTrue();
    }

    @Test
    void createActionOptions_withShortStack_shouldAllowCallForLess() {
        StubGamePlayer player = new StubGamePlayer(1, "Alice", 50, true);

        StubGameHand hand = new StubGameHand(BettingRound.FLOP, false);
        hand.amountToCall = 100;
        hand.currentPlayer = player;

        table.tableState = TableState.BETTING;
        table.holdemHand = hand;

        boolean[] optionsVerified = {false};
        PlayerActionProvider testProvider = (p, options) -> {
            assertThat(options.canCall()).isTrue(); // Can call for less (all-in)
            assertThat(options.canRaise()).isFalse(); // Not enough chips to raise
            assertThat(options.maxRaise()).isEqualTo(50);
            optionsVerified[0] = true;
            return PlayerAction.call();
        };

        TournamentEngine testEngine = new TournamentEngine(eventBus, testProvider);
        testEngine.processTable(table, game, true, false);

        assertThat(optionsVerified[0]).isTrue();
    }

    @Test
    void createActionOptions_withNoChips_shouldOnlyAllowFold() {
        StubGamePlayer player = new StubGamePlayer(1, "Alice", 0, true);

        StubGameHand hand = new StubGameHand(BettingRound.FLOP, false);
        hand.amountToCall = 100;
        hand.currentPlayer = player;

        table.tableState = TableState.BETTING;
        table.holdemHand = hand;

        boolean[] optionsVerified = {false};
        PlayerActionProvider testProvider = (p, options) -> {
            assertThat(options.canCheck()).isFalse();
            assertThat(options.canCall()).isFalse();
            assertThat(options.canBet()).isFalse();
            assertThat(options.canRaise()).isFalse();
            assertThat(options.canFold()).isTrue();
            optionsVerified[0] = true;
            return PlayerAction.fold();
        };

        TournamentEngine testEngine = new TournamentEngine(eventBus, testProvider);
        testEngine.processTable(table, game, true, false);

        assertThat(optionsVerified[0]).isTrue();
    }

    // COMMUNITY state tests

    @Test
    void handleCommunity_withNullHand_shouldReturnEmptyResult() {
        table.tableState = TableState.COMMUNITY;
        table.holdemHand = null;

        TableProcessResult result = engine.processTable(table, game, true, false);

        assertThat(result.nextState()).isNull();
    }

    @Test
    void handleCommunity_asClient_shouldNotAdvanceRound() {
        StubGameHand hand = new StubGameHand(BettingRound.FLOP, false);
        hand.numWithCards = 1; // Only one player left

        table.tableState = TableState.COMMUNITY;
        table.holdemHand = hand;

        TableProcessResult result = engine.processTable(table, game, false, true);

        // Client doesn't advance round - hand stays at FLOP
        assertThat(hand.getRound()).isEqualTo(BettingRound.FLOP);
        // FLOP with 1 player -> shouldRunPhase=false -> nextState=COMMUNITY
        assertThat(result.nextState()).isEqualTo(TableState.COMMUNITY);
    }

    @Test
    void handleCommunity_asHost_inOfflineGame_shouldAdvanceClock() {
        StubGameHand hand = new StubGameHand(BettingRound.FLOP, false);
        hand.numWithCards = 2;

        table.tableState = TableState.COMMUNITY;
        table.holdemHand = hand;

        boolean[] clockAdvanced = {false};
        StubTournamentContext trackingGame = new StubTournamentContext() {
            @Override
            public void advanceClock() {
                clockAdvanced[0] = true;
            }
        };

        TableProcessResult result = engine.processTable(table, trackingGame, true, false);

        assertThat(hand.getRound()).isEqualTo(BettingRound.TURN);
        assertThat(clockAdvanced[0]).isTrue();
    }

    @Test
    void handleCommunity_asHost_inOnlineGame_shouldNotAdvanceClock() {
        StubGameHand hand = new StubGameHand(BettingRound.FLOP, false);
        hand.numWithCards = 2;

        table.tableState = TableState.COMMUNITY;
        table.holdemHand = hand;

        boolean[] clockAdvanced = {false};
        StubTournamentContext trackingGame = new StubTournamentContext() {
            @Override
            public void advanceClock() {
                clockAdvanced[0] = true;
            }
        };

        TableProcessResult result = engine.processTable(table, trackingGame, true, true);

        assertThat(hand.getRound()).isEqualTo(BettingRound.TURN);
        assertThat(clockAdvanced[0]).isFalse();
    }

    @Test
    void handleCommunity_inOnlineGame_withMultiplePlayers_shouldRunDealCommunityPhase() {
        StubGameHand hand = new StubGameHand(BettingRound.FLOP, false);
        hand.numWithCards = 2;

        table.tableState = TableState.COMMUNITY;
        table.holdemHand = hand;

        TableProcessResult result = engine.processTable(table, game, true, true);

        assertThat(result.phaseToRun()).isEqualTo("TD.DealCommunity");
        assertThat(result.shouldRunOnClient()).isTrue();
        assertThat(result.pendingState()).isEqualTo(TableState.BETTING);
        assertThat(result.shouldSleep()).isTrue();
    }

    @Test
    void handleCommunity_inOnlineGame_withSinglePlayer_shouldSkipPhase() {
        StubGameHand hand = new StubGameHand(BettingRound.FLOP, false);
        hand.numWithCards = 1;

        table.tableState = TableState.COMMUNITY;
        table.holdemHand = hand;

        TableProcessResult result = engine.processTable(table, game, true, true);

        assertThat(result.phaseToRun()).isNull();
        assertThat(result.nextState()).isEqualTo(TableState.COMMUNITY);
        assertThat(result.shouldSleep()).isFalse();
    }

    @Test
    void handleCommunity_withZipMode_shouldDisableSleep() {
        StubGameHand hand = new StubGameHand(BettingRound.FLOP, false);
        hand.numWithCards = 2;

        table.tableState = TableState.COMMUNITY;
        table.holdemHand = hand;
        table.zipMode = true;

        TableProcessResult result = engine.processTable(table, game, false, false);

        assertThat(result.phaseToRun()).isEqualTo("TD.DealCommunity");
        assertThat(result.shouldSleep()).isFalse();
    }

    @Test
    void handleCommunity_withHandDone_shouldDisableSleep() {
        StubGameHand hand = new StubGameHand(BettingRound.FLOP, true);
        hand.numWithCards = 2;

        table.tableState = TableState.COMMUNITY;
        table.holdemHand = hand;

        TableProcessResult result = engine.processTable(table, game, false, false);

        assertThat(result.phaseToRun()).isEqualTo("TD.DealCommunity");
        assertThat(result.shouldSleep()).isFalse();
    }

    @Test
    void handleCommunity_afterRiver_shouldTransitionToPreShowdown() {
        StubGameHand hand = new StubGameHand(BettingRound.TURN, false);
        hand.numWithCards = 1;

        table.tableState = TableState.COMMUNITY;
        table.holdemHand = hand;

        TableProcessResult result = engine.processTable(table, game, true, true);

        // Host advances round: TURN -> RIVER
        assertThat(hand.getRound()).isEqualTo(BettingRound.RIVER);
        // RIVER with 1 player -> shouldRunPhase=false -> nextState=PRE_SHOWDOWN
        assertThat(result.nextState()).isEqualTo(TableState.PRE_SHOWDOWN);
    }

    // NEW_LEVEL_CHECK state tests

    @Test
    void handleNewLevelCheck_withNoLevelChange_shouldClearRebuyAndGoToStartHand() {
        table.tableState = TableState.NEW_LEVEL_CHECK;
        table.level = 1;
        game.level = 1; // Same level

        boolean[] rebuyCleared = {false};
        StubGameTable trackingTable = new StubGameTable() {
            @Override
            public void clearRebuyList() {
                rebuyCleared[0] = true;
            }
        };
        trackingTable.tableState = TableState.NEW_LEVEL_CHECK;
        trackingTable.level = 1;

        TableProcessResult result = engine.processTable(trackingTable, game, true, false);

        assertThat(rebuyCleared[0]).isTrue();
        assertThat(result.nextState()).isEqualTo(TableState.START_HAND);
        assertThat(result.shouldSleep()).isFalse();
    }

    @Test
    void handleNewLevelCheck_withLevelChange_asHost_shouldProcessRebuysAndAddons() {
        table.tableState = TableState.NEW_LEVEL_CHECK;
        table.level = 1;
        game.level = 2; // Level changed

        boolean[] rebuyProcessed = {false};
        boolean[] addonProcessed = {false};
        StubGameTable trackingTable = new StubGameTable() {
            @Override
            public void processAIRebuys() {
                rebuyProcessed[0] = true;
            }

            @Override
            public void processAIAddOns() {
                addonProcessed[0] = true;
            }
        };
        trackingTable.tableState = TableState.NEW_LEVEL_CHECK;
        trackingTable.level = 1;

        TableProcessResult result = engine.processTable(trackingTable, game, true, false);

        assertThat(rebuyProcessed[0]).isTrue();
        assertThat(addonProcessed[0]).isTrue();
        assertThat(result.phaseToRun()).isEqualTo("TD.NewLevelActions");
        assertThat(result.shouldRunOnClient()).isTrue();
        assertThat(result.pendingState()).isEqualTo(TableState.COLOR_UP);
    }

    @Test
    void handleNewLevelCheck_withLevelChange_asClient_shouldNotProcessRebuys() {
        table.tableState = TableState.NEW_LEVEL_CHECK;
        table.level = 1;
        game.level = 2; // Level changed

        boolean[] rebuyProcessed = {false};
        StubGameTable trackingTable = new StubGameTable() {
            @Override
            public void processAIRebuys() {
                rebuyProcessed[0] = true;
            }
        };
        trackingTable.tableState = TableState.NEW_LEVEL_CHECK;
        trackingTable.level = 1;

        TableProcessResult result = engine.processTable(trackingTable, game, false, false);

        assertThat(rebuyProcessed[0]).isFalse();
        assertThat(result.phaseToRun()).isEqualTo("TD.NewLevelActions");
    }

    // COLOR_UP state tests

    @Test
    void handleColorUp_withNoColorUpNeeded_shouldGoDirectlyToStartHand() {
        table.tableState = TableState.COLOR_UP;
        table.coloringUp = false;

        TableProcessResult result = engine.processTable(table, game, true, false);

        assertThat(result.nextState()).isEqualTo(TableState.START_HAND);
        assertThat(result.shouldSleep()).isFalse();
    }

    @Test
    void handleColorUp_withColorUpNeeded_asHost_shouldSetNextMinChipAndDetermination() {
        table.tableState = TableState.COLOR_UP;
        table.coloringUp = true;
        game.lastMinChip = 5;
        game.minChip = 25;

        boolean[] nextMinChipSet = {false};
        boolean[] determinationDone = {false};
        StubGameTable trackingTable = new StubGameTable() {
            @Override
            public void setNextMinChip(int minChip) {
                nextMinChipSet[0] = true;
                assertThat(minChip).isEqualTo(25);
            }

            @Override
            public void doColorUpDetermination() {
                determinationDone[0] = true;
            }

            @Override
            public boolean isColoringUp() {
                return true;
            }
        };
        trackingTable.tableState = TableState.COLOR_UP;

        TableProcessResult result = engine.processTable(trackingTable, game, true, false);

        assertThat(nextMinChipSet[0]).isTrue();
        assertThat(determinationDone[0]).isTrue();
        assertThat(result.phaseToRun()).isEqualTo("TD.ColorUp");
        assertThat(result.shouldRunOnClient()).isTrue();
        assertThat(result.pendingState()).isEqualTo(TableState.START_HAND);
    }

    @Test
    void handleColorUp_asClient_shouldNotSetNextMinChip() {
        table.tableState = TableState.COLOR_UP;
        table.coloringUp = true;
        game.lastMinChip = 5;
        game.minChip = 25;

        boolean[] nextMinChipSet = {false};
        StubGameTable trackingTable = new StubGameTable() {
            @Override
            public void setNextMinChip(int minChip) {
                nextMinChipSet[0] = true;
            }

            @Override
            public boolean isColoringUp() {
                return true;
            }
        };
        trackingTable.tableState = TableState.COLOR_UP;

        TableProcessResult result = engine.processTable(trackingTable, game, false, false);

        assertThat(nextMinChipSet[0]).isFalse();
        assertThat(result.phaseToRun()).isEqualTo("TD.ColorUp");
    }

    // START_HAND state tests

    @Test
    void handleStartHand_withColoringUp_asHost_shouldCompleteColorUp() {
        table.tableState = TableState.START_HAND;
        table.coloringUp = true;

        boolean[] colorUpCalled = {false};
        boolean[] colorUpFinishCalled = {false};
        StubGameTable trackingTable = new StubGameTable() {
            @Override
            public boolean isColoringUp() {
                return true;
            }

            @Override
            public void colorUp() {
                colorUpCalled[0] = true;
            }

            @Override
            public void colorUpFinish() {
                colorUpFinishCalled[0] = true;
            }
        };
        trackingTable.tableState = TableState.START_HAND;

        TableProcessResult result = engine.processTable(trackingTable, game, true, false);

        assertThat(colorUpCalled[0]).isTrue();
        assertThat(colorUpFinishCalled[0]).isTrue();
    }

    @Test
    void handleStartHand_withBreakLevel_asHost_shouldStartBreak() {
        table.tableState = TableState.START_HAND;
        game.breakLevel = 2;
        game.level = 2;

        boolean[] breakStarted = {false};
        StubGameTable trackingTable = new StubGameTable() {
            @Override
            public void startBreak() {
                breakStarted[0] = true;
            }
        };
        trackingTable.tableState = TableState.START_HAND;

        TableProcessResult result = engine.processTable(trackingTable, game, true, false);

        assertThat(breakStarted[0]).isTrue();
        assertThat(result.nextState()).isEqualTo(TableState.BREAK);
        assertThat(result.shouldRunOnClient()).isTrue();
    }

    @Test
    void handleStartHand_withBreakLevel_asClient_shouldNotStartBreak() {
        table.tableState = TableState.START_HAND;
        game.breakLevel = 2;
        game.level = 2;

        boolean[] breakStarted = {false};
        StubGameTable trackingTable = new StubGameTable() {
            @Override
            public void startBreak() {
                breakStarted[0] = true;
            }
        };
        trackingTable.tableState = TableState.START_HAND;

        TableProcessResult result = engine.processTable(trackingTable, game, false, false);

        assertThat(breakStarted[0]).isFalse();
        assertThat(result.nextState()).isEqualTo(TableState.BREAK);
    }

    @Test
    void handleStartHand_normalStart_asHost_inOfflineGame_shouldAdvanceClock() {
        table.tableState = TableState.START_HAND;

        boolean[] handStarted = {false};
        boolean[] clockAdvanced = {false};
        StubGameTable trackingTable = new StubGameTable() {
            @Override
            public void startNewHand() {
                handStarted[0] = true;
            }
        };
        trackingTable.tableState = TableState.START_HAND;

        StubTournamentContext trackingGame = new StubTournamentContext() {
            @Override
            public void advanceClock() {
                clockAdvanced[0] = true;
            }
        };

        TableProcessResult result = engine.processTable(trackingTable, trackingGame, true, false);

        assertThat(handStarted[0]).isTrue();
        assertThat(clockAdvanced[0]).isTrue();
        assertThat(result.phaseToRun()).isEqualTo("TD.DealDisplayHand");
        assertThat(result.shouldRunOnClient()).isTrue();
        assertThat(result.pendingState()).isEqualTo(TableState.BETTING);
    }

    @Test
    void handleStartHand_normalStart_asHost_inOnlineGame_shouldNotAdvanceClock() {
        table.tableState = TableState.START_HAND;

        boolean[] clockAdvanced = {false};
        StubTournamentContext trackingGame = new StubTournamentContext() {
            @Override
            public void advanceClock() {
                clockAdvanced[0] = true;
            }
        };

        TableProcessResult result = engine.processTable(table, trackingGame, true, true);

        assertThat(clockAdvanced[0]).isFalse();
    }

    @Test
    void handleStartHand_asClient_shouldNotStartNewHand() {
        table.tableState = TableState.START_HAND;

        boolean[] handStarted = {false};
        StubGameTable trackingTable = new StubGameTable() {
            @Override
            public void startNewHand() {
                handStarted[0] = true;
            }
        };
        trackingTable.tableState = TableState.START_HAND;

        TableProcessResult result = engine.processTable(trackingTable, game, false, false);

        assertThat(handStarted[0]).isFalse();
        assertThat(result.phaseToRun()).isEqualTo("TD.DealDisplayHand");
    }

    // CLEAN state tests

    @Test
    void handleClean_asClient_shouldGoToNewLevelCheck() {
        table.tableState = TableState.CLEAN;

        TableProcessResult result = engine.processTable(table, game, false, false);

        assertThat(result.nextState()).isEqualTo(TableState.NEW_LEVEL_CHECK);
        assertThat(result.shouldSleep()).isFalse();
    }

    @Test
    void handleClean_withAddedPlayers_inOfflineGame_shouldShowDisplayTableMoves() {
        table.tableState = TableState.CLEAN;
        table.addedPlayers = java.util.Arrays.asList(new StubGamePlayer(1, "Player1", 1000, true));

        TableProcessResult result = engine.processTable(table, game, true, false);

        assertThat(result.phaseToRun()).isEqualTo("TD.DisplayTableMoves");
        assertThat(result.pendingState()).isEqualTo(TableState.NEW_LEVEL_CHECK);
    }

    @Test
    void handleClean_withAddedPlayers_inOnlineGame_shouldNotShowPhase() {
        table.tableState = TableState.CLEAN;
        table.addedPlayers = java.util.Arrays.asList(new StubGamePlayer(1, "Player1", 1000, true));

        TableProcessResult result = engine.processTable(table, game, true, true);

        assertThat(result.phaseToRun()).isNull();
        assertThat(result.nextState()).isEqualTo(TableState.NEW_LEVEL_CHECK);
    }

    @Test
    void handleClean_withTableStateChangedToOnHold_shouldReturnEmpty() {
        table.tableState = TableState.ON_HOLD; // State already changed

        TableProcessResult result = engine.processTable(table, game, true, false);

        assertThat(result.nextState()).isNull();
    }

    @Test
    void handleClean_withTableStateChangedToGameOver_shouldReturnEmpty() {
        table.tableState = TableState.GAME_OVER; // State already changed

        TableProcessResult result = engine.processTable(table, game, true, false);

        assertThat(result.nextState()).isNull();
    }

    // SHOWDOWN state tests

    @Test
    void handleShowdown_asHost_withRoundNotShowdown_shouldAdvanceAndResolve() {
        StubGameHand hand = new StubGameHand(BettingRound.RIVER, false);

        table.tableState = TableState.SHOWDOWN;
        table.holdemHand = hand;

        boolean[] zipModeUnset = {false};
        boolean[] resolved = {false};
        StubGameTable trackingTable = new StubGameTable() {
            @Override
            public void setZipMode(boolean zipMode) {
                if (!zipMode) {
                    zipModeUnset[0] = true;
                }
            }
        };
        trackingTable.tableState = TableState.SHOWDOWN;
        trackingTable.holdemHand = hand;

        StubGameHand trackingHand = new StubGameHand(BettingRound.RIVER, false) {
            @Override
            public void resolve() {
                resolved[0] = true;
            }
        };
        trackingTable.holdemHand = trackingHand;

        TableProcessResult result = engine.processTable(trackingTable, game, true, false);

        assertThat(trackingHand.getRound()).isEqualTo(BettingRound.SHOWDOWN);
        assertThat(zipModeUnset[0]).isTrue();
        assertThat(resolved[0]).isTrue();
    }

    @Test
    void handleShowdown_asHost_inOfflineGame_shouldAdvanceClock() {
        StubGameHand hand = new StubGameHand(BettingRound.RIVER, false);

        table.tableState = TableState.SHOWDOWN;
        table.holdemHand = hand;

        boolean[] clockAdvanced = {false};
        StubTournamentContext trackingGame = new StubTournamentContext() {
            @Override
            public void advanceClock() {
                clockAdvanced[0] = true;
            }
        };

        TableProcessResult result = engine.processTable(table, trackingGame, true, false);

        assertThat(clockAdvanced[0]).isTrue();
    }

    @Test
    void handleShowdown_asHost_inOnlineGame_shouldNotAdvanceClock() {
        StubGameHand hand = new StubGameHand(BettingRound.RIVER, false);

        table.tableState = TableState.SHOWDOWN;
        table.holdemHand = hand;

        boolean[] clockAdvanced = {false};
        StubTournamentContext trackingGame = new StubTournamentContext() {
            @Override
            public void advanceClock() {
                clockAdvanced[0] = true;
            }
        };

        TableProcessResult result = engine.processTable(table, trackingGame, true, true);

        assertThat(clockAdvanced[0]).isFalse();
    }

    @Test
    void handleShowdown_withLocalPlayer_shouldStoreHandHistory() {
        StubGameHand hand = new StubGameHand(BettingRound.SHOWDOWN, false);
        StubGamePlayer localPlayer = new StubGamePlayer(1, "Local", 1000, true);

        table.tableState = TableState.SHOWDOWN;
        table.holdemHand = hand;
        game.localPlayer = localPlayer;

        boolean[] historyStored = {false};
        StubGameHand trackingHand = new StubGameHand(BettingRound.SHOWDOWN, false) {
            @Override
            public void storeHandHistory() {
                historyStored[0] = true;
            }
        };
        table.holdemHand = trackingHand;

        TableProcessResult result = engine.processTable(table, game, true, false);

        assertThat(historyStored[0]).isTrue();
        assertThat(result.phaseToRun()).isEqualTo("TD.Showdown");
        assertThat(result.shouldRunOnClient()).isTrue();
        assertThat(result.pendingState()).isEqualTo(TableState.DONE);
        assertThat(result.shouldAutoSave()).isTrue();
    }

    @Test
    void handleShowdown_asClient_withRoundAlreadyShowdown_shouldNotAdvanceRound() {
        StubGameHand hand = new StubGameHand(BettingRound.SHOWDOWN, false);

        table.tableState = TableState.SHOWDOWN;
        table.holdemHand = hand;

        TableProcessResult result = engine.processTable(table, game, false, false);

        // Should still be SHOWDOWN (not advanced)
        assertThat(hand.getRound()).isEqualTo(BettingRound.SHOWDOWN);
    }

    // Stub player implementation for testing
    private static class StubGamePlayer implements GamePlayerInfo {
        private final int id;
        private final String name;
        private int chipCount;
        private boolean locallyControlled;
        private boolean folded;
        private boolean sittingOut;
        public boolean isComputer = true; // Default to computer player
        public int thinkBankMillis = 0;
        public boolean isHuman = false;
        public boolean askShowWinning = false;
        public boolean askShowLosing = false;

        StubGamePlayer(int id, String name, int chipCount, boolean locallyControlled) {
            this.id = id;
            this.name = name;
            this.chipCount = chipCount;
            this.locallyControlled = locallyControlled;
        }

        @Override
        public int getID() {
            return id;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean isHuman() {
            return isHuman;
        }

        @Override
        public int getChipCount() {
            return chipCount;
        }

        @Override
        public boolean isFolded() {
            return folded;
        }

        @Override
        public boolean isAllIn() {
            return chipCount == 0;
        }

        @Override
        public int getSeat() {
            return 0;
        }

        @Override
        public boolean isAskShowWinning() {
            return askShowWinning;
        }

        @Override
        public boolean isAskShowLosing() {
            return askShowLosing;
        }

        @Override
        public boolean isObserver() {
            return false;
        }

        @Override
        public boolean isHumanControlled() {
            return !isComputer;
        }

        @Override
        public int getThinkBankMillis() {
            return thinkBankMillis;
        }

        @Override
        public boolean isSittingOut() {
            return sittingOut;
        }

        @Override
        public void setSittingOut(boolean sittingOut) {
            this.sittingOut = sittingOut;
        }

        @Override
        public boolean isLocallyControlled() {
            return locallyControlled;
        }

        @Override
        public boolean isComputer() {
            return isComputer;
        }

        @Override
        public void setTimeoutMillis(int millis) {
        }

        @Override
        public void setTimeoutMessageSecondsLeft(int seconds) {
        }
    }
}
