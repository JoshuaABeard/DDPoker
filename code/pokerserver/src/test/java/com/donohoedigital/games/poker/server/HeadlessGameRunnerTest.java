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
import com.donohoedigital.games.poker.core.PlayerActionProvider;
import com.donohoedigital.games.poker.core.TableProcessResult;
import com.donohoedigital.games.poker.core.TournamentContext;
import com.donohoedigital.games.poker.core.TournamentEngine;
import com.donohoedigital.games.poker.engine.event.*;
import com.donohoedigital.games.poker.engine.GamePlayerInfo;
import com.donohoedigital.games.poker.engine.PlayerAction;
import com.donohoedigital.games.poker.engine.state.*;
import com.donohoedigital.games.poker.engine.Card;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Phase 5: Server Integration Proof-of-Concept
 *
 * Demonstrates that pokerserver can depend on pokergamecore with zero Swing
 * dependencies. This test verifies:
 *
 * 1. pokerserver can create TournamentEngine and core interfaces 2. No Swing
 * classes are required at runtime 3. Basic headless implementations work
 * without GUI
 *
 * This proves pokergamecore is completely Swing-free and ready for
 * server-hosted games.
 *
 * Note: Full game execution requires substantial game logic (hand evaluation,
 * betting rounds, showdown, etc.) which is beyond the scope of this
 * proof-of-concept.
 */
class HeadlessGameRunnerTest {

    /**
     * Creates a tournament-aware AI that recognizes stack pressure and makes
     * intelligent decisions based on M-ratio (chip stack relative to blinds).
     *
     * This AI serves as a proof-of-concept for Phase 7: AI Extraction,
     * demonstrating that intelligent poker AI can work without Swing dependencies.
     *
     * Strategy: - Critical zone (M < 5): Push/fold - go all-in or fold - Danger
     * zone (5 <= M < 10): Aggressive - frequent raises - Comfortable zone (M >=
     * 10): Balanced - normal play with some aggression
     *
     * @param seed
     *            Random seed for reproducibility
     * @param tournament
     *            Tournament context for reading blind levels
     * @return PlayerActionProvider with tournament-aware decision making
     */
    private static PlayerActionProvider createTournamentAI(long seed, TournamentContext tournament) {
        java.util.Random random = new java.util.Random(seed);

        return (player, options) -> {
            // Calculate M-ratio: stack / cost per orbit
            // Use tournament context to get accurate blind structure
            int stack = player.getChipCount();
            int level = tournament.getLevel();
            int smallBlind = tournament.getSmallBlind(level);
            int bigBlind = tournament.getBigBlind(level);
            int ante = tournament.getAnte(level);
            // Standard 10-handed cost per orbit: SB + BB + (10 * ante)
            int costPerOrbit = smallBlind + bigBlind + (10 * ante);
            double mRatio = stack / (double) Math.max(1, costPerOrbit);

            // CRITICAL ZONE (M < 5): Push or fold
            if (mRatio < 5.0) {
                // Short-stacked desperation - need to take risks
                if (options.canBet() || options.canRaise()) {
                    // 70% chance to go all-in, 30% fold
                    if (random.nextInt(100) < 70) {
                        int allIn = Math.min(stack, options.maxBet() > 0 ? options.maxBet() : options.maxRaise());
                        return options.canBet() ? PlayerAction.bet(allIn) : PlayerAction.raise(allIn);
                    } else {
                        return PlayerAction.fold();
                    }
                }
                // Call if we can (pot odds might be good)
                if (options.canCall()) {
                    return random.nextInt(100) < 60 ? PlayerAction.call() : PlayerAction.fold();
                }
                if (options.canCheck())
                    return PlayerAction.check();
                return PlayerAction.fold();
            }

            // DANGER ZONE (5 <= M < 10): Aggressive play
            if (mRatio < 10.0) {
                // Moderate pressure - play aggressive to accumulate chips
                if (options.canRaise() && random.nextInt(100) < 50) {
                    int raiseAmount = options.minRaise()
                            + random.nextInt(Math.max(1, (options.maxRaise() - options.minRaise()) / 3 + 1));
                    return PlayerAction.raise(Math.min(raiseAmount, stack));
                }
                if (options.canBet() && random.nextInt(100) < 50) {
                    int betAmount = options.minBet()
                            + random.nextInt(Math.max(1, (options.maxBet() - options.minBet()) / 3 + 1));
                    return PlayerAction.bet(Math.min(betAmount, stack));
                }
                if (options.canCall() && random.nextInt(100) < 60) {
                    return PlayerAction.call();
                }
                if (options.canCheck())
                    return PlayerAction.check();
                return PlayerAction.fold();
            }

            // COMFORTABLE ZONE (M >= 10): Balanced play with moderate aggression
            java.util.List<PlayerAction> availableActions = new java.util.ArrayList<>();

            // Build weighted action list (some actions added multiple times for
            // probability)
            if (options.canCheck()) {
                availableActions.add(PlayerAction.check());
                availableActions.add(PlayerAction.check()); // 2x weight for checking
            }
            if (options.canCall()) {
                availableActions.add(PlayerAction.call());
            }
            if (options.canBet()) {
                int betAmount = options.minBet()
                        + random.nextInt(Math.max(1, (options.maxBet() - options.minBet()) / 2 + 1));
                availableActions.add(PlayerAction.bet(Math.min(betAmount, stack)));
            }
            if (options.canRaise()) {
                int raiseAmount = options.minRaise()
                        + random.nextInt(Math.max(1, (options.maxRaise() - options.minRaise()) / 2 + 1));
                availableActions.add(PlayerAction.raise(Math.min(raiseAmount, stack)));
            }
            if (options.canFold()) {
                availableActions.add(PlayerAction.fold());
            }

            return availableActions.isEmpty()
                    ? PlayerAction.fold()
                    : availableActions.get(random.nextInt(availableActions.size()));
        };
    }

    /**
     * Verify pokergamecore components can be instantiated and used in headless mode
     * without Swing dependencies.
     */
    @Test
    void canCreateHeadlessComponents() {
        // 1. Create headless game components (no Swing dependencies)
        HeadlessTournamentContext tournament = new HeadlessTournamentContext(2);
        HeadlessGameTable table = (HeadlessGameTable) tournament.getTable(0);
        GameEventBus eventBus = new GameEventBus();
        PlayerActionProvider aiProvider = (player, options) -> PlayerAction.fold();

        // 2. Verify TournamentEngine can be created
        TournamentEngine engine = new TournamentEngine(eventBus, aiProvider);
        assertThat(engine).isNotNull();

        // 3. Verify event bus works
        List<String> events = new ArrayList<>();
        eventBus.subscribe(event -> events.add(event.getClass().getSimpleName()));
        eventBus.publish(new GameEvent.TableStateChanged(0, TableState.BEGIN, TableState.BEGIN_WAIT));
        assertThat(events).containsExactly("TableStateChanged");

        // 4. Verify tournament context provides expected values
        assertThat(tournament.getNumTables()).isEqualTo(1);
        assertThat(tournament.getNumPlayers()).isEqualTo(2);
        assertThat(tournament.isPractice()).isTrue();
        assertThat(tournament.isOnlineGame()).isFalse();

        // 5. Verify table provides players
        assertThat(table.getSeats()).isEqualTo(10);
        assertThat(table.getNumOccupiedSeats()).isEqualTo(2);
        assertThat(table.getPlayer(0)).isNotNull();
        assertThat(table.getPlayer(1)).isNotNull();

        // 6. Verify player implementations work
        HeadlessPlayer player1 = (HeadlessPlayer) table.getPlayer(0);
        assertThat(player1.getName()).isEqualTo("Player1");
        assertThat(player1.getChipCount()).isEqualTo(1500);
        assertThat(player1.isComputer()).isTrue();
        assertThat(player1.isHuman()).isFalse();

        // 7. Verify engine can process initial state
        TableProcessResult result = engine.processTable(table, tournament, true, false);
        assertThat(result).isNotNull();
        assertThat(result.nextState()).isNotNull();

        // 8. Verify PlayerAction records work
        PlayerAction fold = PlayerAction.fold();
        assertThat(fold.actionType()).isEqualTo(ActionType.FOLD);

        PlayerAction bet = PlayerAction.bet(100);
        assertThat(bet.actionType()).isEqualTo(ActionType.BET);
        assertThat(bet.amount()).isEqualTo(100);

        System.out.println("[OK]pokergamecore components created successfully in headless mode");
        System.out.println("[OK]TournamentEngine initialized with " + tournament.getNumPlayers() + " players");
        System.out.println("[OK]Event bus functional (received " + events.size() + " event)");
        System.out.println("[OK]No Swing dependencies required");
    }

