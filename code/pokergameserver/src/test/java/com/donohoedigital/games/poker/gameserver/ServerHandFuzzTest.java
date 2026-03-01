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

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;

import com.donohoedigital.games.poker.core.PlayerAction;
import com.donohoedigital.games.poker.core.state.BettingRound;

/**
 * Fuzz testing suite for ServerHand. Generates random-but-legal game setups and
 * action sequences, asserting invariants (chip conservation, no negative chips,
 * clean hand termination) after every hand.
 *
 * <p>
 * Every test is seeded deterministically so failures are reproducible.
 */
class ServerHandFuzzTest {

    /** Base seed combined with repetition number for deterministic randomness. */
    private static final long BASE_SEED = 0xDEAD_BEEF_CAFE_1234L;

    /** Maximum actions per betting round before we bail out (safety). */
    private static final int MAX_ACTIONS_PER_HAND = 100;

    /** Maximum hands per game (safety). */
    private static final int MAX_HANDS_PER_GAME = 100;

    // ============================== Infrastructure ==============================

    /**
     * Mock table for testing. Implements ServerHand.MockTable interface.
     */
    private static class MockServerGameTable implements ServerHand.MockTable {
        private final ServerPlayer[] seats;
        private final int numSeats;
        private int button;

        MockServerGameTable(int numSeats) {
            this.numSeats = numSeats;
            this.seats = new ServerPlayer[numSeats];
        }

        void addPlayer(ServerPlayer player, int seat) {
            seats[seat] = player;
        }

        @Override
        public ServerPlayer getPlayer(int seat) {
            return seats[seat];
        }

        @Override
        public int getNumSeats() {
            return numSeats;
        }

        @Override
        public int getButton() {
            return button;
        }

        void setButton(int button) {
            this.button = button;
        }
    }

    /**
     * Generate a legal action for the current player given the hand state.
     *
     * <p>
     * Rules:
     * <ul>
     * <li>Facing a bet (amountToCall &gt; 0): fold (30%), call (40%), raise (30%)
     * <li>No bet facing: check (50%), bet (50%)
     * <li>10% chance of all-in on any bet/raise
     * <li>If player can't afford minRaise, they go all-in with whatever they have
     * </ul>
     */
    private PlayerAction generateLegalAction(ServerHand hand, ServerPlayer player, Random rng) {
        int amountToCall = hand.getAmountToCall(player);
        int chips = player.getChipCount();

        if (chips == 0) {
            // Player has no chips (already all-in from blind/ante) — check or fold
            return amountToCall > 0 ? PlayerAction.fold() : PlayerAction.check();
        }

        if (amountToCall > 0) {
            // Facing a bet
            int roll = rng.nextInt(100);
            if (roll < 30) {
                return PlayerAction.fold();
            } else if (roll < 70) {
                // Call — applyPlayerAction handles all-in clamping
                return PlayerAction.call();
            } else {
                // Raise
                return generateRaise(hand, player, rng);
            }
        } else {
            // No bet facing (post-flop or BB option)
            int roll = rng.nextInt(100);
            if (roll < 50) {
                return PlayerAction.check();
            } else {
                // Bet
                return generateBet(hand, player, rng);
            }
        }
    }

    /**
     * Generate a raise action. The amount is the ADDITIONAL chips to add (not
     * total). Min additional = amountToCall + bigBlind. If player can't afford the
     * minimum, go all-in.
     */
    private PlayerAction generateRaise(ServerHand hand, ServerPlayer player, Random rng) {
        int chips = player.getChipCount();
        int amountToCall = hand.getAmountToCall(player);
        int minRaiseTotal = hand.getMinRaise();

        // playerBet = currentBet - amountToCall
        // minAdditional = minRaiseTotal - (currentBet - amountToCall) = minRaiseTotal -
        // currentBet + amountToCall
        // But simpler: to reach minRaiseTotal, player needs (minRaiseTotal - playerBet)
        // additional chips
        // playerBet is what the player already has in this round
        // amountToCall = currentBet - playerBet => playerBet = currentBet -
        // amountToCall
        // minAdditional = minRaiseTotal - (currentBet - amountToCall) = minRaiseTotal -
        // currentBet + amountToCall
        // Since minRaiseTotal = currentBet + bigBlind:
        // minAdditional = currentBet + bigBlind - currentBet + amountToCall = bigBlind
        // + amountToCall
        int minAdditional = amountToCall + hand.getMinBet();

        if (chips <= minAdditional) {
            // All-in — put in everything
            return PlayerAction.raise(chips);
        }

        // 10% chance of all-in
        if (rng.nextInt(100) < 10) {
            return PlayerAction.raise(chips);
        }

        // Random raise between minAdditional and all chips
        int raiseAmount = minAdditional + rng.nextInt(chips - minAdditional + 1);
        return PlayerAction.raise(raiseAmount);
    }

