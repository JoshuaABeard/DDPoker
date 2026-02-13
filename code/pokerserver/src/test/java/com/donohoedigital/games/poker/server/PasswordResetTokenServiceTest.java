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
package com.donohoedigital.games.poker.server;

import com.donohoedigital.games.poker.dao.PasswordResetTokenDao;
import com.donohoedigital.games.poker.model.OnlineProfile;
import com.donohoedigital.games.poker.model.PasswordResetToken;
import com.donohoedigital.games.poker.service.OnlineProfileService;
import com.donohoedigital.games.poker.service.PasswordHashingService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for password reset token functionality (SEC-BACKEND-3).
 *
 * Covers token generation, validation, expiration, single-use enforcement, and
 * the complete password reset flow.
 */
@Tag("slow")
@SpringJUnitConfig(locations = {"/app-context-pokerservertests.xml"})
@Transactional
class PasswordResetTokenServiceTest {

    @Autowired
    private OnlineProfileService profileService;

    @Autowired
    private PasswordResetTokenDao tokenDao;

    @Autowired
    private PasswordHashingService passwordHashingService;

    // ========================================
    // Token Generation Tests
    // ========================================

    @Test
    @Rollback
    void should_GenerateValidToken_When_ProfileExists() {
        // Given: A saved profile
        OnlineProfile profile = PokerTestData.createOnlineProfile("TokenUser");
        profileService.saveOnlineProfile(profile);

        // When: Generating a password reset token
        PasswordResetToken token = profileService.generatePasswordResetToken(profile);

        // Then: Token should be created and valid
        assertThat(token).isNotNull();
        assertThat(token.getId()).isNotNull();
        assertThat(token.getToken()).isNotNull().hasSize(36); // UUID format
        assertThat(token.getProfileId()).isEqualTo(profile.getId());
        assertThat(token.getCreateDate()).isNotNull().isBeforeOrEqualTo(Instant.now());
        assertThat(token.getExpiryDate()).isNotNull().isAfter(Instant.now());
        assertThat(token.isUsed()).isFalse();
        assertThat(token.isValid()).isTrue();
    }

    @Test
    @Rollback
    void should_ThrowException_When_GeneratingTokenForUnsavedProfile() {
        // Given: An unsaved profile
        OnlineProfile profile = PokerTestData.createOnlineProfile("UnsavedUser");

        // When/Then: Should throw exception
        assertThatThrownBy(() -> profileService.generatePasswordResetToken(profile))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Profile must be saved");
    }

    @Test
    @Rollback
    void should_InvalidateOldTokens_When_GeneratingNewToken() {
        // Given: A profile with an existing token
        OnlineProfile profile = PokerTestData.createOnlineProfile("MultiTokenUser");
        profileService.saveOnlineProfile(profile);

        PasswordResetToken firstToken = profileService.generatePasswordResetToken(profile);
        String firstTokenString = firstToken.getToken();

        // When: Generating a second token
        PasswordResetToken secondToken = profileService.generatePasswordResetToken(profile);

        // Then: First token should be invalidated, second should be valid
        PasswordResetToken reloadedFirstToken = tokenDao.findByToken(firstTokenString);
        assertThat(reloadedFirstToken.isUsed()).isTrue();
        assertThat(reloadedFirstToken.isValid()).isFalse();

        assertThat(secondToken.isUsed()).isFalse();
        assertThat(secondToken.isValid()).isTrue();
    }

    // ========================================
    // Token Validation Tests
    // ========================================

    @Test
    @Rollback
    void should_ValidateToken_When_TokenIsValid() {
        // Given: A valid token
        OnlineProfile profile = PokerTestData.createOnlineProfile("ValidTokenUser");
        profileService.saveOnlineProfile(profile);
        PasswordResetToken token = profileService.generatePasswordResetToken(profile);

        // When: Validating the token
        PasswordResetToken validated = profileService.validatePasswordResetToken(token.getToken());

        // Then: Should return the token
        assertThat(validated).isNotNull();
        assertThat(validated.getToken()).isEqualTo(token.getToken());
        assertThat(validated.isValid()).isTrue();
    }

    @Test
    @Rollback
    void should_ReturnNull_When_TokenDoesNotExist() {
        // When: Validating a non-existent token
        PasswordResetToken validated = profileService.validatePasswordResetToken("nonexistent-token-uuid");

        // Then: Should return null
        assertThat(validated).isNull();
    }

    @Test
    @Rollback
    void should_ReturnNull_When_TokenIsNull() {
        // When: Validating a null token
        PasswordResetToken validated = profileService.validatePasswordResetToken(null);

        // Then: Should return null
        assertThat(validated).isNull();
    }

