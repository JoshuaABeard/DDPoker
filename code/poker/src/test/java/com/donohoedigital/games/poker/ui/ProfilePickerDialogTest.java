/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community
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
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker.ui;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ProfilePickerDialog} logic (non-UI, headless safe).
 *
 * <p>
 * These tests exercise the dialog's result transitions via the
 * {@code simulate*} test hooks, without creating any visible Swing windows.
 */
class ProfilePickerDialogTest {

    /**
     * Default result before any action is CANCELLED.
     */
    @Test
    void initialResult_isCancelled() {
        ProfilePickerDialog dialog = new ProfilePickerDialog(null);

        assertThat(dialog.getPickerResult()).isEqualTo(ProfilePickerDialog.PickerResult.CANCELLED);
    }

    /**
     * simulateCancelled returns and sets CANCELLED.
     */
    @Test
    void simulateCancelled_returnsAndSetsCancelled() {
        ProfilePickerDialog dialog = new ProfilePickerDialog(null);

        ProfilePickerDialog.PickerResult result = dialog.simulateCancelled();

        assertThat(result).isEqualTo(ProfilePickerDialog.PickerResult.CANCELLED);
        assertThat(dialog.getPickerResult()).isEqualTo(ProfilePickerDialog.PickerResult.CANCELLED);
    }

    /**
     * simulateNewProfile returns and sets NEW_PROFILE.
     */
    @Test
    void simulateNewProfile_returnsAndSetsNewProfile() {
        ProfilePickerDialog dialog = new ProfilePickerDialog(null);

        ProfilePickerDialog.PickerResult result = dialog.simulateNewProfile();

        assertThat(result).isEqualTo(ProfilePickerDialog.PickerResult.NEW_PROFILE);
        assertThat(dialog.getPickerResult()).isEqualTo(ProfilePickerDialog.PickerResult.NEW_PROFILE);
    }

    /**
     * selectedProfile is null before any selection.
     */
    @Test
    void beforeSelection_selectedProfileIsNull() {
        ProfilePickerDialog dialog = new ProfilePickerDialog(null);

        assertThat(dialog.getSelectedProfile()).isNull();
    }

    /**
     * simulateSelect with a valid index returns SELECTED and sets selectedProfile.
     */
    @Test
    void simulateSelect_validIndex_returnsSelectedAndSetsProfile() {
        ProfilePickerDialog dialog = new ProfilePickerDialog(null);

        // Only run this check if at least one profile is loaded
        if (dialog.getProfileCount() > 0) {
            ProfilePickerDialog.PickerResult result = dialog.simulateSelect(0);

            assertThat(result).isEqualTo(ProfilePickerDialog.PickerResult.SELECTED);
            assertThat(dialog.getPickerResult()).isEqualTo(ProfilePickerDialog.PickerResult.SELECTED);
            assertThat(dialog.getSelectedProfile()).isNotNull();
        }
    }

    /**
     * simulateSelect with an out-of-range index returns CANCELLED.
     */
    @Test
    void simulateSelect_outOfRangeIndex_returnsCancelled() {
        ProfilePickerDialog dialog = new ProfilePickerDialog(null);

        ProfilePickerDialog.PickerResult result = dialog.simulateSelect(Integer.MAX_VALUE);

        assertThat(result).isEqualTo(ProfilePickerDialog.PickerResult.CANCELLED);
    }

    /**
     * getLoadedProfiles returns a non-null list.
     */
    @Test
    void getLoadedProfiles_returnsNonNullList() {
        ProfilePickerDialog dialog = new ProfilePickerDialog(null);

        assertThat(dialog.getLoadedProfiles()).isNotNull();
    }

    /**
     * getProfileCount is consistent with getLoadedProfiles size.
     */
    @Test
    void getProfileCount_matchesLoadedProfilesSize() {
        ProfilePickerDialog dialog = new ProfilePickerDialog(null);

        assertThat(dialog.getProfileCount()).isEqualTo(dialog.getLoadedProfiles().size());
    }
}
