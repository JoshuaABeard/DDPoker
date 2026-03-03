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
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;

import com.donohoedigital.games.poker.gameserver.auth.JwtTokenProvider;
import com.donohoedigital.games.poker.gameserver.dto.LoginResponse;
import com.donohoedigital.games.poker.gameserver.dto.ProfileResponse;
import com.donohoedigital.games.poker.gameserver.dto.RequestEmailChangeResponse;
import com.donohoedigital.games.poker.gameserver.dto.ResendVerificationResponse;
import com.donohoedigital.games.poker.gameserver.dto.VerifyEmailResponse;
import com.donohoedigital.games.poker.model.PasswordResetToken;
import com.donohoedigital.games.poker.gameserver.persistence.repository.OnlineProfileRepository;
import com.donohoedigital.games.poker.gameserver.persistence.repository.PasswordResetTokenRepository;
import com.donohoedigital.games.poker.model.OnlineProfile;

/**
 * Service for authentication operations (login, registration, password reset).
 */
@Service
public class AuthService {

    private static final Logger log = LogManager.getLogger(AuthService.class);

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

    /** WS token: 60 seconds TTL (initial connection only). */
    static final long WS_TOKEN_TTL_MS = 60_000L;

    /** Reconnect token: 24 hours TTL. */
    static final long RECONNECT_TOKEN_TTL_MS = 24L * 60 * 60 * 1000;

    /** Observe token: 4 hours TTL. */
    static final long OBSERVE_TOKEN_TTL_MS = 4L * 60 * 60 * 1000;

    /** WS token rate limit: max 5 requests per user per minute. */
    static final int WS_TOKEN_RATE_LIMIT = 5;
    static final long WS_TOKEN_RATE_WINDOW_MS = 60_000L;

    private final OnlineProfileRepository profileRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final BanService banService;
    private final JwtTokenProvider tokenProvider;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    /** In-memory rate limit: normalized email → last request time (epoch ms). */
    private final ConcurrentHashMap<String, Long> forgotPasswordRateLimits = new ConcurrentHashMap<>();

    /**
     * WS token rate limit tracking: profileId → list of request timestamps (epoch
     * ms). Each list contains timestamps within the current sliding window.
     */
    private final ConcurrentHashMap<Long, ArrayList<Long>> wsTokenRateLimits = new ConcurrentHashMap<>();

    /**
     * Used jti set for single-use WS connect tokens: jti → expiry epoch ms. Entries
     * are TTL-evicted lazily on each access.
     */
    private final ConcurrentHashMap<String, Long> usedJtis = new ConcurrentHashMap<>();

