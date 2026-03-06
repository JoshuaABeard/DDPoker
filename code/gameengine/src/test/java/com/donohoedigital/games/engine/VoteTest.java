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
 * Tests for Vote - a numeric vote value with optional debug tracking.
 */
class VoteTest {

    // ========== Constructor Tests ==========

    @Test
    void should_HaveZeroValue_When_ConstructedWithStringOnly() {
        Vote vote = new Vote("init");

        assertThat(vote.get()).isEqualTo(0.0f);
    }

    @Test
    void should_HaveSpecifiedValue_When_ConstructedWithFloatAndString() {
        Vote vote = new Vote(5.5f, "start");

        assertThat(vote.get()).isEqualTo(5.5f);
    }

    @Test
    void should_CopyValueAndDebug_When_ConstructedFromAnotherVote() {
        Vote original = new Vote(3.0f, "orig");
        Vote copy = new Vote(original);

        assertThat(copy.get()).isEqualTo(3.0f);
        assertThat(copy.toString()).isEqualTo(original.toString());
    }

    @Test
    void should_EnableDebugTracking_When_DebugStringIsNonNull() {
        Vote vote = new Vote("debug");

        assertThat(vote.sbDebug).isNotNull();
    }

    @Test
    void should_DisableDebugTracking_When_DebugStringIsNull() {
        Vote vote = new Vote(1.0f, null);

        assertThat(vote.sbDebug).isNull();
    }

    // ========== get() Tests ==========

    @Test
    void should_ReturnCurrentValue_When_GetCalled() {
        Vote vote = new Vote(7.25f, "test");

        assertThat(vote.get()).isEqualTo(7.25f);
    }

    // ========== set() Tests ==========

    @Test
    void should_UpdateValue_When_SetCalled() {
        Vote vote = new Vote("init");
        vote.set(10.0f, "reset");

        assertThat(vote.get()).isEqualTo(10.0f);
    }

    @Test
    void should_ClearAndReplaceDebug_When_SetCalled() {
        Vote vote = new Vote(1.0f, "first");
        vote.add(2.0f, "added");
        vote.set(5.0f, "reset");

        String debug = vote.toString();
        assertThat(debug).contains("reset");
        assertThat(debug).doesNotContain("first");
        assertThat(debug).doesNotContain("added");
    }

    // ========== add(float, String) Tests ==========

    @Test
    void should_AccumulateValue_When_AddFloatCalled() {
        Vote vote = new Vote(2.0f, "init");
        vote.add(3.0f, "bonus");

        assertThat(vote.get()).isEqualTo(5.0f);
    }

    @Test
    void should_AppendDebugInfo_When_AddFloatCalled() {
        Vote vote = new Vote(1.0f, "init");
        vote.add(2.0f, "bonus");

        String debug = vote.toString();
        assertThat(debug).contains("init");
        assertThat(debug).contains("bonus");
    }

    // ========== add(Vote) Tests ==========

    @Test
    void should_AddOtherVoteValue_When_AddVoteCalled() {
        Vote vote1 = new Vote(4.0f, "base");
        Vote vote2 = new Vote(6.0f, "other");

        vote1.add(vote2);

        assertThat(vote1.get()).isEqualTo(10.0f);
    }

    @Test
    void should_AppendAddDebugInfo_When_AddVoteCalled() {
        Vote vote1 = new Vote(1.0f, "base");
        Vote vote2 = new Vote(2.0f, "other");

        vote1.add(vote2);

        String debug = vote1.toString();
        assertThat(debug).contains("ADD(");
        assertThat(debug).contains("other");
    }

    // ========== Multiple Operations Tests ==========

    @Test
    void should_AccumulateCorrectly_When_MultipleAddsPerformed() {
        Vote vote = new Vote(0.0f, "start");
        vote.add(1.5f, "a");
        vote.add(2.5f, "b");
        vote.add(3.0f, "c");

        assertThat(vote.get()).isEqualTo(7.0f);
    }

    // ========== toString() Tests ==========

    @Test
    void should_FormatCorrectly_When_ToStringCalled() {
        Vote vote = new Vote(3.14f, "pi");

        String result = vote.toString();
        assertThat(result).contains("=");
        assertThat(result).contains("pi");
        // Format is "X.XX = debug..."
        assertThat(result).matches(".*\\d+\\.\\d+.*=.*pi.*");
    }

    @Test
    void should_ContainFormattedValues_When_DebugOutputGenerated() {
        Vote vote = new Vote(0.0f, "init");
        vote.add(1.5f, "add1");

        String result = vote.toString();
        // Should contain formatted float values in parentheses
        assertThat(result).contains("init(");
        assertThat(result).contains("add1(");
        assertThat(result).contains(")");
    }

    // ========== Null Debug Tests ==========

    @Test
    void should_NotTrackDebug_When_ConstructedWithNullDebug() {
        Vote vote = new Vote(5.0f, null);
        vote.add(3.0f, "ignored");

        assertThat(vote.get()).isEqualTo(8.0f);
        assertThat(vote.sbDebug).isNull();
    }

    @Test
    void should_NotTrackDebug_When_AddVoteCalledWithNullDebug() {
        Vote vote1 = new Vote(1.0f, null);
        Vote vote2 = new Vote(2.0f, "other");

        vote1.add(vote2);

        assertThat(vote1.get()).isEqualTo(3.0f);
        assertThat(vote1.sbDebug).isNull();
    }
}
