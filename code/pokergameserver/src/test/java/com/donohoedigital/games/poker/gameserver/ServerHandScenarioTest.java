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
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.donohoedigital.games.poker.engine.PlayerAction;
import com.donohoedigital.games.poker.engine.state.ActionType;
import com.donohoedigital.games.poker.engine.state.BettingRound;
import com.donohoedigital.games.poker.engine.Card;

/**
 * Deterministic scenario tests that mirror the highest-value shell scripts,
 * running against {@link ServerHand} directly in milliseconds instead of
 * requiring a full JVM launch.
 *
 * <p>
 * Scenarios ported:
 * <ul>
 * <li>Chip conservation (test-chip-conservation.sh)
 * <li>All actions (test-all-actions.sh)
 * <li>Hand flow / community cards (test-hand-flow.sh)
 * <li>Blind posting at various player counts (test-blind-posting.sh)
 * </ul>
 */
class ServerHandScenarioTest {

    /** Safety limit on actions per betting round to prevent infinite loops. */
    private static final int MAX_ACTIONS_PER_ROUND = 50;

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

    /**
     * Create a fresh table with the given players and button position.
     */
    private MockServerGameTable setupTable(List<ServerPlayer> players, int button) {
        MockServerGameTable table = new MockServerGameTable(players.size());
        table.setButton(button);
        for (int i = 0; i < players.size(); i++) {
            ServerPlayer p = players.get(i);
            p.setFolded(false);
            p.setAllIn(false);
            table.addPlayer(p, i);
        }
        return table;
    }

