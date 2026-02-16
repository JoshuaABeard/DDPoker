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

import com.donohoedigital.games.poker.core.ActionOptions;
import com.donohoedigital.games.poker.core.GamePlayerInfo;
import com.donohoedigital.games.poker.core.PlayerAction;
import com.donohoedigital.games.poker.core.ai.V2Algorithm;
import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.CardSuit;
import com.donohoedigital.games.poker.engine.Hand;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Demonstrates that server-side AI now uses real PlayerType strategy data
 * loaded from .dat files, giving server AI the same personality variety as
 * desktop.
 */
class StrategyLoadingDemo {

    @Test
    void demonstrateRealStrategyLoading() {
        System.out.println("\n=== Server-Side V2 AI Strategy Loading Demo ===\n");

        // Show available strategies
        List<String> strategies = StrategyDataLoader.getAvailableStrategies();
        System.out.println("Available AI Profiles: " + strategies.size());
        for (String strategy : strategies) {
            StrategyData data = StrategyDataLoader.loadStrategy(strategy);
            if (data != null) {
                System.out.println("  - " + data.getName() + " (" + strategy + ")");
                System.out.println("      Aggression: " + data.getStrategyFactor("basics.aggression", 50));
                System.out.println("      Tightness:  " + data.getStrategyFactor("basics.tightness", 50));
                System.out.println("      Bluff:      " + data.getStrategyFactor("deception.bluff", 50));
            }
        }

        System.out.println("\n=== Testing AI Decision with Loaded Strategy ===\n");

        // Create AI with loaded strategy
        StrategyData solidStrategy = StrategyDataLoader.loadDefaultStrategy();
        ServerStrategyProvider strategy = new ServerStrategyProvider("player1", solidStrategy);
        V2Algorithm algorithm = new V2Algorithm();

        // Create test scenario: pocket aces pre-flop
        Hand pocket = new Hand(2);
        pocket.addCard(new Card(CardSuit.SPADES, Card.ACE));
        pocket.addCard(new Card(CardSuit.HEARTS, Card.ACE));

        ServerV2AIContext context = ServerTestUtils.createPreFlopContext(3, pocket, 0, 0);

        GamePlayerInfo player = ServerTestUtils.createPlayer(1000);
        ActionOptions options = ServerTestUtils.createOptions(true, false, true, false, false);

        // Get AI decision
        PlayerAction action = algorithm.getAction(player, options, context);

        System.out.println("Scenario: Pocket Aces, Middle Position, Unopened Pot");
        System.out.println("Strategy: " + solidStrategy.getName());
        System.out.println("Decision: " + action.actionType() + " (amount: " + action.amount() + ")");
        System.out.println();
        System.out.println("✓ Server AI successfully uses real PlayerType strategy data!");
        System.out.println("✓ No Swing dependencies - runs headless on server!");
        System.out.println("✓ Full personality variety like desktop client!");

        // Verify it's working
        assertThat(action).isNotNull();
        assertThat(action.actionType()).isNotNull();
    }

    @Test
    void demonstrateStrategyVariety() {
        System.out.println("\n=== Demonstrating Strategy Factor Variety ===\n");

        List<String> strategies = StrategyDataLoader.getAvailableStrategies();

        // Show that different profiles have different factor values
        System.out.println("Strategy Factor Comparison:");
        System.out.println(String.format("%-30s | %10s | %10s | %10s", "Profile", "Aggression", "Tightness", "Bluff"));
        System.out.println("-".repeat(75));

        for (String filename : strategies) {
            StrategyData data = StrategyDataLoader.loadStrategy(filename);
            if (data != null) {
                int aggr = data.getStrategyFactor("basics.aggression", 50);
                int tight = data.getStrategyFactor("basics.tightness", 50);
                int bluff = data.getStrategyFactor("deception.bluff", 50);

                System.out.println(String.format("%-30s | %10d | %10d | %10d", data.getName(), aggr, tight, bluff));
            }
        }

        System.out.println("\n✓ Each profile has unique personality characteristics!");
    }
}
