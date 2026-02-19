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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;

import com.donohoedigital.games.poker.gameserver.auth.JwtTokenProvider;
import com.donohoedigital.games.poker.gameserver.dto.LoginResponse;
import com.donohoedigital.games.poker.gameserver.dto.ProfileResponse;
import com.donohoedigital.games.poker.model.PasswordResetToken;
import com.donohoedigital.games.poker.gameserver.persistence.repository.OnlineProfileRepository;
import com.donohoedigital.games.poker.gameserver.persistence.repository.PasswordResetTokenRepository;
import com.donohoedigital.games.poker.model.OnlineProfile;

/**
 * Service for authentication operations (login, registration, password reset).
 */
@Service
public class AuthService {

    /**
     * Thrown when a password reset token is invalid, expired, or already used. All
     * failure cases use the same message to prevent information leakage.
     */
    public static class InvalidResetTokenException extends RuntimeException {
        public InvalidResetTokenException() {
            super("Invalid or expired reset token");
        }
    }

    /** Rate limit window: 1 request per email per hour. */
    static final long FORGOT_PASSWORD_RATE_LIMIT_MILLIS = 60L * 60 * 1000;

    private final OnlineProfileRepository profileRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final BanService banService;
    private final JwtTokenProvider tokenProvider;
    private final SecureRandom secureRandom = new SecureRandom();

    /** In-memory rate limit: normalized email → last request time (epoch ms). */
    private final ConcurrentHashMap<String, Long> forgotPasswordRateLimits = new ConcurrentHashMap<>();

    public AuthService(OnlineProfileRepository profileRepository, PasswordResetTokenRepository resetTokenRepository,
            BanService banService, JwtTokenProvider tokenProvider) {
        this.profileRepository = profileRepository;
        this.resetTokenRepository = resetTokenRepository;
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
        if (banService.isEmailBanned(email)) {
            return new LoginResponse(false, null, null, null, "This email address is banned");
        }
        if (profileRepository.existsByName(username)) {
            return new LoginResponse(false, null, null, null, "Username already exists");
        }
        if (profileRepository.existsByEmail(email)) {
            return new LoginResponse(false, null, null, null, "Email already in use");
        }

        OnlineProfile profile = new OnlineProfile();
        profile.setName(username);
        profile.setEmail(email);
        profile.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt()));
        profile.setUuid(java.util.UUID.randomUUID().toString());
        profile = profileRepository.save(profile);

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
        OnlineProfile profile = profileRepository.findByName(username).orElse(null);
        if (profile == null) {
            return new LoginResponse(false, null, null, null, "Invalid username or password");
        }
        if (!BCrypt.checkpw(password, profile.getPasswordHash())) {
            return new LoginResponse(false, null, null, null, "Invalid username or password");
        }
        if (profile.isRetired()) {
            return new LoginResponse(false, null, null, null, "This account has been retired");
        }
        if (banService.isProfileBanned(profile.getId())) {
            return new LoginResponse(false, null, null, null, "This account is banned");
        }

        String token = tokenProvider.generateToken(username, profile.getId(), rememberMe);
        return new LoginResponse(true, token, profile.getId(), username, null);
    }

    /**
     * Fetch the current authenticated user's profile data.
     *
     * @param profileId
     *            the authenticated user's profile ID (from JWT)
     * @return profile DTO, or null if not found
     */
    public ProfileResponse getCurrentUser(Long profileId) {
        return profileRepository.findById(profileId)
                .map(p -> new ProfileResponse(p.getId(), p.getName(), p.getEmail(), p.isRetired())).orElse(null);
    }

    /**
     * Initiate a password reset for the given email address.
     *
     * <p>
     * Rate-limited to 1 request per email per hour. Returns the generated token so
     * the caller (controller) can decide whether to expose it based on the active
     * Spring profile. Returns null if the email is unknown or rate-limited —
     * callers must return 200 in either case to avoid leaking information.
     *
     * @param email
     *            the email address to reset
     * @return the reset token, or null if rate-limited or email not registered
     */
    public String forgotPassword(String email) {
        String normalizedEmail = email.toLowerCase();

        // Rate limiting: 1 request per email per hour
        Long lastRequest = forgotPasswordRateLimits.get(normalizedEmail);
        if (lastRequest != null && System.currentTimeMillis() - lastRequest < FORGOT_PASSWORD_RATE_LIMIT_MILLIS) {
            return null;
        }
        // Update rate limit before lookup to avoid leaking email existence via timing
        forgotPasswordRateLimits.put(normalizedEmail, System.currentTimeMillis());

        OnlineProfile profile = profileRepository.findByEmail(email).orElse(null);
        if (profile == null) {
            return null;
        }

        // 32-byte cryptographically secure token, Base64URL encoded (no padding)
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        PasswordResetToken resetToken = new PasswordResetToken(profile.getId(), PasswordResetToken.DEFAULT_EXPIRY_MS);
        resetToken.setToken(token);
        resetTokenRepository.save(resetToken);

        return token;
    }

    /**
     * Complete a password reset using a one-time token.
     *
     * <p>
     * All failure cases (token not found, expired, already used) throw
     * {@link InvalidResetTokenException} with the same message to prevent
     * information leakage. Constant-time byte comparison is used to mitigate timing
     * oracle attacks after fetching the token from the database.
     *
     * @param token
     *            the reset token
     * @param newPassword
     *            the new password (plain text; will be BCrypt hashed)
     * @throws InvalidResetTokenException
     *             if the token is invalid, expired, or already used
     */
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken stored = resetTokenRepository.findByToken(token).orElse(null);

        if (stored == null) {
            throw new InvalidResetTokenException();
        }

        // Constant-time comparison mitigates oracle attacks on the token value itself
        boolean tokenMatches = MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),
                stored.getToken().getBytes(StandardCharsets.UTF_8));

        if (!tokenMatches || stored.getExpiryDate().isBefore(Instant.now()) || stored.isUsed()) {
            throw new InvalidResetTokenException();
        }

        // Mark single-use token as consumed
        stored.markAsUsed();
        resetTokenRepository.save(stored);

        // Update password
        OnlineProfile profile = profileRepository.findById(stored.getProfileId())
                .orElseThrow(InvalidResetTokenException::new);
        profile.setPasswordHash(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        profileRepository.save(profile);
    }
}
