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
 * Tests for BorderPoints - deduplicated list of BorderPoint objects.
 */
class BorderPointsTest {

    // ========== Constructor Tests ==========

    @Test
    void should_CreateEmptyList_When_DefaultConstructorUsed() {
        BorderPoints points = new BorderPoints();

        assertThat(points).isEmpty();
    }

    @Test
    void should_CreateEmptyList_When_SizeConstructorUsed() {
        BorderPoints points = new BorderPoints(10);

        assertThat(points).isEmpty();
    }

    // ========== addBorderPoint Tests ==========

    @Test
    void should_AddAndReturnPoint_When_NewPointAdded() {
        BorderPoints points = new BorderPoints();
        BorderPoint bp = new BorderPoint(10, 20);

        BorderPoint result = points.addBorderPoint(bp);

        assertThat(result).isSameAs(bp);
        assertThat(points).hasSize(1);
    }

    @Test
    void should_ReturnExistingPoint_When_DuplicateAdded() {
        BorderPoints points = new BorderPoints();
        BorderPoint bp1 = new BorderPoint(10, 20);
        BorderPoint bp2 = new BorderPoint(10, 20);

        points.addBorderPoint(bp1);
        BorderPoint result = points.addBorderPoint(bp2);

        assertThat(result).isSameAs(bp1);
        assertThat(points).hasSize(1);
    }

    @Test
    void should_AddAtIndex_When_IndexProvided() {
        BorderPoints points = new BorderPoints();
        BorderPoint bp1 = new BorderPoint(1, 1);
        BorderPoint bp2 = new BorderPoint(2, 2);
        BorderPoint bp3 = new BorderPoint(3, 3);

        points.addBorderPoint(bp1);
        points.addBorderPoint(bp2);
        points.addBorderPoint(bp3, 1);

        assertThat(points.getBorderPoint(0)).isSameAs(bp1);
        assertThat(points.getBorderPoint(1)).isSameAs(bp3);
        assertThat(points.getBorderPoint(2)).isSameAs(bp2);
    }

    @Test
    void should_AddAtEnd_When_NegativeIndexProvided() {
        BorderPoints points = new BorderPoints();
        BorderPoint bp1 = new BorderPoint(1, 1);
        BorderPoint bp2 = new BorderPoint(2, 2);

        points.addBorderPoint(bp1);
        points.addBorderPoint(bp2, -1);

        assertThat(points.getBorderPoint(0)).isSameAs(bp1);
        assertThat(points.getBorderPoint(1)).isSameAs(bp2);
    }

    @Test
    void should_AddAtEnd_When_IndexExceedsSize() {
        BorderPoints points = new BorderPoints();
        BorderPoint bp1 = new BorderPoint(1, 1);
        BorderPoint bp2 = new BorderPoint(2, 2);

        points.addBorderPoint(bp1);
        points.addBorderPoint(bp2, 999);

        assertThat(points.getBorderPoint(1)).isSameAs(bp2);
    }

    // ========== getBorderPoint Tests ==========

    @Test
    void should_ReturnPoint_When_GetBorderPointCalledWithValidIndex() {
        BorderPoints points = new BorderPoints();
        BorderPoint bp = new BorderPoint(5, 10);
        points.addBorderPoint(bp);

        assertThat(points.getBorderPoint(0)).isSameAs(bp);
    }

    // ========== removeBorderPoint Tests ==========

    @Test
    void should_RemovePoint_When_RemoveBorderPointCalled() {
        BorderPoints points = new BorderPoints();
        BorderPoint bp = new BorderPoint(5, 10);
        points.addBorderPoint(bp);

        points.removeBorderPoint(bp);

        assertThat(points).isEmpty();
    }

    @Test
    void should_LeaveOtherPoints_When_OnePointRemoved() {
        BorderPoints points = new BorderPoints();
        BorderPoint bp1 = new BorderPoint(1, 1);
        BorderPoint bp2 = new BorderPoint(2, 2);
        points.addBorderPoint(bp1);
        points.addBorderPoint(bp2);

        points.removeBorderPoint(bp1);

        assertThat(points).hasSize(1);
        assertThat(points.getBorderPoint(0)).isSameAs(bp2);
    }
}
