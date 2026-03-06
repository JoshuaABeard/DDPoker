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
import com.donohoedigital.games.poker.protocol.dto.LoginResponse;
import com.donohoedigital.games.poker.protocol.dto.RequestEmailChangeResponse;
import com.donohoedigital.games.poker.protocol.dto.ResendVerificationResponse;
import com.donohoedigital.games.poker.protocol.dto.VerifyEmailResponse;
import com.donohoedigital.games.poker.gameserver.service.AuthService;

@WebMvcTest
@Import({TestSecurityConfiguration.class, AuthController.class, AuthControllerTest.TestConfig.class})
class AuthControllerTest {

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

    @Test
    void testRegisterSuccess() throws Exception {
        when(authService.register(anyString(), anyString(), anyString())).thenReturn(
                new LoginResponse(true, "test-token", 1L, "testuser", "testuser@example.com", false, null, null));

        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"testuser\",\"password\":\"password123\",\"email\":\"test@example.com\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.token").value("test-token")).andExpect(jsonPath("$.profileId").value(1))
                .andExpect(jsonPath("$.username").value("testuser")).andExpect(cookie().exists("DDPoker-JWT"));
    }

    @Test
    void testRegisterFailure() throws Exception {
        when(authService.register(anyString(), anyString(), anyString()))
                .thenReturn(new LoginResponse(false, null, null, null, null, false, "Username already exists", null));

        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"existing\",\"password\":\"password\",\"email\":\"test@example.com\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Username already exists"));
    }

    @Test
    void testLoginSuccess() throws Exception {
        when(authService.login(anyString(), anyString(), anyBoolean())).thenReturn(
                new LoginResponse(true, "test-token", 1L, "testuser", "testuser@example.com", true, null, null));

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"testuser\",\"password\":\"password123\",\"rememberMe\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.token").value("test-token")).andExpect(jsonPath("$.profileId").value(1))
                .andExpect(jsonPath("$.username").value("testuser")).andExpect(cookie().exists("DDPoker-JWT"));
    }

    @Test
    void testLoginFailure() throws Exception {
        when(authService.login(anyString(), anyString(), anyBoolean())).thenReturn(
                new LoginResponse(false, null, null, null, null, false, "Invalid username or password", null));

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"testuser\",\"password\":\"wrongpass\",\"rememberMe\":false}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void login_whenAccountLocked_returns423() throws Exception {
        when(authService.login(anyString(), anyString(), anyBoolean()))
                .thenReturn(new LoginResponse(false, null, null, null, null, false, null, 120L));

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"testuser\",\"password\":\"password123\"}")).andExpect(status().isLocked());
    }

    @Test
    void testLogout() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")).andExpect(status().isOk())
                .andExpect(cookie().maxAge("DDPoker-JWT", 0));
    }

    @Test
    void verifyEmail_withValidToken_returns200() throws Exception {
        when(authService.verifyEmail("good-token")).thenReturn(new VerifyEmailResponse(true, "fresh-jwt", null));

        mockMvc.perform(get("/api/v1/auth/verify-email").param("token", "good-token")).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.token").value("fresh-jwt"));
    }

    @Test
    void verifyEmail_withInvalidToken_returns400() throws Exception {
        when(authService.verifyEmail("bad-token"))
                .thenReturn(new VerifyEmailResponse(false, null, "Invalid verification token"));

        mockMvc.perform(get("/api/v1/auth/verify-email").param("token", "bad-token")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid verification token"));
    }

    @Test
    void resendVerification_whenAuthenticated_returns200() throws Exception {
        when(authService.resendVerification("testuser"))
                .thenReturn(new ResendVerificationResponse(true, false, "Verification email sent"));

        mockMvc.perform(post("/api/v1/auth/resend-verification")).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void resendVerification_whenRateLimited_returns429() throws Exception {
        when(authService.resendVerification("testuser")).thenReturn(new ResendVerificationResponse(false, true,
                "Please wait before requesting another verification email"));

        mockMvc.perform(post("/api/v1/auth/resend-verification")).andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void changeEmail_withValidEmail_returns200() throws Exception {
        when(authService.requestEmailChange(eq("testuser"), anyString()))
                .thenReturn(new RequestEmailChangeResponse(true, "Confirmation email sent to new address"));

        mockMvc.perform(put("/api/v1/auth/email").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"new@example.com\"}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void changeEmail_withBlankEmail_returns400() throws Exception {
        mockMvc.perform(put("/api/v1/auth/email").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void checkUsername_whenAvailable_returnsTrue() throws Exception {
        when(authService.isUsernameAvailable("newuser")).thenReturn(true);

        mockMvc.perform(get("/api/v1/auth/check-username").param("username", "newuser")).andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void checkUsername_whenTaken_returnsFalse() throws Exception {
        when(authService.isUsernameAvailable("takenuser")).thenReturn(false);

        mockMvc.perform(get("/api/v1/auth/check-username").param("username", "takenuser")).andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
    }
}
