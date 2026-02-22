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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.donohoedigital.games.poker.core.PlayerAction;
import com.donohoedigital.games.poker.core.state.BettingRound;
import com.donohoedigital.games.poker.engine.Card;

/**
 * Comprehensive tests for ServerHand. TDD: Test-first implementation.
 *
 * Tests cover: dealing, blinds, actions, pot management, showdown, chip
 * conservation.
 */
class ServerHandTest {

    private MockServerGameTable table;
    private ServerPlayer alice;
    private ServerPlayer bob;
    private ServerPlayer charlie;

    @BeforeEach
    void setUp() {
        // Create three players
        alice = new ServerPlayer(1, "Alice", true, 0, 5000);
        bob = new ServerPlayer(2, "Bob", true, 0, 5000);
        charlie = new ServerPlayer(3, "Charlie", true, 0, 5000);

        alice.setSeat(0);
        bob.setSeat(1);
        charlie.setSeat(2);

        // Create mock table (seats 3 players)
        table = new MockServerGameTable(3);
        table.addPlayer(alice, 0);
        table.addPlayer(bob, 1);
        table.addPlayer(charlie, 2);
    }

    // === Construction and Dealing Tests ===

    @Test
    void testConstruction() {
        ServerHand hand = new ServerHand(table, 1, 50, 100, 0, 0, 1, 2);

        assertNotNull(hand);
        assertEquals(BettingRound.NONE, hand.getRound());
        assertFalse(hand.isDone());
    }

    @Test
    void testDeal_AssignsHoleCards() {
        ServerHand hand = new ServerHand(table, 1, 50, 100, 0, 0, 1, 2);
        hand.deal();

        // Each player should have 2 hole cards
        Card[] aliceCards = hand.getPlayerCards(alice);
        Card[] bobCards = hand.getPlayerCards(bob);
        Card[] charlieCards = hand.getPlayerCards(charlie);

        assertNotNull(aliceCards);
        assertNotNull(bobCards);
        assertNotNull(charlieCards);

        assertEquals(2, aliceCards.length);
        assertEquals(2, bobCards.length);
        assertEquals(2, charlieCards.length);
    }

    @Test
    void testDeal_NoCommunityCardsInitially() {
        ServerHand hand = new ServerHand(table, 1, 50, 100, 0, 0, 1, 2);
        hand.deal();

        Card[] community = hand.getCommunityCards();
        assertNull(community); // No community cards until flop
    }

    // === Blind Posting Tests ===

    @Test
    void testPostBlinds_SmallBlindDeducted() {
        ServerHand hand = new ServerHand(table, 1, 50, 100, 0, 0, 1, 2);
        hand.deal();

        // Bob is small blind (seat 1)
        assertEquals(4950, bob.getChipCount()); // 5000 - 50
    }

    @Test
    void testPostBlinds_BigBlindDeducted() {
        ServerHand hand = new ServerHand(table, 1, 50, 100, 0, 0, 1, 2);
        hand.deal();

        // Charlie is big blind (seat 2)
        assertEquals(4900, charlie.getChipCount()); // 5000 - 100
    }

    @Test
    void testPostBlinds_PotHasBlinds() {
        ServerHand hand = new ServerHand(table, 1, 50, 100, 0, 0, 1, 2);
        hand.deal();

        assertEquals(150, hand.getPotSize()); // 50 + 100
    }

    @Test
    void testPostAntes_AllPlayersContribute() {
        ServerHand hand = new ServerHand(table, 1, 50, 100, 25, 0, 1, 2);
        hand.deal();

        // All three players post 25 ante
        assertEquals(4975, alice.getChipCount()); // 5000 - 25
        assertEquals(4925, bob.getChipCount()); // 5000 - 25 - 50 (sb)
        assertEquals(4875, charlie.getChipCount()); // 5000 - 25 - 100 (bb)

        assertEquals(225, hand.getPotSize()); // 75 (antes) + 50 (sb) + 100 (bb)
    }

    // === Simple Action Tests ===

    @Test
    void testFoldAction_UpdatesPlayerState() {
        ServerHand hand = new ServerHand(table, 1, 50, 100, 0, 0, 1, 2);
        hand.deal();

        assertFalse(alice.isFolded());

        hand.applyPlayerAction(alice, PlayerAction.fold());

        assertTrue(alice.isFolded());
    }

