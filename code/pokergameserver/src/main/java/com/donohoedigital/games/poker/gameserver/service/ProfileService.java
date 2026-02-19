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
package com.donohoedigital.games.poker.gameserver.service;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;

import com.donohoedigital.games.poker.gameserver.persistence.repository.OnlineProfileRepository;
import com.donohoedigital.games.poker.model.OnlineProfile;

/**
 * Service for profile management operations (CRUD, password change).
 */
@Service
public class ProfileService {

    /**
     * Thrown when the supplied current password does not match the stored hash.
     */
    public static class InvalidPasswordException extends RuntimeException {
        public InvalidPasswordException() {
            super("Incorrect current password");
        }
    }

    private final OnlineProfileRepository profileRepository;

    public ProfileService(OnlineProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    /**
     * Retrieve a profile by ID.
     *
     * @param id
     *            profile ID
     * @return profile or null if not found
     */
    public OnlineProfile getProfile(Long id) {
        return profileRepository.findById(id).orElse(null);
    }

    /**
     * Update profile email.
     *
     * @param id
     *            profile ID
     * @param email
     *            new email address
     * @return true if updated, false if profile not found
     */
    public boolean updateProfile(Long id, String email) {
        OnlineProfile profile = profileRepository.findById(id).orElse(null);
        if (profile == null) {
            return false;
        }

        profile.setEmail(email);
        profileRepository.save(profile);
        return true;
    }

    /**
     * Delete (retire) a profile.
     *
     * @param id
     *            profile ID
     * @return true if deleted, false if profile not found
     */
    public boolean deleteProfile(Long id) {
        OnlineProfile profile = profileRepository.findById(id).orElse(null);
        if (profile == null) {
            return false;
        }

        profile.setRetired(true);
        profileRepository.save(profile);
        return true;
    }

    /**
     * Change an authenticated user's password.
     *
     * <p>
     * The JWT must already match {@code profileId} (enforced by the controller).
     * This method verifies the old password before updating.
     *
     * @param profileId
     *            the authenticated profile's ID
     * @param oldPassword
     *            the current password to verify
     * @param newPassword
     *            the new password (plain text; will be BCrypt hashed)
     * @throws InvalidPasswordException
     *             if {@code oldPassword} does not match the stored hash
     * @throws IllegalArgumentException
     *             if the profile is not found (should not happen in normal flow)
     */
    public void changePassword(Long profileId, String oldPassword, String newPassword) {
        OnlineProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found: " + profileId));

        if (!BCrypt.checkpw(oldPassword, profile.getPasswordHash())) {
            throw new InvalidPasswordException();
        }

        profile.setPasswordHash(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        profileRepository.save(profile);
    }
}
