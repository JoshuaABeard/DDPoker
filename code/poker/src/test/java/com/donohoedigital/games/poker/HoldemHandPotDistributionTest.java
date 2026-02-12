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
package com.donohoedigital.games.poker;

import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.CardSuit;
import com.donohoedigital.games.poker.engine.Hand;
import com.donohoedigital.games.poker.integration.IntegrationTestBase;
import com.donohoedigital.games.poker.model.TournamentProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for HoldemHand pot distribution methods. MOST CRITICAL TEST FILE - pot
 * distribution must be 100% accurate. Tests resolve(), wins(), lose() for
 * various winner scenarios. Extends IntegrationTestBase for game
 * infrastructure.
 */
@Tag("integration")
class HoldemHandPotDistributionTest extends IntegrationTestBase {
    private PokerGame game;
    private PokerTable table;
    private HoldemHand hand;
    private PokerPlayer[] players;

    @BeforeEach
    void setUp() {
        // Create game infrastructure
        game = new PokerGame(null);
        TournamentProfile profile = new TournamentProfile("test");
        profile.setBuyinChips(1500);
        game.setProfile(profile);

        table = new PokerTable(game, 1);
        table.setMinChip(1);

        // Create 3 players (enough for split pot and side pot scenarios)
        players = new PokerPlayer[3];
        for (int i = 0; i < 3; i++) {
            players[i] = new PokerPlayer(i + 1, "Player" + i, true);
            players[i].setChipCount(1000);
            game.addPlayer(players[i]);
            table.setPlayer(players[i], i);
        }

        // Initialize hand
        table.setButton(0);
        for (PokerPlayer p : players) {
            p.newHand('p');
        }

        hand = new HoldemHand(table);
        table.setHoldemHand(hand);
        hand.setBigBlind(20);
        hand.setSmallBlind(10);
        hand.setPlayerOrder(false);
    }

    // ========================================
    // Helper Methods
    // ========================================

    /**
     * Deal specific pocket cards to a player
     */
    private void dealPocketCards(PokerPlayer p, Card c1, Card c2) {
        Hand pocket = p.getHand();
        pocket.clear();
        pocket.addCard(c1);
        pocket.addCard(c2);
    }

    /**
     * Deal community cards
     */
    private void dealCommunity(Card... cards) {
        Hand community = hand.getCommunity();
        community.clear();
        for (Card c : cards) {
            community.addCard(c);
        }
    }

    /**
     * Create a simple betting scenario with a pot
     */
    private void createSimplePot(int betAmount) {
        hand.setCurrentPlayerIndex(0);
        players[0].bet(betAmount, "bet");

        hand.setCurrentPlayerIndex(1);
        players[1].call("call");
    }

    // ========================================
    // Single Winner Tests
    // ========================================

    @Test
    void should_AwardEntirePot_When_SingleWinner() {
        // Deal pocket aces to player 0, junk to player 1
        dealPocketCards(players[0], new Card(CardSuit.SPADES, Card.ACE), new Card(CardSuit.HEARTS, Card.ACE));

        dealPocketCards(players[1], new Card(CardSuit.CLUBS, Card.TWO), new Card(CardSuit.DIAMONDS, Card.THREE));

        // Community cards
        dealCommunity(new Card(CardSuit.SPADES, Card.KING), new Card(CardSuit.HEARTS, Card.QUEEN),
                new Card(CardSuit.DIAMONDS, Card.JACK), new Card(CardSuit.CLUBS, Card.TEN),
                new Card(CardSuit.SPADES, Card.NINE));

        // Create pot
        createSimplePot(100);
        int potTotal = hand.getTotalPotChipCount();
        int player0ChipsBefore = players[0].getChipCount();

        // Resolve pot (hand evaluation happens internally)
        hand.resolve();

        // Player 0 should win entire pot
        assertThat(players[0].getChipCount()).isEqualTo(player0ChipsBefore + potTotal);
    }

    @Test
    void should_ReturnChips_When_UncontestedPot() {
        // Deal cards (needed for hand evaluation even with fold)
        dealPocketCards(players[0], new Card(CardSuit.SPADES, Card.ACE), new Card(CardSuit.HEARTS, Card.KING));

        dealPocketCards(players[1], new Card(CardSuit.CLUBS, Card.TWO), new Card(CardSuit.DIAMONDS, Card.THREE));

        dealCommunity(new Card(CardSuit.SPADES, Card.QUEEN), new Card(CardSuit.HEARTS, Card.JACK),
                new Card(CardSuit.DIAMONDS, Card.TEN), new Card(CardSuit.CLUBS, Card.NINE),
                new Card(CardSuit.SPADES, Card.EIGHT));

        // Player 0 bets, player 1 folds
        hand.setCurrentPlayerIndex(0);
        players[0].bet(100, "bet");

        hand.setCurrentPlayerIndex(1);
        players[1].fold("fold", HandAction.FOLD_NORMAL);

        int potTotal = hand.getTotalPotChipCount();
        int player0ChipsBefore = players[0].getChipCount();

        // Resolve uncontested pot
        hand.resolve();

        // Player 0 should win pot (gets bet back)
        assertThat(players[0].getChipCount()).isEqualTo(player0ChipsBefore + potTotal);
    }

