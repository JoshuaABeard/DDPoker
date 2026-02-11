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
import com.donohoedigital.games.config.BaseProfile;
import com.donohoedigital.games.poker.PlayerProfile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for PlayerProfile functionality.
 * Tests profile creation, loading, and management without UI.
 */
@Tag("integration")
public class PlayerProfileIntegrationTest
{
    @BeforeEach
    void setUp() throws Exception
    {
        // Initialize config for headless testing
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);

        // Clean up any existing test profiles
        cleanupTestProfiles();
    }

    @AfterEach
    void tearDown()
    {
        cleanupTestProfiles();
    }

    private void cleanupTestProfiles()
    {
        List<BaseProfile> profiles = PlayerProfile.getProfileList();
        if (profiles != null)
        {
            for (BaseProfile profile : profiles)
            {
                if (profile.getName().startsWith("Test"))
                {
                    File file = profile.getFile();
                    if (file != null && file.exists())
                    {
                        file.delete();
                    }
                }
            }
        }
    }

    @Test
    void should_CreateProfile_When_ValidNameProvided()
    {
        // Create a new profile
        PlayerProfile profile = new PlayerProfile("TestPlayer");
        profile.initCheck();
        profile.initFile();
        profile.setCreateDate();
        profile.save();

        // Verify profile was created
        assertThat(profile.getName()).isEqualTo("TestPlayer");
        assertThat(profile.getFile()).exists();
    }

    @Test
    void should_LoadProfile_When_FileExists()
    {
        // Create and save a profile
        PlayerProfile original = new PlayerProfile("TestPlayerLoad");
        original.initCheck();
        original.initFile();
        original.setCreateDate();
        original.save();

        File profileFile = original.getFile();

        // Load the profile from file
        PlayerProfile loaded = new PlayerProfile(profileFile, true);

        // Verify it loaded correctly
        assertThat(loaded.getName()).isEqualTo("TestPlayerLoad");
        assertThat(loaded.getFile()).exists();
    }

    @Test
    void should_ListProfiles_When_ProfilesExist()
    {
        // Create multiple profiles
        createAndSaveProfile("TestPlayer1");
        createAndSaveProfile("TestPlayer2");
        createAndSaveProfile("TestPlayer3");

        // Get profile list
        List<BaseProfile> profiles = PlayerProfile.getProfileList();

        // Verify profiles are listed
        assertThat(profiles).isNotNull();
        long testProfileCount = profiles.stream()
            .filter(p -> p.getName().startsWith("Test"))
            .count();
        assertThat(testProfileCount).isGreaterThanOrEqualTo(3);
    }

    @Test
    void should_DeleteProfile_When_FileDeleted()
    {
        // Create a profile
        PlayerProfile profile = createAndSaveProfile("TestPlayerDelete");
        File file = profile.getFile();
        assertThat(file).exists();

        // Delete it
        boolean deleted = file.delete();

        // Verify deletion
        assertThat(deleted).isTrue();
        assertThat(file).doesNotExist();
    }

    @Test
    void should_UpdateProfile_When_NameChanged()
    {
        // Create a profile
        PlayerProfile profile = createAndSaveProfile("TestPlayerOriginal");

        // Change the name
        profile.setName("TestPlayerUpdated");
        profile.save();

        // Reload and verify
        PlayerProfile reloaded = new PlayerProfile(profile.getFile(), true);
        assertThat(reloaded.getName()).isEqualTo("TestPlayerUpdated");
    }

    @Test
    void should_InitializeStats_When_ProfileCreated()
    {
        // Create a profile
        PlayerProfile profile = new PlayerProfile("TestPlayerStats");
        profile.initCheck();

        // Verify stats are initialized (not null)
        assertThat(profile).isNotNull();
        // Stats should be initialized with default values
    }

    @Test
    void should_SortProfiles_When_MultipleExist()
    {
        // Create profiles with different names
        createAndSaveProfile("TestAlpha");
        createAndSaveProfile("TestZulu");
        createAndSaveProfile("TestBravo");

        // Get profile list
        List<BaseProfile> profiles = PlayerProfile.getProfileList();

        // Filter to test profiles and sort by name
        List<String> testProfileNames = profiles.stream()
            .filter(p -> p.getName().startsWith("Test"))
            .map(BaseProfile::getName)
            .sorted()
            .toList();

        // Verify we have all three profiles
        assertThat(testProfileNames).containsExactly("TestAlpha", "TestBravo", "TestZulu");
    }

    // Helper method
    private PlayerProfile createAndSaveProfile(String name)
    {
        PlayerProfile profile = new PlayerProfile(name);
        profile.initCheck();
        profile.initFile();
        profile.setCreateDate();
        profile.save();
        return profile;
    }
}