    @Test
    @Rollback
    void should_ReturnNull_When_TokenIsEmpty() {
        // When: Validating an empty token
        PasswordResetToken validated = profileService.validatePasswordResetToken("");

        // Then: Should return null
        assertThat(validated).isNull();
    }

    @Test
    @Rollback
    void should_ReturnNull_When_TokenIsExpired() {
        // Given: An expired token
        OnlineProfile profile = PokerTestData.createOnlineProfile("ExpiredTokenUser");
        profileService.saveOnlineProfile(profile);

        PasswordResetToken token = new PasswordResetToken(profile.getId(), 1); // Expires in 1ms
        tokenDao.save(token);

        // Wait for expiration
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When: Validating the expired token
        PasswordResetToken validated = profileService.validatePasswordResetToken(token.getToken());

        // Then: Should return null
        assertThat(validated).isNull();
    }

    @Test
    @Rollback
    void should_ReturnNull_When_TokenIsUsed() {
        // Given: A used token
        OnlineProfile profile = PokerTestData.createOnlineProfile("UsedTokenUser");
        profileService.saveOnlineProfile(profile);

        PasswordResetToken token = profileService.generatePasswordResetToken(profile);
        token.markAsUsed();
        tokenDao.update(token);

        // When: Validating the used token
        PasswordResetToken validated = profileService.validatePasswordResetToken(token.getToken());

        // Then: Should return null
        assertThat(validated).isNull();
    }

    // ========================================
    // Password Reset Tests
    // ========================================

    @Test
    @Rollback
    void should_ResetPassword_When_TokenIsValid() {
        // Given: A profile with a valid token
        OnlineProfile profile = PokerTestData.createOnlineProfile("ResetUser");
        profileService.saveOnlineProfile(profile);

        PasswordResetToken token = profileService.generatePasswordResetToken(profile);
        String testPassword = "newSecurePassword123";

        // When: Resetting password with valid token
        boolean success = profileService.resetPasswordWithToken(token.getToken(), testPassword);

        // Then: Password should be reset and token should be marked as used
        assertThat(success).isTrue();

        // Verify token is marked as used
        PasswordResetToken usedToken = tokenDao.findByToken(token.getToken());
        assertThat(usedToken.isUsed()).isTrue();
        assertThat(usedToken.isValid()).isFalse();

        // Verify password was actually changed
        OnlineProfile updatedProfile = profileService.getOnlineProfileById(profile.getId());
        assertThat(passwordHashingService.checkPassword(testPassword, updatedProfile.getPasswordHash())).isTrue();
        assertThat(passwordHashingService.checkPassword("password", updatedProfile.getPasswordHash())).isFalse();
    }

    @Test
    @Rollback
    void should_FailReset_When_TokenIsInvalid() {
        // When: Attempting to reset with invalid token
        boolean success = profileService.resetPasswordWithToken("invalid-token", "testPassword");

        // Then: Should fail
        assertThat(success).isFalse();
    }

    @Test
    @Rollback
    void should_FailReset_When_TokenIsExpired() {
        // Given: An expired token
        OnlineProfile profile = PokerTestData.createOnlineProfile("ExpiredResetUser");
        profileService.saveOnlineProfile(profile);

        PasswordResetToken token = new PasswordResetToken(profile.getId(), 1); // Expires in 1ms
        tokenDao.save(token);

        // Wait for expiration
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When: Attempting to reset with expired token
        boolean success = profileService.resetPasswordWithToken(token.getToken(), "testPassword");

        // Then: Should fail
        assertThat(success).isFalse();
    }

    @Test
    @Rollback
    void should_FailReset_When_TokenIsAlreadyUsed() {
        // Given: A token that has already been used
        OnlineProfile profile = PokerTestData.createOnlineProfile("DoubleResetUser");
        profileService.saveOnlineProfile(profile);

        PasswordResetToken token = profileService.generatePasswordResetToken(profile);

        // First reset succeeds
        boolean firstReset = profileService.resetPasswordWithToken(token.getToken(), "firstTestPassword");
        assertThat(firstReset).isTrue();

        // When: Attempting to use the same token again
        boolean secondReset = profileService.resetPasswordWithToken(token.getToken(), "secondTestPassword");

        // Then: Should fail
        assertThat(secondReset).isFalse();

        // Verify password is still the first new password
        OnlineProfile updatedProfile = profileService.getOnlineProfileById(profile.getId());
        assertThat(passwordHashingService.checkPassword("firstTestPassword", updatedProfile.getPasswordHash()))
                .isTrue();
        assertThat(passwordHashingService.checkPassword("secondTestPassword", updatedProfile.getPasswordHash()))
                .isFalse();
    }

    // ========================================
    // DAO-level Tests
    // ========================================