    /**
     * Generate a bet action (when no bet is facing). Amount must be at least minBet
     * (= big blind). If player can't afford minBet, go all-in.
     */
    private PlayerAction generateBet(ServerHand hand, ServerPlayer player, Random rng) {
        int chips = player.getChipCount();
        int minBet = hand.getMinBet();

        if (chips <= minBet) {
            // All-in
            return PlayerAction.bet(chips);
        }

        // 10% chance of all-in
        if (rng.nextInt(100) < 10) {
            return PlayerAction.bet(chips);
        }

        // Random bet between minBet and all chips
        int betAmount = minBet + rng.nextInt(chips - minBet + 1);
        return PlayerAction.bet(betAmount);
    }

    /**
     * Play one complete hand with random-but-legal actions.
     *
     * @return true if the hand completed successfully, false if safety counter
     *         tripped
     */
    private boolean playOneHand(ServerHand hand, List<ServerPlayer> players, Random rng) {
        hand.deal();

        // Play through betting rounds
        while (!hand.isUncontested()) {
            // Process current betting round
            int safetyCounter = 0;
            while (!hand.isDone() && safetyCounter < MAX_ACTIONS_PER_HAND) {
                ServerPlayer current = (ServerPlayer) hand.getCurrentPlayerWithInit();
                if (current == null)
                    break;

                PlayerAction action = generateLegalAction(hand, current, rng);
                hand.applyPlayerAction(current, action);
                safetyCounter++;
            }

            if (hand.isUncontested())
                break;
            if (hand.getRound() == BettingRound.RIVER)
                break;
            hand.advanceRound();
        }

        // Complete remaining streets (for showdown)
        while (hand.getRound() != BettingRound.SHOWDOWN) {
            hand.advanceRound();
        }

        hand.resolve();
        return true;
    }

    /**
     * Assert invariants that must hold after every resolved hand.
     */
    private void assertInvariants(ServerHand hand, List<ServerPlayer> players, int expectedTotalChips, int handNum,
            long seed) {
        String ctx = "hand " + handNum + " (seed=" + seed + ")";

        // 1. Chip conservation
        int actualTotal = players.stream().mapToInt(ServerPlayer::getChipCount).sum();
        assertEquals(expectedTotalChips, actualTotal, "Chip conservation violated after " + ctx + ": expected="
                + expectedTotalChips + " actual=" + actualTotal);

        // 2. No negative chips
        for (ServerPlayer p : players) {
            assertTrue(p.getChipCount() >= 0,
                    p.getName() + " has negative chips (" + p.getChipCount() + ") after " + ctx);
        }

        // 3. Hand is done
        assertTrue(hand.isDone(), "Hand should be done after resolve (" + ctx + ")");
    }

    /**
     * Create a fresh table and players for a new hand. Reuses existing player
     * objects but resets their fold/allIn state.
     */
    private MockServerGameTable setupTable(List<ServerPlayer> players, int button) {
        int numPlayers = players.size();
        MockServerGameTable table = new MockServerGameTable(numPlayers);
        table.setButton(button);
        for (int i = 0; i < numPlayers; i++) {
            ServerPlayer p = players.get(i);
            p.setFolded(false);
            p.setAllIn(false);
            table.addPlayer(p, i);
        }
        return table;
    }

