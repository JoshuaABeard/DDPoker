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
import com.donohoedigital.games.poker.core.event.GameEvent;
import com.donohoedigital.games.poker.core.state.ActionType;
import com.donohoedigital.games.poker.core.state.BettingRound;
import com.donohoedigital.games.poker.core.state.TableState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
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

    /**
     * Test that the inter-hand pause (aiActionDelayMs) prevents AI-only hands from
     * racing. With aiActionDelayMs=20ms and instant AI actions (0ms per action),
     * the gap between consecutive HAND_STARTED events must be >= 20ms.
     *
     * Regression test for: hands completing in <1ms when nextState==CLEAN and
     * TD.CheckEndHand were both dead code paths for auto-deal games. The correct
     * hook is nextState==BEGIN (handleDone() always returns BEGIN after showdown).
     */
    @Test
    void interHandPausePreventsRacing() throws Exception {
        int delayMs = 20; // small enough for fast tests, large enough to detect racing

        // 6 players ensures at least 5 hands are played before one player wins all
        // chips
        List<ServerPlayer> players = createPlayers(6, 500);
        ServerTournamentContext tournament = createTournament(players, 1);

        InMemoryGameEventStore eventStore = new InMemoryGameEventStore("test-pacing");
        ServerGameEventBus eventBus = new ServerGameEventBus(eventStore);

        // AI actions: instant (no per-action delay). Only inter-hand pause is tested.
        PlayerActionProvider aiProvider = createSimpleAI(42);
        ServerPlayerActionProvider actionProvider = new ServerPlayerActionProvider(aiProvider, request -> {
        }, 0, 2, new java.util.concurrent.ConcurrentHashMap<>());

        // Record timestamp of each HAND_STARTED event
        List<Long> handStartedNanos = new CopyOnWriteArrayList<>();
        eventBus.subscribe(event -> {
            if (event instanceof GameEvent.HandStarted) {
                handStartedNanos.add(System.nanoTime());
            }
        });

        ServerTournamentDirector director = new ServerTournamentDirector(new TournamentEngine(eventBus, actionProvider),
                tournament, eventBus, actionProvider,
                new GameServerProperties(50, 30, 120, 10, 1000, 3, 2, 5, 5, 24, 7, "ws://localhost", delayMs),
                event -> {
                });

        Thread thread = new Thread(director);
        thread.start();
        thread.join(30000); // wait for game to complete naturally

        assertThat(thread.isAlive()).isFalse();
        assertThat(tournament.isGameOver()).isTrue();

        // Must have played at least 2 hands for gap assertions to be meaningful
        assertThat(handStartedNanos.size())
                .as("Expected at least 2 HAND_STARTED events but got %d", handStartedNanos.size())
                .isGreaterThanOrEqualTo(2);

        // Verify inter-hand gaps: every consecutive pair should be >= delayMs.
        // Old racing bug: gaps were <1ms (sleep hook was dead code). Fixed by
        // hooking nextState==BEGIN instead of unreachable CLEAN/CheckEndHand.
        long minExpectedGapNs = (long) delayMs * 1_000_000L;
        for (int i = 1; i < handStartedNanos.size(); i++) {
            long gapNs = handStartedNanos.get(i) - handStartedNanos.get(i - 1);
            assertThat(gapNs)
                    .as("Gap between HAND_STARTED[%d] and [%d] should be >= %dms (racing fix)", i - 1, i, delayMs)
                    .isGreaterThanOrEqualTo(minExpectedGapNs);
        }
    }

    /**
     * Test that CommunityCardsDealt events are published when community cards are
     * dealt. Regression test for: TD.DealCommunity phase previously fell through to
     * the default case without publishing the event, so clients never received
     * community card data.
     *
     * <p>
     * Uses a 2-player all-in setup: starting chips (100) equal the big blind (100),
     * so both players go all-in on the first hand. This guarantees that the flop,
     * turn, and river are dealt (all-in showdown) and the game finishes in ≤ 2
     * hands.
     */
    @Test
    void communityCardsDealtEventsPublished() throws Exception {
        // 2-player game where starting stack == big blind → forced all-in showdown
        // ensures community cards are always dealt and the game ends quickly.
        List<ServerPlayer> players = createPlayers(2, 100);
        ServerTournamentContext tournament = createTournament(players, 1);

        InMemoryGameEventStore eventStore = new InMemoryGameEventStore("test-community");
        ServerGameEventBus eventBus = new ServerGameEventBus(eventStore);

        // Passive AI: always check or call, never fold. Players go all-in via blinds.
        PlayerActionProvider passiveAI = (player, options) -> {
            if (options.canCheck())
                return PlayerAction.check();
            return PlayerAction.call();
        };
        ServerPlayerActionProvider actionProvider = new ServerPlayerActionProvider(passiveAI, request -> {
        }, 0, 2, new java.util.concurrent.ConcurrentHashMap<>());

        List<GameEvent.CommunityCardsDealt> communityEvents = new CopyOnWriteArrayList<>();
        eventBus.subscribe(event -> {
            if (event instanceof GameEvent.CommunityCardsDealt e) {
                communityEvents.add(e);
            }
        });

        ServerTournamentDirector director = new ServerTournamentDirector(new TournamentEngine(eventBus, actionProvider),
                tournament, eventBus, actionProvider,
                new GameServerProperties(50, 30, 120, 10, 1000, 3, 2, 5, 5, 24, 7, "ws://localhost", 0), event -> {
                });

        Thread thread = new Thread(director);
        thread.start();
        thread.join(10000); // 10 seconds is more than enough for a 2-player all-in game

        assertThat(thread.isAlive()).isFalse();
        assertThat(tournament.isGameOver()).isTrue();

        // Debug: print ALL events to understand the state machine flow
        eventStore.getEvents().forEach(se -> System.out.println("EVENT: " + se.event()));
        System.out.println("CommunityCardsDealt count: " + communityEvents.size());

        assertThat(communityEvents).isNotEmpty();
        assertThat(communityEvents).allSatisfy(e -> {
            assertThat(e.round()).isIn(BettingRound.FLOP, BettingRound.TURN, BettingRound.RIVER);
            assertThat(e.tableId()).isGreaterThanOrEqualTo(0);
        });
    }

    /**
     * ShowdownStarted, PotAwarded, and HandCompleted must be published for every
     * completed hand. Uses the same forced all-in setup as
     * {@link #communityCardsDealtEventsPublished}.
     */
    @Test
    void showdownEventsPublished() throws Exception {
        List<ServerPlayer> players = createPlayers(2, 100);
        ServerTournamentContext tournament = createTournament(players, 1);
        InMemoryGameEventStore eventStore = new InMemoryGameEventStore("test-showdown");
        ServerGameEventBus eventBus = new ServerGameEventBus(eventStore);

        PlayerActionProvider passiveAI = (player, options) -> {
            if (options.canCheck())
                return PlayerAction.check();
            return PlayerAction.call();
        };
        ServerPlayerActionProvider actionProvider = new ServerPlayerActionProvider(passiveAI, request -> {
        }, 0, 2, new java.util.concurrent.ConcurrentHashMap<>());

        List<GameEvent.ShowdownStarted> showdownEvents = new CopyOnWriteArrayList<>();
        List<GameEvent.PotAwarded> potEvents = new CopyOnWriteArrayList<>();
        List<GameEvent.HandCompleted> handDoneEvents = new CopyOnWriteArrayList<>();
        eventBus.subscribe(event -> {
            if (event instanceof GameEvent.ShowdownStarted e)
                showdownEvents.add(e);
            else if (event instanceof GameEvent.PotAwarded e)
                potEvents.add(e);
            else if (event instanceof GameEvent.HandCompleted e)
                handDoneEvents.add(e);
        });

        ServerTournamentDirector director = new ServerTournamentDirector(new TournamentEngine(eventBus, actionProvider),
                tournament, eventBus, actionProvider,
                new GameServerProperties(50, 30, 120, 10, 1000, 3, 2, 5, 5, 24, 7, "ws://localhost", 0), event -> {
                });

        Thread thread = new Thread(director);
        thread.start();
        thread.join(10000);

        assertThat(thread.isAlive()).isFalse();
        assertThat(tournament.isGameOver()).isTrue();

        assertThat(showdownEvents).as("ShowdownStarted").isNotEmpty();
        assertThat(potEvents).as("PotAwarded").isNotEmpty();
        assertThat(potEvents).as("PotAwarded entries").allSatisfy(e -> {
            assertThat(e.winnerIds()).as("winner IDs").isNotEmpty();
            assertThat(e.amount()).as("pot amount").isGreaterThan(0);
            assertThat(e.tableId()).isGreaterThanOrEqualTo(0);
        });
        assertThat(handDoneEvents).as("HandCompleted").isNotEmpty();

        // Order: ShowdownStarted → ≥1 PotAwarded → HandCompleted
        List<GameEvent> allEvents = eventStore.getEvents().stream().map(se -> se.event())
                .filter(e -> e instanceof GameEvent.ShowdownStarted || e instanceof GameEvent.PotAwarded
                        || e instanceof GameEvent.HandCompleted)
                .toList();
        int firstShowdown = -1, lastPot = -1, firstDone = -1;
        for (int i = 0; i < allEvents.size(); i++) {
            if (allEvents.get(i) instanceof GameEvent.ShowdownStarted && firstShowdown < 0)
                firstShowdown = i;
            if (allEvents.get(i) instanceof GameEvent.PotAwarded)
                lastPot = i;
            if (allEvents.get(i) instanceof GameEvent.HandCompleted && firstDone < 0)
                firstDone = i;
        }
        assertThat(firstShowdown).as("ShowdownStarted index").isGreaterThanOrEqualTo(0);
        assertThat(lastPot).as("PotAwarded after ShowdownStarted").isGreaterThan(firstShowdown);
        assertThat(firstDone).as("HandCompleted after PotAwarded").isGreaterThan(lastPot);
    }

    /**
     * LevelChanged must be published whenever the tournament advances to the next
     * blind level. Uses HANDS-based level advancement (every 2 hands) with a
     * multi-hand game.
     */
    @Test
    void levelChangedEventPublishedOnLevelAdvance() throws Exception {
        List<ServerPlayer> players = createPlayers(4, 500);
        ServerTournamentContext tournament = createTournament(players, 1);
        InMemoryGameEventStore eventStore = new InMemoryGameEventStore("test-level");
        ServerGameEventBus eventBus = new ServerGameEventBus(eventStore);
        PlayerActionProvider aiProvider = createSimpleAI(99);
        ServerPlayerActionProvider actionProvider = new ServerPlayerActionProvider(aiProvider, request -> {
        }, 0, 2, new java.util.concurrent.ConcurrentHashMap<>());

        List<GameEvent.LevelChanged> levelEvents = new CopyOnWriteArrayList<>();
        eventBus.subscribe(event -> {
            if (event instanceof GameEvent.LevelChanged e)
                levelEvents.add(e);
        });

        ServerTournamentDirector director = new ServerTournamentDirector(new TournamentEngine(eventBus, actionProvider),
                tournament, eventBus, actionProvider,
                new GameServerProperties(50, 30, 120, 10, 1000, 3, 2, 5, 5, 24, 7, "ws://localhost", 0), event -> {
                });

        Thread thread = new Thread(director);
        thread.start();
        thread.join(30000);

        assertThat(thread.isAlive()).isFalse();
        assertThat(tournament.isGameOver()).isTrue();

        assertThat(levelEvents).as("LevelChanged events").isNotEmpty();
        assertThat(levelEvents).as("LevelChanged newLevel values").allSatisfy(e -> {
            assertThat(e.newLevel()).isGreaterThan(0);
            assertThat(e.tableId()).isGreaterThanOrEqualTo(0);
        });
    }

    /**
     * PlayerEliminated events must be published for each eliminated player with a
     * correct finish position. A 4-player tournament eliminates 3 players (the
     * winner keeps chips); each must receive a position in [2, numPlayers].
     */
    @Test
    void playerEliminatedEventsPublished() throws Exception {
        List<ServerPlayer> players = createPlayers(4, 100);
        ServerTournamentContext tournament = createTournament(players, 1);

        InMemoryGameEventStore eventStore = new InMemoryGameEventStore("test-elim");
        ServerGameEventBus eventBus = new ServerGameEventBus(eventStore);

        // Passive AI: always call — guarantees showdowns and chip transfers
        PlayerActionProvider passiveAI = (player, options) -> {
            if (options.canCheck())
                return PlayerAction.check();
            return PlayerAction.call();
        };
        ServerPlayerActionProvider actionProvider = new ServerPlayerActionProvider(passiveAI, request -> {
        }, 0, 2, new java.util.concurrent.ConcurrentHashMap<>());

        List<GameEvent.PlayerEliminated> elimEvents = new CopyOnWriteArrayList<>();
        eventBus.subscribe(event -> {
            if (event instanceof GameEvent.PlayerEliminated e)
                elimEvents.add(e);
        });

        ServerTournamentDirector director = new ServerTournamentDirector(new TournamentEngine(eventBus, actionProvider),
                tournament, eventBus, actionProvider,
                new GameServerProperties(50, 30, 120, 10, 1000, 3, 2, 5, 5, 24, 7, "ws://localhost", 0), event -> {
                });

        Thread thread = new Thread(director);
        thread.start();
        thread.join(30000);

        assertThat(thread.isAlive()).isFalse();
        assertThat(tournament.isGameOver()).isTrue();

        // 4 players → exactly 3 eliminations; winner is never eliminated
        assertThat(elimEvents).as("PlayerEliminated count").hasSize(3);
        assertThat(elimEvents).as("PlayerEliminated positions").allSatisfy(e -> {
            assertThat(e.finishPosition()).isBetween(2, 4);
            assertThat(e.tableId()).isGreaterThanOrEqualTo(0);
        });
    }

    /**
     * PlayerActed events for BLIND_SM and BLIND_BIG (and ANTE when configured) must
     * be published immediately after each HAND_STARTED so clients can update chip
     * counts before ACTION_REQUIRED arrives.
     */
    @Test
    void blindPostingEventsPublished() throws Exception {
        List<ServerPlayer> players = createPlayers(2, 100);
        ServerTournamentContext tournament = createTournament(players, 1);

        InMemoryGameEventStore eventStore = new InMemoryGameEventStore("test-blinds");
        ServerGameEventBus eventBus = new ServerGameEventBus(eventStore);

        // Passive AI: always call — guarantees community cards and full hand play
        PlayerActionProvider passiveAI = (player, options) -> {
            if (options.canCheck())
                return PlayerAction.check();
            return PlayerAction.call();
        };
        ServerPlayerActionProvider actionProvider = new ServerPlayerActionProvider(passiveAI, request -> {
        }, 0, 2, new java.util.concurrent.ConcurrentHashMap<>());

        List<GameEvent.PlayerActed> blindEvents = new CopyOnWriteArrayList<>();
        eventBus.subscribe(event -> {
            if (event instanceof GameEvent.PlayerActed e && (e.action() == ActionType.BLIND_SM
                    || e.action() == ActionType.BLIND_BIG || e.action() == ActionType.ANTE)) {
                blindEvents.add(e);
            }
        });

        ServerTournamentDirector director = new ServerTournamentDirector(new TournamentEngine(eventBus, actionProvider),
                tournament, eventBus, actionProvider,
                new GameServerProperties(50, 30, 120, 10, 1000, 3, 2, 5, 5, 24, 7, "ws://localhost", 0), event -> {
                });

        Thread thread = new Thread(director);
        thread.start();
        thread.join(10000);

        assertThat(thread.isAlive()).isFalse();
        assertThat(tournament.isGameOver()).isTrue();

        // At least one hand must publish BLIND_SM, BLIND_BIG, and ANTE events.
        // Antes are configured at level 0, so the first hand always fires all three.
        assertThat(blindEvents).as("blind/ante events").isNotEmpty();
        assertThat(blindEvents).anySatisfy(e -> assertThat(e.action()).isEqualTo(ActionType.BLIND_SM));
        assertThat(blindEvents).anySatisfy(e -> assertThat(e.action()).isEqualTo(ActionType.BLIND_BIG));
        assertThat(blindEvents).anySatisfy(e -> assertThat(e.action()).isEqualTo(ActionType.ANTE));
    }

    /**
     * When the human player folds, the director should enable zip mode so the
     * remaining AI actions run without delay. Verifies that: 1. The game completes
     * correctly with a human player present. 2. Zip mode resets to false at the
     * start of each new hand (TD.DealDisplayHand).
     *
     * <p>
     * Uses a human player that always folds immediately. The inter-hand pause is
     * set to 50ms; with zip mode active it should be skipped, making the game
     * complete well within the timeout even with aiActionDelayMs > 0.
     */
    @Test
    void zipModeActivatesWhenHumanFoldsAndResetsEachHand() throws Exception {
        int delayMs = 50;

        // Mix: 1 human + 3 AI
        List<ServerPlayer> players = new ArrayList<>();
        ServerPlayer humanPlayer = new ServerPlayer(1, "Human", true, 0, 500);
        humanPlayer.setSeat(0);
        players.add(humanPlayer);
        for (int i = 1; i <= 3; i++) {
            ServerPlayer ai = new ServerPlayer(i + 1, "AI" + i, false, 5, 500);
            ai.setSeat(i);
            players.add(ai);
        }
        ServerTournamentContext tournament = createTournament(players, 1);

        InMemoryGameEventStore eventStore = new InMemoryGameEventStore("test-zip");
        ServerGameEventBus eventBus = new ServerGameEventBus(eventStore);

        // Use a holder so the fold callback can reference the provider it's constructed
        // into.
        final ServerPlayerActionProvider[] holder = {null};
        PlayerActionProvider aiProvider = createSimpleAI(77);
        holder[0] = new ServerPlayerActionProvider(aiProvider, request -> {
            // Human always folds immediately — called synchronously before future.get()
            holder[0].submitAction(request.player().getID(), PlayerAction.fold());
        }, 5, 2, new java.util.concurrent.ConcurrentHashMap<>(), delayMs);
        ServerPlayerActionProvider actionProvider = holder[0];

        // Track zip mode state at each HAND_STARTED (fires after setZipMode(false))
        List<Boolean> zipStatesAtHandStart = new CopyOnWriteArrayList<>();
        eventBus.subscribe(event -> {
            if (event instanceof GameEvent.HandStarted) {
                zipStatesAtHandStart.add(actionProvider.isZipMode());
            }
        });

        ServerTournamentDirector director = new ServerTournamentDirector(new TournamentEngine(eventBus, actionProvider),
                tournament, eventBus, actionProvider,
                new GameServerProperties(50, 30, 120, 10, 1000, 3, 2, 5, 5, 24, 7, "ws://localhost", delayMs),
                event -> {
                });

        Thread thread = new Thread(director);
        thread.start();
        // With zip mode working, even delayMs=50 should not make this slow —
        // the human folds every hand, so zip mode skips both per-action and inter-hand
        // delays.
        thread.join(30000);

        assertThat(thread.isAlive()).isFalse();
        assertThat(tournament.isGameOver()).isTrue();

        // Each HAND_STARTED fires after setZipMode(false), so zip mode must be false.
        assertThat(zipStatesAtHandStart).as("Zip mode at each hand start").isNotEmpty();
        assertThat(zipStatesAtHandStart).as("Zip mode always off at hand start")
                .allSatisfy(zipActive -> assertThat(zipActive).isFalse());
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
