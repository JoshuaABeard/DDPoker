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

    // ========== Remove Border Tests ==========

    @Test
    void should_RemoveBorder_When_RemoveBorderCalled() {
        BorderPoint point = new BorderPoint(1, 2);
        Border border = BorderTestHelper.createBorder("X", "Y");
        point.addBorder(border);

        point.removeBorder(border);

        assertThat(point.getBorders()).isEmpty();
    }

    @Test
    void should_RemoveFromAllBorders_When_RemoveFromAllBordersCalled() {
        Border border1 = BorderTestHelper.createBorder("A", "B");
        Border border2 = BorderTestHelper.createBorder("C", "D");
        BorderPoint point = new BorderPoint(5, 5);
        border1.addBorderPoint(point);
        border2.addBorderPoint(point);

        assertThat(point.getBorders()).hasSize(2);

        point.removeFromAllBorders();

        assertThat(point.getBorders()).isEmpty();
        assertThat(border1.size()).isZero();
        assertThat(border2.size()).isZero();
    }

    @Test
    void should_RemoveFromSpecificBorder_When_RemoveFromBorderCalled() {
        Border border1 = BorderTestHelper.createBorder("A", "B");
        Border border2 = BorderTestHelper.createBorder("C", "D");
        BorderPoint point = new BorderPoint(5, 5);
        border1.addBorderPoint(point);
        border2.addBorderPoint(point);

        point.removeFromBorder(border1);

        assertThat(border1.size()).isZero();
        assertThat(border2.size()).isEqualTo(1);
        assertThat(point.getBorders()).hasSize(1);
    }

    // ========== Current Border Tests ==========

    @Test
    void should_ReturnFirstBorder_When_GetCurrentBorderCalledWithoutSetting() {
        BorderPoint point = new BorderPoint(1, 2);
        Border border = BorderTestHelper.createBorder("X", "Y");
        point.addBorder(border);

        assertThat(point.getCurrentBorder()).isSameAs(border);
    }

    @Test
    void should_ReturnNull_When_GetCurrentBorderCalledOnOrphanPoint() {
        BorderPoint point = new BorderPoint(1, 2);

        assertThat(point.getCurrentBorder()).isNull();
    }

    @Test
    void should_SetCurrentBorder_When_SetCurrentBorderCalledWithValidBorder() {
        BorderPoint point = new BorderPoint(1, 2);
        Border border1 = BorderTestHelper.createBorder("A", "B");
        Border border2 = BorderTestHelper.createBorder("C", "D");
        point.addBorder(border1);
        point.addBorder(border2);

        point.setCurrentBorder(border2);

        assertThat(point.getCurrentBorder()).isSameAs(border2);
    }

    @Test
    void should_ClearCurrentBorder_When_RemovedBorderWasCurrent() {
        Border border = BorderTestHelper.createBorder("A", "B");
        BorderPoint point = new BorderPoint(1, 2);
        border.addBorderPoint(point);
        point.setCurrentBorder(border);

        point.removeBorder(border);

        // After removal, getCurrentBorder returns null since no borders remain
        assertThat(point.getCurrentBorder()).isNull();
    }

    // ========== Next Border Tests ==========

    @Test
    void should_CycleThroughBorders_When_NextBorderCalled() {
        BorderPoint point = new BorderPoint(1, 2);
        Border border1 = BorderTestHelper.createBorder("A", "B");
        Border border2 = BorderTestHelper.createBorder("C", "D");
        point.addBorder(border1);
        point.addBorder(border2);
        point.setCurrentBorder(border1);

        Border next = point.nextBorder();

        assertThat(next).isSameAs(border2);
    }

    @Test
    void should_WrapAround_When_NextBorderCalledOnLastBorder() {
        BorderPoint point = new BorderPoint(1, 2);
        Border border1 = BorderTestHelper.createBorder("A", "B");
        Border border2 = BorderTestHelper.createBorder("C", "D");
        point.addBorder(border1);
        point.addBorder(border2);
        point.setCurrentBorder(border2);

        Border next = point.nextBorder();

        assertThat(next).isSameAs(border1);
    }

    // ========== Navigation Tests ==========

    @Test
    void should_ReturnNextPoint_When_GetNextPointCalled() {
        Border border = BorderTestHelper.createBorder("A", "B");
        BorderPoint p1 = new BorderPoint(1, 1);
        BorderPoint p2 = new BorderPoint(2, 2);
        BorderPoint p3 = new BorderPoint(3, 3);
        border.addBorderPoint(p1);
        border.addBorderPoint(p2);
        border.addBorderPoint(p3);
        p2.setCurrentBorder(border);

        assertThat(p2.getNextPoint()).isSameAs(p3);
    }

    @Test
    void should_WrapToFirst_When_GetNextPointCalledOnLastPoint() {
        Border border = BorderTestHelper.createBorder("A", "B");
        BorderPoint p1 = new BorderPoint(1, 1);
        BorderPoint p2 = new BorderPoint(2, 2);
        border.addBorderPoint(p1);
        border.addBorderPoint(p2);
        p2.setCurrentBorder(border);

        assertThat(p2.getNextPoint()).isSameAs(p1);
    }

    @Test
    void should_ReturnPrevPoint_When_GetPrevPointCalled() {
        Border border = BorderTestHelper.createBorder("A", "B");
        BorderPoint p1 = new BorderPoint(1, 1);
        BorderPoint p2 = new BorderPoint(2, 2);
        BorderPoint p3 = new BorderPoint(3, 3);
        border.addBorderPoint(p1);
        border.addBorderPoint(p2);
        border.addBorderPoint(p3);
        p2.setCurrentBorder(border);

        assertThat(p2.getPrevPoint()).isSameAs(p1);
    }

    @Test
    void should_WrapToLast_When_GetPrevPointCalledOnFirstPoint() {
        Border border = BorderTestHelper.createBorder("A", "B");
        BorderPoint p1 = new BorderPoint(1, 1);
        BorderPoint p2 = new BorderPoint(2, 2);
        border.addBorderPoint(p1);
        border.addBorderPoint(p2);
        p1.setCurrentBorder(border);

        assertThat(p1.getPrevPoint()).isSameAs(p2);
    }

    @Test
    void should_ReturnAdjacentPoint_When_GetNearestPointCalled() {
        Border border = BorderTestHelper.createBorder("A", "B");
        BorderPoint p1 = new BorderPoint(1, 1);
        BorderPoint p2 = new BorderPoint(2, 2);
        BorderPoint p3 = new BorderPoint(3, 3);
        border.addBorderPoint(p1);
        border.addBorderPoint(p2);
        border.addBorderPoint(p3);
        p2.setCurrentBorder(border);

        // For delete nav, middle point returns previous point
        assertThat(p2.getNearestPoint()).isSameAs(p1);
    }

    @Test
    void should_ReturnSecondPoint_When_GetNearestPointCalledOnFirstPoint() {
        Border border = BorderTestHelper.createBorder("A", "B");
        BorderPoint p1 = new BorderPoint(1, 1);
        BorderPoint p2 = new BorderPoint(2, 2);
        border.addBorderPoint(p1);
        border.addBorderPoint(p2);
        p1.setCurrentBorder(border);

        // For delete nav at index 0, returns point at index 1
        assertThat(p1.getNearestPoint()).isSameAs(p2);
    }

    @Test
    void should_ReturnNull_When_GetNearestPointCalledOnSinglePointBorder() {
        Border border = BorderTestHelper.createBorder("A", "B");
        BorderPoint p1 = new BorderPoint(1, 1);
        border.addBorderPoint(p1);
        p1.setCurrentBorder(border);

        assertThat(p1.getNearestPoint()).isNull();
    }

    @Test
    void should_ReturnSelf_When_GetNextPointCalledOnSinglePointBorder() {
        Border border = BorderTestHelper.createBorder("A", "B");
        BorderPoint p1 = new BorderPoint(1, 1);
        border.addBorderPoint(p1);
        p1.setCurrentBorder(border);

        assertThat(p1.getNextPoint()).isSameAs(p1);
    }

    // ========== Shift Tests ==========

    @Test
    void should_MovePointUp_When_ShiftUpCalled() {
        Border border = BorderTestHelper.createBorder("A", "B");
        BorderPoint p1 = new BorderPoint(1, 1);
        BorderPoint p2 = new BorderPoint(2, 2);
        BorderPoint p3 = new BorderPoint(3, 3);
        border.addBorderPoint(p1);
        border.addBorderPoint(p2);
        border.addBorderPoint(p3);
        p2.setCurrentBorder(border);

        p2.shift(true); // shift up (toward higher index)

        assertThat(border.getBorderPoint(0)).isSameAs(p1);
        assertThat(border.getBorderPoint(1)).isSameAs(p3);
        assertThat(border.getBorderPoint(2)).isSameAs(p2);
    }

    @Test
    void should_MovePointDown_When_ShiftDownCalled() {
        Border border = BorderTestHelper.createBorder("A", "B");
        BorderPoint p1 = new BorderPoint(1, 1);
        BorderPoint p2 = new BorderPoint(2, 2);
        BorderPoint p3 = new BorderPoint(3, 3);
        border.addBorderPoint(p1);
        border.addBorderPoint(p2);
        border.addBorderPoint(p3);
        p2.setCurrentBorder(border);

        p2.shift(false); // shift down (toward lower index)

        assertThat(border.getBorderPoint(0)).isSameAs(p2);
        assertThat(border.getBorderPoint(1)).isSameAs(p1);
        assertThat(border.getBorderPoint(2)).isSameAs(p3);
    }

    @Test
    void should_WrapToBeginning_When_ShiftUpFromLastPosition() {
        Border border = BorderTestHelper.createBorder("A", "B");
        BorderPoint p1 = new BorderPoint(1, 1);
        BorderPoint p2 = new BorderPoint(2, 2);
        border.addBorderPoint(p1);
        border.addBorderPoint(p2);
        p2.setCurrentBorder(border);

        p2.shift(true); // shift up from last position wraps to beginning

        assertThat(border.getBorderPoint(0)).isSameAs(p2);
        assertThat(border.getBorderPoint(1)).isSameAs(p1);
    }

    @Test
    void should_WrapToEnd_When_ShiftDownFromFirstPosition() {
        Border border = BorderTestHelper.createBorder("A", "B");
        BorderPoint p1 = new BorderPoint(1, 1);
        BorderPoint p2 = new BorderPoint(2, 2);
        border.addBorderPoint(p1);
        border.addBorderPoint(p2);
        p1.setCurrentBorder(border);

        p1.shift(false); // shift down from first position wraps to end

        assertThat(border.getBorderPoint(0)).isSameAs(p2);
        assertThat(border.getBorderPoint(1)).isSameAs(p1);
    }

    // ========== toString / longDesc Tests ==========

    @Test
    void should_IncludeCoordinatesInToString() {
        BorderPoint point = new BorderPoint(42, 99);

        assertThat(point.toString()).contains("42").contains("99");
    }

    @Test
    void should_IncludeSharedBorderInfo_When_LongDescCalledWithNullBorderAndMultipleBorders() {
        BorderPoint point = new BorderPoint(5, 10);
        Border border1 = BorderTestHelper.createBorder("A", "B");
        Border border2 = BorderTestHelper.createBorder("C", "D");
        point.addBorder(border1);
        point.addBorder(border2);

        String desc = point.longDesc(null);

        assertThat(desc).contains("(5,10)");
        assertThat(desc).contains("used in:");
    }

    @Test
    void should_IncludeSharedWithInfo_When_LongDescCalledWithSpecificBorder() {
        BorderPoint point = new BorderPoint(5, 10);
        Border border1 = BorderTestHelper.createBorder("A", "B");
        Border border2 = BorderTestHelper.createBorder("C", "D");
        point.addBorder(border1);
        point.addBorder(border2);

        String desc = point.longDesc(border1);

        assertThat(desc).contains("(5,10)");
        assertThat(desc).contains("Shared with:");
    }

    @Test
    void should_OnlyShowCoordinates_When_LongDescCalledWithSingleBorder() {
        BorderPoint point = new BorderPoint(5, 10);
        Border border = BorderTestHelper.createBorder("A", "B");
        point.addBorder(border);

        String desc = point.longDesc(border);

        assertThat(desc).isEqualTo("(5,10)");
    }
}
