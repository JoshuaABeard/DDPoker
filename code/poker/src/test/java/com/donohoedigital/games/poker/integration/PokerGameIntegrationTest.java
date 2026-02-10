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
import com.donohoedigital.games.poker.model.TournamentProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for PokerGame requiring GameEngine/PokerMain infrastructure.
 *
 * <p>These tests validate end-to-end game functionality that depends on:</p>
 * <ul>
 *   <li>PokerMain.getPokerMain() - for computer player setup</li>
 *   <li>GameEngine clock management - for game timing</li>
 * </ul>
 *
 * <p>Run these tests separately from unit tests:</p>
 * <pre>
 * mvn test -Dgroups=integration
 * </pre>
 */
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