    /**
     * Calculate blind seats for a given button position and player count. For
     * heads-up: SB = button, BB = (button+1) % N. For 3+: SB = (button+1) % N, BB =
     * (button+2) % N.
     */
    private int[] blindSeats(int button, int numPlayers) {
        if (numPlayers == 2) {
            return new int[]{button, (button + 1) % numPlayers};
        }
        return new int[]{(button + 1) % numPlayers, (button + 2) % numPlayers};
    }

    // ============================== Test Methods ==============================

    /**
     * Random player count (2-8), random stacks (500-10000), random blinds. Play 5
     * hands per iteration. Assert invariants after each hand.
     */
    @RepeatedTest(200)
    void fuzz_randomGames_chipConservation(RepetitionInfo info) {
        long seed = BASE_SEED + info.getCurrentRepetition();
        Random rng = new Random(seed);

        int numPlayers = 2 + rng.nextInt(7); // 2-8
        int bigBlind = (1 + rng.nextInt(10)) * 10; // 10-100 in steps of 10
        int smallBlind = bigBlind / 2;
        int ante = rng.nextInt(3) == 0 ? bigBlind / 4 : 0; // 33% chance of ante

        // Create players with random stacks
        List<ServerPlayer> players = new ArrayList<>();
        for (int i = 0; i < numPlayers; i++) {
            int stack = 500 + rng.nextInt(9501); // 500-10000
            ServerPlayer p = new ServerPlayer(i + 1, "P" + (i + 1), false, 0, stack);
            p.setSeat(i);
            players.add(p);
        }

        int expectedTotal = players.stream().mapToInt(ServerPlayer::getChipCount).sum();

        // Play 5 hands, rotating button
        for (int h = 0; h < 5; h++) {
            // Skip if fewer than 2 players have chips
            long playersWithChips = players.stream().filter(p -> p.getChipCount() > 0).count();
            if (playersWithChips < 2)
                break;

            int button = h % numPlayers;
            int[] blinds = blindSeats(button, numPlayers);

            // Skip if blind players have 0 chips — find next valid button
            if (players.get(blinds[0]).getChipCount() == 0 || players.get(blinds[1]).getChipCount() == 0) {
                continue;
            }

            MockServerGameTable table = setupTable(players, button);
            ServerHand hand = new ServerHand(table, h + 1, smallBlind, bigBlind, ante, button, blinds[0], blinds[1]);

            playOneHand(hand, players, rng);
            assertInvariants(hand, players, expectedTotal, h + 1, seed);
        }
    }

    /**
     * Always 2 players (heads-up). Stack ratios from 1:1 to 50:1. Play 5 hands per
     * iteration.
     */
    @RepeatedTest(200)
    void fuzz_headsUp_allVariations(RepetitionInfo info) {
        long seed = BASE_SEED + info.getCurrentRepetition();
        Random rng = new Random(seed);

        int bigBlind = (1 + rng.nextInt(5)) * 20; // 20-100 in steps of 20
        int smallBlind = bigBlind / 2;
        int ante = rng.nextInt(4) == 0 ? bigBlind / 4 : 0; // 25% chance of ante

        // Stack ratio from 1:1 to 50:1
        int baseStack = 500 + rng.nextInt(2001); // 500-2500
        int ratio = 1 + rng.nextInt(50); // 1-50
        int stack1 = baseStack;
        int stack2 = Math.max(bigBlind, baseStack / ratio); // At least big blind

        List<ServerPlayer> players = new ArrayList<>();
        ServerPlayer p1 = new ServerPlayer(1, "HU1", false, 0, stack1);
        ServerPlayer p2 = new ServerPlayer(2, "HU2", false, 0, stack2);
        p1.setSeat(0);
        p2.setSeat(1);
        players.add(p1);
        players.add(p2);

        int expectedTotal = p1.getChipCount() + p2.getChipCount();

        for (int h = 0; h < 5; h++) {
            // Stop if one player is busted
            if (p1.getChipCount() == 0 || p2.getChipCount() == 0)
                break;

            int button = h % 2;
            // Heads-up: SB = button, BB = other
            int sbSeat = button;
            int bbSeat = (button + 1) % 2;

            MockServerGameTable table = setupTable(players, button);
            ServerHand hand = new ServerHand(table, h + 1, smallBlind, bigBlind, ante, button, sbSeat, bbSeat);

            playOneHand(hand, players, rng);
            assertInvariants(hand, players, expectedTotal, h + 1, seed);
        }
    }

