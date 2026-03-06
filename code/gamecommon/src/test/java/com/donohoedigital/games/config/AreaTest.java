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
 * Tests for Area - named region grouping territories.
 */
class AreaTest {

    // ========== Constructor Tests ==========

    @Test
    void should_SetName_When_StringConstructorUsed() {
        Area area = new Area("Europe");

        assertThat(area.getName()).isEqualTo("Europe");
    }

    // ========== getName / setName Tests ==========

    @Test
    void should_ReturnName_When_GetNameCalled() {
        Area area = new Area("Asia");

        assertThat(area.getName()).isEqualTo("Asia");
    }

    @Test
    void should_UpdateName_When_SetNameCalled() {
        Area area = new Area("Old");

        area.setName("New");

        assertThat(area.getName()).isEqualTo("New");
    }

    // ========== toString Tests ==========

    @Test
    void should_ReturnName_When_ToStringCalled() {
        Area area = new Area("Africa");

        assertThat(area.toString()).isEqualTo("Africa");
    }

    // ========== isNone Tests ==========

    @Test
    void should_ReturnFalse_When_IsNoneCalledOnStringConstructedArea() {
        Area area = new Area("SomeArea");

        assertThat(area.isNone()).isFalse();
    }

    @Test
    void should_ReturnFalse_When_IsNoneCalledWithNoneName() {
        // String constructor does not set bNone_ flag (only XML constructor does)
        Area area = new Area("NONE");

        assertThat(area.isNone()).isFalse();
    }

    // ========== equals Tests ==========

    @Test
    void should_BeEqual_When_SameName() {
        Area a1 = new Area("Europe");
        Area a2 = new Area("Europe");

        assertThat(a1).isEqualTo(a2);
    }

    @Test
    void should_NotBeEqual_When_DifferentName() {
        Area a1 = new Area("Europe");
        Area a2 = new Area("Asia");

        assertThat(a1).isNotEqualTo(a2);
    }

    @Test
    void should_BeEqual_When_SameInstance() {
        Area area = new Area("Europe");

        assertThat(area.equals(area)).isTrue();
    }

    @Test
    void should_NotBeEqual_When_ComparedToNull() {
        Area area = new Area("Europe");

        assertThat(area.equals(null)).isFalse();
    }

    @Test
    void should_NotBeEqual_When_ComparedToNonArea() {
        Area area = new Area("Europe");

        assertThat(area.equals("Europe")).isFalse();
    }

    // ========== COMPARATOR Tests ==========

    @SuppressWarnings("unchecked")
    @Test
    void should_ReturnZero_When_ComparatorComparesEqualAreas() {
        Area a1 = new Area("Europe");
        Area a2 = new Area("Europe");

        assertThat(Area.COMPARATOR.compare(a1, a2)).isEqualTo(0);
    }

    @SuppressWarnings("unchecked")
    @Test
    void should_ReturnNegative_When_ComparatorComparesAlphabeticallyEarlierArea() {
        Area a1 = new Area("Africa");
        Area a2 = new Area("Europe");

        assertThat(Area.COMPARATOR.compare(a1, a2)).isNegative();
    }

    @SuppressWarnings("unchecked")
    @Test
    void should_ReturnPositive_When_ComparatorComparesAlphabeticallyLaterArea() {
        Area a1 = new Area("Europe");
        Area a2 = new Area("Africa");

        assertThat(Area.COMPARATOR.compare(a1, a2)).isPositive();
    }

    @Test
    void should_OrderCorrectly_When_CompareStaticMethodUsed() {
        Area a1 = new Area("Alpha");
        Area a2 = new Area("Beta");

        assertThat(Area.compare(a1, a2)).isNegative();
        assertThat(Area.compare(a2, a1)).isPositive();
        assertThat(Area.compare(a1, a1)).isEqualTo(0);
    }

    // ========== getUserData / setUserData Tests ==========

    @Test
    void should_ReturnNull_When_UserDataNotSet() {
        Area area = new Area("Test");

        assertThat(area.getUserData()).isNull();
    }

    @Test
    void should_ReturnUserData_When_UserDataSet() {
        Area area = new Area("Test");
        Object data = "some data";

        area.setUserData(data);

        assertThat(area.getUserData()).isSameAs(data);
    }

    @Test
    void should_ReturnUpdatedUserData_When_UserDataOverwritten() {
        Area area = new Area("Test");
        area.setUserData("first");

        area.setUserData("second");

        assertThat(area.getUserData()).isEqualTo("second");
    }

    // ========== toStringDebug Tests ==========

    @Test
    void should_ContainName_When_ToStringDebugCalled() {
        Area area = new Area("TestRegion");

        String debug = area.toStringDebug();

        assertThat(debug).startsWith("TestRegion:");
    }

    @Test
    void should_ContainAllFields_When_ToStringDebugCalled() {
        Area area = new Area("MyArea");

        String debug = area.toStringDebug();

        // Before calculateStats, all numeric fields are 0
        assertThat(debug).contains("num=0");
        assertThat(debug).contains("islands=0");
        assertThat(debug).contains("adjland=0");
        assertThat(debug).contains("adjwater=0");
        assertThat(debug).contains("adjarea=0");
    }

    // ========== Stat Accessors (default values) Tests ==========

    @Test
    void should_ReturnZero_When_StatAccessorsCalledWithoutCalculation() {
        Area area = new Area("Fresh");

        assertThat(area.getNumTerritories()).isEqualTo(0);
        assertThat(area.getNumIslands()).isEqualTo(0);
        assertThat(area.getNumAdjacentWater()).isEqualTo(0);
        assertThat(area.getNumAdjacentLand()).isEqualTo(0);
        assertThat(area.getNumAdjacentAreas()).isEqualTo(0);
    }

    @Test
    void should_ReturnNull_When_GetTerritoriesCalledBeforeCalculation() {
        Area area = new Area("NoCalc");

        assertThat(area.getTerritories()).isNull();
    }
}