    /**
     * Run a complete 10-player full table tournament headless. Tests that
     * pokergamecore can handle a realistic full table game.
     */
    @Test
    void runFullTableTournament() {
        // Create headless game components with full 10-player table
        HeadlessTournamentContext tournament = new HeadlessTournamentContext(10);
        HeadlessGameTable table = (HeadlessGameTable) tournament.getTable(0);
        GameEventBus eventBus = new GameEventBus();

        // Randomized AI that uses all poker actions
        java.util.Random random = new java.util.Random(42); // Fixed seed for reproducibility
        PlayerActionProvider aiProvider = (player, options) -> {
            // Randomly choose an available action
            java.util.List<PlayerAction> availableActions = new java.util.ArrayList<>();

            if (options.canFold())
                availableActions.add(PlayerAction.fold());
            if (options.canCheck())
                availableActions.add(PlayerAction.check());
            if (options.canCall())
                availableActions.add(PlayerAction.call());
            if (options.canBet()) {
                // Random bet amount between min and half of max
                int betAmount = options.minBet()
                        + random.nextInt(Math.max(1, (options.maxBet() - options.minBet()) / 2 + 1));
                availableActions.add(PlayerAction.bet(betAmount));
            }
            if (options.canRaise()) {
                // Random raise amount between min and half of max
                int raiseAmount = options.minRaise()
                        + random.nextInt(Math.max(1, (options.maxRaise() - options.minRaise()) / 2 + 1));
                availableActions.add(PlayerAction.raise(raiseAmount));
            }

            // Pick a random action from available options
            return availableActions.isEmpty()
                    ? PlayerAction.fold()
                    : availableActions.get(random.nextInt(availableActions.size()));
        };

        TournamentEngine engine = new TournamentEngine(eventBus, aiProvider);

        // Track game events
        List<String> events = new ArrayList<>();
        eventBus.subscribe(event -> events.add(event.getClass().getSimpleName()));

        // Run game loop until tournament completes
        int maxIterations = 10000; // Higher limit for 10 players (more hands needed)
        int iterations = 0;

        while (!table.tableState.equals(TableState.GAME_OVER) && iterations < maxIterations) {
            TableProcessResult result = engine.processTable(table, tournament, true, false);

            // Debug output every 50 iterations (first 100) then every 500
            if (iterations < 100 && iterations % 50 == 0 || iterations % 500 == 0) {
                long playersLeft = tournament.players.stream().filter(p -> p.chipCount > 0).count();
                System.out.println("Iteration " + iterations + ": state=" + table.tableState + ", players remaining="
                        + playersLeft);
            }

            // Handle result
            if (result.nextState() != null) {
                table.tableState = result.nextState();
            }

            // Check for game over (since we skip phases that would normally set this)
            if (tournament.isGameOver() && !table.tableState.equals(TableState.GAME_OVER)) {
                // Award any remaining pot chips to the winner before ending
                if (table.currentHand != null && table.currentHand.pot > 0) {
                    HeadlessPlayer winner = tournament.players.stream().filter(p -> p.chipCount > 0).findFirst()
                            .orElse(null);
                    if (winner != null) {
                        winner.chipCount += table.currentHand.pot;
                        table.currentHand.pot = 0;
                    }
                }
                table.tableState = TableState.GAME_OVER;
            }

            // Simulate phase execution (would normally run Swing phases)
            if (result.phaseToRun() != null) {
                // In headless mode, auto-execute phases by transitioning to pending state
                String phase = result.phaseToRun();
                if ("TD.WaitForDeal".equals(phase)) {
                    // Simulate Deal button press
                    table.tableState = TableState.CHECK_END_HAND;
                } else if (result.pendingState() != null) {
                    // For all other phases, just transition to pending state
                    // This simulates the phase running and completing
                    table.tableState = result.pendingState();
                }
            }

            iterations++;
        }

        // Verify tournament completed
        assertThat(iterations).isLessThan(maxIterations).as("Tournament should complete");
        assertThat(table.tableState).isEqualTo(TableState.GAME_OVER);

        // Verify we got game events
        assertThat(events).isNotEmpty();

        // Verify there's exactly one winner
        List<HeadlessPlayer> winners = tournament.players.stream().filter(p -> p.chipCount > 0)
                .collect(java.util.stream.Collectors.toList());

        assertThat(winners).hasSize(1);
        HeadlessPlayer winner = winners.get(0);
        // Winner should have ALL chips - total must be conserved
        assertThat(winner.chipCount).isEqualTo(15000); // 10 players * 1500 chips

        System.out.println("[OK]Full table tournament completed in " + iterations + " iterations");
        System.out.println("[OK]Winner: " + winner.name + " with " + winner.chipCount + " chips");
        System.out.println("[OK]Events fired: " + events.size());
    }

    /**
     * Run a multi-table tournament with 18 players across 3 tables (6 per table).
     * Tests concurrent table processing.
     */
    @Test
    void runMultiTableTournament() {
        // Create tournament with 18 players across 3 tables (6 per table)
        int numPlayers = 18;
        int numTables = 3;

        // Create all players
        List<HeadlessPlayer> allPlayers = new ArrayList<>();
        for (int i = 0; i < numPlayers; i++) {
            allPlayers.add(new HeadlessPlayer(i + 1, "Player" + (i + 1), 1500));
        }

        // Create tables and distribute players
        List<HeadlessGameTable> tables = new ArrayList<>();
        int playersPerTable = numPlayers / numTables;
        for (int t = 0; t < numTables; t++) {
            List<HeadlessPlayer> tablePlayers = new ArrayList<>();
            for (int p = 0; p < playersPerTable; p++) {
                tablePlayers.add(allPlayers.get(t * playersPerTable + p));
            }
            tables.add(new HeadlessGameTable(tablePlayers));
        }

        // Create multi-table context
        MultiTableContext tournament = new MultiTableContext(allPlayers, tables);
        GameEventBus eventBus = new GameEventBus();

        // Randomized AI
        java.util.Random random = new java.util.Random(123);
        PlayerActionProvider aiProvider = (player, options) -> {
            java.util.List<PlayerAction> availableActions = new java.util.ArrayList<>();
            if (options.canFold())
                availableActions.add(PlayerAction.fold());
            if (options.canCheck())
                availableActions.add(PlayerAction.check());
            if (options.canCall())
                availableActions.add(PlayerAction.call());
            if (options.canBet()) {
                int betAmount = options.minBet()
                        + random.nextInt(Math.max(1, (options.maxBet() - options.minBet()) / 2 + 1));
                availableActions.add(PlayerAction.bet(betAmount));
            }
            if (options.canRaise()) {
                int raiseAmount = options.minRaise()
                        + random.nextInt(Math.max(1, (options.maxRaise() - options.minRaise()) / 2 + 1));
                availableActions.add(PlayerAction.raise(raiseAmount));
            }
            return availableActions.isEmpty()
                    ? PlayerAction.fold()
                    : availableActions.get(random.nextInt(availableActions.size()));
        };

        TournamentEngine engine = new TournamentEngine(eventBus, aiProvider);

        int maxIterations = 50000; // Increased for multi-table tournaments that consolidate
        int iterations = 0;

        while (!tournament.isGameOver() && iterations < maxIterations) {
            // Process all tables each iteration
            for (HeadlessGameTable table : tables) {
                if (table.tableState.equals(TableState.GAME_OVER))
                    continue;

                // Skip tables with fewer than 2 players with chips
                long playersWithChipsAtTable = table.players.stream().filter(p -> p.chipCount > 0).count();
                if (playersWithChipsAtTable < 2) {
                    table.tableState = TableState.GAME_OVER;
                    continue;
                }

                TableProcessResult result = engine.processTable(table, tournament, true, false);

                if (result.nextState() != null) {
                    table.tableState = result.nextState();
                }

                if (tournament.isGameOver() && !table.tableState.equals(TableState.GAME_OVER)) {
                    if (table.currentHand != null && table.currentHand.pot > 0) {
                        HeadlessPlayer winner = allPlayers.stream().filter(p -> p.chipCount > 0).findFirst()
                                .orElse(null);
                        if (winner != null) {
                            winner.chipCount += table.currentHand.pot;
                            table.currentHand.pot = 0;
                        }
                    }
                    table.tableState = TableState.GAME_OVER;
                }

                if (result.phaseToRun() != null) {
                    String phase = result.phaseToRun();
                    if ("TD.WaitForDeal".equals(phase)) {
                        table.tableState = TableState.CHECK_END_HAND;
                    } else if (result.pendingState() != null) {
                        table.tableState = result.pendingState();
                    }
                }
            }

            // Table consolidation: move players from tables with 1 player to other tables
            for (HeadlessGameTable sourceTable : tables) {
                if (sourceTable.tableState.equals(TableState.GAME_OVER))
                    continue;

                long playersAtSource = sourceTable.players.stream().filter(p -> p.chipCount > 0).count();
                if (playersAtSource == 1) {
                    // Find a target table with 2+ players (or any active table if none available)
                    HeadlessGameTable targetTable = tables.stream()
                            .filter(t -> !t.tableState.equals(TableState.GAME_OVER)).filter(t -> t != sourceTable)
                            .filter(t -> t.players.stream().filter(p -> p.chipCount > 0).count() >= 2).findFirst()
                            .orElse(tables.stream().filter(t -> !t.tableState.equals(TableState.GAME_OVER))
                                    .filter(t -> t != sourceTable).findFirst().orElse(null));

                    if (targetTable != null) {
                        // Move the player from source to target table
                        HeadlessPlayer playerToMove = sourceTable.players.stream().filter(p -> p.chipCount > 0)
                                .findFirst().orElse(null);

                        if (playerToMove != null) {
                            // Award any pot chips to the player being moved
                            if (sourceTable.currentHand != null && sourceTable.currentHand.pot > 0) {
                                playerToMove.chipCount += sourceTable.currentHand.pot;
                                System.out.println("  Awarded " + sourceTable.currentHand.pot + " pot chips to "
                                        + playerToMove.name);
                                sourceTable.currentHand.pot = 0;
                            }

                            targetTable.players.add(playerToMove);
                            sourceTable.tableState = TableState.GAME_OVER;
                            System.out.println("Consolidated: Moved " + playerToMove.name + " ("
                                    + playerToMove.chipCount + " chips) from table to another table");
                        }
                    }
                }
            }

            if (iterations % 1000 == 0) {
                long playersLeft = allPlayers.stream().filter(p -> p.chipCount > 0).count();
                int activeTables = (int) tables.stream().filter(t -> !t.tableState.equals(TableState.GAME_OVER))
                        .count();
                System.out.println(
                        "Iteration " + iterations + ": players=" + playersLeft + ", active tables=" + activeTables);
            }

            // Check if all tables are inactive (shouldn't happen with consolidation)
            long activeTableCount = tables.stream().filter(t -> !t.tableState.equals(TableState.GAME_OVER)).count();
            if (activeTableCount == 0) {
                System.out.println("All tables inactive - ending tournament");
                break;
            }

            iterations++;
        }

        assertThat(iterations).isLessThan(maxIterations).as("Tournament should complete");

        List<HeadlessPlayer> winners = allPlayers.stream().filter(p -> p.chipCount > 0)
                .collect(java.util.stream.Collectors.toList());

        // With table consolidation, we should have exactly 1 winner
        assertThat(winners).hasSize(1).as("Should have exactly one winner with table consolidation");

        // Verify chip conservation - total chips should still be 27,000
        int totalChips = allPlayers.stream().mapToInt(p -> p.chipCount).sum();
        int totalPotChips = tables.stream().filter(t -> t.currentHand != null).mapToInt(t -> t.currentHand.pot).sum();
        assertThat(totalChips + totalPotChips).isEqualTo(27000).as("Total chips should be conserved");
        assertThat(winners.get(0).chipCount).isEqualTo(27000).as("Winner should have all chips");

        System.out.println("[OK]Multi-table tournament completed in " + iterations + " iterations");
        System.out.println("[OK]Winner: " + winners.get(0).name + " with " + winners.get(0).chipCount + " chips");
        System.out.println("[OK]Total chips: " + totalChips + ", in pots: " + totalPotChips);
    }

