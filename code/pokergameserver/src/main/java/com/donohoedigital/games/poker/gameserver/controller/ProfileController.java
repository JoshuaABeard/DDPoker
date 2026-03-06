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

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.donohoedigital.games.poker.gameserver.auth.JwtAuthenticationFilter;
import com.donohoedigital.games.poker.gameserver.persistence.repository.OnlineProfileRepository;
import com.donohoedigital.games.poker.gameserver.service.ProfileService;
import com.donohoedigital.games.poker.model.OnlineProfile;
import com.donohoedigital.games.poker.protocol.dto.PublicProfileResponse;

/**
 * REST controller for profile management.
 */
@RestController
@RequestMapping("/api/v1/profiles")
public class ProfileController {

    private final ProfileService profileService;
    private final OnlineProfileRepository profileRepository;

    public ProfileController(ProfileService profileService, OnlineProfileRepository profileRepository) {
        this.profileService = profileService;
        this.profileRepository = profileRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<PublicProfileResponse> getProfile(@PathVariable("id") Long id) {
        OnlineProfile profile = profileService.getProfile(id);
        if (profile == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toPublicProfile(profile));
    }

    /**
     * Get a profile by display name.
     */
    @GetMapping("/name/{name}")
    public ResponseEntity<PublicProfileResponse> getProfileByName(@PathVariable("name") String name) {
        OnlineProfile profile = profileRepository.findByName(name).orElse(null);
        if (profile == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toPublicProfile(profile));
    }

    /**
     * List all profiles (aliases) for the authenticated user's email, excluding the
     * current profile name.
     */
    @GetMapping("/aliases")
    public ResponseEntity<List<PublicProfileResponse>> getAliases() {
        Long profileId = getAuthenticatedProfileId();
        OnlineProfile profile = profileService.getProfile(profileId);
        if (profile == null) {
            return ResponseEntity.notFound().build();
        }
        List<PublicProfileResponse> aliases = profileRepository
                .findByEmailExcludingName(profile.getEmail(), profile.getName()).stream().map(this::toPublicProfile)
                .toList();
        return ResponseEntity.ok(aliases);
    }

    /**
     * Retire (soft-delete) a profile by ID. The JWT profile ID must match the path
     * {id}.
     */
    @PostMapping("/{id}/retire")
    public ResponseEntity<Map<String, Object>> retireProfile(@PathVariable("id") Long id) {
        Long authenticatedProfileId = getAuthenticatedProfileId();
        if (!authenticatedProfileId.equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        boolean success = profileService.deleteProfile(id);
        if (!success) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "Profile retired successfully"));
    }

    private PublicProfileResponse toPublicProfile(OnlineProfile p) {
        return new PublicProfileResponse(p.getId(), p.getName(),
                p.getCreateDate() != null ? p.getCreateDate().toString() : null);
    }

    private Long getAuthenticatedProfileId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationFilter.JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getProfileId();
        }
        throw new IllegalStateException("No authenticated user found");
    }
}
