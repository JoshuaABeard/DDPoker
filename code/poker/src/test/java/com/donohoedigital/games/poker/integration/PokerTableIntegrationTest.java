/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
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
package com.donohoedigital.games.poker.integration;

import com.donohoedigital.games.poker.PokerGame;
import com.donohoedigital.games.poker.PokerPlayer;
import com.donohoedigital.games.poker.PokerTable;
import com.donohoedigital.games.poker.model.TournamentProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for PokerTable requiring GameEngine infrastructure.
 *
 * <p>These tests validate table functionality that depends on:</p>
 * <ul>
 *   <li>GameEngine.isDemo() - for button calculation and validation</li>
 *   <li>GameEngine event system - for observer registration</li>
 * </ul>
 *
 * <p>Run these tests separately from unit tests:</p>
 * <pre>
 * mvn test -Dgroups=integration
 * </pre>
 */
class PokerTableIntegrationTest extends IntegrationTestBase {

    private PokerTable table;
    private PokerGame game;

    @BeforeEach
    void setUp() {
        game = new PokerGame(null);
        TournamentProfile profile = new TournamentProfile("integration-test");
        profile.setBuyinChips(1500);
        game.setProfile(profile);

        table = new PokerTable(game, 1);
    }

    // =================================================================
    // Button and Position Tests
    // =================================================================

    @Test
    void should_SetButton_When_ButtonSet() {
        // Add players so button validation passes
        PokerPlayer player1 = createTestPlayer("Player1");
        PokerPlayer player2 = createTestPlayer("Player2");
        PokerPlayer player3 = createTestPlayer("Player3");
        PokerPlayer player4 = createTestPlayer("Player4");

        table.setPlayer(player1, 0);
        table.setPlayer(player2, 1);
        table.setPlayer(player3, 2);
        table.setPlayer(player4, 3);

        // This requires GameEngine validation
        table.setButton(3);

        assertThat(table.getButton()).isEqualTo(3);
    }

    @Test
    void should_CallSetButton_When_SetButtonNoArgsCalled() {
        PokerPlayer player1 = createTestPlayer("Player1");
        PokerPlayer player2 = createTestPlayer("Player2");
        table.setPlayer(player1, 0);
        table.setPlayer(player2, 1);

        // setButton() with no args calculates button position
        // This requires GameEngine.isDemo() for button calculation
        table.setButton();

        assertThat(table.getButton()).isGreaterThanOrEqualTo(0);
    }

    // =================================================================
    // Observer Tests
    // =================================================================

    @Test
    void should_AddObserver_When_ObserverAdded() {
        PokerPlayer observer = createTestPlayer("Observer");
        observer.setObserver(true);

        // This requires GameEngine event system for observer registration
        table.addObserver(observer);

        assertThat(table.getNumObservers()).isEqualTo(1);
    }

    @Test
    void should_RemoveObserver_When_ObserverRemoved() {
        PokerPlayer observer = createTestPlayer("Observer");
        observer.setObserver(true);
        table.addObserver(observer);

        // This requires GameEngine event system
        table.removeObserver(observer);

        assertThat(table.getNumObservers()).isZero();
    }

    @Test
    void should_AddMultipleObservers_When_MultipleObserversAdded() {
        PokerPlayer observer1 = createTestPlayer("Observer1");
        observer1.setObserver(true);
        PokerPlayer observer2 = createTestPlayer("Observer2");
        observer2.setObserver(true);

        // This requires GameEngine event system
        table.addObserver(observer1);
        table.addObserver(observer2);

        assertThat(table.getNumObservers()).isEqualTo(2);
    }

    // =================================================================
    // Test Helper Methods
    // =================================================================

    private PokerPlayer createTestPlayer(String name) {
        return new PokerPlayer(0, name, true);
    }
}
