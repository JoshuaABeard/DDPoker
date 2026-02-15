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
package com.donohoedigital.games.poker.core;

import com.donohoedigital.games.poker.core.state.ActionType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit tests for {@link PlayerAction}. */
class PlayerActionTest {

    // Factory method tests

    @Test
    void fold_shouldCreateFoldActionWithZeroAmount() {
        PlayerAction action = PlayerAction.fold();

        assertThat(action.actionType()).isEqualTo(ActionType.FOLD);
        assertThat(action.amount()).isEqualTo(0);
    }

    @Test
    void check_shouldCreateCheckActionWithZeroAmount() {
        PlayerAction action = PlayerAction.check();

        assertThat(action.actionType()).isEqualTo(ActionType.CHECK);
        assertThat(action.amount()).isEqualTo(0);
    }

    @Test
    void call_shouldCreateCallActionWithZeroAmount() {
        PlayerAction action = PlayerAction.call();

        assertThat(action.actionType()).isEqualTo(ActionType.CALL);
        assertThat(action.amount()).isEqualTo(0);
    }

    @Test
    void bet_shouldCreateBetActionWithSpecifiedAmount() {
        PlayerAction action = PlayerAction.bet(100);

        assertThat(action.actionType()).isEqualTo(ActionType.BET);
        assertThat(action.amount()).isEqualTo(100);
    }

    @Test
    void bet_shouldHandleMinimumBet() {
        PlayerAction action = PlayerAction.bet(1);

        assertThat(action.actionType()).isEqualTo(ActionType.BET);
        assertThat(action.amount()).isEqualTo(1);
    }

    @Test
    void bet_shouldHandleLargeBet() {
        PlayerAction action = PlayerAction.bet(1_000_000);

        assertThat(action.actionType()).isEqualTo(ActionType.BET);
        assertThat(action.amount()).isEqualTo(1_000_000);
    }

    @Test
    void raise_shouldCreateRaiseActionWithSpecifiedAmount() {
        PlayerAction action = PlayerAction.raise(200);

        assertThat(action.actionType()).isEqualTo(ActionType.RAISE);
        assertThat(action.amount()).isEqualTo(200);
    }

    @Test
    void raise_shouldHandleMinimumRaise() {
        PlayerAction action = PlayerAction.raise(1);

        assertThat(action.actionType()).isEqualTo(ActionType.RAISE);
        assertThat(action.amount()).isEqualTo(1);
    }

    @Test
    void raise_shouldHandleLargeRaise() {
        PlayerAction action = PlayerAction.raise(10_000_000);

        assertThat(action.actionType()).isEqualTo(ActionType.RAISE);
        assertThat(action.amount()).isEqualTo(10_000_000);
    }

    // Record behavior tests

    @Test
    void equals_shouldReturnTrueForSameActionAndAmount() {
        PlayerAction action1 = PlayerAction.bet(100);
        PlayerAction action2 = PlayerAction.bet(100);

        assertThat(action1).isEqualTo(action2);
    }

    @Test
    void equals_shouldReturnFalseForDifferentActionType() {
        PlayerAction bet = PlayerAction.bet(100);
        PlayerAction raise = PlayerAction.raise(100);

        assertThat(bet).isNotEqualTo(raise);
    }

    @Test
    void equals_shouldReturnFalseForDifferentAmount() {
        PlayerAction action1 = PlayerAction.bet(100);
        PlayerAction action2 = PlayerAction.bet(200);

        assertThat(action1).isNotEqualTo(action2);
    }

    @Test
    void hashCode_shouldBeConsistentForSameValues() {
        PlayerAction action1 = PlayerAction.raise(500);
        PlayerAction action2 = PlayerAction.raise(500);

        assertThat(action1.hashCode()).isEqualTo(action2.hashCode());
    }

    @Test
    void toString_shouldIncludeActionTypeAndAmount() {
        PlayerAction action = PlayerAction.bet(250);

        String result = action.toString();

        assertThat(result).contains("BET").contains("250");
    }

    @Test
    void constructor_shouldCreateActionWithSpecifiedValues() {
        PlayerAction action = new PlayerAction(ActionType.FOLD, 0);

        assertThat(action.actionType()).isEqualTo(ActionType.FOLD);
        assertThat(action.amount()).isEqualTo(0);
    }

    @Test
    void constructor_shouldAllowNonZeroAmountForAnyAction() {
        // While factory methods enforce conventions, the record constructor allows
        // flexibility
        PlayerAction action = new PlayerAction(ActionType.FOLD, 100);

        assertThat(action.actionType()).isEqualTo(ActionType.FOLD);
        assertThat(action.amount()).isEqualTo(100);
    }
}
