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
package com.donohoedigital.games.poker.integration;

import com.donohoedigital.games.poker.HandAction;
import com.donohoedigital.games.poker.HoldemHand;
import com.donohoedigital.games.poker.PokerGame;
import com.donohoedigital.games.poker.PokerPlayer;
import com.donohoedigital.games.poker.PokerTable;
import com.donohoedigital.games.engine.DiceRoller;
import com.donohoedigital.games.poker.engine.PokerConstants;
import com.donohoedigital.games.poker.model.LevelAdvanceMode;
import com.donohoedigital.games.poker.model.TournamentProfile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test that runs a full poker tournament with all AI players,
 * exercising the real game engine: dealing, AI decision-making (V2Player), pot
 * resolution, and player elimination.
 *
 * <p>
 * This test drives the game loop manually (deal → betting → showdown → resolve)
 * using the same classes the real game uses: {@link HoldemHand},
 * {@link PokerPlayer}, and the production {@code PokerAI} implementations.
 * </p>
 *
 * <pre>
 * mvn test -pl poker -Dgroups=integration -Dtest=AllAITournamentSimulationTest
 * </pre>
 */
@Tag("slow")
class AllAITournamentSimulationTest extends IntegrationTestBase {

    private static final Logger logger = LogManager.getLogger(AllAITournamentSimulationTest.class);

    private static final int NUM_PLAYERS = 6;
    private static final int BUYIN_CHIPS = 1500;
    private static final int MAX_HANDS = 500;
    private static final int MAX_ACTIONS_PER_HAND = 200;

