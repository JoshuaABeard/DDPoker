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

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.donohoedigital.config.ConfigTestHelper;
import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.protocol.dto.HandEvaluationData;

class HandEvaluationHelperTest {

    @BeforeAll
    static void initConfig() {
        ConfigTestHelper.initializeForTesting("poker");
    }

    // Helper to create cards from string notation
    private static Card c(String s) {
        return Card.getCard(s);
    }

    // =========================================================================
    // Edge cases — null/empty inputs
    // =========================================================================

    @Test
    void evaluate_nullHoleCards_returnsNone() {
        HandEvaluationData result = HandEvaluationHelper.evaluate((List<Card>) null, List.of());
        assertThat(result).isEqualTo(HandEvaluationData.NONE);
    }

    @Test
    void evaluate_emptyHoleCards_returnsNone() {
        HandEvaluationData result = HandEvaluationHelper.evaluate(List.of(), List.of());
        assertThat(result).isEqualTo(HandEvaluationData.NONE);
    }

    @Test
    void evaluate_nullCommunityCards_doesNotThrow() {
        HandEvaluationData result = HandEvaluationHelper.evaluate(List.of(c("As"), c("Ks")), null);
        assertThat(result).isNotNull();
        assertThat(result.score()).isGreaterThan(0);
    }

    // =========================================================================
    // Array overload
    // =========================================================================

    @Test
    void evaluate_arrayOverload_nullHoleCards_returnsNone() {
        HandEvaluationData result = HandEvaluationHelper.evaluate((Card[]) null, null);
        assertThat(result).isEqualTo(HandEvaluationData.NONE);
    }

    @Test
    void evaluate_arrayOverload_worksLikeListOverload() {
        Card[] hole = {c("As"), c("Ks")};
        Card[] community = {c("Qs"), c("Js"), c("Ts")};

        HandEvaluationData arrayResult = HandEvaluationHelper.evaluate(hole, community);
        HandEvaluationData listResult = HandEvaluationHelper.evaluate(List.of(c("As"), c("Ks")),
                List.of(c("Qs"), c("Js"), c("Ts")));

        assertThat(arrayResult.score()).isEqualTo(listResult.score());
        assertThat(arrayResult.handType()).isEqualTo(listResult.handType());
    }

    // =========================================================================
    // Hand type detection
    // =========================================================================

    @Test
    void evaluate_royalFlush_detectsCorrectly() {
        HandEvaluationData result = HandEvaluationHelper.evaluate(List.of(c("As"), c("Ks")),
                List.of(c("Qs"), c("Js"), c("Ts")));

        assertThat(result.handDescription()).isEqualTo("Royal Flush");
        assertThat(result.bestFiveCards()).hasSize(5);
    }

    @Test
    void evaluate_straightFlush_detectsCorrectly() {
        HandEvaluationData result = HandEvaluationHelper.evaluate(List.of(c("9h"), c("8h")),
                List.of(c("7h"), c("6h"), c("5h")));

        assertThat(result.handDescription()).contains("Straight Flush");
        assertThat(result.straightHighRank()).isNotNull();
    }

    @Test
    void evaluate_fourOfAKind_detectsCorrectly() {
        HandEvaluationData result = HandEvaluationHelper.evaluate(List.of(c("As"), c("Ah")),
                List.of(c("Ad"), c("Ac"), c("2c")));

        assertThat(result.handDescription()).contains("Four of a Kind");
        assertThat(result.quadsRank()).isNotNull();
    }

    @Test
    void evaluate_fullHouse_detectsCorrectly() {
        HandEvaluationData result = HandEvaluationHelper.evaluate(List.of(c("As"), c("Ah")),
                List.of(c("Ad"), c("Kc"), c("Kh")));

        assertThat(result.handDescription()).contains("Full House");
        assertThat(result.tripsRank()).isNotNull();
        assertThat(result.bigPairRank()).isNotNull();
    }

    @Test
    void evaluate_flush_detectsCorrectly() {
        HandEvaluationData result = HandEvaluationHelper.evaluate(List.of(c("Ah"), c("Th")),
                List.of(c("7h"), c("4h"), c("2h")));

        assertThat(result.handDescription()).contains("Flush");
        assertThat(result.flushHighRank()).isNotNull();
    }

    @Test
    void evaluate_straight_detectsCorrectly() {
        HandEvaluationData result = HandEvaluationHelper.evaluate(List.of(c("As"), c("Kh")),
                List.of(c("Qd"), c("Jc"), c("Ts")));

        assertThat(result.handDescription()).contains("Straight");
        assertThat(result.straightHighRank()).isNotNull();
        assertThat(result.straightLowRank()).isNotNull();
    }

    @Test
    void evaluate_trips_detectsCorrectly() {
        HandEvaluationData result = HandEvaluationHelper.evaluate(List.of(c("As"), c("Ah")),
                List.of(c("Ad"), c("Kc"), c("2c")));

        assertThat(result.handDescription()).contains("Three of a Kind");
        assertThat(result.tripsRank()).isNotNull();
    }

    @Test
    void evaluate_twoPair_detectsCorrectly() {
        HandEvaluationData result = HandEvaluationHelper.evaluate(List.of(c("As"), c("Ah")),
                List.of(c("Kd"), c("Kc"), c("2c")));

        assertThat(result.handDescription()).contains("Two Pair");
        assertThat(result.bigPairRank()).isNotNull();
        assertThat(result.smallPairRank()).isNotNull();
    }

    @Test
    void evaluate_onePair_detectsCorrectly() {
        HandEvaluationData result = HandEvaluationHelper.evaluate(List.of(c("As"), c("Ah")),
                List.of(c("Kd"), c("7c"), c("2c")));

        assertThat(result.handDescription()).contains("One Pair");
        assertThat(result.bigPairRank()).isNotNull();
    }

    @Test
    void evaluate_highCard_detectsCorrectly() {
        HandEvaluationData result = HandEvaluationHelper.evaluate(List.of(c("As"), c("Kh")),
                List.of(c("7d"), c("4c"), c("2h")));

        assertThat(result.handDescription()).contains("High Card");
        assertThat(result.highCardRank()).isNotNull();
    }

    // =========================================================================
    // Incomplete hands (fewer than 5 cards)
    // =========================================================================

    @Test
    void evaluate_twoCardsOnly_returnsScoreButNoDescription() {
        HandEvaluationData result = HandEvaluationHelper.evaluate(List.of(c("As"), c("Kh")), List.of());

        assertThat(result.score()).isGreaterThan(0);
        assertThat(result.handDescription()).isEmpty();
        assertThat(result.bestFiveCards()).isEmpty();
    }

    @Test
    void evaluate_fourCards_returnsScoreButNoDescription() {
        HandEvaluationData result = HandEvaluationHelper.evaluate(List.of(c("As"), c("Kh")), List.of(c("Qd"), c("Jc")));

        assertThat(result.score()).isGreaterThan(0);
        assertThat(result.handDescription()).isEmpty();
        assertThat(result.bestFiveCards()).isEmpty();
    }

    // =========================================================================
    // Best five cards
    // =========================================================================

    @Test
    void evaluate_sevenCards_returnsBestFiveCardsFromSeven() {
        HandEvaluationData result = HandEvaluationHelper.evaluate(List.of(c("As"), c("Kh")),
                List.of(c("Qd"), c("Jc"), c("Ts"), c("2c"), c("3h")));

        assertThat(result.bestFiveCards()).hasSize(5);
    }
}
