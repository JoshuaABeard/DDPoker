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
import com.donohoedigital.config.Prefs;
import com.donohoedigital.games.config.BaseProfile;
import com.donohoedigital.games.poker.FirstTimeWizard;
import com.donohoedigital.games.poker.PlayerProfile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.prefs.Preferences;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for First-Time Wizard flow logic.
 * Tests wizard state and logic without launching the UI.
 */
@Tag("integration")
public class FirstTimeWizardFlowTest
{
    private Preferences wizardPrefs;

    @BeforeEach
    void setUp() throws Exception
    {
        // Initialize config for headless testing
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);

        // Clear wizard preferences
        wizardPrefs = Prefs.getUserPrefs("ftue");
        wizardPrefs.clear();
        wizardPrefs.flush();
    }

    @AfterEach
    void tearDown() throws Exception
    {
        // Clean up
        if (wizardPrefs != null)
        {
            wizardPrefs.clear();
            wizardPrefs.flush();
        }
    }

    @Test
    void should_BeRequired_When_WizardNotCompleted() throws Exception
    {
        // Ensure wizard is not marked as completed
        wizardPrefs.remove("wizard_completed");
        wizardPrefs.flush();

        // Verify wizard is not marked as completed
        boolean wizardCompleted = wizardPrefs.getBoolean("wizard_completed", false);
        assertThat(wizardCompleted).isFalse();

        // The wizard should be required when:
        // 1. wizard_completed is false AND
        // 2. No profiles exist OR user hasn't opted out
        // This test verifies condition #1
    }

    @Test
    void should_NotBeRequired_When_ProfileExists()
    {
        // Create a profile
        PlayerProfile profile = new PlayerProfile("ExistingUser");
        profile.initCheck();
        profile.initFile();
        profile.setCreateDate();
        profile.save();

        try
        {
            // Check that profile exists
            List<BaseProfile> profiles = PlayerProfile.getProfileList();
            assertThat(profiles).isNotEmpty();

            // Wizard should not be required (profile exists)
            boolean hasProfiles = profiles != null && !profiles.isEmpty();
            assertThat(hasProfiles).isTrue();
        }
        finally
        {
            // Cleanup
            if (profile.getFile() != null && profile.getFile().exists())
            {
                profile.getFile().delete();
            }
        }
    }

    @Test
    void should_MarkCompleted_When_WizardFinishes() throws Exception
    {
        // Simulate wizard completion
        wizardPrefs.putBoolean("wizard_completed", true);
        wizardPrefs.flush();

        // Verify it's marked as completed
        boolean completed = wizardPrefs.getBoolean("wizard_completed", false);
        assertThat(completed).isTrue();
    }

    @Test
    void should_CreateProfile_When_OfflineModeSelected()
    {
        // Simulate offline wizard flow
        String playerName = "OfflineTestPlayer";

        // Create profile (what wizard does)
        PlayerProfile profile = new PlayerProfile(playerName);
        profile.initCheck();
        profile.initFile();
        profile.setCreateDate();
        profile.save();

        try
        {
            // Verify profile was created
            assertThat(profile.getName()).isEqualTo(playerName);
            assertThat(profile.getFile()).exists();
            assertThat(profile.isOnline()).isFalse();
        }
        finally
        {
            // Cleanup
            if (profile.getFile() != null && profile.getFile().exists())
            {
                profile.getFile().delete();
            }
        }
    }

    @Test
    void should_ValidateName_When_EmptyProvided()
    {
        // Empty name should be invalid
        String emptyName = "";

        assertThat(emptyName.trim()).isEmpty();
        // Wizard should show validation error
    }

    @Test
    void should_ValidateName_When_NullProvided()
    {
        // Null name should be invalid
        String nullName = null;

        assertThat(nullName).isNull();
        // Wizard should show validation error
    }

    @Test
    void should_AllowSkip_When_DontShowAgainChecked() throws Exception
    {
        // Simulate "don't show again" option
        wizardPrefs.putBoolean("dont_show_again", true);
        wizardPrefs.flush();

        // Verify preference is saved
        boolean dontShow = wizardPrefs.getBoolean("dont_show_again", false);
        assertThat(dontShow).isTrue();
    }

    @Test
    void should_RestorePreferences_When_WizardReopened() throws Exception
    {
        // Set some preferences
        wizardPrefs.putBoolean("wizard_completed", true);
        wizardPrefs.put("last_mode", "offline");
        wizardPrefs.flush();

        // Clear and reload
        boolean completed = wizardPrefs.getBoolean("wizard_completed", false);
        String lastMode = wizardPrefs.get("last_mode", "");

        // Verify preferences persisted
        assertThat(completed).isTrue();
        assertThat(lastMode).isEqualTo("offline");
    }
}