    @Test
    @Rollback
    void should_FindToken_When_SearchingByTokenString() {
        // Given: A saved token
        OnlineProfile profile = PokerTestData.createOnlineProfile("DaoTestUser");
        profileService.saveOnlineProfile(profile);
        PasswordResetToken token = profileService.generatePasswordResetToken(profile);

        // When: Finding by token string
        PasswordResetToken found = tokenDao.findByToken(token.getToken());

        // Then: Should find the token
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(token.getId());
        assertThat(found.getToken()).isEqualTo(token.getToken());
    }

    @Test
    @Rollback
    void should_InvalidateMultipleTokens_When_CalledForProfile() {
        // Given: A profile with multiple tokens
        OnlineProfile profile = PokerTestData.createOnlineProfile("MultiInvalidateUser");
        profileService.saveOnlineProfile(profile);

        // Create tokens manually to have multiple active ones
        PasswordResetToken token1 = new PasswordResetToken(profile.getId(), PasswordResetToken.DEFAULT_EXPIRY_MS);
        PasswordResetToken token2 = new PasswordResetToken(profile.getId(), PasswordResetToken.DEFAULT_EXPIRY_MS);
        tokenDao.save(token1);
        tokenDao.save(token2);

        // When: Invalidating all tokens for profile
        int invalidated = tokenDao.invalidateTokensForProfile(profile.getId());

        // Then: Both tokens should be invalidated
        assertThat(invalidated).isEqualTo(2);

        PasswordResetToken reloaded1 = tokenDao.findByToken(token1.getToken());
        PasswordResetToken reloaded2 = tokenDao.findByToken(token2.getToken());

        assertThat(reloaded1.isUsed()).isTrue();
        assertThat(reloaded2.isUsed()).isTrue();
    }

    @Test
    @Rollback
    void should_DeleteExpiredAndUsedTokens_When_CleanupCalled() {
        // Given: Mix of expired, used, and valid tokens
        OnlineProfile profile = PokerTestData.createOnlineProfile("CleanupTestUser");
        profileService.saveOnlineProfile(profile);

        // Create expired token
        PasswordResetToken expiredToken = new PasswordResetToken(profile.getId(), 1);
        tokenDao.save(expiredToken);

        // Create used token
        PasswordResetToken usedToken = new PasswordResetToken(profile.getId(), PasswordResetToken.DEFAULT_EXPIRY_MS);
        usedToken.markAsUsed();
        tokenDao.save(usedToken);

        // Create valid token
        PasswordResetToken validToken = new PasswordResetToken(profile.getId(), PasswordResetToken.DEFAULT_EXPIRY_MS);
        tokenDao.save(validToken);

        // Wait for first token to expire
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When: Running cleanup
        int deleted = tokenDao.deleteExpiredAndUsedTokens();

        // Then: Should delete expired and used tokens, but not valid one
        assertThat(deleted).isEqualTo(2);

        assertThat(tokenDao.findByToken(expiredToken.getToken())).isNull();
        assertThat(tokenDao.findByToken(usedToken.getToken())).isNull();
        assertThat(tokenDao.findByToken(validToken.getToken())).isNotNull();
    }

    // ========================================
    // Token Model Tests
    // ========================================

    @Test
    void should_CreateValidToken_When_ConstructedWithProfileId() {
        // When: Creating a token with standard expiry
        PasswordResetToken token = new PasswordResetToken(123L, PasswordResetToken.DEFAULT_EXPIRY_MS);

        // Then: Token should be properly initialized
        assertThat(token.getToken()).isNotNull().hasSize(36);
        assertThat(token.getProfileId()).isEqualTo(123L);
        assertThat(token.getCreateDate()).isNotNull().isBeforeOrEqualTo(Instant.now());
        assertThat(token.getExpiryDate()).isNotNull().isAfter(Instant.now());
        assertThat(token.isUsed()).isFalse();
        assertThat(token.isValid()).isTrue();
    }

    @Test
    void should_MarkTokenAsInvalid_When_MarkedAsUsed() {
        // Given: A valid token
        PasswordResetToken token = new PasswordResetToken(123L, PasswordResetToken.DEFAULT_EXPIRY_MS);
        assertThat(token.isValid()).isTrue();

        // When: Marking as used
        token.markAsUsed();

        // Then: Should be invalid
        assertThat(token.isUsed()).isTrue();
        assertThat(token.isValid()).isFalse();
    }

    @Test
    void should_BeInvalid_When_TokenIsExpired() {
        // Given: A token that expires immediately
        PasswordResetToken token = new PasswordResetToken(123L, 1);

        // Wait for expiration
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Then: Should be invalid
        assertThat(token.isValid()).isFalse();
    }
}
