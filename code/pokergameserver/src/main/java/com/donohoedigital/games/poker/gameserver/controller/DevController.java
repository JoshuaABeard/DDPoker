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
package com.donohoedigital.games.poker.gameserver.controller;

import java.util.Map;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.donohoedigital.games.poker.gameserver.persistence.repository.OnlineProfileRepository;
import com.donohoedigital.games.poker.model.OnlineProfile;

/**
 * Development-only controller for test automation helpers.
 *
 * <p>
 * Only active under the {@code embedded} Spring profile. Provides endpoints
 * that bypass normal flows for use by automated tests.
 */
@RestController
@RequestMapping("/api/v1/dev")
@Profile("embedded")
public class DevController {

    private final OnlineProfileRepository profileRepository;

    public DevController(OnlineProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    /**
     * Mark a user's email as verified, clearing the verification token and expiry.
     *
     * @param username
     *            the username to verify
     * @return 200 with verified status if found, 404 if not found
     */
    @PostMapping("/verify-user")
    public ResponseEntity<Map<String, Object>> verifyUser(@RequestParam(name = "username") String username) {
        Optional<OnlineProfile> maybeProfile = profileRepository.findByName(username);

        if (maybeProfile.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("found", false, "username", username));
        }

        OnlineProfile profile = maybeProfile.get();
        profile.setEmailVerified(true);
        profile.setEmailVerificationToken(null);
        profile.setEmailVerificationTokenExpiry(null);
        profileRepository.save(profile);

        return ResponseEntity.ok(Map.of("found", true, "verified", true, "username", username));
    }
}
