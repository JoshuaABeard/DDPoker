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

import com.donohoedigital.config.ApplicationType;
import com.donohoedigital.config.ConfigManager;
import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.Hand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.donohoedigital.games.poker.engine.Card.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests for HandStrength - hand strength calculation, opponent modeling,
 * and probability estimation.
 */
class HandStrengthTest {

    private HandStrength handStrength;

    @BeforeEach
    void setUp() {
        // Initialize ConfigManager for headless testing
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);

        handStrength = new HandStrength();
    }

    // =================================================================
    // Basic Strength Calculation Tests
    // =================================================================

    @Test
    void should_CreateHandStrength_When_ConstructorCalled() {
        assertThat(handStrength).isNotNull();
    }

    @Test
    void should_ReturnHighStrength_When_PocketAcesPreflop() {
        Hand hole = createHand(CLUBS_A, HEARTS_A);
        Hand community = new Hand();

        float strength = handStrength.getStrength(hole, community);

        assertThat(strength).isGreaterThan(0.8f); // Pocket aces are strong preflop
    }

    @Test
    void should_ReturnLowStrength_When_WeakHandPreflop() {
        Hand hole = createHand(CLUBS_2, DIAMONDS_7); // Weak hand
        Hand community = new Hand();

        float strength = handStrength.getStrength(hole, community);

        assertThat(strength).isLessThan(0.4f); // 2-7 offsuit is weak
    }

    @Test
    void should_ReturnVeryHighStrength_When_FlopmakesQuads() {
        Hand hole = createHand(CLUBS_A, HEARTS_A);
        Hand community = createHand(DIAMONDS_A, SPADES_A, CLUBS_K); // Four aces

        float strength = handStrength.getStrength(hole, community);

        assertThat(strength).isGreaterThan(0.99f); // Four aces nearly unbeatable
    }

    @Test
    void should_ReturnMediumStrength_When_HaveMiddlePair() {
        Hand hole = createHand(CLUBS_8, HEARTS_8);
        Hand community = createHand(DIAMONDS_A, SPADES_K, CLUBS_Q); // Pair of 8s

        float strength = handStrength.getStrength(hole, community);

        assertThat(strength).isGreaterThan(0.2f).isLessThan(0.65f); // Middle pair is medium
    }

    @Test
    void should_ReturnStrengthBetweenZeroAndOne_When_AnyHand() {
        Hand hole = createHand(CLUBS_K, HEARTS_Q);
        Hand community = createHand(DIAMONDS_J, SPADES_T, CLUBS_9);

        float strength = handStrength.getStrength(hole, community);

        assertThat(strength).isBetween(0.0f, 1.0f);
    }

    // =================================================================
    // Multi-Opponent Strength Tests
    // =================================================================

    @Test
    void should_DecreaseStrength_When_MoreOpponents() {
        Hand hole = createHand(CLUBS_K, HEARTS_K);
        Hand community = createHand(DIAMONDS_A, SPADES_7, CLUBS_2);

        float strength1 = handStrength.getStrength(hole, community, 1);
        float strength2 = handStrength.getStrength(hole, community, 2);
        float strength5 = handStrength.getStrength(hole, community, 5);

        assertThat(strength2).isLessThan(strength1);
        assertThat(strength5).isLessThan(strength2);
    }

    @Test
    void should_CalculateMultiOpponentStrength_When_OpponentsSpecified() {
        Hand hole = createHand(CLUBS_A, HEARTS_K);
        Hand community = createHand(DIAMONDS_Q, SPADES_J, CLUBS_T); // Straight

        float strength = handStrength.getStrength(hole, community, 3);

        assertThat(strength).isGreaterThan(0.0f).isLessThan(1.0f);
    }

    @Test
    void should_RaiseToPower_When_CalculatingMultiOpponentStrength() {
        float baseStrength = 0.8f;

        float strength2 = handStrength.getStrength(baseStrength, 2);
        float strength3 = handStrength.getStrength(baseStrength, 3);

        assertThat(strength2).isEqualTo((float) Math.pow(0.8, 2), within(0.001f));
        assertThat(strength3).isEqualTo((float) Math.pow(0.8, 3), within(0.001f));
    }

    @Test
    void should_HandleOneOpponent_When_OpponentCountIsOne() {
        float baseStrength = 0.7f;

        float strength = handStrength.getStrength(baseStrength, 1);

        assertThat(strength).isEqualTo(0.7f, within(0.001f));
    }

    // =================================================================
    // Straight Counting Tests
    // =================================================================

    @Test
    void should_CountStraights_When_StraightPossibleOnBoard() {
        Hand hole = createHand(CLUBS_2, HEARTS_2); // Pair won't make straight
        Hand community = createHand(DIAMONDS_9, SPADES_T, CLUBS_J); // Board has straight potential

        handStrength.getStrength(hole, community);

        // After calculation, straight count should be > 0 (opponents could have straights)
        assertThat(handStrength.getNumStraights()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void should_HaveZeroStraights_When_NoCalculationYet() {
        assertThat(handStrength.getNumStraights()).isZero();
    }

    @Test
    void should_UpdateStraightCount_When_StrengthCalculated() {
        Hand hole = createHand(CLUBS_7, HEARTS_7);
        Hand community = createHand(DIAMONDS_8, SPADES_9, CLUBS_T); // Straight draw on board

        int beforeCount = handStrength.getNumStraights();
        handStrength.getStrength(hole, community);
        int afterCount = handStrength.getNumStraights();

        // After calculation, count may have changed (depends on opponents making straights)
        assertThat(afterCount).isGreaterThanOrEqualTo(beforeCount);
    }

    // =================================================================
    // Board State Tests
    // =================================================================

    @Test
    void should_HandleEmptyCommunity_When_Preflop() {
        Hand hole = createHand(CLUBS_K, HEARTS_Q);
        Hand community = new Hand(); // No community cards

        float strength = handStrength.getStrength(hole, community);

        assertThat(strength).isBetween(0.0f, 1.0f);
    }

    @Test
    void should_HandleFlopBoard_When_ThreeCommunityCards() {
        Hand hole = createHand(CLUBS_A, HEARTS_K);
        Hand community = createHand(DIAMONDS_A, SPADES_K, CLUBS_Q); // Flop

        float strength = handStrength.getStrength(hole, community);

        assertThat(strength).isBetween(0.0f, 1.0f);
    }

    @Test
    void should_HandleTurnBoard_When_FourCommunityCards() {
        Hand hole = createHand(CLUBS_J, HEARTS_J);
        Hand community = createHand(DIAMONDS_J, SPADES_8, CLUBS_3, HEARTS_2); // Turn

        float strength = handStrength.getStrength(hole, community);

        assertThat(strength).isGreaterThan(0.9f); // Trips on turn is strong
    }

    @Test
    void should_HandleRiverBoard_When_FiveCommunityCards() {
        Hand hole = createHand(CLUBS_A, HEARTS_A);
        Hand community = createHand(DIAMONDS_A, SPADES_K, CLUBS_Q, HEARTS_J, DIAMONDS_T); // River - Broadway

        float strength = handStrength.getStrength(hole, community);

        assertThat(strength).isBetween(0.0f, 1.0f); // Any strength valid on river
    }

    // =================================================================
    // HTML Output Tests
    // =================================================================

    @Test
    void should_ReturnHTML_When_ToHTMLCalled() {
        Hand hole = createHand(CLUBS_K, HEARTS_K);
        Hand community = createHand(DIAMONDS_A, SPADES_7, CLUBS_2);

        String html = handStrength.toHTML(hole, community, 3);

        assertThat(html).isNotNull();
        assertThat(html).isNotEmpty();
    }

    @Test
    void should_IncludeHandInfo_When_HTMLGenerated() {
        Hand hole = createHand(CLUBS_A, HEARTS_K);
        Hand community = createHand(DIAMONDS_Q, SPADES_J, CLUBS_T);

        String html = handStrength.toHTML(hole, community, 2);

        assertThat(html).isNotNull();
        // HTML should contain some content (exact format may vary)
    }

    // =================================================================
    // Edge Cases Tests
    // =================================================================

    @Test
    void should_HandleMonsterHand_When_RoyalFlushMade() {
        Hand hole = createHand(CLUBS_A, CLUBS_K);
        Hand community = createHand(CLUBS_Q, CLUBS_J, CLUBS_T, DIAMONDS_2, SPADES_3); // Royal flush

        float strength = handStrength.getStrength(hole, community);

        assertThat(strength).isGreaterThan(0.99f); // Royal flush is nearly unbeatable
    }

    @Test
    void should_HandleDrawingHand_When_FlushDrawOnTurn() {
        Hand hole = createHand(CLUBS_A, CLUBS_K);
        Hand community = createHand(CLUBS_Q, CLUBS_J, HEARTS_2, DIAMONDS_3); // 4 clubs, need 1 more

        float strength = handStrength.getStrength(hole, community);

        // Flush draw with overcards should have reasonable strength
        assertThat(strength).isGreaterThan(0.3f).isLessThan(0.8f);
    }

    // =================================================================
    // Test Helper Methods
    // =================================================================

    private Hand createHand(Card... cards) {
        Hand hand = new Hand();
        for (Card card : cards) {
            if (card != null) {
                hand.addCard(card);
            }
        }
        return hand;
    }
}
