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
package com.donohoedigital.games.poker.server;

import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.CardSuit;
import com.donohoedigital.games.poker.engine.Hand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for ServerStrategyProvider.
 */
class ServerStrategyProviderTest {

    @Test
    void getStratFactor_withDefaultValues_returnsMappedValue() {
        ServerStrategyProvider provider = new ServerStrategyProvider("player1");

        // Base value = 50, modifier = random -10 to +10
        // Result should be in range [min + (40/100)*(max-min), min +
        // (60/100)*(max-min)]
        // For range 0.0 to 1.0: [0.4, 0.6]
        float factor = provider.getStratFactor("test.factor", 0.0f, 1.0f);

        // Should be within plausible range (50 +/- 10 = 40-60, mapped to 0.0-1.0)
        assertThat(factor).isBetween(0.3f, 0.7f);
    }

    @Test
    void getStratFactor_sameFactorName_returnsSameValue() {
        ServerStrategyProvider provider = new ServerStrategyProvider("player1");

        float factor1 = provider.getStratFactor("consistency.test", 0.0f, 1.0f);
        float factor2 = provider.getStratFactor("consistency.test", 0.0f, 1.0f);

        // Same factor name should return same value (modifier is cached)
        assertThat(factor1).isEqualTo(factor2);
    }

    @Test
    void getStratFactor_differentPlayers_returnsDifferentModifiers() {
        ServerStrategyProvider provider1 = new ServerStrategyProvider("player1");
        ServerStrategyProvider provider2 = new ServerStrategyProvider("player2");

        float factor1 = provider1.getStratFactor("test.factor", 0.0f, 1.0f);
        float factor2 = provider2.getStratFactor("test.factor", 0.0f, 1.0f);

        // Different players should have different random modifiers (very unlikely to be
        // equal)
        // Run multiple times to avoid rare collision
        boolean foundDifference = false;
        for (int i = 0; i < 5; i++) {
            float f1 = provider1.getStratFactor("test" + i, 0.0f, 1.0f);
            float f2 = provider2.getStratFactor("test" + i, 0.0f, 1.0f);
            if (f1 != f2) {
                foundDifference = true;
                break;
            }
        }
        assertThat(foundDifference).isTrue();
    }

    @Test
    void getStratFactor_withNegativeRange_mapsProperly() {
        ServerStrategyProvider provider = new ServerStrategyProvider("player1");

        float factor = provider.getStratFactor("negative.range", -0.5f, 0.5f);

        // Should be in range [-0.5 + 0.4, -0.5 + 0.6] = [-0.1, 0.1] approximately
        assertThat(factor).isBetween(-0.6f, 0.6f);
    }

    @Test
    void getHandStrength_pocketAces_returnsHighStrength() {
        ServerStrategyProvider provider = new ServerStrategyProvider("player1");

        Hand pocket = new Hand();
        pocket.addCard(new Card(CardSuit.SPADES, Card.ACE));
        pocket.addCard(new Card(CardSuit.HEARTS, Card.ACE));

        float strength = provider.getHandStrength(pocket);

        // AA should be very strong (Sklansky Group 1)
        assertThat(strength).isGreaterThan(0.9f);
    }

    @Test
    void getHandStrength_pocketKings_returnsHighStrength() {
        ServerStrategyProvider provider = new ServerStrategyProvider("player1");

        Hand pocket = new Hand();
        pocket.addCard(new Card(CardSuit.SPADES, Card.KING));
        pocket.addCard(new Card(CardSuit.HEARTS, Card.KING));

        float strength = provider.getHandStrength(pocket);

        // KK should be very strong (Sklansky Group 1)
        assertThat(strength).isGreaterThan(0.9f);
    }

    @Test
    void getHandStrength_aceSuited_returnsGoodStrength() {
        ServerStrategyProvider provider = new ServerStrategyProvider("player1");

        Hand pocket = new Hand();
        pocket.addCard(new Card(CardSuit.SPADES, Card.ACE));
        pocket.addCard(new Card(CardSuit.SPADES, Card.KING));

        float strength = provider.getHandStrength(pocket);

        // AKs should be strong (Sklansky Group 1-2)
        assertThat(strength).isGreaterThan(0.85f);
    }

