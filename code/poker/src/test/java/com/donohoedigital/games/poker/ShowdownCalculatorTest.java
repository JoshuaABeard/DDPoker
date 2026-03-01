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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ShowdownCalculatorTest {

    // =================================================================
    // shouldShowCards tests
    // =================================================================

    @Nested
    class ShouldShowCards {

        @Test
        void should_ShowCards_When_CardsExposed() {
            assertThat(ShowdownCalculator.shouldShowCards(true, false, false, false, false, false, false, false, false,
                    false)).isTrue();
        }

        @Test
        void should_ShowCards_When_MuckedAndLost() {
            // showMucked=true, won=false, uncontested=false
            assertThat(ShowdownCalculator.shouldShowCards(false, false, true, false, false, false, false, false, false,
                    false)).isTrue();
        }

        @Test
        void should_NotShowCards_When_MuckedButWon() {
            // showMucked=true but won=true, so the muck clause is blocked
            assertThat(ShowdownCalculator.shouldShowCards(false, false, true, true, false, false, false, false, false,
                    false)).isFalse();
        }

        @Test
        void should_ShowCards_When_WinningAndShowWinEnabled() {
            assertThat(ShowdownCalculator.shouldShowCards(false, false, false, true, true, false, false, false, false,
                    false)).isTrue();
        }

        @Test
        void should_ShowCards_When_HumanWithCardsUp() {
            assertThat(ShowdownCalculator.shouldShowCards(false, false, false, false, false, true, true, true, false,
                    false)).isTrue();
        }

        @Test
        void should_NotShowCards_When_HumanButNotLocallyControlled() {
            assertThat(ShowdownCalculator.shouldShowCards(false, false, false, false, false, true, false, true, false,
                    false)).isFalse();
        }

        @Test
        void should_ShowCards_When_ComputerWithAiFaceUp() {
            assertThat(ShowdownCalculator.shouldShowCards(false, false, false, false, false, false, false, false, true,
                    true)).isTrue();
        }

        @Test
        void should_NotShowCards_When_AllFlagsOff() {
            assertThat(ShowdownCalculator.shouldShowCards(false, false, false, false, false, false, false, false, false,
                    false)).isFalse();
        }

        @Test
        void should_NotShowCards_When_UncontestedAndMuckedEnabled() {
            // uncontested=true blocks the muck display clause
            assertThat(ShowdownCalculator.shouldShowCards(false, true, true, false, false, false, false, false, false,
                    false)).isFalse();
        }
    }

    // =================================================================
    // shouldShowHandType tests
    // =================================================================

    @Nested
    class ShouldShowHandType {

        @Test
        void should_ShowHandType_When_NotUncontested() {
            // base = !uncontested => true
            assertThat(ShowdownCalculator.shouldShowHandType(false, false, false, false, false, false)).isTrue();
        }

        @Test
        void should_ShowHandType_When_UncontestedWithRiverAndShowWin() {
            // base = uncontested but (showRiver && showWinning) => true
            assertThat(ShowdownCalculator.shouldShowHandType(true, true, false, true, false, false)).isTrue();
        }

        @Test
        void should_ShowHandType_When_UncontestedWithSeenRiverAndShowWin() {
            // base = uncontested but (seenRiver && showWinning) => true
            assertThat(ShowdownCalculator.shouldShowHandType(true, false, true, true, false, false)).isTrue();
        }

        @Test
        void should_NotShowHandType_When_UncontestedNoRiverNoShowWin() {
            // base = uncontested, no river, no showWin => false; no human override either
            assertThat(ShowdownCalculator.shouldShowHandType(true, false, false, false, false, false)).isFalse();
        }

        @Test
        void should_ShowHandType_When_HumanAndShowRiver() {
            // isHuman && showRiver => always true regardless of other flags
            assertThat(ShowdownCalculator.shouldShowHandType(true, true, false, false, true, false)).isTrue();
        }

        @Test
        void should_ShowHandType_When_UncontestedPlayerShowWinningAndSeenRiver() {
            // uncontested && playerShowWinning && seenRiver => true
            assertThat(ShowdownCalculator.shouldShowHandType(true, false, true, false, false, true)).isTrue();
        }

        @Test
        void should_ShowHandType_When_UncontestedPlayerShowWinningAndShowRiver() {
            // uncontested && playerShowWinning && showRiver => true
            assertThat(ShowdownCalculator.shouldShowHandType(true, true, false, false, false, true)).isTrue();
        }

        @Test
        void should_NotShowHandType_When_UncontestedPlayerShowWinningButNoRiver() {
            // uncontested && playerShowWinning but neither showRiver nor seenRiver
            assertThat(ShowdownCalculator.shouldShowHandType(true, false, false, false, false, true)).isFalse();
        }
    }

    // =================================================================
    // shouldShowHandTypeFold tests
    // =================================================================

    @Nested
    class ShouldShowHandTypeFold {

        @Test
        void should_ShowHandTypeFold_When_NotUncontested() {
            assertThat(ShowdownCalculator.shouldShowHandTypeFold(false, false, false)).isTrue();
        }

        @Test
        void should_ShowHandTypeFold_When_UncontestedAndShowRiver() {
            assertThat(ShowdownCalculator.shouldShowHandTypeFold(true, true, false)).isTrue();
        }

        @Test
        void should_ShowHandTypeFold_When_UncontestedAndSeenRiver() {
            assertThat(ShowdownCalculator.shouldShowHandTypeFold(true, false, true)).isTrue();
        }

        @Test
        void should_NotShowHandTypeFold_When_UncontestedNoRiver() {
            assertThat(ShowdownCalculator.shouldShowHandTypeFold(true, false, false)).isFalse();
        }
    }

    // =================================================================
    // determineResultType tests
    // =================================================================

    @Nested
    class DetermineResultType {

        @Test
        void should_ReturnLose_When_TotalWonIsZero() {
            assertThat(ShowdownCalculator.determineResultType(0, 0)).isEqualTo(ResultsPiece.LOSE);
        }

        @Test
        void should_ReturnOverbet_When_OverbetEqualsTotal() {
            assertThat(ShowdownCalculator.determineResultType(50, 50)).isEqualTo(ResultsPiece.OVERBET);
        }

        @Test
        void should_ReturnWin_When_TotalPositive() {
            assertThat(ShowdownCalculator.determineResultType(100, 0)).isEqualTo(ResultsPiece.WIN);
        }

        @Test
        void should_ReturnWin_When_OverbetLessThanTotal() {
            assertThat(ShowdownCalculator.determineResultType(150, 50)).isEqualTo(ResultsPiece.WIN);
        }
    }
}
