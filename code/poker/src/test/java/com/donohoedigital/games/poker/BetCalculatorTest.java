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

class BetCalculatorTest {

    // =================================================================
    // determineInputMode tests
    // =================================================================

    @Nested
    class DetermineInputMode {

        @Test
        void should_ReturnCheckBet_When_NothingToCallAndNoBet() {
            assertThat(BetCalculator.determineInputMode(0, 0)).isEqualTo(PokerTableInput.MODE_CHECK_BET);
        }

        @Test
        void should_ReturnCheckRaise_When_NothingToCallButBetExists() {
            assertThat(BetCalculator.determineInputMode(0, 100)).isEqualTo(PokerTableInput.MODE_CHECK_RAISE);
        }

        @Test
        void should_ReturnCallRaise_When_ToCallIsPositive() {
            assertThat(BetCalculator.determineInputMode(50, 0)).isEqualTo(PokerTableInput.MODE_CALL_RAISE);
        }

        @Test
        void should_ReturnCallRaise_When_ToCallAndBetBothPositive() {
            assertThat(BetCalculator.determineInputMode(50, 100)).isEqualTo(PokerTableInput.MODE_CALL_RAISE);
        }
    }

    // =================================================================
    // roundToMinChip tests
    // =================================================================

    @Nested
    class RoundToMinChip {

        @Test
        void should_ReturnSameAmount_When_ExactMultiple() {
            assertThat(BetCalculator.roundToMinChip(100, 25)).isEqualTo(100);
        }

        @Test
        void should_RoundDown_When_RemainderLessThanHalf() {
            // 110 % 25 = 10, half of 25 = 12.5, 10 < 12.5 => round down to 100
            assertThat(BetCalculator.roundToMinChip(110, 25)).isEqualTo(100);
        }

        @Test
        void should_RoundUp_When_RemainderGreaterThanHalf() {
            // 115 % 25 = 15, half of 25 = 12.5, 15 >= 12.5 => round up to 125
            assertThat(BetCalculator.roundToMinChip(115, 25)).isEqualTo(125);
        }

        @Test
        void should_RoundUp_When_RemainderExactlyHalf() {
            // 50 % 20 = 10, half of 20 = 10.0, 10 >= 10.0 => round up to 60
            assertThat(BetCalculator.roundToMinChip(50, 20)).isEqualTo(60);
        }

        @Test
        void should_RoundDown_When_RemainderJustBelowHalf() {
            // 29 % 20 = 9, half of 20 = 10.0, 9 < 10.0 => round down to 20
            assertThat(BetCalculator.roundToMinChip(29, 20)).isEqualTo(20);
        }

        @Test
        void should_ReturnSameAmount_When_MinChipIsOne() {
            assertThat(BetCalculator.roundToMinChip(137, 1)).isEqualTo(137);
        }

        @Test
        void should_HandleLargeValues() {
            // 10030 % 50 = 30, half of 50 = 25.0, 30 >= 25.0 => round up to 10050
            assertThat(BetCalculator.roundToMinChip(10030, 50)).isEqualTo(10050);
        }
    }

    // =================================================================
    // determineBetOrRaise tests
    // =================================================================

    @Nested
    class DetermineBetOrRaise {

        @Test
        void should_ReturnBet_When_ModeIsCheckBet() {
            assertThat(BetCalculator.determineBetOrRaise(PokerTableInput.MODE_CHECK_BET))
                    .isEqualTo(HandAction.ACTION_BET);
        }

        @Test
        void should_ReturnRaise_When_ModeIsCheckRaise() {
            assertThat(BetCalculator.determineBetOrRaise(PokerTableInput.MODE_CHECK_RAISE))
                    .isEqualTo(HandAction.ACTION_RAISE);
        }

        @Test
        void should_ReturnRaise_When_ModeIsCallRaise() {
            assertThat(BetCalculator.determineBetOrRaise(PokerTableInput.MODE_CALL_RAISE))
                    .isEqualTo(HandAction.ACTION_RAISE);
        }
    }

    // =================================================================
    // determineCheckOrCall tests
    // =================================================================

    @Nested
    class DetermineCheckOrCall {

        @Test
        void should_ReturnCheck_When_ModeIsCheckBet() {
            assertThat(BetCalculator.determineCheckOrCall(PokerTableInput.MODE_CHECK_BET))
                    .isEqualTo(HandAction.ACTION_CHECK);
        }

        @Test
        void should_ReturnCheck_When_ModeIsCheckRaise() {
            assertThat(BetCalculator.determineCheckOrCall(PokerTableInput.MODE_CHECK_RAISE))
                    .isEqualTo(HandAction.ACTION_CHECK);
        }

        @Test
        void should_ReturnCall_When_ModeIsCallRaise() {
            assertThat(BetCalculator.determineCheckOrCall(PokerTableInput.MODE_CALL_RAISE))
                    .isEqualTo(HandAction.ACTION_CALL);
        }
    }
}
