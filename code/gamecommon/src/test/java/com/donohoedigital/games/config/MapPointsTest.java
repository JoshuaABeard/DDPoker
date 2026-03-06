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
 * Tests for MapPoints - deduplicated list of MapPoint objects.
 */
class MapPointsTest {

    // ========== Constructor Tests ==========

    @Test
    void should_CreateEmptyList_When_DefaultConstructorUsed() {
        MapPoints points = new MapPoints();

        assertThat(points).isEmpty();
    }

    @Test
    void should_CreateEmptyList_When_SizeConstructorUsed() {
        MapPoints points = new MapPoints(10);

        assertThat(points).isEmpty();
    }

    // ========== addMapPoint Tests ==========

    @Test
    void should_AddAndReturnPoint_When_NewPointAdded() {
        MapPoints points = new MapPoints();
        MapPoint p = new MapPoint(10, 20, "city");

        MapPoint result = points.addMapPoint(p);

        assertThat(result).isSameAs(p);
        assertThat(points).hasSize(1);
    }

    @Test
    void should_ReturnExistingPoint_When_DuplicateAdded() {
        MapPoints points = new MapPoints();
        MapPoint p1 = new MapPoint(10, 20, "city");
        MapPoint p2 = new MapPoint(10, 20, "city");

        points.addMapPoint(p1);
        MapPoint result = points.addMapPoint(p2);

        assertThat(result).isSameAs(p1);
        assertThat(points).hasSize(1);
    }

    @Test
    void should_AddAtIndex_When_IndexProvided() {
        MapPoints points = new MapPoints();
        MapPoint p1 = new MapPoint(1, 1, "a");
        MapPoint p2 = new MapPoint(2, 2, "b");
        MapPoint p3 = new MapPoint(3, 3, "c");

        points.addMapPoint(p1);
        points.addMapPoint(p2);
        points.addMapPoint(p3, 1);

        assertThat(points.getMapPoint(0)).isSameAs(p1);
        assertThat(points.getMapPoint(1)).isSameAs(p3);
        assertThat(points.getMapPoint(2)).isSameAs(p2);
    }

    @Test
    void should_AddAtEnd_When_NegativeIndexProvided() {
        MapPoints points = new MapPoints();
        MapPoint p1 = new MapPoint(1, 1, "a");
        MapPoint p2 = new MapPoint(2, 2, "b");

        points.addMapPoint(p1);
        points.addMapPoint(p2, -1);

        assertThat(points.getMapPoint(0)).isSameAs(p1);
        assertThat(points.getMapPoint(1)).isSameAs(p2);
    }

    @Test
    void should_AddAtEnd_When_IndexExceedsSize() {
        MapPoints points = new MapPoints();
        MapPoint p1 = new MapPoint(1, 1, "a");
        MapPoint p2 = new MapPoint(2, 2, "b");

        points.addMapPoint(p1);
        points.addMapPoint(p2, 999);

        assertThat(points.getMapPoint(1)).isSameAs(p2);
    }

    // ========== getMapPoint Tests ==========

    @Test
    void should_ReturnPoint_When_GetMapPointCalledWithValidIndex() {
        MapPoints points = new MapPoints();
        MapPoint p = new MapPoint(5, 10, "marker");
        points.addMapPoint(p);

        assertThat(points.getMapPoint(0)).isSameAs(p);
    }

    // ========== removeMapPoint Tests ==========

    @Test
    void should_RemovePoint_When_RemoveMapPointCalled() {
        MapPoints points = new MapPoints();
        MapPoint p = new MapPoint(5, 10, "marker");
        points.addMapPoint(p);

        points.removeMapPoint(p);

        assertThat(points).isEmpty();
    }

    @Test
    void should_LeaveOtherPoints_When_OnePointRemoved() {
        MapPoints points = new MapPoints();
        MapPoint p1 = new MapPoint(1, 1, "a");
        MapPoint p2 = new MapPoint(2, 2, "b");
        points.addMapPoint(p1);
        points.addMapPoint(p2);

        points.removeMapPoint(p1);

        assertThat(points).hasSize(1);
        assertThat(points.getMapPoint(0)).isSameAs(p2);
    }
}
