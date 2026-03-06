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
 * Tests for TerritoryPoints - deduplicated list of TerritoryPoint objects.
 */
class TerritoryPointsTest {

    // ========== Constructor Tests ==========

    @Test
    void should_CreateEmptyList_When_DefaultConstructorUsed() {
        TerritoryPoints points = new TerritoryPoints();

        assertThat(points).isEmpty();
    }

    @Test
    void should_CreateEmptyList_When_SizeConstructorUsed() {
        TerritoryPoints points = new TerritoryPoints(10);

        assertThat(points).isEmpty();
    }

    // ========== addTerritoryPoint Tests ==========

    @Test
    void should_AddAndReturnPoint_When_NewPointAdded() {
        TerritoryPoints points = new TerritoryPoints();
        TerritoryPoint tp = new TerritoryPoint(10, 20, "city");

        TerritoryPoint result = points.addTerritoryPoint(tp);

        assertThat(result).isSameAs(tp);
        assertThat(points).hasSize(1);
    }

    @Test
    void should_ReturnExistingPoint_When_DuplicateAdded() {
        TerritoryPoints points = new TerritoryPoints();
        TerritoryPoint tp1 = new TerritoryPoint(10, 20, "city");
        TerritoryPoint tp2 = new TerritoryPoint(10, 20, "city");

        points.addTerritoryPoint(tp1);
        TerritoryPoint result = points.addTerritoryPoint(tp2);

        assertThat(result).isSameAs(tp1);
        assertThat(points).hasSize(1);
    }

    @Test
    void should_AddAtIndex_When_IndexProvided() {
        TerritoryPoints points = new TerritoryPoints();
        TerritoryPoint tp1 = new TerritoryPoint(1, 1, "a");
        TerritoryPoint tp2 = new TerritoryPoint(2, 2, "b");
        TerritoryPoint tp3 = new TerritoryPoint(3, 3, "c");

        points.addTerritoryPoint(tp1);
        points.addTerritoryPoint(tp2);
        points.addTerritoryPoint(tp3, 1);

        assertThat(points.getTerritoryPoint(0)).isSameAs(tp1);
        assertThat(points.getTerritoryPoint(1)).isSameAs(tp3);
        assertThat(points.getTerritoryPoint(2)).isSameAs(tp2);
    }

    @Test
    void should_AddAtEnd_When_NegativeIndexProvided() {
        TerritoryPoints points = new TerritoryPoints();
        TerritoryPoint tp1 = new TerritoryPoint(1, 1, "a");
        TerritoryPoint tp2 = new TerritoryPoint(2, 2, "b");

        points.addTerritoryPoint(tp1);
        points.addTerritoryPoint(tp2, -1);

        assertThat(points.getTerritoryPoint(0)).isSameAs(tp1);
        assertThat(points.getTerritoryPoint(1)).isSameAs(tp2);
    }

    @Test
    void should_AddAtEnd_When_IndexExceedsSize() {
        TerritoryPoints points = new TerritoryPoints();
        TerritoryPoint tp1 = new TerritoryPoint(1, 1, "a");
        TerritoryPoint tp2 = new TerritoryPoint(2, 2, "b");

        points.addTerritoryPoint(tp1);
        points.addTerritoryPoint(tp2, 999);

        assertThat(points.getTerritoryPoint(1)).isSameAs(tp2);
    }

    // ========== getTerritoryPoint Tests ==========

    @Test
    void should_ReturnPoint_When_GetTerritoryPointCalledWithValidIndex() {
        TerritoryPoints points = new TerritoryPoints();
        TerritoryPoint tp = new TerritoryPoint(5, 10, "marker");
        points.addTerritoryPoint(tp);

        assertThat(points.getTerritoryPoint(0)).isSameAs(tp);
    }

    // ========== removeTerritoryPoint Tests ==========

    @Test
    void should_RemovePoint_When_RemoveTerritoryPointCalled() {
        TerritoryPoints points = new TerritoryPoints();
        TerritoryPoint tp = new TerritoryPoint(5, 10, "marker");
        points.addTerritoryPoint(tp);

        points.removeTerritoryPoint(tp);

        assertThat(points).isEmpty();
    }

    @Test
    void should_LeaveOtherPoints_When_OnePointRemoved() {
        TerritoryPoints points = new TerritoryPoints();
        TerritoryPoint tp1 = new TerritoryPoint(1, 1, "a");
        TerritoryPoint tp2 = new TerritoryPoint(2, 2, "b");
        points.addTerritoryPoint(tp1);
        points.addTerritoryPoint(tp2);

        points.removeTerritoryPoint(tp1);

        assertThat(points).hasSize(1);
        assertThat(points.getTerritoryPoint(0)).isSameAs(tp2);
    }
}