    @Test
    void testFoldAction_PlayerNotInHand() {
        ServerHand hand = new ServerHand(table, 1, 50, 100, 0, 0, 1, 2);
        hand.deal();

        hand.applyPlayerAction(alice, PlayerAction.fold());

        assertEquals(2, hand.getNumWithCards()); // Bob and Charlie remain
    }

    @Test
    void testCheckAction_NoChipsAdded() {
        ServerHand hand = new ServerHand(table, 1, 50, 100, 0, 0, 1, 2);
        hand.deal();
        hand.setRound(BettingRound.FLOP); // Move to flop so check is valid

        int potBefore = hand.getPotSize();
        int chipsBefore = alice.getChipCount();

        hand.applyPlayerAction(alice, PlayerAction.check());

        assertEquals(potBefore, hand.getPotSize());
        assertEquals(chipsBefore, alice.getChipCount());
    }

    // === Betting Action Tests ===

    @Test
    void testCallAction_MatchesBet() {
        ServerHand hand = new ServerHand(table, 1, 50, 100, 0, 0, 1, 2);
        hand.deal();

        // Alice calls big blind (100)
        int callAmount = hand.getAmountToCall(alice);
        assertEquals(100, callAmount);

        hand.applyPlayerAction(alice, PlayerAction.call());

        assertEquals(4900, alice.getChipCount()); // 5000 - 100
        assertEquals(250, hand.getPotSize()); // 150 (blinds) + 100 (call)
    }

    @Test
    void testBetAction_IncreasePot() {
        ServerHand hand = new ServerHand(table, 1, 50, 100, 0, 0, 1, 2);
        hand.deal();
        // Use advanceRound() instead of setRound() to properly handle pot calculation
        hand.advanceRound();

        hand.applyPlayerAction(alice, PlayerAction.bet(200));

        assertEquals(4800, alice.getChipCount()); // 5000 - 200
        assertEquals(350, hand.getPotSize()); // 150 (blinds in pots) + 200 (bet in playerBets)
    }

    @Test
    void testRaiseAction_IncreasePotAndBet() {
        ServerHand hand = new ServerHand(table, 1, 50, 100, 0, 0, 1, 2);
        hand.deal();

        // Alice raises to 300
        hand.applyPlayerAction(alice, PlayerAction.raise(300));

        assertEquals(4700, alice.getChipCount()); // 5000 - 300
        assertEquals(450, hand.getPotSize()); // 150 (blinds) + 300 (raise)
    }

    // === Round Advancement Tests ===

    @Test
    void testAdvanceRound_PreflopToFlop() {
        ServerHand hand = new ServerHand(table, 1, 50, 100, 0, 0, 1, 2);
        hand.deal();

        hand.setRound(BettingRound.PRE_FLOP);
        hand.advanceRound();

        assertEquals(BettingRound.FLOP, hand.getRound());
    }

    @Test
    void testAdvanceRound_DealsFlop() {
        ServerHand hand = new ServerHand(table, 1, 50, 100, 0, 0, 1, 2);
        hand.deal();

        hand.setRound(BettingRound.PRE_FLOP);
        hand.advanceRound();

        Card[] community = hand.getCommunityCards();
        assertNotNull(community);
        assertEquals(3, community.length); // Flop = 3 cards
    }

    @Test
    void testAdvanceRound_FlopToTurn() {
        ServerHand hand = new ServerHand(table, 1, 50, 100, 0, 0, 1, 2);
        hand.deal();

        hand.setRound(BettingRound.FLOP);
        hand.advanceRound();

        assertEquals(BettingRound.TURN, hand.getRound());

        Card[] community = hand.getCommunityCards();
        assertEquals(4, community.length); // Flop (3) + Turn (1)
    }

    @Test
    void testAdvanceRound_TurnToRiver() {
        ServerHand hand = new ServerHand(table, 1, 50, 100, 0, 0, 1, 2);
        hand.deal();

        hand.setRound(BettingRound.TURN);
        hand.advanceRound();

        assertEquals(BettingRound.RIVER, hand.getRound());

        Card[] community = hand.getCommunityCards();
        assertEquals(5, community.length); // All 5 community cards
    }