    /**
     * Run a large multi-table tournament with 60 players across 6 tables (10 per
     * table). Tests table consolidation at scale.
     */
    @Test
    void runLargeMultiTableTournament() {
        // Create tournament with 60 players across 6 tables (10 per table)
        // Using maximum supported chips: 1,000,000 per player
        int numPlayers = 60;
        int numTables = 6;
        int startingChips = 1000000; // Maximum supported chips

        // Create all players
        List<HeadlessPlayer> allPlayers = new ArrayList<>();
        for (int i = 0; i < numPlayers; i++) {
            allPlayers.add(new HeadlessPlayer(i + 1, "Player" + (i + 1), startingChips));
        }

        // Create tables first (without tournament context)
        List<HeadlessGameTable> tables = new ArrayList<>();
        int playersPerTable = numPlayers / numTables;
        for (int t = 0; t < numTables; t++) {
            List<HeadlessPlayer> tablePlayers = new ArrayList<>();
            for (int p = 0; p < playersPerTable; p++) {
                tablePlayers.add(allPlayers.get(t * playersPerTable + p));
            }
            tables.add(new HeadlessGameTable(tablePlayers));
        }

        // Create multi-table context and link tables
        MultiTableContext tournament = new MultiTableContext(allPlayers, tables);

        // Update tables to use the tournament context for blind progression
        for (int i = 0; i < tables.size(); i++) {
            HeadlessGameTable oldTable = tables.get(i);
            tables.set(i, new HeadlessGameTable(oldTable.players, tournament));
        }
        GameEventBus eventBus = new GameEventBus();

        // Randomized AI
        java.util.Random random = new java.util.Random(456); // Different seed for variety
        PlayerActionProvider aiProvider = (player, options) -> {
            java.util.List<PlayerAction> availableActions = new java.util.ArrayList<>();
            if (options.canFold())
                availableActions.add(PlayerAction.fold());
            if (options.canCheck())
                availableActions.add(PlayerAction.check());
            if (options.canCall())
                availableActions.add(PlayerAction.call());
            if (options.canBet()) {
                int betAmount = options.minBet()
                        + random.nextInt(Math.max(1, (options.maxBet() - options.minBet()) / 2 + 1));
                availableActions.add(PlayerAction.bet(betAmount));
            }
            if (options.canRaise()) {
                int raiseAmount = options.minRaise()
                        + random.nextInt(Math.max(1, (options.maxRaise() - options.minRaise()) / 2 + 1));
                availableActions.add(PlayerAction.raise(raiseAmount));
            }
            return availableActions.isEmpty()
                    ? PlayerAction.fold()
                    : availableActions.get(random.nextInt(availableActions.size()));
        };

        TournamentEngine engine = new TournamentEngine(eventBus, aiProvider);

        int maxIterations = 500000; // Very high limit for 100k starting chips with small blinds
        int iterations = 0;

        while (!tournament.isGameOver() && iterations < maxIterations) {
            // Process all tables each iteration
            for (HeadlessGameTable table : tables) {
                if (table.tableState.equals(TableState.GAME_OVER))
                    continue;

                // Skip tables with fewer than 2 players with chips
                long playersWithChipsAtTable = table.players.stream().filter(p -> p.chipCount > 0).count();
                if (playersWithChipsAtTable < 2) {
                    table.tableState = TableState.GAME_OVER;
                    continue;
                }

                TableProcessResult result = engine.processTable(table, tournament, true, false);

                if (result.nextState() != null) {
                    table.tableState = result.nextState();
                }

                if (tournament.isGameOver() && !table.tableState.equals(TableState.GAME_OVER)) {
                    if (table.currentHand != null && table.currentHand.pot > 0) {
                        HeadlessPlayer winner = allPlayers.stream().filter(p -> p.chipCount > 0).findFirst()
                                .orElse(null);
                        if (winner != null) {
                            winner.chipCount += table.currentHand.pot;
                            table.currentHand.pot = 0;
                        }
                    }
                    table.tableState = TableState.GAME_OVER;
                }

                if (result.phaseToRun() != null) {
                    String phase = result.phaseToRun();
                    if ("TD.WaitForDeal".equals(phase)) {
                        table.tableState = TableState.CHECK_END_HAND;
                    } else if (result.pendingState() != null) {
                        table.tableState = result.pendingState();
                    }
                }
            }

            // Table consolidation: move players from tables with 1 player to other tables
            for (HeadlessGameTable sourceTable : tables) {
                if (sourceTable.tableState.equals(TableState.GAME_OVER))
                    continue;

                long playersAtSource = sourceTable.players.stream().filter(p -> p.chipCount > 0).count();
                if (playersAtSource == 1) {
                    // Find a target table with 2+ players (or any active table if none available)
                    HeadlessGameTable targetTable = tables.stream()
                            .filter(t -> !t.tableState.equals(TableState.GAME_OVER)).filter(t -> t != sourceTable)
                            .filter(t -> t.players.stream().filter(p -> p.chipCount > 0).count() >= 2).findFirst()
                            .orElse(tables.stream().filter(t -> !t.tableState.equals(TableState.GAME_OVER))
                                    .filter(t -> t != sourceTable).findFirst().orElse(null));

                    if (targetTable != null) {
                        // Move the player from source to target table
                        HeadlessPlayer playerToMove = sourceTable.players.stream().filter(p -> p.chipCount > 0)
                                .findFirst().orElse(null);

                        if (playerToMove != null) {
                            // Award any pot chips to the player being moved
                            if (sourceTable.currentHand != null && sourceTable.currentHand.pot > 0) {
                                playerToMove.chipCount += sourceTable.currentHand.pot;
                                System.out.println("  Awarded " + sourceTable.currentHand.pot + " pot chips to "
                                        + playerToMove.name);
                                sourceTable.currentHand.pot = 0;
                            }

                            targetTable.players.add(playerToMove);
                            sourceTable.tableState = TableState.GAME_OVER;
                            System.out.println("Consolidated: Moved " + playerToMove.name + " ("
                                    + playerToMove.chipCount + " chips) from table to another table");
                        }
                    }
                }
            }

            if (iterations % 25000 == 0) {
                long playersLeft = allPlayers.stream().filter(p -> p.chipCount > 0).count();
                int activeTables = (int) tables.stream().filter(t -> !t.tableState.equals(TableState.GAME_OVER))
                        .count();
                System.out.println(
                        "Iteration " + iterations + ": players=" + playersLeft + ", active tables=" + activeTables);
            }

            // Check if all tables are inactive (shouldn't happen with consolidation)
            long activeTableCount = tables.stream().filter(t -> !t.tableState.equals(TableState.GAME_OVER)).count();
            if (activeTableCount == 0) {
                System.out.println("All tables inactive - ending tournament");
                break;
            }

            iterations++;
        }

        assertThat(iterations).isLessThan(maxIterations).as("Tournament should complete");

        List<HeadlessPlayer> winners = allPlayers.stream().filter(p -> p.chipCount > 0)
                .collect(java.util.stream.Collectors.toList());

        // With table consolidation, we should have exactly 1 winner
        assertThat(winners).hasSize(1).as("Should have exactly one winner with table consolidation");

        // Verify chip conservation - total chips should be 60 * 1,000,000 = 60,000,000
        int totalChips = allPlayers.stream().mapToInt(p -> p.chipCount).sum();
        int totalPotChips = tables.stream().filter(t -> t.currentHand != null).mapToInt(t -> t.currentHand.pot).sum();
        assertThat(totalChips + totalPotChips).isEqualTo(60000000).as("Total chips should be conserved");
        assertThat(winners.get(0).chipCount).isEqualTo(60000000).as("Winner should have all chips");

        System.out.println("[OK]Large multi-table tournament completed in " + iterations + " iterations");
        System.out.println("[OK]Winner: " + winners.get(0).name + " with " + winners.get(0).chipCount + " chips");
        System.out.println("[OK]Total chips: " + totalChips + ", in pots: " + totalPotChips);
        System.out.println("[OK]Started with " + numTables + " tables, consolidated down to 1");
    }

