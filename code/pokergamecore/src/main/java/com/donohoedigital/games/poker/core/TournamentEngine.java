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

import com.donohoedigital.games.poker.core.event.GameEventBus;
import com.donohoedigital.games.poker.core.state.BettingRound;
import com.donohoedigital.games.poker.core.state.TableState;

/**
 * Core tournament state machine engine. Extracted from
 * TournamentDirector._processTable() (lines 674-888). Stateless - receives
 * collaborators via constructor, processes table state, returns result.
 */
public class TournamentEngine {
    private final GameEventBus eventBus;
    private final PlayerActionProvider actionProvider;

    /**
     * Create a tournament engine.
     *
     * @param eventBus
     *            for publishing game events
     * @param actionProvider
     *            for obtaining player decisions
     */
    public TournamentEngine(GameEventBus eventBus, PlayerActionProvider actionProvider) {
        this.eventBus = eventBus;
        this.actionProvider = actionProvider;
    }

    /**
     * Process a table's current state and determine what to do next.
     *
     * @param table
     *            the table to process
     * @param game
     *            the game context
     * @param isHost
     *            true if this is the host/server
     * @param isOnline
     *            true if this is an online game
     * @return result with next state, phase to run, and flags
     */
    public TableProcessResult processTable(GameTable table, GameContext game, boolean isHost, boolean isOnline) {
        TableState state = table.getTableState();

        return switch (state) {
            case NONE -> handleNone(table, game, isHost, isOnline);
            case PENDING_LOAD -> handlePendingLoad(table, game, isHost, isOnline);
            case PENDING -> handlePending(table, game, isHost, isOnline);
            case ON_HOLD -> handleOnHold(table, game, isHost, isOnline);
            case DEAL_FOR_BUTTON -> handleDealForButton(table, game, isHost, isOnline);
            case BEGIN -> handleBegin(table, game, isHost, isOnline);
            case BEGIN_WAIT -> handleBeginWait(table, game, isHost, isOnline);
            case CHECK_END_HAND -> handleCheckEndHand(table, game, isHost, isOnline);
            case CLEAN -> handleClean(table, game, isHost, isOnline);
            case NEW_LEVEL_CHECK -> handleNewLevelCheck(table, game, isHost, isOnline);
            case COLOR_UP -> handleColorUp(table, game, isHost, isOnline);
            case START_HAND -> handleStartHand(table, game, isHost, isOnline);
            case BETTING -> handleBetting(table, game, isHost, isOnline);
            case COMMUNITY -> handleCommunity(table, game, isHost, isOnline);
            case PRE_SHOWDOWN -> handlePreShowdown(table, game, isHost, isOnline);
            case SHOWDOWN -> handleShowdown(table, game, isHost, isOnline);
            case DONE -> handleDone(table, game, isHost, isOnline);
            case GAME_OVER -> handleGameOver(table, game, isHost, isOnline);
            case BREAK -> handleBreak(table, game, isHost, isOnline);
        };
    }

    private TableProcessResult handleNone(GameTable table, GameContext game, boolean isHost, boolean isOnline) {
        // TODO: Implement NONE state handler
        return TableProcessResult.builder().build();
    }

    private TableProcessResult handlePendingLoad(GameTable table, GameContext game, boolean isHost, boolean isOnline) {
        // Extracted from TournamentDirector lines 692-716
        // PENDING_LOAD: Transition to PENDING, optionally run pending phase
        TableProcessResult.Builder builder = TableProcessResult.builder().nextState(TableState.PENDING);

        // If there's a pending phase to run, include it
        String pendingPhase = table.getPendingPhase();
        if (pendingPhase != null && !pendingPhase.isEmpty()) {
            builder.phaseToRun(pendingPhase);
        }

        return builder.build();
    }

    private TableProcessResult handlePending(GameTable table, GameContext game, boolean isHost, boolean isOnline) {
        // Extracted from TournamentDirector lines 718-720
        // PENDING: Delegates to doPending which handles state transitions internally
        // For Phase 1, return empty result (complex logic extracted in Phase 2)
        return TableProcessResult.builder().build();
    }

    private TableProcessResult handleOnHold(GameTable table, GameContext game, boolean isHost, boolean isOnline) {
        // Extracted from TournamentDirector lines 722-726
        // ON_HOLD: if >1 player, transition to BEGIN
        if (table.getNumOccupiedSeats() > 1) {
            return TableProcessResult.builder().nextState(TableState.BEGIN).build();
        }
        return TableProcessResult.builder().build();
    }

    private TableProcessResult handleDealForButton(GameTable table, GameContext game, boolean isHost,
            boolean isOnline) {
        // Extracted from TournamentDirector lines 728-731
        // DEAL_FOR_BUTTON: Calls dealForButton, sets pending state to BEGIN
        // For Phase 1, just set pending state (dealForButton logic extracted in Phase
        // 2)
        return TableProcessResult.builder().pendingState(TableState.BEGIN).build();
    }

    private TableProcessResult handleBegin(GameTable table, GameContext game, boolean isHost, boolean isOnline) {
        // Extracted from TournamentDirector lines 733-745
        // BEGIN: If auto-deal, start deal; else run WaitForDeal phase
        if (table.isAutoDeal()) {
            return TableProcessResult.builder().nextState(TableState.START_HAND).build();
        } else {
            return TableProcessResult.builder().phaseToRun("TD.WaitForDeal").nextState(TableState.BEGIN_WAIT).build();
        }
    }

