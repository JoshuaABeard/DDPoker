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
package com.donohoedigital.games.poker.service.impl;

import com.donohoedigital.games.poker.dao.OnlineProfileDao;
import com.donohoedigital.games.poker.dao.PasswordResetTokenDao;
import com.donohoedigital.games.poker.model.OnlineProfile;
import com.donohoedigital.games.poker.model.PasswordResetToken;
import com.donohoedigital.games.poker.service.PasswordHashingService;
import com.donohoedigital.games.poker.service.helper.DisallowedManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Instant;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OnlineProfileServiceImpl with mocked DAOs.
 */
@ExtendWith(MockitoExtension.class)
class OnlineProfileServiceImplUnitTest {

    @Mock
    private OnlineProfileDao dao;

    @Mock
    private PasswordResetTokenDao passwordResetTokenDao;

    @Mock
    private PasswordHashingService passwordHashingService;

    @InjectMocks
    private OnlineProfileServiceImpl service;

    private DisallowedManager disallowedManager;

    @BeforeEach
    void setUp() throws Exception {
        // Inject a real DisallowedManager (it loads from classpath)
        disallowedManager = new DisallowedManager();
        Field field = OnlineProfileServiceImpl.class.getDeclaredField("disallowed");
        field.setAccessible(true);
        field.set(service, disallowedManager);
    }

    // ========================================
    // isNameValid
    // ========================================

    @Test
    void should_ReturnTrue_When_NameIsValid() {
        assertThat(service.isNameValid("GoodPlayer")).isTrue();
    }

    @Test
    void should_ReturnFalse_When_NameIsDisallowed() {
        // "ddpoker" is in the disallowed list
        assertThat(service.isNameValid("ddpoker")).isFalse();
    }

    @Test
    void should_ReturnFalse_When_NameContainsQuestionMarks() {
        // "???" matches a disallowed pattern
        assertThat(service.isNameValid("???")).isFalse();
    }

    @Test
    void should_ReturnFalse_When_NameIsEmpty() {
        assertThat(service.isNameValid("")).isFalse();
    }

    // ========================================
    // authenticateOnlineProfile
    // ========================================

    @Test
    void should_ReturnProfile_When_PasswordMatches() {
        OnlineProfile stored = new OnlineProfile();
        stored.setName("TestUser");
        stored.setPasswordHash("$2a$hashed");
        stored.setRetired(false);

        OnlineProfile login = new OnlineProfile();
        login.setName("TestUser");
        login.setPassword("plaintext");

        when(dao.getByName("TestUser")).thenReturn(stored);
        when(passwordHashingService.checkPassword("plaintext", "$2a$hashed")).thenReturn(true);

        OnlineProfile result = service.authenticateOnlineProfile(login);

        assertThat(result).isSameAs(stored);
    }

    @Test
    void should_ReturnNull_When_PasswordDoesNotMatch() {
        OnlineProfile stored = new OnlineProfile();
        stored.setName("TestUser");
        stored.setPasswordHash("$2a$hashed");
        stored.setRetired(false);

        OnlineProfile login = new OnlineProfile();
        login.setName("TestUser");
        login.setPassword("wrong");

        when(dao.getByName("TestUser")).thenReturn(stored);
        when(passwordHashingService.checkPassword("wrong", "$2a$hashed")).thenReturn(false);

        OnlineProfile result = service.authenticateOnlineProfile(login);

        assertThat(result).isNull();
    }

    @Test
    void should_ReturnNull_When_ProfileIsRetired() {
        OnlineProfile stored = new OnlineProfile();
        stored.setName("RetiredUser");
        stored.setPasswordHash("$2a$hashed");
        stored.setRetired(true);

        OnlineProfile login = new OnlineProfile();
        login.setName("RetiredUser");
        login.setPassword("plaintext");

        when(dao.getByName("RetiredUser")).thenReturn(stored);

        OnlineProfile result = service.authenticateOnlineProfile(login);

        assertThat(result).isNull();
        // Should never check password for retired profile
        verify(passwordHashingService, never()).checkPassword(anyString(), anyString());
    }

    @Test
    void should_ReturnNull_When_ProfileNotFound() {
        OnlineProfile login = new OnlineProfile();
        login.setName("Unknown");
        login.setPassword("password");

        when(dao.getByName("Unknown")).thenReturn(null);

        OnlineProfile result = service.authenticateOnlineProfile(login);

        assertThat(result).isNull();
    }