    /**
     * Run a complete 2-player tournament headless. Verifies that pokergamecore can
     * execute a full game loop without GUI dependencies.
     */
    @Test
    void runCompleteTournament() {
        // Create headless game components
        HeadlessTournamentContext tournament = new HeadlessTournamentContext(2);
        HeadlessGameTable table = (HeadlessGameTable) tournament.getTable(0);
        GameEventBus eventBus = new GameEventBus();

        // Randomized AI that uses all poker actions
        java.util.Random random = new java.util.Random(42); // Fixed seed for reproducibility
        PlayerActionProvider aiProvider = (player, options) -> {
            // Randomly choose an available action
            java.util.List<PlayerAction> availableActions = new java.util.ArrayList<>();

            if (options.canFold())
                availableActions.add(PlayerAction.fold());
            if (options.canCheck())
                availableActions.add(PlayerAction.check());
            if (options.canCall())
                availableActions.add(PlayerAction.call());
            if (options.canBet()) {
                // Random bet amount between min and half of max
                int betAmount = options.minBet()
                        + random.nextInt(Math.max(1, (options.maxBet() - options.minBet()) / 2 + 1));
                availableActions.add(PlayerAction.bet(betAmount));
            }
            if (options.canRaise()) {
                // Random raise amount between min and half of max
                int raiseAmount = options.minRaise()
                        + random.nextInt(Math.max(1, (options.maxRaise() - options.minRaise()) / 2 + 1));
                availableActions.add(PlayerAction.raise(raiseAmount));
            }

            // Pick a random action from available options
            return availableActions.isEmpty()
                    ? PlayerAction.fold()
                    : availableActions.get(random.nextInt(availableActions.size()));
        };

        TournamentEngine engine = new TournamentEngine(eventBus, aiProvider);

        // Track game events
        List<String> events = new ArrayList<>();
        eventBus.subscribe(event -> events.add(event.getClass().getSimpleName()));

        // Run game loop until tournament completes
        int maxIterations = 1000; // Safety limit
        int iterations = 0;

        while (!table.tableState.equals(TableState.GAME_OVER) && iterations < maxIterations) {
            TableProcessResult result = engine.processTable(table, tournament, true, false);

            // Debug output every 10 iterations (first 50) then every 100
            if (iterations < 50 && iterations % 10 == 0 || iterations % 100 == 0) {
                String chips = tournament.players.stream().map(p -> p.name + "=" + p.chipCount)
                        .reduce((a, b) -> a + ", " + b).orElse("none");
                System.out
                        .println("Iteration " + iterations + ": state=" + table.tableState + ", chips=[" + chips + "]");
            }

            // Handle result
            if (result.nextState() != null) {
                table.tableState = result.nextState();
            }

            // Check for game over (since we skip phases that would normally set this)
            if (tournament.isGameOver() && !table.tableState.equals(TableState.GAME_OVER)) {
                // Award any remaining pot chips to the winner before ending
                if (table.currentHand != null && table.currentHand.pot > 0) {
                    HeadlessPlayer winner = tournament.players.stream().filter(p -> p.chipCount > 0).findFirst()
                            .orElse(null);
                    if (winner != null) {
                        winner.chipCount += table.currentHand.pot;
                        table.currentHand.pot = 0;
                    }
                }
                table.tableState = TableState.GAME_OVER;
            }

            // Simulate phase execution (would normally run Swing phases)
            if (result.phaseToRun() != null) {
                // In headless mode, auto-execute phases by transitioning to pending state
                String phase = result.phaseToRun();
                if ("TD.WaitForDeal".equals(phase)) {
                    // Simulate Deal button press
                    table.tableState = TableState.CHECK_END_HAND;
                } else if (result.pendingState() != null) {
                    // For all other phases, just transition to pending state
                    // This simulates the phase running and completing
                    table.tableState = result.pendingState();
                }
            }

            iterations++;
        }

        // Verify tournament completed
        assertThat(iterations).isLessThan(maxIterations).as("Tournament should complete");
        assertThat(table.tableState).isEqualTo(TableState.GAME_OVER);

        // Verify we got game events
        assertThat(events).isNotEmpty();

        // Verify there's a winner with ALL chips - total must be conserved
        HeadlessPlayer winner = tournament.players.stream().filter(p -> p.chipCount > 0).findFirst().orElse(null);

        assertThat(winner).isNotNull();
        assertThat(winner.chipCount).isEqualTo(3000); // 2 players * 1500 chips

        System.out.println("[OK]Headless tournament completed in " + iterations + " iterations");
        System.out.println("[OK]Winner: " + winner.name + " with " + winner.chipCount + " chips");
        System.out.println("[OK]Events fired: " + events.size());
    }

    /**
     * Edge case: Single player tournament should end immediately.
     */
    @Test
    void singlePlayerTournamentEndsImmediately() {
        HeadlessTournamentContext tournament = new HeadlessTournamentContext(1);
        HeadlessGameTable table = (HeadlessGameTable) tournament.getTable(0);
        GameEventBus eventBus = new GameEventBus();
        PlayerActionProvider aiProvider = (player, options) -> PlayerAction.fold();
        TournamentEngine engine = new TournamentEngine(eventBus, aiProvider);

        // Should be game over immediately with 1 player
        assertThat(tournament.isGameOver()).isTrue();
        assertThat(tournament.isOnePlayerLeft()).isTrue();

        // Process one iteration to transition to GAME_OVER state
        TableProcessResult result = engine.processTable(table, tournament, true, false);

        // Verify tournament state
        HeadlessPlayer winner = tournament.players.get(0);
        assertThat(winner.chipCount).isEqualTo(1500);
        assertThat(tournament.isGameOver()).isTrue();

        System.out.println("[OK]Single player tournament ends immediately");
        System.out.println("[OK]Winner: " + winner.name + " with " + winner.chipCount + " chips");
    }

