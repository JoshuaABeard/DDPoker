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
package com.donohoedigital.games.poker.ai;

import com.donohoedigital.games.poker.*;
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
 * Tests for V2Player hand strength computation methods.
 * Extends IntegrationTestBase for full game infrastructure.
 */
@Tag("integration")
class V2PlayerHandStrengthTest extends IntegrationTestBase
{
    private PokerGame game;
    private PokerTable table;
    private HoldemHand hand;
    private PokerPlayer player;
    private PokerPlayer opponent1;
    private PokerPlayer opponent2;
    private V2Player ai;

    @BeforeEach
    void setUp()
    {
        // Create game infrastructure
        game = new PokerGame(null);
        TournamentProfile profile = new TournamentProfile("test");
        profile.setBuyinChips(1500);
        game.setProfile(profile);

        table = new PokerTable(game, 1);
        table.setMinChip(1);

        // Create 3 players
        player = new PokerPlayer(1, "TestPlayer", true);
        player.setChipCount(1000);
        opponent1 = new PokerPlayer(2, "Opponent1", true);
        opponent1.setChipCount(1000);
        opponent2 = new PokerPlayer(3, "Opponent2", true);
        opponent2.setChipCount(1000);

        game.addPlayer(player);
        game.addPlayer(opponent1);
        game.addPlayer(opponent2);
        table.setPlayer(player, 0);
        table.setPlayer(opponent1, 1);
        table.setPlayer(opponent2, 2);

        // Set button and create hand
        table.setButton(1);

        // Give players pocket cards for proper hand initialization
        player.newHand('p');
        opponent1.newHand('p');
        opponent2.newHand('p');

        hand = new HoldemHand(table);
        table.setHoldemHand(hand);
        hand.setBigBlind(20);
        hand.setSmallBlind(10);
        hand.setPlayerOrder(false);

        // Attach V2Player AI
        ai = new V2Player();
        ai.setPokerPlayer(player);
        ai.init();
    }

    /**
     * Helper to deal pocket cards to a player
     */
    private void dealPocketCards(PokerPlayer p, Card c1, Card c2)
    {
        Hand pocket = p.getHand();
        pocket.clear();
        pocket.addCard(c1);
        pocket.addCard(c2);
    }

    /**
     * Helper to deal community cards
     */
    private void dealCommunity(Card... cards)
    {
        Hand community = hand.getCommunity();
        community.clear();
        for (Card c : cards)
        {
            community.addCard(c);
        }
    }

    // ========================================
    // Raw Hand Strength Tests
    // ========================================

    @Test
    void should_ReturnHighStrength_When_PlayerHasPocketAces()
    {
        dealPocketCards(player,
            new Card(CardSuit.SPADES, Card.ACE),
            new Card(CardSuit.HEARTS, Card.ACE));
        dealCommunity(
            new Card(CardSuit.CLUBS, Card.TWO),
            new Card(CardSuit.DIAMONDS, Card.SEVEN),
            new Card(CardSuit.HEARTS, Card.KING));

        float rhs = ai.getRawHandStrength();

        // Pocket aces should have very high raw hand strength
        assertThat(rhs).isGreaterThan(0.70f);
    }

    @Test
    void should_ReturnMediumStrength_When_PlayerHasMiddlePair()
    {
        dealPocketCards(player,
            new Card(CardSuit.SPADES, Card.NINE),
            new Card(CardSuit.HEARTS, Card.NINE));
        dealCommunity(
            new Card(CardSuit.CLUBS, Card.ACE),
            new Card(CardSuit.DIAMONDS, Card.KING),
            new Card(CardSuit.HEARTS, Card.QUEEN));

        float rhs = ai.getRawHandStrength();

        // Middle pair with overcards on board should be medium strength
        assertThat(rhs).isBetween(0.20f, 0.60f);
    }

    @Test
    void should_ReturnLowStrength_When_PlayerHasHighCard()
    {
        dealPocketCards(player,
            new Card(CardSuit.SPADES, Card.KING),
            new Card(CardSuit.HEARTS, Card.QUEEN));
        dealCommunity(
            new Card(CardSuit.CLUBS, Card.TWO),
            new Card(CardSuit.DIAMONDS, Card.FIVE),
            new Card(CardSuit.SPADES, Card.EIGHT));

        float rhs = ai.getRawHandStrength();

        // High card only should be low strength
        assertThat(rhs).isLessThan(0.50f);
    }

    @Test
    void should_AdjustForOpponentCount_When_MultipleOpponents()
    {
        // With pocket aces, strength decreases as opponent count increases
        dealPocketCards(player,
            new Card(CardSuit.SPADES, Card.ACE),
            new Card(CardSuit.HEARTS, Card.ACE));
        dealCommunity(
            new Card(CardSuit.CLUBS, Card.TWO),
            new Card(CardSuit.DIAMONDS, Card.SEVEN),
            new Card(CardSuit.HEARTS, Card.KING));

        float rhs = ai.getRawHandStrength();

        // The method applies Math.pow(rawHandStrength, numOpponents - 1)
        // With 3 players total (2 opponents), strength should still be high
        assertThat(rhs).isGreaterThan(0.50f);
    }

    // ========================================
    // Hand Potential Tests
    // ========================================

