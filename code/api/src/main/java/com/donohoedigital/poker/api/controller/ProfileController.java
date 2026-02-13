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

import com.donohoedigital.games.poker.model.OnlineProfile;
import com.donohoedigital.games.poker.service.OnlineProfileService;
import com.donohoedigital.games.poker.service.PasswordHashingService;
import com.donohoedigital.poker.api.dto.ForgotPasswordRequest;
import com.donohoedigital.poker.api.service.EmailService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Profile endpoints - view/update user profile, manage aliases.
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);

    @Autowired
    private OnlineProfileService profileService;

    @Autowired
    private PasswordHashingService passwordHashingService;

    @Autowired
    private EmailService emailService;

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
     * Update password.
     */
    @PutMapping("/password")
    public ResponseEntity<Map<String, Object>> updatePassword(@AuthenticationPrincipal String username,
            @RequestBody Map<String, String> request) {

        String oldPassword = request.get("oldPassword");
        String newPassword = request.get("newPassword");

        OnlineProfile profile = profileService.getOnlineProfileByName(username);

        // Verify old password
        if (!passwordHashingService.checkPassword(oldPassword, profile.getPasswordHash())) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Current password is incorrect");
            return ResponseEntity.ok(error);
        }

        // Update password
        profileService.hashAndSetPassword(profile, newPassword);
        profileService.saveOnlineProfile(profile);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Password updated successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Get all aliases for current user's email.
     */
    @GetMapping("/aliases")
    public ResponseEntity<List<OnlineProfile>> getAliases(@AuthenticationPrincipal String username) {
        OnlineProfile profile = profileService.getOnlineProfileByName(username);
        List<OnlineProfile> aliases = profileService.getAllOnlineProfilesForEmail(profile.getEmail(), username);
        return ResponseEntity.ok(aliases);
    }

    /**
     * Retire current profile (soft delete).
     */
    @PostMapping("/retire")
    public ResponseEntity<Map<String, Object>> retireProfile(@AuthenticationPrincipal String username) {
        profileService.retire(username);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Profile retired successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Forgot password - generate temporary password and send to user's email. TODO:
     * Add rate limiting (max 3 requests per username per hour) to prevent abuse
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String username = request.getUsername();
        Map<String, Object> response = new HashMap<>();

        logger.info("Password reset requested for username: {}", username);

        // Look up profile
        OnlineProfile profile = profileService.getOnlineProfileByName(username);

        // Use generic error message to prevent username enumeration
        String genericError = "If this username exists and has an email on file, a password reset email has been sent.";

        // Check if profile exists
        if (profile == null || profile.isRetired()) {
            logger.info("Password reset: profile not found or retired for {}", username);
            response.put("success", true);
            response.put("message", genericError);
            return ResponseEntity.ok(response);
        }

        // Check if profile has email
        String email = profile.getEmail();
        if (email == null || email.trim().isEmpty()) {
            logger.info("Password reset: no email on file for {}", username);
            response.put("success", true);
            response.put("message", genericError);
            return ResponseEntity.ok(response);
        }

        // Generate temporary password (8 chars: letters and numbers)
        String tempPassword = generateTemporaryPassword();

        // Hash and save temporary password
        profileService.hashAndSetPassword(profile, tempPassword);
        profileService.saveOnlineProfile(profile);

        // Send email with temporary password
        boolean emailSent = emailService.sendPasswordResetEmail(email, username, tempPassword);

        if (emailSent) {
            logger.info("Password reset email sent successfully to {} for user {}", email, username);
            response.put("success", true);
            response.put("message", "A temporary password has been sent to your email address");
        } else {
            logger.error("Password reset failed: could not send email to {} for user {}", email, username);
            response.put("success", false);
            response.put("message", "Failed to send email. Please try again later or contact support.");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Generate a random 8-character temporary password.
     */
    private String generateTemporaryPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        StringBuilder password = new StringBuilder();
        java.security.SecureRandom random = new java.security.SecureRandom();

        for (int i = 0; i < 8; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }

        return password.toString();
    }
}
