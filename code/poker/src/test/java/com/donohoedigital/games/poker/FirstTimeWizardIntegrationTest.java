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
package com.donohoedigital.games.poker;

import com.donohoedigital.comms.DMTypedHashMap;
import com.donohoedigital.config.ApplicationType;
import com.donohoedigital.config.ConfigManager;
import com.donohoedigital.config.Prefs;
import com.donohoedigital.games.engine.GameContext;
import com.donohoedigital.games.engine.GameEngine;
import com.donohoedigital.games.engine.ProfileList;
import org.junit.jupiter.api.*;

import java.io.File;
import java.util.prefs.Preferences;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for FirstTimeWizard - Tests full flow with PokerStartMenu integration.
 *
 * These tests verify the wizard works correctly when integrated with the actual
 * application startup flow, including profile creation, preference management,
 * and wizard state persistence.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FirstTimeWizardIntegrationTest {

    private static GameEngine engine;
    private static GameContext context;
    private FirstTimeWizard wizard;
    private Preferences ftuePrefs;
    private Preferences profilePrefs;

    @BeforeAll
    static void setUpClass() {
        // Initialize ConfigManager once for all tests
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);

        // Use null for engine and context in headless tests
        engine = null;
        context = null;
    }

    @BeforeEach
    void setUp() throws Exception {
        // Clear all wizard-related preferences before each test
        ftuePrefs = Prefs.getUserPrefs("ftue");
        ftuePrefs.remove("wizard_completed");
        ftuePrefs.remove("dont_show_again");
        ftuePrefs.flush();

        // Clear profile preferences
        profilePrefs = Prefs.getUserPrefs("player");
        profilePrefs.clear();
        profilePrefs.flush();

        // Delete any existing default profile
        deleteDefaultProfile();

        // Verify preferences are cleared
        assertThat(ftuePrefs.getBoolean("wizard_completed", false)).isFalse();
        assertThat(ftuePrefs.getBoolean("dont_show_again", false)).isFalse();
    }

    @AfterEach
    void tearDown() throws Exception {
        // Clean up after each test
        if (ftuePrefs != null) {
            ftuePrefs.remove("wizard_completed");
            ftuePrefs.remove("dont_show_again");
            ftuePrefs.flush();
        }
        if (profilePrefs != null) {
            profilePrefs.clear();
            profilePrefs.flush();
        }
        deleteDefaultProfile();
    }

    private void deleteDefaultProfile() {
        // Delete the default profile file if it exists
        String userHome = System.getProperty("user.home");
        File profileDir = new File(userHome, ".ddpoker/profiles");
        if (profileDir.exists()) {
            File[] files = profileDir.listFiles((dir, name) -> name.endsWith(".profile"));
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
        }
    }

    // =================================================================
    // Integration Test 1: First-Time User Flow - Offline Mode
    // =================================================================

    @Test
    @Order(1)
    void should_LaunchWizard_When_NoProfileExists() {
        // Given: Wizard hasn't been completed
        assertThat(FirstTimeWizard.shouldShowWizard()).isTrue();

        // When: Wizard is initialized
        wizard = new FirstTimeWizard();
        wizard.init(engine, context, new DMTypedHashMap());

        // Then: Wizard should be initialized and ready
        assertThat(wizard.isWizardInitialized()).isTrue();
        assertThat(wizard.getCurrentStepType()).isEqualTo(FirstTimeWizard.STEP_TYPE_PLAY_MODE);
    }

    @Test
    @Order(2)
    void should_CreateLocalProfile_When_CompletingOfflineWizard() {
        // Given: Fresh wizard in offline mode
        wizard = new FirstTimeWizard();
        wizard.init(engine, context, new DMTypedHashMap());

        // When: User completes offline wizard flow
        wizard.selectPlayMode(FirstTimeWizard.MODE_OFFLINE);
        wizard.nextStep(); // Move to profile creation
        wizard.setPlayerName("IntegrationTestUser");

        // Then: Should be on profile creation step
        assertThat(wizard.getCurrentStepType()).isEqualTo(FirstTimeWizard.STEP_TYPE_PROFILE);
        assertThat(wizard.isServerConfigRequired()).isFalse();

        // When: Complete wizard
        PlayerProfile profile = wizard.completeWizard();

        // Then: Profile should be created as local profile
        assertThat(profile).isNotNull();
        assertThat(profile.getName()).isEqualTo("IntegrationTestUser");
        assertThat(profile.isOnline()).isFalse();
        assertThat(profile.getEmail()).isNullOrEmpty();
        assertThat(wizard.isWizardComplete()).isTrue();
    }

    @Test
    @Order(3)
    void should_SaveWizardCompletion_When_WizardFinished() {
        // Given: Wizard is completed
        wizard = new FirstTimeWizard();
        wizard.init(engine, context, new DMTypedHashMap());
        wizard.selectPlayMode(FirstTimeWizard.MODE_OFFLINE);
        wizard.setPlayerName("TestUser");
        wizard.completeWizard();

        // When: Wizard completion is saved
        wizard.saveCompletionPreference(ftuePrefs);

        // Then: Preference should be persisted
        assertThat(ftuePrefs.getBoolean("wizard_completed", false)).isTrue();
        assertThat(FirstTimeWizard.shouldShowWizard()).isFalse();
    }

    @Test
    @Order(4)
    void should_NotShowWizard_When_AlreadyCompleted() {
        // Given: Wizard was previously completed
        ftuePrefs.putBoolean("wizard_completed", true);

        // Then: Wizard should not show
        assertThat(FirstTimeWizard.shouldShowWizard()).isFalse();
    }

    // =================================================================
    // Integration Test 2: Online Profile Flow
    // =================================================================

    @Test
    @Order(5)
    void should_RequireServerConfig_When_OnlineModeSelected() {
        // Given: Wizard in online new profile mode
        wizard = new FirstTimeWizard();
        wizard.init(engine, context, new DMTypedHashMap());

        // When: User selects online new profile mode
        wizard.selectPlayMode(FirstTimeWizard.MODE_ONLINE_NEW);
        wizard.nextStep();

        // Then: Should show server config BEFORE profile creation
        assertThat(wizard.getCurrentStepType()).isEqualTo(FirstTimeWizard.STEP_TYPE_SERVER_CONFIG);
        assertThat(wizard.isServerConfigRequired()).isTrue();
    }

    @Test
    @Order(6)
    void should_MoveToProfileCreation_When_ServerConfigured() {
        // Given: Wizard with server configured
        wizard = new FirstTimeWizard();
        wizard.init(engine, context, new DMTypedHashMap());
        wizard.selectPlayMode(FirstTimeWizard.MODE_ONLINE_NEW);
        wizard.nextStep(); // To server config

        // When: Server is configured
        wizard.setGameServer("localhost:8877");
        wizard.setChatServer("localhost:11886");
        wizard.setServerConfigComplete(true);
        wizard.nextStep();

        // Then: Should move to profile creation
        assertThat(wizard.getCurrentStepType()).isEqualTo(FirstTimeWizard.STEP_TYPE_PROFILE);
    }

    @Test
    @Order(7)
    void should_CreateOnlineProfile_When_CompletingOnlineWizard() {
        // Given: Wizard in online mode with server configured
        wizard = new FirstTimeWizard();
        wizard.init(engine, context, new DMTypedHashMap());
        wizard.selectPlayMode(FirstTimeWizard.MODE_ONLINE_NEW);
        wizard.nextStep();
        wizard.setGameServer("localhost:8877");
        wizard.setServerConfigComplete(true);
        wizard.nextStep();

        // When: User enters profile information
        wizard.setPlayerName("OnlineUser");
        wizard.setPlayerEmail("test@example.com");
        wizard.nextStep(); // Would trigger profile creation

        // Then: Should move to email sent step
        assertThat(wizard.getCurrentStepType()).isEqualTo(FirstTimeWizard.STEP_TYPE_EMAIL_SENT);
    }

    // =================================================================
    // Integration Test 3: Link Existing Profile Flow
    // =================================================================

    @Test
    @Order(8)
    void should_ShowLinkPanel_When_LinkModeSelected() {
        // Given: Wizard in link existing profile mode
        wizard = new FirstTimeWizard();
        wizard.init(engine, context, new DMTypedHashMap());

        // When: User selects link existing mode
        wizard.selectPlayMode(FirstTimeWizard.MODE_ONLINE_LINK);
        wizard.nextStep(); // To server config
        wizard.setGameServer("localhost:8877");
        wizard.setServerConfigComplete(true);
        wizard.nextStep();

        // Then: Should show link profile panel (not new profile)
        assertThat(wizard.getCurrentStepType()).isEqualTo(FirstTimeWizard.STEP_TYPE_LINK_PROFILE);
    }

    @Test
    @Order(9)
    void should_AuthenticateProfile_When_LinkingExisting() {
        // Given: Wizard in link mode with server configured
        wizard = new FirstTimeWizard();
        wizard.init(engine, context, new DMTypedHashMap());
        wizard.selectPlayMode(FirstTimeWizard.MODE_ONLINE_LINK);
        wizard.nextStep();
        wizard.setServerConfigComplete(true);
        wizard.nextStep();

        // When: User provides credentials
        wizard.setPlayerName("ExistingUser");
        wizard.setPlayerPassword("password123");
        wizard.linkExistingProfile();

        // Then: Profile should be marked as linked
        assertThat(wizard.isProfileLinked()).isTrue();
    }

    // =================================================================
    // Integration Test 4: Validation and Navigation
    // =================================================================

    @Test
    @Order(10)
    void should_PreventNavigation_When_ValidationFails() {
        // Given: Wizard on profile creation step
        wizard = new FirstTimeWizard();
        wizard.init(engine, context, new DMTypedHashMap());
        wizard.selectPlayMode(FirstTimeWizard.MODE_OFFLINE);
        wizard.nextStep();

        // When: User tries to proceed without entering name
        wizard.setPlayerName(""); // Empty name
        boolean isValid = wizard.validateProfileName();

        // Then: Validation should fail
        assertThat(isValid).isFalse();
        assertThat(wizard.getValidationError()).isNotEmpty();
    }

    @Test
    @Order(11)
    void should_AllowNavigation_When_ValidationPasses() {
        // Given: Wizard on profile creation step
        wizard = new FirstTimeWizard();
        wizard.init(engine, context, new DMTypedHashMap());
        wizard.selectPlayMode(FirstTimeWizard.MODE_OFFLINE);
        wizard.nextStep();

        // When: User enters valid name
        wizard.setPlayerName("ValidUser");
        boolean isValid = wizard.validateProfileName();

        // Then: Validation should pass
        assertThat(isValid).isTrue();
        assertThat(wizard.getValidationError()).isNullOrEmpty();
    }

    @Test
    @Order(12)
    void should_NavigateBackward_When_BackPressed() {
        // Given: Wizard on profile creation step
        wizard = new FirstTimeWizard();
        wizard.init(engine, context, new DMTypedHashMap());
        wizard.selectPlayMode(FirstTimeWizard.MODE_OFFLINE);
        wizard.nextStep();

        assertThat(wizard.getCurrentStepType()).isEqualTo(FirstTimeWizard.STEP_TYPE_PROFILE);

        // When: User goes back
        wizard.previousStep();

        // Then: Should return to play mode selection
        assertThat(wizard.getCurrentStepType()).isEqualTo(FirstTimeWizard.STEP_TYPE_PLAY_MODE);
    }

    @Test
    @Order(13)
    void should_PreserveData_When_NavigatingBackAndForth() {
        // Given: Wizard with some data entered
        wizard = new FirstTimeWizard();
        wizard.init(engine, context, new DMTypedHashMap());
        wizard.selectPlayMode(FirstTimeWizard.MODE_ONLINE_NEW);
        wizard.nextStep();

        String server = "test.server.com:8877";
        wizard.setGameServer(server);

        // When: Navigate back and forth
        wizard.previousStep();
        wizard.nextStep();

        // Then: Data should be preserved
        assertThat(wizard.getGameServer()).isEqualTo(server);
    }

    // =================================================================
    // Integration Test 5: Skip and Don't Show Again
    // =================================================================

    @Test
    @Order(14)
    void should_CreateDefaultProfile_When_Skipped() {
        // Given: Fresh wizard
        wizard = new FirstTimeWizard();
        wizard.init(engine, context, new DMTypedHashMap());

        // When: User skips wizard
        PlayerProfile profile = wizard.skipWizard();

        // Then: Default profile should be created
        assertThat(profile).isNotNull();
        assertThat(profile.getName()).isNotEmpty();
        assertThat(profile.isOnline()).isFalse();
        assertThat(wizard.wasWizardSkipped()).isTrue();
    }

    @Test
    @Order(15)
    void should_SaveDontShowPreference_When_Set() {
        // Given: Wizard with don't show again enabled
        wizard = new FirstTimeWizard();
        wizard.init(engine, context, new DMTypedHashMap());

        // When: Don't show again is set and saved
        wizard.setDontShowAgain(true);
        wizard.saveDontShowPreference(ftuePrefs);

        // Then: Preference should be saved
        assertThat(ftuePrefs.getBoolean("dont_show_again", false)).isTrue();
        assertThat(FirstTimeWizard.shouldShowWizard()).isFalse();
    }

    // =================================================================
    // Integration Test 6: Email and Password Validation
    // =================================================================

    @Test
    @Order(16)
    void should_ValidateEmailFormat_When_Online() {
        // Given: Wizard in online mode
        wizard = new FirstTimeWizard();
        wizard.init(engine, context, new DMTypedHashMap());
        wizard.selectPlayMode(FirstTimeWizard.MODE_ONLINE_NEW);

        // When: Invalid email is entered
        wizard.setPlayerEmail("notanemail");
        boolean isValid = wizard.validateEmail();

        // Then: Validation should fail
        assertThat(isValid).isFalse();

        // When: Valid email is entered
        wizard.setPlayerEmail("user@example.com");
        isValid = wizard.validateEmail();

        // Then: Validation should pass
        assertThat(isValid).isTrue();
    }

    @Test
    @Order(17)
    void should_ValidateServerAddress_When_ConfiguringServer() {
        // Given: Wizard configuring server
        wizard = new FirstTimeWizard();
        wizard.init(engine, context, new DMTypedHashMap());

        // When: Invalid server address
        wizard.setGameServer("invalid-server");
        boolean isValid = wizard.validateServerAddress();

        // Then: Validation should fail
        assertThat(isValid).isFalse();

        // When: Valid server address
        wizard.setGameServer("localhost:8877");
        isValid = wizard.validateServerAddress();

        // Then: Validation should pass
        assertThat(isValid).isTrue();
    }

    // =================================================================
    // Integration Test 7: Complete End-to-End Flow
    // =================================================================

    @Test
    @Order(18)
    void should_CompleteFullOfflineFlow_When_UserFollowsAllSteps() {
        // Given: Brand new user with no profile
        assertThat(FirstTimeWizard.shouldShowWizard()).isTrue();

        // When: User goes through complete offline wizard
        wizard = new FirstTimeWizard();
        wizard.init(engine, context, new DMTypedHashMap());

        // Step 1: Select offline mode
        assertThat(wizard.getCurrentStepType()).isEqualTo(FirstTimeWizard.STEP_TYPE_PLAY_MODE);
        wizard.selectPlayMode(FirstTimeWizard.MODE_OFFLINE);
        wizard.nextStep();

        // Step 2: Enter profile name
        assertThat(wizard.getCurrentStepType()).isEqualTo(FirstTimeWizard.STEP_TYPE_PROFILE);
        wizard.setPlayerName("EndToEndUser");
        wizard.nextStep();

        // Step 3: Move to complete step
        wizard.nextStep();
        assertThat(wizard.getCurrentStepType()).isEqualTo(FirstTimeWizard.STEP_TYPE_COMPLETE);

        // Complete wizard
        PlayerProfile profile = wizard.completeWizard();
        wizard.saveCompletionPreference(ftuePrefs);

        // Then: Everything should be set up correctly
        assertThat(profile).isNotNull();
        assertThat(profile.getName()).isEqualTo("EndToEndUser");
        assertThat(profile.isOnline()).isFalse();
        assertThat(wizard.isWizardComplete()).isTrue();
        assertThat(FirstTimeWizard.shouldShowWizard()).isFalse();
    }

    @Test
    @Order(19)
    void should_CompleteFullOnlineFlow_When_UserFollowsAllSteps() {
        // Given: Brand new user with no profile
        assertThat(FirstTimeWizard.shouldShowWizard()).isTrue();

        // When: User goes through complete online wizard
        wizard = new FirstTimeWizard();
        wizard.init(engine, context, new DMTypedHashMap());

        // Step 1: Select online new mode
        assertThat(wizard.getCurrentStepType()).isEqualTo(FirstTimeWizard.STEP_TYPE_PLAY_MODE);
        wizard.selectPlayMode(FirstTimeWizard.MODE_ONLINE_NEW);
        wizard.nextStep();

        // Step 2: Configure server
        assertThat(wizard.getCurrentStepType()).isEqualTo(FirstTimeWizard.STEP_TYPE_SERVER_CONFIG);
        wizard.setGameServer("localhost:8877");
        wizard.setChatServer("localhost:11886");
        wizard.setServerConfigComplete(true);
        wizard.nextStep();

        // Step 3: Enter profile details
        assertThat(wizard.getCurrentStepType()).isEqualTo(FirstTimeWizard.STEP_TYPE_PROFILE);
        wizard.setPlayerName("OnlineEndToEndUser");
        wizard.setPlayerEmail("test@example.com");
        wizard.createOnlineProfile();

        // Step 4: Email sent step
        assertThat(wizard.getCurrentStepType()).isEqualTo(FirstTimeWizard.STEP_TYPE_EMAIL_SENT);
        wizard.setReceivedPassword("testpass123");
        wizard.nextStep();

        // Step 5: Password entry step (would validate with server in real scenario)
        wizard.nextStep();

        // Step 6: Complete
        assertThat(wizard.getCurrentStepType()).isEqualTo(FirstTimeWizard.STEP_TYPE_COMPLETE);

        // Complete wizard
        PlayerProfile profile = wizard.completeWizard();

        // Then: Wizard flow completed successfully
        assertThat(profile).isNotNull();
        assertThat(profile.getName()).isEqualTo("OnlineEndToEndUser");
        assertThat(profile.getEmail()).isEqualTo("test@example.com");
        assertThat(wizard.isWizardComplete()).isTrue();
    }
}
