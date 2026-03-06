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
package com.donohoedigital.games.poker.server;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for StrategyData AI personality configuration.
 */
class StrategyDataTest {

    // ========== Constructor Tests ==========

    @Test
    void should_ReturnName_When_Constructed() {
        StrategyData data = new StrategyData("aggressive", "An aggressive player");

        assertThat(data.getName()).isEqualTo("aggressive");
    }

    @Test
    void should_ReturnDescription_When_Constructed() {
        StrategyData data = new StrategyData("aggressive", "An aggressive player");

        assertThat(data.getDescription()).isEqualTo("An aggressive player");
    }

    @Test
    void should_HandleNullNameAndDescription() {
        StrategyData data = new StrategyData(null, null);

        assertThat(data.getName()).isNull();
        assertThat(data.getDescription()).isNull();
    }

    // ========== Strategy Factor Tests ==========

    @Test
    void should_ReturnSetValue_When_FactorExists() {
        StrategyData data = new StrategyData("test", "test");

        data.setStrategyFactor("basics.aggression", 75);

        assertThat(data.getStrategyFactor("basics.aggression", 0)).isEqualTo(75);
    }

    @Test
    void should_ReturnDefault_When_FactorMissing() {
        StrategyData data = new StrategyData("test", "test");

        assertThat(data.getStrategyFactor("nonexistent.key", 42)).isEqualTo(42);
    }

    @Test
    void should_OverwriteFactor_When_SetTwice() {
        StrategyData data = new StrategyData("test", "test");

        data.setStrategyFactor("basics.aggression", 50);
        data.setStrategyFactor("basics.aggression", 90);

        assertThat(data.getStrategyFactor("basics.aggression", 0)).isEqualTo(90);
    }

    @Test
    void should_FallbackToParentKey_When_SpecificKeyMissing() {
        StrategyData data = new StrategyData("test", "test");

        data.setStrategyFactor("basics.aggression", 60);

        // Querying a child key should fall back to parent
        assertThat(data.getStrategyFactor("basics.aggression.suited_ace", 0)).isEqualTo(60);
    }

    @Test
    void should_FallbackMultipleLevels_When_IntermediateKeysMissing() {
        StrategyData data = new StrategyData("test", "test");

        data.setStrategyFactor("basics", 30);

        // Two levels of fallback: basics.aggression.suited_ace -> basics.aggression ->
        // basics
        assertThat(data.getStrategyFactor("basics.aggression.suited_ace", 0)).isEqualTo(30);
    }

    @Test
    void should_PreferExactKey_Over_ParentKey() {
        StrategyData data = new StrategyData("test", "test");

        data.setStrategyFactor("basics", 30);
        data.setStrategyFactor("basics.aggression", 60);
        data.setStrategyFactor("basics.aggression.suited_ace", 90);

        assertThat(data.getStrategyFactor("basics.aggression.suited_ace", 0)).isEqualTo(90);
        assertThat(data.getStrategyFactor("basics.aggression", 0)).isEqualTo(60);
        assertThat(data.getStrategyFactor("basics", 0)).isEqualTo(30);
    }

    @Test
    void should_ReturnDefault_When_NoParentKeyExists() {
        StrategyData data = new StrategyData("test", "test");

        data.setStrategyFactor("other.key", 50);

        assertThat(data.getStrategyFactor("basics.aggression", 99)).isEqualTo(99);
    }

    // ========== Hand Selection Scheme Tests ==========

    @Test
    void should_ReturnScheme_When_SetForTableSize() {
        StrategyData data = new StrategyData("test", "test");

        data.setHandSelectionScheme("full", "/schemes/full-tight.dat");

        assertThat(data.getHandSelectionScheme("full")).isEqualTo("/schemes/full-tight.dat");
    }

    @Test
    void should_ReturnNull_When_SchemeNotSet() {
        StrategyData data = new StrategyData("test", "test");

        assertThat(data.getHandSelectionScheme("full")).isNull();
    }

    @Test
    void should_TrackSchemesPerTableSize() {
        StrategyData data = new StrategyData("test", "test");

        data.setHandSelectionScheme("full", "/schemes/full.dat");
        data.setHandSelectionScheme("short", "/schemes/short.dat");
        data.setHandSelectionScheme("veryshort", "/schemes/veryshort.dat");
        data.setHandSelectionScheme("hup", "/schemes/hup.dat");

        assertThat(data.getHandSelectionScheme("full")).isEqualTo("/schemes/full.dat");
        assertThat(data.getHandSelectionScheme("short")).isEqualTo("/schemes/short.dat");
        assertThat(data.getHandSelectionScheme("veryshort")).isEqualTo("/schemes/veryshort.dat");
        assertThat(data.getHandSelectionScheme("hup")).isEqualTo("/schemes/hup.dat");
    }

    @Test
    void should_OverwriteScheme_When_SetTwiceForSameSize() {
        StrategyData data = new StrategyData("test", "test");

        data.setHandSelectionScheme("full", "/schemes/old.dat");
        data.setHandSelectionScheme("full", "/schemes/new.dat");

        assertThat(data.getHandSelectionScheme("full")).isEqualTo("/schemes/new.dat");
    }

    // ========== isComplete Tests ==========

    @Test
    void should_ReturnFalse_When_NoFactorsSet() {
        StrategyData data = new StrategyData("test", "test");

        assertThat(data.isComplete()).isFalse();
    }

    @Test
    void should_ReturnTrue_When_AtLeastOneFactorSet() {
        StrategyData data = new StrategyData("test", "test");

        data.setStrategyFactor("basics.aggression", 50);

        assertThat(data.isComplete()).isTrue();
    }

    @Test
    void should_ReturnFalse_When_OnlySchemesSetNoFactors() {
        StrategyData data = new StrategyData("test", "test");

        data.setHandSelectionScheme("full", "/schemes/full.dat");

        assertThat(data.isComplete()).isFalse();
    }
}
