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

import com.donohoedigital.games.poker.CommunityCardCalculator.CommunityCardVisibility;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class CommunityCardCalculatorTest {

    // Round constants from HoldemHand
    private static final int PRE_FLOP = HoldemHand.ROUND_PRE_FLOP; // 0
    private static final int FLOP = HoldemHand.ROUND_FLOP; // 1
    private static final int TURN = HoldemHand.ROUND_TURN; // 2
    private static final int RIVER = HoldemHand.ROUND_RIVER; // 3
    private static final int SHOWDOWN = HoldemHand.ROUND_SHOWDOWN; // 4

    // =================================================================
    // Pre-Flop tests
    // =================================================================

    @Nested
    class PreFlop {

        @Test
        void should_HaveNoCardsDrawn_When_PreFlop() {
            CommunityCardVisibility v = CommunityCardCalculator.calculateVisibility(PRE_FLOP, PRE_FLOP, 2, false);

            assertThat(v.active()).containsExactly(false, false, false, false, false);
            assertThat(v.drawnNormal()).containsExactly(false, false, false, false, false);
            assertThat(v.drawn()).containsExactly(false, false, false, false, false);
        }
    }

    // =================================================================
    // Flop tests
    // =================================================================

    @Nested
    class FlopRound {

        @Test
        void should_DrawThreeCards_When_FlopRound() {
            CommunityCardVisibility v = CommunityCardCalculator.calculateVisibility(FLOP, FLOP, 2, false);

            // Flop cards (0-2) are active; turn (3) and river (4) are not
            assertThat(v.active()).containsExactly(true, true, true, false, false);
            // numWithCards=2 > 1 so bDrawnNormal=true; also bCardDealt=true (lastBetting >=
            // FLOP)
            assertThat(v.drawnNormal()).containsExactly(true, true, true, false, false);
            assertThat(v.drawn()).containsExactly(true, true, true, false, false);
        }

        @Test
        void should_SetDrawnNormal_When_MultiplePlayersHaveCards() {
            // numWithCards=3, lastBetting=PRE_FLOP (bCardDealt for flop = false)
            CommunityCardVisibility v = CommunityCardCalculator.calculateVisibility(FLOP, PRE_FLOP, 3, false);

            // bDrawnNormal = 3 > 1 = true; bCardDealt = (0 >= 1) = false
            // drawnNormal = true || false = true; drawn = false || true = true
            assertThat(v.drawnNormal()).containsExactly(true, true, true, false, false);
            assertThat(v.drawn()).containsExactly(true, true, true, false, false);
        }

        @Test
        void should_SetDrawnOnly_When_RabbitHuntEnabled() {
            // numWithCards=1 (only one player), rabbitHunt=true, lastBetting=PRE_FLOP
            CommunityCardVisibility v = CommunityCardCalculator.calculateVisibility(FLOP, PRE_FLOP, 1, true);

            // bDrawnNormal = 1 > 1 = false; bDrawn = true || false = true
            // bCardDealt = (0 >= 1) = false
            // drawnNormal = false || false = false; drawn = true || false = true
            assertThat(v.active()).containsExactly(true, true, true, false, false);
            assertThat(v.drawnNormal()).containsExactly(false, false, false, false, false);
            assertThat(v.drawn()).containsExactly(true, true, true, false, false);
        }
    }

    // =================================================================
    // Turn tests
    // =================================================================

    @Nested
    class TurnRound {

        @Test
        void should_DrawFourCards_When_TurnRound() {
            CommunityCardVisibility v = CommunityCardCalculator.calculateVisibility(TURN, TURN, 2, false);

            assertThat(v.active()).containsExactly(true, true, true, true, false);
            assertThat(v.drawnNormal()).containsExactly(true, true, true, true, false);
            assertThat(v.drawn()).containsExactly(true, true, true, true, false);
        }

        @Test
        void should_MarkFlopAsCardDealt_When_LastBettingReachedFlop() {
            // displayRound=TURN, lastBetting=FLOP, numWithCards=1, no rabbit hunt
            // bDrawnNormal = false; bDrawn = false
            CommunityCardVisibility v = CommunityCardCalculator.calculateVisibility(TURN, FLOP, 1, false);

            // Turn card: bCardDealt = (1 >= 2) = false; drawnNormal = false; drawn = false
            // Flop cards: bCardDealt = (1 >= 1) = true; drawnNormal = true; drawn = true
            assertThat(v.active()).containsExactly(true, true, true, true, false);
            assertThat(v.drawnNormal()[3]).isFalse(); // turn not dealt yet
            assertThat(v.drawn()[3]).isFalse();
            assertThat(v.drawnNormal()[0]).isTrue(); // flop dealt
            assertThat(v.drawnNormal()[1]).isTrue();
            assertThat(v.drawnNormal()[2]).isTrue();
            assertThat(v.drawn()[0]).isTrue();
        }
    }

    // =================================================================
    // River tests
    // =================================================================

    @Nested
    class RiverRound {

        @Test
        void should_DrawAllFiveCards_When_RiverRound() {
            CommunityCardVisibility v = CommunityCardCalculator.calculateVisibility(RIVER, RIVER, 2, false);

            assertThat(v.active()).containsExactly(true, true, true, true, true);
            assertThat(v.drawnNormal()).containsExactly(true, true, true, true, true);
            assertThat(v.drawn()).containsExactly(true, true, true, true, true);
        }

        @Test
        void should_DrawAllFiveCards_When_ShowdownRound() {
            CommunityCardVisibility v = CommunityCardCalculator.calculateVisibility(SHOWDOWN, RIVER, 2, false);

            assertThat(v.active()).containsExactly(true, true, true, true, true);
            assertThat(v.drawnNormal()).containsExactly(true, true, true, true, true);
            assertThat(v.drawn()).containsExactly(true, true, true, true, true);
        }
    }

    // =================================================================
    // Edge cases
    // =================================================================

    @Nested
    class EdgeCases {

        @Test
        void should_SetDrawnFromCardDealt_When_LastBettingRoundPassedStreet() {
            // displayRound=RIVER, lastBetting=RIVER, numWithCards=1, no rabbit
            // bDrawnNormal = false; bDrawn = false
            // But bCardDealt is true for each street because lastBetting >= each
            CommunityCardVisibility v = CommunityCardCalculator.calculateVisibility(RIVER, RIVER, 1, false);

            // river: bCardDealt = (3 >= 3) = true -> drawnNormal=true, drawn=true
            // turn: bCardDealt = (3 >= 2) = true -> drawnNormal=true, drawn=true
            // flop: bCardDealt = (3 >= 1) = true -> drawnNormal=true, drawn=true
            assertThat(v.drawnNormal()).containsExactly(true, true, true, true, true);
            assertThat(v.drawn()).containsExactly(true, true, true, true, true);
        }

        @Test
        void should_HaveBothFlags_When_RabbitHuntAndMultiplePlayers() {
            // numWithCards=3, rabbitHunt=true, displayRound=RIVER, lastBetting=PRE_FLOP
            CommunityCardVisibility v = CommunityCardCalculator.calculateVisibility(RIVER, PRE_FLOP, 3, true);

            // bDrawnNormal = true; bDrawn = true
            // All bCardDealt = false (lastBetting=0 < each street threshold)
            // drawnNormal = true || false = true; drawn = true || false = true
            assertThat(v.active()).containsExactly(true, true, true, true, true);
            assertThat(v.drawnNormal()).containsExactly(true, true, true, true, true);
            assertThat(v.drawn()).containsExactly(true, true, true, true, true);
        }
    }
}