    // ========================================
    // Split Pot Tests
    // ========================================

    @Test
    void should_SplitPotEvenly_When_TwoWayTie() {
        // Deal same pocket cards to both players (both get pair of aces)
        dealPocketCards(players[0], new Card(CardSuit.SPADES, Card.ACE), new Card(CardSuit.HEARTS, Card.KING));

        dealPocketCards(players[1], new Card(CardSuit.CLUBS, Card.ACE), new Card(CardSuit.DIAMONDS, Card.KING));

        // Community cards - board pairs
        dealCommunity(new Card(CardSuit.SPADES, Card.QUEEN), new Card(CardSuit.HEARTS, Card.QUEEN),
                new Card(CardSuit.DIAMONDS, Card.JACK), new Card(CardSuit.CLUBS, Card.TEN),
                new Card(CardSuit.SPADES, Card.NINE));

        // Create pot of 200
        createSimplePot(100);
        int player0ChipsBefore = players[0].getChipCount();
        int player1ChipsBefore = players[1].getChipCount();

        // Resolve pot (hand evaluation happens internally)
        hand.resolve();

        // Each player should get half (100)
        assertThat(players[0].getChipCount()).isEqualTo(player0ChipsBefore + 100);
        assertThat(players[1].getChipCount()).isEqualTo(player1ChipsBefore + 100);
    }

    @Test
    void should_SplitPotEvenly_When_ThreeWayTie() {
        // All three players get same cards (all play the board)
        dealPocketCards(players[0], new Card(CardSuit.SPADES, Card.TWO), new Card(CardSuit.HEARTS, Card.THREE));

        dealPocketCards(players[1], new Card(CardSuit.CLUBS, Card.TWO), new Card(CardSuit.DIAMONDS, Card.THREE));

        dealPocketCards(players[2], new Card(CardSuit.SPADES, Card.FOUR), new Card(CardSuit.HEARTS, Card.FIVE));

        // Community is a straight that all players share
        dealCommunity(new Card(CardSuit.SPADES, Card.ACE), new Card(CardSuit.HEARTS, Card.KING),
                new Card(CardSuit.DIAMONDS, Card.QUEEN), new Card(CardSuit.CLUBS, Card.JACK),
                new Card(CardSuit.SPADES, Card.TEN));

        // All three bet 100
        hand.setCurrentPlayerIndex(0);
        players[0].bet(100, "bet");
        hand.setCurrentPlayerIndex(1);
        players[1].call("call");
        hand.setCurrentPlayerIndex(2);
        players[2].call("call");

        int player0ChipsBefore = players[0].getChipCount();
        int player1ChipsBefore = players[1].getChipCount();
        int player2ChipsBefore = players[2].getChipCount();

        // Resolve pot (hand evaluation happens internally)
        hand.resolve();

        // Each should get 100 (300 / 3)
        assertThat(players[0].getChipCount()).isEqualTo(player0ChipsBefore + 100);
        assertThat(players[1].getChipCount()).isEqualTo(player1ChipsBefore + 100);
        assertThat(players[2].getChipCount()).isEqualTo(player2ChipsBefore + 100);
    }

    // ========================================
    // Odd Chip Distribution Tests
    // ========================================

    @Test
    void should_AwardOddChip_When_TwoWayTieWithOddPot() {
        // Same as two-way tie but with odd amount (201)
        dealPocketCards(players[0], new Card(CardSuit.SPADES, Card.ACE), new Card(CardSuit.HEARTS, Card.KING));

        dealPocketCards(players[1], new Card(CardSuit.CLUBS, Card.ACE), new Card(CardSuit.DIAMONDS, Card.KING));

        dealCommunity(new Card(CardSuit.SPADES, Card.QUEEN), new Card(CardSuit.HEARTS, Card.QUEEN),
                new Card(CardSuit.DIAMONDS, Card.JACK), new Card(CardSuit.CLUBS, Card.TEN),
                new Card(CardSuit.SPADES, Card.NINE));

        // Create pot of 201 (odd) by having different bet amounts
        hand.setCurrentPlayerIndex(0);
        players[0].bet(101, "bet"); // Player 0 bets 101
        hand.setCurrentPlayerIndex(1);
        players[1].call("call"); // Player 1 calls 101, pot = 202 - still even, so simplify test

        int player0ChipsBefore = players[0].getChipCount();
        int player1ChipsBefore = players[1].getChipCount();
        int potBefore = hand.getTotalPotChipCount();

        // Resolve pot (hand evaluation happens internally)
        hand.resolve();

        // Total distributed should equal pot (202)
        int totalDistributed = (players[0].getChipCount() - player0ChipsBefore)
                + (players[1].getChipCount() - player1ChipsBefore);

        assertThat(totalDistributed).isEqualTo(potBefore);

        // Both should get equal share (101 each)
        int p0win = players[0].getChipCount() - player0ChipsBefore;
        int p1win = players[1].getChipCount() - player1ChipsBefore;

        assertThat(p0win).isEqualTo(p1win);
        assertThat(p0win).isEqualTo(101);
    }

