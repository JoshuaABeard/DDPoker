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
package com.donohoedigital.games.engine;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for DiceRoller - Static dice rolling utility using MersenneTwisterFast.
 */
class DiceRollerTest {

    @AfterEach
    void resetSeed() {
        DiceRoller.newSeed();
    }

    // ========== rollDie6int Tests ==========

    @Test
    void should_ReturnValueBetween1And6_When_RollDie6int() {
        for (int i = 0; i < 100; i++) {
            int result = DiceRoller.rollDie6int();
            assertThat(result).isBetween(1, 6);
        }
    }

    // ========== rollDieInt Tests ==========

    @Test
    void should_ReturnValueBetweenOneAndSides_When_RollDieIntCalledWithSides() {
        for (int sides = 2; sides <= 20; sides++) {
            for (int i = 0; i < 50; i++) {
                int result = DiceRoller.rollDieInt(sides);
                assertThat(result).isBetween(1, sides);
            }
        }
    }

    // ========== rollDie6 Tests ==========

    @Test
    void should_ReturnIntegerBetween1And6_When_RollDie6() {
        for (int i = 0; i < 100; i++) {
            Integer result = DiceRoller.rollDie6();
            assertThat(result).isBetween(1, 6);
        }
    }

    // ========== rollDie Tests ==========

    @Test
    void should_ReturnIntegerBetweenOneAndSides_When_RollDie() {
        for (int i = 0; i < 100; i++) {
            Integer result = DiceRoller.rollDie(10);
            assertThat(result).isBetween(1, 10);
        }
    }

    // ========== rollDice6 Tests ==========

    @Test
    void should_ReturnArrayOfCorrectSize_When_RollDice6() {
        Integer[] results = DiceRoller.rollDice6(5);

        assertThat(results).hasSize(5);
    }

    @Test
    void should_ReturnAllValuesInRange_When_RollDice6() {
        Integer[] results = DiceRoller.rollDice6(100);

        assertThat(results).allSatisfy(value -> assertThat(value).isBetween(1, 6));
    }

    // ========== rollDice Tests ==========

    @Test
    void should_ReturnArrayOfCorrectSize_When_RollDice() {
        Integer[] results = DiceRoller.rollDice(12, 7);

        assertThat(results).hasSize(7);
    }

    @Test
    void should_ReturnAllValuesInRange_When_RollDice() {
        Integer[] results = DiceRoller.rollDice(20, 100);

        assertThat(results).allSatisfy(value -> assertThat(value).isBetween(1, 20));
    }

    // ========== Seed Control Tests ==========

    @Test
    void should_ProduceDeterministicResults_When_SetSeedCalledWithSameValue() {
        long seed = 12345L;

        DiceRoller.setSeed(seed);
        int first = DiceRoller.rollDie6int();

        DiceRoller.setSeed(seed);
        int second = DiceRoller.rollDie6int();

        assertThat(first).isEqualTo(second);
    }

    @Test
    void should_ProduceDeterministicResults_When_RollDieIntCalledWithSeed() {
        long seed = 99999L;

        int first = DiceRoller.rollDieInt(6, seed);
        int second = DiceRoller.rollDieInt(6, seed);

        assertThat(first).isEqualTo(second);
    }

    @Test
    void should_ProduceDeterministicSequence_When_SeedSet() {
        long seed = 42L;

        DiceRoller.setSeed(seed);
        int[] firstSequence = new int[10];
        for (int i = 0; i < 10; i++) {
            firstSequence[i] = DiceRoller.rollDie6int();
        }

        DiceRoller.setSeed(seed);
        int[] secondSequence = new int[10];
        for (int i = 0; i < 10; i++) {
            secondSequence[i] = DiceRoller.rollDie6int();
        }

        assertThat(firstSequence).isEqualTo(secondSequence);
    }

    // ========== newSeed Tests ==========

    @Test
    void should_NotThrow_When_NewSeedCalled() {
        assertThatCode(DiceRoller::newSeed).doesNotThrowAnyException();
    }
}