    private TableProcessResult handleBeginWait(GameTable table, GameContext game, boolean isHost, boolean isOnline) {
        // Extracted from TournamentDirector lines 747-749
        // BEGIN_WAIT: State changed in doDeal() when Deal pressed - just wait
        return TableProcessResult.builder().build();
    }

    private TableProcessResult handleCheckEndHand(GameTable table, GameContext game, boolean isHost, boolean isOnline) {
        // Extracted from TournamentDirector lines 751-754
        // CHECK_END_HAND: Calls doCheckEndHand, sets pending state to CLEAN
        // For Phase 1, just set pending state (doCheckEndHand logic extracted in Phase
        // 2)
        return TableProcessResult.builder().pendingState(TableState.CLEAN).build();
    }

    private TableProcessResult handleClean(GameTable table, GameContext game, boolean isHost, boolean isOnline) {
        // Extracted from TournamentDirector lines 756-768
        // CLEAN: Calls doClean, transitions to NEW_LEVEL_CHECK
        // Complex logic in doClean (table consolidation, wait list) extracted in Phase
        // 2
        // For Phase 1, simple transition to NEW_LEVEL_CHECK
        return TableProcessResult.builder().nextState(TableState.NEW_LEVEL_CHECK).shouldSleep(false).build();
    }

    private TableProcessResult handleNewLevelCheck(GameTable table, GameContext game, boolean isHost,
            boolean isOnline) {
        // Extracted from TournamentDirector lines 770-782
        // NEW_LEVEL_CHECK: Checks for level changes, color-up, breaks
        // Complex logic in doNewLevelCheck extracted in Phase 2
        // For Phase 1, simple transition to START_HAND
        return TableProcessResult.builder().nextState(TableState.START_HAND).build();
    }

    private TableProcessResult handleColorUp(GameTable table, GameContext game, boolean isHost, boolean isOnline) {
        // Extracted from TournamentDirector lines 784-797
        // COLOR_UP: Performs color-up, transitions to START_HAND
        // Complex logic in doColorUp extracted in Phase 2
        return TableProcessResult.builder().nextState(TableState.START_HAND).build();
    }

    private TableProcessResult handleStartHand(GameTable table, GameContext game, boolean isHost, boolean isOnline) {
        // Extracted from TournamentDirector lines 799-817
        // START_HAND: Starts a new hand, transitions to BETTING
        // Complex logic in doStart extracted in Phase 2
        return TableProcessResult.builder().nextState(TableState.BETTING).build();
    }

    private TableProcessResult handleBetting(GameTable table, GameContext game, boolean isHost, boolean isOnline) {
        // Extracted from TournamentDirector lines 832-836
        // BETTING: Handles player betting rounds
        // Very complex logic in doBetting (player actions, AI, timeouts) extracted in
        // Phase 2
        // For Phase 1, return result with sleep disabled for offline games
        return TableProcessResult.builder().shouldSleep(isOnline).build();
    }

    private TableProcessResult handleCommunity(GameTable table, GameContext game, boolean isHost, boolean isOnline) {
        // Extracted from TournamentDirector lines 838-854
        // COMMUNITY: Deals community cards, advances betting rounds
        GameHand hand = table.getHoldemHand();
        if (hand == null) {
            return TableProcessResult.builder().build();
        }

        // Complex logic in doCommunity extracted in Phase 2
        // For Phase 1, simple state transitions based on current round
        if (hand.getRound() == BettingRound.RIVER) {
            return TableProcessResult.builder().nextState(TableState.PRE_SHOWDOWN).shouldSleep(false).build();
        } else {
            return TableProcessResult.builder().nextState(TableState.COMMUNITY).shouldSleep(false).build();
        }
    }

    private TableProcessResult handlePreShowdown(GameTable table, GameContext game, boolean isHost, boolean isOnline) {
        // Extracted from TournamentDirector lines 856-865
        // PRE_SHOWDOWN: Prepares for showdown, transitions to SHOWDOWN
        // Complex logic in doPreShowdown extracted in Phase 2
        return TableProcessResult.builder().nextState(TableState.SHOWDOWN).shouldSleep(false).build();
    }

    private TableProcessResult handleShowdown(GameTable table, GameContext game, boolean isHost, boolean isOnline) {
        // Extracted from TournamentDirector lines 867-874
        // SHOWDOWN: Shows cards, awards pots, transitions to DONE
        // Complex logic in doShowdown extracted in Phase 2
        return TableProcessResult.builder().nextState(TableState.DONE).shouldAutoSave(true).shouldSave(true)
                .shouldSleep(false).build();
    }

    private TableProcessResult handleDone(GameTable table, GameContext game, boolean isHost, boolean isOnline) {
        // Extracted from TournamentDirector lines 876-880
        // DONE: transition to BEGIN to start next hand
        return TableProcessResult.builder().nextState(TableState.BEGIN).build();
    }

    private TableProcessResult handleGameOver(GameTable table, GameContext game, boolean isHost, boolean isOnline) {
        // Extracted from TournamentDirector lines 882-887
        // GAME_OVER: terminal state, no next state
        return TableProcessResult.builder().build();
    }

    private TableProcessResult handleBreak(GameTable table, GameContext game, boolean isHost, boolean isOnline) {
        // Extracted from TournamentDirector lines 821-830
        // BREAK: Checks if break is done, transitions to NEW_LEVEL_CHECK
        // Complex logic in doCheckEndBreak extracted in Phase 2
        return TableProcessResult.builder().nextState(TableState.NEW_LEVEL_CHECK).build();
    }
}
