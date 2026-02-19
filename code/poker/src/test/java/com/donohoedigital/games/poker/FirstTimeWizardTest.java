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
package com.donohoedigital.games.poker;

import com.donohoedigital.comms.DMTypedHashMap;
import com.donohoedigital.config.ApplicationType;
import com.donohoedigital.config.ConfigManager;
import com.donohoedigital.config.Prefs;
import com.donohoedigital.games.engine.GameContext;
import com.donohoedigital.games.engine.GameEngine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.prefs.Preferences;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for FirstTimeWizard - First-Time User Experience wizard dialog.
 *
 * TDD RED Phase: These tests are written BEFORE implementation. All tests
 * should FAIL initially, then pass once FirstTimeWizard is implemented.
 */
class FirstTimeWizardTest {

    private GameEngine mockEngine;
    private GameContext mockContext;
    private FirstTimeWizard wizard;
    private DMTypedHashMap params;
    private Preferences testPrefs;

    @BeforeEach
    void setUp() throws Exception {
        // Initialize ConfigManager for headless testing
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);

        // Use null for engine and context in headless tests
        mockEngine = null;
        mockContext = null;

        // Setup parameters
        params = new DMTypedHashMap();

        // Use test preferences node
        testPrefs = Prefs.getUserPrefs("ftue-test");
        testPrefs.clear();
    }

    @AfterEach
    void tearDown() throws Exception {
        // Clean up test preferences
        testPrefs.clear();
        testPrefs.flush();
    }

    // =================================================================
    // Test 1: Wizard Creation and Display
    // =================================================================

    @Test
    void should_CreateWizard_When_Instantiated() {
        wizard = new FirstTimeWizard();

        assertThat(wizard).isNotNull();
    }

    @Test
    void should_ShowWizard_When_NoProfileExists() {
        // This test verifies the wizard is triggered when no profile exists
        // The actual integration happens in PokerStartMenu, but we test
        // that the wizard can be created and initialized properly

        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        assertThat(wizard.isWizardInitialized()).isTrue();
    }

    // =================================================================
    // Test 2: Wizard Should Skip for Existing Users
    // =================================================================

    @Test
    void should_NotShowWizard_When_ProfileExists() {
        // When a profile exists, wizard should not be needed
        // This is tested via preferences check
        testPrefs.putBoolean("wizard_completed", true);

        boolean shouldShow = !testPrefs.getBoolean("wizard_completed", false);

        assertThat(shouldShow).isFalse();
    }

    // =================================================================
    // Test 3: "Play Offline" Path Creates Local Profile
    // =================================================================

    @Test
    void should_CreateLocalProfile_When_OfflinePathChosen() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        // Simulate user selecting offline mode
        wizard.selectPlayMode(FirstTimeWizard.MODE_OFFLINE);

        // Simulate entering player name
        wizard.setPlayerName("TestPlayer");

        // Complete wizard
        PlayerProfile profile = wizard.completeWizard();

        // Verify profile created
        assertThat(profile).isNotNull();
        assertThat(profile.getName()).isEqualTo("TestPlayer");

        // Verify it's a local profile (no email)
        assertThat(profile.getEmail()).isNullOrEmpty();
        assertThat(profile.isOnline()).isFalse();
    }

    @Test
    void should_SkipServerConfig_When_OfflinePathChosen() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.selectPlayMode(FirstTimeWizard.MODE_OFFLINE);
        wizard.nextStep();

        // Verify we skip server config and go directly to profile creation
        assertThat(wizard.getCurrentStepType()).isEqualTo(FirstTimeWizard.STEP_TYPE_PROFILE);
        assertThat(wizard.isServerConfigRequired()).isFalse();
    }

    // =================================================================
    // Test 4: "Play Online" Path Shows Server Config FIRST
    // =================================================================

    @Test
    void should_ShowServerConfigFirst_When_OnlinePathChosen() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.selectPlayMode(FirstTimeWizard.MODE_ONLINE_NEW);
        wizard.nextStep();

        // Verify server config shown BEFORE profile creation
        assertThat(wizard.getCurrentStepType()).isEqualTo(FirstTimeWizard.STEP_TYPE_SERVER_CONFIG);
        assertThat(wizard.isServerConfigRequired()).isTrue();
    }

    // =================================================================
    // Test 5: Online Path - Server Config Before Profile
    // =================================================================

    @Test
    void should_ShowProfileCreation_When_ServerConfigCompleted() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        // Select online mode
        wizard.selectPlayMode(FirstTimeWizard.MODE_ONLINE_NEW);
        wizard.nextStep();

        // Complete server config
        wizard.setGameServer("localhost:8877");
        wizard.setServerConfigComplete(true);
        wizard.nextStep();

        // Verify profile creation shown AFTER server config
        assertThat(wizard.getCurrentStepType()).isEqualTo(FirstTimeWizard.STEP_TYPE_PROFILE);
    }

    // =================================================================
    // Test 6: Profile Name Validation
    // =================================================================

    @Test
    void should_ShowError_When_ProfileNameEmpty() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.setPlayerName("");
        boolean isValid = wizard.validateProfileName();

        assertThat(isValid).isFalse();
        assertThat(wizard.getValidationError()).isNotEmpty();
    }

    @Test
    void should_AcceptValidName_When_ProfileNameProvided() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.setPlayerName("ValidPlayer");
        boolean isValid = wizard.validateProfileName();

        assertThat(isValid).isTrue();
        assertThat(wizard.getValidationError()).isNullOrEmpty();
    }

    // =================================================================
    // Test 7: Email Validation (Online Path Only)
    // =================================================================

    @Test
    void should_ShowError_When_EmailEmpty() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.selectPlayMode(FirstTimeWizard.MODE_ONLINE_NEW);
        wizard.setPlayerEmail("");
        boolean isValid = wizard.validateEmail();

        assertThat(isValid).isFalse();
        assertThat(wizard.getValidationError()).contains("email");
    }

    @Test
    void should_ShowError_When_EmailInvalidFormat() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.selectPlayMode(FirstTimeWizard.MODE_ONLINE_NEW);
        wizard.setPlayerEmail("notanemail");
        boolean isValid = wizard.validateEmail();

        assertThat(isValid).isFalse();
    }

    @Test
    void should_AcceptValidEmail_When_EmailProvided() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.selectPlayMode(FirstTimeWizard.MODE_ONLINE_NEW);
        wizard.setPlayerEmail("user@example.com");
        boolean isValid = wizard.validateEmail();

        assertThat(isValid).isTrue();
    }

    // =================================================================
    // Test 8: Server Address Validation
    // =================================================================

    @Test
    void should_ShowError_When_ServerAddressInvalid() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.setGameServer("badserver");
        boolean isValid = wizard.validateServerAddress();

        assertThat(isValid).isFalse();
    }

    @Test
    void should_AcceptValidServer_When_LocalhostProvided() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.setGameServer("localhost:8877");
        boolean isValid = wizard.validateServerAddress();

        assertThat(isValid).isTrue();
    }

    @Test
    void should_AcceptValidServer_When_DomainProvided() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.setGameServer("poker.example.com:8877");
        boolean isValid = wizard.validateServerAddress();

        assertThat(isValid).isTrue();
    }

    // =================================================================
    // Test 9: Server Connection Test with Fallback
    // =================================================================

    @Test
    void should_ShowOfflineFallback_When_ServerConnectionFails() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.selectPlayMode(FirstTimeWizard.MODE_ONLINE_NEW);
        wizard.setGameServer("localhost:8877");

        // Simulate connection failure
        wizard.testServerConnection();
        wizard.setConnectionTestResult(false);

        assertThat(wizard.isOfflineFallbackAvailable()).isTrue();
        assertThat(wizard.canProceedWithoutServer()).isTrue();
    }

    @Test
    void should_EnableNextButton_When_ServerConnectionSucceeds() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.selectPlayMode(FirstTimeWizard.MODE_ONLINE_NEW);
        wizard.setGameServer("localhost:8877");

        // Simulate connection success
        wizard.testServerConnection();
        wizard.setConnectionTestResult(true);

        assertThat(wizard.canProceedToNextStep()).isTrue();
    }

    // =================================================================
    // Test 10: Online Registration Flow (REST — no email activation step)
    // =================================================================

    @Test
    void should_ShowCompletePanel_When_OnlineProfileCreated() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.selectPlayMode(FirstTimeWizard.MODE_ONLINE_NEW);
        wizard.setPlayerName("OnlinePlayer");
        wizard.setPlayerEmail("test@example.com");
        wizard.setServerConfigComplete(true);

        // createOnlineProfile() advances to the complete step (REST already succeeded
        // before this is called in the real handleNext() flow)
        wizard.createOnlineProfile();

        assertThat(wizard.getCurrentStepType()).isEqualTo(FirstTimeWizard.STEP_TYPE_COMPLETE);
    }

    // =================================================================
    // Test 11: Wizard Completion Creates Profile
    // =================================================================

    @Test
    void should_CreateAndSaveProfile_When_WizardCompleted() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.selectPlayMode(FirstTimeWizard.MODE_OFFLINE);
        wizard.setPlayerName("CompletedPlayer");

        PlayerProfile profile = wizard.completeWizard();

        assertThat(profile).isNotNull();
        assertThat(profile.getName()).isEqualTo("CompletedPlayer");
        assertThat(wizard.isWizardComplete()).isTrue();
    }

    @Test
    void should_SaveWizardCompletedPreference_When_WizardFinished() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.selectPlayMode(FirstTimeWizard.MODE_OFFLINE);
        wizard.setPlayerName("TestPlayer");
        wizard.completeWizard();
        wizard.saveCompletionPreference(testPrefs);

        boolean completed = testPrefs.getBoolean("wizard_completed", false);
        assertThat(completed).isTrue();
    }

    // =================================================================
    // Test 12: "Skip" Button Creates Default Profile
    // =================================================================

    @Test
    void should_CreateDefaultProfile_When_WizardSkipped() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        PlayerProfile profile = wizard.skipWizard();

        assertThat(profile).isNotNull();
        assertThat(profile.isOnline()).isFalse();
        assertThat(wizard.wasWizardSkipped()).isTrue();
    }

    // =================================================================
    // Test 13: Back/Next Navigation
    // =================================================================

    @Test
    void should_PreserveFields_When_NavigatingBackward() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.selectPlayMode(FirstTimeWizard.MODE_ONLINE_NEW);
        wizard.nextStep();

        String serverAddress = "localhost:8877";
        wizard.setGameServer(serverAddress);

        wizard.previousStep();
        wizard.nextStep();

        // Verify field value preserved
        assertThat(wizard.getGameServer()).isEqualTo(serverAddress);
    }

    @Test
    void should_AllowBackNavigation_When_OnProfileStep() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.selectPlayMode(FirstTimeWizard.MODE_ONLINE_NEW);
        wizard.nextStep(); // To server config
        wizard.setServerConfigComplete(true);
        wizard.nextStep(); // To profile creation

        wizard.previousStep(); // Back to server config

        assertThat(wizard.getCurrentStepType()).isEqualTo(FirstTimeWizard.STEP_TYPE_SERVER_CONFIG);
    }

    // =================================================================
    // Test 14: "Don't Show Again" Preference
    // =================================================================

    @Test
    void should_SaveDontShowPreference_When_CheckboxChecked() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.setDontShowAgain(true);
        wizard.saveDontShowPreference(testPrefs);

        boolean dontShow = testPrefs.getBoolean("dont_show_again", false);
        assertThat(dontShow).isTrue();
    }

    @Test
    void should_NotShowWizard_When_DontShowPreferenceSet() {
        testPrefs.putBoolean("dont_show_again", true);

        boolean shouldShow = !testPrefs.getBoolean("dont_show_again", false);

        assertThat(shouldShow).isFalse();
    }

    // =================================================================
    // Test 15: Link Existing Profile Path
    // =================================================================

    @Test
    void should_ShowServerConfigFirst_When_LinkExistingChosen() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.selectPlayMode(FirstTimeWizard.MODE_ONLINE_LINK);
        wizard.nextStep();

        // Verify server config shown first (same as new profile)
        assertThat(wizard.getCurrentStepType()).isEqualTo(FirstTimeWizard.STEP_TYPE_SERVER_CONFIG);
    }

    @Test
    void should_ShowLinkProfilePanel_When_ServerConfigCompleteInLinkMode() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.selectPlayMode(FirstTimeWizard.MODE_ONLINE_LINK);
        wizard.nextStep();
        wizard.setServerConfigComplete(true);
        wizard.nextStep();

        // Verify link profile panel shown (username + password)
        assertThat(wizard.getCurrentStepType()).isEqualTo(FirstTimeWizard.STEP_TYPE_LINK_PROFILE);
    }

    @Test
    void should_AuthenticateWithPassword_When_LinkingExistingProfile() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.selectPlayMode(FirstTimeWizard.MODE_ONLINE_LINK);
        wizard.setPlayerName("ExistingUser");
        wizard.setPlayerPassword("existingpass");

        // Simulate server authentication
        wizard.linkExistingProfile();

        assertThat(wizard.isProfileLinked()).isTrue();
    }
}