    public AuthService(OnlineProfileRepository profileRepository, PasswordResetTokenRepository resetTokenRepository,
            BanService banService, JwtTokenProvider tokenProvider, EmailService emailService) {
        this.profileRepository = profileRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.banService = banService;
        this.tokenProvider = tokenProvider;
        this.emailService = emailService;
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
        // Password strength validation
        if (password.length() < 8 || password.length() > 128) {
            return new LoginResponse(false, null, null, null, null, false, "Password must be 8-128 characters", null);
        }
        if (banService.isEmailBanned(email)) {
            return new LoginResponse(false, null, null, null, null, false, "This email address is banned", null);
        }
        if (profileRepository.existsByName(username)) {
            return new LoginResponse(false, null, null, null, null, false, "Username already exists", null);
        }
        if (profileRepository.existsByEmail(email)) {
            return new LoginResponse(false, null, null, null, null, false, "Email already in use", null);
        }

        OnlineProfile profile = new OnlineProfile();
        profile.setName(username);
        profile.setEmail(email);
        profile.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt()));
        profile.setUuid(java.util.UUID.randomUUID().toString());
        profile = profileRepository.save(profile);

        // Generate verification token
        String verificationToken = generateVerificationToken();
        long expiry = System.currentTimeMillis() + VERIFICATION_TOKEN_TTL_MS;
        profile.setEmailVerificationToken(verificationToken);
        profile.setEmailVerificationTokenExpiry(expiry);
        profileRepository.save(profile);

        // Send verification email (non-blocking — if email fails, registration still
        // succeeds)
        try {
            emailService.sendVerificationEmail(email, username, verificationToken);
        } catch (Exception e) {
            log.warn("Failed to send verification email to {}: {}", email, e.getMessage());
        }

        // New registrations are always unverified
        String token = tokenProvider.generateToken(username, profile.getId(), false, false);
        return new LoginResponse(true, token, profile.getId(), username, email, false, null, null);
    }

    private String generateVerificationToken() {
        byte[] bytes = new byte[48]; // 48 bytes → 64 Base64URL chars
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Lock durations in ms indexed by lockoutCount (1-based; index 0 unused). */
    static final long[] LOCK_DURATIONS_MS = {0L, // index 0: unused
            5L * 60 * 1000, // 1st lockout: 5 minutes
            15L * 60 * 1000, // 2nd lockout: 15 minutes
            60L * 60 * 1000 // 3rd lockout: 1 hour
            // 4th+ lockout: Long.MAX_VALUE (requires admin unlock)
    };

    /**
     * Returns the lock duration in ms for the given lockout count.
     *
     * <p>
     * Counts 1–3 use progressive durations. Count 4 and above produce
     * {@code Long.MAX_VALUE}, requiring admin intervention to unlock.
     */
    static long getLockDuration(int lockoutCount) {
        if (lockoutCount >= LOCK_DURATIONS_MS.length) {
            return Long.MAX_VALUE;
        }
        return LOCK_DURATIONS_MS[lockoutCount];
    }

    /**
     * Authenticate user and generate JWT token.
     *
     * <p>
     * Enforces progressive account lockout on repeated failed attempts. After 5
     * failures the account is locked for an escalating duration. Returns a locked
     * response (with {@code retryAfterSeconds} set) if the account is currently
     * locked.
     *
     * @param username
     *            the username
     * @param password
     *            the plain text password
     * @param rememberMe
     *            whether to use extended token expiration
     * @return login response with token if successful, or failure/locked response
     */
    public LoginResponse login(String username, String password, boolean rememberMe) {
        OnlineProfile profile = profileRepository.findByName(username).orElse(null);
        if (profile == null) {
            return new LoginResponse(false, null, null, null, null, false, "Invalid username or password", null);
        }

        long now = System.currentTimeMillis();

        // STEP 1: Check if account is currently locked
        if (profile.getLockedUntil() != null && now < profile.getLockedUntil()) {
            long retryAfterSeconds = (profile.getLockedUntil() - now) / 1000;
            return new LoginResponse(false, null, null, null, null, false, "Account is locked", retryAfterSeconds);
        }

        // STEP 2: Auto-unlock if lock period has elapsed — clear lockedUntil, keep
        // lockoutCount
        if (profile.getLockedUntil() != null) {
            profile.setLockedUntil(null);
            profileRepository.save(profile);
        }

        // STEP 3/4: Validate password
        if (!BCrypt.checkpw(password, profile.getPasswordHash())) {
            // Failed password — increment counter and possibly lock
            profile.setFailedLoginAttempts(profile.getFailedLoginAttempts() + 1);
            if (profile.getFailedLoginAttempts() >= 5) {
                profile.setLockoutCount(profile.getLockoutCount() + 1);
                long lockDurationMs = getLockDuration(profile.getLockoutCount());
                long lockedUntil = lockDurationMs == Long.MAX_VALUE ? Long.MAX_VALUE : now + lockDurationMs;
                profile.setLockedUntil(lockedUntil);
                profile.setFailedLoginAttempts(0);
                profileRepository.save(profile);
                long retryAfterSeconds = lockDurationMs == Long.MAX_VALUE
                        ? Long.MAX_VALUE / 1000
                        : lockDurationMs / 1000;
                return new LoginResponse(false, null, null, null, null, false, "Account is locked", retryAfterSeconds);
            }
            profileRepository.save(profile);
            return new LoginResponse(false, null, null, null, null, false, "Invalid username or password", null);
        }

        // Password correct — check retired/banned before resetting counters
        if (profile.isRetired()) {
            return new LoginResponse(false, null, null, null, null, false, "This account has been retired", null);
        }
        if (banService.isProfileBanned(profile.getId())) {
            return new LoginResponse(false, null, null, null, null, false, "This account is banned", null);
        }

        // STEP 3 success: reset counters
        profile.setFailedLoginAttempts(0);
        profile.setLockoutCount(0);
        profile.setLockedUntil(null);
        profileRepository.save(profile);

        boolean emailVerified = profile.isEmailVerified();
        String token = tokenProvider.generateToken(username, profile.getId(), rememberMe, emailVerified);
        return new LoginResponse(true, token, profile.getId(), username, profile.getEmail(), emailVerified, null, null);
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

    /**
     * Generate a short-lived WebSocket connect token for the given user.
     *
     * <p>
     * Rate-limited to {@link #WS_TOKEN_RATE_LIMIT} requests per user per minute.
     * Returns {@code null} if rate-limited.
     *
     * @param profileId
     *            authenticated user's profile ID
     * @param username
     *            authenticated user's username
     * @return a {@code ws-connect}-scoped JWT, or {@code null} if rate-limited
     */
    public String generateWsToken(Long profileId, String username) {
        long now = System.currentTimeMillis();

        // Sliding-window rate limit: 5 requests per 60s per user.
        // Check + record in a single atomic compute() to prevent TOCTOU races.
        boolean[] rateLimited = {false};
        wsTokenRateLimits.compute(profileId, (id, timestamps) -> {
            if (timestamps == null) {
                timestamps = new ArrayList<>();
            }
            // Evict stale entries outside the window
            long windowStart = now - WS_TOKEN_RATE_WINDOW_MS;
            timestamps.removeIf(ts -> ts < windowStart);

            if (timestamps.size() >= WS_TOKEN_RATE_LIMIT) {
                rateLimited[0] = true;
            } else {
                timestamps.add(now);
            }
            return timestamps;
        });

        if (rateLimited[0]) {
            return null;
        }

        return tokenProvider.generateScopedToken(username, profileId, "ws-connect", null, WS_TOKEN_TTL_MS);
    }

    /**
     * Generate a game-scoped reconnect token for inclusion in the CONNECTED
     * message.
     *
     * <p>
     * The reconnect token has a 24-hour TTL and is scoped to a single game. It is
     * stored in memory by the client and used for WebSocket reconnection without
     * requiring a valid session cookie.
     *
     * @param profileId
     *            the player's profile ID
     * @param username
     *            the player's username
     * @param gameId
     *            the game this token is scoped to
     * @return a {@code reconnect}-scoped JWT
     */
    public String generateReconnectToken(Long profileId, String username, String gameId) {
        return tokenProvider.generateScopedToken(username, profileId, "reconnect", gameId, RECONNECT_TOKEN_TTL_MS);
    }

    /**
     * Generate a game-scoped observe token for spectating a game.
     *
     * @param profileId
     *            the observer's profile ID
     * @param username
     *            the observer's username
     * @param gameId
     *            the game to observe
     * @return an {@code observe}-scoped JWT
     */
    public String generateObserveToken(Long profileId, String username, String gameId) {
        return tokenProvider.generateScopedToken(username, profileId, "observe", gameId, OBSERVE_TOKEN_TTL_MS);
    }

    /**
     * Mark a WS connect token's {@code jti} as used.
     *
     * <p>
     * Lazily evicts expired entries before recording the new jti.
     *
     * @param jti
     *            JWT ID claim from the token
     * @param expiryMs
     *            token expiry as epoch milliseconds
     */
    public void markJtiUsed(String jti, long expiryMs) {
        // Lazy eviction of expired jtis
        long now = System.currentTimeMillis();
        for (Iterator<Map.Entry<String, Long>> it = usedJtis.entrySet().iterator(); it.hasNext();) {
            if (it.next().getValue() < now) {
                it.remove();
            }
        }
        usedJtis.put(jti, expiryMs);
    }

    /**
     * Returns true if the given jti has already been used.
     *
     * @param jti
     *            JWT ID claim
     * @return true if already used or expired
     */
    public boolean isJtiUsed(String jti) {
        Long expiry = usedJtis.get(jti);
        if (expiry == null) {
            return false;
        }
        if (expiry < System.currentTimeMillis()) {
            usedJtis.remove(jti);
            return false;
        }
        return true;
    }

    /** Verification token TTL: 7 days in milliseconds. */
    static final long VERIFICATION_TOKEN_TTL_MS = 7L * 24 * 60 * 60 * 1000;

    /** Resend rate limit: 1 per 5 minutes. */
    static final long RESEND_RATE_LIMIT_MS = 5L * 60 * 1000;

    /**
     * Verify an email address using a one-time verification token.
     *
     * <p>
     * If the profile has a {@code pendingEmail}, this is treated as an email-change
     * confirmation: the pending email becomes the canonical email and
     * {@code pendingEmail} is cleared. Otherwise the profile's existing email is
     * simply marked as verified.
     *
     * <p>
     * On success a fresh JWT with {@code emailVerified=true} is returned.
     *
     * @param token
     *            the email verification token
     * @return response with success status, fresh JWT (on success), and optional
     *         error message
     */
    public VerifyEmailResponse verifyEmail(String token) {
        OnlineProfile profile = profileRepository.findByEmailVerificationToken(token).orElse(null);
        if (profile == null) {
            return new VerifyEmailResponse(false, null, "Invalid verification token");
        }

        Long expiry = profile.getEmailVerificationTokenExpiry();
        if (expiry == null || System.currentTimeMillis() > expiry) {
            return new VerifyEmailResponse(false, null, "Verification token has expired");
        }

        // Email-change confirmation: swap pending email into canonical email field
        if (profile.getPendingEmail() != null) {
            profile.setEmail(profile.getPendingEmail());
            profile.setPendingEmail(null);
        }

        profile.setEmailVerified(true);
        profile.setEmailVerificationToken(null);
        profile.setEmailVerificationTokenExpiry(null);
        profileRepository.save(profile);

        String freshToken = tokenProvider.generateToken(profile.getName(), profile.getId(), false, true);
        return new VerifyEmailResponse(true, freshToken, null);
    }

    /**
     * Resend the email verification message for an authenticated user.
     *
     * <p>
     * Rate-limited to one resend per 5 minutes. Returns an error if the email is
     * already verified.
     *
     * @param username
     *            the authenticated user's username (from JWT)
     * @return response with success status and optional error message
     */
    public ResendVerificationResponse resendVerification(String username) {
        OnlineProfile profile = profileRepository.findByName(username).orElse(null);
        if (profile == null) {
            return new ResendVerificationResponse(false, "Profile not found");
        }

        if (profile.isEmailVerified()) {
            return new ResendVerificationResponse(false, "Email already verified");
        }

        // Rate limit: allow at most 1 resend per 5 minutes.
        // The token expiry encodes when the token was issued:
        // issuedAt = expiry - VERIFICATION_TOKEN_TTL_MS
        if (profile.getEmailVerificationTokenExpiry() != null) {
            long issuedAt = profile.getEmailVerificationTokenExpiry() - VERIFICATION_TOKEN_TTL_MS;
            if (System.currentTimeMillis() < issuedAt + RESEND_RATE_LIMIT_MS) {
                return new ResendVerificationResponse(false,
                        "Please wait before requesting another verification email");
            }
        }

        String verificationToken = generateVerificationToken();
        long expiry = System.currentTimeMillis() + VERIFICATION_TOKEN_TTL_MS;
        profile.setEmailVerificationToken(verificationToken);
        profile.setEmailVerificationTokenExpiry(expiry);
        profileRepository.save(profile);

        String targetEmail = profile.getPendingEmail() != null ? profile.getPendingEmail() : profile.getEmail();
        try {
            emailService.sendVerificationEmail(targetEmail, username, verificationToken);
        } catch (Exception e) {
            log.warn("Failed to send verification email to {}: {}", targetEmail, e.getMessage());
        }

        return new ResendVerificationResponse(true, "Verification email sent");
    }

    /**
     * Initiate an email address change for an authenticated user.
     *
     * <p>
     * Validates the new email, checks it is not already claimed by another account
     * (either as a confirmed email or as a pending email), sets it as the user's
     * pending email, and sends a confirmation link to the new address.
     *
     * <p>
     * Sending the confirmation email is non-fatal: if it fails, the method still
     * returns success so the pending email and verification token are committed.
     *
     * @param username
     *            the authenticated user's username (from JWT)
     * @param newEmail
     *            the new email address the user wants to use
     * @return response with success status and optional error message
     */
    public RequestEmailChangeResponse requestEmailChange(String username, String newEmail) {
        OnlineProfile profile = profileRepository.findByName(username).orElse(null);
        if (profile == null) {
            return new RequestEmailChangeResponse(false, "Profile not found");
        }

        // Basic format validation: non-blank and contains '@'
        if (newEmail == null || newEmail.isBlank() || !newEmail.contains("@")) {
            return new RequestEmailChangeResponse(false, "Invalid email address");
        }

        // Reject if new email is the same as the current confirmed email
        if (newEmail.equalsIgnoreCase(profile.getEmail())) {
            return new RequestEmailChangeResponse(false, "New email must be different from current email");
        }

        // Check if another account already has this as their confirmed email
        if (profileRepository.findByEmail(newEmail).isPresent()) {
            return new RequestEmailChangeResponse(false, "Email already in use");
        }

        // Check if another account already has this as their pending email
        if (profileRepository.findByPendingEmail(newEmail).isPresent()) {
            return new RequestEmailChangeResponse(false, "Email already in use");
        }

        String token = generateVerificationToken();
        long expiry = System.currentTimeMillis() + VERIFICATION_TOKEN_TTL_MS;

        profile.setPendingEmail(newEmail);
        profile.setEmailVerificationToken(token);
        profile.setEmailVerificationTokenExpiry(expiry);
        profileRepository.save(profile);

        try {
            emailService.sendEmailChangeConfirmation(newEmail, username, token);
        } catch (Exception e) {
            log.warn("Failed to send email change confirmation to {}: {}", newEmail, e.getMessage());
        }

        return new RequestEmailChangeResponse(true, "Confirmation email sent to new address");
    }
}