    /**
     * Stress test: Very large tournament with 100 players across 10 tables.
     */
    @Test
    void runMassiveMultiTableTournament() {
        int numPlayers = 100;
        int numTables = 10;
        int startingChips = 1000000;

        // Create all players
        List<HeadlessPlayer> allPlayers = new ArrayList<>();
        for (int i = 0; i < numPlayers; i++) {
            allPlayers.add(new HeadlessPlayer(i + 1, "Player" + (i + 1), startingChips));
        }

        // Create tables
        List<HeadlessGameTable> tables = new ArrayList<>();
        int playersPerTable = numPlayers / numTables;
        for (int t = 0; t < numTables; t++) {
            List<HeadlessPlayer> tablePlayers = new ArrayList<>();
            for (int p = 0; p < playersPerTable; p++) {
                tablePlayers.add(allPlayers.get(t * playersPerTable + p));
            }
            tables.add(new HeadlessGameTable(tablePlayers));
        }

        MultiTableContext tournament = new MultiTableContext(allPlayers, tables);
        for (int i = 0; i < tables.size(); i++) {
            HeadlessGameTable oldTable = tables.get(i);
            tables.set(i, new HeadlessGameTable(oldTable.players, tournament));
        }

        GameEventBus eventBus = new GameEventBus();
        java.util.Random random = new java.util.Random(789);
        PlayerActionProvider aiProvider = (player, options) -> {
            java.util.List<PlayerAction> availableActions = new java.util.ArrayList<>();
            if (options.canFold())
                availableActions.add(PlayerAction.fold());
            if (options.canCheck())
                availableActions.add(PlayerAction.check());
            if (options.canCall())
                availableActions.add(PlayerAction.call());
            if (options.canBet()) {
                int betAmount = options.minBet()
                        + random.nextInt(Math.max(1, (options.maxBet() - options.minBet()) / 2 + 1));
                availableActions.add(PlayerAction.bet(betAmount));
            }
            if (options.canRaise()) {
                int raiseAmount = options.minRaise()
                        + random.nextInt(Math.max(1, (options.maxRaise() - options.minRaise()) / 2 + 1));
                availableActions.add(PlayerAction.raise(raiseAmount));
            }
            return availableActions.isEmpty()
                    ? PlayerAction.fold()
                    : availableActions.get(random.nextInt(availableActions.size()));
        };

        TournamentEngine engine = new TournamentEngine(eventBus, aiProvider);
        int maxIterations = 1000000;
        int iterations = 0;

        while (!tournament.isGameOver() && iterations < maxIterations) {
            for (HeadlessGameTable table : tables) {
                if (table.tableState.equals(TableState.GAME_OVER))
                    continue;
                long playersWithChipsAtTable = table.players.stream().filter(p -> p.chipCount > 0).count();
                if (playersWithChipsAtTable < 2) {
                    table.tableState = TableState.GAME_OVER;
                    continue;
                }
                TableProcessResult result = engine.processTable(table, tournament, true, false);
                if (result.nextState() != null)
                    table.tableState = result.nextState();
                if (tournament.isGameOver() && !table.tableState.equals(TableState.GAME_OVER)) {
                    if (table.currentHand != null && table.currentHand.pot > 0) {
                        HeadlessPlayer winner = allPlayers.stream().filter(p -> p.chipCount > 0).findFirst()
                                .orElse(null);
                        if (winner != null) {
                            winner.chipCount += table.currentHand.pot;
                            table.currentHand.pot = 0;
                        }
                    }
                    table.tableState = TableState.GAME_OVER;
                }
                if (result.phaseToRun() != null) {
                    String phase = result.phaseToRun();
                    if ("TD.WaitForDeal".equals(phase)) {
                        table.tableState = TableState.CHECK_END_HAND;
                    } else if (result.pendingState() != null) {
                        table.tableState = result.pendingState();
                    }
                }
            }

            // Table consolidation
            for (HeadlessGameTable sourceTable : tables) {
                if (sourceTable.tableState.equals(TableState.GAME_OVER))
                    continue;
                long playersAtSource = sourceTable.players.stream().filter(p -> p.chipCount > 0).count();
                if (playersAtSource == 1) {
                    HeadlessGameTable targetTable = tables.stream()
                            .filter(t -> !t.tableState.equals(TableState.GAME_OVER)).filter(t -> t != sourceTable)
                            .filter(t -> t.players.stream().filter(p -> p.chipCount > 0).count() >= 2).findFirst()
                            .orElse(tables.stream().filter(t -> !t.tableState.equals(TableState.GAME_OVER))
                                    .filter(t -> t != sourceTable).findFirst().orElse(null));
                    if (targetTable != null) {
                        HeadlessPlayer playerToMove = sourceTable.players.stream().filter(p -> p.chipCount > 0)
                                .findFirst().orElse(null);
                        if (playerToMove != null) {
                            if (sourceTable.currentHand != null && sourceTable.currentHand.pot > 0) {
                                playerToMove.chipCount += sourceTable.currentHand.pot;
                                sourceTable.currentHand.pot = 0;
                            }
                            targetTable.players.add(playerToMove);
                            sourceTable.tableState = TableState.GAME_OVER;
                        }
                    }
                }
            }

            if (iterations % 50000 == 0) {
                long playersLeft = allPlayers.stream().filter(p -> p.chipCount > 0).count();
                int activeTables = (int) tables.stream().filter(t -> !t.tableState.equals(TableState.GAME_OVER))
                        .count();
                System.out.println(
                        "Iteration " + iterations + ": players=" + playersLeft + ", active tables=" + activeTables);
            }

            long activeTableCount = tables.stream().filter(t -> !t.tableState.equals(TableState.GAME_OVER)).count();
            if (activeTableCount == 0)
                break;
            iterations++;
        }

        assertThat(iterations).isLessThan(maxIterations);
        List<HeadlessPlayer> winners = allPlayers.stream().filter(p -> p.chipCount > 0)
                .collect(java.util.stream.Collectors.toList());
        assertThat(winners).hasSize(1);
        int totalChips = allPlayers.stream().mapToInt(p -> p.chipCount).sum();
        int totalPotChips = tables.stream().filter(t -> t.currentHand != null).mapToInt(t -> t.currentHand.pot).sum();
        assertThat(totalChips + totalPotChips).isEqualTo(100000000);
        assertThat(winners.get(0).chipCount).isEqualTo(100000000);

        System.out.println("[OK]Massive 100-player tournament completed in " + iterations + " iterations");
        System.out.println("[OK]Winner: " + winners.get(0).name + " with " + winners.get(0).chipCount + " chips");
    }

    /**
     * Uneven table distribution with consolidation.
     */
    @Test
    void unevenTableDistributionConsolidation() {
        // Create 3 tables with 5, 6, 7 players
        List<HeadlessPlayer> allPlayers = new ArrayList<>();
        for (int i = 0; i < 18; i++) {
            allPlayers.add(new HeadlessPlayer(i + 1, "Player" + (i + 1), 50000));
        }

        List<HeadlessGameTable> tables = new ArrayList<>();
        // Table 1: 5 players
        tables.add(new HeadlessGameTable(new ArrayList<>(allPlayers.subList(0, 5))));
        // Table 2: 6 players
        tables.add(new HeadlessGameTable(new ArrayList<>(allPlayers.subList(5, 11))));
        // Table 3: 7 players
        tables.add(new HeadlessGameTable(new ArrayList<>(allPlayers.subList(11, 18))));

        MultiTableContext tournament = new MultiTableContext(allPlayers, tables);
        for (int i = 0; i < tables.size(); i++) {
            HeadlessGameTable oldTable = tables.get(i);
            tables.set(i, new HeadlessGameTable(oldTable.players, tournament));
        }

        GameEventBus eventBus = new GameEventBus();
        java.util.Random random = new java.util.Random(555);
        PlayerActionProvider aiProvider = (player, options) -> {
            java.util.List<PlayerAction> availableActions = new java.util.ArrayList<>();
            if (options.canFold())
                availableActions.add(PlayerAction.fold());
            if (options.canCheck())
                availableActions.add(PlayerAction.check());
            if (options.canCall())
                availableActions.add(PlayerAction.call());
            if (options.canBet()) {
                int betAmount = options.minBet()
                        + random.nextInt(Math.max(1, (options.maxBet() - options.minBet()) / 2 + 1));
                availableActions.add(PlayerAction.bet(betAmount));
            }
            if (options.canRaise()) {
                int raiseAmount = options.minRaise()
                        + random.nextInt(Math.max(1, (options.maxRaise() - options.minRaise()) / 2 + 1));
                availableActions.add(PlayerAction.raise(raiseAmount));
            }
            return availableActions.isEmpty()
                    ? PlayerAction.fold()
                    : availableActions.get(random.nextInt(availableActions.size()));
        };

        TournamentEngine engine = new TournamentEngine(eventBus, aiProvider);
        int maxIterations = 50000;
        int iterations = 0;
        int consolidations = 0;

        while (!tournament.isGameOver() && iterations < maxIterations) {
            for (HeadlessGameTable table : tables) {
                if (table.tableState.equals(TableState.GAME_OVER))
                    continue;
                long playersWithChipsAtTable = table.players.stream().filter(p -> p.chipCount > 0).count();
                if (playersWithChipsAtTable < 2) {
                    table.tableState = TableState.GAME_OVER;
                    continue;
                }
                TableProcessResult result = engine.processTable(table, tournament, true, false);
                if (result.nextState() != null)
                    table.tableState = result.nextState();
                if (tournament.isGameOver() && !table.tableState.equals(TableState.GAME_OVER)) {
                    if (table.currentHand != null && table.currentHand.pot > 0) {
                        HeadlessPlayer winner = allPlayers.stream().filter(p -> p.chipCount > 0).findFirst()
                                .orElse(null);
                        if (winner != null) {
                            winner.chipCount += table.currentHand.pot;
                            table.currentHand.pot = 0;
                        }
                    }
                    table.tableState = TableState.GAME_OVER;
                }
                if (result.phaseToRun() != null) {
                    String phase = result.phaseToRun();
                    if ("TD.WaitForDeal".equals(phase)) {
                        table.tableState = TableState.CHECK_END_HAND;
                    } else if (result.pendingState() != null) {
                        table.tableState = result.pendingState();
                    }
                }
            }

            // Table consolidation
            for (HeadlessGameTable sourceTable : tables) {
                if (sourceTable.tableState.equals(TableState.GAME_OVER))
                    continue;
                long playersAtSource = sourceTable.players.stream().filter(p -> p.chipCount > 0).count();
                if (playersAtSource == 1) {
                    HeadlessGameTable targetTable = tables.stream()
                            .filter(t -> !t.tableState.equals(TableState.GAME_OVER)).filter(t -> t != sourceTable)
                            .filter(t -> t.players.stream().filter(p -> p.chipCount > 0).count() >= 2).findFirst()
                            .orElse(tables.stream().filter(t -> !t.tableState.equals(TableState.GAME_OVER))
                                    .filter(t -> t != sourceTable).findFirst().orElse(null));
                    if (targetTable != null) {
                        HeadlessPlayer playerToMove = sourceTable.players.stream().filter(p -> p.chipCount > 0)
                                .findFirst().orElse(null);
                        if (playerToMove != null) {
                            if (sourceTable.currentHand != null && sourceTable.currentHand.pot > 0) {
                                playerToMove.chipCount += sourceTable.currentHand.pot;
                                sourceTable.currentHand.pot = 0;
                            }
                            targetTable.players.add(playerToMove);
                            sourceTable.tableState = TableState.GAME_OVER;
                            consolidations++;
                        }
                    }
                }
            }

            long activeTableCount = tables.stream().filter(t -> !t.tableState.equals(TableState.GAME_OVER)).count();
            if (activeTableCount == 0)
                break;
            iterations++;
        }

        assertThat(iterations).isLessThan(maxIterations);
        assertThat(consolidations).isGreaterThan(0);

        List<HeadlessPlayer> winners = allPlayers.stream().filter(p -> p.chipCount > 0)
                .collect(java.util.stream.Collectors.toList());
        assertThat(winners).hasSize(1);
        int totalChips = allPlayers.stream().mapToInt(p -> p.chipCount).sum();
        assertThat(totalChips).isEqualTo(900000);

        System.out.println("[OK]Uneven table distribution tournament completed");
        System.out.println("[OK]Consolidations performed: " + consolidations);
        System.out.println("[OK]Winner: " + winners.get(0).name);
    }

