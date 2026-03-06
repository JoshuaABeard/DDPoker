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
package com.donohoedigital.games.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for BorderArrayList - deduplicated list of Border objects.
 */
class BorderArrayListTest {

    // ========== Constructor Tests ==========

    @Test
    void should_CreateEmptyList_When_Constructed() {
        BorderArrayList list = new BorderArrayList(5);

        assertThat(list).isEmpty();
    }

    // ========== addBorder Tests ==========

    @Test
    void should_AddBorder_When_NewBorderAdded() {
        BorderArrayList list = new BorderArrayList(5);
        Border border = BorderTestHelper.createBorder("A", "B");

        list.addBorder(border);

        assertThat(list).hasSize(1);
    }

    @Test
    void should_NotAddDuplicate_When_SameBorderAddedTwice() {
        BorderArrayList list = new BorderArrayList(5);
        Border border = BorderTestHelper.createBorder("A", "B");

        list.addBorder(border);
        list.addBorder(border);

        assertThat(list).hasSize(1);
    }

    @Test
    void should_AddMultipleBorders_When_DifferentBordersAdded() {
        BorderArrayList list = new BorderArrayList(5);
        Border b1 = BorderTestHelper.createBorder("A", "B");
        Border b2 = BorderTestHelper.createBorder("C", "D");

        list.addBorder(b1);
        list.addBorder(b2);

        assertThat(list).hasSize(2);
    }

    // ========== getBorder Tests ==========

    @Test
    void should_ReturnBorder_When_GetBorderCalledWithValidIndex() {
        BorderArrayList list = new BorderArrayList(5);
        Border border = BorderTestHelper.createBorder("X", "Y");
        list.addBorder(border);

        Border result = list.getBorder(0);

        assertThat(result).isSameAs(border);
    }

    @Test
    void should_ReturnCorrectBorder_When_MultipleBordersPresent() {
        BorderArrayList list = new BorderArrayList(5);
        Border b1 = BorderTestHelper.createBorder("A", "B");
        Border b2 = BorderTestHelper.createBorder("C", "D");
        list.addBorder(b1);
        list.addBorder(b2);

        assertThat(list.getBorder(0)).isSameAs(b1);
        assertThat(list.getBorder(1)).isSameAs(b2);
    }

    // ========== removeBorder Tests ==========

    @Test
    void should_RemoveBorder_When_RemoveBorderCalled() {
        BorderArrayList list = new BorderArrayList(5);
        Border border = BorderTestHelper.createBorder("A", "B");
        list.addBorder(border);

        list.removeBorder(border);

        assertThat(list).isEmpty();
    }

    @Test
    void should_LeaveOtherBorders_When_OneBorderRemoved() {
        BorderArrayList list = new BorderArrayList(5);
        Border b1 = BorderTestHelper.createBorder("A", "B");
        Border b2 = BorderTestHelper.createBorder("C", "D");
        list.addBorder(b1);
        list.addBorder(b2);

        list.removeBorder(b1);

        assertThat(list).hasSize(1);
        assertThat(list.getBorder(0)).isSameAs(b2);
    }

    // ========== toString Tests ==========

    @Test
    void should_ReturnEmptyString_When_ListEmpty() {
        BorderArrayList list = new BorderArrayList(5);

        assertThat(list.toString()).isEmpty();
    }

    @Test
    void should_ReturnShortDesc_When_SingleBorderPresent() {
        BorderArrayList list = new BorderArrayList(5);
        Border border = BorderTestHelper.createBorder("Alpha", "Beta");
        list.addBorder(border);

        String result = list.toString();

        assertThat(result).contains("Alpha");
        assertThat(result).contains("Beta");
    }

    @Test
    void should_SeparateWithCommas_When_MultipleBordersPresent() {
        BorderArrayList list = new BorderArrayList(5);
        list.addBorder(BorderTestHelper.createBorder("A", "B"));
        list.addBorder(BorderTestHelper.createBorder("C", "D"));

        String result = list.toString();

        assertThat(result).contains(", ");
    }
}
