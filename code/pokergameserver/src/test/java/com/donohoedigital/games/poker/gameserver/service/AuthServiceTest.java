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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

import com.donohoedigital.games.poker.gameserver.auth.JwtTokenProvider;
import com.donohoedigital.games.poker.protocol.dto.LoginResponse;
import com.donohoedigital.games.poker.protocol.dto.ProfileResponse;
import com.donohoedigital.games.poker.protocol.dto.RequestEmailChangeResponse;
import com.donohoedigital.games.poker.protocol.dto.ResendVerificationResponse;
import com.donohoedigital.games.poker.protocol.dto.VerifyEmailResponse;
import com.donohoedigital.games.poker.model.PasswordResetToken;
import com.donohoedigital.games.poker.gameserver.persistence.TestJpaConfiguration;
import com.donohoedigital.games.poker.gameserver.persistence.repository.BanRepository;
import com.donohoedigital.games.poker.gameserver.persistence.repository.OnlineProfileRepository;
import com.donohoedigital.games.poker.gameserver.persistence.repository.PasswordResetTokenRepository;
import com.donohoedigital.games.poker.model.OnlineProfile;

import java.nio.file.Path;
import java.security.KeyPair;

@DataJpaTest
@ContextConfiguration(classes = {TestJpaConfiguration.class, AuthServiceTest.TestConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AuthServiceTest {

    @Autowired
    private OnlineProfileRepository profileRepository;

    @Autowired
    private BanRepository banRepository;

    @Autowired
    private PasswordResetTokenRepository resetTokenRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @org.springframework.context.annotation.Configuration
    static class TestConfig {
        @org.springframework.context.annotation.Bean
        public JwtTokenProvider jwtTokenProvider() throws Exception {
            KeyPair keyPair = com.donohoedigital.games.poker.gameserver.auth.JwtKeyManager.generateKeyPair();
            String uniqueId = java.util.UUID.randomUUID().toString();
            Path privateKey = Path.of(System.getProperty("java.io.tmpdir"), "test-jwt-private-" + uniqueId + ".pem");
            Path publicKey = Path.of(System.getProperty("java.io.tmpdir"), "test-jwt-public-" + uniqueId + ".pem");

            com.donohoedigital.games.poker.gameserver.auth.JwtKeyManager.savePrivateKey(keyPair.getPrivate(),
                    privateKey);
            com.donohoedigital.games.poker.gameserver.auth.JwtKeyManager.savePublicKey(keyPair.getPublic(), publicKey);

            return new JwtTokenProvider(privateKey, publicKey, 3600000L, 604800000L);
        }

        @org.springframework.context.annotation.Bean
        public BanService banService(BanRepository banRepository) {
            return new BanService(banRepository);
        }

        @org.springframework.context.annotation.Bean
        public EmailService emailService() {
            return mock(EmailService.class);
        }

        @org.springframework.context.annotation.Bean
        public AuthService authService(OnlineProfileRepository profileRepository,
                PasswordResetTokenRepository resetTokenRepository, BanService banService,
                JwtTokenProvider tokenProvider, EmailService emailService) {
            return new AuthService(profileRepository, resetTokenRepository, banService, tokenProvider, emailService);
        }
    }

    @Test
    void testRegisterNewProfile() {
        LoginResponse response = authService.register("newuser", "password123", "new@example.com");

        assertThat(response.success()).isTrue();
        assertThat(response.token()).isNotNull();
        assertThat(response.profile()).isNotNull();
        assertThat(response.profile().id()).isNotNull();

        // Verify profile was created
        OnlineProfile profile = profileRepository.findByName("newuser").orElse(null);
        assertThat(profile).isNotNull();
        assertThat(profile.getEmail()).isEqualTo("new@example.com");
    }

    @Test
    void testRegisterDuplicateUsername() {
        // Create existing profile
        OnlineProfile existing = new OnlineProfile();
        existing.setName("existing");
        existing.setEmail("existing@example.com");
        existing.setPasswordHash(org.mindrot.jbcrypt.BCrypt.hashpw("password", org.mindrot.jbcrypt.BCrypt.gensalt()));
        existing.setUuid(java.util.UUID.randomUUID().toString());
        profileRepository.save(existing);

        LoginResponse response = authService.register("existing", "newpass1!", "different@example.com");

        assertThat(response.success()).isFalse();
        assertThat(response.message()).contains("already exists");
    }

    @Test
    void testLoginSuccess() {
        // Create profile with BCrypt password
        OnlineProfile profile = new OnlineProfile();
        profile.setName("testuser");
        profile.setEmail("test@example.com");
        profile.setPasswordHash(org.mindrot.jbcrypt.BCrypt.hashpw("password123", org.mindrot.jbcrypt.BCrypt.gensalt()));
        profile.setUuid(java.util.UUID.randomUUID().toString());
        profileRepository.save(profile);

        LoginResponse response = authService.login("testuser", "password123", false);

        assertThat(response.success()).isTrue();
        assertThat(response.token()).isNotNull();
        assertThat(response.profile().id()).isEqualTo(profile.getId());
        assertThat(response.profile().username()).isEqualTo("testuser");
    }

    @Test
    void testLoginWrongPassword() {
        OnlineProfile profile = new OnlineProfile();
        profile.setName("testuser");
        profile.setEmail("test@example.com");
        profile.setPasswordHash(org.mindrot.jbcrypt.BCrypt.hashpw("correctpass", org.mindrot.jbcrypt.BCrypt.gensalt()));
        profile.setUuid(java.util.UUID.randomUUID().toString());
        profileRepository.save(profile);

        LoginResponse response = authService.login("testuser", "wrongpass", false);

        assertThat(response.success()).isFalse();
        assertThat(response.message()).contains("Invalid");
    }

    @Test
    void testLoginNonexistentUser() {
        LoginResponse response = authService.login("nonexistent", "password", false);

        assertThat(response.success()).isFalse();
        assertThat(response.message()).contains("Invalid");
    }

    @Test
    void testLoginBannedProfile() {
        // Create profile
        OnlineProfile profile = new OnlineProfile();
        profile.setName("banned");
        profile.setEmail("banned@example.com");
        profile.setPasswordHash(org.mindrot.jbcrypt.BCrypt.hashpw("password", org.mindrot.jbcrypt.BCrypt.gensalt()));
        profile.setUuid(java.util.UUID.randomUUID().toString());
        profileRepository.save(profile);

        // Ban the profile
        com.donohoedigital.games.poker.gameserver.persistence.entity.BanEntity ban = new com.donohoedigital.games.poker.gameserver.persistence.entity.BanEntity();
        ban.setBanType(com.donohoedigital.games.poker.gameserver.persistence.entity.BanEntity.BanType.PROFILE);
        ban.setProfileId(profile.getId());
        ban.setUntil(java.time.LocalDate.now().plusDays(30));
        ban.setReason("Test ban");
        banRepository.save(ban);

        LoginResponse response = authService.login("banned", "password", false);

        assertThat(response.success()).isFalse();
        assertThat(response.message()).contains("banned");
    }

    @Test
    void testRegisterBannedEmail() {
        // Ban an email
        com.donohoedigital.games.poker.gameserver.persistence.entity.BanEntity ban = new com.donohoedigital.games.poker.gameserver.persistence.entity.BanEntity();
        ban.setBanType(com.donohoedigital.games.poker.gameserver.persistence.entity.BanEntity.BanType.EMAIL);
        ban.setEmail("banned@example.com");
        ban.setUntil(java.time.LocalDate.now().plusDays(30));
        ban.setReason("Email ban");
        banRepository.save(ban);

        LoginResponse response = authService.register("newuser", "password123", "banned@example.com");

        assertThat(response.success()).isFalse();
        assertThat(response.message()).contains("banned");
    }

    // -------------------------------------------------------------------------
    // Lockout tests
    // -------------------------------------------------------------------------

    private OnlineProfile createProfile(String username, String password) {
        OnlineProfile profile = new OnlineProfile();
        profile.setName(username);
        profile.setEmail(username + "@example.com");
        profile.setPasswordHash(org.mindrot.jbcrypt.BCrypt.hashpw(password, org.mindrot.jbcrypt.BCrypt.gensalt()));
        profile.setUuid(java.util.UUID.randomUUID().toString());
        return profileRepository.save(profile);
    }

    @Test
    void login_whenLockedAndTimeHasNotExpired_returns423WithRetryAfter() {
        OnlineProfile profile = createProfile("lockeduser", "pass");
        // Set a lock 5 minutes in the future
        long lockedUntil = System.currentTimeMillis() + 5 * 60 * 1000;
        profile.setLockedUntil(lockedUntil);
        profile.setLockoutCount(1);
        profileRepository.save(profile);

        LoginResponse response = authService.login("lockeduser", "pass", false);

        assertThat(response.success()).isFalse();
        assertThat(response.retryAfterSeconds()).isNotNull();
        assertThat(response.retryAfterSeconds()).isGreaterThan(0L);
        assertThat(response.retryAfterSeconds()).isLessThanOrEqualTo(300L);
        assertThat(response.message()).contains("locked");
    }

    @Test
    void login_whenLockedAndTimeExpired_unlocksAndProceedsToPasswordCheck() {
        OnlineProfile profile = createProfile("expiredlockuser", "pass");
        // Set a lock in the past (already expired)
        profile.setLockedUntil(System.currentTimeMillis() - 1000);
        profile.setLockoutCount(1);
        profileRepository.save(profile);

        // Correct password should succeed after expired lock
        LoginResponse response = authService.login("expiredlockuser", "pass", false);

        assertThat(response.success()).isTrue();
        assertThat(response.retryAfterSeconds()).isNull();

        // Verify lockedUntil was cleared, lockoutCount reset on success
        OnlineProfile updated = profileRepository.findByName("expiredlockuser").orElseThrow();
        assertThat(updated.getLockedUntil()).isNull();
        assertThat(updated.getFailedLoginAttempts()).isEqualTo(0);
        assertThat(updated.getLockoutCount()).isEqualTo(0);
    }

    @Test
    void login_whenLockedAndTimeExpired_wrongPassword_incrementsFailedAttempts() {
        OnlineProfile profile = createProfile("expiredlockwrong", "correctpass");
        // Set a lock in the past (already expired)
        profile.setLockedUntil(System.currentTimeMillis() - 1000);
        profile.setLockoutCount(1);
        profileRepository.save(profile);

        // Wrong password after expired lock — should fail normally (not locked
        // response)
        LoginResponse response = authService.login("expiredlockwrong", "wrongpass", false);

        assertThat(response.success()).isFalse();
        assertThat(response.retryAfterSeconds()).isNull();
        assertThat(response.message()).contains("Invalid");

        OnlineProfile updated = profileRepository.findByName("expiredlockwrong").orElseThrow();
        assertThat(updated.getLockedUntil()).isNull();
        assertThat(updated.getFailedLoginAttempts()).isEqualTo(1);
    }

    @Test
    void login_wrongPassword_incrementsFailedAttempts() {
        OnlineProfile profile = createProfile("failuser", "correctpass");

        LoginResponse response = authService.login("failuser", "wrongpass", false);

        assertThat(response.success()).isFalse();
        assertThat(response.retryAfterSeconds()).isNull();

        OnlineProfile updated = profileRepository.findByName("failuser").orElseThrow();
        assertThat(updated.getFailedLoginAttempts()).isEqualTo(1);
        assertThat(updated.getLockedUntil()).isNull();
    }

    @Test
    void login_5thFailure_locksAccountAndSetsLockoutCount() {
        OnlineProfile profile = createProfile("fivestrikes", "correctpass");
        // Pre-set 4 failed attempts
        profile.setFailedLoginAttempts(4);
        profileRepository.save(profile);

        LoginResponse response = authService.login("fivestrikes", "wrongpass", false);

        assertThat(response.success()).isFalse();
        assertThat(response.retryAfterSeconds()).isNotNull();
        assertThat(response.retryAfterSeconds()).isEqualTo(300L); // 5 minutes for 1st lockout
        assertThat(response.message()).contains("locked");

        OnlineProfile updated = profileRepository.findByName("fivestrikes").orElseThrow();
        assertThat(updated.getLockoutCount()).isEqualTo(1);
        assertThat(updated.getFailedLoginAttempts()).isEqualTo(0);
        assertThat(updated.getLockedUntil()).isNotNull();
        assertThat(updated.getLockedUntil()).isGreaterThan(System.currentTimeMillis());
    }

    @Test
    void login_successAfterFailures_resetsCounters() {
        OnlineProfile profile = createProfile("resetuser", "correctpass");
        profile.setFailedLoginAttempts(3);
        profile.setLockoutCount(1);
        profileRepository.save(profile);

        LoginResponse response = authService.login("resetuser", "correctpass", false);

        assertThat(response.success()).isTrue();
        assertThat(response.retryAfterSeconds()).isNull();

        OnlineProfile updated = profileRepository.findByName("resetuser").orElseThrow();
        assertThat(updated.getFailedLoginAttempts()).isEqualTo(0);
        assertThat(updated.getLockoutCount()).isEqualTo(0);
        assertThat(updated.getLockedUntil()).isNull();
    }

    @Test
    void login_4thLockout_setsMaxValue() {
        OnlineProfile profile = createProfile("maxlockuser", "correctpass");
        // 3 prior lockouts; 4th attempt with 4 failed so far triggers lockout
        profile.setFailedLoginAttempts(4);
        profile.setLockoutCount(3);
        profileRepository.save(profile);

        LoginResponse response = authService.login("maxlockuser", "wrongpass", false);

        assertThat(response.success()).isFalse();
        assertThat(response.retryAfterSeconds()).isNotNull();

        OnlineProfile updated = profileRepository.findByName("maxlockuser").orElseThrow();
        assertThat(updated.getLockoutCount()).isEqualTo(4);
        assertThat(updated.getLockedUntil()).isEqualTo(Long.MAX_VALUE);
        assertThat(updated.getFailedLoginAttempts()).isEqualTo(0);
    }

    @Test
    void login_progressiveLockDurations_correctMapping() {
        assertThat(AuthService.getLockDuration(1)).isEqualTo(5L * 60 * 1000);
        assertThat(AuthService.getLockDuration(2)).isEqualTo(15L * 60 * 1000);
        assertThat(AuthService.getLockDuration(3)).isEqualTo(60L * 60 * 1000);
        assertThat(AuthService.getLockDuration(4)).isEqualTo(Long.MAX_VALUE);
        assertThat(AuthService.getLockDuration(10)).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void login_2ndLockout_uses15Minutes() {
        OnlineProfile profile = createProfile("secondlockuser", "correctpass");
        // 1 prior lockout; 5th failure triggers 2nd lockout
        profile.setFailedLoginAttempts(4);
        profile.setLockoutCount(1);
        profileRepository.save(profile);

        LoginResponse response = authService.login("secondlockuser", "wrongpass", false);

        assertThat(response.success()).isFalse();
        assertThat(response.retryAfterSeconds()).isEqualTo(900L); // 15 minutes

        OnlineProfile updated = profileRepository.findByName("secondlockuser").orElseThrow();
        assertThat(updated.getLockoutCount()).isEqualTo(2);
    }

    @Test
    void login_3rdLockout_uses1Hour() {
        OnlineProfile profile = createProfile("thirdlockuser", "correctpass");
        // 2 prior lockouts; 5th failure triggers 3rd lockout
        profile.setFailedLoginAttempts(4);
        profile.setLockoutCount(2);
        profileRepository.save(profile);

        LoginResponse response = authService.login("thirdlockuser", "wrongpass", false);

        assertThat(response.success()).isFalse();
        assertThat(response.retryAfterSeconds()).isEqualTo(3600L); // 1 hour

        OnlineProfile updated = profileRepository.findByName("thirdlockuser").orElseThrow();
        assertThat(updated.getLockoutCount()).isEqualTo(3);
    }

    // -------------------------------------------------------------------------
    // Password strength and email verification tests
    // -------------------------------------------------------------------------

    @Test
    void register_withShortPassword_returnsError() {
        LoginResponse response = authService.register("shortpwuser", "abc1234", "shortpw@example.com");

        assertThat(response.success()).isFalse();
        assertThat(response.message()).contains("8-128");
    }

    @Test
    void register_withLongPassword_returnsError() {
        String longPassword = "a".repeat(129);
        LoginResponse response = authService.register("longpwuser", longPassword, "longpw@example.com");

        assertThat(response.success()).isFalse();
        assertThat(response.message()).contains("8-128");
    }

    @Test
    void register_withValidPassword_storesVerificationToken() {
        long beforeMs = System.currentTimeMillis();

        authService.register("tokenuser", "validpass1", "tokenuser@example.com");

        OnlineProfile profile = profileRepository.findByName("tokenuser").orElseThrow();
        assertThat(profile.getEmailVerificationToken()).isNotNull();
        assertThat(profile.getEmailVerificationToken()).hasSize(64);
        assertThat(profile.getEmailVerificationTokenExpiry()).isNotNull();
        long expectedExpiry = beforeMs + (7L * 24 * 60 * 60 * 1000);
        assertThat(profile.getEmailVerificationTokenExpiry()).isBetween(expectedExpiry - 5000, expectedExpiry + 5000);
    }

    @Test
    void register_withValidPassword_callsEmailService() {
        authService.register("emailcalluser", "validpass2", "emailcall@example.com");

        OnlineProfile profile = profileRepository.findByName("emailcalluser").orElseThrow();
        verify(emailService).sendVerificationEmail("emailcall@example.com", "emailcalluser",
                profile.getEmailVerificationToken());
    }

    @Test
    void register_withValidPassword_returnsEmailVerifiedFalse() {
        LoginResponse response = authService.register("verifyfalseuser", "validpass3", "verifyfalse@example.com");

        assertThat(response.success()).isTrue();
        assertThat(response.profile().emailVerified()).isFalse();
    }

    @Test
    void register_whenEmailServiceFails_registrationStillSucceeds() {
        doThrow(new RuntimeException("SMTP connection refused")).when(emailService).sendVerificationEmail(anyString(),
                anyString(), anyString());

        LoginResponse response = authService.register("emailfailuser", "validpass4", "emailfail@example.com");

        assertThat(response.success()).isTrue();
        assertThat(response.token()).isNotNull();
    }

    // -------------------------------------------------------------------------
    // verifyEmail tests
    // -------------------------------------------------------------------------

    private OnlineProfile createUnverifiedProfileWithToken(String username, String email) {
        OnlineProfile profile = new OnlineProfile();
        profile.setName(username);
        profile.setEmail(email);
        profile.setPasswordHash(org.mindrot.jbcrypt.BCrypt.hashpw("pass", org.mindrot.jbcrypt.BCrypt.gensalt()));
        profile.setUuid(java.util.UUID.randomUUID().toString());
        profile.setEmailVerified(false);
        profile.setEmailVerificationToken("valid-token-" + username);
        profile.setEmailVerificationTokenExpiry(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000);
        return profileRepository.save(profile);
    }

    @Test
    void verifyEmail_withValidToken_setsEmailVerifiedAndClearsToken() {
        createUnverifiedProfileWithToken("verifyuser1", "verifyuser1@example.com");

        VerifyEmailResponse response = authService.verifyEmail("valid-token-verifyuser1");

        assertThat(response.success()).isTrue();

        OnlineProfile updated = profileRepository.findByName("verifyuser1").orElseThrow();
        assertThat(updated.isEmailVerified()).isTrue();
        assertThat(updated.getEmailVerificationToken()).isNull();
        assertThat(updated.getEmailVerificationTokenExpiry()).isNull();
    }

    @Test
    void verifyEmail_withUnknownToken_returnsError() {
        VerifyEmailResponse response = authService.verifyEmail("nonexistent-token");

        assertThat(response.success()).isFalse();
        assertThat(response.token()).isNull();
        assertThat(response.message()).contains("Invalid");
    }

    @Test
    void verifyEmail_withExpiredToken_returnsError() {
        OnlineProfile profile = new OnlineProfile();
        profile.setName("expiredverifyuser");
        profile.setEmail("expiredverify@example.com");
        profile.setPasswordHash(org.mindrot.jbcrypt.BCrypt.hashpw("pass", org.mindrot.jbcrypt.BCrypt.gensalt()));
        profile.setUuid(java.util.UUID.randomUUID().toString());
        profile.setEmailVerified(false);
        profile.setEmailVerificationToken("expired-token");
        // Set expiry in the past
        profile.setEmailVerificationTokenExpiry(System.currentTimeMillis() - 1000);
        profileRepository.save(profile);

        VerifyEmailResponse response = authService.verifyEmail("expired-token");

        assertThat(response.success()).isFalse();
        assertThat(response.token()).isNull();
        assertThat(response.message()).contains("expired");
    }

    @Test
    void verifyEmail_withNullExpiry_returnsError() {
        OnlineProfile profile = new OnlineProfile();
        profile.setName("nullexpiryuser");
        profile.setEmail("nullexpiry@example.com");
        profile.setPasswordHash(org.mindrot.jbcrypt.BCrypt.hashpw("pass", org.mindrot.jbcrypt.BCrypt.gensalt()));
        profile.setUuid(java.util.UUID.randomUUID().toString());
        profile.setEmailVerified(false);
        profile.setEmailVerificationToken("null-expiry-token");
        profile.setEmailVerificationTokenExpiry(null);
        profileRepository.save(profile);

        VerifyEmailResponse response = authService.verifyEmail("null-expiry-token");

        assertThat(response.success()).isFalse();
        assertThat(response.token()).isNull();
        assertThat(response.message()).contains("expired");
    }

    @Test
    void verifyEmail_withValidToken_returnsFreshJwt() {
        createUnverifiedProfileWithToken("verifyuser2", "verifyuser2@example.com");

        VerifyEmailResponse response = authService.verifyEmail("valid-token-verifyuser2");

        assertThat(response.success()).isTrue();
        assertThat(response.token()).isNotNull();
        assertThat(tokenProvider.getEmailVerifiedFromToken(response.token())).isTrue();
    }

    @Test
    void verifyEmail_withPendingEmail_swapsEmailAndClears() {
        OnlineProfile profile = new OnlineProfile();
        profile.setName("pendingemailuser");
        profile.setEmail("old@example.com");
        profile.setPasswordHash(org.mindrot.jbcrypt.BCrypt.hashpw("pass", org.mindrot.jbcrypt.BCrypt.gensalt()));
        profile.setUuid(java.util.UUID.randomUUID().toString());
        profile.setEmailVerified(true); // already verified; now changing email
        profile.setPendingEmail("new@example.com");
        profile.setEmailVerificationToken("email-change-token");
        profile.setEmailVerificationTokenExpiry(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000);
        profileRepository.save(profile);

        VerifyEmailResponse response = authService.verifyEmail("email-change-token");

        assertThat(response.success()).isTrue();

        OnlineProfile updated = profileRepository.findByName("pendingemailuser").orElseThrow();
        assertThat(updated.getEmail()).isEqualTo("new@example.com");
        assertThat(updated.getPendingEmail()).isNull();
        assertThat(updated.isEmailVerified()).isTrue();
        assertThat(updated.getEmailVerificationToken()).isNull();
    }

    // -------------------------------------------------------------------------
    // resendVerification tests
    // -------------------------------------------------------------------------

    @Test
    void resendVerification_whenUnverified_sendsEmailAndReturnsSuccess() {
        // Create profile with token issued more than 5 minutes ago so rate limit passes
        OnlineProfile profile = new OnlineProfile();
        profile.setName("resenduser1");
        profile.setEmail("resend1@example.com");
        profile.setPasswordHash(org.mindrot.jbcrypt.BCrypt.hashpw("pass", org.mindrot.jbcrypt.BCrypt.gensalt()));
        profile.setUuid(java.util.UUID.randomUUID().toString());
        profile.setEmailVerified(false);
        // Token issued 10 minutes ago → expiry = issuedAt + 7 days
        long issuedAt = System.currentTimeMillis() - 10L * 60 * 1000;
        profile.setEmailVerificationToken("old-token");
        profile.setEmailVerificationTokenExpiry(issuedAt + 7L * 24 * 60 * 60 * 1000);
        profileRepository.save(profile);

        ResendVerificationResponse response = authService.resendVerification("resenduser1");

        assertThat(response.success()).isTrue();
        verify(emailService).sendVerificationEmail(eq("resend1@example.com"), eq("resenduser1"), anyString());
    }

    @Test
    void resendVerification_whenAlreadyVerified_returnsError() {
        OnlineProfile profile = new OnlineProfile();
        profile.setName("alreadyverifieduser");
        profile.setEmail("alreadyverified@example.com");
        profile.setPasswordHash(org.mindrot.jbcrypt.BCrypt.hashpw("pass", org.mindrot.jbcrypt.BCrypt.gensalt()));
        profile.setUuid(java.util.UUID.randomUUID().toString());
        profile.setEmailVerified(true);
        profileRepository.save(profile);

        ResendVerificationResponse response = authService.resendVerification("alreadyverifieduser");

        assertThat(response.success()).isFalse();
        assertThat(response.message()).contains("already verified");
    }

    @Test
    void resendVerification_withinRateLimit_returnsError() {
        // Token issued just now → within 5-minute rate limit window
        OnlineProfile profile = new OnlineProfile();
        profile.setName("ratelimituser");
        profile.setEmail("ratelimit@example.com");
        profile.setPasswordHash(org.mindrot.jbcrypt.BCrypt.hashpw("pass", org.mindrot.jbcrypt.BCrypt.gensalt()));
        profile.setUuid(java.util.UUID.randomUUID().toString());
        profile.setEmailVerified(false);
        long issuedAt = System.currentTimeMillis(); // just now
        profile.setEmailVerificationToken("recent-token");
        profile.setEmailVerificationTokenExpiry(issuedAt + 7L * 24 * 60 * 60 * 1000);
        profileRepository.save(profile);

        ResendVerificationResponse response = authService.resendVerification("ratelimituser");

        assertThat(response.success()).isFalse();
        assertThat(response.message()).isNotBlank();
    }

    @Test
    void resendVerification_afterRateLimitExpiry_succeedsAndSendsEmail() {
        // Token issued 6 minutes ago → rate limit has passed
        OnlineProfile profile = new OnlineProfile();
        profile.setName("afterratelimituser");
        profile.setEmail("afterratelimit@example.com");
        profile.setPasswordHash(org.mindrot.jbcrypt.BCrypt.hashpw("pass", org.mindrot.jbcrypt.BCrypt.gensalt()));
        profile.setUuid(java.util.UUID.randomUUID().toString());
        profile.setEmailVerified(false);
        long issuedAt = System.currentTimeMillis() - 6L * 60 * 1000; // 6 minutes ago
        profile.setEmailVerificationToken("old-token-2");
        profile.setEmailVerificationTokenExpiry(issuedAt + 7L * 24 * 60 * 60 * 1000);
        profileRepository.save(profile);

        ResendVerificationResponse response = authService.resendVerification("afterratelimituser");

        assertThat(response.success()).isTrue();
        verify(emailService).sendVerificationEmail(eq("afterratelimit@example.com"), eq("afterratelimituser"),
                anyString());
    }

    // -------------------------------------------------------------------------
    // isUsernameAvailable tests
    // -------------------------------------------------------------------------

    @Test
    void isUsernameAvailable_whenUserExists_returnsFalse() {
        createProfile("existingusername", "pass");

        assertThat(authService.isUsernameAvailable("existingusername")).isFalse();
    }

    @Test
    void isUsernameAvailable_whenUserDoesNotExist_returnsTrue() {
        assertThat(authService.isUsernameAvailable("nonexistentusername")).isTrue();
    }

    // -------------------------------------------------------------------------
    // requestEmailChange tests
    // -------------------------------------------------------------------------

    @Test
    void requestEmailChange_withValidNewEmail_setsPendingEmailAndSendsConfirmation() {
        OnlineProfile profile = createProfile("emailchangeuser", "pass");

        RequestEmailChangeResponse response = authService.requestEmailChange("emailchangeuser", "newemail@example.com");

        assertThat(response.success()).isTrue();

        OnlineProfile updated = profileRepository.findByName("emailchangeuser").orElseThrow();
        assertThat(updated.getPendingEmail()).isEqualTo("newemail@example.com");
        assertThat(updated.getEmailVerificationToken()).isNotNull();
        assertThat(updated.getEmailVerificationTokenExpiry()).isNotNull();
        assertThat(updated.getEmailVerificationTokenExpiry())
                .isGreaterThan(System.currentTimeMillis() + 6L * 24 * 60 * 60 * 1000);

        verify(emailService).sendEmailChangeConfirmation(eq("newemail@example.com"), eq("emailchangeuser"),
                anyString());
    }

    @Test
    void requestEmailChange_withAlreadyUsedEmail_returnsError() {
        // Another account already holds the target email as their confirmed email
        OnlineProfile other = createProfile("otheremailuser", "pass");
        // createProfile sets email to username@example.com; we need a custom email
        other.setEmail("taken@example.com");
        profileRepository.save(other);

        OnlineProfile requester = createProfile("requesteruser", "pass");

        RequestEmailChangeResponse response = authService.requestEmailChange("requesteruser", "taken@example.com");

        assertThat(response.success()).isFalse();
        assertThat(response.message()).contains("Email address not available");

        // pendingEmail must not have been set
        OnlineProfile unchanged = profileRepository.findByName("requesteruser").orElseThrow();
        assertThat(unchanged.getPendingEmail()).isNull();
    }

    @Test
    void requestEmailChange_withPendingEmailInUse_returnsError() {
        // Another account already has the target address as their pendingEmail
        OnlineProfile other = createProfile("pendingowner", "pass");
        other.setPendingEmail("pending@example.com");
        other.setEmailVerificationToken("some-token-pending");
        other.setEmailVerificationTokenExpiry(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000);
        profileRepository.save(other);

        OnlineProfile requester = createProfile("requester2", "pass");

        RequestEmailChangeResponse response = authService.requestEmailChange("requester2", "pending@example.com");

        assertThat(response.success()).isFalse();
        assertThat(response.message()).contains("Email address not available");

        OnlineProfile unchanged = profileRepository.findByName("requester2").orElseThrow();
        assertThat(unchanged.getPendingEmail()).isNull();
    }

    @Test
    void requestEmailChange_withSameAsCurrentEmail_returnsError() {
        createProfile("sameemailuser", "pass");

        // createProfile sets email to sameemailuser@example.com; use mixed-case to
        // verify
        // normalization catches the duplicate even when case differs
        RequestEmailChangeResponse response = authService.requestEmailChange("sameemailuser",
                "SameEmailUser@Example.COM");

        assertThat(response.success()).isFalse();
        assertThat(response.message()).isNotBlank();
    }

    @Test
    void requestEmailChange_emailServiceFailure_stillReturnsSuccess() {
        doThrow(new RuntimeException("SMTP error")).when(emailService).sendEmailChangeConfirmation(anyString(),
                anyString(), anyString());

        createProfile("emailfailchange", "pass");

        RequestEmailChangeResponse response = authService.requestEmailChange("emailfailchange",
                "failchange@example.com");

        assertThat(response.success()).isTrue();

        OnlineProfile updated = profileRepository.findByName("emailfailchange").orElseThrow();
        assertThat(updated.getPendingEmail()).isEqualTo("failchange@example.com");
    }

    // -------------------------------------------------------------------------
    // forgotPassword tests
    // -------------------------------------------------------------------------

    @Test
    void forgotPassword_withRegisteredEmail_returnsTokenAndCreatesResetToken() {
        OnlineProfile profile = createProfile("forgotuser", "password123");

        String token = authService.forgotPassword("forgotuser@example.com");

        assertThat(token).isNotNull().isNotBlank();
        assertThat(resetTokenRepository.findByToken(token)).isPresent();
    }

    @Test
    void forgotPassword_withUnknownEmail_returnsNull() {
        String token = authService.forgotPassword("nobody@example.com");

        assertThat(token).isNull();
    }

    @Test
    void forgotPassword_calledTwiceWithinRateLimit_returnsNullOnSecondCall() {
        createProfile("ratelimitforgot", "pass");

        String first = authService.forgotPassword("ratelimitforgot@example.com");
        String second = authService.forgotPassword("ratelimitforgot@example.com");

        assertThat(first).isNotNull();
        assertThat(second).isNull();
    }

    @Test
    void forgotPassword_rateLimitIsCaseInsensitive() {
        createProfile("caseforgot", "pass");

        String first = authService.forgotPassword("caseforgot@example.com");
        String second = authService.forgotPassword("CaseForgot@Example.COM");

        assertThat(first).isNotNull();
        assertThat(second).isNull();
    }

    // -------------------------------------------------------------------------
    // resetPassword tests
    // -------------------------------------------------------------------------

    @Test
    void resetPassword_withValidToken_updatesPasswordHash() {
        OnlineProfile profile = createProfile("resetuser", "oldpass");
        String token = authService.forgotPassword("resetuser@example.com");

        authService.resetPassword(token, "newpass123");

        // Verify new password works
        LoginResponse response = authService.login("resetuser", "newpass123", false);
        assertThat(response.success()).isTrue();
    }

    @Test
    void resetPassword_withValidToken_marksTokenAsUsed() {
        createProfile("resetused", "oldpass");
        String token = authService.forgotPassword("resetused@example.com");

        authService.resetPassword(token, "newpass123");

        // Second attempt with same token should fail
        assertThatThrownBy(() -> authService.resetPassword(token, "anotherpass"))
                .isInstanceOf(AuthService.InvalidResetTokenException.class);
    }

    @Test
    void resetPassword_withUnknownToken_throwsInvalidResetTokenException() {
        assertThatThrownBy(() -> authService.resetPassword("bogus-token-value", "newpass"))
                .isInstanceOf(AuthService.InvalidResetTokenException.class);
    }

    @Test
    void resetPassword_withExpiredToken_throwsInvalidResetTokenException() {
        OnlineProfile profile = createProfile("expiredresetuser", "oldpass");
        String token = authService.forgotPassword("expiredresetuser@example.com");

        // Manually expire the token
        PasswordResetToken stored = resetTokenRepository.findByToken(token).orElseThrow();
        stored.setExpiryDate(java.time.Instant.now().minusSeconds(3600));
        resetTokenRepository.save(stored);

        assertThatThrownBy(() -> authService.resetPassword(token, "newpass"))
                .isInstanceOf(AuthService.InvalidResetTokenException.class);
    }

    // -------------------------------------------------------------------------
    // getCurrentUser tests
    // -------------------------------------------------------------------------

    @Test
    void getCurrentUser_withExistingProfile_returnsProfileResponse() {
        OnlineProfile profile = createProfile("currentuser", "pass");

        ProfileResponse response = authService.getCurrentUser(profile.getId());

        assertThat(response).isNotNull();
        assertThat(response.username()).isEqualTo("currentuser");
        assertThat(response.email()).isEqualTo("currentuser@example.com");
    }

    @Test
    void getCurrentUser_withNonexistentId_returnsNull() {
        ProfileResponse response = authService.getCurrentUser(999999L);

        assertThat(response).isNull();
    }

    // -------------------------------------------------------------------------
    // generateWsToken tests
    // -------------------------------------------------------------------------

    @Test
    void generateWsToken_returnsNonNullJwt() {
        String token = authService.generateWsToken(1L, "testuser");

        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    void generateWsToken_withinRateLimit_returnsTokensUpToLimit() {
        // Should succeed for first 5 calls
        for (int i = 0; i < 5; i++) {
            assertThat(authService.generateWsToken(100L, "ratelimitwsuser")).isNotNull();
        }

        // 6th call should be rate-limited
        assertThat(authService.generateWsToken(100L, "ratelimitwsuser")).isNull();
    }

    @Test
    void generateWsToken_differentUsers_areIndependent() {
        // Exhaust rate limit for user 200
        for (int i = 0; i < 5; i++) {
            authService.generateWsToken(200L, "user200");
        }
        assertThat(authService.generateWsToken(200L, "user200")).isNull();

        // User 201 should still be fine
        assertThat(authService.generateWsToken(201L, "user201")).isNotNull();
    }

    // -------------------------------------------------------------------------
    // generateReconnectToken / generateObserveToken tests
    // -------------------------------------------------------------------------

    @Test
    void generateReconnectToken_returnsNonNullJwt() {
        String token = authService.generateReconnectToken(1L, "testuser", "game-123");

        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    void generateObserveToken_returnsNonNullJwt() {
        String token = authService.generateObserveToken(1L, "testuser", "game-456");

        assertThat(token).isNotNull().isNotBlank();
    }

    // -------------------------------------------------------------------------
    // markJtiUsed / isJtiUsed tests
    // -------------------------------------------------------------------------

    @Test
    void isJtiUsed_withUnknownJti_returnsFalse() {
        assertThat(authService.isJtiUsed("unknown-jti")).isFalse();
    }

    @Test
    void markJtiUsed_thenIsJtiUsed_returnsTrue() {
        long futureExpiry = System.currentTimeMillis() + 60_000;
        authService.markJtiUsed("test-jti-1", futureExpiry);

        assertThat(authService.isJtiUsed("test-jti-1")).isTrue();
    }

    @Test
    void isJtiUsed_afterExpiry_returnsFalseAndCleansUp() {
        long pastExpiry = System.currentTimeMillis() - 1000;
        authService.markJtiUsed("expired-jti", pastExpiry);

        assertThat(authService.isJtiUsed("expired-jti")).isFalse();
    }

    @Test
    void markJtiUsed_multipleJtis_trackedIndependently() {
        long futureExpiry = System.currentTimeMillis() + 60_000;
        authService.markJtiUsed("jti-a", futureExpiry);
        authService.markJtiUsed("jti-b", futureExpiry);

        assertThat(authService.isJtiUsed("jti-a")).isTrue();
        assertThat(authService.isJtiUsed("jti-b")).isTrue();
        assertThat(authService.isJtiUsed("jti-c")).isFalse();
    }
}
