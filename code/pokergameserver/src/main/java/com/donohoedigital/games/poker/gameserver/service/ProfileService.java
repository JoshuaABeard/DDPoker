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

import org.springframework.stereotype.Service;

import com.donohoedigital.games.poker.gameserver.persistence.repository.OnlineProfileRepository;
import com.donohoedigital.games.poker.model.OnlineProfile;

/**
 * Service for profile management operations (CRUD).
 */
@Service
public class ProfileService {

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
}