    /**
     * Test consolidation during active play.
     */
    @Test
    void consolidationDuringActivePlay() {
        // Create small tables to force frequent consolidation
        int numPlayers = 12;
        List<HeadlessPlayer> allPlayers = new ArrayList<>();
        for (int i = 0; i < numPlayers; i++) {
            allPlayers.add(new HeadlessPlayer(i + 1, "Player" + (i + 1), 5000));
        }

        // Create 4 tables with 3 players each
        List<HeadlessGameTable> tables = new ArrayList<>();
        for (int t = 0; t < 4; t++) {
            List<HeadlessPlayer> tablePlayers = new ArrayList<>();
            for (int p = 0; p < 3; p++) {
                tablePlayers.add(allPlayers.get(t * 3 + p));
            }
            tables.add(new HeadlessGameTable(tablePlayers));
        }

        MultiTableContext tournament = new MultiTableContext(allPlayers, tables);
        for (int i = 0; i < tables.size(); i++) {
            HeadlessGameTable oldTable = tables.get(i);
            tables.set(i, new HeadlessGameTable(oldTable.players, tournament));
        }

        GameEventBus eventBus = new GameEventBus();
        java.util.Random random = new java.util.Random(888);
        PlayerActionProvider aiProvider = (player, options) -> {
            java.util.List<PlayerAction> availableActions = new java.util.ArrayList<>();
            if (options.canFold())
                availableActions.add(PlayerAction.fold());
            if (options.canCheck())
                availableActions.add(PlayerAction.check());
            if (options.canCall())
                availableActions.add(PlayerAction.call());
            if (options.canBet()) {
                int betAmount = options.minBet()
                        + random.nextInt(Math.max(1, (options.maxBet() - options.minBet()) / 2 + 1));
                availableActions.add(PlayerAction.bet(betAmount));
            }
            if (options.canRaise()) {
                int raiseAmount = options.minRaise()
                        + random.nextInt(Math.max(1, (options.maxRaise() - options.minRaise()) / 2 + 1));
                availableActions.add(PlayerAction.raise(raiseAmount));
            }
            return availableActions.isEmpty()
                    ? PlayerAction.fold()
                    : availableActions.get(random.nextInt(availableActions.size()));
        };

        TournamentEngine engine = new TournamentEngine(eventBus, aiProvider);
        int maxIterations = 20000;
        int iterations = 0;
        int consolidations = 0;

        while (!tournament.isGameOver() && iterations < maxIterations) {
            for (HeadlessGameTable table : tables) {
                if (table.tableState.equals(TableState.GAME_OVER))
                    continue;
                long playersWithChipsAtTable = table.players.stream().filter(p -> p.chipCount > 0).count();
                if (playersWithChipsAtTable < 2) {
                    table.tableState = TableState.GAME_OVER;
                    continue;
                }
                TableProcessResult result = engine.processTable(table, tournament, true, false);
                if (result.nextState() != null)
                    table.tableState = result.nextState();
                if (tournament.isGameOver() && !table.tableState.equals(TableState.GAME_OVER)) {
                    if (table.currentHand != null && table.currentHand.pot > 0) {
                        HeadlessPlayer winner = allPlayers.stream().filter(p -> p.chipCount > 0).findFirst()
                                .orElse(null);
                        if (winner != null) {
                            winner.chipCount += table.currentHand.pot;
                            table.currentHand.pot = 0;
                        }
                    }
                    table.tableState = TableState.GAME_OVER;
                }
                if (result.phaseToRun() != null) {
                    String phase = result.phaseToRun();
                    if ("TD.WaitForDeal".equals(phase)) {
                        table.tableState = TableState.CHECK_END_HAND;
                    } else if (result.pendingState() != null) {
                        table.tableState = result.pendingState();
                    }
                }
            }

            // Table consolidation
            for (HeadlessGameTable sourceTable : tables) {
                if (sourceTable.tableState.equals(TableState.GAME_OVER))
                    continue;
                long playersAtSource = sourceTable.players.stream().filter(p -> p.chipCount > 0).count();
                if (playersAtSource == 1) {
                    HeadlessGameTable targetTable = tables.stream()
                            .filter(t -> !t.tableState.equals(TableState.GAME_OVER)).filter(t -> t != sourceTable)
                            .filter(t -> t.players.stream().filter(p -> p.chipCount > 0).count() >= 2).findFirst()
                            .orElse(tables.stream().filter(t -> !t.tableState.equals(TableState.GAME_OVER))
                                    .filter(t -> t != sourceTable).findFirst().orElse(null));
                    if (targetTable != null) {
                        HeadlessPlayer playerToMove = sourceTable.players.stream().filter(p -> p.chipCount > 0)
                                .findFirst().orElse(null);
                        if (playerToMove != null) {
                            if (sourceTable.currentHand != null && sourceTable.currentHand.pot > 0) {
                                playerToMove.chipCount += sourceTable.currentHand.pot;
                                System.out.println("Consolidation during active play: Awarded "
                                        + sourceTable.currentHand.pot + " pot chips");
                                sourceTable.currentHand.pot = 0;
                            }
                            targetTable.players.add(playerToMove);
                            sourceTable.tableState = TableState.GAME_OVER;
                            consolidations++;
                        }
                    }
                }
            }

            long activeTableCount = tables.stream().filter(t -> !t.tableState.equals(TableState.GAME_OVER)).count();
            if (activeTableCount == 0)
                break;
            iterations++;
        }

        assertThat(iterations).isLessThan(maxIterations);
        assertThat(consolidations).isGreaterThan(0);

        List<HeadlessPlayer> winners = allPlayers.stream().filter(p -> p.chipCount > 0)
                .collect(java.util.stream.Collectors.toList());
        assertThat(winners).hasSize(1);
        int totalChips = allPlayers.stream().mapToInt(p -> p.chipCount).sum();
        assertThat(totalChips).isEqualTo(60000);

        System.out.println("[OK]Consolidation during active play test completed");
        System.out.println("[OK]Total consolidations: " + consolidations);
        System.out.println("[OK]Winner: " + winners.get(0).name);
    }

    /**
     * Headless tournament context - no Swing dependencies
     */
    private static class HeadlessTournamentContext implements TournamentContext {
        protected final List<HeadlessPlayer> players; // Protected for subclass access
        protected final HeadlessGameTable table; // Protected for subclass access
        private int currentLevel = 1;
        private int handsAtCurrentLevel = 0;
        private static final int HANDS_PER_LEVEL = 10; // Advance level every 10 hands

        HeadlessTournamentContext(int numPlayers) {
            this.players = new ArrayList<>();
            for (int i = 0; i < numPlayers; i++) {
                players.add(new HeadlessPlayer(i + 1, "Player" + (i + 1), 1500));
            }
            this.table = new HeadlessGameTable(players, this);
        }

