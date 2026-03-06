/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2026 Joshua Beard and contributors
 *
 * This file is part of DD Poker, originally created by Doug Donohoe.
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
package com.donohoedigital.games.poker;

import static org.assertj.core.api.Assertions.*;

import com.donohoedigital.games.poker.CommunityCardCalculator.CommunityCardVisibility;
import com.donohoedigital.games.poker.display.ClientBettingRound;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CommunityCardCalculatorTest {

    // Round constants from BettingRound
    private static final int PRE_FLOP = ClientBettingRound.ROUND_PRE_FLOP; // 0
    private static final int FLOP = ClientBettingRound.ROUND_FLOP; // 1
    private static final int TURN = ClientBettingRound.ROUND_TURN; // 2
    private static final int RIVER = ClientBettingRound.ROUND_RIVER; // 3
    private static final int SHOWDOWN = ClientBettingRound.ROUND_SHOWDOWN; // 4

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
    // CommunityCardVisibility record validation
    // =================================================================

    @Nested
    class VisibilityRecordValidation {

        @Test
        void should_ThrowIllegalArgument_When_ActiveArrayTooShort() {
            assertThatThrownBy(() -> new CommunityCardVisibility(new boolean[4], new boolean[5], new boolean[5]))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("5");
        }

        @Test
        void should_ThrowIllegalArgument_When_DrawnNormalArrayTooShort() {
            assertThatThrownBy(() -> new CommunityCardVisibility(new boolean[5], new boolean[4], new boolean[5]))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("5");
        }

        @Test
        void should_ThrowIllegalArgument_When_DrawnArrayTooShort() {
            assertThatThrownBy(() -> new CommunityCardVisibility(new boolean[5], new boolean[5], new boolean[4]))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("5");
        }

        @Test
        void should_AcceptExactly5Elements_When_AllArraysCorrectSize() {
            CommunityCardVisibility v = new CommunityCardVisibility(new boolean[5], new boolean[5], new boolean[5]);
            assertThat(v.active()).hasSize(5);
            assertThat(v.drawnNormal()).hasSize(5);
            assertThat(v.drawn()).hasSize(5);
        }
    }

    // =================================================================
    // Edge cases
    // =================================================================

    @Nested
    class ShowdownRound {

        @Test
        void should_DrawAllCards_When_ShowdownAndSinglePlayerAndCardDealt() {
            // displayRound=SHOWDOWN, numWithCards=1 (bDrawnNormal=false), lastBetting=RIVER
            CommunityCardVisibility v = CommunityCardCalculator.calculateVisibility(SHOWDOWN, RIVER, 1, false);

            // bCardDealt is true for each card since lastBetting >= each street
            assertThat(v.active()).containsExactly(true, true, true, true, true);
            assertThat(v.drawnNormal()).containsExactly(true, true, true, true, true);
            assertThat(v.drawn()).containsExactly(true, true, true, true, true);
        }

        @Test
        void should_HaveFlopCardsOnly_When_ShowdownWithOnlyFlopDealt() {
            // displayRound=SHOWDOWN, lastBetting=FLOP, numWithCards=1, no rabbit
            CommunityCardVisibility v = CommunityCardCalculator.calculateVisibility(SHOWDOWN, FLOP, 1, false);

            // bDrawnNormal=false, bDrawn=false
            // river: bCardDealt = (1 >= 3) = false; drawnNormal=false, drawn=false
            // turn: bCardDealt = (1 >= 2) = false; drawnNormal=false, drawn=false
            // flop: bCardDealt = (1 >= 1) = true; drawnNormal=true, drawn=true
            assertThat(v.active()).containsExactly(true, true, true, true, true);
            assertThat(v.drawnNormal()).containsExactly(true, true, true, false, false);
            assertThat(v.drawn()).containsExactly(true, true, true, false, false);
        }
    }

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
