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
package com.donohoedigital.comms;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for DMArrayList - a marshallable ArrayList supporting String, Integer,
 * Long, Double, Boolean, and DataMarshal contents.
 */
class DMArrayListTest {

    // ---- Constructors ----

    @Test
    void should_CreateEmptyList_When_DefaultConstructed() {
        DMArrayList<String> list = new DMArrayList<>();

        assertThat(list).isEmpty();
    }

    @Test
    void should_CreateEmptyList_When_ConstructedWithSize() {
        DMArrayList<String> list = new DMArrayList<>(10);

        assertThat(list).isEmpty();
    }

    @Test
    void should_CopyElements_When_CopyConstructed() {
        DMArrayList<String> original = new DMArrayList<>();
        original.add("a");
        original.add("b");

        DMArrayList<String> copy = new DMArrayList<>(original);

        assertThat(copy).containsExactly("a", "b");
    }

    @Test
    void should_BeIndependent_When_CopyConstructed() {
        DMArrayList<String> original = new DMArrayList<>();
        original.add("a");

        DMArrayList<String> copy = new DMArrayList<>(original);
        copy.add("b");

        assertThat(original).hasSize(1);
        assertThat(copy).hasSize(2);
    }

    // ---- ArrayList operations ----

    @Test
    void should_AddAndRetrieveElements_When_StandardOperationsUsed() {
        DMArrayList<Integer> list = new DMArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);

        assertThat(list).hasSize(3);
        assertThat(list.get(0)).isEqualTo(1);
        assertThat(list.get(1)).isEqualTo(2);
        assertThat(list.get(2)).isEqualTo(3);
    }

    // ---- Marshal/demarshal round-trip ----

    @Test
    void should_RoundTripIntegers_When_MarshalledAndDemarshalled() {
        MsgState state = new MsgState();
        DMArrayList<Integer> original = new DMArrayList<>();
        original.add(10);
        original.add(20);
        original.add(30);

        String marshalled = original.marshal(state);

        DMArrayList<Integer> restored = new DMArrayList<>();
        restored.demarshal(state, marshalled);

        assertThat(restored).containsExactly(10, 20, 30);
    }

    @Test
    void should_RoundTripStrings_When_MarshalledAndDemarshalled() {
        MsgState state = new MsgState();
        DMArrayList<String> original = new DMArrayList<>();
        original.add("hello");
        original.add("world");

        String marshalled = original.marshal(state);

        DMArrayList<String> restored = new DMArrayList<>();
        restored.demarshal(state, marshalled);

        assertThat(restored).containsExactly("hello", "world");
    }

    @Test
    void should_RoundTripBooleans_When_MarshalledAndDemarshalled() {
        MsgState state = new MsgState();
        DMArrayList<Boolean> original = new DMArrayList<>();
        original.add(true);
        original.add(false);

        String marshalled = original.marshal(state);

        DMArrayList<Boolean> restored = new DMArrayList<>();
        restored.demarshal(state, marshalled);

        assertThat(restored).containsExactly(true, false);
    }

    @Test
    void should_RoundTripDoubles_When_MarshalledAndDemarshalled() {
        MsgState state = new MsgState();
        DMArrayList<Double> original = new DMArrayList<>();
        original.add(1.5);
        original.add(2.5);

        String marshalled = original.marshal(state);

        DMArrayList<Double> restored = new DMArrayList<>();
        restored.demarshal(state, marshalled);

        assertThat(restored).containsExactly(1.5, 2.5);
    }

    @Test
    void should_RoundTripLongs_When_MarshalledAndDemarshalled() {
        MsgState state = new MsgState();
        DMArrayList<Long> original = new DMArrayList<>();
        original.add(100L);
        original.add(Long.MAX_VALUE);

        String marshalled = original.marshal(state);

        DMArrayList<Long> restored = new DMArrayList<>();
        restored.demarshal(state, marshalled);

        assertThat(restored).containsExactly(100L, Long.MAX_VALUE);
    }

    @Test
    void should_RoundTripEmptyList_When_MarshalledAndDemarshalled() {
        MsgState state = new MsgState();
        DMArrayList<String> original = new DMArrayList<>();

        String marshalled = original.marshal(state);

        DMArrayList<String> restored = new DMArrayList<>();
        restored.demarshal(state, marshalled);

        assertThat(restored).isEmpty();
    }

    // ---- toString ----

    @Test
    void should_FormatAsBracketedList_When_ToStringCalled() {
        DMArrayList<Integer> list = new DMArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);

        assertThat(list.toString()).isEqualTo("[1,2,3]");
    }

    @Test
    void should_ReturnEmptyBrackets_When_ToStringCalledOnEmptyList() {
        DMArrayList<String> list = new DMArrayList<>();

        assertThat(list.toString()).isEqualTo("[]");
    }
}
