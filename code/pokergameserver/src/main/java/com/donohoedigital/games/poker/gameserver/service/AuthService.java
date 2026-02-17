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

import com.donohoedigital.games.poker.gameserver.auth.JwtTokenProvider;
import com.donohoedigital.games.poker.gameserver.dto.LoginResponse;
import com.donohoedigital.games.poker.gameserver.persistence.repository.OnlineProfileRepository;
import com.donohoedigital.games.poker.model.OnlineProfile;

/**
 * Service for authentication operations (login, registration).
 */
@Service
public class AuthService {

    private final OnlineProfileRepository profileRepository;
    private final BanService banService;
    private final JwtTokenProvider tokenProvider;

    public AuthService(OnlineProfileRepository profileRepository, BanService banService,
            JwtTokenProvider tokenProvider) {
        this.profileRepository = profileRepository;
        this.banService = banService;
        this.tokenProvider = tokenProvider;
    }

    /**
     * Register a new user profile.
     *
     * @param username
     *            desired username
     * @param password
     *            plain text password (will be BCrypt hashed)
     * @param email
     *            email address
     * @return login response with token if successful
     */
    public LoginResponse register(String username, String password, String email) {
        // Check if email is banned
        if (banService.isEmailBanned(email)) {
            return new LoginResponse(false, null, null, null, "This email address is banned");
        }

        // Check if username already exists
        if (profileRepository.existsByName(username)) {
            return new LoginResponse(false, null, null, null, "Username already exists");
        }

        // Check if email already exists
        if (profileRepository.existsByEmail(email)) {
            return new LoginResponse(false, null, null, null, "Email already in use");
        }

        // Create new profile
        OnlineProfile profile = new OnlineProfile();
        profile.setName(username);
        profile.setEmail(email);
        profile.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt()));
        profile.setUuid(java.util.UUID.randomUUID().toString());

        profile = profileRepository.save(profile);

        // Generate token
        String token = tokenProvider.generateToken(username, profile.getId(), false);

        return new LoginResponse(true, token, profile.getId(), username, null);
    }

    /**
     * Authenticate user and generate JWT token.
     *
     * @param username
     *            the username
     * @param password
     *            the plain text password
     * @param rememberMe
     *            whether to use extended token expiration
     * @return login response with token if successful
     */
    public LoginResponse login(String username, String password, boolean rememberMe) {
        // Find profile
        OnlineProfile profile = profileRepository.findByName(username).orElse(null);
        if (profile == null) {
            return new LoginResponse(false, null, null, null, "Invalid username or password");
        }

        // Verify password
        if (!BCrypt.checkpw(password, profile.getPasswordHash())) {
            return new LoginResponse(false, null, null, null, "Invalid username or password");
        }

        // Check if profile is retired
        if (profile.isRetired()) {
            return new LoginResponse(false, null, null, null, "This account has been retired");
        }

        // Check if profile is banned
        if (banService.isProfileBanned(profile.getId())) {
            return new LoginResponse(false, null, null, null, "This account is banned");
        }

        // Generate token
        String token = tokenProvider.generateToken(username, profile.getId(), rememberMe);

        return new LoginResponse(true, token, profile.getId(), username, null);
    }
}