    /**
     * Play one complete hand where all players check or call only (passive play).
     * Returns the resolved hand.
     */
    private ServerHand playPassiveHand(MockServerGameTable table, List<ServerPlayer> players, int handNum, int sb,
            int bb, int ante, int button, int sbSeat, int bbSeat) {
        ServerHand hand = new ServerHand(table, handNum, sb, bb, ante, button, sbSeat, bbSeat);
        hand.deal();

        while (!hand.isUncontested()) {
            int safetyCounter = 0;
            while (!hand.isDone() && safetyCounter < MAX_ACTIONS_PER_ROUND) {
                ServerPlayer current = (ServerPlayer) hand.getCurrentPlayerWithInit();
                if (current == null)
                    break;

                int amountToCall = hand.getAmountToCall(current);
                PlayerAction action = amountToCall > 0 ? PlayerAction.call() : PlayerAction.check();
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
        return hand;
    }

    /**
     * Assert chip conservation: total chips across all players must equal expected.
     */
    private void assertChipConservation(List<ServerPlayer> players, int expectedTotal, String context) {
        int actualTotal = players.stream().mapToInt(ServerPlayer::getChipCount).sum();
        assertEquals(expectedTotal, actualTotal,
                "Chip conservation violated (" + context + "): expected=" + expectedTotal + " actual=" + actualTotal);
    }

    // ==================== Scenario 1: Chip Conservation ====================

    /**
     * Play 10 hands with a call-everything strategy against 3 players with large
     * stacks (50000 each so no one busts). Assert chip conservation after every
     * hand.
     */
    @Test
    void scenario_chipConservation_10Hands_callStrategy() {
        int numPlayers = 3;
        int startingChips = 50000;
        int sb = 50;
        int bb = 100;
        int expectedTotal = numPlayers * startingChips;

        List<ServerPlayer> players = new ArrayList<>();
        for (int i = 0; i < numPlayers; i++) {
            ServerPlayer p = new ServerPlayer(i + 1, "P" + (i + 1), false, 0, startingChips);
            p.setSeat(i);
            players.add(p);
        }

        for (int h = 0; h < 10; h++) {
            int button = h % numPlayers;
            int[] blinds = blindSeats(button, numPlayers);

            MockServerGameTable table = setupTable(players, button);
            playPassiveHand(table, players, h + 1, sb, bb, 0, button, blinds[0], blinds[1]);

            assertChipConservation(players, expectedTotal, "hand " + (h + 1));

            // No player should have negative chips
            for (ServerPlayer p : players) {
                assertTrue(p.getChipCount() >= 0, p.getName() + " has negative chips after hand " + (h + 1));
            }
        }
    }

    // ==================== Scenario 2: All Actions ====================

    /**
     * Exercise every action type (FOLD, CHECK, CALL, BET, RAISE, ALL_IN) in
     * controlled hands, verifying chip conservation after each.
     */
    @Test
    void scenario_allActions_exerciseEveryActionType() {
        Set<ActionType> exercisedActions = EnumSet.noneOf(ActionType.class);
        int startingChips = 5000;

        // ----- Hand 1: alice folds preflop -> uncontested win -----
        {
            List<ServerPlayer> players = createPlayers(3, startingChips);
            int button = 0;
            int[] blinds = blindSeats(button, 3);
            MockServerGameTable table = setupTable(players, button);
            ServerHand hand = new ServerHand(table, 1, 50, 100, 0, button, blinds[0], blinds[1]);
            hand.deal();

            // Preflop: alice (UTG/button in 3-handed) acts first
            ServerPlayer current = (ServerPlayer) hand.getCurrentPlayerWithInit();
            hand.applyPlayerAction(current, PlayerAction.fold());
            exercisedActions.add(ActionType.FOLD);

            // bob (SB) folds too -> charlie (BB) wins uncontested
            current = (ServerPlayer) hand.getCurrentPlayerWithInit();
            hand.applyPlayerAction(current, PlayerAction.fold());

            assertTrue(hand.isUncontested(), "Hand should be uncontested after 2 folds");

            // Advance to showdown and resolve
            while (hand.getRound() != BettingRound.SHOWDOWN) {
                hand.advanceRound();
            }
            hand.resolve();
            assertChipConservation(players, 3 * startingChips, "hand 1 (fold)");
        }

        // ----- Hand 2: all check to showdown (CHECK action) -----
        {
            List<ServerPlayer> players = createPlayers(3, startingChips);
            int button = 0;
            int[] blinds = blindSeats(button, 3);
            MockServerGameTable table = setupTable(players, button);
            ServerHand hand = new ServerHand(table, 2, 50, 100, 0, button, blinds[0], blinds[1]);
            hand.deal();

            // Preflop: alice calls, bob (SB) calls, charlie (BB) checks
            ServerPlayer current = (ServerPlayer) hand.getCurrentPlayerWithInit();
            hand.applyPlayerAction(current, PlayerAction.call()); // alice calls BB
            current = (ServerPlayer) hand.getCurrentPlayerWithInit();
            hand.applyPlayerAction(current, PlayerAction.call()); // bob completes SB
            current = (ServerPlayer) hand.getCurrentPlayerWithInit();
            hand.applyPlayerAction(current, PlayerAction.check()); // charlie checks BB option
            exercisedActions.add(ActionType.CHECK);

            // Flop through river: all check
            for (int street = 0; street < 3; street++) {
                hand.advanceRound();
                int actions = 0;
                while (!hand.isDone() && actions < MAX_ACTIONS_PER_ROUND) {
                    current = (ServerPlayer) hand.getCurrentPlayerWithInit();
                    if (current == null)
                        break;
                    hand.applyPlayerAction(current, PlayerAction.check());
                    actions++;
                }
            }

            hand.advanceRound(); // to SHOWDOWN
            hand.resolve();
            assertChipConservation(players, 3 * startingChips, "hand 2 (check)");
        }

        // ----- Hand 3: alice calls, bob raises, charlie calls (CALL + RAISE) -----
        {
            List<ServerPlayer> players = createPlayers(3, startingChips);
            int button = 0;
            int[] blinds = blindSeats(button, 3);
            MockServerGameTable table = setupTable(players, button);
            ServerHand hand = new ServerHand(table, 3, 50, 100, 0, button, blinds[0], blinds[1]);
            hand.deal();

            // Preflop: alice calls
            ServerPlayer current = (ServerPlayer) hand.getCurrentPlayerWithInit();
            hand.applyPlayerAction(current, PlayerAction.call());
            exercisedActions.add(ActionType.CALL);

            // bob (SB) raises to 300 total (needs to put in 250 more beyond SB 50)
            current = (ServerPlayer) hand.getCurrentPlayerWithInit();
            hand.applyPlayerAction(current, PlayerAction.raise(250));
            exercisedActions.add(ActionType.RAISE);

            // charlie (BB) calls the raise, alice calls the raise
            finishRoundPassive(hand);

            // Play flop-river passively
            for (int street = 0; street < 3; street++) {
                if (hand.getRound() == BettingRound.SHOWDOWN)
                    break;
                hand.advanceRound();
                finishRoundPassive(hand);
            }

            if (hand.getRound() != BettingRound.SHOWDOWN) {
                hand.advanceRound();
            }
            hand.resolve();
            assertChipConservation(players, 3 * startingChips, "hand 3 (call+raise)");
        }

        // ----- Hand 4: post-flop bet action -----
        {
            List<ServerPlayer> players = createPlayers(3, startingChips);
            int button = 0;
            int[] blinds = blindSeats(button, 3);
            MockServerGameTable table = setupTable(players, button);
            ServerHand hand = new ServerHand(table, 4, 50, 100, 0, button, blinds[0], blinds[1]);
            hand.deal();

            // Preflop: everyone calls/checks
            finishRoundPassive(hand);

            // Flop: bob (SB) bets 200, others call
            hand.advanceRound();
            ServerPlayer current = (ServerPlayer) hand.getCurrentPlayerWithInit();
            assertNotNull(current, "Should have a player to act on flop");
            hand.applyPlayerAction(current, PlayerAction.bet(200));
            exercisedActions.add(ActionType.BET);

            // Others call
            finishRoundPassive(hand);

            // Turn and river passively
            for (int street = 0; street < 2; street++) {
                if (hand.getRound() == BettingRound.SHOWDOWN)
                    break;
                hand.advanceRound();
                finishRoundPassive(hand);
            }

            if (hand.getRound() != BettingRound.SHOWDOWN) {
                hand.advanceRound();
            }
            hand.resolve();
            assertChipConservation(players, 3 * startingChips, "hand 4 (bet)");
        }

        // ----- Hand 5: all-in via raise of entire stack -----
        {
            List<ServerPlayer> players = createPlayers(3, startingChips);
            int button = 0;
            int[] blinds = blindSeats(button, 3);
            MockServerGameTable table = setupTable(players, button);
            ServerHand hand = new ServerHand(table, 5, 50, 100, 0, button, blinds[0], blinds[1]);
            hand.deal();

            // Preflop: alice goes all-in
            ServerPlayer alice = players.get(0);
            hand.applyPlayerAction(alice, PlayerAction.raise(alice.getChipCount()));
            assertTrue(alice.isAllIn(), "Alice should be all-in");

            // Others fold
            ServerPlayer current = (ServerPlayer) hand.getCurrentPlayerWithInit();
            if (current != null) {
                hand.applyPlayerAction(current, PlayerAction.fold());
            }
            current = (ServerPlayer) hand.getCurrentPlayerWithInit();
            if (current != null) {
                hand.applyPlayerAction(current, PlayerAction.fold());
            }

            // Advance to showdown
            while (hand.getRound() != BettingRound.SHOWDOWN) {
                hand.advanceRound();
            }
            hand.resolve();
            assertChipConservation(players, 3 * startingChips, "hand 5 (all-in)");
        }

        // Verify all action types were exercised
        Set<ActionType> expected = EnumSet.of(ActionType.FOLD, ActionType.CHECK, ActionType.CALL, ActionType.BET,
                ActionType.RAISE);
        assertTrue(exercisedActions.containsAll(expected),
                "Not all action types exercised. Missing: " + diff(expected, exercisedActions));
    }

    // ==================== Scenario 3: Hand Flow ====================

    /**
     * Verify community cards appear at correct streets, using card injection via
     * ServerDeck for deterministic cards.
     */
    @Test
    void scenario_handFlow_communityCardsAtCorrectStreets() {
        // Build a deterministic deck. For 3 players:
        // Hole cards: seat0=[0,1], seat1=[2,3], seat2=[4,5]
        // Burn[6], Flop=[7,8,9], Burn[10], Turn=[11], Burn[12], River=[13]
        Card[] expectedFlop = {Card.HEARTS_T, Card.DIAMONDS_J, Card.CLUBS_Q};
        Card expectedTurn = Card.SPADES_K;
        Card expectedRiver = Card.HEARTS_A;

        List<Card> deckOrder = List.of(
                // Hole cards (6 cards for 3 players)
                Card.SPADES_2, Card.SPADES_3, // seat 0
                Card.SPADES_4, Card.SPADES_5, // seat 1
                Card.SPADES_6, Card.SPADES_7, // seat 2
                // Burn
                Card.SPADES_8,
                // Flop
                expectedFlop[0], expectedFlop[1], expectedFlop[2],
                // Burn
                Card.SPADES_9,
                // Turn
                expectedTurn,
                // Burn
                Card.SPADES_T,
                // River
                expectedRiver);

        ServerDeck deck = new ServerDeck(deckOrder);

        List<ServerPlayer> players = createPlayers(3, 5000);
        int button = 0;
        int[] blinds = blindSeats(button, 3);
        MockServerGameTable table = setupTable(players, button);

        ServerHand hand = new ServerHand(table, 1, 50, 100, 0, button, blinds[0], blinds[1], deck);
        hand.deal();

        // After deal: 0 community cards
        assertNull(hand.getCommunityCards(), "No community cards after deal");
        assertEquals(BettingRound.PRE_FLOP, hand.getRound());

        // Play through preflop passively
        finishRoundPassive(hand);

        // Advance to flop
        hand.advanceRound();
        assertEquals(BettingRound.FLOP, hand.getRound());

        Card[] community = hand.getCommunityCards();
        assertNotNull(community, "Community cards should exist after flop");
        assertEquals(3, community.length, "Flop should have 3 community cards");
        assertEquals(expectedFlop[0], community[0], "Flop card 1");
        assertEquals(expectedFlop[1], community[1], "Flop card 2");
        assertEquals(expectedFlop[2], community[2], "Flop card 3");

        // Play through flop passively
        finishRoundPassive(hand);

        // Advance to turn
        hand.advanceRound();
        assertEquals(BettingRound.TURN, hand.getRound());

        community = hand.getCommunityCards();
        assertNotNull(community);
        assertEquals(4, community.length, "Turn should have 4 community cards");
        assertEquals(expectedTurn, community[3], "Turn card");

        // Play through turn passively
        finishRoundPassive(hand);

        // Advance to river
        hand.advanceRound();
        assertEquals(BettingRound.RIVER, hand.getRound());

        community = hand.getCommunityCards();
        assertNotNull(community);
        assertEquals(5, community.length, "River should have 5 community cards");
        assertEquals(expectedRiver, community[4], "River card");

        // Original flop cards should still be intact
        assertEquals(expectedFlop[0], community[0]);
        assertEquals(expectedFlop[1], community[1]);
        assertEquals(expectedFlop[2], community[2]);
        assertEquals(expectedTurn, community[3]);
    }

    // ==================== Scenario 4: Blind Posting ====================

    /**
     * Verify blind/ante deductions at various player counts.
     */
    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4, 6, 9})
    void scenario_blindPosting_variousPlayerCounts(int numPlayers) {
        int startingChips = 5000;
        int sb = 50;
        int bb = 100;

        List<ServerPlayer> players = createPlayers(numPlayers, startingChips);
        int button = 0;
        int[] blinds = blindSeats(button, numPlayers);
        int sbSeat = blinds[0];
        int bbSeat = blinds[1];

        MockServerGameTable table = setupTable(players, button);
        ServerHand hand = new ServerHand(table, 1, sb, bb, 0, button, sbSeat, bbSeat);
        hand.deal();

        // Verify SB player deducted
        assertEquals(startingChips - sb, players.get(sbSeat).getChipCount(),
                "SB player at seat " + sbSeat + " should have " + (startingChips - sb) + " chips");

        // Verify BB player deducted
        assertEquals(startingChips - bb, players.get(bbSeat).getChipCount(),
                "BB player at seat " + bbSeat + " should have " + (startingChips - bb) + " chips");

        // Verify pot has blinds
        assertEquals(sb + bb, hand.getPotSize(), "Pot should contain blinds total");

        // Verify other players untouched
        for (int i = 0; i < numPlayers; i++) {
            if (i != sbSeat && i != bbSeat) {
                assertEquals(startingChips, players.get(i).getChipCount(),
                        "Player at seat " + i + " (not blind) should have full stack");
            }
        }
    }

