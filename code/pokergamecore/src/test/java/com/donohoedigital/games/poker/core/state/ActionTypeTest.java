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

/** Tests for {@link ActionType} enum. */
class ActionTypeTest {

    @Test
    void fromLegacy_shouldConvertAllActions() {
        assertThat(ActionType.fromLegacy(-1)).isEqualTo(ActionType.NONE);
        assertThat(ActionType.fromLegacy(0)).isEqualTo(ActionType.FOLD);
        assertThat(ActionType.fromLegacy(1)).isEqualTo(ActionType.CHECK);
        assertThat(ActionType.fromLegacy(2)).isEqualTo(ActionType.CHECK_RAISE);
        assertThat(ActionType.fromLegacy(3)).isEqualTo(ActionType.CALL);
        assertThat(ActionType.fromLegacy(4)).isEqualTo(ActionType.BET);
        assertThat(ActionType.fromLegacy(5)).isEqualTo(ActionType.RAISE);
        assertThat(ActionType.fromLegacy(6)).isEqualTo(ActionType.BLIND_BIG);
        assertThat(ActionType.fromLegacy(7)).isEqualTo(ActionType.BLIND_SM);
        assertThat(ActionType.fromLegacy(8)).isEqualTo(ActionType.ANTE);
        assertThat(ActionType.fromLegacy(9)).isEqualTo(ActionType.WIN);
        assertThat(ActionType.fromLegacy(10)).isEqualTo(ActionType.OVERBET);
        assertThat(ActionType.fromLegacy(11)).isEqualTo(ActionType.LOSE);
    }

    @Test
    void fromLegacy_shouldThrowOnInvalidAction() {
        assertThatThrownBy(() -> ActionType.fromLegacy(999)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown action type: 999");
    }

    @Test
    void toLegacy_shouldConvertAllActions() {
        assertThat(ActionType.NONE.toLegacy()).isEqualTo(-1);
        assertThat(ActionType.FOLD.toLegacy()).isEqualTo(0);
        assertThat(ActionType.CHECK.toLegacy()).isEqualTo(1);
        assertThat(ActionType.CHECK_RAISE.toLegacy()).isEqualTo(2);
        assertThat(ActionType.CALL.toLegacy()).isEqualTo(3);
        assertThat(ActionType.BET.toLegacy()).isEqualTo(4);
        assertThat(ActionType.RAISE.toLegacy()).isEqualTo(5);
        assertThat(ActionType.BLIND_BIG.toLegacy()).isEqualTo(6);
        assertThat(ActionType.BLIND_SM.toLegacy()).isEqualTo(7);
        assertThat(ActionType.ANTE.toLegacy()).isEqualTo(8);
        assertThat(ActionType.WIN.toLegacy()).isEqualTo(9);
        assertThat(ActionType.OVERBET.toLegacy()).isEqualTo(10);
        assertThat(ActionType.LOSE.toLegacy()).isEqualTo(11);
    }

    @Test
    void roundTrip_shouldBeIdempotent() {
        for (ActionType action : ActionType.values()) {
            int legacy = action.toLegacy();
            ActionType roundTrip = ActionType.fromLegacy(legacy);
            assertThat(roundTrip).isEqualTo(action);
        }
    }
}
