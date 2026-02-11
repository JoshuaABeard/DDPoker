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
package com.donohoedigital.games.poker.ui;

import com.donohoedigital.config.Prefs;
import com.donohoedigital.games.config.BaseProfile;
import com.donohoedigital.games.engine.ProfileList;
import com.donohoedigital.games.poker.PlayerProfile;
import com.donohoedigital.games.poker.PlayerProfileOptions;

import java.io.File;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Utility class for managing player profiles and preferences in UI tests.
 * Provides methods for test setup/teardown to ensure proper test isolation.
 */
public class TestProfileHelper
{
    private static final String FTUE_PREFS_NODE = "ftue";
    private static final String WIZARD_COMPLETED_KEY = "wizard_completed";
    private static final String DONT_SHOW_AGAIN_KEY = "dont_show_again";

    /**
     * Ensures a default profile exists with the given name.
     * This prevents the FirstTimeWizard from blocking non-wizard tests.
     * If a profile with this name already exists, it is used; otherwise, a new one is created.
     *
     * @param name The name of the profile to create/ensure exists
     * @return The profile that was ensured to exist
     */
    public static PlayerProfile ensureDefaultProfileExists(String name)
    {
        // Check if profile already exists
        List<BaseProfile> profiles = PlayerProfile.getProfileList();
        for (BaseProfile profile : profiles)
        {
            if (profile.getName().equals(name))
            {
                // Profile exists, set it as the default
                PlayerProfile playerProfile = (PlayerProfile) profile;
                ProfileList.setStoredProfile(playerProfile, PlayerProfileOptions.PROFILE_NAME);
                return playerProfile;
            }
        }

        // Profile doesn't exist, create it
        PlayerProfile profile = new PlayerProfile(name);
        profile.initCheck();
        profile.initFile();
        profile.setCreateDate();
        profile.save();

        // Set as default profile
        ProfileList.setStoredProfile(profile, PlayerProfileOptions.PROFILE_NAME);

        return profile;
    }

    /**
     * Clears all player profiles from the filesystem.
     * Used to ensure wizard tests start with a clean slate (no existing profiles).
     * WARNING: This deletes profile files from disk. Only use in test teardown/setup.
     */
    public static void clearAllProfiles()
    {
        List<BaseProfile> profiles = PlayerProfile.getProfileList();
        if (profiles == null) return;

        for (BaseProfile profile : profiles)
        {
            File file = profile.getFile();
            if (file != null && file.exists())
            {
                file.delete();
            }
        }

        // Clear the stored profile preference
        ProfileList.setStoredProfile(null, PlayerProfileOptions.PROFILE_NAME);
    }

    /**
     * Clears wizard-related preferences to force the wizard to display.
     * This resets the "wizard_completed" and "dont_show_again" flags.
     * Used in wizard tests to ensure the wizard appears on application launch.
     */
    public static void clearWizardPreferences()
    {
        try
        {
            Preferences ftuePrefs = Prefs.getUserPrefs(FTUE_PREFS_NODE);
            ftuePrefs.remove(WIZARD_COMPLETED_KEY);
            ftuePrefs.remove(DONT_SHOW_AGAIN_KEY);
            ftuePrefs.flush();
        }
        catch (Exception e)
        {
            System.err.println("Failed to clear wizard preferences: " + e.getMessage());
        }
    }

    /**
     * Sets the wizard as completed in preferences.
     * Useful for non-wizard tests that need to skip the wizard entirely.
     */
    public static void markWizardCompleted()
    {
        try
        {
            Preferences ftuePrefs = Prefs.getUserPrefs(FTUE_PREFS_NODE);
            ftuePrefs.putBoolean(WIZARD_COMPLETED_KEY, true);
            ftuePrefs.flush();
        }
        catch (Exception e)
        {
            System.err.println("Failed to mark wizard as completed: " + e.getMessage());
        }
    }

    /**
     * Complete test setup: creates a default profile and marks wizard as completed.
     * This is the standard setup for non-wizard UI tests.
     *
     * @param profileName The name of the test profile to create
     * @return The created profile
     */
    public static PlayerProfile setupForNonWizardTests(String profileName)
    {
        markWizardCompleted();
        return ensureDefaultProfileExists(profileName);
    }

    /**
     * Complete test setup for wizard tests: clears all profiles and wizard preferences.
     * This ensures the wizard will appear on application launch.
     */
    public static void setupForWizardTests()
    {
        clearAllProfiles();
        clearWizardPreferences();
    }

    /**
     * Gets the count of existing profiles.
     * Useful for assertions in tests.
     *
     * @return Number of existing profiles
     */
    public static int getProfileCount()
    {
        List<BaseProfile> profiles = PlayerProfile.getProfileList();
        return profiles == null ? 0 : profiles.size();
    }
}
