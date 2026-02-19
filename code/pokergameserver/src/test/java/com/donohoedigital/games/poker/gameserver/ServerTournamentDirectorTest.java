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

import com.donohoedigital.games.poker.model.LevelAdvanceMode;
import com.donohoedigital.games.poker.core.PlayerAction;
import com.donohoedigital.games.poker.core.PlayerActionProvider;
import com.donohoedigital.games.poker.core.TournamentEngine;
import com.donohoedigital.games.poker.core.state.TableState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * Test suite for ServerTournamentDirector. Tests the main game loop that
 * orchestrates TournamentEngine without Swing dependencies.
 */
class ServerTournamentDirectorTest {

    /**
     * Test that a single-table tournament with AI players runs to completion.
     */
    @Test
    void singleTableTournamentCompletes() throws Exception {
        // Create 4-player tournament
        List<ServerPlayer> players = createPlayers(4, 500);
        ServerTournamentContext tournament = createTournament(players, 1);

        // Create server components
        InMemoryGameEventStore eventStore = new InMemoryGameEventStore("test-game");
        ServerGameEventBus eventBus = new ServerGameEventBus(eventStore);
        PlayerActionProvider aiProvider = createSimpleAI(42);
        ServerPlayerActionProvider actionProvider = new ServerPlayerActionProvider(aiProvider, request -> {
        }, 0, 2, new java.util.concurrent.ConcurrentHashMap<>());

        // Create director
        List<String> lifecycleEvents = new ArrayList<>();
        ServerTournamentDirector director = new ServerTournamentDirector(new TournamentEngine(eventBus, actionProvider),
                tournament, eventBus, actionProvider,
                new GameServerProperties(50, 30, 120, 10, 1000, 3, 2, 5, 5, 24, 7, "ws://localhost", 0),
                event -> lifecycleEvents.add(event.toString()));

        // Run in background thread
        Thread thread = new Thread(director);
        thread.start();
        thread.join(30000); // 30 second timeout

        // Verify completion
        assertThat(thread.isAlive()).isFalse();
        assertThat(tournament.isGameOver()).isTrue();
        assertThat(lifecycleEvents).contains("COMPLETED");

        // Verify winner has all chips
        long winnersWithChips = players.stream().filter(p -> p.getChipCount() > 0).count();
        assertThat(winnersWithChips).isEqualTo(1);

        ServerPlayer winner = players.stream().filter(p -> p.getChipCount() > 0).findFirst().orElse(null);
        assertThat(winner).isNotNull();
        assertThat(winner.getChipCount()).isEqualTo(2000); // 4 * 500
    }

    /**
     * Test that a multi-table tournament consolidates tables correctly.
     */
    @Test
    void multiTableTournamentConsolidates() throws Exception {
        // Create 12-player tournament (2 tables of 6)
        List<ServerPlayer> players = createPlayers(12, 500);
        ServerTournamentContext tournament = createTournament(players, 2);

        // Create server components
        InMemoryGameEventStore eventStore = new InMemoryGameEventStore("test-game-multi");
        ServerGameEventBus eventBus = new ServerGameEventBus(eventStore);
        PlayerActionProvider aiProvider = createSimpleAI(123);
        ServerPlayerActionProvider actionProvider = new ServerPlayerActionProvider(aiProvider, request -> {
        }, 0, 2, new java.util.concurrent.ConcurrentHashMap<>());

        // Create director
        List<String> lifecycleEvents = new ArrayList<>();
        ServerTournamentDirector director = new ServerTournamentDirector(new TournamentEngine(eventBus, actionProvider),
                tournament, eventBus, actionProvider,
                new GameServerProperties(50, 30, 120, 10, 1000, 3, 2, 5, 5, 24, 7, "ws://localhost", 0),
                event -> lifecycleEvents.add(event.toString()));

        // Run in background thread
        Thread thread = new Thread(director);
        thread.start();
        thread.join(120000); // 120 second timeout for multi-table

        // Verify completion
        assertThat(thread.isAlive()).isFalse();
        assertThat(tournament.isGameOver()).isTrue();

        // Verify winner has all chips
        ServerPlayer winner = players.stream().filter(p -> p.getChipCount() > 0).findFirst().orElse(null);
        assertThat(winner).isNotNull();
        assertThat(winner.getChipCount()).isEqualTo(6000); // 12 * 500

        // Verify tables were consolidated (exactly one active table remains)
        int activeTables = 0;
        for (int i = 0; i < tournament.getNumTables(); i++) {
            if (tournament.getTable(i).getTableState() != TableState.GAME_OVER) {
                activeTables++;
            }
        }
        assertThat(activeTables).isLessThanOrEqualTo(1);
    }

