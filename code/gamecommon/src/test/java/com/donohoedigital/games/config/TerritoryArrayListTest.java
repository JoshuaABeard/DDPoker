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
 * Tests for TerritoryArrayList - list of Territory objects with toString.
 */
class TerritoryArrayListTest {

    // ========== Constructor Tests ==========

    @Test
    void should_CreateEmptyList_When_Constructed() {
        TerritoryArrayList list = new TerritoryArrayList();

        assertThat(list).isEmpty();
    }

    // ========== toString Tests ==========

    @Test
    void should_ReturnEmptyString_When_ListEmpty() {
        TerritoryArrayList list = new TerritoryArrayList();

        assertThat(list.toString()).isEmpty();
    }

    @Test
    void should_ReturnTerritoryName_When_SingleTerritoryPresent() {
        TerritoryArrayList list = new TerritoryArrayList();
        Territory t = BorderTestHelper.createTerritory("France");
        list.add(t);

        assertThat(list.toString()).isEqualTo("France");
    }

    @Test
    void should_SeparateWithCommas_When_MultipleTerritoriesPresent() {
        TerritoryArrayList list = new TerritoryArrayList();
        list.add(BorderTestHelper.createTerritory("France"));
        list.add(BorderTestHelper.createTerritory("Germany"));
        list.add(BorderTestHelper.createTerritory("Spain"));

        String result = list.toString();

        assertThat(result).isEqualTo("France, Germany, Spain");
    }

    @Test
    void should_ReturnTwoNames_When_TwoTerritoriesPresent() {
        TerritoryArrayList list = new TerritoryArrayList();
        list.add(BorderTestHelper.createTerritory("Alpha"));
        list.add(BorderTestHelper.createTerritory("Beta"));

        String result = list.toString();

        assertThat(result).isEqualTo("Alpha, Beta");
    }
}
