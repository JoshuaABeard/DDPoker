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

/** Tests for {@link TableState} enum. */
class TableStateTest {

    @Test
    void fromLegacy_shouldConvertAllStates() {
        assertThat(TableState.fromLegacy(0)).isEqualTo(TableState.NONE);
        assertThat(TableState.fromLegacy(1)).isEqualTo(TableState.PENDING);
        assertThat(TableState.fromLegacy(2)).isEqualTo(TableState.DEAL_FOR_BUTTON);
        assertThat(TableState.fromLegacy(3)).isEqualTo(TableState.BEGIN);
        assertThat(TableState.fromLegacy(4)).isEqualTo(TableState.BEGIN_WAIT);
        assertThat(TableState.fromLegacy(5)).isEqualTo(TableState.CHECK_END_HAND);
        assertThat(TableState.fromLegacy(6)).isEqualTo(TableState.CLEAN);
        assertThat(TableState.fromLegacy(7)).isEqualTo(TableState.NEW_LEVEL_CHECK);
        assertThat(TableState.fromLegacy(8)).isEqualTo(TableState.COLOR_UP);
        assertThat(TableState.fromLegacy(9)).isEqualTo(TableState.START_HAND);
        assertThat(TableState.fromLegacy(10)).isEqualTo(TableState.BETTING);
        assertThat(TableState.fromLegacy(11)).isEqualTo(TableState.COMMUNITY);
        assertThat(TableState.fromLegacy(12)).isEqualTo(TableState.SHOWDOWN);
        assertThat(TableState.fromLegacy(13)).isEqualTo(TableState.DONE);
        assertThat(TableState.fromLegacy(14)).isEqualTo(TableState.GAME_OVER);
        assertThat(TableState.fromLegacy(15)).isEqualTo(TableState.PENDING_LOAD);
        assertThat(TableState.fromLegacy(16)).isEqualTo(TableState.ON_HOLD);
        assertThat(TableState.fromLegacy(17)).isEqualTo(TableState.BREAK);
        assertThat(TableState.fromLegacy(18)).isEqualTo(TableState.PRE_SHOWDOWN);
    }

    @Test
    void fromLegacy_shouldThrowOnInvalidState() {
        assertThatThrownBy(() -> TableState.fromLegacy(999)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown table state: 999");
    }

    @Test
    void toLegacy_shouldConvertAllStates() {
        assertThat(TableState.NONE.toLegacy()).isEqualTo(0);
        assertThat(TableState.PENDING.toLegacy()).isEqualTo(1);
        assertThat(TableState.DEAL_FOR_BUTTON.toLegacy()).isEqualTo(2);
        assertThat(TableState.BEGIN.toLegacy()).isEqualTo(3);
        assertThat(TableState.BEGIN_WAIT.toLegacy()).isEqualTo(4);
        assertThat(TableState.CHECK_END_HAND.toLegacy()).isEqualTo(5);
        assertThat(TableState.CLEAN.toLegacy()).isEqualTo(6);
        assertThat(TableState.NEW_LEVEL_CHECK.toLegacy()).isEqualTo(7);
        assertThat(TableState.COLOR_UP.toLegacy()).isEqualTo(8);
        assertThat(TableState.START_HAND.toLegacy()).isEqualTo(9);
        assertThat(TableState.BETTING.toLegacy()).isEqualTo(10);
        assertThat(TableState.COMMUNITY.toLegacy()).isEqualTo(11);
        assertThat(TableState.SHOWDOWN.toLegacy()).isEqualTo(12);
        assertThat(TableState.DONE.toLegacy()).isEqualTo(13);
        assertThat(TableState.GAME_OVER.toLegacy()).isEqualTo(14);
        assertThat(TableState.PENDING_LOAD.toLegacy()).isEqualTo(15);
        assertThat(TableState.ON_HOLD.toLegacy()).isEqualTo(16);
        assertThat(TableState.BREAK.toLegacy()).isEqualTo(17);
        assertThat(TableState.PRE_SHOWDOWN.toLegacy()).isEqualTo(18);
    }

    @Test
    void roundTrip_shouldBeIdempotent() {
        for (TableState state : TableState.values()) {
            int legacy = state.toLegacy();
            TableState roundTrip = TableState.fromLegacy(legacy);
            assertThat(roundTrip).isEqualTo(state);
        }
    }
}
