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
 * Tests for Territory — name, id, type flags, equality, comparison, and border
 * associations. Uses {@link BorderTestHelper} to bypass constructor
 * dependencies.
 */
class TerritoryTest {

    // ========== Name Tests ==========

    @Test
    void should_ReturnName_When_TerritoryCreated() {
        Territory territory = BorderTestHelper.createTerritory("Gondor");

        assertThat(territory.getName()).isEqualTo("Gondor");
    }

    @Test
    void should_ReturnUpdatedName_When_SetNameCalled() {
        Territory territory = BorderTestHelper.createTerritory("Gondor");

        territory.setName("Rohan");

        assertThat(territory.getName()).isEqualTo("Rohan");
    }

    // ========== ID Tests ==========

    @Test
    void should_ReturnNegativeOne_When_IdNotSet() {
        Territory territory = BorderTestHelper.createTerritory("Gondor");

        // Unsafe-allocated instance has null id_ field
        assertThat(territory.getID()).isEqualTo(-1);
    }

    @Test
    void should_ReturnId_When_SetIdCalled() {
        Territory territory = BorderTestHelper.createTerritory("Gondor");

        territory.setID(42);

        assertThat(territory.getID()).isEqualTo(42);
    }

    // ========== Type / Boolean Flag Tests ==========

    @Test
    void should_SetEdgeFlag_When_SetTypeCalledWithEdgeValue() {
        Territory territory = BorderTestHelper.createTerritory("Edge");

        territory.setType(Territory.EDGE);

        assertThat(territory.isEdge()).isTrue();
        assertThat(territory.isDecoration()).isFalse();
        assertThat(territory.isWater()).isFalse();
        assertThat(territory.isLand()).isFalse();
    }

    @Test
    void should_SetLandFlag_When_SetTypeCalledWithLandValue() {
        Territory territory = BorderTestHelper.createTerritory("Plains");

        territory.setType(Territory.LAND);

        assertThat(territory.isLand()).isTrue();
        assertThat(territory.isEdge()).isFalse();
        assertThat(territory.isWater()).isFalse();
        assertThat(territory.isDecoration()).isFalse();
    }

    @Test
    void should_SetWaterFlag_When_SetTypeCalledWithWaterValue() {
        Territory territory = BorderTestHelper.createTerritory("Ocean");

        territory.setType(Territory.WATER);

        assertThat(territory.isWater()).isTrue();
        assertThat(territory.isLand()).isFalse();
    }

    @Test
    void should_SetDecorationFlag_When_SetTypeCalledWithDecorationValue() {
        Territory territory = BorderTestHelper.createTerritory("Ornament");

        territory.setType(Territory.DECORATION);

        assertThat(territory.isDecoration()).isTrue();
        assertThat(territory.isLand()).isFalse();
    }

    // ========== Equality Tests ==========

    @Test
    void should_BeEqual_When_TwoTerritoriesHaveSameName() {
        Territory t1 = BorderTestHelper.createTerritory("Shire");
        Territory t2 = BorderTestHelper.createTerritory("Shire");

        assertThat(t1).isEqualTo(t2);
    }

    @Test
    void should_NotBeEqual_When_TwoTerritoriesHaveDifferentNames() {
        Territory t1 = BorderTestHelper.createTerritory("Shire");
        Territory t2 = BorderTestHelper.createTerritory("Mordor");

        assertThat(t1).isNotEqualTo(t2);
    }

    @Test
    void should_BeEqualToItself_When_SameReference() {
        Territory territory = BorderTestHelper.createTerritory("Shire");

        assertThat(territory).isEqualTo(territory);
    }

    // ========== Comparison Tests ==========

    @Test
    void should_ReturnZero_When_ComparedToTerritoryWithSameName() {
        Territory t1 = BorderTestHelper.createTerritory("Rohan");
        Territory t2 = BorderTestHelper.createTerritory("Rohan");

        assertThat(t1.compareTo(t2)).isEqualTo(0);
    }

    @Test
    void should_ReturnNegative_When_ComparedToLexicographicallyLaterTerritory() {
        Territory t1 = BorderTestHelper.createTerritory("Alpha");
        Territory t2 = BorderTestHelper.createTerritory("Zebra");

        assertThat(t1.compareTo(t2)).isLessThan(0);
    }

    // ========== Border Association Tests ==========

    @Test
    void should_HaveEmptyBorderList_When_TerritoryCreated() {
        Territory territory = BorderTestHelper.createTerritory("Gondor");

        assertThat(territory.getBorders().size()).isEqualTo(0);
    }

    @Test
    void should_ListBorder_When_BorderAdded() {
        Territory t1 = BorderTestHelper.createTerritory("Alpha");
        Territory t2 = BorderTestHelper.createTerritory("Beta");
        Border border = new Border(t1, t2, false);

        t1.addBorder(border);

        assertThat(t1.getBorders().size()).isEqualTo(1);
        assertThat(t1.getBorders().getBorder(0)).isSameAs(border);
    }

    @Test
    void should_RemoveBorder_When_RemoveBorderCalled() {
        Territory t1 = BorderTestHelper.createTerritory("Alpha");
        Territory t2 = BorderTestHelper.createTerritory("Beta");
        Border border = new Border(t1, t2, false);
        t1.addBorder(border);

        t1.removeBorder(border);

        assertThat(t1.getBorders().size()).isEqualTo(0);
    }
}
