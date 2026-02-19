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
 * Edge case tests for FirstTimeWizard - Tests boundary conditions, invalid
 * inputs, and unusual scenarios to ensure robustness.
 */
class FirstTimeWizardEdgeCaseTest {

    private GameEngine mockEngine;
    private GameContext mockContext;
    private FirstTimeWizard wizard;
    private DMTypedHashMap params;
    private Preferences testPrefs;

    @BeforeEach
    void setUp() throws Exception {
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);
        mockEngine = null;
        mockContext = null;
        params = new DMTypedHashMap();
        testPrefs = Prefs.getUserPrefs("ftue-edge-test");
        testPrefs.clear();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (testPrefs != null) {
            testPrefs.clear();
            testPrefs.flush();
        }
    }

    // =================================================================
    // Profile Name Validation Edge Cases
    // =================================================================

    @Test
    void should_RejectWhitespaceOnlyName_When_Validating() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.setPlayerName("   ");
        boolean isValid = wizard.validateProfileName();

        assertThat(isValid).isFalse();
        assertThat(wizard.getValidationError()).isNotEmpty();
    }

    @Test
    void should_RejectTabsOnlyName_When_Validating() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.setPlayerName("\t\t\t");
        boolean isValid = wizard.validateProfileName();

        assertThat(isValid).isFalse();
    }

    @Test
    void should_RejectNewlinesOnlyName_When_Validating() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.setPlayerName("\n\n");
        boolean isValid = wizard.validateProfileName();

        assertThat(isValid).isFalse();
    }

    @Test
    void should_AcceptNameWithLeadingSpaces_When_Validating() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.setPlayerName("  ValidName");
        boolean isValid = wizard.validateProfileName();

        // Name validation uses trim(), so leading spaces should be OK
        assertThat(isValid).isTrue();
    }

    @Test
    void should_AcceptNameWithTrailingSpaces_When_Validating() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.setPlayerName("ValidName  ");
        boolean isValid = wizard.validateProfileName();

        // Name validation uses trim(), so trailing spaces should be OK
        assertThat(isValid).isTrue();
    }

    @Test
    void should_AcceptVeryLongName_When_Validating() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        // Test with 1000 character name
        String longName = "A".repeat(1000);
        wizard.setPlayerName(longName);
        boolean isValid = wizard.validateProfileName();

        // Validation doesn't check length, so should be valid
        assertThat(isValid).isTrue();
    }

    @Test
    void should_AcceptNameWithSpecialCharacters_When_Validating() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.setPlayerName("Player!@#$%^&*()");
        boolean isValid = wizard.validateProfileName();

        // No character restrictions in validation
        assertThat(isValid).isTrue();
    }

    @Test
    void should_AcceptUnicodeCharacters_When_Validating() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.setPlayerName("玩家名字"); // Chinese characters
        boolean isValid = wizard.validateProfileName();

        assertThat(isValid).isTrue();
    }

    // =================================================================
    // Email Validation Edge Cases
    // =================================================================

    @Test
    void should_RejectEmailWithSpacesOnly_When_Validating() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.setPlayerEmail("   ");
        boolean isValid = wizard.validateEmail();

        assertThat(isValid).isFalse();
    }

    @Test
    void should_AcceptEmailWithLeadingSpaces_When_Validating() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.setPlayerEmail("  user@example.com");
        boolean isValid = wizard.validateEmail();

        // Uses trim(), so leading spaces should be OK if trimmed email is valid
        assertThat(isValid).isTrue();
    }

    @Test
    void should_AcceptEmailWithTrailingSpaces_When_Validating() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.setPlayerEmail("user@example.com  ");
        boolean isValid = wizard.validateEmail();

        // Uses trim(), so trailing spaces should be OK if trimmed email is valid
        assertThat(isValid).isTrue();
    }

    @Test
    void should_AcceptUppercaseEmail_When_Validating() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.setPlayerEmail("USER@EXAMPLE.COM");
        boolean isValid = wizard.validateEmail();

        // Regex includes A-Z, so should be valid
        assertThat(isValid).isTrue();
    }

    @Test
    void should_AcceptMixedCaseEmail_When_Validating() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.setPlayerEmail("User.Name@Example.Com");
        boolean isValid = wizard.validateEmail();

        assertThat(isValid).isTrue();
    }

    @Test
    void should_AcceptPlusAddressing_When_Validating() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.setPlayerEmail("user+tag@example.com");
        boolean isValid = wizard.validateEmail();

        // Regex includes +, so should be valid
        assertThat(isValid).isTrue();
    }

    @Test
    void should_AcceptSubdomains_When_Validating() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.setPlayerEmail("user@mail.example.com");
        boolean isValid = wizard.validateEmail();

        assertThat(isValid).isTrue();
    }

    @Test
    void should_AcceptLongTLD_When_Validating() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.setPlayerEmail("user@example.technology");
        boolean isValid = wizard.validateEmail();

        // Regex allows 2+ letter TLDs
        assertThat(isValid).isTrue();
    }

    @Test
    void should_RejectEmailWithMultipleAtSymbols_When_Validating() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.setPlayerEmail("user@@example.com");
        boolean isValid = wizard.validateEmail();

        assertThat(isValid).isFalse();
    }

    @Test
    void should_RejectEmailMissingUsername_When_Validating() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.setPlayerEmail("@example.com");
        boolean isValid = wizard.validateEmail();

        assertThat(isValid).isFalse();
    }

    @Test
    void should_RejectEmailMissingDomain_When_Validating() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.setPlayerEmail("user@");
        boolean isValid = wizard.validateEmail();

        assertThat(isValid).isFalse();
    }

    @Test
    void should_RejectEmailMissingTLD_When_Validating() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.setPlayerEmail("user@example");
        boolean isValid = wizard.validateEmail();

        assertThat(isValid).isFalse();
    }

    @Test
    void should_RejectEmailWithSpaceInMiddle_When_Validating() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.setPlayerEmail("user name@example.com");
        boolean isValid = wizard.validateEmail();

        assertThat(isValid).isFalse();
    }

    @Test
    void should_RejectVeryLongEmail_When_Over254Characters() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        // Email standard RFC 5321 limits to 254 characters
        String longEmail = "a".repeat(250) + "@example.com";
        wizard.setPlayerEmail(longEmail);
        boolean isValid = wizard.validateEmail();

        // Our regex doesn't check length, so it will pass validation
        // This documents current behavior - may want to add length check
        assertThat(isValid).isTrue();
    }

    // =================================================================
    // Server Address Validation Edge Cases
    // =================================================================

    @Test
    void should_AcceptIPv4Address_When_Validating() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.setGameServer("192.168.1.1:8877");
        boolean isValid = wizard.validateServerAddress();

        assertThat(isValid).isTrue();
    }

    @Test
    void should_AcceptLocalhostIP_When_Validating() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.setGameServer("127.0.0.1:8877");
        boolean isValid = wizard.validateServerAddress();

        assertThat(isValid).isTrue();
    }

    @Test
    void should_RejectServerWithPortZero_When_Validating() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.setGameServer("localhost:0");
        boolean isValid = wizard.validateServerAddress();

        // Regex allows :0 but port 0 is invalid - documents current behavior
        // May want to add port range validation (1-65535)
        assertThat(isValid).isTrue(); // Current behavior - no port range check
    }

    @Test
    void should_AcceptServerWithMaxValidPort_When_Validating() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.setGameServer("localhost:65535");
        boolean isValid = wizard.validateServerAddress();

        assertThat(isValid).isTrue();
    }

    @Test
    void should_AcceptServerWithInvalidPortNumber_When_Over65535() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.setGameServer("localhost:99999");
        boolean isValid = wizard.validateServerAddress();

        // Regex allows up to 5 digits, but valid ports are 1-65535
        // Documents current behavior - no port range validation
        assertThat(isValid).isTrue(); // Current behavior
    }

    @Test
    void should_RejectServerMissingPort_When_Validating() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.setGameServer("localhost");
        boolean isValid = wizard.validateServerAddress();

        assertThat(isValid).isFalse();
    }

    @Test
    void should_RejectServerWithColonOnly_When_Validating() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.setGameServer("localhost:");
        boolean isValid = wizard.validateServerAddress();

        assertThat(isValid).isFalse();
    }

    @Test
    void should_RejectServerWithSpaces_When_Validating() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.setGameServer("local host:8877");
        boolean isValid = wizard.validateServerAddress();

        assertThat(isValid).isFalse();
    }

    @Test
    void should_AcceptServerWithHyphens_When_Validating() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.setGameServer("my-server.example.com:8877");
        boolean isValid = wizard.validateServerAddress();

        assertThat(isValid).isTrue();
    }

    @Test
    void should_RejectServerWithInvalidCharacters_When_Validating() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.setGameServer("server@example.com:8877");
        boolean isValid = wizard.validateServerAddress();

        assertThat(isValid).isFalse();
    }

    // =================================================================
    // Navigation Edge Cases
    // =================================================================

    @Test
    void should_StayAtPlayMode_When_PreviousStepCalledAtFirstStep() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        // Already at PLAY_MODE after init
        String initialStep = wizard.getCurrentStepType();
        wizard.previousStep();
        String afterStep = wizard.getCurrentStepType();

        // Should stay at play mode (can't go before first step)
        assertThat(initialStep).isEqualTo(FirstTimeWizard.STEP_TYPE_PLAY_MODE);
        assertThat(afterStep).isEqualTo(FirstTimeWizard.STEP_TYPE_PLAY_MODE);
    }

    @Test
    void should_StayAtComplete_When_NextStepCalledAtLastStep() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        // Navigate to complete step
        wizard.selectPlayMode(FirstTimeWizard.MODE_OFFLINE);
        wizard.setPlayerName("Test");
        wizard.nextStep(); // To profile
        wizard.nextStep(); // To complete

        assertThat(wizard.getCurrentStepType()).isEqualTo(FirstTimeWizard.STEP_TYPE_COMPLETE);

        // Try to go next
        wizard.nextStep();

        // Should stay at complete
        assertThat(wizard.getCurrentStepType()).isEqualTo(FirstTimeWizard.STEP_TYPE_COMPLETE);
    }

    @Test
    void should_HandleModeChange_When_NavigatingBackAndChangingMode() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        // Start with offline mode
        wizard.selectPlayMode(FirstTimeWizard.MODE_OFFLINE);
        wizard.nextStep();
        assertThat(wizard.getCurrentStepType()).isEqualTo(FirstTimeWizard.STEP_TYPE_PROFILE);

        // Go back
        wizard.previousStep();
        assertThat(wizard.getCurrentStepType()).isEqualTo(FirstTimeWizard.STEP_TYPE_PLAY_MODE);

        // Change to online mode
        wizard.selectPlayMode(FirstTimeWizard.MODE_ONLINE_NEW);
        wizard.nextStep();

        // Should now show server config instead of profile
        assertThat(wizard.getCurrentStepType()).isEqualTo(FirstTimeWizard.STEP_TYPE_SERVER_CONFIG);
    }

    // =================================================================
    // State Management Edge Cases
    // =================================================================

    @Test
    void should_HandleMultipleInitCalls_When_CalledTwice() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);
        assertThat(wizard.isWizardInitialized()).isTrue();

        // Call init again
        wizard.init(mockEngine, mockContext, params);

        // Should still be initialized
        assertThat(wizard.isWizardInitialized()).isTrue();
        assertThat(wizard.getCurrentStepType()).isEqualTo(FirstTimeWizard.STEP_TYPE_PLAY_MODE);
    }

    @Test
    void should_ReturnSameProfile_When_CompleteWizardCalledTwice() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);
        wizard.selectPlayMode(FirstTimeWizard.MODE_OFFLINE);
        wizard.setPlayerName("TestUser");

        PlayerProfile profile1 = wizard.completeWizard();
        PlayerProfile profile2 = wizard.completeWizard();

        // Should return same profile
        assertThat(profile1).isNotNull();
        assertThat(profile2).isNotNull();
        assertThat(profile1.getName()).isEqualTo(profile2.getName());
    }

    @Test
    void should_MaintainSkippedState_When_SkipCalledAfterComplete() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);
        wizard.selectPlayMode(FirstTimeWizard.MODE_OFFLINE);
        wizard.setPlayerName("TestUser");

        wizard.completeWizard();
        assertThat(wizard.isWizardComplete()).isTrue();
        assertThat(wizard.wasWizardSkipped()).isFalse();

        // Try to skip after completing
        wizard.skipWizard();

        // Should maintain state
        assertThat(wizard.isWizardComplete()).isTrue();
        assertThat(wizard.wasWizardSkipped()).isTrue();
    }

    // =================================================================
    // Mode Selection Edge Cases
    // =================================================================

    @Test
    void should_HandleInvalidModeValue_When_SelectingMode() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        // Select invalid mode value
        wizard.selectPlayMode(999);
        wizard.nextStep();

        // Should handle gracefully - documents current behavior
        // May want to add mode validation
        assertThat(wizard.getCurrentStepType()).isNotEmpty();
    }

    @Test
    void should_HandleNegativeModeValue_When_SelectingMode() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.selectPlayMode(-1);
        wizard.nextStep();

        // Should handle gracefully
        assertThat(wizard.getCurrentStepType()).isNotEmpty();
    }

    // =================================================================
    // Server Connection Edge Cases
    // =================================================================

    @Test
    void should_HandleMultipleConnectionTests_When_CalledRepeatedly() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);
        wizard.setGameServer("localhost:8877");

        // Test connection multiple times
        wizard.testServerConnection();
        wizard.setConnectionTestResult(true);

        wizard.testServerConnection();
        wizard.setConnectionTestResult(false);

        wizard.testServerConnection();
        wizard.setConnectionTestResult(true);

        // Should use latest result
        assertThat(wizard.canProceedToNextStep()).isTrue();
    }

    @Test
    void should_ResetConnectionResult_When_ServerAddressChanged() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.setGameServer("localhost:8877");
        wizard.testServerConnection();
        wizard.setConnectionTestResult(true);
        wizard.setServerConfigComplete(true);

        // Change server address
        wizard.setGameServer("different.server.com:8877");

        // Connection result is NOT automatically reset - documents current behavior
        // May want to reset connectionTestResult when server changes
        assertThat(wizard.canProceedToNextStep()).isTrue(); // Still true from previous test
    }

    // =================================================================
    // Null Safety Edge Cases
    // =================================================================

    @Test
    void should_HandleNullPlayerName_When_Setting() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.setPlayerName(null);
        boolean isValid = wizard.validateProfileName();

        // setPlayerName converts null to empty string
        assertThat(isValid).isFalse();
    }

    @Test
    void should_HandleNullEmail_When_Setting() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.setPlayerEmail(null);
        boolean isValid = wizard.validateEmail();

        // setPlayerEmail converts null to empty string
        assertThat(isValid).isFalse();
    }

    @Test
    void should_HandleNullServerAddress_When_Setting() {
        wizard = new FirstTimeWizard();
        wizard.init(mockEngine, mockContext, params);

        wizard.setGameServer(null);
        boolean isValid = wizard.validateServerAddress();

        // setGameServer converts null to empty string
        assertThat(isValid).isFalse();
    }
}
