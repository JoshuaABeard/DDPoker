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

import com.donohoedigital.games.poker.model.OnlineProfile;
import com.donohoedigital.games.poker.service.OnlineProfileService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for PokerServer admin profile initialization logic.
 */
@Tag("slow")
@SpringJUnitConfig(locations = {"/app-context-pokerservertests.xml"})
@Transactional
class PokerServerTest
{
    @Autowired
    private OnlineProfileService profileService;

    private PokerServer pokerServer;

    @AfterEach
    void cleanup()
    {
        // Clear any system properties set during tests
        System.clearProperty("settings.admin.user");
        System.clearProperty("settings.admin.password");
    }

    /**
     * Helper to create a PokerServer instance with injected dependencies for testing
     */
    private PokerServer createPokerServerForTest()
    {
        PokerServer server = new PokerServer();
        // Use reflection to inject the OnlineProfileService since it's package-private
        try
        {
            var field = PokerServer.class.getDeclaredField("onlineProfileService");
            field.setAccessible(true);
            field.set(server, profileService);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to inject OnlineProfileService", e);
        }
        return server;
    }

    @Test
    @Rollback
    void should_CreateAdminProfile_When_ItDoesNotExist()
    {
        // Set admin credentials via system properties
        System.setProperty("settings.admin.user", "newadmin");
        System.setProperty("settings.admin.password", "testpass123");

        // Create PokerServer instance for testing
        pokerServer = createPokerServerForTest();

        // Call initialization
        pokerServer.initializeAdminProfile();

        // Verify profile was created
        OnlineProfile profile = profileService.getOnlineProfileByName("newadmin");
        assertThat(profile).isNotNull();
        assertThat(profile.getName()).isEqualTo("newadmin");
        assertThat(profile.isActivated()).isTrue();
        assertThat(profile.isRetired()).isFalse();
        assertThat(profile.getEmail()).isEqualTo("admin@localhost");

        // Verify password authentication works
        OnlineProfile authProfile = new OnlineProfile();
        authProfile.setName("newadmin");
        authProfile.setPassword("testpass123");
        assertThat(profileService.authenticateOnlineProfile(authProfile)).isNotNull();
    }

    @Test
    @Rollback
    void should_UpdateAdminProfile_When_ItAlreadyExists()
    {
        // Create an existing admin profile
        OnlineProfile existing = PokerTestData.createOnlineProfile("existingadmin");
        existing.setActivated(false);
        existing.setRetired(true);
        profileService.saveOnlineProfile(existing);

        // Set admin credentials via system properties
        System.setProperty("settings.admin.user", "existingadmin");
        System.setProperty("settings.admin.password", "newpass456");

        // Create PokerServer instance for testing
        pokerServer = createPokerServerForTest();

        // Call initialization
        pokerServer.initializeAdminProfile();

        // Verify profile was updated
        OnlineProfile profile = profileService.getOnlineProfileByName("existingadmin");
        assertThat(profile).isNotNull();
        assertThat(profile.isActivated()).isTrue();
        assertThat(profile.isRetired()).isFalse();

        // Verify new password works
        OnlineProfile authProfile = new OnlineProfile();
        authProfile.setName("existingadmin");
        authProfile.setPassword("newpass456");
        assertThat(profileService.authenticateOnlineProfile(authProfile)).isNotNull();

        // Verify old password doesn't work
        authProfile.setPassword("password");
        assertThat(profileService.authenticateOnlineProfile(authProfile)).isNull();
    }

    @Test
    @Rollback
    void should_GeneratePassword_When_NotProvided()
    {
        // Set only admin username, no password
        System.setProperty("settings.admin.user", "autogenadmin");

        // Create PokerServer instance for testing
        pokerServer = createPokerServerForTest();

        // Call initialization
        pokerServer.initializeAdminProfile();

        // Verify profile was created
        OnlineProfile profile = profileService.getOnlineProfileByName("autogenadmin");
        assertThat(profile).isNotNull();
        assertThat(profile.getName()).isEqualTo("autogenadmin");
        assertThat(profile.isActivated()).isTrue();
        assertThat(profile.getPassword()).isNotNull();
        assertThat(profile.getPassword()).isNotEmpty();
    }

    @Test
    @Rollback
    void should_SkipInitialization_When_AdminUsernameNotSet()
    {
        // Ensure no admin user is set
        System.clearProperty("settings.admin.user");

        // Count profiles before
        int countBefore = profileService.getMatchingOnlineProfilesCount(null, null, null, true);

        // Create PokerServer instance for testing
        pokerServer = createPokerServerForTest();

        // Call initialization
        pokerServer.initializeAdminProfile();

        // Verify no profile was created
        int countAfter = profileService.getMatchingOnlineProfilesCount(null, null, null, true);
        assertThat(countAfter).isEqualTo(countBefore);
    }
}