    @Test
    void getHandStrength_lowPair_returnsModerateStrength() {
        ServerStrategyProvider provider = new ServerStrategyProvider("player1");

        Hand pocket = new Hand();
        pocket.addCard(new Card(CardSuit.SPADES, Card.THREE));
        pocket.addCard(new Card(CardSuit.HEARTS, Card.THREE));

        float strength = provider.getHandStrength(pocket);

        // 33 should be moderate (Sklansky Group 5-8)
        assertThat(strength).isBetween(0.2f, 0.5f);
    }

    @Test
    void getHandStrength_trash_returnsLowStrength() {
        ServerStrategyProvider provider = new ServerStrategyProvider("player1");

        Hand pocket = new Hand();
        pocket.addCard(new Card(CardSuit.SPADES, Card.SEVEN));
        pocket.addCard(new Card(CardSuit.HEARTS, Card.TWO));

        float strength = provider.getHandStrength(pocket);

        // 72o should be weak
        assertThat(strength).isLessThan(0.3f);
    }

    @Test
    void getHandStrength_withTableSize_adjustsForShortHanded() {
        ServerStrategyProvider provider = new ServerStrategyProvider("player1");

        Hand pocket = new Hand();
        pocket.addCard(new Card(CardSuit.SPADES, Card.JACK));
        pocket.addCard(new Card(CardSuit.HEARTS, Card.TEN));

        float strengthFullTable = provider.getHandStrength(pocket, 9);
        float strengthShortTable = provider.getHandStrength(pocket, 5);
        float strengthHeadsUp = provider.getHandStrength(pocket, 2);

        // Strength should increase for short-handed play
        assertThat(strengthShortTable).isGreaterThan(strengthFullTable);
        assertThat(strengthHeadsUp).isGreaterThan(strengthShortTable);
    }

    @Test
    void getHandStrength_withTableSize_respectsMaximum() {
        ServerStrategyProvider provider = new ServerStrategyProvider("player1");

        // Already strong hand
        Hand pocket = new Hand();
        pocket.addCard(new Card(CardSuit.SPADES, Card.ACE));
        pocket.addCard(new Card(CardSuit.HEARTS, Card.ACE));

        float strengthHeadsUp = provider.getHandStrength(pocket, 2);

        // Should never exceed 1.0 even with heads-up bonus
        assertThat(strengthHeadsUp).isLessThanOrEqualTo(1.0f);
    }

    @Test
    void getHandStrength_nullHand_returnsZero() {
        ServerStrategyProvider provider = new ServerStrategyProvider("player1");

        float strength = provider.getHandStrength(null);

        assertThat(strength).isEqualTo(0.0f);
    }

    @Test
    void getHandStrength_emptyHand_returnsZero() {
        ServerStrategyProvider provider = new ServerStrategyProvider("player1");

        Hand pocket = new Hand();

        float strength = provider.getHandStrength(pocket);

        assertThat(strength).isEqualTo(0.0f);
    }

    @Test
    void getHandStrength_suitedConnectors_returnsMediumStrength() {
        ServerStrategyProvider provider = new ServerStrategyProvider("player1");

        Hand pocket = new Hand();
        pocket.addCard(new Card(CardSuit.SPADES, Card.NINE));
        pocket.addCard(new Card(CardSuit.SPADES, Card.EIGHT));

        float strength = provider.getHandStrength(pocket);

        // 98s should be medium-low playable hand
        assertThat(strength).isBetween(0.3f, 0.6f);
    }

    @Test
    void getStratFactor_withHandNoOp_delegatesToBaseFactor() {
        ServerStrategyProvider provider = new ServerStrategyProvider("player1");

        Hand pocket = new Hand();
        pocket.addCard(new Card(CardSuit.SPADES, Card.ACE));
        pocket.addCard(new Card(CardSuit.HEARTS, Card.ACE));

        // Currently hand-specific factors use default values (TODO in implementation)
        // So this should return same as non-hand version (with same modifier)
        float factorNoHand = provider.getStratFactor("test.factor", 0.0f, 1.0f);
        float factorWithHand = provider.getStratFactor("test.factor", pocket, 0.0f, 1.0f);

        // Should be equal since hand-specific lookup returns default
        assertThat(factorWithHand).isEqualTo(factorNoHand);
    }
}