        // Blind schedule - doubles each level
        @Override
        public int getSmallBlind(int level) {
            return 500 * (1 << (level - 1)); // 500, 1000, 2000, 4000, 8000, etc.
        }

        @Override
        public int getBigBlind(int level) {
            return 1000 * (1 << (level - 1)); // 1000, 2000, 4000, 8000, 16000, etc.
        }

        @Override
        public int getAnte(int level) {
            return 0; // No antes for now
        }

        public void handCompleted() {
            handsAtCurrentLevel++;
            if (handsAtCurrentLevel >= HANDS_PER_LEVEL) {
                nextLevel();
            }
        }

        @Override
        public int getNumTables() {
            return 1;
        }

        @Override
        public GameTable getTable(int index) {
            return table;
        }

        @Override
        public int getNumPlayers() {
            return players.size();
        }

        @Override
        public GamePlayerInfo getPlayerByID(int playerId) {
            return players.stream().filter(p -> p.id == playerId).findFirst().orElse(null);
        }

        @Override
        public boolean isPractice() {
            return true;
        }

        @Override
        public boolean isOnlineGame() {
            return false;
        }

        @Override
        public boolean isGameOver() {
            long playersWithChips = players.stream().filter(p -> p.chipCount > 0).count();
            return playersWithChips <= 1;
        }

        @Override
        public boolean isOnePlayerLeft() {
            long playersWithChips = players.stream().filter(p -> p.chipCount > 0).count();
            return playersWithChips == 1;
        }

        @Override
        public int getLevel() {
            return currentLevel;
        }

        @Override
        public void nextLevel() {
            currentLevel++;
            handsAtCurrentLevel = 0;
            System.out.println("[INFO]Blind level increased to " + currentLevel + " (SB=" + getSmallBlind(currentLevel)
                    + ", BB=" + getBigBlind(currentLevel) + ")");
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
            return players.isEmpty() ? null : players.get(0); // Return first player for headless test
        }

        @Override
        public int getTimeoutSeconds() {
            return 30; // 30 second timeout for AI decisions
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
            return 0;
        }

        @Override
        public int getTimeoutForRound(int round) {
            return 30; // 30 seconds for all rounds
        }

        public GameTable getCurrentTable() {
            return table;
        }

        @Override
        public int getStartingChips() {
            return 1500;
        }

        @Override
        public boolean isRebuyPeriodActive(GamePlayerInfo player) {
            return false; // Stub: no rebuy period in headless tests
        }
    }

    /**
     * Multi-table tournament context - manages multiple tables
     */
    private static class MultiTableContext implements TournamentContext {
        private final List<HeadlessPlayer> players;
        private final List<HeadlessGameTable> tables;
        private int currentLevel = 1;
        private int handsAtCurrentLevel = 0;
        private static final int HANDS_PER_LEVEL = 10; // Advance level every 10 hands

        MultiTableContext(List<HeadlessPlayer> players, List<HeadlessGameTable> tables) {
            this.players = players;
            this.tables = tables;
        }

        // Blind schedule - doubles each level
        @Override
        public int getSmallBlind(int level) {
            return 500 * (1 << (level - 1)); // 500, 1000, 2000, 4000, 8000, etc.
        }

        @Override
        public int getBigBlind(int level) {
            return 1000 * (1 << (level - 1)); // 1000, 2000, 4000, 8000, 16000, etc.
        }

        @Override
        public int getAnte(int level) {
            return 0; // No antes for now
        }

        public void handCompleted() {
            handsAtCurrentLevel++;
            if (handsAtCurrentLevel >= HANDS_PER_LEVEL) {
                nextLevel();
            }
        }

        @Override
        public int getNumTables() {
            return tables.size();
        }

        @Override
        public GameTable getTable(int index) {
            return tables.get(index);
        }

        @Override
        public int getNumPlayers() {
            return players.size();
        }

        @Override
        public GamePlayerInfo getPlayerByID(int playerId) {
            return players.stream().filter(p -> p.id == playerId).findFirst().orElse(null);
        }

        @Override
        public boolean isPractice() {
            return true;
        }

        @Override
        public boolean isOnlineGame() {
            return false;
        }

        @Override
        public boolean isGameOver() {
            long playersWithChips = players.stream().filter(p -> p.chipCount > 0).count();
            return playersWithChips <= 1;
        }

        @Override
        public boolean isOnePlayerLeft() {
            long playersWithChips = players.stream().filter(p -> p.chipCount > 0).count();
            return playersWithChips == 1;
        }

        @Override
        public int getLevel() {
            return currentLevel;
        }

        @Override
        public void nextLevel() {
            currentLevel++;
            handsAtCurrentLevel = 0;
            System.out.println("[INFO]Blind level increased to " + currentLevel + " (SB=" + getSmallBlind(currentLevel)
                    + ", BB=" + getBigBlind(currentLevel) + ")");
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
            return players.isEmpty() ? null : players.get(0);
        }

        @Override
        public int getTimeoutSeconds() {
            return 30;
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
            return 0;
        }

        @Override
        public int getTimeoutForRound(int round) {
            return 30;
        }

        public GameTable getCurrentTable() {
            return tables.isEmpty() ? null : tables.get(0);
        }

        @Override
        public int getStartingChips() {
            return 1500;
        }

        @Override
        public boolean isRebuyPeriodActive(GamePlayerInfo player) {
            return false; // Stub: no rebuy period in multi-table tests
        }
    }

    /**
     * Headless game table - no Swing dependencies
     */
    private static class HeadlessGameTable implements GameTable {
        public TableState tableState = TableState.BEGIN;
        private final List<HeadlessPlayer> players;
        private HeadlessGameHand currentHand;
        private final TournamentContext tournament;

        HeadlessGameTable(List<HeadlessPlayer> players, TournamentContext tournament) {
            this.players = players;
            this.tournament = tournament;
        }

        // Constructor for multi-table context (which passes itself later)
        HeadlessGameTable(List<HeadlessPlayer> players) {
            this.players = players;
            this.tournament = null; // Will be set via setTournament if needed
        }

        @Override
        public int getNumber() {
            return 0;
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
            return null;
        }

        @Override
        public void setPendingTableState(TableState state) {
        }

        @Override
        public TableState getPreviousTableState() {
            return null;
        }

        @Override
        public String getPendingPhase() {
            return null;
        }

        @Override
        public void setPendingPhase(String phase) {
        }

        @Override
        public int getSeats() {
            return 10;
        }

        @Override
        public int getNumOccupiedSeats() {
            return (int) players.stream().filter(p -> !p.isObserver()).count();
        }

        @Override
        public GamePlayerInfo getPlayer(int seat) {
            return players.stream().filter(p -> p.seat == seat).findFirst().orElse(null);
        }

        @Override
        public int getButton() {
            return 0;
        }

        @Override
        public void setButton(int seat) {
        }

        @Override
        public int getNextSeatAfterButton() {
            return 1;
        }

        @Override
        public int getNextSeat(int seat) {
            return (seat + 1) % getSeats();
        }

        @Override
        public int getHandNum() {
            return 1;
        }

        @Override
        public void setHandNum(int handNum) {
        }

        @Override
        public int getLevel() {
            return 1;
        }

        @Override
        public void setLevel(int level) {
        }

        @Override
        public int getMinChip() {
            return 5;
        }

        @Override
        public GameHand getHoldemHand() {
            return currentHand;
        }

        @Override
        public void setHoldemHand(GameHand hand) {
            this.currentHand = (HeadlessGameHand) hand;
        }

        @Override
        public boolean isAutoDeal() {
            return false;
        }

        @Override
        public boolean isCurrent() {
            return true;
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
            // Reset player states
            players.forEach(p -> p.folded = false);
            // Create new hand with only players who have chips
            List<HeadlessPlayer> playersWithChips = players.stream().filter(p -> p.chipCount > 0)
                    .collect(java.util.stream.Collectors.toList());
            currentHand = new HeadlessGameHand(playersWithChips, tournament);
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
        public List<GamePlayerInfo> getAddedPlayersList() {
            return new ArrayList<>();
        }

        @Override
        public boolean isRemoved() {
            return false;
        }

        @Override
        public boolean isAllComputer() {
            return true;
        }
    }

    /**
     * Headless player - no Swing dependencies
     */
    private static class HeadlessPlayer implements GamePlayerInfo {
        private final int id;
        private final String name;
        private int chipCount;
        private final int seat;
        private boolean folded;
        private boolean sittingOut;

        HeadlessPlayer(int id, String name, int chipCount) {
            this.id = id;
            this.name = name;
            this.chipCount = chipCount;
            this.seat = id - 1; // Seat 0, 1, etc.
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
            return false;
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
            return seat;
        }

        @Override
        public boolean isAskShowWinning() {
            return false;
        }

        @Override
        public boolean isAskShowLosing() {
            return false;
        }