    /**
     * 3-6 players, all equal stacks. 50% chance of all-in on any bet/raise action.
     * Play until someone busts or 10 hands.
     */
    @RepeatedTest(100)
    void fuzz_allSameStack_allIns(RepetitionInfo info) {
        long seed = BASE_SEED + info.getCurrentRepetition();
        Random rng = new Random(seed);

        int numPlayers = 3 + rng.nextInt(4); // 3-6
        int stack = 500 + rng.nextInt(4501); // 500-5000
        int bigBlind = Math.max(10, stack / (10 + rng.nextInt(20))); // ~2-10% of stack
        int smallBlind = bigBlind / 2;

        List<ServerPlayer> players = new ArrayList<>();
        for (int i = 0; i < numPlayers; i++) {
            ServerPlayer p = new ServerPlayer(i + 1, "AI" + (i + 1), false, 0, stack);
            p.setSeat(i);
            players.add(p);
        }

        int expectedTotal = players.stream().mapToInt(ServerPlayer::getChipCount).sum();

        for (int h = 0; h < 10; h++) {
            // Stop if fewer than 2 players have chips
            long playersWithChips = players.stream().filter(p -> p.getChipCount() > 0).count();
            if (playersWithChips < 2)
                break;

            int button = h % numPlayers;
            int[] blinds = blindSeats(button, numPlayers);

            // Skip if blind players busted
            if (players.get(blinds[0]).getChipCount() == 0 || players.get(blinds[1]).getChipCount() == 0) {
                continue;
            }

            MockServerGameTable table = setupTable(players, button);
            ServerHand hand = new ServerHand(table, h + 1, smallBlind, bigBlind, 0, button, blinds[0], blinds[1]);

            playOneHandAggressive(hand, players, rng);
            assertInvariants(hand, players, expectedTotal, h + 1, seed);
        }
    }

    /**
     * Like {@link #playOneHand} but with 50% chance of all-in on any bet/raise.
     */
    private boolean playOneHandAggressive(ServerHand hand, List<ServerPlayer> players, Random rng) {
        hand.deal();

        while (!hand.isUncontested()) {
            int safetyCounter = 0;
            while (!hand.isDone() && safetyCounter < MAX_ACTIONS_PER_HAND) {
                ServerPlayer current = (ServerPlayer) hand.getCurrentPlayerWithInit();
                if (current == null)
                    break;

                PlayerAction action = generateAggressiveAction(hand, current, rng);
                hand.applyPlayerAction(current, action);
                safetyCounter++;
            }

            if (hand.isUncontested())
                break;
            if (hand.getRound() == BettingRound.RIVER)
                break;
            hand.advanceRound();
        }

        while (hand.getRound() != BettingRound.SHOWDOWN) {
            hand.advanceRound();
        }

        hand.resolve();
        return true;
    }

    /**
     * Aggressive action generator: 50% chance of all-in on any bet/raise.
     */
    private PlayerAction generateAggressiveAction(ServerHand hand, ServerPlayer player, Random rng) {
        int amountToCall = hand.getAmountToCall(player);
        int chips = player.getChipCount();

        if (chips == 0) {
            return amountToCall > 0 ? PlayerAction.fold() : PlayerAction.check();
        }

        if (amountToCall > 0) {
            int roll = rng.nextInt(100);
            if (roll < 20) {
                return PlayerAction.fold();
            } else if (roll < 40) {
                return PlayerAction.call();
            } else {
                // 60% raise — with 50% chance all-in
                if (rng.nextBoolean()) {
                    return PlayerAction.raise(chips); // All-in
                }
                return generateRaise(hand, player, rng);
            }
        } else {
            int roll = rng.nextInt(100);
            if (roll < 30) {
                return PlayerAction.check();
            } else {
                // 70% bet — with 50% chance all-in
                if (rng.nextBoolean()) {
                    return PlayerAction.bet(chips); // All-in
                }
                return generateBet(hand, player, rng);
            }
        }
    }

