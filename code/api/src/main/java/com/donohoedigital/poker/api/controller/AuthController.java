/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
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
package com.donohoedigital.poker.api.controller;

import com.donohoedigital.config.PropertyConfig;
import com.donohoedigital.poker.api.dto.AuthResponse;
import com.donohoedigital.poker.api.dto.LoginRequest;
import com.donohoedigital.poker.api.security.JwtTokenProvider;
import com.donohoedigital.games.poker.model.OnlineProfile;
import com.donohoedigital.games.poker.service.OnlineProfileService;
import com.donohoedigital.games.poker.service.PasswordHashingService;
import com.donohoedigital.games.server.model.BannedKey;
import com.donohoedigital.games.server.service.BannedKeyService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication endpoints - login, logout, password reset.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private static final String TOKEN_COOKIE_NAME = "ddpoker-token";
    private static final int COOKIE_MAX_AGE_30_DAYS = 30 * 24 * 60 * 60;

    @Autowired
    private OnlineProfileService profileService;

    @Autowired
    private PasswordHashingService passwordHashingService;

    @Autowired
    private BannedKeyService bannedKeyService;

    @Autowired
    private JwtTokenProvider tokenProvider;

    /**
     * Login endpoint - authenticate user and return JWT token in cookie.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        String username = request.getUsername();
        String password = request.getPassword();
        boolean rememberMe = request.isRememberMe();

        // Lookup profile
        OnlineProfile profile = profileService.getOnlineProfileByName(username);

        // Check if profile exists and not retired
        if (profile == null || profile.isRetired()) {
            logger.info("Login failed for {}: profile not found or not accessible", username);
            return ResponseEntity.ok(new AuthResponse(false, "Invalid username or password"));
        }

        // Check if banned
        BannedKey ban = bannedKeyService.getIfBanned(profile.getEmail(), profile.getName());
        if (ban != null) {
            logger.info("Login failed for {}: user is banned until {}", username, ban.getUntil());
            return ResponseEntity.ok(new AuthResponse(false, "This account has been banned"));
        }

        // Verify password
        if (!passwordHashingService.checkPassword(password, profile.getPasswordHash())) {
            logger.info("Login failed for {}: password mismatch", username);
            return ResponseEntity.ok(new AuthResponse(false, "Invalid username or password"));
        }

        // Check if admin
        String adminUser = PropertyConfig.getStringProperty("settings.admin.user", null, false);
        boolean isAdmin = adminUser != null && username.equals(adminUser);

        // Generate JWT token
        String token = tokenProvider.generateToken(username, isAdmin, rememberMe);

        // Set HttpOnly cookie with SameSite=Strict for CSRF protection
        ResponseCookie cookie = ResponseCookie.from(TOKEN_COOKIE_NAME, token).httpOnly(true).secure(false) // Set to
                                                                                                            // true in
                                                                                                            // production
                                                                                                            // with
                                                                                                            // HTTPS
                .path("/").maxAge(rememberMe ? COOKIE_MAX_AGE_30_DAYS : -1) // Session cookie if not remember me
                .sameSite("Strict") // CSRF protection
                .build();
        response.addHeader("Set-Cookie", cookie.toString());

        logger.info("User {} logged in successfully (admin={}, rememberMe={})", username, isAdmin, rememberMe);

        return ResponseEntity.ok(new AuthResponse(true, "Login successful", username, isAdmin));
    }

    /**
     * Logout endpoint - clear JWT token cookie.
     */
    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logout(HttpServletResponse response) {
        // Clear cookie
        ResponseCookie cookie = ResponseCookie.from(TOKEN_COOKIE_NAME, "").httpOnly(true).secure(false).path("/")
                .maxAge(0) // Delete cookie
                .sameSite("Strict").build();
        response.addHeader("Set-Cookie", cookie.toString());

        return ResponseEntity.ok(new AuthResponse(true, "Logout successful"));
    }

    /**
     * Get current user info (if authenticated).
     */
    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getCurrentUser(@AuthenticationPrincipal String username) {
        if (username == null) {
            return ResponseEntity.ok(new AuthResponse(false, "Not authenticated"));
        }

        String adminUser = PropertyConfig.getStringProperty("settings.admin.user", null, false);
        boolean isAdmin = adminUser != null && username.equals(adminUser);

        return ResponseEntity.ok(new AuthResponse(true, "Authenticated", username, isAdmin));
    }
}