        @Override
        public boolean isObserver() {
            return false;
        }

        @Override
        public boolean isHumanControlled() {
            return false;
        }

        @Override
        public int getThinkBankMillis() {
            return 0;
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
            return true; // Headless test - all players are local AI
        }

        @Override
        public boolean isComputer() {
            return true;
        }

        @Override
        public void setTimeoutMillis(int millis) {
        }

        @Override
        public void setTimeoutMessageSecondsLeft(int seconds) {
        }

        @Override
        public int getNumRebuys() {
            return 0;
        }
    }

    /**
     * Headless game hand - minimal implementation for testing
     */
    private static class HeadlessGameHand implements GameHand {
        private BettingRound round = BettingRound.PRE_FLOP;
        private final List<HeadlessPlayer> players;
        private final List<HeadlessPlayer> activePlayers;
        private int currentPlayerIndex = 0;
        private int pot = 0;
        private boolean done = false;
        private final int smallBlindAmount;
        private final int bigBlindAmount;
        private final TournamentContext tournament;

        HeadlessGameHand(List<HeadlessPlayer> players, TournamentContext tournament) {
            this.tournament = tournament;
            this.players = players;
            this.activePlayers = new ArrayList<>(players);

            // Get blinds from tournament context based on current level
            int level = tournament != null ? tournament.getLevel() : 1;
            if (tournament instanceof HeadlessTournamentContext) {
                HeadlessTournamentContext ctx = (HeadlessTournamentContext) tournament;
                this.smallBlindAmount = ctx.getSmallBlind(level);
                this.bigBlindAmount = ctx.getBigBlind(level);
            } else if (tournament instanceof MultiTableContext) {
                MultiTableContext ctx = (MultiTableContext) tournament;
                this.smallBlindAmount = ctx.getSmallBlind(level);
                this.bigBlindAmount = ctx.getBigBlind(level);
            } else {
                // Fallback to default
                this.smallBlindAmount = 500;
                this.bigBlindAmount = 1000;
            }

            // Post blinds
            if (players.size() >= 2) {
                HeadlessPlayer smallBlind = players.get(0);
                HeadlessPlayer bigBlind = players.get(1);
                int sbAmount = Math.min(smallBlindAmount, smallBlind.chipCount);
                int bbAmount = Math.min(bigBlindAmount, bigBlind.chipCount);
                smallBlind.chipCount -= sbAmount;
                bigBlind.chipCount -= bbAmount;
                pot += sbAmount + bbAmount;
            }
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
            return done || activePlayers.size() <= 1;
        }

        @Override
        public int getNumWithCards() {
            return (int) activePlayers.stream().filter(p -> !p.folded).count();
        }

        @Override
        public int getCurrentPlayerInitIndex() {
            return currentPlayerIndex;
        }

        @Override
        public void advanceRound() {
            currentPlayerIndex = 0;
            switch (round) {
                case PRE_FLOP -> round = BettingRound.FLOP;
                case FLOP -> round = BettingRound.TURN;
                case TURN -> round = BettingRound.RIVER;
                case RIVER -> done = true;
            }
        }

        @Override
        public void preResolve(boolean isOnline) {
            // Check if only one player remains
            if (getNumWithCards() <= 1) {
                done = true;
            }
        }

        @Override
        public void resolve() {
            // Award pot to last remaining player
            HeadlessPlayer winner = activePlayers.stream().filter(p -> !p.folded).findFirst()
                    .orElse(activePlayers.get(0));
            winner.chipCount += pot;
            pot = 0;
            done = true;

            // Notify tournament that hand is complete (for blind level progression)
            if (tournament instanceof HeadlessTournamentContext) {
                ((HeadlessTournamentContext) tournament).handCompleted();
            } else if (tournament instanceof MultiTableContext) {
                ((MultiTableContext) tournament).handCompleted();
            }
        }

        @Override
        public void storeHandHistory() {
            // Not needed for headless test
        }

        @Override
        public List<GamePlayerInfo> getPreWinners() {
            if (getNumWithCards() == 1) {
                return activePlayers.stream().filter(p -> !p.folded).collect(java.util.stream.Collectors.toList());
            }
            return new ArrayList<>();
        }

        @Override
        public List<GamePlayerInfo> getPreLosers() {
            return activePlayers.stream().filter(p -> p.folded).collect(java.util.stream.Collectors.toList());
        }

        @Override
        public boolean isUncontested() {
            return getNumWithCards() <= 1;
        }

        @Override
        public GamePlayerInfo getCurrentPlayerWithInit() {
            if (activePlayers.isEmpty())
                return null;

            // Find next non-folded player
            int attempts = 0;
            while (attempts < activePlayers.size()) {
                HeadlessPlayer player = activePlayers.get(currentPlayerIndex % activePlayers.size());
                if (!player.folded) {
                    return player;
                }
                currentPlayerIndex++;
                attempts++;
            }

            // All players folded - hand should be done
            return null;
        }

        @Override
        public int getAmountToCall(GamePlayerInfo player) {
            return 0; // Simplified - no betting after blinds
        }

        @Override
        public int getMinBet() {
            return bigBlindAmount;
        }

        @Override
        public int getMinRaise() {
            return bigBlindAmount;
        }

        @Override
        public void applyPlayerAction(GamePlayerInfo player, PlayerAction action) {
            HeadlessPlayer hp = (HeadlessPlayer) player;

            switch (action.actionType()) {
                case FOLD -> {
                    hp.folded = true;
                    // Move to next player
                    currentPlayerIndex++;
                    // Check if betting is complete
                    if (getNumWithCards() <= 1 || currentPlayerIndex >= activePlayers.size()) {
                        if (getNumWithCards() <= 1) {
                            done = true;
                        } else {
                            advanceRound();
                        }
                    }
                }
                case CHECK, CALL -> {
                    currentPlayerIndex++;
                    if (currentPlayerIndex >= activePlayers.size()) {
                        advanceRound();
                    }
                }
                case BET, RAISE -> {
                    int amount = Math.min(action.amount(), hp.chipCount);
                    hp.chipCount -= amount;
                    pot += amount;
                    currentPlayerIndex++;
                    if (currentPlayerIndex >= activePlayers.size()) {
                        advanceRound();
                    }
                }
            }
        }

        // === V2 AI Card Access Methods ===

        @Override
        public Card[] getCommunityCards() {
            return new Card[0]; // No community cards in headless test
        }

        @Override
        public Card[] getPlayerCards(GamePlayerInfo player) {
            return null; // No hole cards tracked in headless test
        }

        // === V2 AI Pot State Methods ===

        @Override
        public int getPotSize() {
            return pot;
        }

        @Override
        public int getPotStatus() {
            return 0; // NO_POT_ACTION
        }

        @Override
        public float getPotOdds(GamePlayerInfo player) {
            int toCall = getAmountToCall(player);
            return toCall > 0 ? (float) pot / toCall : 0.0f;
        }

        // === V2 AI Betting History Methods ===

        @Override
        public boolean wasRaisedPreFlop() {
            return false; // Simplified - no raises tracked
        }

        @Override
        public GamePlayerInfo getFirstBettor(int round, boolean includeRaises) {
            return null; // No betting history tracked
        }

        @Override
        public GamePlayerInfo getLastBettor(int round, boolean includeRaises) {
            return null; // No betting history tracked
        }

        @Override
        public boolean wasFirstRaiserPreFlop(GamePlayerInfo player) {
            return false; // No raise history tracked
        }

        @Override
        public boolean wasLastRaiserPreFlop(GamePlayerInfo player) {
            return false; // No raise history tracked
        }

        @Override
        public boolean wasOnlyRaiserPreFlop(GamePlayerInfo player) {
            return false; // No raise history tracked
        }

        @Override
        public boolean wasPotAction(int round) {
            return false; // No action history tracked
        }

        // === V2 AI Player State Methods ===

        @Override
        public boolean paidToPlay(GamePlayerInfo player) {
            return true; // Assume all players paid blinds
        }

        @Override
        public boolean couldLimp(GamePlayerInfo player) {
            return false; // Simplified - no limping tracked
        }

        @Override
        public boolean limped(GamePlayerInfo player) {
            return false; // No limping tracked
        }

        @Override
        public boolean isBlind(GamePlayerInfo player) {
            if (players.isEmpty())
                return false;
            // First two players are blinds
            return player == players.get(0) || (players.size() > 1 && player == players.get(1));
        }

        @Override
        public boolean hasActedThisRound(GamePlayerInfo player) {
            return false; // No action tracking per round
        }

        @Override
        public int getLastActionThisRound(GamePlayerInfo player) {
            return 0; // No action history
        }

        @Override
        public int getFirstVoluntaryAction(GamePlayerInfo player, int round) {
            return 0; // No action history
        }

        // === V2 AI Count Methods ===

        @Override
        public int getNumLimpers() {
            return 0; // No limper tracking
        }

        @Override
        public int getNumFoldsSinceLastBet() {
            return 0; // No fold tracking
        }
    }
}