    /**
     * Tracks non-fold AI actions across a tournament to verify AI is actually
     * playing.
     */
    private int nonFoldActions;

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void should_CompleteFullTournament() {
        DiceRoller.setSeed(42);
        PokerGame game = createAndInitTournament();
        int expectedTotalChips = NUM_PLAYERS * BUYIN_CHIPS;

        TournamentResult result = playTournament(game);

        assertThat(result.winner()).isNotNull();
        assertThat(result.winner().getChipCount()).isEqualTo(expectedTotalChips);
        assertThat(result.handsPlayed()).isGreaterThan(0);
        assertThat(countPlayersWithChips(game)).isEqualTo(1);
        // Verify AI is actually playing, not silently falling back to fold
        assertThat(result.nonFoldActions()).as("AI non-fold actions").isGreaterThan(0);

        logger.info("Tournament complete: {} won with ${} after {} hands ({} non-fold actions)",
                result.winner().getName(), result.winner().getChipCount(), result.handsPlayed(),
                result.nonFoldActions());
    }

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void should_CompleteMultipleIndependentTournaments() {
        // AI uses Math.random() internally (AIOutcome, BetRange), so exact
        // determinism isn't possible with DiceRoller seed alone. Instead,
        // verify that multiple independent tournaments all complete correctly.
        int expectedTotalChips = NUM_PLAYERS * BUYIN_CHIPS;

        for (int run = 0; run < 3; run++) {
            DiceRoller.setSeed(12345 + run);
            PokerGame game = createAndInitTournament();
            TournamentResult result = playTournament(game);

            assertThat(result.winner()).as("Run %d winner", run).isNotNull();
            assertThat(result.winner().getChipCount()).as("Run %d chips", run).isEqualTo(expectedTotalChips);
            assertThat(result.handsPlayed()).as("Run %d hands", run).isGreaterThan(0);
            assertThat(result.nonFoldActions()).as("Run %d AI non-fold actions", run).isGreaterThan(0);

            logger.info("Run {}: {} won with ${} after {} hands ({} non-fold actions)", run, result.winner().getName(),
                    result.winner().getChipCount(), result.handsPlayed(), result.nonFoldActions());
        }
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void should_EliminatePlayers_AsTournamentProgresses() {
        DiceRoller.setSeed(99);
        PokerGame game = createAndInitTournament();
        PokerTable table = game.getTable(0);

        int initialPlayers = table.getNumOccupiedSeats();
        assertThat(initialPlayers).isEqualTo(NUM_PLAYERS);

        // Play hands until at least one player is eliminated
        int handsPlayed = 0;
        while (countPlayersWithChips(game) >= NUM_PLAYERS && handsPlayed < MAX_HANDS) {
            playHand(game, table);
            handsPlayed++;
        }

        assertThat(countPlayersWithChips(game)).isLessThan(NUM_PLAYERS);
        logger.info("First elimination after {} hands, {} players remain", handsPlayed, countPlayersWithChips(game));
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void should_ConserveChips_AcrossAllHands() {
        DiceRoller.setSeed(777);
        PokerGame game = createAndInitTournament();
        PokerTable table = game.getTable(0);
        int expectedTotalChips = NUM_PLAYERS * BUYIN_CHIPS;

        for (int i = 0; i < 20 && countPlayersWithChips(game) > 1; i++) {
            playHand(game, table);
            int totalChips = countTotalChips(table);
            assertThat(totalChips).as("Chip conservation after hand %d", i + 1).isEqualTo(expectedTotalChips);
        }
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void should_RecordHandHistory() {
        DiceRoller.setSeed(555);
        PokerGame game = createAndInitTournament();
        PokerTable table = game.getTable(0);

        playHand(game, table);

        HoldemHand hand = table.getHoldemHand();
        assertThat(hand).isNotNull();
        assertThat(hand.getHistorySize()).isGreaterThan(0);
        assertThat(table.getHandNum()).isEqualTo(1);
    }

    // =========================================================================
    // Tournament Setup
    // =========================================================================

    private PokerGame createAndInitTournament() {
        PokerGame game = new PokerGame(null);

        // Add a human player placeholder (required by initTournament to have at least
        // one player)
        // Use "false" for isHuman since we want all AI
        PokerPlayer host = new PokerPlayer("test-key", game.getNextPlayerID(), "AI Host", false);
        game.addPlayer(host);

        TournamentProfile profile = createTournamentProfile();
        game.initTournament(profile);

        // Mark the first table as current — this triggers AI initialization for
        // all-computer tables
        PokerTable table = game.getTable(0);
        game.setCurrentTable(table);

        // Set button so we can deal
        table.setButton();

        logger.info("Tournament initialized: {} players, ${} buyin, {} tables", game.getNumPlayers(), BUYIN_CHIPS,
                game.getNumTables());

        return game;
    }

    private TournamentProfile createTournamentProfile() {
        TournamentProfile profile = new TournamentProfile();
        profile.setName("AI Simulation Test");
        profile.setNumPlayers(NUM_PLAYERS);
        profile.setBuyinChips(BUYIN_CHIPS);

        // Use hands-based level advancement so blinds increase without a clock
        profile.setLevelAdvanceMode(LevelAdvanceMode.HANDS);
        profile.setHandsPerLevel(10);

        // Aggressive blind structure to ensure tournament completes quickly
        profile.setLevel(1, 0, 25, 50, 0); // Level 1: 25/50
        profile.setLevel(2, 0, 50, 100, 0); // Level 2: 50/100
        profile.setLevel(3, 25, 75, 150, 0); // Level 3: 75/150 + 25 ante
        profile.setLevel(4, 50, 100, 200, 0); // Level 4: 100/200 + 50 ante
        profile.setLevel(5, 75, 150, 300, 0); // Level 5: 150/300 + 75 ante

        // Tell BlindStructure how many levels are defined; without this, all
        // blinds resolve to 0 because lastlevel defaults to 0.
        profile.getMap().setInteger(TournamentProfile.PARAM_LASTLEVEL, 5);
        // Double blinds each level beyond the last defined level
        profile.getMap().setBoolean("doubleafterlast", true);

        return profile;
    }

    // =========================================================================
    // Game Loop
    // =========================================================================

    private TournamentResult playTournament(PokerGame game) {
        PokerTable table = game.getTable(0);
        int expectedTotalChips = NUM_PLAYERS * BUYIN_CHIPS;
        int handsPlayed = 0;
        nonFoldActions = 0;

        while (countPlayersWithChips(game) > 1 && handsPlayed < MAX_HANDS) {
            playHand(game, table);

            // Verify chip conservation before elimination
            int chipsBeforeElimination = countTotalChips(table);
            assertThat(chipsBeforeElimination)
                    .as("Chip conservation after hand %d (before elimination)", handsPlayed + 1)
                    .isEqualTo(expectedTotalChips);

            eliminateBustedPlayers(game, table);
            handsPlayed++;

            if (handsPlayed % 50 == 0) {
                logger.info("Hand {}: {} players remaining, chips at table: {}", handsPlayed,
                        countPlayersWithChips(game), countTotalChips(table));
            }
        }

        PokerPlayer winner = findWinner(game);

        if (winner == null && handsPlayed >= MAX_HANDS) {
            fail("Tournament did not complete after " + MAX_HANDS + " hands. " + countPlayersWithChips(game)
                    + " players still have chips.");
        }

        return new TournamentResult(winner, handsPlayed, nonFoldActions);
    }

    private void playHand(PokerGame game, PokerTable table) {
        // Skip if only one player left
        if (table.getNumOccupiedSeats() < 2) {
            return;
        }

        // Deal new hand
        table.startNewHand();
        HoldemHand hand = table.getHoldemHand();

        // Process betting rounds
        processBettingRounds(hand);

        // Deal remaining community cards if needed (required for hand evaluation in
        // resolve)
        while (hand.getRound().toLegacy() < HoldemHand.ROUND_RIVER) {
            hand.advanceRound();
        }

        // Advance to showdown round
        hand.advanceRound(); // RIVER -> SHOWDOWN

        // Resolve hand — determines winners, awards pots
        hand.preResolve(false);
        hand.resolve();
    }

    private void processBettingRounds(HoldemHand hand) {
        int actionCount = 0;

        // Process pre-flop through river
        while (hand.getRound().toLegacy() < HoldemHand.ROUND_SHOWDOWN) {

            // Process all actions in this round
            while (!hand.isDone()) {
                PokerPlayer current = hand.getCurrentPlayerWithInit();
                if (current == null) {
                    break;
                }

                HandAction action = current.getAction(false);
                applyAction(current, action);

                actionCount++;
                if (actionCount > MAX_ACTIONS_PER_HAND) {
                    logger.warn("Max actions reached in hand, forcing completion");
                    return;
                }
            }

            // If only one player left, stop
            if (hand.isUncontested()) {
                break;
            }

            // If all players have acted and betting is done, advance to next round
            if (hand.getRound().toLegacy() < HoldemHand.ROUND_RIVER) {
                hand.advanceRound();
            } else {
                // River betting complete
                break;
            }
        }
    }

    private void applyAction(PokerPlayer player, HandAction action) {
        // Route through PokerPlayer so chips are properly deducted before the
        // hand history entry is recorded. Calling HoldemHand methods directly
        // skips the chip deduction and breaks chip conservation invariants.
        boolean isFold = action.getAction() == HandAction.ACTION_FOLD;
        player.processAction(action);
        if (!isFold) {
            nonFoldActions++;
        }
    }

    // =========================================================================
    // Player Management
    // =========================================================================

    private void eliminateBustedPlayers(PokerGame game, PokerTable table) {
        List<Integer> seatsToRemove = new ArrayList<>();

        for (int i = 0; i < PokerConstants.SEATS; i++) {
            PokerPlayer player = table.getPlayer(i);
            if (player != null && player.getChipCount() == 0 && !player.isEliminated()) {
                game.playerOut(player);
                seatsToRemove.add(i);
            }
        }

        for (int seat : seatsToRemove) {
            table.removePlayer(seat);
        }
    }

    private int countPlayersWithChips(PokerGame game) {
        int count = 0;
        for (int i = 0; i < game.getNumPlayers(); i++) {
            PokerPlayer player = game.getPokerPlayerAt(i);
            if (player.getChipCount() > 0 && !player.isEliminated()) {
                count++;
            }
        }
        return count;
    }

    private int countTotalChips(PokerTable table) {
        int total = 0;
        for (int i = 0; i < PokerConstants.SEATS; i++) {
            PokerPlayer player = table.getPlayer(i);
            if (player != null) {
                total += player.getChipCount();
            }
        }
        return total;
    }

    private PokerPlayer findWinner(PokerGame game) {
        for (int i = 0; i < game.getNumPlayers(); i++) {
            PokerPlayer player = game.getPokerPlayerAt(i);
            if (player.getChipCount() > 0 && !player.isEliminated()) {
                return player;
            }
        }
        return null;
    }

    // =========================================================================
    // Result Record
    // =========================================================================

    private record TournamentResult(PokerPlayer winner, int handsPlayed, int nonFoldActions) {
    }
}
