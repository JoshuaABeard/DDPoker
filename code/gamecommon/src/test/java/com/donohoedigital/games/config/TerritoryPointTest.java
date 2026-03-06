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

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for TerritoryPoint - a point belonging to a Territory.
 */
class TerritoryPointTest {

    /**
     * Creates a Territory via BorderTestHelper and injects a TerritoryPoints list
     * so that addTerritoryPoint/size/getPointIndex work correctly.
     */
    private static Territory createTerritoryWithPoints(String name) {
        Territory t = BorderTestHelper.createTerritory(name);
        try {
            Field pointsField = Territory.class.getDeclaredField("myPoints_");
            pointsField.setAccessible(true);
            pointsField.set(t, new TerritoryPoints());
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject myPoints_ on Territory", e);
        }
        return t;
    }

    // ========== Constructor Tests ==========

    @Test
    void should_SetCoordinatesAndType_When_Constructed() {
        TerritoryPoint tp = new TerritoryPoint(10, 20, "city");

        assertThat(tp.getX()).isEqualTo(10);
        assertThat(tp.getY()).isEqualTo(20);
        assertThat(tp.getType()).isEqualTo("city");
    }

    @Test
    void should_InheritMapPointBehavior_When_Constructed() {
        TerritoryPoint tp = new TerritoryPoint(5, 15, "label");

        assertThat(tp.toString()).contains("5").contains("15");
    }

    // ========== setTerritory / getTerritory Tests ==========

    @Test
    void should_ReturnNull_When_TerritoryNotSet() {
        TerritoryPoint tp = new TerritoryPoint(0, 0, "none");

        assertThat(tp.getTerritory()).isNull();
    }

    @Test
    void should_ReturnTerritory_When_TerritorySet() {
        TerritoryPoint tp = new TerritoryPoint(0, 0, "city");
        Territory territory = createTerritoryWithPoints("TestTerritory");

        tp.setTerritory(territory);

        assertThat(tp.getTerritory()).isSameAs(territory);
    }

    @Test
    void should_ClearTerritory_When_SetToNull() {
        TerritoryPoint tp = new TerritoryPoint(0, 0, "city");
        Territory territory = createTerritoryWithPoints("TestTerritory");
        tp.setTerritory(territory);

        tp.setTerritory(null);

        assertThat(tp.getTerritory()).isNull();
    }

    // ========== getNextPoint Tests ==========

    @Test
    void should_ReturnNull_When_GetNextPointCalledWithNoTerritory() {
        TerritoryPoint tp = new TerritoryPoint(0, 0, "test");

        assertThat(tp.getNextPoint()).isNull();
    }

    @Test
    void should_ReturnSamePoint_When_GetNextPointCalledOnSinglePointTerritory() {
        Territory territory = createTerritoryWithPoints("T");
        TerritoryPoint tp = new TerritoryPoint(0, 0, "test");
        territory.addTerritoryPoint(tp);

        assertThat(tp.getNextPoint()).isSameAs(tp);
    }

    @Test
    void should_ReturnNextPoint_When_GetNextPointCalledOnMiddlePoint() {
        Territory territory = createTerritoryWithPoints("T");
        TerritoryPoint tp0 = new TerritoryPoint(0, 0, "a");
        TerritoryPoint tp1 = new TerritoryPoint(1, 1, "b");
        TerritoryPoint tp2 = new TerritoryPoint(2, 2, "c");
        territory.addTerritoryPoint(tp0);
        territory.addTerritoryPoint(tp1);
        territory.addTerritoryPoint(tp2);

        assertThat(tp1.getNextPoint()).isSameAs(tp2);
    }

    @Test
    void should_WrapToFirst_When_GetNextPointCalledOnLastPoint() {
        Territory territory = createTerritoryWithPoints("T");
        TerritoryPoint tp0 = new TerritoryPoint(0, 0, "a");
        TerritoryPoint tp1 = new TerritoryPoint(1, 1, "b");
        territory.addTerritoryPoint(tp0);
        territory.addTerritoryPoint(tp1);

        assertThat(tp1.getNextPoint()).isSameAs(tp0);
    }

    // ========== getPrevPoint Tests ==========

    @Test
    void should_ReturnNull_When_GetPrevPointCalledWithNoTerritory() {
        TerritoryPoint tp = new TerritoryPoint(0, 0, "test");

        assertThat(tp.getPrevPoint()).isNull();
    }

    @Test
    void should_ReturnSamePoint_When_GetPrevPointCalledOnSinglePointTerritory() {
        Territory territory = createTerritoryWithPoints("T");
        TerritoryPoint tp = new TerritoryPoint(0, 0, "test");
        territory.addTerritoryPoint(tp);

        assertThat(tp.getPrevPoint()).isSameAs(tp);
    }

    @Test
    void should_ReturnPreviousPoint_When_GetPrevPointCalledOnMiddlePoint() {
        Territory territory = createTerritoryWithPoints("T");
        TerritoryPoint tp0 = new TerritoryPoint(0, 0, "a");
        TerritoryPoint tp1 = new TerritoryPoint(1, 1, "b");
        territory.addTerritoryPoint(tp0);
        territory.addTerritoryPoint(tp1);

        assertThat(tp1.getPrevPoint()).isSameAs(tp0);
    }

    @Test
    void should_WrapToLast_When_GetPrevPointCalledOnFirstPoint() {
        Territory territory = createTerritoryWithPoints("T");
        TerritoryPoint tp0 = new TerritoryPoint(0, 0, "a");
        TerritoryPoint tp1 = new TerritoryPoint(1, 1, "b");
        TerritoryPoint tp2 = new TerritoryPoint(2, 2, "c");
        territory.addTerritoryPoint(tp0);
        territory.addTerritoryPoint(tp1);
        territory.addTerritoryPoint(tp2);

        assertThat(tp0.getPrevPoint()).isSameAs(tp2);
    }

    // ========== getNearestPoint Tests ==========

    @Test
    void should_ReturnNull_When_GetNearestPointCalledWithNoTerritory() {
        TerritoryPoint tp = new TerritoryPoint(0, 0, "test");

        assertThat(tp.getNearestPoint()).isNull();
    }

    @Test
    void should_ReturnNull_When_GetNearestPointCalledOnSinglePointTerritory() {
        Territory territory = createTerritoryWithPoints("T");
        TerritoryPoint tp = new TerritoryPoint(0, 0, "test");
        territory.addTerritoryPoint(tp);

        assertThat(tp.getNearestPoint()).isNull();
    }

    @Test
    void should_ReturnSecondPoint_When_GetNearestPointCalledOnFirstOfTwo() {
        Territory territory = createTerritoryWithPoints("T");
        TerritoryPoint tp0 = new TerritoryPoint(0, 0, "a");
        TerritoryPoint tp1 = new TerritoryPoint(1, 1, "b");
        territory.addTerritoryPoint(tp0);
        territory.addTerritoryPoint(tp1);

        assertThat(tp0.getNearestPoint()).isSameAs(tp1);
    }

    @Test
    void should_ReturnPreviousPoint_When_GetNearestPointCalledOnNonFirstPoint() {
        Territory territory = createTerritoryWithPoints("T");
        TerritoryPoint tp0 = new TerritoryPoint(0, 0, "a");
        TerritoryPoint tp1 = new TerritoryPoint(1, 1, "b");
        TerritoryPoint tp2 = new TerritoryPoint(2, 2, "c");
        territory.addTerritoryPoint(tp0);
        territory.addTerritoryPoint(tp1);
        territory.addTerritoryPoint(tp2);

        assertThat(tp2.getNearestPoint()).isSameAs(tp1);
    }
}
