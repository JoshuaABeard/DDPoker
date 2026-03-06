/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 Joshua Beard and contributors
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
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for MapPoint - 2D point with type and angle metadata.
 */
class MapPointTest {

    // ========== Constructor Tests ==========

    @Test
    void should_SetXAndY_When_TwoArgConstructorUsed() {
        MapPoint point = new MapPoint(10, 20);

        assertThat(point.getX()).isEqualTo(10);
        assertThat(point.getY()).isEqualTo(20);
    }

    @Test
    void should_SetXYAndType_When_ThreeArgConstructorUsed() {
        MapPoint point = new MapPoint(5, 15, "city");

        assertThat(point.getX()).isEqualTo(5);
        assertThat(point.getY()).isEqualTo(15);
        assertThat(point.getType()).isEqualTo("city");
    }

    @Test
    void should_SetXYAngleAndType_When_FourArgConstructorUsed() {
        MapPoint point = new MapPoint(3, 7, 45, "marker");

        assertThat(point.getX()).isEqualTo(3);
        assertThat(point.getY()).isEqualTo(7);
        assertThat(point.getAngle()).isEqualTo(45);
        assertThat(point.getType()).isEqualTo("marker");
    }

    // ========== Default Values Tests ==========

    @Test
    void should_DefaultTypeToNoType_When_TwoArgConstructorUsed() {
        MapPoint point = new MapPoint(0, 0);

        assertThat(point.getType()).isEqualTo(MapPoint.NO_TYPE);
    }

    @Test
    void should_DefaultAngleToZero_When_NoAngleProvided() {
        MapPoint point = new MapPoint(0, 0);

        assertThat(point.getAngle()).isEqualTo(0);
    }

    // ========== Getter/Setter Tests ==========

    @Test
    void should_UpdateX_When_SetXCalled() {
        MapPoint point = new MapPoint(0, 0);

        point.setX(100);

        assertThat(point.getX()).isEqualTo(100);
    }

    @Test
    void should_UpdateY_When_SetYCalled() {
        MapPoint point = new MapPoint(0, 0);

        point.setY(200);

        assertThat(point.getY()).isEqualTo(200);
    }

    @Test
    void should_UpdateType_When_SetTypeCalled() {
        MapPoint point = new MapPoint(0, 0);

        point.setType("forest");

        assertThat(point.getType()).isEqualTo("forest");
    }

    @Test
    void should_UpdateAngle_When_SetAngleCalled() {
        MapPoint point = new MapPoint(0, 0);

        point.setAngle(90);

        assertThat(point.getAngle()).isEqualTo(90);
    }

    // ========== Equals Tests ==========

    @Test
    void should_BeEqual_When_SameXYAndType() {
        MapPoint p1 = new MapPoint(10, 20, "city");
        MapPoint p2 = new MapPoint(10, 20, "city");

        assertThat(p1).isEqualTo(p2);
    }

    @Test
    void should_NotBeEqual_When_DifferentX() {
        MapPoint p1 = new MapPoint(10, 20, "city");
        MapPoint p2 = new MapPoint(11, 20, "city");

        assertThat(p1).isNotEqualTo(p2);
    }

    @Test
    void should_NotBeEqual_When_DifferentY() {
        MapPoint p1 = new MapPoint(10, 20, "city");
        MapPoint p2 = new MapPoint(10, 21, "city");

        assertThat(p1).isNotEqualTo(p2);
    }

    @Test
    void should_NotBeEqual_When_DifferentType() {
        MapPoint p1 = new MapPoint(10, 20, "city");
        MapPoint p2 = new MapPoint(10, 20, "town");

        assertThat(p1).isNotEqualTo(p2);
    }

    @Test
    void should_BeEqual_When_DifferentAngleButSameXYType() {
        // Angle doesn't affect equality per the implementation
        MapPoint p1 = new MapPoint(10, 20, 45, "city");
        MapPoint p2 = new MapPoint(10, 20, 90, "city");

        assertThat(p1).isEqualTo(p2);
    }

    @Test
    void should_BeEqualToSelf_When_SameReference() {
        MapPoint point = new MapPoint(5, 5);

        assertThat(point.equals(point)).isTrue();
    }

    @Test
    void should_NotBeEqual_When_ComparedToNonMapPoint() {
        MapPoint point = new MapPoint(5, 5);

        assertThat(point.equals("not a point")).isFalse();
    }

    // ========== toString / shortDesc Tests ==========

    @Test
    void should_ReturnCoordinates_When_ToStringCalledWithDefaultType() {
        MapPoint point = new MapPoint(10, 20);

        String result = point.toString();

        assertThat(result).isEqualTo("(10,20)");
    }

    @Test
    void should_IncludeType_When_ToStringCalledWithType() {
        MapPoint point = new MapPoint(10, 20, "city");

        String result = point.shortDesc();

        assertThat(result).contains("city");
        assertThat(result).contains("(10,20)");
    }

    @Test
    void should_IncludeAngle_When_ToStringCalledWithAngle() {
        MapPoint point = new MapPoint(10, 20, 45, "city");

        String result = point.shortDesc();

        assertThat(result).contains("45 degrees");
    }

    @Test
    void should_MatchToString_When_ShortDescCalled() {
        MapPoint point = new MapPoint(10, 20);

        assertThat(point.toString()).isEqualTo(point.shortDesc());
    }
}