    /**
     * Passive action generator: always check (if possible) or call (if facing a
     * bet). Never fold, bet, or raise.
     */
    private PlayerAction generatePassiveAction(ServerHand hand, ServerPlayer player) {
        int amountToCall = hand.getAmountToCall(player);

        if (amountToCall > 0) {
            return PlayerAction.call();
        }
        return PlayerAction.check();
    }

    /**
     * Play one complete hand where all players check or call only (passive play).
     */
    private boolean playOneHandPassive(ServerHand hand, List<ServerPlayer> players) {
        hand.deal();

        while (!hand.isUncontested()) {
            int safetyCounter = 0;
            while (!hand.isDone() && safetyCounter < MAX_ACTIONS_PER_HAND) {
                ServerPlayer current = (ServerPlayer) hand.getCurrentPlayerWithInit();
                if (current == null)
                    break;

                PlayerAction action = generatePassiveAction(hand, current);
                hand.applyPlayerAction(current, action);
                safetyCounter++;
            }

            if (hand.isUncontested())
                break;
            if (hand.getRound() == BettingRound.RIVER)
                break;
            hand.advanceRound();
        }

        while (hand.getRound() != BettingRound.SHOWDOWN) {
            hand.advanceRound();
        }

        hand.resolve();
        return true;
    }

    // ====================== Additional Edge-Case Fuzz Tests ======================

    /**
     * Half micro-stacks (1-2 BB), half deep-stacks (50-100 BB). 4-6 players.
     * Stresses all-in side pot calculation with extreme stack disparity.
     */
    @RepeatedTest(100)
    void fuzz_microStacks_vs_deepStacks(RepetitionInfo info) {
        long seed = BASE_SEED + 40000 + info.getCurrentRepetition();
        Random rng = new Random(seed);

        int numPlayers = 4 + rng.nextInt(3); // 4-6
        int bigBlind = 100;
        int smallBlind = 50;

        List<ServerPlayer> players = new ArrayList<>();
        for (int i = 0; i < numPlayers; i++) {
            int stack;
            if (i < numPlayers / 2) {
                // Micro-stack: 1-2 big blinds (100-200 chips)
                stack = bigBlind + rng.nextInt(bigBlind + 1); // 100-200
            } else {
                // Deep stack: 50-100 big blinds (5000-10000 chips)
                stack = 50 * bigBlind + rng.nextInt(51 * bigBlind); // 5000-10000
            }
            ServerPlayer p = new ServerPlayer(i + 1, "P" + (i + 1), false, 0, stack);
            p.setSeat(i);
            players.add(p);
        }

        int expectedTotal = players.stream().mapToInt(ServerPlayer::getChipCount).sum();

        for (int h = 0; h < 5; h++) {
            long playersWithChips = players.stream().filter(p -> p.getChipCount() > 0).count();
            if (playersWithChips < 2)
                break;

            int button = h % numPlayers;
            int[] blinds = blindSeats(button, numPlayers);

            if (players.get(blinds[0]).getChipCount() == 0 || players.get(blinds[1]).getChipCount() == 0) {
                continue;
            }

            MockServerGameTable table = setupTable(players, button);
            ServerHand hand = new ServerHand(table, h + 1, smallBlind, bigBlind, 0, button, blinds[0], blinds[1]);

            playOneHand(hand, players, rng);
            assertInvariants(hand, players, expectedTotal, h + 1, seed);
        }
    }

