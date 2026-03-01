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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for Territories — add, get, size, iteration, and cached array. Uses
 * {@link BorderTestHelper} to bypass XML/Areas constructor dependencies.
 */
class TerritoriesTest {

    private Territories territories;

    @BeforeEach
    void setUp() {
        territories = BorderTestHelper.createTerritories();
    }

    // ========== Empty State Tests ==========

    @Test
    void should_HaveSizeZero_When_Empty() {
        assertThat(territories.size()).isEqualTo(0);
    }

    @Test
    void should_ReturnNull_When_GetTerritoryCalledOnEmptyCollection() {
        assertThat(territories.getTerritory("Gondor")).isNull();
    }

    // ========== Add / Get Tests ==========

    @Test
    void should_ReturnTerritory_When_AddedAndRetrievedByName() {
        Territory gondor = BorderTestHelper.createTerritory("Gondor");

        territories.addTerritory(gondor);

        assertThat(territories.getTerritory("Gondor")).isSameAs(gondor);
    }

    @Test
    void should_IncreaseSizeByOne_When_TerritoryAdded() {
        territories.addTerritory(BorderTestHelper.createTerritory("Rohan"));

        assertThat(territories.size()).isEqualTo(1);
    }

    @Test
    void should_ReturnNull_When_TerritoryNameNotFound() {
        territories.addTerritory(BorderTestHelper.createTerritory("Gondor"));

        assertThat(territories.getTerritory("Mordor")).isNull();
    }

    // ========== Multiple Territories Tests ==========

    @Test
    void should_StoreAllTerritories_When_MultipleAdded() {
        Territory gondor = BorderTestHelper.createTerritory("Gondor");
        Territory rohan = BorderTestHelper.createTerritory("Rohan");
        Territory mordor = BorderTestHelper.createTerritory("Mordor");

        territories.addTerritory(gondor);
        territories.addTerritory(rohan);
        territories.addTerritory(mordor);

        assertThat(territories.size()).isEqualTo(3);
        assertThat(territories.getTerritory("Gondor")).isSameAs(gondor);
        assertThat(territories.getTerritory("Rohan")).isSameAs(rohan);
        assertThat(territories.getTerritory("Mordor")).isSameAs(mordor);
    }

    // ========== getTerritoryArray Tests ==========

    @Test
    void should_ReturnAllTerritories_When_GetTerritoryArrayCalled() {
        Territory gondor = BorderTestHelper.createTerritory("Gondor");
        Territory rohan = BorderTestHelper.createTerritory("Rohan");
        territories.addTerritory(gondor);
        territories.addTerritory(rohan);

        Territory[] array = territories.getTerritoryArray();

        assertThat(array).hasSize(2);
        assertThat(array).containsExactlyInAnyOrder(gondor, rohan);
    }

    @Test
    void should_ReturnTerritoriesInNameOrder_When_GetTerritoryArrayCalled() {
        // Territories extends TreeMap which sorts keys lexicographically.
        Territory zebra = BorderTestHelper.createTerritory("Zebra");
        Territory alpha = BorderTestHelper.createTerritory("Alpha");
        territories.addTerritory(zebra);
        territories.addTerritory(alpha);

        Territory[] array = territories.getTerritoryArray();

        assertThat(array[0].getName()).isEqualTo("Alpha");
        assertThat(array[1].getName()).isEqualTo("Zebra");
    }

    // ========== getTerritoryArrayCached Tests ==========

    @Test
    void should_ReturnSameArrayInstance_When_GetTerritoryArrayCachedCalledTwice() {
        territories.addTerritory(BorderTestHelper.createTerritory("Gondor"));

        Territory[] first = territories.getTerritoryArrayCached();
        Territory[] second = territories.getTerritoryArrayCached();

        assertThat(first).isSameAs(second);
    }

    @Test
    void should_ReturnNewArrayInstance_When_GetTerritoryArrayCalledTwice() {
        territories.addTerritory(BorderTestHelper.createTerritory("Gondor"));

        Territory[] first = territories.getTerritoryArray();
        Territory[] second = territories.getTerritoryArray();

        assertThat(first).isNotSameAs(second);
    }
}