    /**
     * Test pause and resume functionality.
     */
    @Test
    void pauseAndResume() throws Exception {
        List<ServerPlayer> players = createPlayers(4, 500);
        ServerTournamentContext tournament = createTournament(players, 1);

        InMemoryGameEventStore eventStore = new InMemoryGameEventStore("test-pause");
        ServerGameEventBus eventBus = new ServerGameEventBus(eventStore);
        PlayerActionProvider aiProvider = createSimpleAI(42);
        ServerPlayerActionProvider actionProvider = new ServerPlayerActionProvider(aiProvider, request -> {
        }, 0, 2, new java.util.concurrent.ConcurrentHashMap<>());

        List<String> lifecycleEvents = new ArrayList<>();
        ServerTournamentDirector director = new ServerTournamentDirector(new TournamentEngine(eventBus, actionProvider),
                tournament, eventBus, actionProvider,
                new GameServerProperties(50, 30, 120, 10, 1000, 3, 2, 5, 5, 24, 7, "ws://localhost", 0),
                event -> lifecycleEvents.add(event.toString()));

        // Start in background
        Thread thread = new Thread(director);
        thread.start();

        // Wait a bit, then pause
        Thread.sleep(100);
        director.pause();
        assertThat(lifecycleEvents).contains("PAUSED");

        // Wait a bit while paused
        Thread.sleep(100);

        // Resume
        director.resume();
        assertThat(lifecycleEvents).contains("RESUMED");

        // Let it finish
        thread.join(60000); // 60 second timeout

        assertThat(thread.isAlive()).isFalse();
        assertThat(tournament.isGameOver()).isTrue();
    }

    /**
     * Test that shutdown stops the director cleanly.
     */
    @Test
    void shutdownStopsCleanly() throws Exception {
        List<ServerPlayer> players = createPlayers(4, 500);
        ServerTournamentContext tournament = createTournament(players, 1);

        InMemoryGameEventStore eventStore = new InMemoryGameEventStore("test-shutdown");
        ServerGameEventBus eventBus = new ServerGameEventBus(eventStore);
        PlayerActionProvider aiProvider = createSimpleAI(42);
        ServerPlayerActionProvider actionProvider = new ServerPlayerActionProvider(aiProvider, request -> {
        }, 0, 2, new java.util.concurrent.ConcurrentHashMap<>());

        CountDownLatch shutdownLatch = new CountDownLatch(1);
        ServerTournamentDirector director = new ServerTournamentDirector(new TournamentEngine(eventBus, actionProvider),
                tournament, eventBus, actionProvider,
                new GameServerProperties(50, 30, 120, 10, 1000, 3, 2, 5, 5, 24, 7, "ws://localhost", 0), event -> {
                    if (event == GameLifecycleEvent.COMPLETED) {
                        shutdownLatch.countDown();
                    }
                });

        // Start in background
        Thread thread = new Thread(director);
        thread.start();

        // Wait a bit then request shutdown
        Thread.sleep(100);
        director.shutdown();

        // Wait for shutdown
        boolean completed = shutdownLatch.await(5, TimeUnit.SECONDS);
        thread.join(5000);

        assertThat(completed).isTrue();
        assertThat(thread.isAlive()).isFalse();
    }

    /**
     * Test that chips are conserved throughout the tournament.
     */
    @Test
    void chipsAreConserved() throws Exception {
        List<ServerPlayer> players = createPlayers(6, 500);
        ServerTournamentContext tournament = createTournament(players, 1);

        int totalChips = players.stream().mapToInt(ServerPlayer::getChipCount).sum();
        assertThat(totalChips).isEqualTo(3000); // 6 * 500

        InMemoryGameEventStore eventStore = new InMemoryGameEventStore("test-chips");
        ServerGameEventBus eventBus = new ServerGameEventBus(eventStore);
        PlayerActionProvider aiProvider = createSimpleAI(42);
        ServerPlayerActionProvider actionProvider = new ServerPlayerActionProvider(aiProvider, request -> {
        }, 0, 2, new java.util.concurrent.ConcurrentHashMap<>());

        ServerTournamentDirector director = new ServerTournamentDirector(new TournamentEngine(eventBus, actionProvider),
                tournament, eventBus, actionProvider,
                new GameServerProperties(50, 30, 120, 10, 1000, 3, 2, 5, 5, 24, 7, "ws://localhost", 0), event -> {
                });

        // Run
        Thread thread = new Thread(director);
        thread.start();
        thread.join(30000);

        // Verify total chips unchanged
        int finalTotalChips = players.stream().mapToInt(ServerPlayer::getChipCount).sum();
        assertThat(finalTotalChips).isEqualTo(totalChips);
    }

    // Helper methods

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
        // Aggressive blinds with hands-based level advancement for fast test completion
        // Starting M of ~3.3 (500 chips / (50+100) blinds) forces quick action
        // Levels advance every 2 hands with 6 tiers for rapid escalation
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
                0 // timeout seconds
                , LevelAdvanceMode.HANDS, // hands-based advancement
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
