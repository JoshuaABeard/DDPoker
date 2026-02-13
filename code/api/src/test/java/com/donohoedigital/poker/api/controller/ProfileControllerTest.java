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
import com.donohoedigital.poker.api.service.EmailService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for ProfileController.
 *
 * TODO: Currently disabled due to Java 25 / Spring Boot 3.3.9 incompatibility.
 * Spring Boot 3.3.9 uses Spring Framework 6.1.x which embeds ASM 9.7 that
 * doesn't support Java 25 bytecode (class file version 69). Options to fix: 1.
 * Wait for Spring Boot 3.4.x with Spring Framework 6.2.x (includes ASM 9.8) 2.
 * Upgrade to Spring Boot 4.0+ with Spring Framework 7.0 (native Java 25
 * support) 3. Downgrade to Java 21 LTS
 */
@Disabled("Java 25 incompatibility with Spring Boot 3.3.9 - see class javadoc")
@SpringBootTest
@AutoConfigureMockMvc
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OnlineProfileService profileService;

    @MockBean
    private EmailService emailService;

    @Test
    void testForgotPassword_ValidUsername_Success() throws Exception {
        // Setup mock profile
        OnlineProfile profile = new OnlineProfile();
        profile.setName("testuser");
        profile.setEmail("test@example.com");

        when(profileService.getOnlineProfileByName("testuser")).thenReturn(profile);
        when(emailService.sendPasswordResetEmail(anyString(), anyString(), anyString())).thenReturn(true);

        mockMvc.perform(post("/api/profile/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"testuser\"}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("A temporary password has been sent to your email address"));
    }

    @Test
    void testForgotPassword_InvalidUsername_GenericMessage() throws Exception {
        when(profileService.getOnlineProfileByName("nonexistent")).thenReturn(null);

        mockMvc.perform(post("/api/profile/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"nonexistent\"}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.message").value(
                        "If this username exists and has an email on file, a password reset email has been sent."));
    }

    @Test
    void testForgotPassword_RetiredProfile_GenericMessage() throws Exception {
        OnlineProfile profile = new OnlineProfile();
        profile.setName("retired");
        profile.setRetired(true);

        when(profileService.getOnlineProfileByName("retired")).thenReturn(profile);

        mockMvc.perform(post("/api/profile/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"retired\"}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.message").value(
                        "If this username exists and has an email on file, a password reset email has been sent."));
    }

    @Test
    void testForgotPassword_NoEmail_GenericMessage() throws Exception {
        OnlineProfile profile = new OnlineProfile();
        profile.setName("noemail");
        profile.setEmail(null);

        when(profileService.getOnlineProfileByName("noemail")).thenReturn(profile);

        mockMvc.perform(post("/api/profile/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"noemail\"}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.message").value(
                        "If this username exists and has an email on file, a password reset email has been sent."));
    }

    @Test
    void testForgotPassword_EmailSendFailure_ErrorMessage() throws Exception {
        OnlineProfile profile = new OnlineProfile();
        profile.setName("testuser");
        profile.setEmail("test@example.com");

        when(profileService.getOnlineProfileByName("testuser")).thenReturn(profile);
        when(emailService.sendPasswordResetEmail(anyString(), anyString(), anyString())).thenReturn(false);

        mockMvc.perform(post("/api/profile/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"testuser\"}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false)).andExpect(jsonPath("$.message")
                        .value("Failed to send email. Please try again later or contact support."));
    }

    @Test
    void testForgotPassword_InvalidUsernameFormat_ValidationError() throws Exception {
        mockMvc.perform(post("/api/profile/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"ab\"}")) // Too short
                .andExpect(status().isBadRequest());
    }

    @Test
    void testForgotPassword_InvalidCharacters_ValidationError() throws Exception {
        mockMvc.perform(post("/api/profile/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"user@123\"}")) // Invalid characters
                .andExpect(status().isBadRequest());
    }

    @Test
    void testForgotPassword_EmptyUsername_ValidationError() throws Exception {
        mockMvc.perform(post("/api/profile/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"\"}")).andExpect(status().isBadRequest());
    }
}
