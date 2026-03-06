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
        int expectedNum = 3;
        Border border = BorderTestHelper.createBorder("Alpha", "Beta", false, expectedNum);

        assertThat(border.getNumber()).isEqualTo(expectedNum);
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
        assertThat(point.getBorders()).isEmpty();
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

    // ========== Setter Tests ==========

    @Test
    void should_UpdateEnclosed_When_SetEnclosedCalled() {
        Border border = BorderTestHelper.createBorder("Alpha", "Beta");

        border.setEnclosed(true);
        assertThat(border.isEnclosed()).isTrue();

        border.setEnclosed(false);
        assertThat(border.isEnclosed()).isFalse();
    }

    @Test
    void should_UpdateWrapAround_When_SetWrapAroundCalled() {
        Border border = BorderTestHelper.createBorder("Alpha", "Beta");

        border.setWrapAround(true);
        assertThat(border.isWrapAround()).isTrue();

        border.setWrapAround(false);
        assertThat(border.isWrapAround()).isFalse();
    }

    @Test
    void should_UpdateNumber_When_SetNumberCalled() {
        Border border = BorderTestHelper.createBorder("Alpha", "Beta");

        border.setNumber(5);

        assertThat(border.getNumber()).isEqualTo(5);
    }

    @Test
    void should_UpdateTerritory1_When_SetTerritory1Called() {
        Territory tA = BorderTestHelper.createTerritory("Alpha");
        Territory tB = BorderTestHelper.createTerritory("Beta");
        Border border = new Border(tA, tB, false);
        Territory tC = BorderTestHelper.createTerritory("Charlie");

        border.setTerritory1(tC);

        assertThat(border.getTerritory1()).isSameAs(tC);
    }

    @Test
    void should_UpdateTerritory2_When_SetTerritory2Called() {
        Territory tA = BorderTestHelper.createTerritory("Alpha");
        Territory tB = BorderTestHelper.createTerritory("Beta");
        Border border = new Border(tA, tB, false);
        Territory tC = BorderTestHelper.createTerritory("Charlie");

        border.setTerritory2(tC);

        assertThat(border.getTerritory2()).isSameAs(tC);
    }

    // ========== Territory Ordering Tests ==========

    @Test
    void should_OrderTerritoriesAlphabetically_When_ConstructedInReverseOrder() {
        Territory tA = BorderTestHelper.createTerritory("Alpha");
        Territory tB = BorderTestHelper.createTerritory("Beta");
        // Pass Beta first, Alpha second — constructor should reorder
        Border border = new Border(tB, tA, false);

        assertThat(border.getTerritory1().getName()).isEqualTo("Alpha");
        assertThat(border.getTerritory2().getName()).isEqualTo("Beta");
    }

    @Test
    void should_PreserveOrder_When_ConstructedInAlphabeticalOrder() {
        Territory tA = BorderTestHelper.createTerritory("Alpha");
        Territory tB = BorderTestHelper.createTerritory("Beta");
        Border border = new Border(tA, tB, false);

        assertThat(border.getTerritory1()).isSameAs(tA);
        assertThat(border.getTerritory2()).isSameAs(tB);
    }

    // ========== Contains Tests ==========

    @Test
    void should_ReturnTrue_When_ContainsMatchingTerritoriesAndNumber() {
        Territory tA = BorderTestHelper.createTerritory("Alpha");
        Territory tB = BorderTestHelper.createTerritory("Beta");
        Border border = new Border(tA, tB, false);

        assertThat(border.contains(tA, tB, 1)).isTrue();
    }

    @Test
    void should_ReturnTrue_When_ContainsTerritoriesInReversedOrder() {
        Territory tA = BorderTestHelper.createTerritory("Alpha");
        Territory tB = BorderTestHelper.createTerritory("Beta");
        Border border = new Border(tA, tB, false);

        // contains uses reference equality with swapped order
        assertThat(border.contains(tB, tA, 1)).isTrue();
    }

    @Test
    void should_ReturnFalse_When_ContainsCalledWithWrongNumber() {
        Territory tA = BorderTestHelper.createTerritory("Alpha");
        Territory tB = BorderTestHelper.createTerritory("Beta");
        Border border = new Border(tA, tB, false);

        assertThat(border.contains(tA, tB, 2)).isFalse();
    }

    @Test
    void should_ReturnFalse_When_ContainsCalledWithDifferentTerritory() {
        Territory tA = BorderTestHelper.createTerritory("Alpha");
        Territory tB = BorderTestHelper.createTerritory("Beta");
        Territory tC = BorderTestHelper.createTerritory("Charlie");
        Border border = new Border(tA, tB, false);

        assertThat(border.contains(tA, tC, 1)).isFalse();
    }

    // ========== Equality Edge Case Tests ==========

    @Test
    void should_BeEqualToItself_When_ComparedWithSameInstance() {
        Border border = BorderTestHelper.createBorder("Alpha", "Beta");

        assertThat(border.equals(border)).isTrue();
    }

    @Test
    void should_NotBeEqual_When_ComparedWithNull() {
        Border border = BorderTestHelper.createBorder("Alpha", "Beta");

        assertThat(border.equals(null)).isFalse();
    }

    @Test
    void should_NotBeEqual_When_ComparedWithNonBorderObject() {
        Border border = BorderTestHelper.createBorder("Alpha", "Beta");

        assertThat(border.equals("not a border")).isFalse();
    }

    @Test
    void should_NotBeEqual_When_DifferentTerritories() {
        Territory tA = BorderTestHelper.createTerritory("Alpha");
        Territory tB = BorderTestHelper.createTerritory("Beta");
        Territory tC = BorderTestHelper.createTerritory("Charlie");
        Border b1 = new Border(tA, tB, false);
        Border b2 = new Border(tA, tC, false);

        assertThat(b1).isNotEqualTo(b2);
    }

    // ========== HashCode Tests ==========

    @Test
    void should_HaveSameHashCode_When_EqualBorders() {
        Territory tA = BorderTestHelper.createTerritory("Alpha");
        Territory tB = BorderTestHelper.createTerritory("Beta");
        Border b1 = new Border(tA, tB, false);
        Border b2 = new Border(tA, tB, true); // enclosed does not affect equality

        assertThat(b1.hashCode()).isEqualTo(b2.hashCode());
    }

    @Test
    void should_HaveDifferentHashCode_When_DifferentNumber() {
        Territory tA = BorderTestHelper.createTerritory("Alpha");
        Territory tB = BorderTestHelper.createTerritory("Beta");
        Border b1 = new Border(tA, tB, false, 1);
        Border b2 = new Border(tA, tB, false, 2);

        // Not strictly required, but highly likely for well-distributed hash
        assertThat(b1.hashCode()).isNotEqualTo(b2.hashCode());
    }

    // ========== ShortDesc / ToString Tests ==========

    @Test
    void should_ContainTerritoryNames_When_ShortDescCalled() {
        Territory tA = BorderTestHelper.createTerritory("Alpha");
        Territory tB = BorderTestHelper.createTerritory("Beta");
        Border border = new Border(tA, tB, false);

        assertThat(border.shortDesc()).isEqualTo("[Alpha - Beta]");
    }

    @Test
    void should_IncludeNumber_When_NumberGreaterThanOne() {
        Territory tA = BorderTestHelper.createTerritory("Alpha");
        Territory tB = BorderTestHelper.createTerritory("Beta");
        Border border = new Border(tA, tB, false, 3);

        assertThat(border.shortDesc()).contains("(#3)");
    }

    @Test
    void should_NotIncludeNumber_When_NumberIsOne() {
        Territory tA = BorderTestHelper.createTerritory("Alpha");
        Territory tB = BorderTestHelper.createTerritory("Beta");
        Border border = new Border(tA, tB, false, 1);

        assertThat(border.shortDesc()).doesNotContain("#");
    }

    @Test
    void should_IncludeWrapLabel_When_WrapAroundSet() {
        Territory tA = BorderTestHelper.createTerritory("Alpha");
        Territory tB = BorderTestHelper.createTerritory("Beta");
        Border border = new Border(tA, tB, false);
        border.setWrapAround(true);

        assertThat(border.shortDesc()).contains("(wrap)");
    }

    @Test
    void should_IncludeEnclosedLabel_When_EnclosedSet() {
        Territory tA = BorderTestHelper.createTerritory("Alpha");
        Territory tB = BorderTestHelper.createTerritory("Beta");
        Border border = new Border(tA, tB, true);

        assertThat(border.shortDesc()).contains("(enclosed)");
    }

    @Test
    void should_MatchShortDesc_When_ToStringCalled() {
        Territory tA = BorderTestHelper.createTerritory("Alpha");
        Territory tB = BorderTestHelper.createTerritory("Beta");
        Border border = new Border(tA, tB, false);

        assertThat(border.toString()).isEqualTo(border.shortDesc());
    }

    // ========== COMPARATOR Tests ==========

    @Test
    void should_ReturnZero_When_ComparingSameBorder() {
        Territory tA = BorderTestHelper.createTerritory("Alpha");
        Territory tB = BorderTestHelper.createTerritory("Beta");
        Border b1 = new Border(tA, tB, false);
        Border b2 = new Border(tA, tB, false);

        assertThat(Border.compare(b1, b2)).isZero();
    }

    @Test
    void should_OrderByFirstTerritory_When_FirstTerritoriesDiffer() {
        Territory tA = BorderTestHelper.createTerritory("Alpha");
        Territory tB = BorderTestHelper.createTerritory("Beta");
        Territory tC = BorderTestHelper.createTerritory("Charlie");
        Border bAB = new Border(tA, tB, false);
        Border bBC = new Border(tB, tC, false);

        assertThat(Border.compare(bAB, bBC)).isLessThan(0);
        assertThat(Border.compare(bBC, bAB)).isGreaterThan(0);
    }

    @Test
    void should_OrderBySecondTerritory_When_FirstTerritoriesMatch() {
        Territory tA = BorderTestHelper.createTerritory("Alpha");
        Territory tB = BorderTestHelper.createTerritory("Beta");
        Territory tC = BorderTestHelper.createTerritory("Charlie");
        Border bAB = new Border(tA, tB, false);
        Border bAC = new Border(tA, tC, false);

        assertThat(Border.compare(bAB, bAC)).isLessThan(0);
        assertThat(Border.compare(bAC, bAB)).isGreaterThan(0);
    }

    @Test
    void should_OrderByNumber_When_TerritoriesMatch() {
        Territory tA = BorderTestHelper.createTerritory("Alpha");
        Territory tB = BorderTestHelper.createTerritory("Beta");
        Border b1 = new Border(tA, tB, false, 1);
        Border b2 = new Border(tA, tB, false, 2);

        assertThat(Border.compare(b1, b2)).isLessThan(0);
        assertThat(Border.compare(b2, b1)).isGreaterThan(0);
    }

    // ========== GetBorderPoint Boundary Tests ==========

    @Test
    void should_ReturnNull_When_GetBorderPointWithNegativeIndex() {
        Border border = BorderTestHelper.createBorder("Alpha", "Beta");
        border.addBorderPoint(new BorderPoint(1, 1));

        assertThat(border.getBorderPoint(-1)).isNull();
    }

    @Test
    void should_ReturnNull_When_GetBorderPointWithIndexOutOfBounds() {
        Border border = BorderTestHelper.createBorder("Alpha", "Beta");
        border.addBorderPoint(new BorderPoint(1, 1));

        assertThat(border.getBorderPoint(5)).isNull();
    }

    @Test
    void should_ReturnNull_When_GetBorderPointOnEmptyBorder() {
        Border border = BorderTestHelper.createBorder("Alpha", "Beta");

        assertThat(border.getBorderPoint(0)).isNull();
    }

    // ========== GetBorderPoints Tests ==========

    @Test
    void should_ReturnBorderPointsCollection_When_GetBorderPointsCalled() {
        Border border = BorderTestHelper.createBorder("Alpha", "Beta");
        BorderPoint p = new BorderPoint(10, 20);
        border.addBorderPoint(p);

        BorderPoints points = border.getBorderPoints();

        assertThat(points).isNotNull();
        assertThat(points).hasSize(1);
        assertThat(points.getBorderPoint(0)).isSameAs(p);
    }

    // ========== AddBorderPoint At Index Tests ==========

    @Test
    void should_InsertAtIndex_When_AddBorderPointCalledWithIndex() {
        Border border = BorderTestHelper.createBorder("Alpha", "Beta");
        BorderPoint p1 = new BorderPoint(1, 1);
        BorderPoint p2 = new BorderPoint(2, 2);
        BorderPoint p3 = new BorderPoint(3, 3);
        border.addBorderPoint(p1);
        border.addBorderPoint(p3);

        border.addBorderPoint(p2, 1);

        assertThat(border.getBorderPoint(0)).isSameAs(p1);
        assertThat(border.getBorderPoint(1)).isSameAs(p2);
        assertThat(border.getBorderPoint(2)).isSameAs(p3);
    }

    // ========== Start/End Point Boundary Tests ==========

    @Test
    void should_ReturnNull_When_GetStartPointOnEmptyBorder() {
        Border border = BorderTestHelper.createBorder("Alpha", "Beta");

        assertThat(border.getStartPoint()).isNull();
        assertThat(border.getEndPoint()).isNull();
    }

    // ========== Four-Param Constructor Tests ==========

    @Test
    void should_SetAllFields_When_FourParamConstructorUsed() {
        Territory tA = BorderTestHelper.createTerritory("Alpha");
        Territory tB = BorderTestHelper.createTerritory("Beta");
        Border border = new Border(tA, tB, true, 7);

        assertThat(border.isEnclosed()).isTrue();
        assertThat(border.isWrapAround()).isFalse();
        assertThat(border.getNumber()).isEqualTo(7);
        assertThat(border.getTerritory1().getName()).isEqualTo("Alpha");
        assertThat(border.getTerritory2().getName()).isEqualTo("Beta");
    }
}