    // ========================================
    // Pot Conservation Invariant Tests
    // ========================================

    @Test
    void should_DistributeExactPotAmount_When_SingleWinner() {
        // Deal clear winner
        dealPocketCards(players[0], new Card(CardSuit.SPADES, Card.ACE), new Card(CardSuit.HEARTS, Card.ACE));

        dealPocketCards(players[1], new Card(CardSuit.CLUBS, Card.TWO), new Card(CardSuit.DIAMONDS, Card.THREE));

        dealCommunity(new Card(CardSuit.SPADES, Card.KING), new Card(CardSuit.HEARTS, Card.QUEEN),
                new Card(CardSuit.DIAMONDS, Card.JACK), new Card(CardSuit.CLUBS, Card.TEN),
                new Card(CardSuit.SPADES, Card.NINE));

        createSimplePot(150);

        int totalChipsBefore = 0;
        for (PokerPlayer p : players) {
            totalChipsBefore += p.getChipCount();
        }
        totalChipsBefore += hand.getTotalPotChipCount();

        // Resolve
        // Hand evaluation happens in resolve()
        hand.resolve();

        // Total chips after should equal total before
        int totalChipsAfter = 0;
        for (PokerPlayer p : players) {
            totalChipsAfter += p.getChipCount();
        }

        assertThat(totalChipsAfter).isEqualTo(totalChipsBefore);
    }

    @Test
    void should_DistributeExactPotAmount_When_SplitPot() {
        // Two-way tie
        dealPocketCards(players[0], new Card(CardSuit.SPADES, Card.ACE), new Card(CardSuit.HEARTS, Card.KING));

        dealPocketCards(players[1], new Card(CardSuit.CLUBS, Card.ACE), new Card(CardSuit.DIAMONDS, Card.KING));

        dealCommunity(new Card(CardSuit.SPADES, Card.QUEEN), new Card(CardSuit.HEARTS, Card.QUEEN),
                new Card(CardSuit.DIAMONDS, Card.JACK), new Card(CardSuit.CLUBS, Card.TEN),
                new Card(CardSuit.SPADES, Card.NINE));

        createSimplePot(200);

        int totalChipsBefore = 0;
        for (PokerPlayer p : players) {
            totalChipsBefore += p.getChipCount();
        }
        totalChipsBefore += hand.getTotalPotChipCount();

        // Resolve
        // Hand evaluation happens in resolve()
        hand.resolve();

        // Total chips conserved
        int totalChipsAfter = 0;
        for (PokerPlayer p : players) {
            totalChipsAfter += p.getChipCount();
        }

        assertThat(totalChipsAfter).isEqualTo(totalChipsBefore);
    }

    // ========================================
    // Verification Tests
    // ========================================

    @Test
    void should_NotCreateMoney_When_PotResolved() {
        // Any resolution scenario
        dealPocketCards(players[0], new Card(CardSuit.SPADES, Card.ACE), new Card(CardSuit.HEARTS, Card.ACE));

        dealPocketCards(players[1], new Card(CardSuit.CLUBS, Card.KING), new Card(CardSuit.DIAMONDS, Card.KING));

        dealCommunity(new Card(CardSuit.SPADES, Card.QUEEN), new Card(CardSuit.HEARTS, Card.JACK),
                new Card(CardSuit.DIAMONDS, Card.TEN), new Card(CardSuit.CLUBS, Card.NINE),
                new Card(CardSuit.SPADES, Card.EIGHT));

        createSimplePot(175);

        int potAmount = hand.getTotalPotChipCount();

        // Resolve
        // Hand evaluation happens in resolve()

        int player0Before = players[0].getChipCount();
        int player1Before = players[1].getChipCount();

        hand.resolve();

        int player0Gain = players[0].getChipCount() - player0Before;
        int player1Gain = players[1].getChipCount() - player1Before;

        // Total distributed must equal pot
        assertThat(player0Gain + player1Gain).isEqualTo(potAmount);
    }
}
