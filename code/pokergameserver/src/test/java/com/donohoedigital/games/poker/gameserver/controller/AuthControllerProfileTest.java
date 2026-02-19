/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community
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
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker.gameserver.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.donohoedigital.games.poker.gameserver.auth.JwtProperties;
import com.donohoedigital.games.poker.gameserver.dto.ProfileResponse;
import com.donohoedigital.games.poker.gameserver.service.AuthService;

/**
 * Unit tests for the profile-related endpoints added to AuthController in M7
 * Phase 7.1: GET /me, POST /forgot-password, POST /reset-password.
 *
 * <p>
 * No active Spring profile is set, so the controller treats the environment as
 * dev mode and returns the reset token in the response body.
 */
@WebMvcTest
@Import({TestSecurityConfiguration.class, AuthController.class, AuthControllerProfileTest.TestConfig.class})
class AuthControllerProfileTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    static class TestConfig {
        @Bean
        public JwtProperties jwtProperties() {
            JwtProperties props = new JwtProperties();
            props.setCookieName("DDPoker-JWT");
            return props;
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/auth/me
    // -------------------------------------------------------------------------

    @Test
    void getMe_authenticated_returnsProfile() throws Exception {
        // TestSecurityConfiguration injects profileId=1L, username="testuser"
        when(authService.getCurrentUser(1L)).thenReturn(new ProfileResponse(1L, "testuser", "test@example.com", false));

        mockMvc.perform(get("/api/v1/auth/me")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com")).andExpect(jsonPath("$.retired").value(false));
    }

    @Test
    void getMe_profileNotFound_returns404() throws Exception {
        when(authService.getCurrentUser(1L)).thenReturn(null);

        mockMvc.perform(get("/api/v1/auth/me")).andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/auth/forgot-password
    // -------------------------------------------------------------------------

    @Test
    void forgotPassword_validEmail_devMode_returnsTokenInBody() throws Exception {
        // No active Spring profile → dev mode → token returned in body
        when(authService.forgotPassword("user@example.com")).thenReturn("reset-token-abc");

        mockMvc.perform(post("/api/v1/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@example.com\"}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.resetToken").value("reset-token-abc"));
    }

    @Test
    void forgotPassword_unknownEmail_returns200WithNoToken() throws Exception {
        // Service returns null when email not found — must not reveal this to client
        when(authService.forgotPassword(anyString())).thenReturn(null);

        mockMvc.perform(post("/api/v1/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"unknown@example.com\"}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.resetToken").doesNotExist());
    }

    @Test
    void forgotPassword_rateLimitExceeded_returns200SameResponse() throws Exception {
        // Rate limited → service returns null — same 200 response, don't reveal
        // throttle
        when(authService.forgotPassword(anyString())).thenReturn(null);

        mockMvc.perform(post("/api/v1/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@example.com\"}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.resetToken").doesNotExist());
    }

    @Test
    void forgotPassword_blankEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"\"}")).andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/auth/reset-password
    // -------------------------------------------------------------------------

    @Test
    void resetPassword_validToken_returns200() throws Exception {
        doNothing().when(authService).resetPassword(anyString(), anyString());

        mockMvc.perform(post("/api/v1/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"valid-token\",\"newPassword\":\"newPass123\"}")).andExpect(status().isOk());
    }

    @Test
    void resetPassword_invalidToken_returns400WithGenericMessage() throws Exception {
        doThrow(new AuthService.InvalidResetTokenException()).when(authService).resetPassword(anyString(), anyString());

        mockMvc.perform(post("/api/v1/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"bad-token\",\"newPassword\":\"newPass123\"}")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid or expired reset token"));
    }

    @Test
    void resetPassword_shortPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"some-token\",\"newPassword\":\"short\"}")).andExpect(status().isBadRequest());
    }
}