    /**
     * Verify blind + ante posting with 4 players.
     */
    @Test
    void scenario_blindPosting_withAntes() {
        int numPlayers = 4;
        int startingChips = 5000;
        int sb = 50;
        int bb = 100;
        int ante = 25;

        List<ServerPlayer> players = createPlayers(numPlayers, startingChips);
        int button = 0;
        int[] blinds = blindSeats(button, numPlayers);
        int sbSeat = blinds[0];
        int bbSeat = blinds[1];

        MockServerGameTable table = setupTable(players, button);
        ServerHand hand = new ServerHand(table, 1, sb, bb, ante, button, sbSeat, bbSeat);
        hand.deal();

        // SB: 5000 - 25 (ante) - 50 (sb) = 4925
        assertEquals(startingChips - ante - sb, players.get(sbSeat).getChipCount(),
                "SB player should have starting - ante - sb");

        // BB: 5000 - 25 (ante) - 100 (bb) = 4875
        assertEquals(startingChips - ante - bb, players.get(bbSeat).getChipCount(),
                "BB player should have starting - ante - bb");

        // Other players (button and UTG+1): 5000 - 25 = 4975
        for (int i = 0; i < numPlayers; i++) {
            if (i != sbSeat && i != bbSeat) {
                assertEquals(startingChips - ante, players.get(i).getChipCount(),
                        "Non-blind player at seat " + i + " should have starting - ante");
            }
        }

        // Pot = 4 * 25 (antes) + 50 (sb) + 100 (bb) = 250
        int expectedPot = numPlayers * ante + sb + bb;
        assertEquals(expectedPot, hand.getPotSize(), "Pot should equal antes + blinds");
    }

