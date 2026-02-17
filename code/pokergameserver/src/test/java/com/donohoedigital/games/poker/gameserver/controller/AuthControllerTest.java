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
import com.donohoedigital.games.poker.gameserver.dto.LoginResponse;
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
        when(authService.register(anyString(), anyString(), anyString()))
                .thenReturn(new LoginResponse(true, "test-token", 1L, "testuser", null));

        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"testuser\",\"password\":\"password123\",\"email\":\"test@example.com\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.token").value("test-token")).andExpect(jsonPath("$.profileId").value(1))
                .andExpect(jsonPath("$.username").value("testuser")).andExpect(cookie().exists("DDPoker-JWT"));
    }

    @Test
    void testRegisterFailure() throws Exception {
        when(authService.register(anyString(), anyString(), anyString()))
                .thenReturn(new LoginResponse(false, null, null, null, "Username already exists"));

        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"existing\",\"password\":\"password\",\"email\":\"test@example.com\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Username already exists"));
    }

    @Test
    void testLoginSuccess() throws Exception {
        when(authService.login(anyString(), anyString(), anyBoolean()))
                .thenReturn(new LoginResponse(true, "test-token", 1L, "testuser", null));

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"testuser\",\"password\":\"password123\",\"rememberMe\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.token").value("test-token")).andExpect(jsonPath("$.profileId").value(1))
                .andExpect(jsonPath("$.username").value("testuser")).andExpect(cookie().exists("DDPoker-JWT"));
    }

    @Test
    void testLoginFailure() throws Exception {
        when(authService.login(anyString(), anyString(), anyBoolean()))
                .thenReturn(new LoginResponse(false, null, null, null, "Invalid username or password"));

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"testuser\",\"password\":\"wrongpass\",\"rememberMe\":false}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void testLogout() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")).andExpect(status().isOk())
                .andExpect(cookie().maxAge("DDPoker-JWT", 0));
    }
}
