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
package com.donohoedigital.games.poker.core;

import java.util.List;

import com.donohoedigital.games.poker.core.event.GameEvent;
import com.donohoedigital.games.poker.core.event.GameEventBus;
import com.donohoedigital.games.poker.core.state.BettingRound;
import com.donohoedigital.games.poker.core.state.TableState;

/**
 * Core tournament state machine engine. Extracted from
 * TournamentDirector._processTable() (lines 674-888). Stateless - receives
 * collaborators via constructor, processes table state, returns result.
 *
 * <p>
 * <b>Phase 2 Status:</b> EXTRACTION COMPLETE - Not yet integrated into
 * TournamentDirector. See .claude/plans/twinkly-marinating-feigenbaum.md for
 * integration steps.
 *
 * <p>
 * Note: This engine defines minimal interfaces (GameTable, GameHand, etc.) that
 * will be implemented by poker module classes (PokerTable, HoldemHand) in Phase
 * 2 integration.
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
    public TableProcessResult processTable(GameTable table, TournamentContext game, boolean isHost, boolean isOnline) {
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

    private TableProcessResult handleNone(GameTable table, TournamentContext game, boolean isHost, boolean isOnline) {
        // NONE is a sentinel value meaning "no state" - used for pending state
        // initialization
        // A table should never actually be in NONE state during normal operation
        // If we get here, just return empty result (no state change)
        return TableProcessResult.builder().build();
    }

    private TableProcessResult handlePendingLoad(GameTable table, TournamentContext game, boolean isHost,
            boolean isOnline) {
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

    private TableProcessResult handlePending(GameTable table, TournamentContext game, boolean isHost,
            boolean isOnline) {
        // Extracted from TournamentDirector.doPending() (lines 981-1036)
        // and _processTable PENDING case (lines 730-732)
        // PENDING: Wait for players to respond to actions, handle timeouts

        // Check for scheduled start time (host only in online games)
        if (isHost && game.isScheduledStartEnabled()) {
            long startTime = game.getScheduledStartTime();
            // Guard against startTime=0 (would cause immediate auto-start)
            if (startTime > 0) {
                long currentTime = System.currentTimeMillis();
                int minPlayers = game.getMinPlayersForScheduledStart();
                int currentPlayers = game.getNumPlayers();

                if (currentTime >= startTime && currentPlayers >= minPlayers) {
                    // Auto-start: remove all from wait list
                    table.removeWaitAll();
                    // Note: sendDirectorChat() is UI/network-specific and stays in
                    // TournamentDirector
                    // The engine delegates the decision (removeWaitAll), not the communication
                }
            }
        }

        // Check for timeouts if there are players in the wait list
        if (table.getWaitSize() > 0 && isOnline) {
            handlePendingTimeouts(table, game);
        }

        // Wait list reduced by various phases when they call removeFromWaitList()
        // Next state initiated when responses from all players received
        if (table.getWaitSize() == 0) {
            // We enter BEGIN state from PENDING only after dealing high card for button
            // If auto-deal is on, we need to put in the pause here
            if (isHost && table.getPendingTableState() == TableState.BEGIN && table.isAutoDeal()) {
                // Note: we don't do full pause for online since each client
                // has to respond to a dialog which causes a pause
                int pauseMillis = isOnline ? 1000 : table.getAutoDealDelay();
                table.setPause(pauseMillis);
            }

            // Transition to pending state
            return TableProcessResult.builder().nextState(table.getPendingTableState()).shouldSleep(false).build();
        }

        // Still waiting - check if we should disable sleep
        // If pending on betting, and if in zip mode or waiting on computer, don't sleep
        // (In online games, we still sleep so as not to overrun clients with AI
        // actions)
        boolean shouldSleep = true;
        if (table.getPendingTableState() == TableState.BETTING) {
            if (table.isZipMode()) {
                shouldSleep = false;
            } else if (table.getWaitSize() > 0) {
                GamePlayerInfo waitingPlayer = table.getWaitPlayer(0);
                if (!waitingPlayer.isHumanControlled() && !isOnline) {
                    shouldSleep = false;
                }
            }
        }

        return TableProcessResult.builder().shouldSleep(shouldSleep).build();
    }

    private void handlePendingTimeouts(GameTable table, TournamentContext game) {
        // Extracted from TournamentDirector.doPendingTimeoutCheck() (lines 1042-1062)
        // and doBettingTimeoutCheck() (lines 1067-1145)

        TableState previousState = table.getPreviousTableState();
        long waitMillis = table.getMillisSinceLastStateChange();

        if (previousState == TableState.BETTING) {
            handleBettingTimeout(table, game, waitMillis);
        } else {
            // Non-betting timeout
            int timeoutMillis = (previousState == TableState.NEW_LEVEL_CHECK)
                    ? 30000 // NEWLEVEL_TIMEOUT_MILLIS
                    : 5000; // NON_BETTING_TIMEOUT_MILLIS

            if (waitMillis > timeoutMillis) {
                // Note: sendCancel() is network-specific (sends message to clients) - stays in
                // TD
                // Note: Logging with Utils.toString() is infrastructure concern - stays in TD
                // Engine makes the decision (timeout occurred), TD handles communication
                table.removeWaitAll();
            }
        }
    }

    private void handleBettingTimeout(GameTable table, TournamentContext game, long waitMillis) {
        // Extracted from TournamentDirector.doBettingTimeoutCheck() (lines 1067-1145)

        if (table.getWaitSize() == 0) {
            return;
        }

        GamePlayerInfo player = table.getWaitPlayer(0);
        if (!player.isHumanControlled()) {
            return;
        }

        // Get current betting round and use round-specific timeout
        GameHand hand = table.getHoldemHand();
        if (hand == null) {
            return;
        }

        int currentRound = hand.getRound().toLegacy();
        int timeoutSecs = game.getTimeoutForRound(currentRound);
        long timeoutMillis = timeoutSecs * 1000 + 1000; // pad timeout a bit
        long remainingMillis = timeoutMillis - waitMillis;
        int thinkBankMillis = player.getThinkBankMillis();

        // If timeout exceeded and no think bank remaining, player times out
        if (remainingMillis <= 0 && thinkBankMillis <= 0) {
            // Note: playerTimeout(table, player, TIMEOUT_ACTION) stays in
            // TournamentDirector
            // Note: This includes fold/check logic, UI updates, and network messages
            table.removeWaitAll();
        }
        // Note: Warning messages at 10/5 second marks stay in TournamentDirector
    }

    private TableProcessResult handleOnHold(GameTable table, TournamentContext game, boolean isHost, boolean isOnline) {
        // Extracted from TournamentDirector lines 722-726
        // ON_HOLD: if >1 player, transition to BEGIN
        if (table.getNumOccupiedSeats() > 1) {
            return TableProcessResult.builder().nextState(TableState.BEGIN).build();
        }
        return TableProcessResult.builder().build();
    }

    private TableProcessResult handleDealForButton(GameTable table, TournamentContext game, boolean isHost,
            boolean isOnline) {
        // Extracted from TournamentDirector.dealForButton() (lines 1185-1202)
        // and _processTable DEAL_FOR_BUTTON case (lines 740-743)
        // DEAL_FOR_BUTTON: Deal high card to determine button position

        if (isHost) {
            // Deal cards to assign button
            table.setButton();

            // Note: Multi-table coordination (doDealForButtonAllComputers) stays in
            // TournamentDirector
        }

        // Start clock when dealing for button (online games only)
        if (isOnline) {
            game.startGameClock();
        }

        // Run DealDisplayHigh phase to show dealt cards, then go to BEGIN
        return TableProcessResult.builder().phaseToRun("TD.DealDisplayHigh").shouldRunOnClient(true)
                .pendingState(TableState.BEGIN).build();
    }

    private TableProcessResult handleBegin(GameTable table, TournamentContext game, boolean isHost, boolean isOnline) {
        // Extracted from TournamentDirector lines 733-745
        // BEGIN: If auto-deal, start deal; else run WaitForDeal phase
        if (table.isAutoDeal()) {
            return TableProcessResult.builder().nextState(TableState.START_HAND).build();
        } else {
            return TableProcessResult.builder().phaseToRun("TD.WaitForDeal").nextState(TableState.BEGIN_WAIT).build();
        }
    }

    private TableProcessResult handleBeginWait(GameTable table, TournamentContext game, boolean isHost,
            boolean isOnline) {
        // Extracted from TournamentDirector lines 747-749
        // BEGIN_WAIT: State changed in doDeal() when Deal pressed - just wait
        return TableProcessResult.builder().build();
    }

    private TableProcessResult handleCheckEndHand(GameTable table, TournamentContext game, boolean isHost,
            boolean isOnline) {
        // Extracted from TournamentDirector.doCheckEndHand() (lines 1221-1248)
        // CHECK_END_HAND: Process end-of-hand operations, transition to CLEAN
        // Note: Table mutations (aiRebuy, addPendingRebuys, bootPlayers, nextLevel)
        // remain in TournamentDirector for now - engine just makes decisions

        return TableProcessResult.builder().phaseToRun("TD.CheckEndHand").shouldRunOnClient(true)
                .pendingState(TableState.CLEAN).build();
    }

    private TableProcessResult handleClean(GameTable table, TournamentContext game, boolean isHost, boolean isOnline) {
        // Extracted from TournamentDirector.doClean() (lines 1469-1591)
        // and _processTable CLEAN case (lines 768-780)
        // CLEAN: Clean up after hand, consolidate tables, check game over

        boolean shouldWaitForPhase = false;

        if (isHost) {
            // Note: Complex table cleanup and consolidation logic stays in
            // TournamentDirector:
            // - cleanTables(table, !bOneLeft) - removes eliminated players, processes wait
            // list
            // - OtherTables.consolidateTables() - multi-table consolidation algorithm (~200
            // lines)
            // - TDClean event gathering and tracking - requires mutable event listener
            // infrastructure
            // - Observer management and movement - UI-specific player visibility
            // - Wait-listed player handling - network player queue management
            // - Network notifications (notifyPlayersCleanDone) - broadcasts state to
            // clients
            // These are infrastructure concerns (UI, network, multi-table coordination),
            // not pure game logic.

            // Check for game over (only one player left)
            if (game.isOnePlayerLeft()) {
                // Note: Game over processing stays in TournamentDirector:
                // - Find last player with chips
                // - game_.playerOut(player) - awards final chips
                // - sendDirectorChat() - announce winner
                // - setGameOver() - marks game as complete

                // After game over, table state will be GAME_OVER (set by setGameOver in TD)
                // Engine will handle this in next iteration via handleGameOver()
            }

            // If players were added to this table, display table moves (practice only)
            java.util.List<GamePlayerInfo> addedPlayers = table.getAddedPlayersList();
            if (addedPlayers != null && !addedPlayers.isEmpty()) {
                if (!isOnline) {
                    // Practice mode: show DisplayTableMoves phase and wait
                    shouldWaitForPhase = true;
                } else {
                    // Online mode: chat messages sent by TD, no phase needed
                    // Note: sendDealerChat() calls stay in TournamentDirector
                }
            }
        }

        // Build result based on whether table state was changed by doClean
        TableState currentState = table.getTableState();

        // doClean may have changed state to ON_HOLD or GAME_OVER
        if (currentState == TableState.ON_HOLD || currentState == TableState.GAME_OVER) {
            // State already changed, don't override it
            return TableProcessResult.builder().build();
        }

        // Normal case: transition to NEW_LEVEL_CHECK
        if (shouldWaitForPhase) {
            return TableProcessResult.builder().phaseToRun("TD.DisplayTableMoves")
                    .pendingState(TableState.NEW_LEVEL_CHECK).build();
        } else {
            return TableProcessResult.builder().nextState(TableState.NEW_LEVEL_CHECK).shouldSleep(false).build();
        }
    }

    private TableProcessResult handleNewLevelCheck(GameTable table, TournamentContext game, boolean isHost,
            boolean isOnline) {
        // Extracted from TournamentDirector.doNewLevelCheck() (lines 1849-1866)
        // and _processTable NEW_LEVEL_CHECK case (lines 782-793)
        // NEW_LEVEL_CHECK: Handle level changes, AI rebuys/addons

        boolean levelChanged = (game.getLevel() != table.getLevel());

        if (levelChanged) {
            // Level has changed - process AI rebuys and add-ons (host only)
            if (isHost) {
                table.processAIRebuys();
                table.processAIAddOns();
                // Note: Multi-table coordination (doLevelCheckAllComputers) stays in
                // TournamentDirector
            }

            // Run NewLevelActions phase and transition to COLOR_UP
            return TableProcessResult.builder().phaseToRun("TD.NewLevelActions").shouldRunOnClient(true)
                    .pendingState(TableState.COLOR_UP).build();
        } else {
            // No level change - clear rebuy list and go to START_HAND
            table.clearRebuyList();
            return TableProcessResult.builder().nextState(TableState.START_HAND).shouldSleep(false).build();
        }
    }

    private TableProcessResult handleColorUp(GameTable table, TournamentContext game, boolean isHost,
            boolean isOnline) {
        // Extracted from TournamentDirector.doColorUp() (lines 1886-1911)
        // and _processTable COLOR_UP case (lines 795-806)
        // COLOR_UP: Determine if color-up is needed, run ColorUp phase if so

        if (isHost) {
            int minNow = game.getLastMinChip();
            int minNext = game.getMinChip();

            if (minNext > minNow) {
                // Color-up is needed - set next min chip and do determination
                table.setNextMinChip(minNext);
                table.doColorUpDetermination();
                // Note: Actual colorUp() and colorUpFinish() done in ColorUpFinish phase
                // Note: Multi-table coordination (doColorUpAllComputers) stays in
                // TournamentDirector
            }
        }

        // Check if color-up is needed
        boolean needsColorUp = table.isColoringUp();

        if (needsColorUp) {
            // Run ColorUp phase on client, then transition to START_HAND
            return TableProcessResult.builder().phaseToRun("TD.ColorUp").shouldRunOnClient(true)
                    .pendingState(TableState.START_HAND).build();
        } else {
            // No color-up needed - go directly to START_HAND
            return TableProcessResult.builder().nextState(TableState.START_HAND).shouldSleep(false).build();
        }
    }

    private TableProcessResult handleStartHand(GameTable table, TournamentContext game, boolean isHost,
            boolean isOnline) {
        // Extracted from TournamentDirector START_HAND case (lines 808-831)
        // and doStart() (lines 1970-1991), doBreak() (lines 1935-1950)
        // START_HAND: Handle color-up finish, check for break, or start new hand

        if (isHost && table.isColoringUp()) {
            // Complete color-up for non-current tables
            // (current tables handle this in ColorUp phase)
            table.colorUp();
            table.colorUpFinish();
        }

        // Check if current level is a break period
        if (game.isBreakLevel(game.getLevel())) {
            // Start break period
            if (isHost) {
                table.startBreak();
                // Note: Multi-table coordination (doBreakAllComputers) stays in
                // TournamentDirector
            }

            return TableProcessResult.builder().nextState(TableState.BREAK).shouldRunOnClient(true).build();
        }

        // Start new hand
        if (isHost) {
            table.startNewHand();
            // Note: Multi-table coordination (doStartAllComputers) stays in
            // TournamentDirector

            // Advance clock in practice (offline) games
            if (!isOnline) {
                game.advanceClock();
            }
        }

        return TableProcessResult.builder().phaseToRun("TD.DealDisplayHand").shouldRunOnClient(true)
                .pendingState(TableState.BETTING).build();
    }

    private TableProcessResult handleBetting(GameTable table, TournamentContext game, boolean isHost,
            boolean isOnline) {
        // Extracted from TournamentDirector.doBetting() (lines 2019-2094)
        // and _processTable BETTING case (lines 844-848)
        // BETTING: Handle player betting round - MOST COMPLEX HANDLER

        GameHand hand = table.getHoldemHand();
        if (hand == null) {
            return TableProcessResult.builder().build();
        }

        // If hand is done, transition to next state
        if (hand.isDone()) {
            TableState nextState = getNextBettingState(hand);
            return TableProcessResult.builder().nextState(nextState).build();
        }

        // Get current player (initializes player order on first call)
        GamePlayerInfo currentPlayer = hand.getCurrentPlayerWithInit();
        if (currentPlayer == null) {
            return TableProcessResult.builder().build();
        }

        // Host initializes timeout tracking for current player
        if (isHost) {
            currentPlayer.setTimeoutMillis(game.getTimeoutSeconds() * 1000);
            currentPlayer.setTimeoutMessageSecondsLeft(0);
        }

        // Player sitting out - auto-fold
        if (currentPlayer.isSittingOut()) {
            table.setPause(1100); // SLEEP_MILLIS + 100
            processPlayerAction(currentPlayer, PlayerAction.fold(), table, hand, game);
            return TableProcessResult.builder().nextState(TableState.BETTING).build();
        }

        // Locally controlled player (local human or AI)
        if (currentPlayer.isLocallyControlled()) {
            // Phase 3B: Use actionProvider for all locally controlled players
            // Create ActionOptions from current game state
            ActionOptions options = createActionOptions(currentPlayer, hand, game);

            // Get action from provider (blocking call - waits for human input or AI
            // decision)
            PlayerAction action = actionProvider.getAction(currentPlayer, options);

            if (action != null) {
                // Process the action and advance game state
                processPlayerAction(currentPlayer, action, table, hand, game);
                return TableProcessResult.builder().nextState(TableState.BETTING).build();
            } else {
                // Safety fallback: if actionProvider returns null, auto-fold
                PlayerAction foldAction = PlayerAction.fold();
                processPlayerAction(currentPlayer, foldAction, table, hand, game);
                return TableProcessResult.builder().nextState(TableState.BETTING).build();
            }
        }

        // Remote player (host waits for their action)
        if (isHost) {
            table.addWait(currentPlayer);

            return TableProcessResult.builder().shouldRunOnClient(true).shouldOnlySendToWaitList(true)
                    .shouldAddAllHumans(false).pendingState(TableState.BETTING).build();
        }

        // Default: stay in BETTING
        return TableProcessResult.builder().build();
    }

    /**
     * Create ActionOptions from current game state for a player decision.
     */
    private ActionOptions createActionOptions(GamePlayerInfo player, GameHand hand, TournamentContext game) {
        // Get amounts from hand
        int amountToCall = hand.getAmountToCall(player);
        int chipCount = player.getChipCount();

        // Determine available actions
        boolean canCheck = (amountToCall == 0);
        boolean canCall = (amountToCall > 0) && (chipCount > 0); // Can call for less (all-in) if short-stacked
        boolean canBet = (amountToCall == 0) && (chipCount > 0);
        boolean canRaise = (amountToCall > 0) && (chipCount > amountToCall);
        boolean canFold = true;

        // Get betting limits
        int minBet = hand.getMinBet();
        int maxBet = chipCount;
        int minRaise = hand.getMinRaise();
        int maxRaise = chipCount;

        // Get timeout from tournament settings
        int timeoutSeconds = game.getTimeoutForRound(hand.getRound().toLegacy());

        return new ActionOptions(canCheck, canCall, canBet, canRaise, canFold, amountToCall, minBet, maxBet, minRaise,
                maxRaise, timeoutSeconds);
    }

    /**
     * Process a player action and update game state. Delegates to GameHand for
     * action processing (which handles conversion to internal representation).
     */
    private void processPlayerAction(GamePlayerInfo player, PlayerAction action, GameTable table, GameHand hand,
            TournamentContext game) {
        // Delegate to hand to process action (converts to HandAction internally in
        // poker module)
        hand.applyPlayerAction(player, action);

        // Emit event for UI/logging
        eventBus.publish(
                new GameEvent.PlayerActed(table.getNumber(), player.getID(), action.actionType(), action.amount()));
    }

    private TableState getNextBettingState(GameHand hand) {
        // Extracted from TournamentDirector.nextBettingState() logic
        // Determine next state after betting round completes

        BettingRound round = hand.getRound();

        // If hand is uncontested (all but one player folded), go directly to showdown
        // This ensures the pot is properly resolved and awarded to the remaining player
        if (hand.isUncontested() || hand.getNumWithCards() <= 1) {
            return TableState.SHOWDOWN;
        }

        // After river, go to pre-showdown (or showdown if only 1 player)
        if (round == BettingRound.RIVER) {
            return (hand.getNumWithCards() > 1) ? TableState.PRE_SHOWDOWN : TableState.SHOWDOWN;
        }

        // After other rounds, go to deal community cards
        return TableState.COMMUNITY;
    }

    private TableProcessResult handleCommunity(GameTable table, TournamentContext game, boolean isHost,
            boolean isOnline) {
        // Extracted from TournamentDirector.doCommunity() (lines 2137-2161)
        // and _processTable COMMUNITY case (lines 850-866)
        // COMMUNITY: Deal community cards and advance to next betting round

        GameHand hand = table.getHoldemHand();
        if (hand == null) {
            return TableProcessResult.builder().build();
        }

        if (isHost) {
            // Advance to next round (flop -> turn -> river) and deal cards
            hand.advanceRound();

            // Advance clock in practice (offline) games
            if (!isOnline) {
                game.advanceClock(); // Action 2, 3, 4 of 5 (flop, turn, river)
            }
        }

        // Determine if we need to run DealCommunity phase
        // In practice mode: always run
        // In online mode: only run if multiple players still have cards
        boolean shouldRunPhase = !isOnline || (isOnline && hand.getNumWithCards() > 1);

        if (shouldRunPhase) {
            // Run DealCommunity phase, then go to BETTING
            boolean disableSleep = table.isZipMode();
            return TableProcessResult.builder().phaseToRun("TD.DealCommunity").shouldRunOnClient(true)
                    .pendingState(TableState.BETTING).shouldSleep(!disableSleep).build();
        } else {
            // Skip phase - hand is done or only one player left
            // Determine next state based on current round
            TableState nextState = (hand.getRound() == BettingRound.RIVER)
                    ? TableState.PRE_SHOWDOWN
                    : TableState.COMMUNITY;

            return TableProcessResult.builder().nextState(nextState).shouldSleep(false).build();
        }
    }

    private TableProcessResult handlePreShowdown(GameTable table, TournamentContext game, boolean isHost,
            boolean isOnline) {
        // Extracted from TournamentDirector.doPreShowdown() (lines 2166-2226)
        // and _processTable PRE_SHOWDOWN case (lines 868-877)
        // PRE_SHOWDOWN: Handle pre-showdown logic, ask winners if they want to show
        // cards

        GameHand hand = table.getHoldemHand();
        if (hand == null) {
            return TableProcessResult.builder().nextState(TableState.SHOWDOWN).shouldSleep(false).build();
        }

        // Host does pre-resolve
        if (isHost) {
            hand.preResolve(isOnline);
        }

        // Client clears wait list since host sends it over
        // (clients don't use wait list anyhow, this avoids warning message)
        if (!isHost) {
            table.removeWaitAll();
        }

        // Online games: figure out if we need to run pre-showdown step
        if (isOnline) {
            List<GamePlayerInfo> winners = hand.getPreWinners();
            List<GamePlayerInfo> losers = hand.getPreLosers();

            List<Integer> winnerIds = null;
            GamePlayerInfo localPlayer = game.getLocalPlayer();
            boolean isLocalInWaitList = false;

            // Check winners who want to be asked about showing
            for (GamePlayerInfo player : winners) {
                if (player.isHuman() && player.isAskShowWinning() && hand.isUncontested()) {
                    table.addWait(player);
                    if (winnerIds == null) {
                        winnerIds = new java.util.ArrayList<>();
                    }
                    winnerIds.add(player.getID());
                    if (player == localPlayer) {
                        isLocalInWaitList = true;
                    }
                }
            }

            // Check losers who want to be asked about showing
            for (GamePlayerInfo player : losers) {
                if (player.isHuman() && player.isAskShowLosing()) {
                    table.addWait(player);
                    if (player == localPlayer) {
                        isLocalInWaitList = true;
                    }
                }
            }

            // If we have players in wait list
            if (table.getWaitSize() > 0) {
                // Only run pre-showdown phase if local player is in the list
                if (isLocalInWaitList) {
                    TableProcessResult.Builder builder = TableProcessResult.builder().phaseToRun("TD.PreShowdown");

                    // Add winner IDs as phase parameter if present
                    if (winnerIds != null) {
                        java.util.Map<String, Object> params = new java.util.HashMap<>();
                        params.put("PARAM_WINNERS", winnerIds);
                        builder = builder.phaseParams(params);
                    }

                    return builder.shouldRunOnClient(true).shouldAddAllHumans(false).shouldOnlySendToWaitList(true)
                            .pendingState(TableState.SHOWDOWN).build();
                } else {
                    // Not local player's turn - wait for others
                    return TableProcessResult.builder().shouldRunOnClient(true).shouldAddAllHumans(false)
                            .shouldOnlySendToWaitList(true).pendingState(TableState.SHOWDOWN).build();
                }
            }
        }

        // No pre-showdown phase needed - go directly to showdown
        return TableProcessResult.builder().nextState(TableState.SHOWDOWN).shouldSleep(false).build();
    }

    private TableProcessResult handleShowdown(GameTable table, TournamentContext game, boolean isHost,
            boolean isOnline) {
        // Extracted from TournamentDirector.doShowdown() (lines 2231-2261)
        // and _processTable SHOWDOWN case (lines 879-886)
        // SHOWDOWN: Resolve hand, show cards, award pots

        GameHand hand = table.getHoldemHand();
        if (hand == null) {
            return TableProcessResult.builder().nextState(TableState.DONE).shouldAutoSave(true)
                    .shouldSave(table.isCurrent()).shouldSleep(false).build();
        }

        // BUG 462 - don't re-run logic if already run (safety check)
        if (isHost && hand.getRound() != BettingRound.SHOWDOWN) {
            // Note: Event recording (ret_.startListening) stays in TournamentDirector

            // Advance round to SHOWDOWN
            hand.advanceRound();

            // Unset zip mode so end hand event is called outside zip mode
            table.setZipMode(false);

            // Resolve the hand (determine winners, award pots)
            hand.resolve();

            // Advance clock in practice (offline) games
            if (!isOnline) {
                game.advanceClock(); // Action 5 of 5
            }
        }

        // Store hand history - called here so it happens on both client and host
        GamePlayerInfo localPlayer = game.getLocalPlayer();
        if (localPlayer != null && (!localPlayer.isObserver() || isHost)) {
            hand.storeHandHistory();
        }

        // Run showdown phase to display results, then go to DONE
        return TableProcessResult.builder().phaseToRun("TD.Showdown").shouldRunOnClient(true)
                .pendingState(TableState.DONE).shouldAutoSave(true) // Save practice games
                .shouldSave(table.isCurrent()) // Save online games if current table
                .shouldSleep(false).build();
    }

    private TableProcessResult handleDone(GameTable table, TournamentContext game, boolean isHost, boolean isOnline) {
        // Extracted from TournamentDirector lines 876-880
        // DONE: transition to BEGIN to start next hand
        return TableProcessResult.builder().nextState(TableState.BEGIN).build();
    }

    private TableProcessResult handleGameOver(GameTable table, TournamentContext game, boolean isHost,
            boolean isOnline) {
        // Extracted from TournamentDirector lines 882-887
        // GAME_OVER: terminal state, no next state
        return TableProcessResult.builder().build();
    }

    private TableProcessResult handleBreak(GameTable table, TournamentContext game, boolean isHost, boolean isOnline) {
        // Extracted from TournamentDirector.doCheckEndBreak() (lines 1305-1337)
        // and _processTable BREAK case (lines 833-842)
        // BREAK: Check if break period is over, advance level if needed

        boolean bEndOfBreak = false;

        if (isHost) {
            // In practice (offline) games, manually advance the clock during break
            if (!isOnline) {
                game.advanceClockBreak();
            }

            // Check if level time expired and advance if needed
            if (game.isLevelExpired()) {
                game.nextLevel();
            }

            // Break ends when game level changes (table level gets updated later)
            if (game.getLevel() != table.getLevel()) {
                bEndOfBreak = true;
            }
        }

        // Restart clock after break ends (in online games)
        if (bEndOfBreak && isOnline) {
            game.startGameClock();
        }

        // Build result
        if (bEndOfBreak) {
            return TableProcessResult.builder().nextState(TableState.NEW_LEVEL_CHECK).build();
        } else {
            // Still in break - pause briefly in online mode
            // Note: Pause handling stays in TournamentDirector
            return TableProcessResult.builder().build();
        }
    }
}