    // ============================== Helpers ==============================

    /**
     * Create N players, each with the given starting chips, seated 0..N-1.
     */
    private List<ServerPlayer> createPlayers(int numPlayers, int startingChips) {
        List<ServerPlayer> players = new ArrayList<>();
        for (int i = 0; i < numPlayers; i++) {
            ServerPlayer p = new ServerPlayer(i + 1, "P" + (i + 1), false, 0, startingChips);
            p.setSeat(i);
            players.add(p);
        }
        return players;
    }

    /**
     * Finish the current betting round passively: each player who needs to act
     * either calls or checks.
     */
    private void finishRoundPassive(ServerHand hand) {
        int safetyCounter = 0;
        while (!hand.isDone() && safetyCounter < MAX_ACTIONS_PER_ROUND) {
            ServerPlayer current = (ServerPlayer) hand.getCurrentPlayerWithInit();
            if (current == null)
                break;

            int amountToCall = hand.getAmountToCall(current);
            PlayerAction action = amountToCall > 0 ? PlayerAction.call() : PlayerAction.check();
            hand.applyPlayerAction(current, action);
            safetyCounter++;
        }
    }

    /**
     * Return the set difference (a - b) for reporting missing actions.
     */
    private static <T> Set<T> diff(Set<T> a, Set<T> b) {
        Set<T> result = new HashSet<>(a);
        result.removeAll(b);
        return result;
    }
}
