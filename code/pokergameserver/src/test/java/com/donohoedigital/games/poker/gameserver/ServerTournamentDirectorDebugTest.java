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
package com.donohoedigital.games.poker.gameserver;

import com.donohoedigital.games.poker.core.GameTable;
import com.donohoedigital.games.poker.model.LevelAdvanceMode;
import com.donohoedigital.games.poker.core.PlayerAction;
import com.donohoedigital.games.poker.core.PlayerActionProvider;
import com.donohoedigital.games.poker.core.TableProcessResult;
import com.donohoedigital.games.poker.core.TournamentEngine;
import com.donohoedigital.games.poker.core.state.TableState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.*;

/**
 * Debug test for ServerTournamentDirector - manually drive the loop with
 * iteration limits to see what's happening without running out of memory.
 */
class ServerTournamentDirectorDebugTest {

    @Test
    void manualGameLoop() {
        // Create 4-player tournament
        List<ServerPlayer> players = createPlayers(4, 500);
        ServerTournamentContext tournament = createTournament(players, 1);

        // Create server components
        InMemoryGameEventStore eventStore = new InMemoryGameEventStore("debug-game");
        ServerGameEventBus eventBus = new ServerGameEventBus(eventStore);
        PlayerActionProvider aiProvider = createSimpleAI(42);
        ServerPlayerActionProvider actionProvider = new ServerPlayerActionProvider(aiProvider, request -> {
        }, 0);

        TournamentEngine engine = new TournamentEngine(eventBus, actionProvider);

        // Manual game loop with iteration limit
        int maxIterations = 1000;
        int iterations = 0;

        while (!tournament.isGameOver() && iterations < maxIterations) {
            GameTable table = tournament.getTable(0);

            if (table.getTableState() == TableState.GAME_OVER) {
                break;
            }

            TableProcessResult result = engine.processTable(table, tournament, true, true);

            // Debug output every 50 iterations
            if (iterations % 50 == 0) {
                long playersLeft = players.stream().filter(p -> p.getChipCount() > 0).count();
                System.out.println("Iteration " + iterations + ": state=" + table.getTableState()
                        + ", players remaining=" + playersLeft + ", phase=" + result.phaseToRun());

                // Show chip counts for remaining players
                for (ServerPlayer p : players) {
                    if (p.getChipCount() > 0) {
                        System.out.println("  " + p.getName() + ": " + p.getChipCount() + " chips");
                    }
                }
            }

            // Apply state transition
            if (result.nextState() != null) {
                table.setTableState(result.nextState());
            }

            // Handle phases
            if (result.phaseToRun() != null) {
                String phase = result.phaseToRun();
                if ("TD.WaitForDeal".equals(phase)) {
                    table.setTableState(TableState.CHECK_END_HAND);
                } else if (result.pendingState() != null) {
                    table.setTableState(result.pendingState());
                }
            }

            // Check for game over
            if (tournament.isGameOver() && table.getTableState() != TableState.GAME_OVER) {
                table.setTableState(TableState.GAME_OVER);
            }

            iterations++;
        }

        System.out.println("Completed in " + iterations + " iterations");
        System.out.println("Game over: " + tournament.isGameOver());
        System.out.println("Table state: " + tournament.getTable(0).getTableState());

        // Should complete in reasonable iterations
        assertThat(iterations).isLessThan(maxIterations);
    }

    private List<ServerPlayer> createPlayers(int count, int startingChips) {
        List<ServerPlayer> players = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ServerPlayer player = new ServerPlayer(i + 1, "Player" + (i + 1), false, // All AI
                    5, // Medium skill
                    startingChips);
            player.setSeat(i);
            players.add(player);
        }
        return players;
    }

    private ServerTournamentContext createTournament(List<ServerPlayer> players, int numTables) {
        // Moderate blinds with hands-based level advancement for fast, predictable
        // tests
        // Starting M of ~6.7 (1000 chips / (50+100) blinds) allows strategic play
        // Levels advance every 3 hands for rapid blind escalation
        return new ServerTournamentContext(players, numTables, 500, // starting chips (aggressive)
                new int[]{50, 100, 200, 400, 800, 1600}, // small blinds (rapid progression)
                new int[]{100, 200, 400, 800, 1600, 3200}, // big blinds (rapid progression)
                new int[]{25, 50, 100, 200, 400, 800}, // antes (aggressive)
                new int[]{20, 20, 20, 20, 20, 20}, // level minutes (unused with HANDS mode)
                new boolean[]{false, false, false, false, false, false}, // break levels
                true, // practice
                0, // max rebuys
                0, // rebuy max level
                false, // allow addons
                0, // timeout seconds
                LevelAdvanceMode.HANDS, // hands-based advancement
                2 // 2 hands per level
        );
    }

    private PlayerActionProvider createSimpleAI(long seed) {
        Random random = new Random(seed);
        return (player, options) -> {
            List<PlayerAction> availableActions = new ArrayList<>();

            if (options.canCheck())
                availableActions.add(PlayerAction.check());
            if (options.canCall())
                availableActions.add(PlayerAction.call());
            if (options.canFold())
                availableActions.add(PlayerAction.fold());

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
    }
}