    // ========================================
    // hashAndSetPassword
    // ========================================

    @Test
    void should_DelegateToHashingService_When_HashAndSetPasswordCalled() {
        OnlineProfile profile = new OnlineProfile();
        when(passwordHashingService.hashPassword("mypass")).thenReturn("$2a$hashed_mypass");

        service.hashAndSetPassword(profile, "mypass");

        assertThat(profile.getPasswordHash()).isEqualTo("$2a$hashed_mypass");
        verify(passwordHashingService).hashPassword("mypass");
    }

    // ========================================
    // retire
    // ========================================

    @Test
    void should_SetRetiredAndHashPassword_When_RetiringProfile() {
        OnlineProfile profile = new OnlineProfile();
        profile.setName("ToRetire");
        profile.setRetired(false);

        when(dao.getByName("ToRetire")).thenReturn(profile);
        when(passwordHashingService.hashPassword("__retired__")).thenReturn("$2a$retired_hash");

        service.retire("ToRetire");

        assertThat(profile.isRetired()).isTrue();
        assertThat(profile.getPasswordHash()).isEqualTo("$2a$retired_hash");
        verify(dao).update(profile);
    }

    @Test
    void should_DoNothing_When_RetiringNonexistentProfile() {
        when(dao.getByName("Ghost")).thenReturn(null);

        service.retire("Ghost");

        verify(dao, never()).update(any());
    }

    // ========================================
    // generatePasswordResetToken
    // ========================================

    @Test
    void should_CreateAndSaveToken_When_GeneratingResetToken() {
        OnlineProfile profile = new OnlineProfile();
        profile.setId(42L);
        profile.setName("TokenUser");

        PasswordResetToken result = service.generatePasswordResetToken(profile);

        assertThat(result).isNotNull();
        assertThat(result.getProfileId()).isEqualTo(42L);
        assertThat(result.getToken()).isNotBlank();
        verify(passwordResetTokenDao).invalidateTokensForProfile(42L);
        verify(passwordResetTokenDao).save(any(PasswordResetToken.class));
    }

