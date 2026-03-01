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
package com.donohoedigital.games.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for BorderPoint — coordinate storage, anchor logic, and border
 * membership.
 */
class BorderPointTest {

    // ========== Coordinate Tests ==========

    @Test
    void should_StoreCoordinates_When_ConstructedWithXY() {
        BorderPoint point = new BorderPoint(10, 20);

        assertThat(point.getX()).isEqualTo(10);
        assertThat(point.getY()).isEqualTo(20);
    }

    @Test
    void should_StoreNegativeCoordinates_When_ConstructedWithNegativeXY() {
        BorderPoint point = new BorderPoint(-5, -15);

        assertThat(point.getX()).isEqualTo(-5);
        assertThat(point.getY()).isEqualTo(-15);
    }

    @Test
    void should_StoreZeroCoordinates_When_ConstructedWithZeroXY() {
        BorderPoint point = new BorderPoint(0, 0);

        assertThat(point.getX()).isEqualTo(0);
        assertThat(point.getY()).isEqualTo(0);
    }

    // ========== Equality Tests ==========

    @Test
    void should_BeEqual_When_SameCoordinates() {
        BorderPoint p1 = new BorderPoint(3, 7);
        BorderPoint p2 = new BorderPoint(3, 7);

        assertThat(p1).isEqualTo(p2);
    }

    @Test
    void should_NotBeEqual_When_DifferentCoordinates() {
        BorderPoint p1 = new BorderPoint(3, 7);
        BorderPoint p2 = new BorderPoint(3, 8);

        assertThat(p1).isNotEqualTo(p2);
    }

    // ========== Anchor Logic Tests ==========

    @Test
    void should_NotBeAnchor_When_NewlyCreated() {
        BorderPoint point = new BorderPoint(1, 2);

        assertThat(point.isAnchor()).isFalse();
    }

    @Test
    void should_NotBeAnchor_When_BelongsToOneBorderOnly() {
        BorderPoint point = new BorderPoint(5, 10);
        // addBorder only adds if not already present, so a single direct add leaves
        // size == 1
        point.addBorder(BorderTestHelper.createBorder("A", "B"));

        assertThat(point.isAnchor()).isFalse();
    }

    @Test
    void should_BeAnchor_When_BelongsToTwoBorders() {
        BorderPoint point = new BorderPoint(5, 10);
        point.addBorder(BorderTestHelper.createBorder("A", "B"));
        point.addBorder(BorderTestHelper.createBorder("A", "C"));

        assertThat(point.isAnchor()).isTrue();
    }

    // ========== Border Membership Tests ==========

    @Test
    void should_HaveNoBorders_When_NewlyCreated() {
        BorderPoint point = new BorderPoint(1, 2);

        assertThat(point.getBorders()).isEmpty();
    }

    @Test
    void should_TrackBorder_When_AddBorderCalled() {
        BorderPoint point = new BorderPoint(1, 2);
        Border border = BorderTestHelper.createBorder("X", "Y");
        point.addBorder(border);

        assertThat(point.getBorders()).hasSize(1);
        assertThat(point.getBorders().getBorder(0)).isSameAs(border);
    }

    @Test
    void should_NotDuplicateBorder_When_SameBorderAddedTwice() {
        BorderPoint point = new BorderPoint(1, 2);
        Border border = BorderTestHelper.createBorder("X", "Y");
        point.addBorder(border);
        point.addBorder(border);

        assertThat(point.getBorders()).hasSize(1);
    }

    // ========== toString / longDesc Tests ==========

    @Test
    void should_IncludeCoordinatesInToString() {
        BorderPoint point = new BorderPoint(42, 99);

        assertThat(point.toString()).contains("42").contains("99");
    }
}
