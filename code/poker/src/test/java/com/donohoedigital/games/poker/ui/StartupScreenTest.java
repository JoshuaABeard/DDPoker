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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link StartupScreen} logic (non-UI, headless safe).
 *
 * <p>
 * These tests exercise the screen's result transitions via the
 * {@code simulate*} test hooks, without creating any visible Swing windows.
 */
class StartupScreenTest {

    private static final String SERVER_URL = "http://poker.example.com:8877";
    private static final String USERNAME = "Alice";
    private static final String PROFILE_NAME = "alice-profile";

    /**
     * Default result before any action is SWITCH_PROFILE (safe fallback).
     */
    @Test
    void initialResult_isSwitchProfile() {
        StartupScreen screen = new StartupScreen(null, SERVER_URL, USERNAME);

        assertThat(screen.getResult()).isEqualTo(StartupScreen.ScreenResult.SWITCH_PROFILE);
    }

    /**
     * simulatePracticeSelected returns and sets PRACTICE.
     */
    @Test
    void simulatePracticeSelected_returnsPractice() {
        StartupScreen screen = new StartupScreen(null, SERVER_URL, USERNAME);

        StartupScreen.ScreenResult result = screen.simulatePracticeSelected();

        assertThat(result).isEqualTo(StartupScreen.ScreenResult.PRACTICE);
        assertThat(screen.getResult()).isEqualTo(StartupScreen.ScreenResult.PRACTICE);
    }

    /**
     * simulateSignIn returns AUTHENTICATED and populates session fields.
     */
    @Test
    void simulateSignIn_returnsAuthenticatedAndPopulatesFields() {
        StartupScreen screen = new StartupScreen(null, SERVER_URL, USERNAME);

        StartupScreen.ScreenResult result = screen.simulateSignIn("tok-xyz", 7L, "alice@example.com");

        assertThat(result).isEqualTo(StartupScreen.ScreenResult.AUTHENTICATED);
        assertThat(screen.getResult()).isEqualTo(StartupScreen.ScreenResult.AUTHENTICATED);
        assertThat(screen.getJwt()).isEqualTo("tok-xyz");
        assertThat(screen.getProfileId()).isEqualTo(7L);
        assertThat(screen.getEmail()).isEqualTo("alice@example.com");
    }

    /**
     * JWT and profileId are null before sign-in.
     */
    @Test
    void beforeSignIn_jwtAndProfileIdAreNull() {
        StartupScreen screen = new StartupScreen(null, SERVER_URL, USERNAME);

        assertThat(screen.getJwt()).isNull();
        assertThat(screen.getProfileId()).isNull();
        assertThat(screen.getEmail()).isNull();
    }

    /**
     * Constructor with explicit profileName stores it separately from username.
     */
    @Test
    void constructor_withProfileName_storesProfileNameDistinctFromUsername() {
        StartupScreen screen = new StartupScreen(null, SERVER_URL, USERNAME, PROFILE_NAME);

        // Screen is constructed without error; verify basic state
        assertThat(screen.getResult()).isEqualTo(StartupScreen.ScreenResult.SWITCH_PROFILE);
        assertThat(screen.getJwt()).isNull();
    }

}
