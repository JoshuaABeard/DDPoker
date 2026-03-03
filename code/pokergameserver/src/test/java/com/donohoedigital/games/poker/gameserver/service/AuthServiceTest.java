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
import com.donohoedigital.games.poker.gameserver.dto.LoginResponse;
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
        assertThat(response.profileId()).isNotNull();

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
        assertThat(response.profileId()).isEqualTo(profile.getId());
        assertThat(response.username()).isEqualTo("testuser");
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
        assertThat(response.emailVerified()).isFalse();
    }

    @Test
    void register_whenEmailServiceFails_registrationStillSucceeds() {
        doThrow(new RuntimeException("SMTP connection refused")).when(emailService).sendVerificationEmail(anyString(),
                anyString(), anyString());

        LoginResponse response = authService.register("emailfailuser", "validpass4", "emailfail@example.com");

        assertThat(response.success()).isTrue();
        assertThat(response.token()).isNotNull();
    }
}