    /**
     * 4 players, starting stacks 1000. Blinds escalate by 50% each hand starting at
     * 10/20. By hand 10 blinds exceed any stack, forcing all-ins from blinds.
     */
    @RepeatedTest(100)
    void fuzz_rapidBlindEscalation(RepetitionInfo info) {
        long seed = BASE_SEED + 50000 + info.getCurrentRepetition();
        Random rng = new Random(seed);

        int numPlayers = 4;
        int startingStack = 1000;
        int baseSB = 10;

        List<ServerPlayer> players = new ArrayList<>();
        for (int i = 0; i < numPlayers; i++) {
            ServerPlayer p = new ServerPlayer(i + 1, "P" + (i + 1), false, 0, startingStack);
            p.setSeat(i);
            players.add(p);
        }

        int expectedTotal = players.stream().mapToInt(ServerPlayer::getChipCount).sum();

        for (int h = 0; h < 10; h++) {
            long playersWithChips = players.stream().filter(p -> p.getChipCount() > 0).count();
            if (playersWithChips < 2)
                break;

            int smallBlind = (int) (baseSB * Math.pow(1.5, h));
            int bigBlind = smallBlind * 2;

            int button = h % numPlayers;
            int[] blinds = blindSeats(button, numPlayers);

            if (players.get(blinds[0]).getChipCount() == 0 || players.get(blinds[1]).getChipCount() == 0) {
                continue;
            }

            MockServerGameTable table = setupTable(players, button);
            ServerHand hand = new ServerHand(table, h + 1, smallBlind, bigBlind, 0, button, blinds[0], blinds[1]);

            playOneHand(hand, players, rng);
            assertInvariants(hand, players, expectedTotal, h + 1, seed);
        }
    }

    /**
     * 2 players, random stacks (500-2000). Play until one player busts or 100
     * hands. Assert that winner has all the chips.
     */
    @RepeatedTest(100)
    void fuzz_playToBust_headsUp(RepetitionInfo info) {
        long seed = BASE_SEED + 60000 + info.getCurrentRepetition();
        Random rng = new Random(seed);

        int bigBlind = 20 + rng.nextInt(5) * 10; // 20-60
        int smallBlind = bigBlind / 2;

        int stack1 = 500 + rng.nextInt(1501); // 500-2000
        int stack2 = 500 + rng.nextInt(1501); // 500-2000

        List<ServerPlayer> players = new ArrayList<>();
        ServerPlayer p1 = new ServerPlayer(1, "HU1", false, 0, stack1);
        ServerPlayer p2 = new ServerPlayer(2, "HU2", false, 0, stack2);
        p1.setSeat(0);
        p2.setSeat(1);
        players.add(p1);
        players.add(p2);

        int expectedTotal = p1.getChipCount() + p2.getChipCount();
        boolean busted = false;

        for (int h = 0; h < MAX_HANDS_PER_GAME; h++) {
            if (p1.getChipCount() == 0 || p2.getChipCount() == 0) {
                busted = true;
                break;
            }

            int button = h % 2;
            int sbSeat = button;
            int bbSeat = (button + 1) % 2;

            MockServerGameTable table = setupTable(players, button);
            ServerHand hand = new ServerHand(table, h + 1, smallBlind, bigBlind, 0, button, sbSeat, bbSeat);

            playOneHand(hand, players, rng);
            assertInvariants(hand, players, expectedTotal, h + 1, seed);
        }

        // Check termination condition after loop
        if (p1.getChipCount() == 0 || p2.getChipCount() == 0) {
            busted = true;
        }

        assertTrue(busted, "Game did not terminate within " + MAX_HANDS_PER_GAME + " hands (seed=" + seed + ")");

        // Winner has all the chips
        ServerPlayer winner = p1.getChipCount() > 0 ? p1 : p2;
        assertEquals(expectedTotal, winner.getChipCount(), "Winner should have all chips (seed=" + seed + ")");
    }

    /**
     * 9 players (full table), 1000 chips each, blinds 25/50. Play 3 hands. Stresses
     * multi-way pot splitting with max players.
     */
    @RepeatedTest(100)
    void fuzz_maxPlayers_fullTable(RepetitionInfo info) {
        long seed = BASE_SEED + 70000 + info.getCurrentRepetition();
        Random rng = new Random(seed);

        int numPlayers = 9;
        int stack = 1000;
        int bigBlind = 50;
        int smallBlind = 25;

        List<ServerPlayer> players = new ArrayList<>();
        for (int i = 0; i < numPlayers; i++) {
            ServerPlayer p = new ServerPlayer(i + 1, "P" + (i + 1), false, 0, stack);
            p.setSeat(i);
            players.add(p);
        }

        int expectedTotal = players.stream().mapToInt(ServerPlayer::getChipCount).sum();

        for (int h = 0; h < 3; h++) {
            long playersWithChips = players.stream().filter(p -> p.getChipCount() > 0).count();
            if (playersWithChips < 2)
                break;

            int button = h % numPlayers;
            int[] blinds = blindSeats(button, numPlayers);

            if (players.get(blinds[0]).getChipCount() == 0 || players.get(blinds[1]).getChipCount() == 0) {
                continue;
            }

            MockServerGameTable table = setupTable(players, button);
            ServerHand hand = new ServerHand(table, h + 1, smallBlind, bigBlind, 0, button, blinds[0], blinds[1]);

            playOneHand(hand, players, rng);
            assertInvariants(hand, players, expectedTotal, h + 1, seed);
        }
    }

