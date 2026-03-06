/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2026 Joshua Beard and contributors
 *
 * This file is part of DD Poker, originally created by Doug Donohoe.
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
package com.donohoedigital.games.poker.ui;

import com.donohoedigital.games.poker.PlayerProfile;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FirstRunWizard} logic (non-UI, headless safe).
 *
 * <p>
 * These tests exercise the wizard's result transitions and state via the
 * {@code simulate*} test hooks, without creating any Swing windows.
 */
@EnabledIfDisplay
class FirstRunWizardTest {

    /**
     * Calling simulateLocalProfileCreation sets result to LOCAL_PROFILE_CREATED and
     * populates the created profile.
     */
    @Test
    void simulateLocalProfileCreation_returnsLocalProfileCreated() {
        FirstRunWizard wizard = new FirstRunWizard(null);

        FirstRunWizard.WizardResult result = wizard.simulateLocalProfileCreation("Alice");

        assertThat(result).isEqualTo(FirstRunWizard.WizardResult.LOCAL_PROFILE_CREATED);
        assertThat(wizard.getResult()).isEqualTo(FirstRunWizard.WizardResult.LOCAL_PROFILE_CREATED);
    }

    /**
     * The created profile has the name supplied to simulateLocalProfileCreation.
     */
    @Test
    void simulateLocalProfileCreation_profileHasSuppliedName() {
        FirstRunWizard wizard = new FirstRunWizard(null);

        wizard.simulateLocalProfileCreation("Bob");

        PlayerProfile profile = wizard.getCreatedProfile();
        assertThat(profile).isNotNull();
        assertThat(profile.getName()).isEqualTo("Bob");
    }

    /**
     * Default result before any action is CANCELLED.
     */
    @Test
    void initialResult_isCancelled() {
        FirstRunWizard wizard = new FirstRunWizard(null);

        assertThat(wizard.getResult()).isEqualTo(FirstRunWizard.WizardResult.CANCELLED);
    }

    /**
     * simulateOnlineLogin sets result to ONLINE_PROFILE_CREATED and populates all
     * online fields.
     */
    @Test
    void simulateOnlineLogin_returnsOnlineProfileCreated() {
        FirstRunWizard wizard = new FirstRunWizard(null);

        FirstRunWizard.WizardResult result = wizard.simulateOnlineLogin("http://poker.example.com:8877", "Alice",
                "tok-abc", 42L, "alice@example.com");

        assertThat(result).isEqualTo(FirstRunWizard.WizardResult.ONLINE_PROFILE_CREATED);
        assertThat(wizard.getResult()).isEqualTo(FirstRunWizard.WizardResult.ONLINE_PROFILE_CREATED);
        assertThat(wizard.getOnlineServerUrl()).isEqualTo("http://poker.example.com:8877");
        assertThat(wizard.getOnlineUsername()).isEqualTo("Alice");
        assertThat(wizard.getOnlineJwt()).isEqualTo("tok-abc");
        assertThat(wizard.getOnlineProfileId()).isEqualTo(42L);
        assertThat(wizard.getOnlineEmail()).isEqualTo("alice@example.com");
    }
}
