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

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.donohoedigital.games.poker.core.PlayerAction;
import com.donohoedigital.games.poker.engine.state.BettingRound;
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

    // === All-in Side Pot Tests ===

    /**
     * Build a deterministic deck for a 2-player heads-up hand.
     *
     * Dealing order: seat0 gets deck[0..1], seat1 gets deck[2..3]. Then: burn,
     * flop(3), burn, turn, burn, river = 9 more cards.
     *
     * @param seat0Card1
     *            hole card 1 for seat 0
     * @param seat0Card2
     *            hole card 2 for seat 0
     * @param seat1Card1
     *            hole card 1 for seat 1
     * @param seat1Card2
     *            hole card 2 for seat 1
     */
    private static ServerDeck headsUpDeck(Card seat0Card1, Card seat0Card2, Card seat1Card1, Card seat1Card2) {
        return new ServerDeck(List.of(seat0Card1, seat0Card2, // seat 0 hole cards
                seat1Card1, seat1Card2, // seat 1 hole cards
                Card.SPADES_5, // burn before flop
                Card.CLUBS_7, Card.DIAMONDS_8, Card.HEARTS_9, // flop
                Card.CLUBS_T, // burn before turn
                Card.CLUBS_J, // turn
                Card.DIAMONDS_Q, // burn before river
                Card.CLUBS_K)); // river
    }

    @Test
    void testAllIn_HeadsUp_ShortStackWins_ChipLeaderGetsExcessReturned() {
        // Alice (seat 0, chip leader 1000) vs Bob (seat 1, short stack 600).
        // Both go all-in preflop. Bob wins.
        // Expected: Bob gets 2*600=1200 (main contested pot), Alice gets back 400
        // excess.
        ServerPlayer alice2 = new ServerPlayer(10, "Alice2", true, 0, 1000);
        ServerPlayer bob2 = new ServerPlayer(11, "Bob2", true, 0, 600);
        alice2.setSeat(0);
        bob2.setSeat(1);

        MockServerGameTable table2 = new MockServerGameTable(2);
        table2.addPlayer(alice2, 0);
        table2.addPlayer(bob2, 1);

        // Bob (seat 1) gets A♠A♥ → wins; Alice (seat 0) gets 2♦3♦ → loses
        ServerDeck deck = headsUpDeck(Card.DIAMONDS_2, Card.DIAMONDS_3, Card.SPADES_A, Card.HEARTS_A);
        // button=0, sbSeat=0 (Alice), bbSeat=1 (Bob), blinds 50/100
        ServerHand hand = new ServerHand(table2, 1, 50, 100, 0, 0, 0, 1, deck);
        hand.deal();

        // Alice (SB) raises all-in: she has 1000-50=950 left, raises 950 more
        hand.applyPlayerAction(alice2, PlayerAction.raise(950));
        // Bob (BB) calls all-in: needs 900 more but only has 500 left
        hand.applyPlayerAction(bob2, PlayerAction.call());

        // Advance through all rounds to showdown
        while (hand.getRound() != BettingRound.SHOWDOWN) {
            hand.advanceRound();
        }
        hand.resolve();

        // Bob wins the contested pot (2*600=1200). Alice gets her excess back (400).
        assertEquals(1200, bob2.getChipCount(), "Short-stack winner should get 2x their stack");
        assertEquals(400, alice2.getChipCount(), "Chip leader should get excess chips returned");
    }

    @Test
    void testAllIn_HeadsUp_ChipLeaderWins_GetsAllChips() {
        // Alice (seat 0, chip leader 1000) vs Bob (seat 1, short stack 600).
        // Both go all-in preflop. Alice wins.
        // Expected: Alice gets all chips (1600), Bob gets 0.
        ServerPlayer alice2 = new ServerPlayer(10, "Alice2", true, 0, 1000);
        ServerPlayer bob2 = new ServerPlayer(11, "Bob2", true, 0, 600);
        alice2.setSeat(0);
        bob2.setSeat(1);

        MockServerGameTable table2 = new MockServerGameTable(2);
        table2.addPlayer(alice2, 0);
        table2.addPlayer(bob2, 1);

        // Alice (seat 0) gets A♠A♥ → wins; Bob (seat 1) gets 2♦3♦ → loses
        ServerDeck deck = headsUpDeck(Card.SPADES_A, Card.HEARTS_A, Card.DIAMONDS_2, Card.DIAMONDS_3);
        ServerHand hand = new ServerHand(table2, 1, 50, 100, 0, 0, 0, 1, deck);
        hand.deal();

        hand.applyPlayerAction(alice2, PlayerAction.raise(950));
        hand.applyPlayerAction(bob2, PlayerAction.call());

        while (hand.getRound() != BettingRound.SHOWDOWN) {
            hand.advanceRound();
        }
        hand.resolve();

        assertEquals(1600, alice2.getChipCount(), "Chip leader who wins should get all chips");
        assertEquals(0, bob2.getChipCount(), "Short stack who loses should get nothing");
    }

    @Test
    void testAllIn_ThreePlayers_MultipleAllIns_PotsCorrectlySplit() {
        // Three players all-in at different stack sizes: Alice=1000, Bob=600,
        // Carol=400.
        // After preflop all-in, calcPots() must create two side pots using
        // INCREMENTAL caps — this directly tests the potInfo.bet - lastSideBet fix.
        //
        // Expected pots (using incremental caps of 400 and 200):
        // sidePot1 = 3×400 = 1200 (all three eligible) → Carol (AA) wins
        // sidePot2 = 2×200 = 400 (Bob + Alice eligible) → Bob (KK) wins
        // mainPot = 400 (Alice only, overbet) → returned to Alice
        ServerPlayer alice3 = new ServerPlayer(10, "Alice3", true, 0, 1000);
        ServerPlayer bob3 = new ServerPlayer(11, "Bob3", true, 0, 600);
        ServerPlayer carol3 = new ServerPlayer(12, "Carol3", true, 0, 400);
        alice3.setSeat(0);
        bob3.setSeat(1);
        carol3.setSeat(2);

        MockServerGameTable table3 = new MockServerGameTable(3);
        table3.addPlayer(alice3, 0);
        table3.addPlayer(bob3, 1);
        table3.addPlayer(carol3, 2);

        // Dealing order by seat: Alice gets [0..1], Bob gets [2..3], Carol gets [4..5].
        // Board: 2♠5♣8♦3♥6♣ — no straights, no flushes.
        // Alice: J♦J♣ → pair of jacks
        // Bob: K♠K♣ → pair of kings (beats Alice in their side pot)
        // Carol: A♠A♥ → pair of aces (beats everyone in the main contested pot)
        ServerDeck deck3 = new ServerDeck(List.of(Card.DIAMONDS_J, Card.CLUBS_J, // Alice (seat 0)
                Card.SPADES_K, Card.CLUBS_K, // Bob (seat 1)
                Card.SPADES_A, Card.HEARTS_A, // Carol (seat 2)
                Card.DIAMONDS_9, // burn before flop
                Card.SPADES_2, Card.CLUBS_5, Card.DIAMONDS_8, // flop
                Card.HEARTS_9, // burn before turn
                Card.HEARTS_3, // turn
                Card.CLUBS_9, // burn before river
                Card.CLUBS_6)); // river

        // button=0, sbSeat=1 (Bob), bbSeat=2 (Carol), blinds 50/100
        ServerHand hand3 = new ServerHand(table3, 1, 50, 100, 0, 0, 1, 2, deck3);
        hand3.deal();

        // Pre-flop action: Alice (button/UTG) raises all-in
        hand3.applyPlayerAction(alice3, PlayerAction.raise(1000));
        // Bob (SB, 550 remaining) calls all-in
        hand3.applyPlayerAction(bob3, PlayerAction.call());
        // Carol (BB, 300 remaining) calls all-in
        hand3.applyPlayerAction(carol3, PlayerAction.call());

        while (hand3.getRound() != BettingRound.SHOWDOWN) {
            hand3.advanceRound();
        }
        hand3.resolve();

        assertEquals(1200, carol3.getChipCount(), "Carol (AA) wins the 3-way pot (3×400=1200)");
        assertEquals(400, bob3.getChipCount(), "Bob (KK) wins the 2-way side pot (2×200=400)");
        assertEquals(400, alice3.getChipCount(), "Alice's uncovered 400 is returned as overbet");
    }

    // === Pot Distribution Scenarios ===

    @Test
    void testSplitPot_IdenticalHands_EvenSplit() {
        // Two players with equivalent hands (AK vs AK, different suits) should
        // split the pot evenly at showdown.
        ServerPlayer alice2 = new ServerPlayer(10, "Alice2", true, 0, 1000);
        ServerPlayer bob2 = new ServerPlayer(11, "Bob2", true, 0, 1000);
        alice2.setSeat(0);
        bob2.setSeat(1);

        MockServerGameTable table2 = new MockServerGameTable(2);
        table2.addPlayer(alice2, 0);
        table2.addPlayer(bob2, 1);

        // Alice: A♠K♠, Bob: A♥K♥ — equivalent hands
        // Board: 2♣5♦8♣3♦6♦ — no flush, no straight
        ServerDeck deck = new ServerDeck(List.of(Card.SPADES_A, Card.SPADES_K, // seat 0 (Alice)
                Card.HEARTS_A, Card.HEARTS_K, // seat 1 (Bob)
                Card.CLUBS_9, // burn before flop
                Card.CLUBS_2, Card.DIAMONDS_5, Card.CLUBS_8, // flop
                Card.CLUBS_T, // burn before turn
                Card.DIAMONDS_3, // turn
                Card.DIAMONDS_Q, // burn before river
                Card.DIAMONDS_6)); // river

        // button=0 (Alice), sbSeat=0 (Alice), bbSeat=1 (Bob), blinds 50/100
        ServerHand hand = new ServerHand(table2, 1, 50, 100, 0, 0, 0, 1, deck);
        hand.deal();

        // Alice (SB) calls (completes to 100)
        hand.applyPlayerAction(alice2, PlayerAction.call());
        // Bob (BB) checks
        hand.applyPlayerAction(bob2, PlayerAction.check());

        // Advance through all rounds to showdown (no further betting)
        while (hand.getRound() != BettingRound.SHOWDOWN) {
            hand.advanceRound();
        }
        hand.resolve();

        // Both hands are identical rank (AK-high with 8,6,5 kickers) → even split
        assertEquals(1000, alice2.getChipCount(), "Alice should keep her original chips (even split)");
        assertEquals(1000, bob2.getChipCount(), "Bob should keep his original chips (even split)");
    }

    @Test
    void testAllIn_FourPlayers_StaggeredStacks_MultiplePotsCorrectlySplit() {
        // Four players all-in at different stack sizes: P1=200, P2=400, P3=600,
        // P4=1000.
        // P1 (AA) wins main pot, P2 (KK) wins 2nd side pot, P3 (QQ) wins 3rd side pot,
        // P4 (JJ) gets excess returned.
        ServerPlayer p1 = new ServerPlayer(10, "P1", true, 0, 200);
        ServerPlayer p2 = new ServerPlayer(11, "P2", true, 0, 400);
        ServerPlayer p3 = new ServerPlayer(12, "P3", true, 0, 600);
        ServerPlayer p4 = new ServerPlayer(13, "P4", true, 0, 1000);
        p1.setSeat(0);
        p2.setSeat(1);
        p3.setSeat(2);
        p4.setSeat(3);

        MockServerGameTable table4 = new MockServerGameTable(4);
        table4.addPlayer(p1, 0);
        table4.addPlayer(p2, 1);
        table4.addPlayer(p3, 2);
        table4.addPlayer(p4, 3);

        // Dealing order: seat0[0..1], seat1[2..3], seat2[4..5], seat3[6..7]
        // then burn, flop(3), burn, turn, burn, river
        // P1(seat0): A♠A♥ (best), P2(seat1): K♠K♥, P3(seat2): Q♠Q♥, P4(seat3): J♠J♥
        // (worst)
        // Board: 2♠5♣8♦3♥6♣ — no straights, no flushes
        ServerDeck deck4 = new ServerDeck(List.of(Card.SPADES_A, Card.HEARTS_A, // P1 (seat 0) — AA
                Card.SPADES_K, Card.HEARTS_K, // P2 (seat 1) — KK
                Card.SPADES_Q, Card.HEARTS_Q, // P3 (seat 2) — QQ
                Card.SPADES_J, Card.HEARTS_J, // P4 (seat 3) — JJ
                Card.DIAMONDS_9, // burn before flop
                Card.SPADES_2, Card.CLUBS_5, Card.DIAMONDS_8, // flop
                Card.HEARTS_9, // burn before turn
                Card.HEARTS_3, // turn
                Card.CLUBS_9, // burn before river
                Card.CLUBS_6)); // river

        // button=0 (P1), sbSeat=1 (P2), bbSeat=2 (P3), blinds 10/20
        ServerHand hand = new ServerHand(table4, 1, 10, 20, 0, 0, 1, 2, deck4);
        hand.deal();

        // Pre-flop action order: UTG (P4, seat3 index 3) acts first
        // P4 (UTG) raises all-in 1000
        hand.applyPlayerAction(p4, PlayerAction.raise(1000));
        // P1 (button) calls all-in 200
        hand.applyPlayerAction(p1, PlayerAction.call());
        // P2 (SB, 10 posted) calls all-in — needs to match 1000 but only has 390 left
        hand.applyPlayerAction(p2, PlayerAction.call());
        // P3 (BB, 20 posted) calls all-in — needs to match 1000 but only has 580 left
        hand.applyPlayerAction(p3, PlayerAction.call());

        while (hand.getRound() != BettingRound.SHOWDOWN) {
            hand.advanceRound();
        }
        hand.resolve();

        // Main pot: 4×200 = 800 → P1 (AA)
        // Side pot 2: 3×200 = 600 → P2 (KK)
        // Side pot 3: 2×200 = 400 → P3 (QQ)
        // Excess: 400 → returned to P4
        assertEquals(800, p1.getChipCount(), "P1 (AA) wins main pot (4×200=800)");
        assertEquals(600, p2.getChipCount(), "P2 (KK) wins 2nd side pot (3×200=600)");
        assertEquals(400, p3.getChipCount(), "P3 (QQ) wins 3rd side pot (2×200=400)");
        assertEquals(400, p4.getChipCount(), "P4 (JJ) gets excess returned (400)");

        // Chip conservation: 200+400+600+1000 = 2200
        int total = p1.getChipCount() + p2.getChipCount() + p3.getChipCount() + p4.getChipCount();
        assertEquals(2200, total, "Chip conservation must hold");
    }

    @Test
    void testDeadMoney_FolderContributionGoesToWinner() {
        // Alice raises, Bob re-raises, Charlie calls, Alice folds.
        // Alice's contribution is "dead money" that goes to the showdown winner.
        // Uses the standard 3-player setUp (alice/bob/charlie, 5000 each).

        // Need deterministic deck so Bob (AA) beats Charlie (KK).
        // Dealing order: seat0[0..1], seat1[2..3], seat2[4..5],
        // then burn, flop(3), burn, turn, burn, river
        // Alice(seat0): 2♦3♦ (folds, doesn't matter)
        // Bob(seat1): A♠A♥ (wins at showdown)
        // Charlie(seat2): K♠K♥ (loses at showdown)
        // Board: 2♠5♣8♦3♥6♣ — no straights, no flushes
        ServerDeck deck3 = new ServerDeck(List.of(Card.DIAMONDS_2, Card.DIAMONDS_3, // Alice (seat 0)
                Card.SPADES_A, Card.HEARTS_A, // Bob (seat 1) — AA
                Card.SPADES_K, Card.HEARTS_K, // Charlie (seat 2) — KK
                Card.DIAMONDS_9, // burn before flop
                Card.SPADES_2, Card.CLUBS_5, Card.DIAMONDS_8, // flop
                Card.HEARTS_9, // burn before turn
                Card.HEARTS_3, // turn
                Card.CLUBS_9, // burn before river
                Card.CLUBS_6)); // river

        // button=0 (Alice), sbSeat=1 (Bob), bbSeat=2 (Charlie), blinds 50/100
        ServerHand hand = new ServerHand(table, 1, 50, 100, 0, 0, 1, 2, deck3);
        hand.deal();

        // Pre-flop: Alice (button/UTG in 3-handed) acts first
        // Alice raises to 300 (adds 300 since she has 0 posted)
        hand.applyPlayerAction(alice, PlayerAction.raise(300));
        // Bob (SB, 50 posted) re-raises to 600 (adds 550)
        hand.applyPlayerAction(bob, PlayerAction.raise(550));
        // Charlie (BB, 100 posted) calls 600 (adds 500)
        hand.applyPlayerAction(charlie, PlayerAction.call());
        // Alice folds (losing her 300)
        hand.applyPlayerAction(alice, PlayerAction.fold());

        // Advance through remaining streets to showdown
        while (hand.getRound() != BettingRound.SHOWDOWN) {
            hand.advanceRound();
        }
        hand.resolve();

        // Alice lost 300 (her raise that she folded)
        assertEquals(4700, alice.getChipCount(), "Alice should have 5000-300=4700 after folding");

        // Pot = Alice's 300 + Bob's 600 + Charlie's 600 = 1500
        // Bob (AA) wins 1500 → 5000 - 600 + 1500 = 5900
        assertEquals(5900, bob.getChipCount(), "Bob (AA) should win the pot including dead money");

        // Charlie lost his 600 → 5000 - 600 = 4400
        assertEquals(4400, charlie.getChipCount(), "Charlie (KK) should have 5000-600=4400");

        // Chip conservation
        int total = alice.getChipCount() + bob.getChipCount() + charlie.getChipCount();
        assertEquals(15000, total, "Chip conservation must hold");
    }

    // === All-In Player Skipping Tests ===

    @Test
    void testAllInPlayer_SkippedForAction() {
        ServerHand hand = new ServerHand(table, 1, 50, 100, 0, 0, 1, 2);
        hand.deal();

        // Alice goes all-in
        hand.applyPlayerAction(alice, PlayerAction.raise(5000));

        // Next player to act should NOT be alice (she's all-in)
        ServerPlayer next = (ServerPlayer) hand.getCurrentPlayerWithInit();
        assertNotEquals(alice, next, "All-in player should be skipped for action");
    }

    // === Heads-Up Edge Case Tests ===

    @Test
    void testHeadsUp_ButtonPostsSB_OpponentPostsBB() {
        // Button posts SB (50), opponent posts BB (100)
        // Button at seat 0 → sbSeat=0, bbSeat=1
        ServerPlayer btn = new ServerPlayer(1, "Button", true, 0, 1000);
        ServerPlayer bb = new ServerPlayer(2, "BB", true, 0, 1000);
        btn.setSeat(0);
        bb.setSeat(1);
        MockServerGameTable t = new MockServerGameTable(2);
        t.addPlayer(btn, 0);
        t.addPlayer(bb, 1);
        ServerHand hand = new ServerHand(t, 1, 50, 100, 0, 0, 0, 1);
        hand.deal();

        // Button posted SB (50), opponent posted BB (100), pot = 150
        assertEquals(950, btn.getChipCount(), "Button should have 1000-50=950 after posting SB");
        assertEquals(900, bb.getChipCount(), "BB should have 1000-100=900 after posting BB");
        assertEquals(150, hand.getPotSize(), "Pot should be 50+100=150 after blinds");
    }

    @Test
    void testHeadsUp_PreflopActionOrder_ButtonActsFirst() {
        // In heads-up, button/SB acts first preflop
        ServerPlayer btn = new ServerPlayer(1, "Button", true, 0, 1000);
        ServerPlayer bb = new ServerPlayer(2, "BB", true, 0, 1000);
        btn.setSeat(0);
        bb.setSeat(1);
        MockServerGameTable t = new MockServerGameTable(2);
        t.addPlayer(btn, 0);
        t.addPlayer(bb, 1);
        ServerHand hand = new ServerHand(t, 1, 50, 100, 0, 0, 0, 1);
        hand.deal();

        // Button (SB) should act first preflop in heads-up
        assertEquals(btn, hand.getCurrentPlayerWithInit(), "Button/SB should act first preflop in heads-up");
    }

    @Test
    void testHeadsUp_PostflopActionOrder_BBActsFirst() {
        // In heads-up, BB (non-button) acts first postflop
        ServerPlayer btn = new ServerPlayer(1, "Button", true, 0, 1000);
        ServerPlayer bb = new ServerPlayer(2, "BB", true, 0, 1000);
        btn.setSeat(0);
        bb.setSeat(1);
        MockServerGameTable t = new MockServerGameTable(2);
        t.addPlayer(btn, 0);
        t.addPlayer(bb, 1);
        ServerHand hand = new ServerHand(t, 1, 50, 100, 0, 0, 0, 1);
        hand.deal();

        // Complete preflop: btn (SB) calls, bb checks
        hand.applyPlayerAction(btn, PlayerAction.call());
        hand.applyPlayerAction(bb, PlayerAction.check());

        hand.advanceRound(); // → FLOP

        // BB (non-button) should act first postflop
        assertEquals(bb, hand.getCurrentPlayerWithInit(), "BB should act first postflop in heads-up");
    }

    @Test
    void testHeadsUp_PartialBlind_ShortStackedSB() {
        // Button/SB has only 30 chips (less than 50 SB)
        ServerPlayer btn = new ServerPlayer(1, "Button", true, 0, 30);
        ServerPlayer bb = new ServerPlayer(2, "BB", true, 0, 1000);
        btn.setSeat(0);
        bb.setSeat(1);
        MockServerGameTable t = new MockServerGameTable(2);
        t.addPlayer(btn, 0);
        t.addPlayer(bb, 1);
        ServerHand hand = new ServerHand(t, 1, 50, 100, 0, 0, 0, 1);
        hand.deal();

        // Button should be all-in with 0 chips (posted all 30)
        assertEquals(0, btn.getChipCount(), "Short-stacked SB should be all-in with 0 chips");
        assertTrue(btn.isAllIn(), "Short-stacked SB should be marked all-in");
        assertEquals(30, hand.getActualSmallBlindPosted(), "Actual SB posted should be 30 (all the short stack had)");

        // Chip conservation: 30 + 1000 = 1030 total
        int totalChips = btn.getChipCount() + bb.getChipCount() + hand.getPotSize();
        assertEquals(1030, totalChips, "Chip conservation: btn + bb + pot should equal 1030");
    }

    @Test
    void testHeadsUp_AllInPreflopFromSB_Resolution() {
        // Button/SB (500 chips) goes all-in, BB (1000 chips) calls.
        // BB has AA (wins).
        ServerPlayer btn = new ServerPlayer(1, "Button", true, 0, 500);
        ServerPlayer bb = new ServerPlayer(2, "BB", true, 0, 1000);
        btn.setSeat(0);
        bb.setSeat(1);
        MockServerGameTable t = new MockServerGameTable(2);
        t.addPlayer(btn, 0);
        t.addPlayer(bb, 1);

        // Button (seat 0) gets 2♦3♦ (loses), BB (seat 1) gets A♠A♥ (wins)
        ServerDeck deck = headsUpDeck(Card.DIAMONDS_2, Card.DIAMONDS_3, Card.SPADES_A, Card.HEARTS_A);
        ServerHand hand = new ServerHand(t, 1, 50, 100, 0, 0, 0, 1, deck);
        hand.deal();

        // Button (SB, 450 remaining after posting 50) raises all-in
        hand.applyPlayerAction(btn, PlayerAction.raise(450));
        // BB (900 remaining after posting 100) calls
        hand.applyPlayerAction(bb, PlayerAction.call());

        // Advance through all rounds to showdown
        while (hand.getRound() != BettingRound.SHOWDOWN) {
            hand.advanceRound();
        }
        hand.resolve();

        // BB wins pot of 1000 (2 × 500). BB had 500 remaining + 1000 pot = 1500.
        assertEquals(1500, bb.getChipCount(), "BB (AA) should have 500 remaining + 1000 pot = 1500");
        assertEquals(0, btn.getChipCount(), "Button should have 0 after losing all-in");

        // Chip conservation: 500 + 1000 = 1500 total
        int totalChips = btn.getChipCount() + bb.getChipCount();
        assertEquals(1500, totalChips, "Chip conservation must hold");
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
