/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026  Doug Donohoe, DD Poker Community
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * For the full License text, please see the LICENSE.txt file in the root directory
 * of this project.
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
package com.donohoedigital.games.poker.gameserver;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.donohoedigital.games.poker.core.GameHand;
import com.donohoedigital.games.poker.core.GameTable;
import com.donohoedigital.games.poker.core.TableProcessResult;
import com.donohoedigital.games.poker.core.TournamentContext;
import com.donohoedigital.games.poker.core.TournamentEngine;
import com.donohoedigital.games.poker.core.event.GameEvent;
import com.donohoedigital.games.poker.core.state.ActionType;
import com.donohoedigital.games.poker.core.state.TableState;
import com.donohoedigital.games.poker.model.LevelAdvanceMode;

/**
 * Server-side tournament director. Drives TournamentEngine without Swing
 * dependencies.
 *
 * <p>
 * This is the server equivalent of TournamentDirector.run(). It processes all
 * tables each cycle, handles multi-table consolidation, and manages game
 * lifecycle (pause/resume/shutdown).
 *
 * <p>
 * Follows the pattern proven by HeadlessGameRunnerTest, but with real
 * PokerGame/PokerTable/HoldemHand objects and server-side adapters for events
 * and player actions.
 */
public class ServerTournamentDirector implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ServerTournamentDirector.class);
    private static final int SLEEP_MILLIS = 10; // Short sleep to prevent CPU spinning

    private final TournamentEngine engine;
    private final TournamentContext tournament;
    private final ServerGameEventBus eventBus;
    private final ServerPlayerActionProvider actionProvider;
    private final GameServerProperties properties;
    private final Consumer<GameLifecycleEvent> lifecycleCallback;

    private final BiPredicate<Integer, Integer> rebuyOfferCallback;
    private final BiPredicate<Integer, Integer> addonOfferCallback;

    private volatile boolean running;
    private volatile boolean paused;
    private volatile boolean shutdownRequested;

    /**
     * Create a new server tournament director (test-compatible overload, no
     * rebuy/addon callbacks).
     */
    public ServerTournamentDirector(TournamentEngine engine, TournamentContext tournament, ServerGameEventBus eventBus,
            ServerPlayerActionProvider actionProvider, GameServerProperties properties,
            Consumer<GameLifecycleEvent> lifecycleCallback) {
        this(engine, tournament, eventBus, actionProvider, properties, lifecycleCallback, null, null);
    }

    /**
     * Create a new server tournament director with rebuy/addon callbacks.
     *
     * @param engine
     *            the tournament engine for state machine processing
     * @param tournament
     *            the tournament context with tables and players
     * @param eventBus
     *            the event bus for publishing game events
     * @param actionProvider
     *            the action provider for AI and human players
     * @param properties
     *            server configuration properties
     * @param lifecycleCallback
     *            callback for game lifecycle events (GameInstance listens)
     * @param rebuyOfferCallback
     *            called with (playerId, tableId) when a rebuy is offered; returns
     *            true if accepted. May be null to disable rebuy offers.
     * @param addonOfferCallback
     *            called with (playerId, tableId) when an addon is offered; returns
     *            true if accepted. May be null to disable addon offers.
     */
    public ServerTournamentDirector(TournamentEngine engine, TournamentContext tournament, ServerGameEventBus eventBus,
            ServerPlayerActionProvider actionProvider, GameServerProperties properties,
            Consumer<GameLifecycleEvent> lifecycleCallback, BiPredicate<Integer, Integer> rebuyOfferCallback,
            BiPredicate<Integer, Integer> addonOfferCallback) {
        this.engine = engine;
        this.tournament = tournament;
        this.eventBus = eventBus;
        this.actionProvider = actionProvider;
        this.properties = properties;
        this.lifecycleCallback = lifecycleCallback;
        this.rebuyOfferCallback = rebuyOfferCallback;
        this.addonOfferCallback = addonOfferCallback;
    }

    /**
     * Main game loop. Processes all tables until tournament completion or shutdown.
     */
    @Override
    public void run() {
        running = true;
        lifecycleCallback.accept(GameLifecycleEvent.STARTED);

        try {
            while (running && !shutdownRequested) {
                if (paused) {
                    sleepMillis(SLEEP_MILLIS);
                    continue;
                }

                processAllTables();

                // Don't end the game while chips are in the pot (all-in showdown in
                // progress). isOnePlayerLeft() returns true as soon as one player's stack
                // hits 0, but the hand must play out so the pot can be awarded normally.
                if (tournament.isGameOver() && !hasActivePot()) {
                    handleGameOver();
                    break;
                }

                // No sleep - run full speed like HeadlessGameRunnerTest
                // The engine handles its own timing internally
            }
        } catch (Exception e) {
            handleFatalError(e);
        } finally {
            running = false;
            lifecycleCallback.accept(GameLifecycleEvent.COMPLETED);
        }
    }

    /**
     * Process all tables in the tournament.
     *
     * @return true if all tables should sleep, false if any table should continue
     *         immediately
     */
    private boolean processAllTables() {
        boolean allSleep = true;

        // Process each table
        for (int i = 0; i < tournament.getNumTables(); i++) {
            GameTable table = tournament.getTable(i);

            if (table.getTableState() == TableState.GAME_OVER) {
                continue;
            }

            TableProcessResult result = engine.processTable(table, tournament, true, true);

            applyResult(table, result);
            allSleep &= result.shouldSleep();

            // Check for game over after each table is processed. Only fire when there
            // are no chips in the pot: isOnePlayerLeft() returns true as soon as one
            // player's stack hits 0 (e.g. BB posting all-in), but the hand must play
            // out through SHOWDOWN so that hand.resolve() awards the pot correctly.
            if (tournament.isGameOver() && table.getTableState() != TableState.GAME_OVER) {
                GameHand hand = table.getHoldemHand();
                if (hand instanceof ServerHand serverHand && serverHand.getPotSize() > 0) {
                    // Chips still in pot — all-in showdown in progress. Let the hand
                    // complete naturally via COMMUNITY/SHOWDOWN; do not force GAME_OVER.
                    continue;
                }
                table.setTableState(TableState.GAME_OVER);
            }
        }

        // Multi-table consolidation after processing all tables
        if (tournament.getNumTables() > 1) {
            consolidateTables();
        }

        return allSleep;
    }

    /**
     * Returns {@code true} if any active table has chips in its pot, indicating an
     * all-in showdown hand is still in progress. Used to prevent premature
     * game-over detection: {@code isOnePlayerLeft()} returns true as soon as a
     * player's stack hits zero (e.g. big-blind all-in), but the hand must run
     * through COMMUNITY/SHOWDOWN so {@code hand.resolve()} can award the pot.
     */
    private boolean hasActivePot() {
        for (int i = 0; i < tournament.getNumTables(); i++) {
            GameTable table = tournament.getTable(i);
            if (table.getTableState() == TableState.GAME_OVER) {
                continue;
            }
            GameHand hand = table.getHoldemHand();
            if (hand instanceof ServerHand serverHand && serverHand.getPotSize() > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Apply the result of table processing.
     *
     * @param table
     *            the table that was processed
     * @param result
     *            the processing result
     */
    private void applyResult(GameTable table, TableProcessResult result) {
        // 1. Apply state transitions first (like HeadlessGameRunnerTest)
        if (result.nextState() != null) {
            TableState previousState = table.getTableState();
            table.setTableState(result.nextState());

            // Publish break lifecycle events based on state transitions.
            if (result.nextState() == TableState.BREAK) {
                eventBus.publish(new GameEvent.BreakStarted(table.getNumber()));
                // Offer addons to all active players at the configured addon break level
                if (addonOfferCallback != null && tournament instanceof ServerTournamentContext stc
                        && stc.isAllowAddons() && stc.getAddonLevel() >= 0 && stc.getLevel() == stc.getAddonLevel()) {
                    for (int s = 0; s < table.getSeats(); s++) {
                        ServerPlayer player = (ServerPlayer) table.getPlayer(s);
                        if (player != null && !player.isSittingOut()) {
                            boolean accepted = addonOfferCallback.test(player.getID(), table.getNumber());
                            if (accepted) {
                                player.addChips(stc.getAddonChips());
                                eventBus.publish(new GameEvent.PlayerAddon(table.getNumber(), player.getID(),
                                        stc.getAddonChips()));
                            }
                        }
                    }
                }
            } else if (result.nextState() == TableState.NEW_LEVEL_CHECK && previousState == TableState.BREAK) {
                eventBus.publish(new GameEvent.BreakEnded(table.getNumber()));
            }

            // Sleep between hands at the universal DONE→BEGIN transition.
            // handleDone() always returns nextState(BEGIN) after every showdown.
            // When isAutoDeal()=true (all online games), handleBegin() goes directly to
            // START_HAND, bypassing WaitForDeal and TD.CheckEndHand entirely — so this
            // is the only reliable hook for the inter-hand pause.
            if (result.nextState() == TableState.BEGIN && tournament instanceof ServerTournamentContext stc) {
                // Always track hands for blind level advancement (shared counter across
                // tables).
                stc.incrementHandsPlayed();
                // HANDS-mode level advancement: the engine's normal mechanism (BREAK state)
                // requires break levels, which server tests don't use. Advance the level here
                // instead. TIME-mode games use the engine's BREAK mechanism — skip to avoid
                // conflicting with it. Guard against advancing past the last configured level,
                // which would make getSmallBlind/getBigBlind/getAnte return 0 (no forced
                // bets), causing an infinite game loop.
                if (stc.getLevelAdvanceMode() == LevelAdvanceMode.HANDS && tournament.isLevelExpired()
                        && tournament.getLevel() < stc.getNumLevels() - 1) {
                    tournament.nextLevel();
                    eventBus.publish(new GameEvent.LevelChanged(table.getNumber(), tournament.getLevel()));
                }
                // CLEAN phase: for single-table only. Multi-table relies on
                // consolidateTables() which uses chip counts — marking 0-chip players as
                // sittingOut there would produce empty playerOrder lists and hang.
                if (tournament.getNumTables() == 1) {
                    eliminateZeroChipPlayers(table);
                }
                if (!tournament.isGameOver() && properties.aiActionDelayMs() > 0) {
                    sleepMillis(properties.aiActionDelayMs());
                }
            }
        }

        // 2. Handle "phases" server-side (no Swing UI)
        // Phases may override the state based on phase type and pendingState
        if (result.phaseToRun() != null) {
            // When DealDisplayHand fires, cards have been dealt and a new hand is starting.
            // Publish HandStarted so the broadcaster can push GAME_STATE snapshots to
            // clients before HAND_STARTED, giving them table context before any
            // ACTION_REQUIRED arrives.
            if ("TD.DealDisplayHand".equals(result.phaseToRun())) {
                eventBus.publish(new GameEvent.HandStarted(table.getNumber(), table.getHandNum()));
                // Publish PlayerActed events for antes and blinds so clients see chip
                // deductions before the first ACTION_REQUIRED arrives.
                GameHand rawHand = table.getHoldemHand();
                if (rawHand instanceof ServerHand hand) {
                    int anteAmt = hand.getAnteAmount();
                    if (anteAmt > 0) {
                        for (int seat = 0; seat < table.getSeats(); seat++) {
                            ServerPlayer player = (ServerPlayer) table.getPlayer(seat);
                            if (player != null && !player.isSittingOut()) {
                                eventBus.publish(new GameEvent.PlayerActed(table.getNumber(), player.getID(),
                                        ActionType.ANTE, anteAmt));
                            }
                        }
                    }
                    int sbSeat = hand.getSmallBlindSeat();
                    int bbSeat = hand.getBigBlindSeat();
                    ServerPlayer sbPlayer = (ServerPlayer) table.getPlayer(sbSeat);
                    ServerPlayer bbPlayer = (ServerPlayer) table.getPlayer(bbSeat);
                    if (sbPlayer != null) {
                        eventBus.publish(new GameEvent.PlayerActed(table.getNumber(), sbPlayer.getID(),
                                ActionType.BLIND_SM, hand.getActualSmallBlindPosted()));
                    }
                    if (bbPlayer != null) {
                        eventBus.publish(new GameEvent.PlayerActed(table.getNumber(), bbPlayer.getID(),
                                ActionType.BLIND_BIG, hand.getActualBigBlindPosted()));
                    }
                }
            } else if ("TD.DealCommunity".equals(result.phaseToRun())) {
                // Community cards are already in the hand at this point; publish so the
                // broadcaster can push COMMUNITY_CARDS_DEALT to clients.
                GameHand hand = table.getHoldemHand();
                if (hand != null) {
                    eventBus.publish(new GameEvent.CommunityCardsDealt(table.getNumber(), hand.getRound()));
                }
            } else if ("TD.Showdown".equals(result.phaseToRun())) {
                // hand.resolve() was already called in TournamentEngine.handleShowdown().
                // Publish ShowdownStarted and one PotAwarded per pot, then HandCompleted
                // after handleServerPhase() sets the state to DONE.
                eventBus.publish(new GameEvent.ShowdownStarted(table.getNumber()));
                GameHand hand = table.getHoldemHand();
                if (hand instanceof ServerHand serverHand) {
                    List<ServerHand.PotResolutionResult> potResults = serverHand.getResolutionResults();
                    for (int pi = 0; pi < potResults.size(); pi++) {
                        ServerHand.PotResolutionResult pr = potResults.get(pi);
                        eventBus.publish(new GameEvent.PotAwarded(table.getNumber(), pi, pr.winnerIds(), pr.amount()));
                    }
                }
            }
            handleServerPhase(table, result.phaseToRun(), result.pendingState());
            // HandCompleted fires after the state is set to DONE (only TD.Showdown
            // uses pendingState=DONE, so this is safe to key on).
            if (TableState.DONE.equals(result.pendingState())) {
                eventBus.publish(new GameEvent.HandCompleted(table.getNumber()));
            }
        }

        // 3. Broadcast state to connected clients (via GameInstance callback)
        if (table instanceof ServerGameTable serverTable) {
            eventBus.broadcastTableState(serverTable);
        }
    }

    /**
     * CLEAN phase: mark players with 0 chips as sitting-out so they are excluded
     * from the next hand's player order. Only called for single-table tournaments.
     *
     * <p>
     * This mirrors the original TournamentDirector's CLEAN state which removed
     * busted players between hands before starting the next deal.
     *
     * <p>
     * Multi-table tournaments must NOT use this — marking all 0-chip players as
     * sitting-out on a table with all busted players would produce an empty
     * playerOrder on the next deal attempt, causing an infinite loop.
     * {@link #consolidateTables()} handles multi-table elimination using chip
     * counts instead.
     *
     * @param table
     *            the table whose players to check
     */
    private void eliminateZeroChipPlayers(GameTable table) {
        // Count active survivors (chips > 0) to derive finish position.
        int survivors = 0;
        for (int seat = 0; seat < table.getSeats(); seat++) {
            ServerPlayer player = (ServerPlayer) table.getPlayer(seat);
            if (player != null && !player.isSittingOut() && player.getChipCount() > 0) {
                survivors++;
            }
        }
        for (int seat = 0; seat < table.getSeats(); seat++) {
            ServerPlayer player = (ServerPlayer) table.getPlayer(seat);
            if (player != null && player.getChipCount() == 0 && !player.isSittingOut()) {
                // Offer rebuy if in the rebuy period and callback is wired
                if (rebuyOfferCallback != null && tournament instanceof ServerTournamentContext stc
                        && stc.isRebuyPeriodActive(player)) {
                    boolean accepted = rebuyOfferCallback.test(player.getID(), table.getNumber());
                    if (accepted) {
                        player.addChips(stc.getRebuyChips());
                        player.incrementRebuys();
                        eventBus.publish(
                                new GameEvent.PlayerRebuy(table.getNumber(), player.getID(), stc.getRebuyChips()));
                        continue; // Player stays in the tournament
                    }
                }
                player.setSittingOut(true);
                logger.debug("[CLEAN] eliminated player={} seat={} (0 chips)", player.getName(), seat);
                // finishPosition = survivors + 1; if multiple players bust in the same
                // hand they share the same position (standard tournament convention).
                player.setFinishPosition(survivors + 1);
                eventBus.publish(new GameEvent.PlayerEliminated(table.getNumber(), player.getID(), survivors + 1));
            }
        }
    }

    /**
     * Handle server-side phase execution. Phases are either auto-completed (no UI
     * needed), converted to WebSocket messages (client handles display), or timed
     * delays (simulate animation time for client rendering).
     *
     * @param table
     *            the table whose phase is running
     * @param phase
     *            the phase name from TournamentEngine
     * @param pendingState
     *            the pending state to transition to after phase
     */
    private void handleServerPhase(GameTable table, String phase, TableState pendingState) {
        switch (phase) {
            case "TD.WaitForDeal" :
                // Auto-deal: no manual button press needed on server
                table.setTableState(TableState.CHECK_END_HAND);
                break;

            case "ShowTournamentTable" :
            case "TournamentShowdown" :
                // Client renders these — just broadcast state and apply pending
                if (pendingState != null) {
                    table.setTableState(pendingState);
                }
                break;

            default :
                // Unknown phase — treat as auto-complete
                if (pendingState != null) {
                    table.setTableState(pendingState);
                }
                break;
        }
    }

    /**
     * Consolidate tables in multi-table tournaments. Moves players from tables with
     * too few players to other active tables.
     *
     * <p>
     * This is a simplified consolidation algorithm based on HeadlessGameRunnerTest.
     * For production, Option A (extract OtherTables.consolidateTables() to
     * pokergamecore) is recommended.
     */
    private void consolidateTables() {
        for (int i = 0; i < tournament.getNumTables(); i++) {
            GameTable sourceTable = tournament.getTable(i);

            if (sourceTable.getTableState() == TableState.GAME_OVER) {
                continue;
            }

            // Count players with chips at this table
            int playersWithChips = 0;
            for (int seat = 0; seat < sourceTable.getSeats(); seat++) {
                if (sourceTable.getPlayer(seat) != null && sourceTable.getPlayer(seat).getChipCount() > 0) {
                    playersWithChips++;
                }
            }

            // If only 1 player left at this table, consolidate
            if (playersWithChips == 1) {
                // Find a target table with 2+ players
                GameTable targetTable = findConsolidationTarget(i);

                if (targetTable != null && sourceTable instanceof ServerGameTable
                        && targetTable instanceof ServerGameTable) {
                    consolidateTable((ServerGameTable) sourceTable, (ServerGameTable) targetTable);
                }
            }

            // If no players with chips, mark table as game over — but ONLY when
            // the pot is empty. If chips are in the pot the chip-holder went all-in
            // during this hand: the engine must resolve the hand first so chips
            // return to the winner's stack. Marking GAME_OVER now would abandon
            // those chips, making isOnePlayerLeft() never return true.
            if (playersWithChips == 0) {
                int potSize = 0;
                GameHand potHand = sourceTable.getHoldemHand();
                if (potHand instanceof ServerHand sh) {
                    potSize = sh.getPotSize();
                }
                if (potSize == 0) {
                    sourceTable.setTableState(TableState.GAME_OVER);
                }
            }
        }
    }

    /**
     * Find a target table for consolidation.
     *
     * @param excludeTableIndex
     *            the table index to exclude (the source table)
     * @return target table with 2+ players, or null if none found
     */
    private GameTable findConsolidationTarget(int excludeTableIndex) {
        for (int i = 0; i < tournament.getNumTables(); i++) {
            if (i == excludeTableIndex) {
                continue;
            }

            GameTable table = tournament.getTable(i);
            if (table.getTableState() == TableState.GAME_OVER) {
                continue;
            }

            int playersWithChips = 0;
            for (int seat = 0; seat < table.getSeats(); seat++) {
                if (table.getPlayer(seat) != null && table.getPlayer(seat).getChipCount() > 0) {
                    playersWithChips++;
                }
            }

            if (playersWithChips >= 2) {
                return table;
            }
        }

        // If no table with 2+ players, return any active table
        for (int i = 0; i < tournament.getNumTables(); i++) {
            if (i == excludeTableIndex) {
                continue;
            }

            GameTable table = tournament.getTable(i);
            if (table.getTableState() != TableState.GAME_OVER) {
                return table;
            }
        }

        return null;
    }

    /**
     * Consolidate a source table into a target table by moving the remaining
     * player.
     *
     * @param sourceTable
     *            the table to consolidate from
     * @param targetTable
     *            the table to consolidate to
     */
    private void consolidateTable(ServerGameTable sourceTable, ServerGameTable targetTable) {
        // Find the remaining player
        ServerPlayer playerToMove = null;
        int sourceSeat = -1;
        for (int seat = 0; seat < sourceTable.getSeats(); seat++) {
            ServerPlayer player = (ServerPlayer) sourceTable.getPlayer(seat);
            if (player != null && player.getChipCount() > 0) {
                playerToMove = player;
                sourceSeat = seat;
                break;
            }
        }

        if (playerToMove == null) {
            return;
        }

        // Award any pot chips to the player being moved
        GameHand hand = sourceTable.getHoldemHand();
        if (hand instanceof ServerHand currentHand && currentHand.getPotSize() > 0) {
            playerToMove.setChipCount(playerToMove.getChipCount() + currentHand.getPotSize());
        }

        // Remove player from source table
        sourceTable.removePlayer(sourceSeat);
        eventBus.publish(new GameEvent.PlayerRemoved(sourceTable.getNumber(), playerToMove.getID(), sourceSeat));

        // Move player to target table (find empty seat)
        for (int seat = 0; seat < targetTable.getSeats(); seat++) {
            if (targetTable.getPlayer(seat) == null) {
                targetTable.addPlayer(playerToMove, seat);
                eventBus.publish(new GameEvent.PlayerAdded(targetTable.getNumber(), playerToMove.getID(), seat));
                break;
            }
        }

        // Mark source table as game over
        sourceTable.setTableState(TableState.GAME_OVER);
    }

    /**
     * Handle game over condition. Marks all tables GAME_OVER and publishes
     * {@link GameEvent.TournamentCompleted} so that {@link GameEventBroadcaster}
     * sends {@code GAME_COMPLETE} to connected clients.
     *
     * <p>
     * This is the single authoritative exit point — covers both the normal path
     * (last player eliminated at DONE→BEGIN) and the mid-hand fast path (all
     * remaining players go all-in during a hand).
     */
    private void handleGameOver() {
        // Publish PlayerEliminated for any players still active with 0 chips.
        // The main loop exits as soon as isGameOver() returns true, which can happen
        // before processTable(DONE→BEGIN) calls eliminateZeroChipPlayers. The engine
        // may also transition directly to GAME_OVER when the last opponent busts, so
        // table state cannot be used as a guard. Always calling is safe: already-
        // eliminated players are skipped (isSittingOut==true), and the tournament
        // winner (chips > 0) is skipped by the chip check in eliminateZeroChipPlayers.
        for (int i = 0; i < tournament.getNumTables(); i++) {
            eliminateZeroChipPlayers(tournament.getTable(i));
        }

        // All tables should be marked as GAME_OVER
        for (int i = 0; i < tournament.getNumTables(); i++) {
            GameTable table = tournament.getTable(i);
            if (table.getTableState() != TableState.GAME_OVER) {
                table.setTableState(TableState.GAME_OVER);
            }
        }

        // Find the winner (only player with chips remaining) and publish GAME_COMPLETE
        int winnerId = -1;
        if (tournament instanceof ServerTournamentContext stc) {
            // Primary: player with chips > 0 in their stack
            for (ServerPlayer player : stc.getAllPlayers()) {
                if (player.getChipCount() > 0) {
                    winnerId = player.getID();
                    break;
                }
            }
            // Fallback: all players went all-in (0 chips each, all in pot).
            // Find the player who is neither sittingOut nor folded — that is the
            // last standing player whose chips are temporarily in the pot.
            if (winnerId == -1) {
                for (int i = 0; i < tournament.getNumTables(); i++) {
                    GameTable table = tournament.getTable(i);
                    for (int seat = 0; seat < table.getSeats(); seat++) {
                        ServerPlayer player = (ServerPlayer) table.getPlayer(seat);
                        if (player != null && !player.isSittingOut() && !player.isFolded()) {
                            winnerId = player.getID();
                            break;
                        }
                    }
                    if (winnerId != -1)
                        break;
                }
            }
        }
        logger.debug("[handleGameOver] tournament complete, winnerId={}", winnerId);
        eventBus.publish(new GameEvent.TournamentCompleted(winnerId));

        running = false;
    }

    /**
     * Handle a fatal error.
     *
     * @param e
     *            the exception that occurred
     */
    private void handleFatalError(Exception e) {
        lifecycleCallback.accept(GameLifecycleEvent.ERROR);
        running = false;
        logger.error("Fatal error in ServerTournamentDirector", e);
    }

    /**
     * Pause the tournament. The game loop will stop processing until resume() is
     * called.
     */
    public void pause() {
        this.paused = true;
        lifecycleCallback.accept(GameLifecycleEvent.PAUSED);
    }

    /**
     * Resume the tournament after pause.
     */
    public void resume() {
        this.paused = false;
        lifecycleCallback.accept(GameLifecycleEvent.RESUMED);
    }

    /**
     * Request shutdown. The game loop will complete the current iteration and then
     * exit cleanly.
     */
    public void shutdown() {
        this.shutdownRequested = true;
    }

    /**
     * Check if the director is currently running.
     *
     * @return true if the game loop is active
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Check if the director is currently paused.
     *
     * @return true if paused
     */
    public boolean isPaused() {
        return paused;
    }

    /**
     * Sleep for the specified number of milliseconds, ignoring interrupts. The
     * interrupt flag is cleared (not re-set) so that a single interrupt does not
     * cascade and skip all future sleeps.
     *
     * @param millis
     *            milliseconds to sleep
     */
    private void sleepMillis(int millis) {
        // Clear any pending interrupt before sleeping so it does not cause the
        // sleep to return immediately without waiting.
        Thread.interrupted();
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // Ignore — this method is documented as "ignoring interrupts"
        }
    }
}