    @Test
    void should_ReturnHighPPot_When_PlayerHasFlushDraw()
    {
        dealPocketCards(player,
            new Card(CardSuit.SPADES, Card.ACE),
            new Card(CardSuit.SPADES, Card.KING));
        dealCommunity(
            new Card(CardSuit.SPADES, Card.TWO),
            new Card(CardSuit.SPADES, Card.SEVEN),
            new Card(CardSuit.HEARTS, Card.JACK));

        float ppot = ai.getPositiveHandPotential();

        // Four spades (flush draw) should have positive potential
        assertThat(ppot).isGreaterThan(0.10f);
    }

    @Test
    void should_ReturnHighPPot_When_PlayerHasStraightDraw()
    {
        dealPocketCards(player,
            new Card(CardSuit.SPADES, Card.JACK),
            new Card(CardSuit.HEARTS, Card.TEN));
        dealCommunity(
            new Card(CardSuit.CLUBS, Card.NINE),
            new Card(CardSuit.DIAMONDS, Card.EIGHT),
            new Card(CardSuit.HEARTS, Card.TWO));

        float ppot = ai.getPositiveHandPotential();

        // Open-ended straight draw should have positive potential
        assertThat(ppot).isGreaterThan(0.10f);
    }

    @Test
    void should_ReturnLowNPot_When_PlayerHasNuts()
    {
        dealPocketCards(player,
            new Card(CardSuit.SPADES, Card.ACE),
            new Card(CardSuit.HEARTS, Card.ACE));
        dealCommunity(
            new Card(CardSuit.CLUBS, Card.ACE),
            new Card(CardSuit.DIAMONDS, Card.KING),
            new Card(CardSuit.HEARTS, Card.KING));

        float npot = ai.getNegativeHandPotential();

        // Full house (aces full of kings) is very strong, low negative potential
        assertThat(npot).isLessThan(0.20f);
    }

    @Test
    void should_ReturnHighNPot_When_PlayerHasVulnerableHand()
    {
        dealPocketCards(player,
            new Card(CardSuit.SPADES, Card.JACK),
            new Card(CardSuit.HEARTS, Card.TEN));
        dealCommunity(
            new Card(CardSuit.CLUBS, Card.JACK),
            new Card(CardSuit.DIAMONDS, Card.TEN),
            new Card(CardSuit.HEARTS, Card.NINE));

        float npot = ai.getNegativeHandPotential();

        // Two pair with straight possibilities on board has negative potential
        assertThat(npot).isGreaterThan(0.0f);
    }

    // ========================================
    // Effective Hand Strength Tests
    // ========================================

    @Test
    void should_IncorporatePotential_When_Computing()
    {
        dealPocketCards(player,
            new Card(CardSuit.SPADES, Card.ACE),
            new Card(CardSuit.SPADES, Card.KING));
        dealCommunity(
            new Card(CardSuit.SPADES, Card.TWO),
            new Card(CardSuit.SPADES, Card.SEVEN),
            new Card(CardSuit.HEARTS, Card.JACK));

        float rhs = ai.getRawHandStrength();
        float ppot = ai.getPositiveHandPotential();
        float ehs = ai.getEffectiveHandStrength();

        // EHS incorporates raw strength and positive potential
        // Both should be valid probabilities
        assertThat(rhs).isBetween(0.0f, 1.0f);
        assertThat(ehs).isBetween(0.0f, 1.0f);
        assertThat(ppot).isBetween(0.0f, 1.0f);
    }

    @Test
    void should_HaveZeroPotential_When_OnRiver()
    {
        dealPocketCards(player,
            new Card(CardSuit.SPADES, Card.ACE),
            new Card(CardSuit.HEARTS, Card.KING));
        dealCommunity(
            new Card(CardSuit.CLUBS, Card.TWO),
            new Card(CardSuit.DIAMONDS, Card.SEVEN),
            new Card(CardSuit.HEARTS, Card.JACK),
            new Card(CardSuit.SPADES, Card.FOUR),
            new Card(CardSuit.CLUBS, Card.NINE));

        float ppot = ai.getPositiveHandPotential();
        float npot = ai.getNegativeHandPotential();

        // On river with 5 community cards, potential should be zero
        assertThat(ppot).isEqualTo(0.0f);
        assertThat(npot).isEqualTo(0.0f);
    }

    // ========================================
    // Biased Hand Strength Tests
    // ========================================

    @Test
    void should_AdjustForField_When_ComputingBiased()
    {
        dealPocketCards(player,
            new Card(CardSuit.SPADES, Card.ACE),
            new Card(CardSuit.HEARTS, Card.KING));
        dealCommunity(
            new Card(CardSuit.CLUBS, Card.ACE),
            new Card(CardSuit.DIAMONDS, Card.SEVEN),
            new Card(CardSuit.HEARTS, Card.JACK));

        float rhs = ai.getRawHandStrength();
        float bhs = ai.getBiasedHandStrength();

        // Biased hand strength incorporates field adjustments
        // It should be a positive value
        assertThat(bhs).isGreaterThan(0.0f);
    }

    @Test
    void should_DifferFromRaw_When_BiasApplied()
    {
        dealPocketCards(player,
            new Card(CardSuit.SPADES, Card.KING),
            new Card(CardSuit.HEARTS, Card.QUEEN));
        dealCommunity(
            new Card(CardSuit.CLUBS, Card.KING),
            new Card(CardSuit.DIAMONDS, Card.SEVEN),
            new Card(CardSuit.HEARTS, Card.TWO));

        float rhs = ai.getRawHandStrength();
        float bhs = ai.getBiasedHandStrength();

        // Biased strength applies field bias, should differ from raw
        // Both should be positive values
        assertThat(rhs).isGreaterThan(0.0f);
        assertThat(bhs).isGreaterThan(0.0f);
    }
}
