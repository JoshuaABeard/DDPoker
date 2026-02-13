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
package com.donohoedigital.games.poker.logic;

import com.donohoedigital.config.ApplicationType;
import com.donohoedigital.config.ConfigManager;
import com.donohoedigital.games.poker.PokerGame;
import com.donohoedigital.games.poker.PokerTable;
import com.donohoedigital.games.poker.model.TournamentProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for PokerLogicUtils - pure logic utility methods with no UI
 * dependencies. Tests run in headless mode. Part of Wave 1 testability
 * refactoring.
 */
@Tag("unit")
class PokerLogicUtilsTest {

    private PokerGame game;
    private PokerTable table;

    @BeforeEach
    void setUp() {
        // Initialize ConfigManager for headless testing
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);

        // Create minimal game and table for roundAmountMinChip tests
        game = new PokerGame(null);
        TournamentProfile profile = new TournamentProfile("test");
        game.setProfile(profile);
        table = new PokerTable(game, 1);
    }

    // =================================================================
    // pow() Tests
    // =================================================================

    @Test
    void should_ReturnOne_When_AnyNumberToZeroPower() {
        assertThat(PokerLogicUtils.pow(5, 0)).isEqualTo(1);
        assertThat(PokerLogicUtils.pow(100, 0)).isEqualTo(1);
        assertThat(PokerLogicUtils.pow(1, 0)).isEqualTo(1);
    }

    @Test
    void should_ReturnSameNumber_When_PowerIsOne() {
        assertThat(PokerLogicUtils.pow(5, 1)).isEqualTo(5);
        assertThat(PokerLogicUtils.pow(42, 1)).isEqualTo(42);
        assertThat(PokerLogicUtils.pow(1, 1)).isEqualTo(1);
    }

    @Test
    void should_CalculatePower_When_SmallExponent() {
        assertThat(PokerLogicUtils.pow(2, 3)).isEqualTo(8);
        assertThat(PokerLogicUtils.pow(5, 2)).isEqualTo(25);
        assertThat(PokerLogicUtils.pow(3, 3)).isEqualTo(27);
    }

    @Test
    void should_CalculatePower_When_LargerExponent() {
        assertThat(PokerLogicUtils.pow(2, 10)).isEqualTo(1024);
        assertThat(PokerLogicUtils.pow(10, 3)).isEqualTo(1000);
    }

    @Test
    void should_HandleNegativeBase_When_PowerIsEven() {
        assertThat(PokerLogicUtils.pow(-2, 2)).isEqualTo(4);
        assertThat(PokerLogicUtils.pow(-3, 4)).isEqualTo(81);
    }

    @Test
    void should_HandleNegativeBase_When_PowerIsOdd() {
        assertThat(PokerLogicUtils.pow(-2, 3)).isEqualTo(-8);
        assertThat(PokerLogicUtils.pow(-3, 3)).isEqualTo(-27);
    }

    // =================================================================
    // nChooseK() Tests
    // =================================================================

    @Test
    void should_ReturnOne_When_ChoosingZeroItems() {
        assertThat(PokerLogicUtils.nChooseK(10, 0)).isEqualTo(1);
        assertThat(PokerLogicUtils.nChooseK(52, 0)).isEqualTo(1);
    }

    @Test
    void should_ReturnOne_When_ChoosingAllItems() {
        assertThat(PokerLogicUtils.nChooseK(10, 10)).isEqualTo(1);
        assertThat(PokerLogicUtils.nChooseK(5, 5)).isEqualTo(1);
    }

    @Test
    void should_ReturnN_When_ChoosingOneItem() {
        assertThat(PokerLogicUtils.nChooseK(10, 1)).isEqualTo(10);
        assertThat(PokerLogicUtils.nChooseK(52, 1)).isEqualTo(52);
    }

    @Test
    void should_CalculateSmallCombinations() {
        assertThat(PokerLogicUtils.nChooseK(5, 2)).isEqualTo(10); // 5!/(2!*3!)
        assertThat(PokerLogicUtils.nChooseK(6, 3)).isEqualTo(20); // 6!/(3!*3!)
        assertThat(PokerLogicUtils.nChooseK(4, 2)).isEqualTo(6); // 4!/(2!*2!)
    }

    @Test
    void should_CalculatePokerRelevantCombinations() {
        // 52 choose 2 - number of possible pocket card combinations
        assertThat(PokerLogicUtils.nChooseK(52, 2)).isEqualTo(1326);

        // 50 choose 3 - flop combinations after 2 pocket cards
        assertThat(PokerLogicUtils.nChooseK(50, 3)).isEqualTo(19600);

        // 13 choose 2 - pairs in same suit
        assertThat(PokerLogicUtils.nChooseK(13, 2)).isEqualTo(78);
    }

    // =================================================================
    // roundAmountMinChip() Tests
    // =================================================================

    @Test
    void should_ReturnSameAmount_When_AlreadyMultipleOfMinChip() {
        table.setMinChip(5);

        assertThat(PokerLogicUtils.roundAmountMinChip(table, 100)).isEqualTo(100);
        assertThat(PokerLogicUtils.roundAmountMinChip(table, 50)).isEqualTo(50);
        assertThat(PokerLogicUtils.roundAmountMinChip(table, 5)).isEqualTo(5);
    }

    @Test
    void should_RoundDown_When_BelowHalfway() {
        table.setMinChip(10);

        // 123 % 10 = 3, which is < 5, so round down to 120
        assertThat(PokerLogicUtils.roundAmountMinChip(table, 123)).isEqualTo(120);

        // 201 % 10 = 1, which is < 5, so round down to 200
        assertThat(PokerLogicUtils.roundAmountMinChip(table, 201)).isEqualTo(200);
    }

    @Test
    void should_RoundUp_When_AtOrAboveHalfway() {
        table.setMinChip(10);

        // 125 % 10 = 5, which is >= 5, so round up to 130
        assertThat(PokerLogicUtils.roundAmountMinChip(table, 125)).isEqualTo(130);

        // 129 % 10 = 9, which is >= 5, so round up to 130
        assertThat(PokerLogicUtils.roundAmountMinChip(table, 129)).isEqualTo(130);

        // 206 % 10 = 6, which is >= 5, so round up to 210
        assertThat(PokerLogicUtils.roundAmountMinChip(table, 206)).isEqualTo(210);
    }

    @Test
    void should_RoundCorrectly_When_MinChipIsOne() {
        table.setMinChip(1);

        // Everything should stay the same when min chip is 1
        assertThat(PokerLogicUtils.roundAmountMinChip(table, 123)).isEqualTo(123);
        assertThat(PokerLogicUtils.roundAmountMinChip(table, 1)).isEqualTo(1);
        assertThat(PokerLogicUtils.roundAmountMinChip(table, 9999)).isEqualTo(9999);
    }

    @Test
    void should_RoundCorrectly_When_MinChipIsTwentyFive() {
        table.setMinChip(25);

        // 100 is exact multiple
        assertThat(PokerLogicUtils.roundAmountMinChip(table, 100)).isEqualTo(100);

        // 110 % 25 = 10, which is < 12.5, so round down to 100
        assertThat(PokerLogicUtils.roundAmountMinChip(table, 110)).isEqualTo(100);

        // 113 % 25 = 13, which is >= 12.5, so round up to 125
        assertThat(PokerLogicUtils.roundAmountMinChip(table, 113)).isEqualTo(125);

        // 250 is exact multiple
        assertThat(PokerLogicUtils.roundAmountMinChip(table, 250)).isEqualTo(250);
    }

    @Test
    void should_RoundCorrectly_When_MinChipIsHundred() {
        table.setMinChip(100);

        // 500 is exact multiple
        assertThat(PokerLogicUtils.roundAmountMinChip(table, 500)).isEqualTo(500);

        // 540 % 100 = 40, which is < 50, so round down to 500
        assertThat(PokerLogicUtils.roundAmountMinChip(table, 540)).isEqualTo(500);

        // 550 % 100 = 50, which is >= 50, so round up to 600
        assertThat(PokerLogicUtils.roundAmountMinChip(table, 550)).isEqualTo(600);

        // 599 % 100 = 99, which is >= 50, so round up to 600
        assertThat(PokerLogicUtils.roundAmountMinChip(table, 599)).isEqualTo(600);
    }

    @Test
    void should_HandleZeroAmount() {
        table.setMinChip(5);

        assertThat(PokerLogicUtils.roundAmountMinChip(table, 0)).isEqualTo(0);
    }

    @Test
    void should_HandleSmallAmounts() {
        table.setMinChip(5);

        // 1 % 5 = 1, which is < 2.5, so round down to 0
        assertThat(PokerLogicUtils.roundAmountMinChip(table, 1)).isEqualTo(0);

        // 3 % 5 = 3, which is >= 2.5, so round up to 5
        assertThat(PokerLogicUtils.roundAmountMinChip(table, 3)).isEqualTo(5);

        // 2 % 5 = 2, which is < 2.5, so round down to 0
        assertThat(PokerLogicUtils.roundAmountMinChip(table, 2)).isEqualTo(0);
    }
}