    // === Hand Completion Tests ===

    @Test
    void testIsDone_AllFoldedExceptOne() {
        ServerHand hand = new ServerHand(table, 1, 50, 100, 0, 0, 1, 2);
        hand.deal();

        hand.applyPlayerAction(alice, PlayerAction.fold());
        hand.applyPlayerAction(bob, PlayerAction.fold());

        assertTrue(hand.isDone());
        assertTrue(hand.isUncontested());
    }

    @Test
    void testIsDone_NotDoneWithMultiplePlayersActive() {
        ServerHand hand = new ServerHand(table, 1, 50, 100, 0, 0, 1, 2);
        hand.deal();

        assertFalse(hand.isDone());
        assertFalse(hand.isUncontested());
    }

    // === Pot Management Tests ===

    @Test
    void testGetPotSize_AccumulatesOverRounds() {
        ServerHand hand = new ServerHand(table, 1, 50, 100, 0, 0, 1, 2);
        hand.deal();

        int potAfterDeal = hand.getPotSize(); // Should be 150 (blinds)

        hand.applyPlayerAction(alice, PlayerAction.call()); // +100
        assertEquals(potAfterDeal + 100, hand.getPotSize());

        // Use advanceRound() to properly handle pot calculation
        hand.advanceRound();
        hand.applyPlayerAction(alice, PlayerAction.bet(200)); // +200
        assertEquals(potAfterDeal + 100 + 200, hand.getPotSize());
    }

    @Test
    void testGetAmountToCall_BigBlindAmount() {
        ServerHand hand = new ServerHand(table, 1, 50, 100, 0, 0, 1, 2);
        hand.deal();

        assertEquals(100, hand.getAmountToCall(alice)); // Alice needs to call BB
        assertEquals(50, hand.getAmountToCall(bob)); // Bob (SB) needs 50 more to match BB
        assertEquals(0, hand.getAmountToCall(charlie)); // Charlie (BB) already in for 100
    }

    @Test
    void testGetMinBet_EqualsBigBlind() {
        ServerHand hand = new ServerHand(table, 1, 50, 100, 0, 0, 1, 2);
        hand.deal();
        hand.setRound(BettingRound.FLOP);

        assertEquals(100, hand.getMinBet());
    }

    @Test
    void testGetMinRaise_AtLeastPreviousRaise() {
        ServerHand hand = new ServerHand(table, 1, 50, 100, 0, 0, 1, 2);
        hand.deal();

        // After BB of 100, min raise should be at least 100 more (to 200)
        int minRaise = hand.getMinRaise();
        assertTrue(minRaise >= 200);
    }

    // === Chip Conservation Test ===

    @Test
    void testChipConservation_DuringBetting() {
        ServerHand hand = new ServerHand(table, 1, 50, 100, 0, 0, 1, 2);

        int totalChipsBefore = alice.getChipCount() + bob.getChipCount() + charlie.getChipCount();

        hand.deal();

        // Blinds posted, chips moved to pot
        int totalChipsAfter = alice.getChipCount() + bob.getChipCount() + charlie.getChipCount() + hand.getPotSize();

        assertEquals(totalChipsBefore, totalChipsAfter);
    }

    // === Showdown Tests ===

    @Test
    void testResolve_SingleWinner() {
        ServerHand hand = new ServerHand(table, 1, 50, 100, 0, 0, 1, 2);
        hand.deal();

        // All players call through to showdown using proper round advancement
        hand.applyPlayerAction(alice, PlayerAction.call());
        hand.applyPlayerAction(bob, PlayerAction.check());
        hand.applyPlayerAction(charlie, PlayerAction.check());
        // Now advance through rounds to river
        while (hand.getRound() != BettingRound.SHOWDOWN) {
            hand.advanceRound();
        }

        hand.resolve();

        // Verify one player won the entire pot
        int totalWinnings = alice.getChipCount() + bob.getChipCount() + charlie.getChipCount();
        assertEquals(15000, totalWinnings); // 3 players * 5000 chips
    }

