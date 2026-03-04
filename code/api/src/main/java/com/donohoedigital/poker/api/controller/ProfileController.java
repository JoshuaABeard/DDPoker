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
package com.donohoedigital.poker.api.controller;

import com.donohoedigital.games.poker.model.OnlineProfile;
import com.donohoedigital.games.poker.service.OnlineProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Profile endpoints - view user profile, manage aliases.
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);

    @Autowired
    private OnlineProfileService profileService;

    /**
     * Get current user's profile.
     */
    @GetMapping
    public ResponseEntity<OnlineProfile> getProfile(@AuthenticationPrincipal String username) {
        OnlineProfile profile = profileService.getOnlineProfileByName(username);
        if (profile == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(profile);
    }

    /**
     * Get all aliases for current user's email.
     */
    @GetMapping("/aliases")
    public ResponseEntity<List<OnlineProfile>> getAliases(@AuthenticationPrincipal String username) {
        OnlineProfile profile = profileService.getOnlineProfileByName(username);
        if (profile == null) {
            return ResponseEntity.notFound().build();
        }
        List<OnlineProfile> aliases = profileService.getAllOnlineProfilesForEmail(profile.getEmail(), username);
        return ResponseEntity.ok(aliases);
    }

    /**
     * Retire current profile (soft delete).
     */
    @PostMapping("/retire")
    public ResponseEntity<Map<String, Object>> retireProfile(@AuthenticationPrincipal String username) {
        profileService.retire(username);

        return ResponseEntity.ok(Map.of("success", true, "message", "Profile retired successfully"));
    }
}
