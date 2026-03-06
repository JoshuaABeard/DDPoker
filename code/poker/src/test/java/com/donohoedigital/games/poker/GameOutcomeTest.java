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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class GameOutcomeTest {

    @Nested
    class Determine {
        @Test
        void should_ReturnWin_When_PlaceIsFirst() {
            assertThat(GameOutcome.determine(1, 500, false)).isEqualTo(GameOutcome.WIN);
        }

        @Test
        void should_ReturnMoney_When_PrizePositiveButNotFirst() {
            assertThat(GameOutcome.determine(3, 100, false)).isEqualTo(GameOutcome.MONEY);
        }

        @Test
        void should_ReturnObserver_When_NoPrizeAndGameOver() {
            assertThat(GameOutcome.determine(5, 0, true)).isEqualTo(GameOutcome.OBSERVER);
        }

        @Test
        void should_ReturnBusted_When_NoPrizeAndGameNotOver() {
            assertThat(GameOutcome.determine(5, 0, false)).isEqualTo(GameOutcome.BUSTED);
        }

        @Test
        void should_ReturnWin_When_FirstPlaceEvenWithZeroPrize() {
            assertThat(GameOutcome.determine(1, 0, false)).isEqualTo(GameOutcome.WIN);
        }

        @Test
        void should_ReturnMoney_When_SecondPlaceWithPrize() {
            assertThat(GameOutcome.determine(2, 250, true)).isEqualTo(GameOutcome.MONEY);
        }
    }
}