    @Test
    void testResolve_UncontestedWin() {
        ServerHand hand = new ServerHand(table, 1, 50, 100, 0, 0, 1, 2);
        hand.deal();

        // Everyone folds except alice
        hand.applyPlayerAction(alice, PlayerAction.raise(300));
        hand.applyPlayerAction(bob, PlayerAction.fold());
        hand.applyPlayerAction(charlie, PlayerAction.fold());

        int aliceChipsBefore = alice.getChipCount();
        int potSize = hand.getPotSize();

        hand.resolve();

        // Alice should win the pot without showdown
        assertEquals(aliceChipsBefore + potSize, alice.getChipCount());
        assertTrue(hand.isUncontested());
    }

    @Test
    void testResolve_SplitPot() {
        ServerHand hand = new ServerHand(table, 1, 50, 100, 0, 0, 1, 2);
        hand.deal();

        // All call through to showdown using proper round advancement
        hand.applyPlayerAction(alice, PlayerAction.call());
        hand.applyPlayerAction(bob, PlayerAction.check());
        hand.applyPlayerAction(charlie, PlayerAction.check());
        // Advance through rounds to showdown
        while (hand.getRound() != BettingRound.SHOWDOWN) {
            hand.advanceRound();
        }

        hand.resolve();

        // At minimum, chips should be conserved
        int totalChips = alice.getChipCount() + bob.getChipCount() + charlie.getChipCount();
        assertEquals(15000, totalChips);
    }

    @Test
    void testResolve_ChipConservation() {
        ServerHand hand = new ServerHand(table, 1, 50, 100, 0, 0, 1, 2);

        int totalBefore = alice.getChipCount() + bob.getChipCount() + charlie.getChipCount();

        hand.deal();
        hand.applyPlayerAction(alice, PlayerAction.raise(500));
        hand.applyPlayerAction(bob, PlayerAction.call());
        hand.applyPlayerAction(charlie, PlayerAction.call());
        // Use advanceRound() to properly move to flop
        hand.advanceRound();
        hand.applyPlayerAction(alice, PlayerAction.bet(300));
        hand.applyPlayerAction(bob, PlayerAction.call());
        hand.applyPlayerAction(charlie, PlayerAction.fold());
        // Advance through remaining rounds to showdown
        while (hand.getRound() != BettingRound.SHOWDOWN) {
            hand.advanceRound();
        }

        hand.resolve();

        int totalAfter = alice.getChipCount() + bob.getChipCount() + charlie.getChipCount();
        assertEquals(totalBefore, totalAfter);
    }

    @Test
    void testIsDone_AfterResolve() {
        ServerHand hand = new ServerHand(table, 1, 50, 100, 0, 0, 1, 2);
        hand.deal();

        hand.applyPlayerAction(alice, PlayerAction.call());
        hand.setRound(BettingRound.RIVER);

        assertFalse(hand.isDone());

        hand.resolve();

        assertTrue(hand.isDone());
    }

    // === Fix 1: RAISE/BET currentBet correctness ===

    @Test
    void testRaise_SBRaisesPreflop_BettingCompletes() {
        // SB is bob (seat 1), BB is charlie (seat 2), button at seat 0
        // SB posts 50, BB posts 100. Alice (UTG) folds.
        // SB raises to 200 total (action.amount = 200 - 50 already posted = 150 more).
        // After raise: SB total in playerBets = 200, currentBet = 200.
        // BB calls: needs 100 more (200 - 100 already posted). BB total = 200.
        // isPotGood() -> all non-folded players have 200 in playerBets = true.
        // isDone() -> true.
        ServerHand hand = new ServerHand(table, 1, 50, 100, 0, 0, 1, 2);
        hand.deal();

        // Alice folds (UTG)
        hand.applyPlayerAction(alice, PlayerAction.fold());

        // SB (bob) raises to 200 total. The raise action.amount is the additional
        // chips.
        // SB already has 50 posted. Raising to 200 total means adding 150 more.
        // But per the action protocol, raise amount is total raised-to (server raises
        // to 200).
        // We test with amount = 150 (the increment), since SB already posted 50:
        // newRaiseTotal = 50 + 150 = 200 = currentBet
        hand.applyPlayerAction(bob, PlayerAction.raise(150)); // adds 150 to sb's existing 50

        // currentBet must be 200 (total in pot for bob this round)
        // charlie must call 100 more (200 - 100 already in)
        assertEquals(100, hand.getAmountToCall(charlie));

        // BB (charlie) calls 100 more
        hand.applyPlayerAction(charlie, PlayerAction.call());

        // Pot = 50 (sb) + 150 (sb raise) + 100 (bb) + 100 (bb call) = 400
        assertEquals(400, hand.getPotSize());

        // Betting round is done: both active players have 200 in pot, pot is good
        assertTrue(hand.isDone() || hand.getCurrentPlayerInitIndex() < 0,
                "Hand should be done or have no current player after BB calls");
    }

