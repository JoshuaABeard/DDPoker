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
package com.donohoedigital.poker.api.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.donohoedigital.games.poker.gameserver.persistence.repository.OnlineProfileRepository;
import com.donohoedigital.games.poker.gameserver.service.EmailService;
import com.donohoedigital.games.poker.model.OnlineProfile;
import com.donohoedigital.games.poker.service.OnlineProfileService;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Unit tests for AdminController — account management endpoints. Tests the
 * controller directly without a Spring context to avoid Java 25 / Spring Boot
 * test compatibility issues.
 *
 * <p>
 * EmailService cannot be Mockito-mocked directly in this no-Spring unit test
 * context (ByteBuddy inline mock limitation with this class). A simple tracking
 * stub subclass is used instead.
 */
class AdminControllerTest {

    /** Minimal stub for EmailService that records sendVerificationEmail calls. */
    static class EmailServiceStub extends EmailService {
        final List<String[]> verificationCalls = new ArrayList<>();

        EmailServiceStub() {
            super(null, "noreply@test.com", "http://localhost");
        }

        @Override
        public void sendVerificationEmail(String toEmail, String username, String token) {
            verificationCalls.add(new String[]{toEmail, username, token});
        }
    }

    private AdminController controller;
    private OnlineProfileService profileService;
    private OnlineProfileRepository profileRepository;
    private EmailServiceStub emailService;

    @BeforeEach
    void setUp() throws Exception {
        controller = new AdminController();

        profileService = mock(OnlineProfileService.class);
        profileRepository = mock(OnlineProfileRepository.class);
        emailService = new EmailServiceStub();

        inject(controller, "profileService", profileService);
        inject(controller, "profileRepository", profileRepository);
        inject(controller, "emailService", emailService);
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    // -------------------------------------------------------------------------
    // manuallyVerify
    // -------------------------------------------------------------------------

    @Test
    void manuallyVerify_setsEmailVerifiedAndClearsToken_returns200() {
        OnlineProfile profile = new OnlineProfile("alice");
        profile.setEmailVerified(false);
        profile.setEmailVerificationToken("sometoken");
        profile.setEmailVerificationTokenExpiry(999L);
        when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));

        ResponseEntity<Void> response = controller.manuallyVerify(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        ArgumentCaptor<OnlineProfile> saved = ArgumentCaptor.forClass(OnlineProfile.class);
        verify(profileRepository).save(saved.capture());
        assertTrue(saved.getValue().isEmailVerified());
        assertNull(saved.getValue().getEmailVerificationToken());
        assertNull(saved.getValue().getEmailVerificationTokenExpiry());
    }

    @Test
    void manuallyVerify_profileNotFound_throws() {
        when(profileRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(Exception.class, () -> controller.manuallyVerify(99L));
    }

    // -------------------------------------------------------------------------
    // unlockAccount
    // -------------------------------------------------------------------------

    @Test
    void unlockAccount_clearsLockoutFields_returns200() {
        OnlineProfile profile = new OnlineProfile("bob");
        profile.setLockedUntil(System.currentTimeMillis() + 60_000L);
        profile.setFailedLoginAttempts(5);
        profile.setLockoutCount(2);
        when(profileRepository.findById(2L)).thenReturn(Optional.of(profile));

        ResponseEntity<Void> response = controller.unlockAccount(2L);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        ArgumentCaptor<OnlineProfile> saved = ArgumentCaptor.forClass(OnlineProfile.class);
        verify(profileRepository).save(saved.capture());
        assertNull(saved.getValue().getLockedUntil());
        assertEquals(0, saved.getValue().getFailedLoginAttempts());
        assertEquals(0, saved.getValue().getLockoutCount());
    }

    @Test
    void unlockAccount_profileNotFound_throws() {
        when(profileRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(Exception.class, () -> controller.unlockAccount(99L));
    }

    // -------------------------------------------------------------------------
    // adminResendVerification
    // -------------------------------------------------------------------------

    @Test
    void adminResendVerification_alreadyVerified_returns400() {
        OnlineProfile profile = new OnlineProfile("carol");
        profile.setEmailVerified(true);
        when(profileRepository.findById(3L)).thenReturn(Optional.of(profile));

        ResponseEntity<Void> response = controller.adminResendVerification(3L);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(profileRepository, never()).save(any());
        assertTrue(emailService.verificationCalls.isEmpty());
    }

    @Test
    void adminResendVerification_notVerified_generatesTokenAndSendsEmail() {
        OnlineProfile profile = new OnlineProfile("dave");
        profile.setEmailVerified(false);
        profile.setEmail("dave@example.com");
        when(profileRepository.findById(4L)).thenReturn(Optional.of(profile));

        ResponseEntity<Void> response = controller.adminResendVerification(4L);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        ArgumentCaptor<OnlineProfile> saved = ArgumentCaptor.forClass(OnlineProfile.class);
        verify(profileRepository).save(saved.capture());
        assertNotNull(saved.getValue().getEmailVerificationToken());
        assertNotNull(saved.getValue().getEmailVerificationTokenExpiry());

        assertEquals(1, emailService.verificationCalls.size());
        assertEquals("dave@example.com", emailService.verificationCalls.get(0)[0]);
        assertEquals("dave", emailService.verificationCalls.get(0)[1]);
        assertNotNull(emailService.verificationCalls.get(0)[2]);
    }

    @Test
    void adminResendVerification_notVerified_tokenIsUrlSafeAndNonEmpty() {
        OnlineProfile profile = new OnlineProfile("eve");
        profile.setEmailVerified(false);
        profile.setEmail("eve@example.com");
        when(profileRepository.findById(5L)).thenReturn(Optional.of(profile));

        controller.adminResendVerification(5L);

        ArgumentCaptor<OnlineProfile> saved = ArgumentCaptor.forClass(OnlineProfile.class);
        verify(profileRepository).save(saved.capture());
        String token = saved.getValue().getEmailVerificationToken();
        // URL-safe base64: alphanumeric, hyphens, underscores only
        assertTrue(token.matches("[A-Za-z0-9_-]+"), "Token should be URL-safe base64: " + token);
        assertTrue(token.length() >= 20, "Token should be reasonably long");
    }

    @Test
    void adminResendVerification_notVerified_tokenExpiryIsSevenDaysAhead() {
        OnlineProfile profile = new OnlineProfile("frank");
        profile.setEmailVerified(false);
        profile.setEmail("frank@example.com");
        when(profileRepository.findById(6L)).thenReturn(Optional.of(profile));

        long before = System.currentTimeMillis();
        controller.adminResendVerification(6L);
        long after = System.currentTimeMillis();

        ArgumentCaptor<OnlineProfile> saved = ArgumentCaptor.forClass(OnlineProfile.class);
        verify(profileRepository).save(saved.capture());
        long expiry = saved.getValue().getEmailVerificationTokenExpiry();
        long sevenDaysMs = 7L * 24 * 60 * 60 * 1000;
        assertTrue(expiry >= before + sevenDaysMs);
        assertTrue(expiry <= after + sevenDaysMs + 1000);
    }

    @Test
    void adminResendVerification_profileNotFound_throws() {
        when(profileRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(Exception.class, () -> controller.adminResendVerification(99L));
    }
}