    @Test
    void should_ThrowException_When_GeneratingTokenForUnsavedProfile() {
        OnlineProfile profile = new OnlineProfile();
        // No id set

        assertThatThrownBy(() -> service.generatePasswordResetToken(profile))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_ThrowException_When_GeneratingTokenForNullProfile() {
        assertThatThrownBy(() -> service.generatePasswordResetToken(null)).isInstanceOf(IllegalArgumentException.class);
    }

    // ========================================
    // validatePasswordResetToken
    // ========================================

    @Test
    void should_ReturnToken_When_TokenIsValid() {
        PasswordResetToken token = new PasswordResetToken(1L, PasswordResetToken.DEFAULT_EXPIRY_MS);

        when(passwordResetTokenDao.findByToken(token.getToken())).thenReturn(token);

        PasswordResetToken result = service.validatePasswordResetToken(token.getToken());

        assertThat(result).isSameAs(token);
    }

    @Test
    void should_ReturnNull_When_TokenIsExpired() {
        PasswordResetToken token = new PasswordResetToken();
        token.setToken("expired-token");
        token.setProfileId(1L);
        token.setExpiryDate(Instant.now().minusSeconds(3600)); // expired 1 hour ago
        token.setUsed(false);

        when(passwordResetTokenDao.findByToken("expired-token")).thenReturn(token);

        PasswordResetToken result = service.validatePasswordResetToken("expired-token");

        assertThat(result).isNull();
    }

    @Test
    void should_ReturnNull_When_TokenIsUsed() {
        PasswordResetToken token = new PasswordResetToken();
        token.setToken("used-token");
        token.setProfileId(1L);
        token.setExpiryDate(Instant.now().plusSeconds(3600));
        token.setUsed(true);

        when(passwordResetTokenDao.findByToken("used-token")).thenReturn(token);

        PasswordResetToken result = service.validatePasswordResetToken("used-token");

        assertThat(result).isNull();
    }

    @Test
    void should_ReturnNull_When_TokenStringIsNull() {
        PasswordResetToken result = service.validatePasswordResetToken(null);

        assertThat(result).isNull();
        verify(passwordResetTokenDao, never()).findByToken(any());
    }

    @Test
    void should_ReturnNull_When_TokenStringIsBlank() {
        PasswordResetToken result = service.validatePasswordResetToken("   ");

        assertThat(result).isNull();
        verify(passwordResetTokenDao, never()).findByToken(any());
    }

    @Test
    void should_ReturnNull_When_TokenNotFoundInDb() {
        when(passwordResetTokenDao.findByToken("no-such-token")).thenReturn(null);

        PasswordResetToken result = service.validatePasswordResetToken("no-such-token");

        assertThat(result).isNull();
    }

    // ========================================
    // resetPasswordWithToken
    // ========================================

    @Test
    void should_ResetPassword_When_TokenIsValid() {
        PasswordResetToken token = new PasswordResetToken(10L, PasswordResetToken.DEFAULT_EXPIRY_MS);

        OnlineProfile profile = new OnlineProfile();
        profile.setId(10L);
        profile.setName("ResetMe");

        when(passwordResetTokenDao.findByToken(token.getToken())).thenReturn(token);
        when(dao.get(10L)).thenReturn(profile);
        when(passwordHashingService.hashPassword("newpass")).thenReturn("$2a$new_hash");

        boolean result = service.resetPasswordWithToken(token.getToken(), "newpass");

        assertThat(result).isTrue();
        assertThat(profile.getPasswordHash()).isEqualTo("$2a$new_hash");
        assertThat(token.isUsed()).isTrue();
        verify(dao).update(profile);
        verify(passwordResetTokenDao).update(token);
    }

    @Test
    void should_ReturnFalse_When_ResetTokenIsInvalid() {
        when(passwordResetTokenDao.findByToken("bad-token")).thenReturn(null);

        boolean result = service.resetPasswordWithToken("bad-token", "newpass");

        assertThat(result).isFalse();
        verify(dao, never()).update(any());
    }

    @Test
    void should_ReturnFalse_When_ProfileNotFoundForResetToken() {
        PasswordResetToken token = new PasswordResetToken(999L, PasswordResetToken.DEFAULT_EXPIRY_MS);

        when(passwordResetTokenDao.findByToken(token.getToken())).thenReturn(token);
        when(dao.get(999L)).thenReturn(null);

        boolean result = service.resetPasswordWithToken(token.getToken(), "newpass");

        assertThat(result).isFalse();
    }

    // ========================================
    // saveOnlineProfile
    // ========================================

    @Test
    void should_SaveAndReturnTrue_When_NameIsAvailable() {
        OnlineProfile profile = new OnlineProfile();
        profile.setName("NewUser");

        when(dao.getByName("NewUser")).thenReturn(null);

        boolean result = service.saveOnlineProfile(profile);

        assertThat(result).isTrue();
        verify(dao).save(profile);
    }

    @Test
    void should_ReturnFalse_When_NameAlreadyExists() {
        OnlineProfile existing = new OnlineProfile();
        existing.setName("Taken");

        OnlineProfile newProfile = new OnlineProfile();
        newProfile.setName("Taken");

        when(dao.getByName("Taken")).thenReturn(existing);

        boolean result = service.saveOnlineProfile(newProfile);

        assertThat(result).isFalse();
        verify(dao, never()).save(any());
    }

    // ========================================
    // DAO delegation methods
    // ========================================

    @Test
    void should_DelegateToDao_When_GetOnlineProfileById() {
        OnlineProfile expected = new OnlineProfile();
        when(dao.get(5L)).thenReturn(expected);

        OnlineProfile result = service.getOnlineProfileById(5L);

        assertThat(result).isSameAs(expected);
    }

    @Test
    void should_DelegateToDao_When_DeleteOnlineProfile() {
        OnlineProfile profile = new OnlineProfile();

        service.deleteOnlineProfile(profile);

        verify(dao).delete(profile);
    }

    @Test
    void should_HandleNullList_When_DeleteOnlineProfiles() {
        service.deleteOnlineProfiles(null);

        verify(dao, never()).delete(any());
    }

    @Test
    void should_DelegateToDao_When_UpdateOnlineProfile() {
        OnlineProfile profile = new OnlineProfile();
        OnlineProfile updated = new OnlineProfile();
        when(dao.update(profile)).thenReturn(updated);

        OnlineProfile result = service.updateOnlineProfile(profile);

        assertThat(result).isSameAs(updated);
    }

    @Test
    void should_GenerateNonEmptyPassword_When_GeneratePasswordCalled() {
        String password = service.generatePassword();
        assertThat(password).isNotNull().isNotEmpty();
    }
}
