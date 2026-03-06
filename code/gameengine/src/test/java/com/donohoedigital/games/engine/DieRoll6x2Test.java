/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
 * Copyright (c) 2026 the DD Poker community
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
package com.donohoedigital.games.engine;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for DieRoll6x2 - a pair of 6-sided dice with marshal/demarshal support.
 */
class DieRoll6x2Test {

    // ========== Constructor Tests ==========

    @Test
    void should_ProduceFirstDieInRange_When_Constructed() {
        for (int i = 0; i < 50; i++) {
            DieRoll6x2 roll = new DieRoll6x2();
            assertThat(roll.getFirst()).isBetween(1, 6);
        }
    }

    @Test
    void should_ProduceSecondDieInRange_When_Constructed() {
        for (int i = 0; i < 50; i++) {
            DieRoll6x2 roll = new DieRoll6x2();
            assertThat(roll.getSecond()).isBetween(1, 6);
        }
    }

    // ========== getSum() Tests ==========

    @Test
    void should_ReturnSumOfBothDice_When_GetSumCalled() {
        for (int i = 0; i < 50; i++) {
            DieRoll6x2 roll = new DieRoll6x2();
            assertThat(roll.getSum()).isEqualTo(roll.getFirst() + roll.getSecond());
        }
    }

    @Test
    void should_ReturnSumInRange_When_GetSumCalled() {
        for (int i = 0; i < 50; i++) {
            DieRoll6x2 roll = new DieRoll6x2();
            assertThat(roll.getSum()).isBetween(2, 12);
        }
    }

    // ========== toString() Tests ==========

    @Test
    void should_FormatAsTuple_When_ToStringCalled() {
        DieRoll6x2 roll = new DieRoll6x2();

        String result = roll.toString();
        assertThat(result).matches("\\(\\d,\\d\\)");
    }

    @Test
    void should_ContainCorrectValues_When_ToStringCalled() {
        DieRoll6x2 roll = new DieRoll6x2();

        String expected = "(" + roll.getFirst() + "," + roll.getSecond() + ")";
        assertThat(roll.toString()).isEqualTo(expected);
    }

    // ========== demarshal() Tests ==========

    @Test
    void should_ParseFirstAndSecondDie_When_DemarshalCalled() {
        DieRoll6x2 roll = new DieRoll6x2();
        roll.demarshal(null, "35");

        assertThat(roll.getFirst()).isEqualTo(3);
        assertThat(roll.getSecond()).isEqualTo(5);
    }

    @Test
    void should_ComputeSum_When_DemarshalCalled() {
        DieRoll6x2 roll = new DieRoll6x2();
        roll.demarshal(null, "35");

        assertThat(roll.getSum()).isEqualTo(8);
    }

    @Test
    void should_ParseMinValues_When_DemarshalCalledWithOnes() {
        DieRoll6x2 roll = new DieRoll6x2();
        roll.demarshal(null, "11");

        assertThat(roll.getFirst()).isEqualTo(1);
        assertThat(roll.getSecond()).isEqualTo(1);
        assertThat(roll.getSum()).isEqualTo(2);
    }

    @Test
    void should_ParseMaxValues_When_DemarshalCalledWithSixes() {
        DieRoll6x2 roll = new DieRoll6x2();
        roll.demarshal(null, "66");

        assertThat(roll.getFirst()).isEqualTo(6);
        assertThat(roll.getSecond()).isEqualTo(6);
        assertThat(roll.getSum()).isEqualTo(12);
    }

    // ========== marshal() Tests ==========

    @Test
    void should_ProduceConcatenatedString_When_MarshalCalled() {
        DieRoll6x2 roll = new DieRoll6x2();
        roll.demarshal(null, "46");

        String marshalled = roll.marshal(null);
        assertThat(marshalled).isEqualTo("46");
    }

    // ========== Marshal/Demarshal Roundtrip Tests ==========

    @Test
    void should_PreserveValues_When_MarshalThenDemarshal() {
        DieRoll6x2 original = new DieRoll6x2();
        String marshalled = original.marshal(null);

        DieRoll6x2 restored = new DieRoll6x2();
        restored.demarshal(null, marshalled);

        assertThat(restored.getFirst()).isEqualTo(original.getFirst());
        assertThat(restored.getSecond()).isEqualTo(original.getSecond());
        assertThat(restored.getSum()).isEqualTo(original.getSum());
    }

    // ========== Multiple Construction Tests ==========

    @Test
    void should_ProduceValidRolls_When_ConstructedMultipleTimes() {
        for (int i = 0; i < 100; i++) {
            DieRoll6x2 roll = new DieRoll6x2();
            assertThat(roll.getFirst()).isBetween(1, 6);
            assertThat(roll.getSecond()).isBetween(1, 6);
            assertThat(roll.getSum()).isEqualTo(roll.getFirst() + roll.getSecond());
        }
    }
}
