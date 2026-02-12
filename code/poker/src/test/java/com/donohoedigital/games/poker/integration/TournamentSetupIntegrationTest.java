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

import com.donohoedigital.config.ApplicationType;
import com.donohoedigital.config.ConfigManager;
import com.donohoedigital.games.poker.model.TournamentProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for tournament setup and configuration. Tests tournament
 * logic without launching the UI.
 */
@Tag("integration")
public class TournamentSetupIntegrationTest {
    @BeforeEach
    void setUp() throws Exception {
        // Initialize config for headless testing
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);
    }

    @Test
    void should_CreateTournament_When_ValidProfileProvided() {
        // Create a tournament profile
        TournamentProfile tournament = new TournamentProfile("TestTournament");

        // Verify creation
        assertThat(tournament).isNotNull();
        assertThat(tournament.getName()).isEqualTo("TestTournament");
    }

    @Test
    void should_SetDefaultValues_When_TournamentCreated() {
        // Create a tournament
        TournamentProfile tournament = new TournamentProfile("DefaultTest");

        // Verify defaults are set
        assertThat(tournament.getNumPlayers()).isGreaterThan(0);
        assertThat(tournament.getBuyinCost()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void should_AllowModification_When_TournamentCreated() {
        // Create a tournament
        TournamentProfile tournament = new TournamentProfile("ModifiableTest");

        // Modify settings
        tournament.setNumPlayers(8);
        tournament.setBuyin(1000);

        // Verify modifications
        assertThat(tournament.getNumPlayers()).isEqualTo(8);
        assertThat(tournament.getBuyinCost()).isEqualTo(1000);
    }

    @Test
    void should_SaveAndLoad_When_TournamentPersisted() {
        // Create and configure tournament
        TournamentProfile original = new TournamentProfile("PersistTest");
        original.setNumPlayers(6);
        original.setBuyin(500);
        original.initFile();
        original.setCreateDate();
        original.save();

        // Load from file
        TournamentProfile loaded = new TournamentProfile(original.getFile(), true);

        // Verify loaded correctly
        assertThat(loaded.getName()).isEqualTo("PersistTest");
        assertThat(loaded.getNumPlayers()).isEqualTo(6);
        assertThat(loaded.getBuyinCost()).isEqualTo(500);

        // Cleanup
        original.getFile().delete();
    }

    @Test
    void should_ValidateSettings_When_CreatingTournament() {
        // Create tournament
        TournamentProfile tournament = new TournamentProfile("ValidationTest");

        // Set valid values
        tournament.setNumPlayers(4);

        // Verify valid
        assertThat(tournament.getNumPlayers()).isEqualTo(4);
        assertThat(tournament.getNumPlayers()).isGreaterThan(1);
    }

    @Test
    void should_SupportMultipleBlindLevels_When_Configured() {
        // Create tournament
        TournamentProfile tournament = new TournamentProfile("BlindLevelsTest");

        // Verify blind structure exists (has at least 1 level)
        assertThat(tournament.getLastLevel()).isGreaterThan(0);
    }

    @Test
    void should_CalculatePayouts_When_TournamentConfigured() {
        // Create tournament with specific settings
        TournamentProfile tournament = new TournamentProfile("PayoutTest");
        tournament.setNumPlayers(9);
        tournament.setBuyin(1000);

        // Prize pool should equal total buy-ins (default has no house take)
        int expectedPrizePool = 9 * 1000;
        assertThat(tournament.getPrizePool()).isEqualTo(expectedPrizePool);
    }
}
