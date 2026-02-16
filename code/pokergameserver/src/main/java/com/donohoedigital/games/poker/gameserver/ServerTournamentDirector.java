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

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.donohoedigital.games.poker.core.GameHand;
import com.donohoedigital.games.poker.core.GameTable;
import com.donohoedigital.games.poker.core.TableProcessResult;
import com.donohoedigital.games.poker.core.TournamentContext;
import com.donohoedigital.games.poker.core.TournamentEngine;
import com.donohoedigital.games.poker.core.state.TableState;

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

    private volatile boolean running;
    private volatile boolean paused;
    private volatile boolean shutdownRequested;

    /**
     * Create a new server tournament director.
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
     */
    public ServerTournamentDirector(TournamentEngine engine, TournamentContext tournament, ServerGameEventBus eventBus,
            ServerPlayerActionProvider actionProvider, GameServerProperties properties,
            Consumer<GameLifecycleEvent> lifecycleCallback) {
        this.engine = engine;
        this.tournament = tournament;
        this.eventBus = eventBus;
        this.actionProvider = actionProvider;
        this.properties = properties;
        this.lifecycleCallback = lifecycleCallback;
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

                if (tournament.isGameOver()) {
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

            // Check for game over (since we auto-complete phases that would normally set
            // this)
            // This is critical to ensure the game ends when only one player remains
            if (tournament.isGameOver() && table.getTableState() != TableState.GAME_OVER) {
                // Award any remaining pot chips to the winner before ending
                GameHand hand = table.getHoldemHand();
                if (hand instanceof ServerHand serverHand && serverHand.getPotSize() > 0) {
                    // Find the last player with chips and award pot
                    for (int seat = 0; seat < table.getSeats(); seat++) {
                        if (table.getPlayer(seat) != null && table.getPlayer(seat).getChipCount() > 0) {
                            ServerPlayer winner = (ServerPlayer) table.getPlayer(seat);
                            winner.setChipCount(winner.getChipCount() + serverHand.getPotSize());
                            break;
                        }
                    }
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
            table.setTableState(result.nextState());

            // Increment hands played when hand completes (transitions to CLEAN state)
            if (result.nextState() == TableState.CLEAN && tournament instanceof ServerTournamentContext) {
                ((ServerTournamentContext) tournament).incrementHandsPlayed();
            }
        }

        // 2. Handle "phases" server-side (no Swing UI)
        // Phases may override the state based on phase type and pendingState
        if (result.phaseToRun() != null) {
            handleServerPhase(table, result.phaseToRun(), result.pendingState());
        }

        // 3. Broadcast state to connected clients (via GameInstance callback)
        if (table instanceof ServerGameTable serverTable) {
            eventBus.broadcastTableState(serverTable);
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

            // If no players with chips, mark table as game over
            if (playersWithChips == 0) {
                sourceTable.setTableState(TableState.GAME_OVER);
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

        // Move player to target table (find empty seat)
        for (int seat = 0; seat < targetTable.getSeats(); seat++) {
            if (targetTable.getPlayer(seat) == null) {
                targetTable.addPlayer(playerToMove, seat);
                break;
            }
        }

        // Mark source table as game over
        sourceTable.setTableState(TableState.GAME_OVER);
    }

    /**
     * Handle game over condition.
     */
    private void handleGameOver() {
        // All tables should be marked as GAME_OVER
        for (int i = 0; i < tournament.getNumTables(); i++) {
            GameTable table = tournament.getTable(i);
            if (table.getTableState() != TableState.GAME_OVER) {
                table.setTableState(TableState.GAME_OVER);
            }
        }

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
     * Sleep for the specified number of milliseconds, ignoring interrupts.
     *
     * @param millis
     *            milliseconds to sleep
     */
    private void sleepMillis(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