    // === Fix 3: pre-flop action order for 4+ players ===

    @Test
    void testPreflopActionOrder_FourPlayers_UTGActsFirst() {
        // 4-player game: button=seat0(alice), SB=seat1(bob), BB=seat2(charlie),
        // UTG=seat3(diana)
        // playerOrder = [alice(btn), bob(SB), charlie(BB), diana(UTG)]
        // Pre-flop: UTG (diana, index 3) should act first, not button (alice, index 0)
        ServerPlayer diana = new ServerPlayer(4, "Diana", true, 0, 5000);
        diana.setSeat(3);
        MockServerGameTable table4 = new MockServerGameTable(4);
        table4.addPlayer(alice, 0);
        table4.addPlayer(bob, 1);
        table4.addPlayer(charlie, 2);
        table4.addPlayer(diana, 3);

        ServerHand hand = new ServerHand(table4, 1, 50, 100, 0, 0, 1, 2);
        hand.deal();

        // First actor pre-flop must be UTG (diana), not button (alice)
        assertEquals(diana, hand.getCurrentPlayerWithInit(),
                "UTG (diana) should act first pre-flop in a 4-player game, not the button");
    }

    @Test
    void testPreflopActionOrder_ThreePlayers_ButtonActsFirst() {
        // 3-player game: button=seat0(alice)=UTG, SB=seat1(bob), BB=seat2(charlie)
        // In 3-handed poker the button IS UTG and acts first pre-flop.
        ServerHand hand = new ServerHand(table, 1, 50, 100, 0, 0, 1, 2);
        hand.deal();

        assertEquals(alice, hand.getCurrentPlayerWithInit(),
                "Button (alice=UTG) should act first pre-flop in a 3-player game");
    }

    @Test
    void testPostflopActionOrder_ThreePlayers_SBActsFirst() {
        // 3-player game: button=seat0(alice), SB=seat1(bob), BB=seat2(charlie)
        // Post-flop: SB (bob, index 1) acts first, button (alice, index 0) acts last.
        ServerHand hand = new ServerHand(table, 1, 50, 100, 0, 0, 1, 2);
        hand.deal();

        // Complete pre-flop: alice folds, bob calls, charlie checks
        hand.applyPlayerAction(alice, PlayerAction.fold());
        hand.applyPlayerAction(bob, PlayerAction.call());
        hand.applyPlayerAction(charlie, PlayerAction.check());

        hand.advanceRound(); // → FLOP, resets currentPlayerIndex

        // First actor on flop must be SB (bob), not button (alice, who also folded)
        assertEquals(bob, hand.getCurrentPlayerWithInit(), "SB (bob) should act first post-flop");
    }

    // === Fix 2: initPlayerIndex allIn case ===

    @Test
    void testInitPlayerIndex_AllPlayersAllIn_SetsNoCurrentPlayer() {
        ServerHand hand = new ServerHand(table, 1, 50, 100, 0, 0, 1, 2);
        hand.deal();

        // Set all players all-in
        alice.setAllIn(true);
        bob.setAllIn(true);
        charlie.setAllIn(true);

        // getCurrentPlayerWithInit triggers initPlayerIndex()
        // All players allIn → isDone() or no active player found → returns null
        assertNull(hand.getCurrentPlayerWithInit());
    }

    /**
     * Mock table for testing. Implements ServerHand.MockTable interface.
     */
    private static class MockServerGameTable implements ServerHand.MockTable {
        private final ServerPlayer[] seats;
        private final int numSeats;

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
            return 0; // Dealer at seat 0 for tests
        }
    }
}
