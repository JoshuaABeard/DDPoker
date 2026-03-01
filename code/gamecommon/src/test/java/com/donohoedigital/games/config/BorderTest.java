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
 * Tests for Border — point collection management, flags, number, and equality.
 */
class BorderTest {

    // ========== Construction / Flags Tests ==========

    @Test
    void should_SetEnclosedFalse_When_ConstructedWithEnclosedFalse() {
        Border border = BorderTestHelper.createBorder("Alpha", "Beta", false);

        assertThat(border.isEnclosed()).isFalse();
    }

    @Test
    void should_SetEnclosedTrue_When_ConstructedWithEnclosedTrue() {
        Border border = BorderTestHelper.createBorder("Alpha", "Beta", true);

        assertThat(border.isEnclosed()).isTrue();
    }

    @Test
    void should_SetWrapAroundFalse_When_ConstructedWithDirectConstructor() {
        Border border = BorderTestHelper.createBorder("Alpha", "Beta");

        assertThat(border.isWrapAround()).isFalse();
    }

    @Test
    void should_HaveDefaultNumber_When_ConstructedWithoutExplicitNumber() {
        Border border = BorderTestHelper.createBorder("Alpha", "Beta");

        assertThat(border.getNumber()).isEqualTo(Border.DEFAULT_NUM);
    }

    @Test
    void should_HaveGivenNumber_When_ConstructedWithExplicitNumber() {
        Border border = BorderTestHelper.createBorder("Alpha", "Beta", false, 3);

        assertThat(border.getNumber()).isEqualTo(3);
    }

    // ========== Point Collection Tests ==========

    @Test
    void should_HaveZeroPoints_When_NewlyCreated() {
        Border border = BorderTestHelper.createBorder("Alpha", "Beta");

        assertThat(border.size()).isEqualTo(0);
    }

    @Test
    void should_IncreaseSizeAndReturnPoint_When_BorderPointAdded() {
        Border border = BorderTestHelper.createBorder("Alpha", "Beta");
        BorderPoint point = new BorderPoint(10, 20);

        border.addBorderPoint(point);

        assertThat(border.size()).isEqualTo(1);
        assertThat(border.getBorderPoint(0)).isSameAs(point);
    }

    @Test
    void should_AddMultiplePointsInOrder_When_AddBorderPointCalledMultipleTimes() {
        Border border = BorderTestHelper.createBorder("Alpha", "Beta");
        BorderPoint p1 = new BorderPoint(1, 1);
        BorderPoint p2 = new BorderPoint(2, 2);
        BorderPoint p3 = new BorderPoint(3, 3);

        border.addBorderPoint(p1);
        border.addBorderPoint(p2);
        border.addBorderPoint(p3);

        assertThat(border.size()).isEqualTo(3);
        assertThat(border.getBorderPoint(0)).isSameAs(p1);
        assertThat(border.getBorderPoint(1)).isSameAs(p2);
        assertThat(border.getBorderPoint(2)).isSameAs(p3);
    }

    @Test
    void should_DecreaseSizeAndUnlinkPoint_When_BorderPointRemoved() {
        Border border = BorderTestHelper.createBorder("Alpha", "Beta");
        BorderPoint point = new BorderPoint(10, 20);
        border.addBorderPoint(point);

        border.removeBorderPoint(point);

        assertThat(border.size()).isEqualTo(0);
        // point should no longer reference this border
        assertThat(point.getBorders().size()).isEqualTo(0);
    }

    @Test
    void should_ReturnCorrectIndex_When_GetPointIndexCalled() {
        Border border = BorderTestHelper.createBorder("Alpha", "Beta");
        BorderPoint p1 = new BorderPoint(5, 5);
        BorderPoint p2 = new BorderPoint(6, 6);
        border.addBorderPoint(p1);
        border.addBorderPoint(p2);

        assertThat(border.getPointIndex(p1)).isEqualTo(0);
        assertThat(border.getPointIndex(p2)).isEqualTo(1);
    }

    @Test
    void should_ReturnNegativeOne_When_PointNotInBorder() {
        Border border = BorderTestHelper.createBorder("Alpha", "Beta");
        BorderPoint absent = new BorderPoint(99, 99);

        assertThat(border.getPointIndex(absent)).isEqualTo(-1);
    }

    // ========== Equality Tests ==========

    @Test
    void should_BeEqual_When_SameTerritoriesAndSameNumber() {
        // Both borders use territories ordered identically by name.
        Territory tA = BorderTestHelper.createTerritory("Alpha");
        Territory tB = BorderTestHelper.createTerritory("Beta");
        Border b1 = new Border(tA, tB, false);
        Border b2 = new Border(tA, tB, true); // enclosed flag does not affect equality

        assertThat(b1).isEqualTo(b2);
    }

    @Test
    void should_NotBeEqual_When_DifferentNumber() {
        Territory tA = BorderTestHelper.createTerritory("Alpha");
        Territory tB = BorderTestHelper.createTerritory("Beta");
        Border b1 = new Border(tA, tB, false, 1);
        Border b2 = new Border(tA, tB, false, 2);

        assertThat(b1).isNotEqualTo(b2);
    }

    // ========== Path Direction Tests ==========

    @Test
    void should_DefaultPathStartsAtBeginning_When_NewBorder() {
        Border border = BorderTestHelper.createBorder("Alpha", "Beta");

        assertThat(border.getPathStartsAtBeginning()).isTrue();
    }

    @Test
    void should_ReturnFirstPoint_When_PathStartsAtBeginningAndGetStartPointCalled() {
        Border border = BorderTestHelper.createBorder("Alpha", "Beta");
        BorderPoint p1 = new BorderPoint(1, 1);
        BorderPoint p2 = new BorderPoint(2, 2);
        border.addBorderPoint(p1);
        border.addBorderPoint(p2);

        border.setPathStartsAtBeginning(true);

        assertThat(border.getStartPoint()).isSameAs(p1);
        assertThat(border.getEndPoint()).isSameAs(p2);
    }

    @Test
    void should_ReturnLastPoint_When_PathStartsAtEndAndGetStartPointCalled() {
        Border border = BorderTestHelper.createBorder("Alpha", "Beta");
        BorderPoint p1 = new BorderPoint(1, 1);
        BorderPoint p2 = new BorderPoint(2, 2);
        border.addBorderPoint(p1);
        border.addBorderPoint(p2);

        border.setPathStartsAtBeginning(false);

        assertThat(border.getStartPoint()).isSameAs(p2);
        assertThat(border.getEndPoint()).isSameAs(p1);
    }
}
