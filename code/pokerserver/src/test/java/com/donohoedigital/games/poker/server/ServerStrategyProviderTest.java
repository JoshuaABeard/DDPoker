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

        // Currently hand-specific factors use default values (Future: implement
        // hand-specific lookup)
        // So this should return same as non-hand version (with same modifier)
        float factorNoHand = provider.getStratFactor("test.factor", 0.0f, 1.0f);
        float factorWithHand = provider.getStratFactor("test.factor", pocket, 0.0f, 1.0f);

        // Should be equal since hand-specific lookup returns default
        assertThat(factorWithHand).isEqualTo(factorNoHand);
    }

    // === Hand categorization via getStratFactor with hand ===

    @Test
    void getStratFactor_withBigPair_usesHandCategory() {
        ServerStrategyProvider provider = new ServerStrategyProvider("player1");

        // JJ-AA = big_pair
        Hand bigPair = new Hand();
        bigPair.addCard(new Card(CardSuit.SPADES, Card.QUEEN));
        bigPair.addCard(new Card(CardSuit.HEARTS, Card.QUEEN));

        float factor = provider.getStratFactor("basics.aggression", bigPair, 0.0f, 1.0f);
        assertThat(factor).isBetween(0.0f, 1.0f);
    }

    @Test
    void getStratFactor_withMediumPair_usesHandCategory() {
        ServerStrategyProvider provider = new ServerStrategyProvider("player1");

        // 77-TT = medium_pair
        Hand medPair = new Hand();
        medPair.addCard(new Card(CardSuit.SPADES, Card.EIGHT));
        medPair.addCard(new Card(CardSuit.HEARTS, Card.EIGHT));

        float factor = provider.getStratFactor("basics.tightness", medPair, 0.0f, 1.0f);
        assertThat(factor).isBetween(0.0f, 1.0f);
    }

    @Test
    void getStratFactor_withSmallPair_usesHandCategory() {
        ServerStrategyProvider provider = new ServerStrategyProvider("player1");

        // 22-66 = small_pair
        Hand smallPair = new Hand();
        smallPair.addCard(new Card(CardSuit.SPADES, Card.FOUR));
        smallPair.addCard(new Card(CardSuit.HEARTS, Card.FOUR));

        float factor = provider.getStratFactor("basics.aggression", smallPair, 0.0f, 1.0f);
        assertThat(factor).isBetween(0.0f, 1.0f);
    }

    @Test
    void getStratFactor_withSuitedHighCards_usesHandCategory() {
        ServerStrategyProvider provider = new ServerStrategyProvider("player1");

        // Both cards T or higher, suited = suited_high_cards
        Hand suitedHighCards = new Hand();
        suitedHighCards.addCard(new Card(CardSuit.SPADES, Card.KING));
        suitedHighCards.addCard(new Card(CardSuit.SPADES, Card.JACK));

        float factor = provider.getStratFactor("basics.aggression", suitedHighCards, 0.0f, 1.0f);
        assertThat(factor).isBetween(0.0f, 1.0f);
    }

    @Test
    void getStratFactor_withUnsuitedHighCards_usesHandCategory() {
        ServerStrategyProvider provider = new ServerStrategyProvider("player1");

        // Both cards T or higher, unsuited = unsuited_high_cards
        Hand unsuitedHighCards = new Hand();
        unsuitedHighCards.addCard(new Card(CardSuit.SPADES, Card.KING));
        unsuitedHighCards.addCard(new Card(CardSuit.HEARTS, Card.JACK));

        float factor = provider.getStratFactor("basics.aggression", unsuitedHighCards, 0.0f, 1.0f);
        assertThat(factor).isBetween(0.0f, 1.0f);
    }

    @Test
    void getStratFactor_withSuitedAce_usesHandCategory() {
        ServerStrategyProvider provider = new ServerStrategyProvider("player1");

        // Ace + low kicker, suited = suited_ace
        Hand suitedAce = new Hand();
        suitedAce.addCard(new Card(CardSuit.SPADES, Card.ACE));
        suitedAce.addCard(new Card(CardSuit.SPADES, Card.FIVE));

        float factor = provider.getStratFactor("basics.aggression", suitedAce, 0.0f, 1.0f);
        assertThat(factor).isBetween(0.0f, 1.0f);
    }

    @Test
    void getStratFactor_withUnsuitedAce_usesHandCategory() {
        ServerStrategyProvider provider = new ServerStrategyProvider("player1");

        // Ace + low kicker, unsuited = unsuited_ace
        Hand unsuitedAce = new Hand();
        unsuitedAce.addCard(new Card(CardSuit.SPADES, Card.ACE));
        unsuitedAce.addCard(new Card(CardSuit.HEARTS, Card.FIVE));

        float factor = provider.getStratFactor("basics.aggression", unsuitedAce, 0.0f, 1.0f);
        assertThat(factor).isBetween(0.0f, 1.0f);
    }

    @Test
    void getStratFactor_withOtherHand_usesHandCategory() {
        ServerStrategyProvider provider = new ServerStrategyProvider("player1");

        // Non-connector, non-ace, non-pair, non-high-cards = "other"
        Hand other = new Hand();
        other.addCard(new Card(CardSuit.SPADES, Card.NINE));
        other.addCard(new Card(CardSuit.HEARTS, Card.THREE));

        float factor = provider.getStratFactor("basics.aggression", other, 0.0f, 1.0f);
        assertThat(factor).isBetween(0.0f, 1.0f);
    }

    // === EmbeddedHandStrength table size variations ===

    @Test
    void getHandStrength_headsUp_usesHeadsUpTable() {
        ServerStrategyProvider provider = new ServerStrategyProvider("player1");

        // Low suited connectors should be playable heads-up
        Hand pocket = new Hand();
        pocket.addCard(new Card(CardSuit.SPADES, Card.SIX));
        pocket.addCard(new Card(CardSuit.SPADES, Card.FIVE));

        float strength = provider.getHandStrength(pocket, 2);
        assertThat(strength).isGreaterThan(0.0f);
    }

    @Test
    void getHandStrength_veryShortTable_usesVeryShortTable() {
        ServerStrategyProvider provider = new ServerStrategyProvider("player1");

        Hand pocket = new Hand();
        pocket.addCard(new Card(CardSuit.SPADES, Card.ACE));
        pocket.addCard(new Card(CardSuit.HEARTS, Card.ACE));

        float strength = provider.getHandStrength(pocket, 3);
        assertThat(strength).isEqualTo(1.0f); // AA = group 10 = 1.0
    }

    @Test
    void getHandStrength_shortTable_usesShortTable() {
        ServerStrategyProvider provider = new ServerStrategyProvider("player1");

        Hand pocket = new Hand();
        pocket.addCard(new Card(CardSuit.SPADES, Card.ACE));
        pocket.addCard(new Card(CardSuit.HEARTS, Card.ACE));

        float strength = provider.getHandStrength(pocket, 5);
        assertThat(strength).isEqualTo(1.0f); // AA = group 10 = 1.0
    }

    @Test
    void getHandStrength_fullTable_usesFullTable() {
        ServerStrategyProvider provider = new ServerStrategyProvider("player1");

        Hand pocket = new Hand();
        pocket.addCard(new Card(CardSuit.SPADES, Card.ACE));
        pocket.addCard(new Card(CardSuit.HEARTS, Card.ACE));

        float strength = provider.getHandStrength(pocket, 9);
        assertThat(strength).isEqualTo(1.0f); // AA = group 10 = 1.0
    }

    @Test
    void getHandStrength_singleCard_returnsZero() {
        ServerStrategyProvider provider = new ServerStrategyProvider("player1");

        Hand pocket = new Hand();
        pocket.addCard(new Card(CardSuit.SPADES, Card.ACE));

        float strength = provider.getHandStrength(pocket);
        assertThat(strength).isEqualTo(0.0f);
    }

    @Test
    void getHandStrength_suitedConnectors_headsUp_returnsPositive() {
        ServerStrategyProvider provider = new ServerStrategyProvider("player1");

        // 98s should be in heads-up data
        Hand pocket = new Hand();
        pocket.addCard(new Card(CardSuit.HEARTS, Card.NINE));
        pocket.addCard(new Card(CardSuit.HEARTS, Card.EIGHT));

        float strength = provider.getHandStrength(pocket, 2);
        assertThat(strength).isGreaterThan(0.0f);
    }

    @Test
    void getHandStrength_offsuit_lowCards_fullTable_returnsLow() {
        ServerStrategyProvider provider = new ServerStrategyProvider("player1");

        // 32o should be very low on full table
        Hand pocket = new Hand();
        pocket.addCard(new Card(CardSuit.SPADES, Card.THREE));
        pocket.addCard(new Card(CardSuit.HEARTS, Card.TWO));

        float strength = provider.getHandStrength(pocket, 9);
        // May or may not be found in full table data
        assertThat(strength).isBetween(0.0f, 0.3f);
    }

    // === Constructor with explicit StrategyData ===

    @Test
    void constructor_withNullStrategyData_usesFallback() {
        ServerStrategyProvider provider = new ServerStrategyProvider("player1", (StrategyData) null);

        // Should not throw and should work with fallback data
        float factor = provider.getStratFactor("basics.aggression", 0.0f, 1.0f);
        assertThat(factor).isBetween(0.0f, 1.0f);
    }

    @Test
    void constructor_withCustomStrategyData_usesProvidedValues() {
        StrategyData customData = new StrategyData("Custom", "Custom strategy");
        customData.setStrategyFactor("test.custom", 80);

        ServerStrategyProvider provider = new ServerStrategyProvider("player1", customData);

        // The factor with base value 80 should map to higher range
        float factor = provider.getStratFactor("test.custom", 0.0f, 1.0f);
        // 80 +/- 10 modifier = 70-90, mapped to 0.0-1.0 = 0.7-0.9
        assertThat(factor).isBetween(0.6f, 1.0f);
    }
}
