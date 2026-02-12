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

import com.donohoedigital.config.ApplicationType;
import com.donohoedigital.config.ConfigManager;
import com.donohoedigital.config.PropertyConfig;
import com.donohoedigital.games.poker.model.OnlineProfile;
import com.donohoedigital.games.poker.service.OnlineProfileService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
class PokerServerTest {
    @Autowired
    private OnlineProfileService profileService;

    private PokerServer pokerServer;

    @BeforeEach
    void setUp() {
        // Initialize ConfigManager for tests (only once)
        if (!PropertyConfig.isInitialized()) {
            new ConfigManager("poker", ApplicationType.SERVER);
        }
    }

    @AfterEach
    void cleanup() {
        // Clear any system properties set during tests
        System.clearProperty("settings.admin.user");
        System.clearProperty("settings.admin.password");
    }

    /**
     * Helper to create a PokerServer instance with injected dependencies for
     * testing
     */
    private PokerServer createPokerServerForTest() {
        PokerServer server = new PokerServer();
        // Use reflection to inject the OnlineProfileService since it's package-private
        try {
            var field = PokerServer.class.getDeclaredField("onlineProfileService");
            field.setAccessible(true);
            field.set(server, profileService);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject OnlineProfileService", e);
        }
        return server;
    }

    @Test
    @Rollback
    void should_CreateAdminProfile_When_ItDoesNotExist() {
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
    void should_UpdateAdminProfile_When_ItAlreadyExists() {
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
    void should_GeneratePassword_When_NotProvided() {
        // Set only admin username, no password
        System.setProperty("settings.admin.user", "autogenadmin");

        // Create PokerServer instance for testing
        pokerServer = createPokerServerForTest();

        // Call initialization
        pokerServer.initializeAdminProfile();

        // Verify profile was created with a hashed password
        OnlineProfile profile = profileService.getOnlineProfileByName("autogenadmin");
        assertThat(profile).isNotNull();
        assertThat(profile.getName()).isEqualTo("autogenadmin");
        assertThat(profile.isActivated()).isTrue();
        assertThat(profile.getPasswordHash()).isNotNull();
        assertThat(profile.getPasswordHash()).startsWith("$2a$");
    }

    @Test
    @Rollback
    void should_CreateDefaultAdmin_When_AdminUsernameNotSet() {
        // Ensure no admin user is set (should default to "admin")
        System.clearProperty("settings.admin.user");
        System.clearProperty("settings.admin.password");

        // Create PokerServer instance for testing
        pokerServer = createPokerServerForTest();

        // Call initialization
        pokerServer.initializeAdminProfile();

        // Verify default "admin" profile was created with hashed password
        OnlineProfile profile = profileService.getOnlineProfileByName("admin");
        assertThat(profile).isNotNull();
        assertThat(profile.getName()).isEqualTo("admin");
        assertThat(profile.isActivated()).isTrue();
        assertThat(profile.isRetired()).isFalse();
        assertThat(profile.getPasswordHash()).isNotNull();
        assertThat(profile.getPasswordHash()).startsWith("$2a$");
    }

    @Test
    @Rollback
    void should_KeepExistingPassword_When_ProfileExistsAndPasswordNotProvided() {
        // Create an existing admin profile with a specific hashed password
        OnlineProfile existing = PokerTestData.createOnlineProfile("keepadmin");
        profileService.hashAndSetPassword(existing, "existingpass123");
        existing.setActivated(true);
        existing.setRetired(false);
        profileService.saveOnlineProfile(existing);

        // Write password file so initializeAdminProfile can read it
        String workDir = System.getenv("WORK");
        if (workDir == null)
            workDir = "/data";
        try {
            java.nio.file.Path dirPath = java.nio.file.Paths.get(workDir);
            java.nio.file.Files.createDirectories(dirPath);
            java.nio.file.Path filePath = dirPath.resolve("admin-password.txt");
            java.nio.file.Files.writeString(filePath, "existingpass123", java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write password file for test", e);
        }

        // Set only admin username, no password (should keep existing password)
        System.setProperty("settings.admin.user", "keepadmin");
        System.clearProperty("settings.admin.password");

        // Create PokerServer instance for testing
        pokerServer = createPokerServerForTest();

        // Call initialization
        pokerServer.initializeAdminProfile();

        // Verify profile still exists with original password
        OnlineProfile profile = profileService.getOnlineProfileByName("keepadmin");
        assertThat(profile).isNotNull();
        assertThat(profile.getName()).isEqualTo("keepadmin");
        assertThat(profile.isActivated()).isTrue();
        assertThat(profile.isRetired()).isFalse();

        // Verify original password still works
        OnlineProfile authProfile = new OnlineProfile();
        authProfile.setName("keepadmin");
        authProfile.setPassword("existingpass123");
        assertThat(profileService.authenticateOnlineProfile(authProfile)).isNotNull();
    }
}
