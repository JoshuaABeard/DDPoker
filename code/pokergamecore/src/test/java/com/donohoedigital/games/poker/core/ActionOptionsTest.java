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
package com.donohoedigital.games.poker.core;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Tests for {@link ActionOptions} record. */
class ActionOptionsTest {

    private static final ActionOptions CHECK_BET = new ActionOptions(true, false, true, false, true, 0, 50, 1000, 0, 0,
            30);

    private static final ActionOptions CALL_RAISE = new ActionOptions(false, true, false, true, true, 100, 0, 0, 200,
            2000, 30);

    @Test
    void should_StoreAllFields_When_Constructed() {
        assertThat(CHECK_BET.canCheck()).isTrue();
        assertThat(CHECK_BET.canCall()).isFalse();
        assertThat(CHECK_BET.canBet()).isTrue();
        assertThat(CHECK_BET.canRaise()).isFalse();
        assertThat(CHECK_BET.canFold()).isTrue();
        assertThat(CHECK_BET.callAmount()).isZero();
        assertThat(CHECK_BET.minBet()).isEqualTo(50);
        assertThat(CHECK_BET.maxBet()).isEqualTo(1000);
        assertThat(CHECK_BET.minRaise()).isZero();
        assertThat(CHECK_BET.maxRaise()).isZero();
        assertThat(CHECK_BET.timeoutSeconds()).isEqualTo(30);
    }

    @Test
    void should_StoreCallRaiseFields_When_Constructed() {
        assertThat(CALL_RAISE.canCheck()).isFalse();
        assertThat(CALL_RAISE.canCall()).isTrue();
        assertThat(CALL_RAISE.canBet()).isFalse();
        assertThat(CALL_RAISE.canRaise()).isTrue();
        assertThat(CALL_RAISE.canFold()).isTrue();
        assertThat(CALL_RAISE.callAmount()).isEqualTo(100);
        assertThat(CALL_RAISE.minBet()).isZero();
        assertThat(CALL_RAISE.maxBet()).isZero();
        assertThat(CALL_RAISE.minRaise()).isEqualTo(200);
        assertThat(CALL_RAISE.maxRaise()).isEqualTo(2000);
        assertThat(CALL_RAISE.timeoutSeconds()).isEqualTo(30);
    }

    @Test
    void should_BeEqual_When_SameFieldValues() {
        ActionOptions a = new ActionOptions(true, false, true, false, true, 0, 50, 1000, 0, 0, 30);
        ActionOptions b = new ActionOptions(true, false, true, false, true, 0, 50, 1000, 0, 0, 30);
        assertThat(a).isEqualTo(b);
    }

    @Test
    void should_NotBeEqual_When_DifferentBooleanField() {
        ActionOptions a = new ActionOptions(true, false, true, false, true, 0, 50, 1000, 0, 0, 30);
        ActionOptions b = new ActionOptions(false, false, true, false, true, 0, 50, 1000, 0, 0, 30);
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void should_NotBeEqual_When_DifferentIntField() {
        ActionOptions a = new ActionOptions(true, false, true, false, true, 0, 50, 1000, 0, 0, 30);
        ActionOptions b = new ActionOptions(true, false, true, false, true, 0, 50, 999, 0, 0, 30);
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void should_HaveSameHashCode_When_Equal() {
        ActionOptions a = new ActionOptions(true, true, true, true, true, 100, 50, 1000, 200, 2000, 60);
        ActionOptions b = new ActionOptions(true, true, true, true, true, 100, 50, 1000, 200, 2000, 60);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void should_HaveDifferentHashCode_When_NotEqual() {
        ActionOptions a = new ActionOptions(true, false, true, false, true, 0, 50, 1000, 0, 0, 30);
        ActionOptions b = new ActionOptions(false, true, false, true, false, 100, 200, 5000, 400, 10000, 60);
        assertThat(a.hashCode()).isNotEqualTo(b.hashCode());
    }

    @Test
    void should_IncludeFieldValues_When_ToString() {
        String str = CHECK_BET.toString();
        assertThat(str).contains("canCheck=true");
        assertThat(str).contains("minBet=50");
        assertThat(str).contains("maxBet=1000");
        assertThat(str).contains("timeoutSeconds=30");
    }

    @Test
    void should_NotBeEqual_When_ComparedToNull() {
        assertThat(CHECK_BET).isNotEqualTo(null);
    }

    @Test
    void should_HandleZeroTimeout_When_NoTimeLimit() {
        ActionOptions noTimeout = new ActionOptions(true, false, false, false, false, 0, 0, 0, 0, 0, 0);
        assertThat(noTimeout.timeoutSeconds()).isZero();
    }
}
