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
package com.donohoedigital.games.poker.core.ai;

import com.donohoedigital.games.poker.engine.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class PocketMatrixIntTest {

    // ========== Constructor Tests ==========

    @Test
    void should_InitializeAllToZero_When_DefaultConstructor() {
        PocketMatrixInt matrix = new PocketMatrixInt();

        assertThat(matrix.get(0, 1)).isZero();
        assertThat(matrix.get(10, 20)).isZero();
        assertThat(matrix.get(50, 51)).isZero();
    }

    @Test
    void should_InitializeAllToValue_When_ValueConstructor() {
        PocketMatrixInt matrix = new PocketMatrixInt(42);

        assertThat(matrix.get(0, 1)).isEqualTo(42);
        assertThat(matrix.get(10, 20)).isEqualTo(42);
        assertThat(matrix.get(50, 51)).isEqualTo(42);
    }

    // ========== Int Index Tests ==========

    @Test
    void should_StoreAndRetrieve_When_IntIndices() {
        PocketMatrixInt matrix = new PocketMatrixInt();

        matrix.set(3, 7, 100_000);
        assertThat(matrix.get(3, 7)).isEqualTo(100_000);
    }

    @Test
    void should_ReturnSameValue_When_IndicesReversed() {
        PocketMatrixInt matrix = new PocketMatrixInt();

        matrix.set(5, 10, 77_777);
        assertThat(matrix.get(10, 5)).isEqualTo(77_777);
    }

    @Test
    void should_StoreDistinctValues_When_DifferentPairs() {
        PocketMatrixInt matrix = new PocketMatrixInt();

        matrix.set(0, 1, 10);
        matrix.set(2, 3, 20);

        assertThat(matrix.get(0, 1)).isEqualTo(10);
        assertThat(matrix.get(2, 3)).isEqualTo(20);
    }

    @Test
    void should_HandleBoundaryIndices_When_LowestPair() {
        PocketMatrixInt matrix = new PocketMatrixInt();

        matrix.set(0, 1, Integer.MAX_VALUE);
        assertThat(matrix.get(0, 1)).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void should_HandleBoundaryIndices_When_HighestPair() {
        PocketMatrixInt matrix = new PocketMatrixInt();

        matrix.set(50, 51, Integer.MIN_VALUE);
        assertThat(matrix.get(50, 51)).isEqualTo(Integer.MIN_VALUE);
    }

    // ========== Card Index Tests ==========

    @Test
    void should_StoreAndRetrieve_When_CardObjects() {
        PocketMatrixInt matrix = new PocketMatrixInt();
        Card aceSpades = Card.getCard(CardSuit.SPADES, Card.ACE);
        Card kingHearts = Card.getCard(CardSuit.HEARTS, Card.KING);

        matrix.set(aceSpades, kingHearts, 55_555);
        assertThat(matrix.get(aceSpades, kingHearts)).isEqualTo(55_555);
    }

    @Test
    void should_ReturnSameValue_When_CardOrderReversed() {
        PocketMatrixInt matrix = new PocketMatrixInt();
        Card card1 = Card.getCard(CardSuit.CLUBS, Card.TWO);
        Card card2 = Card.getCard(CardSuit.DIAMONDS, Card.THREE);

        matrix.set(card1, card2, 333);
        assertThat(matrix.get(card2, card1)).isEqualTo(333);
    }

    // ========== Hand Index Tests ==========

    @Test
    void should_StoreAndRetrieve_When_HandObject() {
        PocketMatrixInt matrix = new PocketMatrixInt();
        Card card1 = Card.getCard(CardSuit.HEARTS, Card.QUEEN);
        Card card2 = Card.getCard(CardSuit.SPADES, Card.JACK);
        Hand hand = new Hand(card1, card2);

        matrix.set(hand, 88_888);
        assertThat(matrix.get(hand)).isEqualTo(88_888);
    }

    @Test
    void should_AgreeWithCardAccess_When_HandUsed() {
        PocketMatrixInt matrix = new PocketMatrixInt();
        Card card1 = Card.getCard(CardSuit.DIAMONDS, Card.TEN);
        Card card2 = Card.getCard(CardSuit.CLUBS, Card.NINE);
        Hand hand = new Hand(card1, card2);

        matrix.set(hand, 444);
        assertThat(matrix.get(card1, card2)).isEqualTo(444);
    }

    // ========== Clear Tests ==========

    @Test
    void should_ResetAllEntries_When_ClearCalled() {
        PocketMatrixInt matrix = new PocketMatrixInt();

        matrix.set(0, 1, 10);
        matrix.set(25, 30, 20);
        matrix.set(50, 51, 30);

        matrix.clear(7);

        assertThat(matrix.get(0, 1)).isEqualTo(7);
        assertThat(matrix.get(25, 30)).isEqualTo(7);
        assertThat(matrix.get(50, 51)).isEqualTo(7);
    }
}
