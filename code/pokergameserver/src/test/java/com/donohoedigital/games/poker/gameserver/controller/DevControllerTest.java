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

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.donohoedigital.games.poker.gameserver.integration.TestApplication;
import com.donohoedigital.games.poker.gameserver.persistence.repository.OnlineProfileRepository;
import com.donohoedigital.games.poker.model.OnlineProfile;

/**
 * Integration tests for {@link DevController}.
 *
 * <p>
 * Loads the full application context with the {@code embedded} profile active
 * so that the {@code @Profile("embedded")} controller is registered. Uses an
 * in-memory H2 database via the test application configuration.
 *
 * <p>
 * The dev endpoints are unauthenticated by design, so the inner
 * {@link OpenSecurityConfig} overrides the security chain to permit all
 * requests.
 */
@SpringBootTest(classes = {TestApplication.class, DevControllerTest.OpenSecurityConfig.class})
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration
@TestPropertySource(properties = {"spring.main.allow-bean-definition-overriding=true"})
@ActiveProfiles("embedded")
class DevControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OnlineProfileRepository profileRepository;

    @TestConfiguration
    static class OpenSecurityConfig {

        /**
         * Overrides the {@code securityFilterChain} bean from {@link TestApplication}
         * (same bean name, bean-definition overriding enabled) to permit all requests.
         * The dev endpoints are unauthenticated by design; this avoids a 403 in tests.
         */
        @Bean(name = "securityFilterChain")
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.csrf(csrf -> csrf.disable()).authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @Test
    void verifyUser_marksUserAsVerified() throws Exception {
        OnlineProfile profile = newProfile("devtestuser");
        profile.setEmailVerified(false);
        profile.setEmailVerificationToken("some-token");
        profile.setEmailVerificationTokenExpiry(System.currentTimeMillis() + 3600_000L);
        profileRepository.save(profile);

        mockMvc.perform(post("/api/v1/dev/verify-user").param("username", "devtestuser")).andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(true)).andExpect(jsonPath("$.verified").value(true))
                .andExpect(jsonPath("$.username").value("devtestuser"));

        OnlineProfile updated = profileRepository.findByName("devtestuser").orElseThrow();
        assertThat(updated.isEmailVerified()).isTrue();
        assertThat(updated.getEmailVerificationToken()).isNull();
        assertThat(updated.getEmailVerificationTokenExpiry()).isNull();
    }

    @Test
    void verifyUser_returns404ForUnknownUser() throws Exception {
        mockMvc.perform(post("/api/v1/dev/verify-user").param("username", "no-such-user"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.found").value(false))
                .andExpect(jsonPath("$.username").value("no-such-user"));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private OnlineProfile newProfile(String name) {
        OnlineProfile p = new OnlineProfile();
        p.setName(name);
        p.setEmail(name + "@example.com");
        p.setPasswordHash("hash");
        p.setUuid(UUID.randomUUID().toString());
        return p;
    }
}
