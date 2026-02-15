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
            return false;
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
        public boolean isZipMode() {
            return false;
        }

        @Override
        public void setZipMode(boolean zipMode) {
        }

        @Override
        public void setButton() {
        }

        @Override
        public void removeWaitAll() {
        }

        @Override
        public void addWait(GamePlayerInfo player) {
        }

        @Override
        public int getWaitSize() {
            return 0;
        }

        @Override
        public GamePlayerInfo getWaitPlayer(int index) {
            return null;
        }

        @Override
        public long getMillisSinceLastStateChange() {
            return 0;
        }

        @Override
        public void setPause(int millis) {
        }

        @Override
        public int getAutoDealDelay() {
            return 0;
        }

        @Override
        public void simulateHand() {
        }

        @Override
        public java.util.List<GamePlayerInfo> getAddedPlayersList() {
            return new java.util.ArrayList<>();
        }

        @Override
        public boolean isRemoved() {
            return false;
        }

        @Override
        public boolean isAllComputer() {
            return false;
        }
    }

    private static class StubTournamentContext implements TournamentContext {
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
            return 0;
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
            return 1;
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
            return 5;
        }

        @Override
        public int getMinChip() {
            return 5;
        }

        @Override
        public void advanceClock() {
        }

        @Override
        public boolean isBreakLevel(int level) {
            return false;
        }

        @Override
        public GamePlayerInfo getLocalPlayer() {
            return null;
        }

        @Override
        public boolean isScheduledStartEnabled() {
            return false;
        }

        @Override
        public long getScheduledStartTime() {
            return 0;
        }

        @Override
        public int getMinPlayersForScheduledStart() {
            return 2;
        }

        @Override
        public int getTimeoutForRound(int round) {
            return 30;
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
            return false;
        }
    }

    private static class StubGameHand implements GameHand {
        public BettingRound round;
        public boolean done;
        public int numWithCards = 2; // Default to 2 players with cards

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
        }

        @Override
        public void resolve() {
        }

        @Override
        public void storeHandHistory() {
        }

        @Override
        public java.util.List<GamePlayerInfo> getPreWinners() {
            return new java.util.ArrayList<>();
        }

        @Override
        public java.util.List<GamePlayerInfo> getPreLosers() {
            return new java.util.ArrayList<>();
        }

        @Override
        public boolean isUncontested() {
            return false;
        }

        @Override
        public GamePlayerInfo getCurrentPlayerWithInit() {
            return null;
        }
    }
}
