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
package com.donohoedigital.games.poker.core.ai;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BetRangeTest {

    // ========== Factory methods ==========

    @Test
    void should_CreateAllInRange_When_AllInFactory() {
        BetRange range = BetRange.allIn();
        assertThat(range.getType()).isEqualTo(BetRange.ALL_IN);
        assertThat(range.getMin()).isEqualTo(0.0f);
        assertThat(range.getMax()).isEqualTo(0.0f);
    }

    @Test
    void should_CreatePotRelativeRange_When_PotRelativeFactory() {
        BetRange range = BetRange.potRelative(0.5f, 1.0f);
        assertThat(range.getType()).isEqualTo(BetRange.POT_SIZE);
        assertThat(range.getMin()).isEqualTo(0.5f);
        assertThat(range.getMax()).isEqualTo(1.0f);
    }

    @Test
    void should_CreateBigBlindRelativeRange_When_BigBlindFactory() {
        BetRange range = BetRange.bigBlindRelative(2.0f, 4.0f);
        assertThat(range.getType()).isEqualTo(BetRange.BIG_BLIND);
        assertThat(range.getMin()).isEqualTo(2.0f);
        assertThat(range.getMax()).isEqualTo(4.0f);
    }

    @Test
    void should_CreateStackRelativeRange_When_StackRelativeFactory() {
        BetRange range = BetRange.stackRelative(1000, 0.25f, 0.75f);
        assertThat(range.getType()).isEqualTo(BetRange.STACK_SIZE);
        assertThat(range.getMin()).isEqualTo(0.25f);
        assertThat(range.getMax()).isEqualTo(0.75f);
        assertThat(range.getStackChipCount()).isEqualTo(1000);
    }

    // ========== Constructor - single arg (ALL_IN) ==========

    @Test
    void should_CreateAllIn_When_SingleArgConstructor() {
        BetRange range = new BetRange(BetRange.ALL_IN);
        assertThat(range.getType()).isEqualTo(BetRange.ALL_IN);
    }

    @Test
    void should_ThrowException_When_SingleArgNotAllIn() {
        assertThatIllegalArgumentException().isThrownBy(() -> new BetRange(BetRange.POT_SIZE));
    }

    // ========== Constructor - two-arg type (POT_SIZE or BIG_BLIND) ==========

    @Test
    void should_CreatePotSize_When_TwoArgConstructor() {
        BetRange range = new BetRange(BetRange.POT_SIZE, 0.5f, 1.0f);
        assertThat(range.getType()).isEqualTo(BetRange.POT_SIZE);
    }

    @Test
    void should_CreateBigBlind_When_TwoArgConstructor() {
        BetRange range = new BetRange(BetRange.BIG_BLIND, 2.0f, 4.0f);
        assertThat(range.getType()).isEqualTo(BetRange.BIG_BLIND);
    }

    @Test
    void should_ThrowException_When_TwoArgWithStackSize() {
        assertThatIllegalArgumentException().isThrownBy(() -> new BetRange(BetRange.STACK_SIZE, 0.5f, 1.0f));
    }

    // ========== Validation ==========

    @Test
    void should_ThrowException_When_MinGreaterThanMax() {
        assertThatIllegalArgumentException().isThrownBy(() -> BetRange.potRelative(1.0f, 0.5f))
                .withMessageContaining("min greater than max");
    }

    @Test
    void should_ThrowException_When_PotSizeMinMaxBothZero() {
        assertThatIllegalArgumentException().isThrownBy(() -> BetRange.potRelative(0.0f, 0.0f))
                .withMessageContaining("both zero");
    }

    @Test
    void should_ThrowException_When_BigBlindMinMaxBothZero() {
        assertThatIllegalArgumentException().isThrownBy(() -> BetRange.bigBlindRelative(0.0f, 0.0f))
                .withMessageContaining("both zero");
    }

    @Test
    void should_ThrowException_When_StackRelativeWithZeroStack() {
        assertThatIllegalArgumentException().isThrownBy(() -> BetRange.stackRelative(0, 0.25f, 0.75f))
                .withMessageContaining("no stack size");
    }

    @Test
    void should_ThrowException_When_StackRelativeWithNegativeStack() {
        assertThatIllegalArgumentException().isThrownBy(() -> BetRange.stackRelative(-1, 0.25f, 0.75f))
                .withMessageContaining("no stack size");
    }

    // ========== getMinBet (v=0.0) ==========

    @Test
    void should_ReturnChipCount_When_AllInMinBet() {
        BetRange range = BetRange.allIn();
        int result = range.getMinBet(1000, 500, 100, 50, 100, 10);
        assertThat(result).isEqualTo(1000);
    }

    @Test
    void should_ReturnPotBasedMin_When_PotRelativeMinBet() {
        // potRelative(0.5, 1.0), v=0.0 -> betSize = (potSize + toCall) * min
        // betSize = (500 + 50) * 0.5 = 275
        BetRange range = BetRange.potRelative(0.5f, 1.0f);
        int result = range.getMinBet(1000, 500, 100, 50, 100, 10);
        assertThat(result).isEqualTo(280); // 275 rounded up to minChip=10
    }

    @Test
    void should_ReturnBigBlindBasedMin_When_BigBlindRelativeMinBet() {
        // bigBlindRelative(2.0, 4.0), v=0.0 -> betSize = bigBlind * min = 100*2 = 200
        BetRange range = BetRange.bigBlindRelative(2.0f, 4.0f);
        int result = range.getMinBet(1000, 500, 100, 50, 100, 10);
        assertThat(result).isEqualTo(200);
    }

    @Test
    void should_ReturnStackBasedMin_When_StackRelativeMinBet() {
        // stackRelative(800, 0.25, 0.75), v=0.0 -> betSize = (800 - toCall) * 0.25 =
        // 750 * 0.25 = 187
        // rounded up to 190 (minChip=10)
        BetRange range = BetRange.stackRelative(800, 0.25f, 0.75f);
        int result = range.getMinBet(1000, 500, 100, 50, 100, 10);
        assertThat(result).isEqualTo(190);
    }

    // ========== getMaxBet (v=1.0) ==========

    @Test
    void should_ReturnChipCount_When_AllInMaxBet() {
        BetRange range = BetRange.allIn();
        int result = range.getMaxBet(1000, 500, 100, 50, 100, 10);
        assertThat(result).isEqualTo(1000);
    }

    @Test
    void should_ReturnPotBasedMax_When_PotRelativeMaxBet() {
        // potRelative(0.5, 1.0), v=1.0 -> betSize = (500 + 50) * 1.0 = 550
        BetRange range = BetRange.potRelative(0.5f, 1.0f);
        int result = range.getMaxBet(1000, 500, 100, 50, 100, 10);
        assertThat(result).isEqualTo(550);
    }

    @Test
    void should_ReturnBigBlindBasedMax_When_BigBlindRelativeMaxBet() {
        // bigBlindRelative(2.0, 4.0), v=1.0 -> betSize = 100 * 4.0 = 400
        BetRange range = BetRange.bigBlindRelative(2.0f, 4.0f);
        int result = range.getMaxBet(1000, 500, 100, 50, 100, 10);
        assertThat(result).isEqualTo(400);
    }

    // ========== chooseBetAmount (random) ==========

    @Test
    void should_ReturnValueBetweenMinAndMax_When_ChoosingBetAmount() {
        BetRange range = BetRange.potRelative(0.5f, 1.0f);
        int min = range.getMinBet(1000, 500, 100, 50, 100, 10);
        int max = range.getMaxBet(1000, 500, 100, 50, 100, 10);
        int chosen = range.chooseBetAmount(1000, 500, 100, 50, 100, 10);
        assertThat(chosen).isBetween(min, max);
    }

    // ========== Bet clamping ==========

    @Test
    void should_ClampToChipCountMinusToCall_When_BetExceedsChips() {
        // potRelative(1.0, 1.0), v=0.0 -> betSize = (500 + 50) * 1.0 = 550
        // chipCount - toCall = 300 - 50 = 250, clamp to 250
        BetRange range = BetRange.potRelative(1.0f, 1.0f);
        int result = range.getMinBet(300, 500, 100, 50, 100, 10);
        assertThat(result).isEqualTo(250);
    }

    @Test
    void should_EnforceMinRaise_When_BetBelowMinRaise() {
        // bigBlindRelative(0.5, 0.5), v=0.0 -> betSize = 100 * 0.5 = 50
        // minRaise=100, betSize < minRaise but min_ > 0 -> betSize = minRaise = 100
        BetRange range = BetRange.bigBlindRelative(0.5f, 0.5f);
        int result = range.getMinBet(1000, 500, 100, 50, 100, 10);
        assertThat(result).isEqualTo(100);
    }

    @Test
    void should_ReturnZero_When_BetBelowMinRaiseAndMinIsZero() {
        // potRelative(0.0, 0.5), v=0.0 -> betSize = (500+50) * 0.0 = 0
        // betSize < minRaise AND min_ == 0 -> betSize = 0
        BetRange range = BetRange.potRelative(0.0f, 0.5f);
        int result = range.getMinBet(1000, 500, 100, 50, 100, 10);
        assertThat(result).isEqualTo(0);
    }

    @Test
    void should_RoundUpToMinChip_When_OddChips() {
        // bigBlindRelative(1.5, 1.5), v=0.0 -> betSize = 100 * 1.5 = 150
        // minChip=25, 150 % 25 = 0 -> no rounding needed -> 150
        BetRange range = BetRange.bigBlindRelative(1.5f, 1.5f);
        int result = range.getMinBet(1000, 500, 100, 50, 100, 25);
        assertThat(result).isEqualTo(150);
    }

    @Test
    void should_RoundUp_When_BetNotMultipleOfMinChip() {
        // bigBlindRelative(1.3, 1.3), v=0.0 -> betSize = 100 * 1.3 = 130
        // minChip=25, 130 % 25 = 5, betSize += 25-5 = 150
        BetRange range = BetRange.bigBlindRelative(1.3f, 1.3f);
        int result = range.getMinBet(1000, 500, 100, 50, 100, 25);
        assertThat(result).isEqualTo(150);
    }

    // ========== toDescription ==========

    @Test
    void should_ReturnAllInDescription_When_AllIn() {
        assertThat(BetRange.allIn().toDescription()).isEqualTo("All In");
    }

    @Test
    void should_ReturnPotRangeDescription_When_PotRelativeRange() {
        BetRange range = BetRange.potRelative(0.5f, 1.0f);
        assertThat(range.toDescription()).isEqualTo("50%-100% pot");
    }

    @Test
    void should_ReturnPotSingleDescription_When_PotRelativeEqual() {
        BetRange range = BetRange.potRelative(0.75f, 0.75f);
        assertThat(range.toDescription()).isEqualTo("75% pot");
    }

    @Test
    void should_ReturnStackRangeDescription_When_StackRelativeRange() {
        BetRange range = BetRange.stackRelative(1000, 0.25f, 0.50f);
        assertThat(range.toDescription()).isEqualTo("25%-50% stack");
    }

    @Test
    void should_ReturnStackSingleDescription_When_StackRelativeEqual() {
        BetRange range = BetRange.stackRelative(1000, 0.50f, 0.50f);
        assertThat(range.toDescription()).isEqualTo("50% stack");
    }

    @Test
    void should_ReturnBBRangeDescription_When_BigBlindRelativeRange() {
        BetRange range = BetRange.bigBlindRelative(2.0f, 4.0f);
        assertThat(range.toDescription()).isEqualTo("2.0-4.0x BB");
    }

    @Test
    void should_ReturnBBSingleDescription_When_BigBlindRelativeEqual() {
        BetRange range = BetRange.bigBlindRelative(3.0f, 3.0f);
        assertThat(range.toDescription()).isEqualTo("3.0x BB");
    }

    // ========== Type constants ==========

    @Test
    void should_HaveDistinctTypeConstants() {
        assertThat(BetRange.POT_SIZE).isEqualTo(1);
        assertThat(BetRange.STACK_SIZE).isEqualTo(2);
        assertThat(BetRange.BIG_BLIND).isEqualTo(3);
        assertThat(BetRange.ALL_IN).isEqualTo(4);
    }
}
