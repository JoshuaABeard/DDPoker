/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
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
package com.donohoedigital.games.poker.core.state;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Tests for {@link BettingRound} enum. */
class BettingRoundTest {

    @Test
    void fromLegacy_shouldConvertAllRounds() {
        assertThat(BettingRound.fromLegacy(-1)).isEqualTo(BettingRound.NONE);
        assertThat(BettingRound.fromLegacy(0)).isEqualTo(BettingRound.PRE_FLOP);
        assertThat(BettingRound.fromLegacy(1)).isEqualTo(BettingRound.FLOP);
        assertThat(BettingRound.fromLegacy(2)).isEqualTo(BettingRound.TURN);
        assertThat(BettingRound.fromLegacy(3)).isEqualTo(BettingRound.RIVER);
        assertThat(BettingRound.fromLegacy(4)).isEqualTo(BettingRound.SHOWDOWN);
    }

    @Test
    void fromLegacy_shouldThrowOnInvalidRound() {
        assertThatThrownBy(() -> BettingRound.fromLegacy(999)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown betting round: 999");
    }

    @Test
    void toLegacy_shouldConvertAllRounds() {
        assertThat(BettingRound.NONE.toLegacy()).isEqualTo(-1);
        assertThat(BettingRound.PRE_FLOP.toLegacy()).isEqualTo(0);
        assertThat(BettingRound.FLOP.toLegacy()).isEqualTo(1);
        assertThat(BettingRound.TURN.toLegacy()).isEqualTo(2);
        assertThat(BettingRound.RIVER.toLegacy()).isEqualTo(3);
        assertThat(BettingRound.SHOWDOWN.toLegacy()).isEqualTo(4);
    }

    @Test
    void roundTrip_shouldBeIdempotent() {
        for (BettingRound round : BettingRound.values()) {
            int legacy = round.toLegacy();
            BettingRound roundTrip = BettingRound.fromLegacy(legacy);
            assertThat(roundTrip).isEqualTo(round);
        }
    }
}