    /**
     * 3-6 players with antes set to 20-50% of the big blind. Play 5 hands. Antes
     * must be included in chip conservation.
     */
    @RepeatedTest(100)
    void fuzz_antesWithBlinds(RepetitionInfo info) {
        long seed = BASE_SEED + 80000 + info.getCurrentRepetition();
        Random rng = new Random(seed);

        int numPlayers = 3 + rng.nextInt(4); // 3-6
        int bigBlind = (2 + rng.nextInt(9)) * 10; // 20-100
        int smallBlind = bigBlind / 2;
        // Ante = 20-50% of big blind
        int ante = bigBlind / 5 + rng.nextInt(bigBlind * 3 / 10 + 1); // ~20-50% of BB

        List<ServerPlayer> players = new ArrayList<>();
        for (int i = 0; i < numPlayers; i++) {
            int stack = 500 + rng.nextInt(4501); // 500-5000
            ServerPlayer p = new ServerPlayer(i + 1, "P" + (i + 1), false, 0, stack);
            p.setSeat(i);
            players.add(p);
        }

        int expectedTotal = players.stream().mapToInt(ServerPlayer::getChipCount).sum();

        for (int h = 0; h < 5; h++) {
            long playersWithChips = players.stream().filter(p -> p.getChipCount() > 0).count();
            if (playersWithChips < 2)
                break;

            int button = h % numPlayers;
            int[] blinds = blindSeats(button, numPlayers);

            if (players.get(blinds[0]).getChipCount() == 0 || players.get(blinds[1]).getChipCount() == 0) {
                continue;
            }

            MockServerGameTable table = setupTable(players, button);
            ServerHand hand = new ServerHand(table, h + 1, smallBlind, bigBlind, ante, button, blinds[0], blinds[1]);

            playOneHand(hand, players, rng);
            assertInvariants(hand, players, expectedTotal, h + 1, seed);
        }
    }

    /**
     * 3-5 players, 2000 chips each, blinds 50/100. All players check or call only.
     * Every hand goes to showdown with max players (maximum showdown stress test).
     */
    @RepeatedTest(100)
    void fuzz_allCheckToShowdown(RepetitionInfo info) {
        long seed = BASE_SEED + 90000 + info.getCurrentRepetition();
        Random rng = new Random(seed);

        int numPlayers = 3 + rng.nextInt(3); // 3-5
        int stack = 2000;
        int bigBlind = 100;
        int smallBlind = 50;

        List<ServerPlayer> players = new ArrayList<>();
        for (int i = 0; i < numPlayers; i++) {
            ServerPlayer p = new ServerPlayer(i + 1, "P" + (i + 1), false, 0, stack);
            p.setSeat(i);
            players.add(p);
        }

        int expectedTotal = players.stream().mapToInt(ServerPlayer::getChipCount).sum();

        for (int h = 0; h < 10; h++) {
            long playersWithChips = players.stream().filter(p -> p.getChipCount() > 0).count();
            if (playersWithChips < 2)
                break;

            int button = h % numPlayers;
            int[] blinds = blindSeats(button, numPlayers);

            if (players.get(blinds[0]).getChipCount() == 0 || players.get(blinds[1]).getChipCount() == 0) {
                continue;
            }

            MockServerGameTable table = setupTable(players, button);
            ServerHand hand = new ServerHand(table, h + 1, smallBlind, bigBlind, 0, button, blinds[0], blinds[1]);

            playOneHandPassive(hand, players);
            assertInvariants(hand, players, expectedTotal, h + 1, seed);
        }
    }
}
