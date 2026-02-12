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
package com.donohoedigital.games.poker.integration;

import com.donohoedigital.games.poker.PokerGame;
import com.donohoedigital.games.poker.model.TournamentProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for PokerGame requiring GameEngine/PokerMain
 * infrastructure.
 *
 * <p>
 * These tests validate end-to-end game functionality that depends on:
 * </p>
 * <ul>
 * <li>PokerMain.getPokerMain() - for computer player setup</li>
 * <li>GameEngine clock management - for game timing</li>
 * </ul>
 *
 * <p>
 * Run these tests separately from unit tests:
 * </p>
 *
 * <pre>
 * mvn test -Dgroups=integration
 * </pre>
 */
@Tag("slow")
class PokerGameIntegrationTest extends IntegrationTestBase {

    private PokerGame game;

    @BeforeEach
    void setUp() {
        game = new PokerGame(null);
    }

    @Test
    void should_InitTournament_When_ProfileProvided() {
        TournamentProfile profile = createTestProfile();

        // This requires PokerMain.getPokerMain().getNames() for computer player setup
        assertThatCode(() -> game.initTournament(profile)).doesNotThrowAnyException();

        // Verify tournament was initialized
        assertThat(game.getProfile()).isEqualTo(profile);
        assertThat(game.getNumPlayers()).isGreaterThan(0);
    }

    @Test
    void should_AdvanceClock_When_ClockModeActive() {
        game.setClockMode(true);
        TournamentProfile profile = createTestProfile();
        game.setProfile(profile);

        // This requires GameEngine clock management
        assertThatCode(() -> game.advanceClock()).doesNotThrowAnyException();
    }

    // =================================================================
    // Test Helper Methods
    // =================================================================

    private TournamentProfile createTestProfile() {
        TournamentProfile profile = new TournamentProfile();
        profile.setName("Integration Test Tournament");
        profile.setNumPlayers(10);
        profile.setBuyinChips(1500);
        return profile;
    }
}
