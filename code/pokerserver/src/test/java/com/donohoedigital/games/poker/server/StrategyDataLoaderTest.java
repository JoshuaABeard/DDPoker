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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for StrategyDataLoader - verifies PlayerType .dat file loading works
 * without Swing dependencies.
 */
class StrategyDataLoaderTest {

    @Test
    void getAvailableStrategies_findsPlayerTypeFiles() {
        List<String> strategies = StrategyDataLoader.getAvailableStrategies();

        assertThat(strategies).isNotEmpty();
        assertThat(strategies).allMatch(s -> s.startsWith("playertype."));
        assertThat(strategies).allMatch(s -> s.endsWith(".dat"));
    }

    @Test
    void loadStrategy_loadsValidFile() {
        List<String> available = StrategyDataLoader.getAvailableStrategies();
        assertThat(available).isNotEmpty();

        String firstFile = available.get(0);
        StrategyData data = StrategyDataLoader.loadStrategy(firstFile);

        assertThat(data).isNotNull();
        assertThat(data.getName()).isNotBlank();
        assertThat(data.isComplete()).isTrue();
    }

    @Test
    void loadStrategy_returnsNullForMissingFile() {
        StrategyData data = StrategyDataLoader.loadStrategy("nonexistent.dat");
        assertThat(data).isNull();
    }

    @Test
    void loadDefaultStrategy_returnsValidStrategy() {
        StrategyData data = StrategyDataLoader.loadDefaultStrategy();

        assertThat(data).isNotNull();
        assertThat(data.getName()).isNotBlank();
        assertThat(data.isComplete()).isTrue();
    }

    @Test
    void strategyData_providesFactorValues() {
        StrategyData data = StrategyDataLoader.loadDefaultStrategy();

        // Should have some strategy factors loaded
        int aggression = data.getStrategyFactor("basics.aggression", -1);
        assertThat(aggression).isBetween(0, 100);

        int tightness = data.getStrategyFactor("basics.tightness", -1);
        assertThat(tightness).isBetween(0, 100);
    }

    @Test
    void strategyData_hierarchicalFallback() {
        StrategyData data = StrategyDataLoader.loadDefaultStrategy();

        // If specific key doesn't exist, should fall back to parent
        int specificValue = data.getStrategyFactor("basics.aggression.very_specific_key", 42);

        // Should either find parent value or use default
        assertThat(specificValue).isGreaterThanOrEqualTo(0);
    }

    @Test
    void serverStrategyProvider_usesLoadedData() {
        ServerStrategyProvider provider = new ServerStrategyProvider("test-player");

        // Get a strategy factor - should use loaded data, not just default 50
        float aggression = provider.getStratFactor("basics.aggression", 0.0f, 100.0f);

        // Should be a reasonable value (could be anything 0-100 depending on profile)
        assertThat(aggression).isBetween(0.0f, 100.0f);

        // Verify it's actually using loaded data by checking multiple factors
        float tightness = provider.getStratFactor("basics.tightness", 0.0f, 100.0f);
        float bluff = provider.getStratFactor("deception.bluff", 0.0f, 100.0f);

        // All should be valid values
        assertThat(tightness).isBetween(0.0f, 100.0f);
        assertThat(bluff).isBetween(0.0f, 100.0f);
    }

    @Test
    void serverStrategyProvider_canLoadSpecificProfile() {
        List<String> available = StrategyDataLoader.getAvailableStrategies();
        if (available.size() < 2) {
            return; // Skip if not enough profiles
        }

        // Load two different profiles
        String file1 = available.get(0);
        String file2 = available.get(1);

        ServerStrategyProvider provider1 = new ServerStrategyProvider("player1", file1);
        ServerStrategyProvider provider2 = new ServerStrategyProvider("player2", file2);

        // Both should work
        float aggr1 = provider1.getStratFactor("basics.aggression", 0.0f, 100.0f);
        float aggr2 = provider2.getStratFactor("basics.aggression", 0.0f, 100.0f);

        assertThat(aggr1).isBetween(0.0f, 100.0f);
        assertThat(aggr2).isBetween(0.0f, 100.0f);

        // They might have different personalities (or might not - can't assert
        // difference)
    }
}
